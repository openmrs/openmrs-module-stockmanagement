package org.openmrs.module.stockmanagement.web.resource;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.webservices.rest.SimpleObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StockOperationResourceTest {

	private static final String OPERATION_UUID = "operation-uuid";

	private StockManagementService service;

	private StockOperationResource resource;

	@Before
	public void setUp() {
		StockOperationType operationType = new StockOperationType();
		operationType.setOperationType(StockOperationType.STOCK_ISSUE);
		operationType.setAllowBatchInfoUpdate(false);
		service = mock(StockManagementService.class);
		when(service.getStockOperationTypeByUuid(any())).thenReturn(operationType);

		resource = new StockOperationResource() {
			@Override
			protected StockManagementService getStockManagementService() {
				return service;
			}
		};
	}

	@Test
	public void getPermission_shouldReflectTheCurrentStatusOfTheOperation() {
		SimpleObject dispatched = resource.getPermission(operation(StockOperationStatus.DISPATCHED));
		assertEquals(true, dispatched.get("canDisplayReceivedItems"));

		SimpleObject completed = resource.getPermission(operation(StockOperationStatus.COMPLETED));
		assertEquals(false, completed.get("canDisplayReceivedItems"));
	}

	@Test
	public void getPermission_shouldBeComputedAgainForEachRequest() {
		StockOperationDTO operation = operation(StockOperationStatus.DISPATCHED);

		assertNotSame(resource.getPermission(operation), resource.getPermission(operation));
	}

	// A locked operation that isn't SUBMITTED can be neither edited nor approved, so
	// getPermission doesn't look up the authenticated user's role scopes for it.
	private static StockOperationDTO operation(StockOperationStatus status) {
		StockOperationDTO operation = new StockOperationDTO();
		operation.setUuid(OPERATION_UUID);
		operation.setOperationTypeUuid("operation-type-uuid");
		operation.setOperationType(StockOperationType.STOCK_ISSUE);
		operation.setStatus(status);
		operation.setLocked(true);
		operation.setVoided(false);
		return operation;
	}
}
