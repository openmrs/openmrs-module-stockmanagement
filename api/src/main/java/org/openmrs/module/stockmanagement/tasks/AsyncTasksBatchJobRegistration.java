package org.openmrs.module.stockmanagement.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.jobs.AsyncTasksBatchJob;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.util.PrivilegeConstants;

import java.util.Date;

public class AsyncTasksBatchJobRegistration implements StartupTask {

	private static final String TASK_UUID = "73eb1d31-6e5e-11ed-9a5b-00155db5005a";

	private Log log = LogFactory.getLog(this.getClass());

	@Override
	public void execute() {
		Context.addProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
		try {
			registerIfMissing();
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
		}
	}

	private void registerIfMissing() {
		SchedulerService schedulerService = Context.getSchedulerService();
		if (schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME) != null) {
			return;
		}

		TaskDefinition taskDefinition = new TaskDefinition();
		taskDefinition.setName(AsyncTasksBatchJob.TASK_NAME);
		taskDefinition.setDescription("Runs report generation and batch jobs asynchronously");
		taskDefinition.setTaskClass(AsyncTasksBatchJob.class.getName());
		taskDefinition.setStartTimePattern("MM/dd/yyyy HH:mm:ss");
		taskDefinition.setStartTime(new Date(0));
		taskDefinition.setRepeatInterval(60L);
		taskDefinition.setStartOnStartup(true);
		if (schedulerService.getTaskByUuid(TASK_UUID) == null) {
			taskDefinition.setUuid(TASK_UUID);
		}

		schedulerService.saveTaskDefinition(taskDefinition);
		try {
			schedulerService.scheduleTask(taskDefinition);
		}
		catch (SchedulerException exception) {
			log.error("Error while scheduling asynchronous report generation and batch jobs", exception);
		}
	}

	@Override
	public int getPriority() {
		return 100;
	}
}
