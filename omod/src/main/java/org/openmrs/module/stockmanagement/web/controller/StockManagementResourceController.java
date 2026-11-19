package org.openmrs.module.stockmanagement.web.controller;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID)
public class StockManagementResourceController extends MainResourceController {
	
	/**
	 * @see org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController#getNamespace()
	 */
	@Override
	public String getNamespace() {
		return "v1/stockmanagement";
	}

	@ExceptionHandler(StockManagementException.class)
    @ResponseBody
    public SimpleObject stockManagementExceptionHandler(
            StockManagementException exception,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        if (RestUtil.hasCause(exception, APIAuthenticationException.class)) {
            return handleException(exception, request, response);
        }

        String message = StringUtils.isBlank(exception.getMessage())
                ? "Invalid request"
                : exception.getMessage();

        return handleException(new IllegalRequestException(message), request, response);
    }
}
