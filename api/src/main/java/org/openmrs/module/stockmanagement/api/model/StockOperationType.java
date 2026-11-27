/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.model;

import java.util.Arrays;
import java.util.Locale;

import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.StockOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.AdjustmentOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.DisposedOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.InitialOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.ReceiptOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.RequisitionOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.ReturnOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.StockIssueOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.StockTakeOperationTypeProcessor;
import org.openmrs.module.stockmanagement.api.impl.TransferOutOperationTypeProcessor;

/**
 * The kinds of stock operation. The constant name is stored in the database and the REST code is
 * derived from it, so renaming a constant needs a data migration and a client change.
 */
public enum StockOperationType {
	
	ADJUSTMENT,
	DISPOSED,
	TRANSFER_OUT,
	RECEIPT,
	RETURN,
	STOCK_ISSUE,
	REQUISITION,
	STOCKTAKE,
	INITIAL;
	
	public static StockOperationType fromOperationType(String operationType) {
		return Arrays.stream(values()).filter(type -> type.getOperationType().equals(operationType)).findFirst()
		        .orElse(null);
	}
	
	/** @return the code REST clients use, for example "transferout" */
	public String getOperationType() {
		return name().toLowerCase(Locale.ROOT).replace("_", "");
	}
	
	public String getName() {
		return Context.getMessageSourceService().getMessage("stockmanagement.operationtype." + getOperationType());
	}
	
	// TODO O3-6321: make the processors Spring beans looked up by type
	public StockOperationTypeProcessor getProcessor() {
		switch (this) {
			case ADJUSTMENT:
				return new AdjustmentOperationTypeProcessor(this);
			case DISPOSED:
				return new DisposedOperationTypeProcessor(this);
			case TRANSFER_OUT:
				return new TransferOutOperationTypeProcessor(this);
			case RECEIPT:
				return new ReceiptOperationTypeProcessor(this);
			case RETURN:
				return new ReturnOperationTypeProcessor(this);
			case STOCK_ISSUE:
				return new StockIssueOperationTypeProcessor(this);
			case REQUISITION:
				return new RequisitionOperationTypeProcessor(this);
			case STOCKTAKE:
				return new StockTakeOperationTypeProcessor(this);
			case INITIAL:
				return new InitialOperationTypeProcessor(this);
			default:
				throw new IllegalStateException("No processor for operation type " + this);
		}
	}
}
