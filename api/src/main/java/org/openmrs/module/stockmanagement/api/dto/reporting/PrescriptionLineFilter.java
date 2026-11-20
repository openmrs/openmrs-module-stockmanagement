/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.dto.reporting;

import java.util.List;

public class PrescriptionLineFilter extends ReportFilter {
	
	private Integer prescriptionTransactionMin;
	
	private Integer patientId;
	
	private Integer stockItemId;
	
	private Integer observationDispensingLocationConceptId;
	
	private Integer observationDrugConceptId;
	
	private Integer drugId;
	
	private List<Fullfillment> fullfillments;
	
	public Integer getPrescriptionTransactionMin() {
		return prescriptionTransactionMin;
	}
	
	public void setPrescriptionTransactionMin(Integer prescriptionTransactionMin) {
		this.prescriptionTransactionMin = prescriptionTransactionMin;
	}
	
	public Integer getPatientId() {
		return patientId;
	}
	
	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}
	
	public Integer getStockItemId() {
		return stockItemId;
	}
	
	public void setStockItemId(Integer stockItemId) {
		this.stockItemId = stockItemId;
	}
	
	public Integer getDrugId() {
		return drugId;
	}
	
	public void setDrugId(Integer drugId) {
		this.drugId = drugId;
	}
	
	public Integer getObservationDispensingLocationConceptId() {
		return observationDispensingLocationConceptId;
	}
	
	public void setObservationDispensingLocationConceptId(Integer observationDispensingLocationConceptId) {
		this.observationDispensingLocationConceptId = observationDispensingLocationConceptId;
	}
	
	public Integer getObservationDrugConceptId() {
		return observationDrugConceptId;
	}
	
	public void setObservationDrugConceptId(Integer observationDrugConceptId) {
		this.observationDrugConceptId = observationDrugConceptId;
	}
	
	public List<Fullfillment> getFullfillments() {
		return fullfillments;
	}
	
	public void setFullfillments(List<Fullfillment> fullfillments) {
		this.fullfillments = fullfillments;
	}
}
