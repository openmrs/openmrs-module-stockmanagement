package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.web.controller.StockManagementResourceController;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

public class StockManagementResourceControllerTest extends BaseModuleWebContextSensitiveTest {

	private StockManagementResourceController controller;

	private HttpServletRequest request;

	private HttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		initializeInMemoryDatabase();
		controller = new StockManagementResourceController();
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
	}

	@Test
	public void stockManagementExceptionHandler_shouldReturnBadRequestWithTheBusinessRuleMessage()
	        throws Exception {
		SimpleObject result = controller.stockManagementExceptionHandler(
		    new StockManagementException("Insufficient stock"), request, response);

		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		assertEquals("Insufficient stock", error(result).get("rawMessage"));
		assertTrue(((String) error(result).get("message")).contains("Insufficient stock"));
	}

	@Test
	public void stockManagementExceptionHandler_shouldNotExposeTheCauseToTheClient()
	        throws Exception {
		StockManagementException exception = new StockManagementException(
		    "Insufficient stock",
		    new IllegalStateException("internal detail"));

		SimpleObject result = controller.stockManagementExceptionHandler(
		    exception, request, response);

		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		assertEquals("Insufficient stock", error(result).get("rawMessage"));
		assertFalse(((String) error(result).get("message")).contains("internal detail"));
	}

	@Test
	public void stockManagementExceptionHandler_shouldFallBackToAGenericMessageWhenTheExceptionHasNone()
	        throws Exception {
		for (String message : new String[] { null, "", "  " }) {
			HttpServletResponse perCallResponse = mock(HttpServletResponse.class);

			SimpleObject result = controller.stockManagementExceptionHandler(
			    new StockManagementException(message), request, perCallResponse);

			verify(perCallResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
			assertEquals("Invalid request", error(result).get("rawMessage"));
		}
	}

	@Test
	public void stockManagementExceptionHandler_shouldKeepAnAuthorizationFailureForbidden()
	        throws Exception {
		StockManagementException exception = new StockManagementException(
		    "Not allowed",
		    new APIAuthenticationException("Privileges required"));

		controller.stockManagementExceptionHandler(exception, request, response);

		verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	private Map<?, ?> error(SimpleObject result) {
		return (Map<?, ?>) result.get("error");
	}
}