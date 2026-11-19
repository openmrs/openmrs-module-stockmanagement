package org.openmrs.module.stockmanagement.tasks;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.openmrs.api.context.ContextMockHelper;
import org.openmrs.module.stockmanagement.api.jobs.AsyncTasksBatchJob;
import org.openmrs.module.stockmanagement.api.jobs.StockBatchExpiryJob;
import org.openmrs.module.stockmanagement.api.jobs.StockRuleEvaluationJob;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ScheduledTaskRegistrationTest extends BaseModuleContextSensitiveTest {

	private static final List<String> TASK_UUIDS = Arrays.asList("73eb1d31-6e5e-11ed-9a5b-00155db5005a",
	    "5a0b041a-5796-11ed-a495-00155dc81c72", "911fc382-5797-11ed-a495-00155dc81c72");

	private ContextMockHelper contextMockHelper;

	private SchedulerService schedulerService;

	private ScheduledTaskRegistration registration;

	@Before
	public void setup() {
		contextMockHelper = new ContextMockHelper();
		schedulerService = mock(SchedulerService.class);
		contextMockHelper.setService(SchedulerService.class, schedulerService);
		registration = new ScheduledTaskRegistration();
	}

	@After
	public void teardown() {
		contextMockHelper.revertMocks();
	}

	@Test
	public void execute_shouldCreateAndScheduleAllMissingTasksAndReuseFreeLegacyUuids() throws Exception {
		registration.execute();

		List<TaskDefinition> tasks = getSavedTasks(3);
		assertTask(tasks.get(0), AsyncTasksBatchJob.class.getName(), "Asynchronous Report Generation And Batch Jobs",
		    "Runs report generation and batch jobs asynchronously", 60L, TASK_UUIDS.get(0));
		assertTask(tasks.get(1), StockRuleEvaluationJob.class.getName(), "Stock Rules Evaluation Job",
		    "Checks the stock rules against the item quantity and notifies when quantity is at or below threshold",
		    86400L, TASK_UUIDS.get(1));
		assertTask(tasks.get(2), StockBatchExpiryJob.class.getName(), "Stock Batch Expiry Job",
		    "Checks for stock items expiring soon and notifies inventory manager and clerk", 86400L, TASK_UUIDS.get(2));

		InOrder inOrder = inOrder(schedulerService);
		for (TaskDefinition task : tasks) {
			inOrder.verify(schedulerService).saveTaskDefinition(task);
			inOrder.verify(schedulerService).scheduleTask(task);
		}

		when(schedulerService.getRegisteredTasks()).thenReturn(tasks);
		registration.execute();
		verify(schedulerService, times(3)).saveTaskDefinition(any(TaskDefinition.class));
		verify(schedulerService, times(3)).scheduleTask(any(TaskDefinition.class));
	}

	@Test
	public void execute_shouldLeaveRenamedExistingTasksAndTheirSettingsUnchanged() throws Exception {
		List<TaskDefinition> tasks = Arrays.asList(existingTask(AsyncTasksBatchJob.class.getName()),
		    existingTask(StockRuleEvaluationJob.class.getName()), existingTask(StockBatchExpiryJob.class.getName()));
		when(schedulerService.getRegisteredTasks()).thenReturn(tasks);

		registration.execute();
		registration.execute();

		verify(schedulerService, times(6)).getRegisteredTasks();
		verifyNoMoreInteractions(schedulerService);
		for (TaskDefinition task : tasks) {
			assertEquals("Custom " + task.getTaskClass(), task.getName());
			assertEquals("Custom description", task.getDescription());
			assertEquals(Long.valueOf(120L), task.getRepeatInterval());
			assertFalse(task.getStartOnStartup());
			assertEquals(new Date(123456789L), task.getStartTime());
			assertEquals("Custom pattern", task.getStartTimePattern());
			assertEquals("Custom " + task.getTaskClass(), task.getUuid());
		}
	}

	@Test
	public void execute_shouldReuseExistingReportTaskAndRegisterOnlyMissingAlerts() throws Exception {
		TaskDefinition existingReport = existingTask(AsyncTasksBatchJob.class.getName());
		when(schedulerService.getRegisteredTasks()).thenReturn(Arrays.asList(existingReport));

		registration.execute();

		List<TaskDefinition> tasks = getSavedTasks(2);
		assertEquals(StockRuleEvaluationJob.class.getName(), tasks.get(0).getTaskClass());
		assertEquals(StockBatchExpiryJob.class.getName(), tasks.get(1).getTaskClass());
		verify(schedulerService, never()).saveTaskDefinition(existingReport);
		verify(schedulerService, never()).scheduleTask(existingReport);
	}

	@Test
	public void execute_shouldUseNewUuidsWhenLegacyUuidsBelongToOtherTasks() throws Exception {
		TaskDefinition unrelatedTask = existingTask("example.UnrelatedTask");
		// Neither a matching display name nor a legacy UUID identifies the implementation class.
		unrelatedTask.setName("Asynchronous Report Generation And Batch Jobs");
		when(schedulerService.getRegisteredTasks()).thenReturn(Arrays.asList(unrelatedTask));
		for (String uuid : TASK_UUIDS) {
			when(schedulerService.getTaskByUuid(uuid)).thenReturn(unrelatedTask);
		}

		registration.execute();

		List<TaskDefinition> tasks = getSavedTasks(3);
		for (TaskDefinition task : tasks) {
			assertNotNull(task.getUuid());
			assertFalse(TASK_UUIDS.contains(task.getUuid()));
			verify(schedulerService).scheduleTask(task);
		}
		assertEquals(3L, tasks.stream().map(TaskDefinition::getUuid).distinct().count());
		verify(schedulerService, never()).saveTaskDefinition(unrelatedTask);
		verify(schedulerService, never()).scheduleTask(unrelatedTask);
	}

	@Test
	public void execute_shouldLogSchedulingFailureAndContinueRegisteringOtherTasks() throws Exception {
		Log log = mock(Log.class);
		ReflectionTestUtils.setField(registration, "log", log);
		SchedulerException failure = new SchedulerException("Cannot schedule task");
		when(schedulerService.scheduleTask(any(TaskDefinition.class))).thenThrow(failure).thenReturn(null);

		registration.execute();

		List<TaskDefinition> tasks = getSavedTasks(3);
		for (TaskDefinition task : tasks) {
			verify(schedulerService).scheduleTask(task);
		}
		verify(log).error("Error while scheduling " + AsyncTasksBatchJob.class.getName(), failure);
	}

	private List<TaskDefinition> getSavedTasks(int count) {
		ArgumentCaptor<TaskDefinition> captor = ArgumentCaptor.forClass(TaskDefinition.class);
		verify(schedulerService, times(count)).saveTaskDefinition(captor.capture());
		return captor.getAllValues();
	}

	private TaskDefinition existingTask(String taskClass) {
		TaskDefinition task = new TaskDefinition();
		task.setTaskClass(taskClass);
		task.setName("Custom " + taskClass);
		task.setDescription("Custom description");
		task.setRepeatInterval(120L);
		task.setStartOnStartup(false);
		task.setStartTime(new Date(123456789L));
		task.setStartTimePattern("Custom pattern");
		task.setUuid("Custom " + taskClass);
		return task;
	}

	private void assertTask(TaskDefinition task, String taskClass, String name, String description, long interval,
	        String uuid) {
		assertEquals(taskClass, task.getTaskClass());
		assertEquals(name, task.getName());
		assertEquals(description, task.getDescription());
		assertEquals("MM/dd/yyyy HH:mm:ss", task.getStartTimePattern());
		assertTrue(task.getStartTime().before(new Date()));
		assertEquals(Long.valueOf(interval), task.getRepeatInterval());
		assertTrue(task.getStartOnStartup());
		assertEquals(uuid, task.getUuid());
	}
}
