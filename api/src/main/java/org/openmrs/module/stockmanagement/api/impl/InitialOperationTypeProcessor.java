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
import org.openmrs.module.stockmanagement.api.model.ReservedTransaction;
import org.openmrs.module.stockmanagement.api.model.StockItemTransaction;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;

import java.math.BigDecimal;

/**
 * The initial operation type is used to populate a stockroom with the initial set of item stock at
 * the time when the system is first set up. It is currently implemented as a distinct class that
 * inherits the behavior of the receipt operation type however it may need to add it's own
 * implementation if we extend the behavior of receipt operations.
 */
public class InitialOperationTypeProcessor extends StockOperationTypeProcessorBase {
	
	public InitialOperationTypeProcessor(StockOperationType stockOperationType) {
		super(stockOperationType);
	}
	
	@Override
	public String getAcronym() {
		return "IST";
	}
	
	@Override
	public boolean getAvailableWhenReserved() {
		return false;
	}
	
	@Override
	public boolean getAllowBatchInfoUpdate() {
		return true;
	}
	
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
	public void onPending(StockOperation operation) {
	}
	
	@Override
	public BigDecimal getQuantityToApplyAtSource(BigDecimal quantity) {
		return BigDecimal.valueOf(0);
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
				tx.setParty(operation.getSource());
			}
		});
	}
}
