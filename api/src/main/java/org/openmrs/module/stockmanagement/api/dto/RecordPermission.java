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

public class RecordPermission {
	
	private boolean canEdit;
	
	private boolean canView;
	
	private boolean canApprove;
	
	private boolean canReceipt;
	
	private boolean displayReceipt;
	
	public boolean getCanEdit() {
		return canEdit;
	}
	
	public void setCanEdit(boolean canEdit) {
		this.canEdit = canEdit;
	}
	
	public boolean getCanView() {
		return canView;
	}
	
	public void setCanView(boolean canView) {
		this.canView = canView;
	}
	
	public boolean getCanApprove() {
		return canApprove;
	}
	
	public void setCanApprove(boolean canApprove) {
		this.canApprove = canApprove;
	}
	
	public boolean getCanReceipt() {
		return canReceipt;
	}
	
	public void setCanReceipt(boolean canReceipt) {
		this.canReceipt = canReceipt;
	}
	
	public boolean getDisplayReceipt() {
		return displayReceipt;
	}
	
	public void setDisplayReceipt(boolean displayReceipt) {
		this.displayReceipt = displayReceipt;
	}
}
