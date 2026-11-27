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

import org.openmrs.module.stockmanagement.api.model.LocationType;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;

import java.math.BigDecimal;

/**
 * Model class that processes a transfer stock operation type. Transfers move item stock from one
 * location to another.
 */
public class RequisitionOperationTypeProcessor extends StockOperationTypeProcessorBase {
	
	public RequisitionOperationTypeProcessor(StockOperationType stockOperationType) {
		super(stockOperationType);
	}
	
	@Override
	public String getAcronym() {
		return "REQ";
	}
	
	@Override
	public LocationType getDestinationType() {
		return LocationType.Location;
	}
	
	@Override
	public boolean getAvailableWhenReserved() {
		return false;
	}
	
	@Override
	public boolean isNegativeItemQuantityAllowed() {
		return false;
	}
	
	@Override
	public boolean requiresActualBatchInformation() {
		return false;
	}
	
	@Override
	public boolean requiresBatchUuid() {
		return false;
	}
	
	@Override
	public boolean isQuantityOptional() {
		return true;
	}
	
	@Override
	public boolean shouldVerifyNegativeStockAmountsAtSource() {
		return false;
	}
	
	@Override
	public BigDecimal getQuantityToApplyAtSource(BigDecimal quantity) {
		return BigDecimal.valueOf(0);
	}
	
	@Override
	public void onPending(final StockOperation operation) {
		// Remove the item stock from the source stockroom
		if (operation.getReservedTransactions() != null) {
			clearReservedTransactions(operation);
		}
	}
	
	@Override
	public void onCancelled(final StockOperation operation) {
		// Re-add the previously removed item stock back into the source
		// stockroom
		if (operation.getReservedTransactions() != null) {
			clearReservedTransactions(operation);
		}
	}
	
	@Override
	public void onCompleted(final StockOperation operation) {
		if (operation.getReservedTransactions() != null) {
			clearReservedTransactions(operation);
		}
	}
}
