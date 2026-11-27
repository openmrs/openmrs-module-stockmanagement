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

import java.util.*;
import java.util.stream.Collectors;
import org.openmrs.*;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.model.*;

/** Shared entity-derived display values used by inventory calculations and REST. */
public final class StockItemFields {
	
	private StockItemFields() {
	}
	
	public static String name(Concept concept, boolean preferLocale) {
		if (concept == null)
			return null;
		List<ConceptName> names = concept.getNames(true).stream()
		        .filter(n -> n.getConceptNameType() == ConceptNameType.FULLY_SPECIFIED)
		        .sorted(Comparator.comparing(ConceptName::getId)).collect(Collectors.toList());
		if (preferLocale)
			for (ConceptName name : names) {
				if (name.getLocale().getDisplayName().equals(Context.getLocale().getDisplayName()))
					return name.getName();
			}
		return names.isEmpty() ? null : names.get(0).getName();
	}
	
	public static boolean isDefaultUnit(StockItemPackagingUOM unit) {
		return unit.getStockItem().getDefaultStockOperationsUoM() != null
		        && Objects.equals(unit.getId(), unit.getStockItem().getDefaultStockOperationsUoM().getId());
	}
	
	public static boolean isDispensingUnit(StockItemPackagingUOM unit) {
		return unit.getStockItem().getDispensingUnitPackagingUoM() != null
		        && Objects.equals(unit.getId(), unit.getStockItem().getDispensingUnitPackagingUoM().getId());
	}
	
	/** Matches packaging query localization, including the dispensing-name override. */
	public static String packagingName(StockItemPackagingUOM unit, boolean dispensing) {
		if (dispensing && isDispensingUnit(unit) && unit.getStockItem().getDispensingUnit() != null) {
			return name(unit.getStockItem().getDispensingUnit(), false);
		}
		return name(unit.getPackagingUom(), true);
	}
}
