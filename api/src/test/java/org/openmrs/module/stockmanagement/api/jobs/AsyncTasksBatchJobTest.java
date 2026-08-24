/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmrs.module.stockmanagement.api.jobs;

import org.junit.Test;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncTasksBatchJobTest {

	private static final String TASK_UUID = "73eb1d31-6e5e-11ed-9a5b-00155db5005a";

	@Test
	public void ensureTaskDefinition_shouldCreateMissingTaskDefinition() {
		SchedulerService schedulerService = mock(SchedulerService.class);
		when(schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME)).thenReturn(null);
		when(schedulerService.getTaskByUuid(TASK_UUID)).thenReturn(null);

		TaskDefinition result = AsyncTasksBatchJob.ensureTaskDefinition(schedulerService);

		assertEquals(AsyncTasksBatchJob.TASK_NAME, result.getName());
		assertEquals(TASK_UUID, result.getUuid());
		assertEquals(AsyncTasksBatchJob.class.getName(), result.getTaskClass());
		assertEquals(Long.valueOf(60L), result.getRepeatInterval());
		assertEquals(Boolean.TRUE, result.getStartOnStartup());
		assertNotNull(result.getStartTime());
		verify(schedulerService).saveTaskDefinition(result);
	}

	@Test
	public void ensureTaskDefinition_shouldRepairTaskFoundByUuid() {
		SchedulerService schedulerService = mock(SchedulerService.class);
		TaskDefinition existingTask = new TaskDefinition();
		existingTask.setUuid(TASK_UUID);
		existingTask.setName("Renamed task");
		when(schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME)).thenReturn(null);
		when(schedulerService.getTaskByUuid(TASK_UUID)).thenReturn(existingTask);

		TaskDefinition result = AsyncTasksBatchJob.ensureTaskDefinition(schedulerService);

		assertSame(existingTask, result);
		assertEquals(AsyncTasksBatchJob.TASK_NAME, result.getName());
		assertEquals(AsyncTasksBatchJob.class.getName(), result.getTaskClass());
		verify(schedulerService).saveTaskDefinition(existingTask);
	}
}
