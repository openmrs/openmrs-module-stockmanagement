package org.openmrs.module.stockmanagement.web.controller;

import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.BatchJob;
import org.openmrs.module.stockmanagement.api.model.BatchJobType;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatchJobArtifactControllerTest {

	private final TestUser user = new TestUser();

	@Test
	public void canAccessArtifact_shouldAllowSuperUsers() {
		BatchJob batchJob = reportBatchJob(null, null);
		user.superUser = true;

		assertTrue(BatchJobArtifactController.canAccessArtifact(user, batchJob, null));
	}

	@Test
	public void canAccessArtifact_shouldRejectUnscopedReportJobs() {
		BatchJob batchJob = reportBatchJob(null, null);

		assertFalse(BatchJobArtifactController.canAccessArtifact(user, batchJob, null));
	}

	@Test
	public void canAccessArtifact_shouldUseGlobalPrivilegeForReportsWithoutALocation() {
		BatchJob batchJob = reportBatchJob(Privileges.APP_STOCKMANAGEMENT_REPORTS, null);
		AtomicBoolean contextPrivilegeChecked = new AtomicBoolean();
		UserContext userContext = new UserContext(null) {

			@Override
			public boolean hasPrivilege(String privilege) {
				assertEquals(Privileges.APP_STOCKMANAGEMENT_REPORTS, privilege);
				contextPrivilegeChecked.set(true);
				return true;
			}
		};
		Context.setUserContext(userContext);

		try {
			assertTrue(BatchJobArtifactController.canAccessArtifact(user, batchJob, null));
			assertTrue(contextPrivilegeChecked.get());
		}
		finally {
			Context.clearUserContext();
		}
	}

	@Test
	public void canAccessArtifact_shouldUseLocationScopedPrivilegeForReportsWithALocation() {
		Location location = new Location();
		BatchJob batchJob = reportBatchJob(Privileges.APP_STOCKMANAGEMENT_REPORTS, location);
		AtomicBoolean privilegeChecked = new AtomicBoolean();
		StockManagementService stockManagementService = (StockManagementService) Proxy.newProxyInstance(
		    StockManagementService.class.getClassLoader(), new Class<?>[] { StockManagementService.class },
		    (proxy, method, args) -> {
			    if (method.getName().equals("userHasStockManagementPrivilege")) {
				    assertEquals(user, args[0]);
				    assertEquals(location, args[1]);
				    assertEquals(Privileges.APP_STOCKMANAGEMENT_REPORTS, args[3]);
				    privilegeChecked.set(true);
				    return true;
			    }
			    return null;
		    });

		assertTrue(BatchJobArtifactController.canAccessArtifact(user, batchJob, stockManagementService));
		assertTrue(privilegeChecked.get());
	}

	@Test
	public void canAccessArtifact_shouldPreserveAccessForUnscopedNonReportJobs() {
		BatchJob batchJob = new BatchJob();
		batchJob.setBatchJobType(BatchJobType.Other);

		assertTrue(BatchJobArtifactController.canAccessArtifact(user, batchJob, null));
	}

	private BatchJob reportBatchJob(String privilegeScope, Location locationScope) {
		BatchJob batchJob = new BatchJob();
		batchJob.setBatchJobType(BatchJobType.Report);
		batchJob.setPrivilegeScope(privilegeScope);
		batchJob.setLocationScope(locationScope);
		return batchJob;
	}

	private static class TestUser extends User {

		private boolean superUser;

		@Override
		public boolean isSuperUser() {
			return superUser;
		}
	}
}
