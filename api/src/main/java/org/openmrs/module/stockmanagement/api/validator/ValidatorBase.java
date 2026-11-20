/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.validator;

import org.openmrs.util.OpenmrsUtil;
import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import java.util.Date;

public abstract class ValidatorBase implements Validator {
	
	/**
	 * Rejects a date if it is in the future.
	 * 
	 * @param errors the error object
	 * @param date the date to check
	 * @param dateField the name of the field
	 */
	private void rejectIfFutureDate(Errors errors, Date date, String dateField) {
		if (OpenmrsUtil.compare(date, new Date()) > 0) {
			errors.rejectValue(dateField, "error.date.future");
		}
	}
}
