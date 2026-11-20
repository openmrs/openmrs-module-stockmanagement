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

import org.openmrs.*;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * The persistent class for the stockmgmt_stock_item_transaction database table.
 */
@Entity(name = "stockmanagement.StockItemTransaction")
@Table(name = "stockmgmt_stock_item_transaction")
public class StockItemTransaction extends TransactionBase implements Serializable {
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = true)
	private Order order;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "encounter_id", nullable = true)
	private Encounter encounter;
	
	public StockItemTransaction() {
	}
	
	public StockItemTransaction(TransactionBase tx) {
		super(tx);
	}
	
	public StockItemTransaction(StockOperation stockOperation, StockOperationItem item) {
		super(stockOperation, item);
	}
	
	public Order getOrder() {
		return order;
	}
	
	public void setOrder(Order order) {
		this.order = order;
	}
	
	public Encounter getEncounter() {
		return encounter;
	}
	
	public void setEncounter(Encounter encounter) {
		this.encounter = encounter;
	}
}
