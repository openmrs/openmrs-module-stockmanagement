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
import org.openmrs.module.stockmanagement.api.utils.Action2;
import org.openmrs.module.stockmanagement.api.model.ReservedTransaction;
import org.openmrs.module.stockmanagement.api.model.StockItemTransaction;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;

import java.math.BigDecimal;

/**
 * Model class that processes a transfer stock operation type. Transfers move item stock from one
 * location to another.
 */
public class StockIssueOperationTypeProcessor extends StockOperationTypeProcessorBase {
	
	public StockIssueOperationTypeProcessor(StockOperationType stockOperationType) {
		super(stockOperationType);
	}
	
	@Override
	public String getAcronym() {
		return "SIS";
	}
	
	@Override
	public LocationType getDestinationType() {
		return LocationType.Location;
	}
	
	@Override
	public boolean isNegativeItemQuantityAllowed() {
		return false;
	}
	
	@Override
	public boolean canBeRelatedToRequisition() {
		return true;
	}
	
	@Override
	public boolean shouldVerifyNegativeStockAmountsAtSource() {
		return true;
	}
	
	public boolean requiresDispatchAcknowledgement() {
		return true;
	}
	
	@Override
	public BigDecimal getQuantityToApplyAtSource(BigDecimal quantity) {
		return quantity.multiply(BigDecimal.valueOf(-1));
	}
	
	@Override
	public void onPending(final StockOperation operation) {
		// Remove the item stock from the source party
		executeCopyReserved(operation, new Action2<ReservedTransaction, StockItemTransaction>() {
			
			@Override
			public void apply(ReservedTransaction reserved, StockItemTransaction tx) {
				tx.setParty(operation.getSource());
				
				// Negate the quantity because the item stock needs to
				// be removed from the source party
				tx.setQuantity(getQuantityToApplyAtSource(tx.getQuantity()));
			}
		});
	}
	
	@Override
	public void onCancelled(final StockOperation operation) {
		// Re-add the previously removed item stock back into the source
		// party
		executeCopyReservedAndClear(operation, new Action2<ReservedTransaction, StockItemTransaction>() {
			
			@Override
			public void apply(ReservedTransaction reserved, StockItemTransaction tx) {
				tx.setParty(operation.getSource());
			}
		});
	}
	
	@Override
	public void onCompleted(final StockOperation operation) {
		// Add the item stock to the destination party
		executeCopyReservedAndClear(operation, new Action2<ReservedTransaction, StockItemTransaction>() {
			
			@Override
			public void apply(ReservedTransaction reserved, StockItemTransaction tx) {
				tx.setQuantity(reserved.getStockOperationItem().getQuantityReceived());
				tx.setStockItemPackagingUOM(reserved.getStockOperationItem().getQuantityReceivedPackagingUOM());
				tx.setParty(operation.getDestination());
			}
		});
	}
}
