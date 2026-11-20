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

import org.openmrs.module.stockmanagement.api.model.UserRoleScope;

import java.util.List;

public class UserRoleScopeLocationSearchFilter {
	
	private String uuid;
	
	private List<UserRoleScope> userRoleScopes;
	
	private boolean includeVoided;
	
	private Integer startIndex;
	
	private Integer limit;
	
	public UserRoleScopeLocationSearchFilter() {
		includeVoided = false;
	}
	
	public UserRoleScopeLocationSearchFilter(List<UserRoleScope> userRoleScopes) {
		this.userRoleScopes = userRoleScopes;
		includeVoided = false;
	}
	
	public boolean getIncludeVoided() {
		return includeVoided;
	}
	
	public void setIncludeVoided(boolean includeVoided) {
		this.includeVoided = includeVoided;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public List<UserRoleScope> getUserRoleScopes() {
		return userRoleScopes;
	}
	
	public void setUserRoleScopes(List<UserRoleScope> userRoleScopes) {
		this.userRoleScopes = userRoleScopes;
	}
	
	public Integer getStartIndex() {
		return startIndex;
	}
	
	public void setStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
	}
	
	public Integer getLimit() {
		return limit;
	}
	
	public void setLimit(Integer limit) {
		this.limit = limit;
	}
	
	public UserRoleScopeLocationSearchFilter withIncludeVoided(boolean includeVoided) {
		setIncludeVoided(includeVoided);
		return this;
	}
}
