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

import java.util.Date;

/**
 * The allowable {@link BatchJobStatus} statuses.
 */
public enum BatchJobStatus {
	
	/**
	 * The batch job is pending.
	 */
	Pending(),
	/**
	 * The batch job is running.
	 */
	Running(),
	/**
	 * The batch job failed.
	 */
	Failed(),
	/**
	 * The batch job completed.
	 */
	Completed(),
	/**
	 * The batch job is cancelled.
	 */
	Cancelled(),
	/**
	 * The batch job has not been run before it is expired.
	 */
	Expired();
	
	BatchJobStatus() {
	}
}
