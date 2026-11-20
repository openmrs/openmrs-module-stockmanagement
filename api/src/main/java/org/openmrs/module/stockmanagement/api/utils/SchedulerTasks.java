/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
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
