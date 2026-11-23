/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.StockSourceService;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.search.StockSourceSearch;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StockSourceServiceTest extends BaseModuleContextSensitiveTest {
	
	private StockSourceService stockSourceService;
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
		stockSourceService = Context.getService(StockSourceService.class);
	}
	
	@Test
	public void saveStockSource_shouldSaveStockSourceAndCreateParty() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Medical Supplies Ltd");
		stockSource.setAcronym("MSL");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		
		StockSource saved = stockSourceService.saveStockSource(stockSource);
		
		assertNotNull(saved);
		assertNotNull(saved.getId());
		assertNotNull(saved.getUuid());
		assertEquals("Medical Supplies Ltd", saved.getName());
		assertEquals("MSL", saved.getAcronym());
		
		StockSource fetched = stockSourceService.getStockSourceByUuid(saved.getUuid());
		assertNotNull(fetched);
		assertEquals(saved.getId(), fetched.getId());
		
		// Verify that a Party was created automatically for the StockSource
		Party party = Context.getService(StockManagementService.class).getPartyByStockSource(saved);
		assertNotNull(party);
		assertEquals(saved.getId(), party.getStockSource().getId());
		Integer partyId = party.getId();
		
		// Save again to verify that the existing Party is reused without creating duplicate parties
		saved.setName("Medical Supplies Ltd Updated");
		StockSource updated = stockSourceService.saveStockSource(saved);
		Party reusedParty = Context.getService(StockManagementService.class).getPartyByStockSource(updated);
		assertNotNull(reusedParty);
		assertEquals(partyId, reusedParty.getId());
	}
	
	@Test
	public void getStockSources_shouldFilterOnAllCriteria() {
		StockSource matchingSource = new StockSource();
		matchingSource.setName("Pharma Source Direct");
		matchingSource.setAcronym("PSD");
		matchingSource.setSourceType(Context.getConceptService().getConcept(3));
		matchingSource.setVoided(false);
		stockSourceService.saveStockSource(matchingSource);
		
		StockSource voidedSource = new StockSource();
		voidedSource.setName("Pharma Voided Direct");
		voidedSource.setAcronym("PVD");
		voidedSource.setSourceType(Context.getConceptService().getConcept(3));
		voidedSource.setVoided(true);
		stockSourceService.saveStockSource(voidedSource);
		
		StockSource otherSource = new StockSource();
		otherSource.setName("Other Medical Supplies");
		otherSource.setAcronym("OMS");
		otherSource.setSourceType(Context.getConceptService().getConcept(3));
		otherSource.setVoided(false);
		stockSourceService.saveStockSource(otherSource);
		
		Context.flushSession();
		
		// Search by name "Pharma"
		StockSourceSearch search = new StockSourceSearch();
		search.setSearch("Pharma");
		List<StockSource> result = stockSourceService.getStockSources(search);
		assertTrue(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(matchingSource.getUuid())));
		assertFalse(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(voidedSource.getUuid())));
		assertFalse(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(otherSource.getUuid())));
		
		// Search by acronym "PSD"
		search = new StockSourceSearch();
		search.setSearch("PSD");
		result = stockSourceService.getStockSources(search);
		assertTrue(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(matchingSource.getUuid())));
		assertFalse(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(voidedSource.getUuid())));
		assertFalse(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(otherSource.getUuid())));
		
		// Search by source type
		search = new StockSourceSearch();
		search.setSourceType(matchingSource.getSourceType());
		result = stockSourceService.getStockSources(search);
		assertTrue(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(matchingSource.getUuid())));
		assertFalse(result.stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(voidedSource.getUuid())));
	}
	
	@Test
	public void voidStockSources_shouldVoidByUuidList() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Bulk Void Vendor");
		stockSource.setAcronym("BVV");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		
		StockSource saved = stockSourceService.saveStockSource(stockSource);
		assertFalse(saved.getVoided());
		
		stockSourceService.voidStockSources(Collections.singletonList(saved.getUuid()), "Bulk voiding test",
		    Context.getAuthenticatedUser().getUserId());
		
		Context.flushSession();
		Context.clearSession();
		
		StockSource refreshed = stockSourceService.getStockSourceByUuid(saved.getUuid());
		assertTrue(refreshed.getVoided());
		assertEquals("Bulk voiding test", refreshed.getVoidReason());
	}
	
	@Test
	public void getStockSourceByUuid_shouldEnforcePrivilege() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Auth Test Vendor 1");
		stockSource.setAcronym("ATV1");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		StockSource saved = stockSourceService.saveStockSource(stockSource);
		
		Role viewerRole = getOrCreateRole("Stock Source Viewer", Privileges.APP_STOCKMANAGEMENT_STOCKSOURCES);
		Role unauthorizedRole = getOrCreateRole("Unauthorized User", "Some Random Privilege");
		
		runWithRole(viewerRole, () -> {
			StockSource fetched = stockSourceService.getStockSourceByUuid(saved.getUuid());
			assertNotNull(fetched);
			assertEquals(saved.getUuid(), fetched.getUuid());
		});
		
		runWithRole(unauthorizedRole, () -> {
			assertThrows(APIAuthenticationException.class, () -> stockSourceService.getStockSourceByUuid(saved.getUuid()));
		});
	}
	
	@Test
	public void getStockSources_shouldEnforcePrivilege() {
		Role viewerRole = getOrCreateRole("Stock Source Viewer", Privileges.APP_STOCKMANAGEMENT_STOCKSOURCES);
		Role unauthorizedRole = getOrCreateRole("Unauthorized User", "Some Random Privilege");
		StockSourceSearch search = new StockSourceSearch();
		
		runWithRole(viewerRole, () -> {
			List<StockSource> list = stockSourceService.getStockSources(search);
			assertNotNull(list);
		});
		
		runWithRole(unauthorizedRole, () -> {
			assertThrows(APIAuthenticationException.class, () -> stockSourceService.getStockSources(search));
		});
	}
	
	@Test
	public void saveStockSource_shouldEnforcePrivilege() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Auth Mutate Vendor");
		stockSource.setAcronym("AMV");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		
		Role mutatorRole = getOrCreateRole("Stock Source Mutator", Privileges.TASK_STOCKMANAGEMENT_STOCKSOURCES_MUTATE);
		Role unauthorizedRole = getOrCreateRole("Unauthorized User", "Some Random Privilege");
		
		runWithRole(mutatorRole, () -> {
			StockSource saved = stockSourceService.saveStockSource(stockSource);
			assertNotNull(saved);
			assertNotNull(saved.getId());
		});
		
		StockSource unprivilegedSource = new StockSource();
		unprivilegedSource.setName("Unauthorized Vendor");
		unprivilegedSource.setAcronym("UV");
		unprivilegedSource.setSourceType(Context.getConceptService().getConcept(3));
		
		runWithRole(unauthorizedRole, () -> {
			assertThrows(APIAuthenticationException.class, () -> stockSourceService.saveStockSource(unprivilegedSource));
		});
	}
	
	@Test
	public void voidStockSources_shouldEnforcePrivilege() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Void Auth Vendor");
		stockSource.setAcronym("VAV");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		StockSource saved = stockSourceService.saveStockSource(stockSource);
		
		Role mutatorRole = getOrCreateRole("Stock Source Mutator", Privileges.TASK_STOCKMANAGEMENT_STOCKSOURCES_MUTATE);
		Role unauthorizedRole = getOrCreateRole("Unauthorized User", "Some Random Privilege");
		
		runWithRole(unauthorizedRole, () -> {
			assertThrows(APIAuthenticationException.class,
			    () -> stockSourceService.voidStockSources(Collections.singletonList(saved.getUuid()), "Unauthorized void",
			        Context.getAuthenticatedUser().getUserId()));
		});
		
		runWithRole(mutatorRole, () -> {
			stockSourceService.voidStockSources(Collections.singletonList(saved.getUuid()), "Authorized void",
			    Context.getAuthenticatedUser().getUserId());
		});
		Context.flushSession();
		Context.clearSession();
		StockSource voided = stockSourceService.getStockSourceByUuid(saved.getUuid());
		assertTrue(voided.getVoided());
	}
	
	private Role getOrCreateRole(String roleName, String privilegeName) {
		Role role = Context.getUserService().getRole(roleName);
		if (role == null) {
			role = new Role();
			role.setRole(roleName);
			role.setDescription(roleName);
			Privilege priv = Context.getUserService().getPrivilege(privilegeName);
			if (priv == null) {
				priv = Context.getUserService().savePrivilege(new Privilege(privilegeName, privilegeName));
			}
			role.addPrivilege(priv);
			role = Context.getUserService().saveRole(role);
		}
		return role;
	}
	
	private void runWithRole(Role role, Runnable action) {
		Set<Role> originalRoles = new HashSet<>(Context.getAuthenticatedUser().getRoles());
		try {
			Set<Role> roles = new HashSet<>();
			if (role != null) {
				roles.add(role);
			}
			Context.getAuthenticatedUser().setRoles(roles);
			action.run();
		}
		finally {
			Context.getAuthenticatedUser().setRoles(originalRoles);
		}
	}
}
