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
 * Model class that represents an adjustment stock operation type. Adjustment operations directly
 * alter the item stock for a given stockroom and are used to correct item stock mistakes.
 */
public class AdjustmentOperationTypeProcessor extends StockOperationTypeProcessorBase {
	
	public AdjustmentOperationTypeProcessor(StockOperationType stockOperationType) {
		super(stockOperationType);
	}
	
	@Override
	public boolean requiresReason() {
		return true;
	}
	
	/**
	 * Specifies whether the quantity should be negated when it is applied. This allows sub-classes to
	 * change the default adjustment behavior.
	 * 
	 * @return
	 */
	protected boolean negateAppliedQuantity() {
		// Note that the quantity is NOT negated because the adjustment quantity
		// is the difference
		return false;
	}
	
	@Override
	public boolean isNegativeItemQuantityAllowed() {
		return true;
	}
	
	@Override
	public boolean shouldVerifyNegativeStockAmountsAtSource() {
		return true;
	}
	
	@Override
	public BigDecimal getQuantityToApplyAtSource(BigDecimal quantity) {
		if (negateAppliedQuantity()) {
			return quantity.multiply(BigDecimal.valueOf(-1));
		}
		return quantity;
	}
	
	@Override
	public void onPending(final StockOperation operation) {
		executeCopyReserved(operation, new BiConsumer<ReservedTransaction, StockItemTransaction>() {
			
			@Override
			public void accept(ReservedTransaction reserved, StockItemTransaction tx) {
				tx.setParty(operation.getSource());
				if (negateAppliedQuantity()) {
					tx.setQuantity(getQuantityToApplyAtSource(tx.getQuantity()));
				}
			}
		});
	}
	
	@Override
	public void onCancelled(final StockOperation operation) {
		executeCopyReservedAndClear(operation, new BiConsumer<ReservedTransaction, StockItemTransaction>() {
			
			@Override
			public void accept(ReservedTransaction reserved, StockItemTransaction tx) {
				tx.setParty(operation.getSource());
				
				// Undo the previously applied transaction by setting
				// the quantity to the opposite of the pending
				// transaction
				if (!negateAppliedQuantity()) {
					tx.setQuantity(tx.getQuantity().multiply(BigDecimal.valueOf(-1)));
				}
			}
		});
	}
	
	@Override
	public void onCompleted(StockOperation operation) {
		// Clear out the transactions for the operation
		clearReservedTransactions(operation);
	}
}
