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

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * The persistent class for the stockmgmt_location_tree database table.
 */
@Entity(name = "stockmanagement.LocationTree")
@Table(name = "stockmgmt_location_tree")
public class LocationTree extends org.openmrs.BaseOpenmrsObject implements Serializable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stockmgmt_location_tree_id")
	private Integer id;
	
	@Column(name = "parent_location_id")
	private Integer parentLocationId;
	
	@Column(name = "child_location_id")
	private Integer childLocationId;
	
	@Column(name = "depth")
	private int depth;
	
	public LocationTree() {
		
	}
	
	public Integer getId() {
		return this.id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Integer getParentLocationId() {
		return parentLocationId;
	}
	
	public void setParentLocationId(Integer parentLocationId) {
		this.parentLocationId = parentLocationId;
	}
	
	public Integer getChildLocationId() {
		return childLocationId;
	}
	
	public void setChildLocationId(Integer childLocationId) {
		this.childLocationId = childLocationId;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}
}
