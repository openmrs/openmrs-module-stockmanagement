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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Model class that represents individual item stock actions while an operation is pending.
 */
@Entity(name = "stockmanagement.ReservedTransaction")
@Table(name = "stockmgmt_reservation_transaction")
public class ReservedTransaction extends TransactionBase {
	
	@Column(name = "is_available")
	private Boolean isAvailable;
	
	public ReservedTransaction() {
	}
	
	public ReservedTransaction(TransactionBase tx) {
		super(tx);
	}
	
	public ReservedTransaction(StockOperation stockOperation, StockOperationItem item) {
		super(stockOperation, item);
	}
	
	public Boolean getIsAvailable() {
		return isAvailable;
	}
	
	public void setIsAvailable(Boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
}
