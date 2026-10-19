package org.openmrs.module.stockmanagement.web.resource;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the error translation in {@link StockOperationResource#save(StockOperationDTO)}:
 * business exceptions must never surface with a null message, unexpected exceptions must never
 * leak their details to the client, and the original cause must survive for troubleshooting.
 */
public class StockOperationResourceTest {

	private StockOperationResource resource;

	private StockManagementService service;

	@Before
	public void setup() {
		// bypass validateStockOperation: it needs a live OpenMRS context (covered by the
		// StockOperationDTOValidatorTest integration tests in the api module)
		resource = new StockOperationResource() {

			@Override
			protected void validateStockOperation(StockOperationDTO delegate) {
				// no-op
			}
		};
		service = mock(StockManagementService.class);
		resource.setStockManagementService(service);
	}

	@Test
	public void save_shouldReturnFallbackMessage_whenBusinessExceptionHasNullMessage() {
		when(service.saveStockOperation(any(StockOperationDTO.class)))
		        .thenThrow(new StockManagementException((String) null));

		try {
			resource.save(new StockOperationDTO());
		} catch (IllegalRequestException exception) {
			assertNotNull("REST errors must never carry a null message", exception.getMessage());
			assertEquals("Unable to save stock operation", exception.getMessage());
			assertNotNull("The original exception must be preserved as the cause", exception.getCause());
			assertSame(StockManagementException.class, exception.getCause().getClass());
		}
	}

	@Test
	public void save_shouldPreserveBusinessExceptionMessage() {
		when(service.saveStockOperation(any(StockOperationDTO.class)))
		        .thenThrow(new StockManagementException("Stock operation requires a destination"));

		try {
			resource.save(new StockOperationDTO());
		} catch (IllegalRequestException exception) {
			assertEquals("Stock operation requires a destination", exception.getMessage());
			assertNotNull(exception.getCause());
		}
	}

	@Test
	public void save_shouldReturnSafeGenericError_whenUnexpectedExceptionOccurs() {
		NullPointerException original = new NullPointerException();
		when(service.saveStockOperation(any(StockOperationDTO.class))).thenThrow(original);

		try {
			resource.save(new StockOperationDTO());
		} catch (GenericRestException exception) {
			// unexpected failures must expose a safe generic message only
			assertEquals("Unable to save stock operation", exception.getMessage());
			assertNotNull(exception.getCause());
			assertSame(original, exception.getCause());
		}
	}
}
