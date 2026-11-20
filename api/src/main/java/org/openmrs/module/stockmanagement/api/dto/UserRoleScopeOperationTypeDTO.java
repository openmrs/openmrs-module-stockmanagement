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

public class UserRoleScopeOperationTypeDTO {
	
	private Integer userRoleScopeId;
	
	private String uuid;
	
	private String operationTypeName;
	
	private String operationTypeUuid;
	
	public Integer getUserRoleScopeId() {
		return userRoleScopeId;
	}
	
	public void setUserRoleScopeId(Integer userRoleScopeId) {
		this.userRoleScopeId = userRoleScopeId;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getOperationTypeName() {
		return operationTypeName;
	}
	
	public void setOperationTypeName(String operationTypeName) {
		this.operationTypeName = operationTypeName;
	}
	
	public String getOperationTypeUuid() {
		return operationTypeUuid;
	}
	
	public void setOperationTypeUuid(String operationTypeUuid) {
		this.operationTypeUuid = operationTypeUuid;
	}
}
