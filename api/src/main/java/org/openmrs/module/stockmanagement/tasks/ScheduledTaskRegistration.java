package org.openmrs.module.stockmanagement.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.jobs.AsyncTasksBatchJob;
import org.openmrs.module.stockmanagement.api.jobs.StockBatchExpiryJob;
import org.openmrs.module.stockmanagement.api.jobs.StockRuleEvaluationJob;
import org.openmrs.module.stockmanagement.api.utils.SchedulerTasks;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.util.PrivilegeConstants;

import java.util.Date;

public class ScheduledTaskRegistration implements StartupTask {

	private Log log = LogFactory.getLog(this.getClass());

	@Override
	public void execute() {
		Context.addProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
		try {
			SchedulerService schedulerService = Context.getSchedulerService();
			registerIfMissing(schedulerService, AsyncTasksBatchJob.class,
			    "Asynchronous Report Generation And Batch Jobs", "Runs report generation and batch jobs asynchronously",
			    "73eb1d31-6e5e-11ed-9a5b-00155db5005a", 60L);
			registerIfMissing(schedulerService, StockRuleEvaluationJob.class, "Stock Rules Evaluation Job",
			    "Checks the stock rules against the item quantity and notifies when quantity is at or below threshold",
			    "5a0b041a-5796-11ed-a495-00155dc81c72", 86400L);
			registerIfMissing(schedulerService, StockBatchExpiryJob.class, "Stock Batch Expiry Job",
			    "Checks for stock items expiring soon and notifies inventory manager and clerk",
			    "911fc382-5797-11ed-a495-00155dc81c72", 86400L);
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
		}
	}

	private void registerIfMissing(SchedulerService schedulerService, Class<? extends Task> taskClass, String name,
	        String description, String legacyUuid, long repeatInterval) {
		if (SchedulerTasks.getTaskByClass(schedulerService, taskClass) != null) {
			return;
		}

		TaskDefinition taskDefinition = new TaskDefinition();
		taskDefinition.setName(name);
		taskDefinition.setDescription(description);
		taskDefinition.setTaskClass(taskClass.getName());
		taskDefinition.setStartTimePattern("MM/dd/yyyy HH:mm:ss");
		taskDefinition.setStartTime(new Date(0));
		taskDefinition.setRepeatInterval(repeatInterval);
		taskDefinition.setStartOnStartup(true);
		if (schedulerService.getTaskByUuid(legacyUuid) == null) {
			taskDefinition.setUuid(legacyUuid);
		}

		schedulerService.saveTaskDefinition(taskDefinition);
		try {
			schedulerService.scheduleTask(taskDefinition);
		}
		catch (SchedulerException exception) {
			log.error("Error while scheduling " + taskClass.getName(), exception);
		}
	}

	@Override
	public int getPriority() {
		return 100;
	}
}
