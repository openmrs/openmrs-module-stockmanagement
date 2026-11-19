/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.utils;

import org.openmrs.module.stockmanagement.api.dto.ConceptNameDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConceptNameLocaleUtil {
	
	private static final int LEAST_PREFERRED = Integer.MAX_VALUE;
	
	private ConceptNameLocaleUtil() {
	}
	
	/**
	 * Picks the name to show for each concept, in this order: the user's exact locale, the user's
	 * language, the default locale, the default locale's language, then any other name.
	 * 
	 * @param names the names of one or more concepts, in any locale
	 * @param userLocale the locale of the current user
	 * @param defaultLocale the default locale of the system
	 * @return one name per concept
	 */
	public static List<ConceptNameDTO> selectBestNames(List<ConceptNameDTO> names, Locale userLocale, Locale defaultLocale) {
		Map<Integer, ConceptNameDTO> bestNames = new LinkedHashMap<>();
		for (ConceptNameDTO name : names) {
			ConceptNameDTO best = bestNames.get(name.getConceptId());
			if (best == null
			        || rank(name.getLocale(), userLocale, defaultLocale) < rank(best.getLocale(), userLocale, defaultLocale)) {
				bestNames.put(name.getConceptId(), name);
			}
		}
		return new ArrayList<>(bestNames.values());
	}
	
	private static int rank(Locale locale, Locale userLocale, Locale defaultLocale) {
		int userRank = rank(locale, userLocale);
		if (userRank != LEAST_PREFERRED) {
			return userRank;
		}
		int defaultRank = rank(locale, defaultLocale);
		return defaultRank != LEAST_PREFERRED ? defaultRank + 3 : LEAST_PREFERRED;
	}
	
	private static int rank(Locale locale, Locale wanted) {
		if (locale == null || wanted == null) {
			return LEAST_PREFERRED;
		}
		if (locale.equals(wanted)) {
			return 0;
		}
		if (!locale.getLanguage().equals(wanted.getLanguage())) {
			return LEAST_PREFERRED;
		}
		return locale.getCountry().isEmpty() ? 1 : 2;
	}
}
