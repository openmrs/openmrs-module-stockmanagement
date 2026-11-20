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

import org.openmrs.Location;
import org.openmrs.Role;
import org.openmrs.User;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * The persistent class for the stockmgmt_user_role_scope_location database table.
 */
@Entity(name = "stockmanagement.UserRoleScopeLocation")
@Table(name = "stockmgmt_user_role_scope_location")
public class UserRoleScopeLocation extends org.openmrs.BaseChangeableOpenmrsData implements Serializable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_role_scope_location_id")
	private Integer id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_role_scope_id")
	private UserRoleScope userRoleScope;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location;
	
	@Column(name = "enable_descendants")
	private boolean enableDescendants;
	
	public UserRoleScopeLocation() {
	}
	
	public Integer getId() {
		return this.id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public UserRoleScope getUserRoleScope() {
		return userRoleScope;
	}
	
	public void setUserRoleScope(UserRoleScope userRoleScope) {
		this.userRoleScope = userRoleScope;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public boolean getEnableDescendants() {
		return enableDescendants;
	}
	
	public void setEnableDescendants(boolean enableDescendants) {
		this.enableDescendants = enableDescendants;
	}
}
