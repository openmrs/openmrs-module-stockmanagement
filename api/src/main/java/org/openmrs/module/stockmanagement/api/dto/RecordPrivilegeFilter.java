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

import org.openmrs.module.stockmanagement.api.model.StockOperationType;

public class RecordPrivilegeFilter {
	
	private Integer locationId;
	
	private StockOperationType operationType;
	
	public Integer getLocationId() {
		return locationId;
	}
	
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}
	
	public StockOperationType getOperationType() {
		return operationType;
	}
	
	public void setOperationType(StockOperationType operationType) {
		this.operationType = operationType;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		
		RecordPrivilegeFilter that = (RecordPrivilegeFilter) o;
		
		if (locationId != null ? !locationId.equals(that.locationId) : that.locationId != null)
			return false;
		return operationType == that.operationType;
		
	}
	
	@Override
	public int hashCode() {
		int result = locationId != null ? locationId.hashCode() : 0;
		result = 31 * result + (operationType != null ? operationType.hashCode() : 0);
		return result;
	}
}
