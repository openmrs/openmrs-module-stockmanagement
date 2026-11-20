/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.impl;

import org.openmrs.module.stockmanagement.api.model.StockOperationType;

/**
 * A disposed operation is for expired items that must be removed from circulation. While it could
 * technically be implemented as a form of an adjustment operation, it is notable enough (for
 * reporting) to warrant it's own type.
 */
public class DisposedOperationTypeProcessor extends AdjustmentOperationTypeProcessor {
	
	public DisposedOperationTypeProcessor(StockOperationType stockOperationType) {
		super(stockOperationType);
	}
	
	@Override
	protected boolean negateAppliedQuantity() {
		return true;
	}
	
	@Override
	public boolean isNegativeItemQuantityAllowed() {
		return false;
	}
}
