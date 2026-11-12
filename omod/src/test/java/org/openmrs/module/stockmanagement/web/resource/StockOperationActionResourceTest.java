package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.annotation.Handler;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.StockOperationAction;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.validator.StockOperationActionValidator;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

public class StockOperationActionResourceTest extends BaseModuleWebContextSensitiveTest {

	private final StockOperationActionResource resource = new StockOperationActionResource();

	private StockManagementService service;

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		service = mock(StockManagementService.class);
		resource.setStockManagementService(service);
	}

	@Test
	public void save_shouldRejectMissingOrBlankUuidBeforeLookingUpOperations() {
		for (String uuid : new String[] { null, "", " \t " }) {
			StockOperationAction action = newAction(StockOperationAction.Action.SUBMIT);
			action.setUuid(uuid);

			ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(action));

			assertNotNull(exception.getErrors().getFieldError("uuid"));
			assertEquals(Context.getMessageSourceService().getMessage("stockmanagement.stockoperation.action.uuidrequired"),
			    exception.getErrors().getFieldError("uuid").getCode());
		}
		verifyNoInteractions(service);
	}

	@Test
	public void save_shouldRejectMissingActionBeforeLookingUpOperations() {
		StockOperationAction action = newAction(null);

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(action));

		assertNotNull(exception.getErrors().getFieldError("name"));
		verifyNoInteractions(service);
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
}
