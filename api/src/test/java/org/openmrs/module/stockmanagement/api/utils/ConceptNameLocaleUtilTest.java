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

import org.junit.Test;
import org.openmrs.module.stockmanagement.api.dto.ConceptNameDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class ConceptNameLocaleUtilTest {
	
	private static final Locale EN_GB = new Locale("en", "GB");
	
	private static final Locale EN_US = new Locale("en", "US");
	
	private static final Locale ES = new Locale("es");
	
	private static final Locale FR = new Locale("fr");
	
	private static ConceptNameDTO name(int conceptId, String name, Locale locale) {
		ConceptNameDTO dto = new ConceptNameDTO();
		dto.setConceptId(conceptId);
		dto.setName(name);
		dto.setLocale(locale);
		return dto;
	}
	
	private static String select(Locale userLocale, Locale defaultLocale, ConceptNameDTO... names) {
		List<ConceptNameDTO> best = ConceptNameLocaleUtil.selectBestNames(Arrays.asList(names), userLocale, defaultLocale);
		assertEquals(1, best.size());
		return best.get(0).getName();
	}
	
	@Test
	public void shouldPreferTheExactLocaleOfTheUser() {
		assertEquals("Tablet", select(Locale.ENGLISH, FR, name(1, "Comprimido", ES), name(1, "Comprimé", FR), name(1, "Tablet", Locale.ENGLISH)));
	}
	
	@Test
	public void shouldFallBackToTheLanguageOfTheUserWhenThereIsNoExactMatch() {
		assertEquals("Tablet", select(EN_GB, FR, name(1, "Comprimido", ES), name(1, "Tablet", Locale.ENGLISH)));
	}
	
	@Test
	public void shouldPreferTheLanguageOnlyNameOverAnotherCountryOfThatLanguage() {
		assertEquals("Tablet", select(EN_GB, FR, name(1, "Tablet (US)", EN_US), name(1, "Tablet", Locale.ENGLISH)));
	}
	
	@Test
	public void shouldFallBackToTheDefaultLocaleWhenThereIsNoNameInTheLanguageOfTheUser() {
		assertEquals("Comprimé", select(Locale.ENGLISH, FR, name(1, "Comprimido", ES), name(1, "Comprimé", FR)));
	}
	
	@Test
	public void shouldFallBackToTheLanguageOfTheDefaultLocale() {
		assertEquals("Tablet", select(FR, EN_GB, name(1, "Comprimido", ES), name(1, "Tablet", Locale.ENGLISH)));
	}
	
	@Test
	public void shouldStillReturnANameWhenNoLocaleMatches() {
		assertEquals("Comprimido", select(Locale.ENGLISH, FR, name(1, "Comprimido", ES)));
	}
	
	@Test
	public void shouldNotFailOnANameWithoutALocale() {
		assertEquals("Tablet", select(Locale.ENGLISH, FR, name(1, "Unknown", null), name(1, "Tablet", Locale.ENGLISH)));
	}
	
	@Test
	public void shouldReturnOneNamePerConcept() {
		List<ConceptNameDTO> names = new ArrayList<>(Arrays.asList(name(1, "Comprimido", ES), name(2, "Caja", ES), name(1, "Tablet", Locale.ENGLISH), name(2, "Box", Locale.ENGLISH)));
		
		List<ConceptNameDTO> best = ConceptNameLocaleUtil.selectBestNames(names, Locale.ENGLISH, FR);
		
		assertEquals(2, best.size());
		assertEquals("Tablet", best.get(0).getName());
		assertEquals("Box", best.get(1).getName());
	}
	
	@Test
	public void shouldReturnNothingForNoNames() {
		assertEquals(0, ConceptNameLocaleUtil.selectBestNames(Collections.<ConceptNameDTO> emptyList(), Locale.ENGLISH, FR).size());
	}
}
