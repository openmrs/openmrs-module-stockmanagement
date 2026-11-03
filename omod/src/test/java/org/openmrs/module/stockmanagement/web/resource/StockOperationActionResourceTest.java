package org.openmrs.module.stockmanagement.web.resource;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.StockOperationAction;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationSearchFilter;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StockOperationActionResourceTest {

	private StockManagementService service;
	private StockOperationActionResource resource;

	@Before
	public void setUp() {
		service = mock(StockManagementService.class);
		resource = new StockOperationActionResource() {
			@Override
			protected StockManagementService getStockManagementService() {
				return service;
			}
		};
	}

	@Test
	public void shouldRejectBlankUuidForEveryActionBeforeLookingUpAnOperation() {
		when(service.findStockOperations(any(StockOperationSearchFilter.class)))
		        .thenReturn(new Result<>(Arrays.asList(new StockOperationDTO(), new StockOperationDTO()), 2));
		for (StockOperationAction.Action name : StockOperationAction.Action.values()) {
			for (String uuid : Arrays.asList(null, "", " ", "\t", "\r\n")) {
				StockOperationAction action = action(name, uuid);
				try {
					resource.save(action);
					fail("Expected validation failure for " + name + " with UUID [" + uuid + "]");
				}
				catch (ValidationException exception) {
					assertNotNull(exception.getErrors().getFieldError("uuid"));
				}
			}
		}
		verifyNoInteractions(service);
	}

	@Test
	public void shouldSubmitOnlyTheRequestedOperationWhenAnotherOperationIsNewer() {
		StockOperationDTO older = new StockOperationDTO();
		older.setUuid("older-operation");
		StockOperationDTO newer = new StockOperationDTO();
		newer.setUuid("newer-operation");
		when(service.findStockOperations(any(StockOperationSearchFilter.class))).thenAnswer(invocation -> {
			StockOperationSearchFilter filter = (StockOperationSearchFilter) invocation.getArguments()[0];
			assertEquals(older.getUuid(), filter.getStockOperationUuid());
			return new Result<>(Collections.singletonList(older), 1);
		});
		StockOperationAction action = action(StockOperationAction.Action.SUBMIT, older.getUuid());

		assertSame(action, resource.save(action));

		verify(service).submitStockOperation(older);
		verify(service, never()).submitStockOperation(newer);
		verify(service).findStockOperations(any(StockOperationSearchFilter.class));
		verifyNoMoreInteractions(service);
	}

	@Test
	public void shouldNotActOnAnotherOperationWhenTheRequestedOperationDoesNotExist() {
		when(service.findStockOperations(any(StockOperationSearchFilter.class)))
		        .thenReturn(new Result<>(Collections.emptyList(), 0));
		try {
			resource.save(action(StockOperationAction.Action.SUBMIT, "unknown-operation"));
			fail("Expected an unknown operation to be rejected");
		}
		catch (org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException expected) {
			assertTrue(expected.getMessage().contains("not found"));
		}
		verify(service).findStockOperations(any(StockOperationSearchFilter.class));
		verifyNoMoreInteractions(service);
	}

	private StockOperationAction action(StockOperationAction.Action name, String uuid) {
		StockOperationAction action = new StockOperationAction();
		action.setName(name);
		action.setUuid(uuid);
		return action;
	}
}
