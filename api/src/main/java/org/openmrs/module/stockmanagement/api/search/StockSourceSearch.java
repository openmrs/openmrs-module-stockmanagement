/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.search;

import org.openmrs.Concept;

import java.io.Serializable;

/**
 * Search criteria for querying {@link org.openmrs.module.stockmanagement.api.model.StockSource}
 * entities.
 */
public class StockSourceSearch implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String uuid;
	
	private String search;
	
	private Concept sourceType;
	
	private Boolean includeVoided;
	
	public StockSourceSearch() {
	}
	
	public StockSourceSearch(String search) {
		this.search = search;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getSearch() {
		return search;
	}
	
	public void setSearch(String search) {
		this.search = search;
	}
	
	public String getTextSearch() {
		return search;
	}
	
	public void setTextSearch(String textSearch) {
		this.search = textSearch;
	}
	
	public Concept getSourceType() {
		return sourceType;
	}
	
	public void setSourceType(Concept sourceType) {
		this.sourceType = sourceType;
	}
	
	public Boolean getIncludeVoided() {
		return includeVoided != null ? includeVoided : false;
	}
	
	public void setIncludeVoided(Boolean includeVoided) {
		this.includeVoided = includeVoided;
	}
}
