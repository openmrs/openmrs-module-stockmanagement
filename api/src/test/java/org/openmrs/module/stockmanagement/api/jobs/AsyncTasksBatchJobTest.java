/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.jobs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.ContextMockHelper;
import org.openmrs.module.stockmanagement.api.model.BatchJob;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class AsyncTasksBatchJobTest extends BaseModuleContextSensitiveTest {
	
	private ContextMockHelper contextMockHelper;
	
	private SchedulerService schedulerService;
	
	private TaskDefinition renamedReportTask;
	
	@BeforeEach
	public void setup() {
		contextMockHelper = new ContextMockHelper();
		schedulerService = mock(SchedulerService.class);
		contextMockHelper.setService(SchedulerService.class, schedulerService);
		renamedReportTask = new TaskDefinition();
		renamedReportTask.setName("Renamed report task");
		renamedReportTask.setTaskClass(AsyncTasksBatchJob.class.getName());
		when(schedulerService.getRegisteredTasks()).thenReturn(Collections.singletonList(renamedReportTask));
	}
	
	@AfterEach
	public void teardown() {
		contextMockHelper.revertMocks();
	}
	
	@Test
	public void queueBatchJob_shouldStartTheReportTaskFoundByClass() {
		AsyncTasksBatchJob.queueBatchJob(new BatchJob());
		
		verify(schedulerService).scheduleIfNotRunning(renamedReportTask);
	}
	
	@Test
	public void stopThisJob_shouldShutDownTheReportTaskFoundByClass() throws Exception {
		new AsyncTasksBatchJob().stopThisJob();
		
		verify(schedulerService).shutdownTask(renamedReportTask);
	}
}
