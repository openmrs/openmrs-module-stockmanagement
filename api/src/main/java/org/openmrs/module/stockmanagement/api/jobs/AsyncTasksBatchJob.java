package org.openmrs.module.stockmanagement.api.jobs;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.BatchJob;
import org.openmrs.module.stockmanagement.api.model.BatchJobType;
import org.openmrs.module.stockmanagement.api.reporting.Report;
import org.openmrs.module.stockmanagement.api.reporting.ReportGenerator;
import org.openmrs.module.stockmanagement.api.utils.FileUtil;
import org.openmrs.module.stockmanagement.api.utils.GlobalProperties;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncTasksBatchJob extends AbstractTask {

	public static final String TASK_NAME = "Asynchronous Report Generation And Batch Jobs";

	private static final String TASK_DESCRIPTION = "Runs report generation and batch jobs asynchronously";

	private static final String TASK_UUID = "73eb1d31-6e5e-11ed-9a5b-00155db5005a";

	private static final long TASK_REPEAT_INTERVAL_SECONDS = 60L;

	private static final Log taskLog = LogFactory.getLog(AsyncTasksBatchJob.class);
	
	private static AtomicBoolean isAlreadyRunning = new AtomicBoolean(false);
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private static final Queue<String> stopExecutionQueue = new ConcurrentLinkedQueue<String>();
	
	private volatile boolean shutDown = false;
	
	private volatile Integer idleTicks = 0;
	
	private List<Report> allReports = null;
	
	@Override
	public void execute() {
		shutDown = false;
		if (isAlreadyRunning.get()) {
			log.debug("Async tasks job is already running");
			return;
		}
		try {
			if (isAlreadyRunning.getAndSet(true)) {
				log.debug("Async tasks job is already running");
				return;
			}
			startExecuting();
			executeInternal();
		}
		catch (Exception exception) {
			log.error("Error occurred while executing Async tasks batch job");
			log.error(exception.getMessage(), exception);
		}
		finally {
			isAlreadyRunning.set(false);
			stopExecuting();
		}
	}
	
	protected void executeInternal() {
		StockManagementService stockManagementService = Context.getService(StockManagementService.class);
		String previousBatchJobUuid = null;
		int previousBatchJobSeenCount = 0;
		while (!shutDown) {
			BatchJob batchJob = stockManagementService.getNextActiveBatchJob();
			if (batchJob == null) {
				idleTicks++;
				if (idleTicks >= 5) {
					stopThisJob();
				} else if (idleTicks.equals(1)) {
					cleanUpExpiredJobs();
				}
				break;
			}
			idleTicks = 0;
			if (previousBatchJobUuid == null || !previousBatchJobUuid.equals(batchJob.getUuid())) {
				previousBatchJobSeenCount = 0;
				previousBatchJobUuid = batchJob.getUuid();
			} else {
				if (previousBatchJobSeenCount < 4) {
					previousBatchJobSeenCount++;
					previousBatchJobUuid = batchJob.getUuid();
				} else {
					throw new StockManagementException(String.format(
					    Context.getMessageSourceService().getMessage("stockmanagement.batchjob.samebatchjobseen"),
					    batchJob.getDescription(), batchJob.getUuid(), Integer.toString(previousBatchJobSeenCount)));
				}
			}
			
			try {
				executeBatchJob(batchJob, stockManagementService);
			}
			catch (Exception exception) {
				log.error(exception.getMessage(), exception);
				try {
					stockManagementService.failBatchJob(batchJob.getUuid(), String.format(Context.getMessageSourceService()
					        .getMessage("stockmanagement.batchjob.reportunexpectederror"), exception.getMessage()));
				}
				catch (Exception silentException) {
					log.error(silentException.getMessage(), silentException);
				}
			}
		}
	}
	
	private void executeBatchJob(BatchJob batchJob, StockManagementService stockManagementService) {
		// Check if the batch job is expired
		if (batchJob.getExpiration() != null && (new Date()).after(batchJob.getExpiration())) {
			stockManagementService.expireBatchJob(batchJob.getUuid(),
			    Context.getMessageSourceService().getMessage("stockmanagement.batchjob.batchjobexpired"));
			return;
		}
		
		if (!BatchJobType.Report.equals(batchJob.getBatchJobType())) {
			stockManagementService.failBatchJob(batchJob.getUuid(),
			    Context.getMessageSourceService().getMessage("stockmanagement.batchjob.batchjobnotsupported"));
			return;
		}
		
		executeReportBatchJob(batchJob, stockManagementService);
	}
	
	private void executeReportBatchJob(BatchJob batchJob, StockManagementService stockManagementService) {
		Properties properties = null;
		try {
			properties = GlobalProperties.fromString(batchJob.getParameters());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			stockManagementService.failBatchJob(batchJob.getUuid(),
					Context.getMessageSourceService().getMessage("stockmanagement.batchjob.parameterformatnotvalid"));
			return;
		}

		if (allReports == null) {
			allReports = stockManagementService.getReports();
		}

		String reportSystemName = properties.getProperty("param.report");
		if (StringUtils.isBlank(reportSystemName)) {
			stockManagementService.failBatchJob(batchJob.getUuid(),
					String.format(
							Context.getMessageSourceService().getMessage("stockmanagement.batchjob.fieldvaluenotexist"),
							"report system name"));
			return;
		}

		Optional<Report> report = allReports.stream().filter(p -> reportSystemName.equals(p.getSystemName())).findAny();
		if (!report.isPresent()) {
			stockManagementService.failBatchJob(batchJob.getUuid(),
					String.format(
							Context.getMessageSourceService().getMessage("stockmanagement.batchjob.fieldvaluenotexist"),
							"report by system name"));
			return;
		}

		ReportGenerator reportGenerator = null;
		try {
			reportGenerator = (ReportGenerator) report.get().getReportGeneratorClass().newInstance();
		} catch (Exception exception) {
			log.error(exception.getMessage(), exception);
			stockManagementService
					.failBatchJob(batchJob.getUuid(),
							String.format(
									Context.getMessageSourceService()
											.getMessage("stockmanagement.batchjob.failedtocreatereportgenerator"),
									exception.getMessage()));
			return;
		}

		stockManagementService.updateBatchJobRunning(batchJob.getUuid());

		reportGenerator.generateReport(batchJob, p -> {
			String batchJobToStop = null;
			if (shutDown) {
				stopExecutionQueue.clear();
				return true;
			}
			boolean result = false;
			while ((batchJobToStop = stopExecutionQueue.poll()) != null) {
				if (batchJobToStop.equals(p.getUuid())) {
					result = true;
					break;
				}
			}
			stopExecutionQueue.clear();
			return result;
		});
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		shutDown = true;
	}
	
	public static boolean stopBatchJob(BatchJob batchJob) {
		stopExecutionQueue.add(batchJob.getUuid());
		return true;
	}
	
	protected void stopThisJob() {
		stopThisJob(Context.getSchedulerService());
	}

	void stopThisJob(SchedulerService schedulerService) {
		TaskDefinition taskDefinition = schedulerService.getTaskByName(TASK_NAME);
		if (taskDefinition == null) {
			taskDefinition = schedulerService.getTaskByUuid(TASK_UUID);
		}
		if (taskDefinition != null) {
			try {
				schedulerService.shutdownTask(taskDefinition);
			}
			catch (SchedulerException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static void queueBatchJob(BatchJob batchJob) {
		ensureTaskScheduled();
	}

	/**
	 * Ensures the asynchronous batch-job task exists before scheduling it. Scheduler definitions can be removed
	 * independently of the module's Liquibase history, so relying only on the original insert change set can leave all
	 * reports pending indefinitely.
	 *
	 * @return the persisted scheduler task definition
	 */
	public static TaskDefinition ensureTaskScheduled() {
		SchedulerService schedulerService = Context.getSchedulerService();
		TaskDefinition taskDefinition = ensureTaskDefinition(schedulerService);
		schedulerService.scheduleIfNotRunning(taskDefinition);
		return taskDefinition;
	}

	static synchronized TaskDefinition ensureTaskDefinition(SchedulerService schedulerService) {
		TaskDefinition taskDefinition = schedulerService.getTaskByName(TASK_NAME);
		if (taskDefinition == null) {
			taskDefinition = schedulerService.getTaskByUuid(TASK_UUID);
		}

		if (taskDefinition == null) {
			taskDefinition = new TaskDefinition();
			taskDefinition.setUuid(TASK_UUID);
			taskDefinition.setStarted(false);
			taskDefinition.setName(TASK_NAME);
			taskDefinition.setDescription(TASK_DESCRIPTION);
			taskDefinition.setTaskClass(AsyncTasksBatchJob.class.getName());
			taskDefinition.setRepeatInterval(TASK_REPEAT_INTERVAL_SECONDS);
			taskDefinition.setStartOnStartup(true);
			taskDefinition.setStartTime(new Date());
			taskLog.warn("Recreating missing asynchronous report and batch-job scheduler task");
			schedulerService.saveTaskDefinition(taskDefinition);
		} else if (!AsyncTasksBatchJob.class.getName().equals(taskDefinition.getTaskClass())) {
			taskDefinition.setTaskClass(AsyncTasksBatchJob.class.getName());
			schedulerService.saveTaskDefinition(taskDefinition);
		}
		return taskDefinition;
	}

	private void silentDelete(File file) {
		try {
			if (file.exists()) {
				file.delete();
			}
		}
		catch (Exception exception) {
			log.error(exception.getMessage(), exception);
		}
	}
	
	public void cleanUpExpiredJobs() {
		StockManagementService stockManagementService = Context.getService(StockManagementService.class);
		List<BatchJob> batchJobs = stockManagementService.getExpiredBatchJobs();
		for (BatchJob batchJob : batchJobs) {
			File file = new File(FileUtil.getBatchJobFolder(), batchJob.getUuid());
			silentDelete(file);
			file = new File(FileUtil.getBatchJobFolder(), batchJob.getUuid() + ".step1");
			silentDelete(file);
			stockManagementService.deleteBatchJob(batchJob);
		}
	}
	
}
