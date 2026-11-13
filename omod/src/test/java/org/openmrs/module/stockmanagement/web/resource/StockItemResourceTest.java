package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.module.stockmanagement.api.dto.StockItemDTO;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

public class StockItemResourceTest extends BaseModuleWebContextSensitiveTest {

	private final StockItemResource resource = new StockItemResource();

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}

	@Test
	public void save_shouldRejectAReorderLevelWithoutAUnit() {
		StockItemDTO dto = newStockItem();
		dto.setReorderLevel(BigDecimal.ONE);

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));

		assertNotNull(exception.getErrors().getFieldError("reorderLevel"));
	}

	@Test
	public void save_shouldRejectANegativeReorderLevel() {
		StockItemDTO dto = newStockItem();
		dto.setReorderLevel(BigDecimal.ONE.negate());
		dto.setReorderLevelUoMUuid("some-packaging-uom-uuid");

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));

		assertNotNull(exception.getErrors().getFieldError("reorderLevel"));
	}

	@Test
	public void save_shouldReturnBadRequestForBusinessFailuresWithASafeMessage() throws Exception {
		StockManagementService service = mock(StockManagementService.class);
		resource.setStockManagementService(service);
		for (String message : Arrays.asList("Invalid stock item", null, "", " ")) {
			doThrow(new StockManagementException(message)).when(service).saveStockItem(any(StockItemDTO.class));

			IllegalRequestException exception = assertThrows(IllegalRequestException.class,
			    () -> resource.save(newStockItem()));

			assertEquals(message == null || message.trim().isEmpty() ? "Unable to save stock item" : message,
			    exception.getMessage());
			assertNull(exception.getCause());
			exception.setStackTrace(new StackTraceElement[0]);
			HttpServletResponse response = mock(HttpServletResponse.class);
			new BaseRestController().handleException(exception, mock(HttpServletRequest.class), response);
			verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private StockItemDTO newStockItem() {
		StockItemDTO dto = new StockItemDTO();
		dto.setConceptUuid("some-concept-uuid");
		dto.setHasExpiration(false);
		return dto;
	}
}
