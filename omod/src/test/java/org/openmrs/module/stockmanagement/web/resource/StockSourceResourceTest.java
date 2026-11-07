package org.openmrs.module.stockmanagement.web.resource;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StockSourceResourceTest {

	private StockManagementService service;

	private StockSourceResource resource;

	@Before
	public void setUp() {
		service = mock(StockManagementService.class);
		resource = new StockSourceResource() {
			@Override
			protected StockManagementService getStockManagementService() {
				return service;
			}
		};
	}

	@Test
	public void save_shouldReturnABadRequestWhenTheServiceRejectsADuplicate() {
		StockSource stockSource = new StockSource();
		when(service.saveStockSource(stockSource))
		        .thenThrow(new StockManagementException("A stock source named \"Watsons\" already exists"));

		try {
			resource.save(stockSource);
			fail("Expected the duplicate stock source to be rejected");
		}
		catch (IllegalRequestException exception) {
			assertEquals("A stock source named \"Watsons\" already exists", exception.getMessage());
		}
	}

	@Test
	public void save_shouldReturnTheSavedStockSource() {
		StockSource stockSource = new StockSource();
		when(service.saveStockSource(stockSource)).thenReturn(stockSource);

		assertSame(stockSource, resource.save(stockSource));
	}
}
