package org.openmrs.module.stockmanagement.tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.openmrs.api.context.ContextMockHelper;
import org.openmrs.module.stockmanagement.api.jobs.AsyncTasksBatchJob;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncTasksBatchJobRegistrationTest extends BaseModuleContextSensitiveTest {

	private static final String TASK_UUID = "73eb1d31-6e5e-11ed-9a5b-00155db5005a";

	private ContextMockHelper contextMockHelper;

	private SchedulerService schedulerService;

	private AsyncTasksBatchJobRegistration registration;

	@Before
	public void setup() {
		contextMockHelper = new ContextMockHelper();
		schedulerService = mock(SchedulerService.class);
		contextMockHelper.setService(SchedulerService.class, schedulerService);
		registration = new AsyncTasksBatchJobRegistration();
	}

	@After
	public void teardown() {
		contextMockHelper.revertMocks();
	}

	@Test
	public void execute_shouldCreateAndScheduleTaskWhenMissing() throws Exception {
		registration.execute();

		ArgumentCaptor<TaskDefinition> captor = ArgumentCaptor.forClass(TaskDefinition.class);
		InOrder inOrder = inOrder(schedulerService);
		inOrder.verify(schedulerService).saveTaskDefinition(captor.capture());
		TaskDefinition taskDefinition = captor.getValue();
		inOrder.verify(schedulerService).scheduleTask(taskDefinition);
		assertEquals(AsyncTasksBatchJob.TASK_NAME, taskDefinition.getName());
		assertEquals("Runs report generation and batch jobs asynchronously", taskDefinition.getDescription());
		assertEquals(AsyncTasksBatchJob.class.getName(), taskDefinition.getTaskClass());
		assertEquals("MM/dd/yyyy HH:mm:ss", taskDefinition.getStartTimePattern());
		assertTrue(taskDefinition.getStartTime().before(new Date()));
		assertEquals(Long.valueOf(60L), taskDefinition.getRepeatInterval());
		assertTrue(taskDefinition.getStartOnStartup());
		assertEquals(TASK_UUID, taskDefinition.getUuid());

		when(schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME)).thenReturn(taskDefinition);
		registration.execute();
		verify(schedulerService, times(1)).saveTaskDefinition(any(TaskDefinition.class));
		verify(schedulerService, times(1)).scheduleTask(taskDefinition);
	}

	@Test
	public void execute_shouldLeaveExistingTaskUnchanged() {
		TaskDefinition existingTask = new TaskDefinition();
		existingTask.setName(AsyncTasksBatchJob.TASK_NAME);
		existingTask.setRepeatInterval(120L);
		existingTask.setStartOnStartup(false);
		when(schedulerService.getTaskByName(AsyncTasksBatchJob.TASK_NAME)).thenReturn(existingTask);

		registration.execute();

		verify(schedulerService).getTaskByName(AsyncTasksBatchJob.TASK_NAME);
		verifyNoMoreInteractions(schedulerService);
		assertEquals(Long.valueOf(120L), existingTask.getRepeatInterval());
		assertFalse(existingTask.getStartOnStartup());
	}

	@Test
	public void execute_shouldUseNewUuidWhenLegacyUuidIsTaken() throws Exception {
		when(schedulerService.getTaskByUuid(TASK_UUID)).thenReturn(new TaskDefinition());

		registration.execute();

		ArgumentCaptor<TaskDefinition> captor = ArgumentCaptor.forClass(TaskDefinition.class);
		verify(schedulerService).saveTaskDefinition(captor.capture());
		assertNotNull(captor.getValue().getUuid());
		assertNotEquals(TASK_UUID, captor.getValue().getUuid());
		verify(schedulerService).scheduleTask(captor.getValue());
	}

	@Test
	public void execute_shouldKeepTaskDefinitionWhenSchedulingFails() throws Exception {
		when(schedulerService.scheduleTask(any(TaskDefinition.class))).thenThrow(
		    new SchedulerException("Cannot schedule task"));

		registration.execute();

		ArgumentCaptor<TaskDefinition> captor = ArgumentCaptor.forClass(TaskDefinition.class);
		verify(schedulerService).saveTaskDefinition(captor.capture());
		verify(schedulerService).scheduleTask(captor.getValue());
	}
}
