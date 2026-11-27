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

import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;

public class StockOperationLinkDTO {
	
	private int id;
	
	private String uuid;
	
	private String parentUuid;
	
	private String parentOperationNumber;
	
	private StockOperationType parentOperationType;
	
	private StockOperationStatus parentStatus;
	
	private boolean parentVoided;
	
	private String childUuid;
	
	private String childOperationNumber;
	
	private StockOperationType childOperationType;
	
	private StockOperationStatus childStatus;
	
	private boolean childVoided;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getChildUuid() {
		return childUuid;
	}
	
	public void setChildUuid(String childUuid) {
		this.childUuid = childUuid;
	}
	
	public String getChildOperationNumber() {
		return childOperationNumber;
	}
	
	public void setChildOperationNumber(String childOperationNumber) {
		this.childOperationNumber = childOperationNumber;
	}
	
	public StockOperationStatus getChildStatus() {
		return childStatus;
	}
	
	public void setChildStatus(StockOperationStatus childStatus) {
		this.childStatus = childStatus;
	}
	
	public StockOperationStatus getParentStatus() {
		return parentStatus;
	}
	
	public void setParentStatus(StockOperationStatus parentStatus) {
		this.parentStatus = parentStatus;
	}
	
	public boolean getChildVoided() {
		return childVoided;
	}
	
	public void setChildVoided(boolean childVoided) {
		this.childVoided = childVoided;
	}
	
	public String getParentUuid() {
		return parentUuid;
	}
	
	public void setParentUuid(String parentUuid) {
		this.parentUuid = parentUuid;
	}
	
	public String getParentOperationNumber() {
		return parentOperationNumber;
	}
	
	public void setParentOperationNumber(String parentOperationNumber) {
		this.parentOperationNumber = parentOperationNumber;
	}
	
	public boolean getParentVoided() {
		return parentVoided;
	}
	
	public void setParentVoided(boolean parentVoided) {
		this.parentVoided = parentVoided;
	}
	
	public String getParentOperationTypeName() {
		return parentOperationType == null ? null : parentOperationType.getName();
	}
	
	public StockOperationType getParentOperationType() {
		return parentOperationType;
	}
	
	public void setParentOperationType(StockOperationType parentOperationType) {
		this.parentOperationType = parentOperationType;
	}
	
	public String getChildOperationTypeName() {
		return childOperationType == null ? null : childOperationType.getName();
	}
	
	public StockOperationType getChildOperationType() {
		return childOperationType;
	}
	
	public void setChildOperationType(StockOperationType childOperationType) {
		this.childOperationType = childOperationType;
	}
}
