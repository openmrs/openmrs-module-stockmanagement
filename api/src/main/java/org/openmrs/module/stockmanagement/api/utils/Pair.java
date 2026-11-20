/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.utils;

public class Pair<T1, T2> {
	
	private T1 value1;
	
	private T2 value2;
	
	public Pair(T1 value1, T2 value2) {
		this.value1 = value1;
		this.value2 = value2;
	}
	
	public T1 getValue1() {
		return value1;
	}
	
	public void setValue1(T1 value1) {
		this.value1 = value1;
	}
	
	public T2 getValue2() {
		return value2;
	}
	
	public void setValue2(T2 value2) {
		this.value2 = value2;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		
		Pair<?, ?> pair = (Pair<?, ?>) o;
		
		if (value1 != null ? !value1.equals(pair.value1) : pair.value1 != null)
			return false;
		return !(value2 != null ? !value2.equals(pair.value2) : pair.value2 != null);
		
	}
	
	@Override
	public int hashCode() {
		int result = value1 != null ? value1.hashCode() : 0;
		result = 31 * result + (value2 != null ? value2.hashCode() : 0);
		return result;
	}
}
