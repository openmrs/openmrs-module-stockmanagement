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
	public void ensureTaskDefinition_shouldRepairWrongTaskClassWithoutChangingSchedulerConfiguration() {
		SchedulerService schedulerService = mock(SchedulerService.class);
		TaskDefinition existingTask = new TaskDefinition();
		existingTask.setUuid(TASK_UUID);
		existingTask.setName("Renamed task");
		existingTask.setTaskClass("org.openmrs.scheduler.tasks.TestTask");
		existingTask.setRepeatInterval(3600L);
		existingTask.setStartOnStartup(false);
		when(schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME)).thenReturn(null);
		when(schedulerService.getTaskByUuid(TASK_UUID)).thenReturn(existingTask);

		TaskDefinition result = AsyncTasksBatchJob.ensureTaskDefinition(schedulerService);

		assertSame(existingTask, result);
		assertEquals("Renamed task", result.getName());
		assertEquals(AsyncTasksBatchJob.class.getName(), result.getTaskClass());
		assertEquals(Long.valueOf(3600L), result.getRepeatInterval());
		assertEquals(Boolean.FALSE, result.getStartOnStartup());
		verify(schedulerService).saveTaskDefinition(existingTask);
	}

	@Test
	public void ensureTaskDefinition_shouldNotSaveValidExistingTaskDefinition() {
		SchedulerService schedulerService = mock(SchedulerService.class);
		TaskDefinition existingTask = new TaskDefinition();
		existingTask.setUuid(TASK_UUID);
		existingTask.setName(AsyncTasksBatchJob.TASK_NAME);
		existingTask.setTaskClass(AsyncTasksBatchJob.class.getName());
		existingTask.setRepeatInterval(3600L);
		existingTask.setStartOnStartup(false);
		when(schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME)).thenReturn(existingTask);

		TaskDefinition result = AsyncTasksBatchJob.ensureTaskDefinition(schedulerService);

		assertSame(existingTask, result);
		assertEquals(Long.valueOf(3600L), result.getRepeatInterval());
		assertEquals(Boolean.FALSE, result.getStartOnStartup());
		verify(schedulerService, never()).saveTaskDefinition(any(TaskDefinition.class));
		verify(schedulerService, never()).getTaskByUuid(anyString());
	}

	@Test
	public void ensureTaskDefinition_shouldPropagateLookupFailureWithoutWriting() {
		SchedulerService schedulerService = mock(SchedulerService.class);
		RuntimeException lookupFailure = new RuntimeException("Database unavailable");
		when(schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME)).thenThrow(lookupFailure);

		try {
			AsyncTasksBatchJob.ensureTaskDefinition(schedulerService);
			fail("Expected scheduler lookup failure to propagate");
		} catch (RuntimeException exception) {
			assertSame(lookupFailure, exception);
		}

		verify(schedulerService, never()).getTaskByUuid(anyString());
		verify(schedulerService, never()).saveTaskDefinition(any(TaskDefinition.class));
	}
}
