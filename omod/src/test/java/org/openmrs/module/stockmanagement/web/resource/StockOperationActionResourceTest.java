package org.openmrs.module.stockmanagement.web.resource;

import org.apache.commons.lang.StringUtils;
import org.openmrs.annotation.Handler;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.validator.StockOperationActionValidator;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.StockOperationAction;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationSearchFilter;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StockOperationActionResourceTest extends BaseModuleWebContextSensitiveTest {

	private StockManagementService service;
	private StockOperationActionResource resource;

	@Before
	public void setUp() throws Exception {
		initializeInMemoryDatabase();
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
				catch (IllegalRequestException exception) {
					assertEquals("Stock operation UUID is required", exception.getMessage());
				}
			}
		}
		verifyNoInteractions(service);
	}

	@Test
	public void shouldRejectMissingActionNameBeforeLookingUpAnOperation() {
		try {
			resource.save(action(null, "existing-operation"));
			fail("Expected a missing action name to be rejected");
		}
		catch (IllegalRequestException exception) {
			assertEquals("Stock operation action is required", exception.getMessage());
		}
		verifyNoInteractions(service);
	}

	@Test
	public void shouldReturnBadRequestUsingTheDeclaredRestVersion() throws Exception {
		try {
			resource.save(action(StockOperationAction.Action.SUBMIT, null));
			fail("Expected a missing UUID to be rejected");
		}
		catch (IllegalRequestException exception) {
			// Stack-trace diagnostics read server global properties, unrelated to this HTTP status check.
			exception.setStackTrace(new StackTraceElement[0]);
			HttpServletResponse response = mock(HttpServletResponse.class);
			new BaseRestController().handleException(exception, mock(HttpServletRequest.class), response);
			verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
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

	@Test
	public void save_shouldRejectBlankReasonsBeforeLookingUpOperations() {
		for (StockOperationAction.Action name : reasonActions()) {
			for (String reason : new String[] { null, "", " \t " }) {
				StockOperationAction action = newAction(name);
				action.setReason(reason);

				ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(action));

				assertNotNull(exception.getErrors().getFieldError("reason"));
			}
		}
		verifyNoInteractions(service);
	}

	@Test
	public void save_shouldRejectOverlongReasonsBeforeLookingUpOperations() {
		for (StockOperationAction.Action name : reasonActions()) {
			StockOperationAction action = newAction(name);
			action.setReason(StringUtils.repeat("a", 501));

			ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(action));

			assertNotNull(exception.getErrors().getFieldError("reason"));
			assertEquals("Reason should not exceed 500 characters", exception.getErrors().getFieldError("reason").getCode());
		}
		verifyNoInteractions(service);
	}

	@Test
	public void save_shouldAllowAReasonOf500Characters() {
		StockOperationAction action = newAction(StockOperationAction.Action.CANCEL);
		action.setReason(StringUtils.repeat("a", 500));
		StockOperationDTO operation = new StockOperationDTO();
		when(service.findStockOperations(any(StockOperationSearchFilter.class)))
		        .thenReturn(new Result<>(Collections.singletonList(operation), 1));

		assertSame(action, resource.save(action));

		verify(service).cancelStockOperation(operation, action.getReason());
	}

	@Test
	public void save_shouldReturnBadRequestForBusinessFailuresWithASafeMessage() throws Exception {
		StockOperationDTO operation = new StockOperationDTO();
		when(service.findStockOperations(any(StockOperationSearchFilter.class)))
		        .thenReturn(new Result<>(Collections.singletonList(operation), 1));
		for (String message : Arrays.asList("Insufficient stock", null, "", " ")) {
			StockOperationAction action = newAction(StockOperationAction.Action.CANCEL);
			action.setReason("Cancellation requested");
			doThrow(new StockManagementException(message)).when(service)
			        .cancelStockOperation(operation, action.getReason());

			IllegalRequestException exception = assertThrows(IllegalRequestException.class, () -> resource.save(action));

			assertEquals(message == null || message.trim().isEmpty()
			        ? "Unable to process the stock operation action" : message, exception.getMessage());
			assertNull(exception.getCause());
			exception.setStackTrace(new StackTraceElement[0]);
			HttpServletResponse response = mock(HttpServletResponse.class);
			new BaseRestController().handleException(exception, mock(HttpServletRequest.class), response);
			verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@Test
	public void validator_shouldRegisterForStockOperationActions() {
		Handler handler = StockOperationActionValidator.class.getAnnotation(Handler.class);

		assertEquals(StockOperationAction.class, handler.supports()[0]);
		assertTrue(new StockOperationActionValidator().supports(StockOperationAction.class));
	}

	private StockOperationAction newAction(StockOperationAction.Action name) {
		StockOperationAction action = new StockOperationAction();
		action.setName(name);
		action.setUuid("some-stock-operation-uuid");
		return action;
	}

	private StockOperationAction.Action[] reasonActions() {
		return new StockOperationAction.Action[] { StockOperationAction.Action.REJECT,
		        StockOperationAction.Action.RETURN, StockOperationAction.Action.CANCEL };
	}
	private StockOperationAction action(StockOperationAction.Action name, String uuid) {
		StockOperationAction action = new StockOperationAction();
		action.setName(name);
		action.setUuid(uuid);
		return action;
	}
}
