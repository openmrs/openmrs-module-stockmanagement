/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.dto;

import java.math.BigDecimal;

public class StockOperationActionLineItem {
	
	private String uuid;
	
	private BigDecimal amount;
	
	private String packagingUoMUuId;
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public String getPackagingUoMUuId() {
		return packagingUoMUuId;
	}
	
	public void setPackagingUoMUuId(String packagingUoMUuId) {
		this.packagingUoMUuId = packagingUoMUuId;
	}
}
