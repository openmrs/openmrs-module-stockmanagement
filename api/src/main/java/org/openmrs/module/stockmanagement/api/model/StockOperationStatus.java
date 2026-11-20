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

import java.util.Date;

/**
 * The allowable {@link StockOperation} statuses.
 */
public enum StockOperationStatus {
	
	/**
	 * The operation is being created but has not yet been submitted.
	 */
	NEW(),
	/**
	 * The operation has been submitted but not yet approved.
	 */
	SUBMITTED(),
	
	/**
	 * The operation has been dispatched but items not yet received.
	 */
	DISPATCHED(),
	/**
	 * The operation has been returned for modification.
	 */
	RETURNED(),
	
	/**
	 * The operation has been rejected by the approver.
	 */
	REJECTED(),
	/**
	 * The operation was cancelled and the pending transactions were reversed.
	 */
	CANCELLED(),
	/**
	 * The operation was completed and the pending transactions were applied.
	 */
	COMPLETED();
	
	StockOperationStatus() {
	}
	
	public static boolean IsUpdateable(StockOperationStatus stockOperationStatus) {
		return stockOperationStatus != null && (stockOperationStatus == NEW || stockOperationStatus == RETURNED);
	}
	
	public static boolean IsApproveable(StockOperationStatus stockOperationStatus) {
		return stockOperationStatus != null && (stockOperationStatus == SUBMITTED);
	}
	
	public static boolean canReceiveItems(StockOperationStatus stockOperationStatus) {
		return stockOperationStatus != null && (stockOperationStatus == DISPATCHED);
	}
	
	public static boolean canDisplayReceivedItems(StockOperationStatus stockOperationStatus,
	        Date stockOperationDispatchedDate) {
		return stockOperationStatus != null && (stockOperationStatus == DISPATCHED
		        || ((stockOperationStatus == RETURNED || stockOperationStatus == COMPLETED)
		                && stockOperationDispatchedDate != null)
		
		);
	}
	
	public static boolean isCompleted(StockOperationStatus stockOperationStatus) {
		return stockOperationStatus != null && stockOperationStatus == COMPLETED;
	}
	
	public static boolean canUpdateBatchInformation(StockOperationStatus stockOperationStatus) {
		return (!IsUpdateable(stockOperationStatus))
		        && (isCompleted(stockOperationStatus) || IsApproveable(stockOperationStatus));
	}
}
