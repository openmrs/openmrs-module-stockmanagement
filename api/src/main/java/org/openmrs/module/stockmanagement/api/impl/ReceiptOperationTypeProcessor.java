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

import java.util.function.BiConsumer;
import org.openmrs.module.stockmanagement.api.dto.StockOperationAction;
import org.openmrs.module.stockmanagement.api.model.*;

import java.math.BigDecimal;

/**
 * Model class that represents a receipt stock operation type. Receipt operations bring new item
 * stock into the system.
 */

public class ReceiptOperationTypeProcessor extends StockOperationTypeProcessorBase {
	
	public ReceiptOperationTypeProcessor(StockOperationType stockOperationType) {
		super(stockOperationType);
	}
	
	@Override
	public String getAcronym() {
		return "RCPT";
	}
	
	@Override
	public LocationType getSourceType() {
		return LocationType.Other;
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
	public boolean getAllowBatchInfoUpdate() {
		return true;
	}
	
	@Override
	public boolean notifiesOn(StockOperationAction.Action action) {
		return action == StockOperationAction.Action.SUBMIT || action == StockOperationAction.Action.APPROVE
		        || action == StockOperationAction.Action.COMPLETE;
	}
	
	@Override
	public boolean isNegativeItemQuantityAllowed() {
		return false;
	}
	
	@Override
	public boolean requiresActualBatchInformation() {
		return true;
	}
	
	@Override
	public boolean requiresBatchUuid() {
		return false;
	}
	
	@Override
	public boolean canCapturePurchasePrice() {
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
	public void onPending(StockOperation operation) {
	}
	
	@Override
	public void onCancelled(StockOperation operation) {
		// Clear out the transactions for the operation
		clearReservedTransactions(operation);
	}
	
	@Override
	public void onCompleted(final StockOperation operation) {
		executeCopyReservedAndClear(operation, new BiConsumer<ReservedTransaction, StockItemTransaction>() {
			
			@Override
			public void accept(ReservedTransaction reserved, StockItemTransaction tx) {
				tx.setParty(operation.getDestination());
			}
		});
	}
}
