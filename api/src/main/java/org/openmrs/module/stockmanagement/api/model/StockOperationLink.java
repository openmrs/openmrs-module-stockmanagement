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

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity(name = "stockmanagement.StockOperationLink")
@Table(name = "stockmgmt_stock_operation_link")
public class StockOperationLink extends org.openmrs.BaseChangeableOpenmrsData implements Serializable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stock_operation_link_id")
	private Integer id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_stock_operation_id")
	private StockOperation parent;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_stock_operation_id")
	private StockOperation child;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public StockOperation getParent() {
		return parent;
	}
	
	public void setParent(StockOperation parent) {
		this.parent = parent;
	}
	
	public StockOperation getChild() {
		return child;
	}
	
	public void setChild(StockOperation child) {
		this.child = child;
	}
}
