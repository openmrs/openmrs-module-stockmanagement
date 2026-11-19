package org.openmrs.module.stockmanagement.api.utils;

import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.TaskDefinition;

public final class SchedulerTasks {

	private SchedulerTasks() {
	}

	/**
	 * Finds a task by its implementation class, regardless of its configurable display name.
	 */
	public static TaskDefinition getTaskByClass(SchedulerService schedulerService, Class<? extends Task> taskClass) {
		for (TaskDefinition taskDefinition : schedulerService.getRegisteredTasks()) {
			if (taskClass.getName().equals(taskDefinition.getTaskClass())) {
				return taskDefinition;
			}
		}
		return null;
	}
}
