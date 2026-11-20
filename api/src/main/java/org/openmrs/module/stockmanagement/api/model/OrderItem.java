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

/**
 * The persistent class for the stockmgmt_stock_item database table.
 */
@Entity(name = "stockmanagement.OrderItem")
@Table(name = "stockmgmt_order_item")
public class OrderItem extends org.openmrs.BaseChangeableOpenmrsData implements Serializable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_item_id")
	private Integer id;
	
	@JoinColumn(name = "order_id")
	@OneToOne(fetch = FetchType.LAZY)
	private Order order;
	
	@JoinColumn(name = "stock_item_id")
	@OneToOne(fetch = FetchType.LAZY)
	private StockItem stockItem;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stock_item_packaging_uom_id")
	private StockItemPackagingUOM stockItemPackagingUOM;
	
	@JoinColumn(name = "created_from")
	@OneToOne(fetch = FetchType.LAZY)
	private Location createdFrom;
	
	@JoinColumn(name = "fulfilment_location_id")
	@OneToOne(fetch = FetchType.LAZY)
	private Location fulfilmentLocation;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Order getOrder() {
		return order;
	}
	
	public void setOrder(Order order) {
		this.order = order;
	}
	
	public Location getCreatedFrom() {
		return createdFrom;
	}
	
	public void setCreatedFrom(Location createdFrom) {
		this.createdFrom = createdFrom;
	}
	
	public StockItem getStockItem() {
		return stockItem;
	}
	
	public void setStockItem(StockItem stockItem) {
		this.stockItem = stockItem;
	}
	
	public Location getFulfilmentLocation() {
		return fulfilmentLocation;
	}
	
	public void setFulfilmentLocation(Location fulfilmentLocation) {
		this.fulfilmentLocation = fulfilmentLocation;
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOM() {
		return stockItemPackagingUOM;
	}
	
	public void setStockItemPackagingUOM(StockItemPackagingUOM stockItemPackagingUOM) {
		this.stockItemPackagingUOM = stockItemPackagingUOM;
	}
}
