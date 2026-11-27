/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.StockRule;
import org.openmrs.module.stockmanagement.api.model.UserRoleScope;
import org.openmrs.module.stockmanagement.api.model.UserRoleScopeLocation;
import org.openmrs.module.stockmanagement.api.model.UserRoleScopeOperationType;
import org.openmrs.module.stockmanagement.tasks.LocationTreeSynchronize;
import org.openmrs.module.stockmanagement.tasks.PartySynchronize;
import org.openmrs.module.stockmanagement.web.controller.StockManagementResourceController;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.Hyperlink;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class StockRuleResourceTest extends BaseModuleWebContextSensitiveTest {
	
	private static final String RESOURCE = "stockrule";
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	@Autowired
	private StockManagementResourceController controller;
	
	private StockManagementDao daoInstance;
	
	private EntityUtil entityUtil;
	
	private Location primaryLocation;
	
	private Location secondaryLocation;
	
	private StockItem stockItem;
	
	private StockItemPackagingUOM packagingUom;
	
	private Role ruleManagerRole;
	
	private User ruleManagerUser;
	
	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
		}
		return daoInstance;
	}
	
	private EntityUtil eu() {
		if (entityUtil == null) {
			entityUtil = new EntityUtil(Context.getConceptService().getDrug(3), Context.getUserService().getUser(501),
			        Context.getLocationService().getLocation(1), Context.getUserService().getRole("Provider"),
			        Context.getConceptService().getDrug(3).getConcept(), Context.getPatientService().getPatient(2));
		}
		return entityUtil;
	}
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_MANAGEMENT_DATA_SET);
		
		primaryLocation = new Location();
		primaryLocation.setName("Stock Rule Primary Location " + UUID.randomUUID());
		primaryLocation = Context.getLocationService().saveLocation(primaryLocation);
		
		secondaryLocation = new Location();
		secondaryLocation.setName("Stock Rule Secondary Location " + UUID.randomUUID());
		secondaryLocation = Context.getLocationService().saveLocation(secondaryLocation);
		
		new PartySynchronize().execute();
		new LocationTreeSynchronize().execute();
		
		stockItem = eu().newStockItem(dao(), false);
		stockItem.setCommonName("Paracetamol 500mg");
		stockItem.setVoided(false);
		dao().saveStockItem(stockItem);
		
		packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		packagingUom.setFactor(BigDecimal.valueOf(10));
		dao().saveStockItemPackagingUOM(packagingUom);
		
		ruleManagerRole = createRole("Stock Rule Manager", Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE, PrivilegeConstants.GET_LOCATIONS,
		    PrivilegeConstants.GET_CONCEPTS, PrivilegeConstants.GET_USERS);
		ruleManagerUser = createUser("stockrule_mgr_" + UUID.randomUUID().toString().substring(0, 8), ruleManagerRole);
		grantLocationScope(ruleManagerUser, ruleManagerRole, primaryLocation);
		
		Context.flushSession();
		Context.clearSession();
		Context.authenticate(ruleManagerUser.getUsername(), "TestPassword123!");
	}
	
	private Role createRole(String name, String... privilegeNames) {
		Role role = new Role();
		role.setRole(name + " " + UUID.randomUUID().toString().substring(0, 6));
		role.setDescription("Test role");
		for (String privilegeName : privilegeNames) {
			Privilege privilege = Context.getUserService().getPrivilege(privilegeName);
			if (privilege == null) {
				privilege = Context.getUserService().savePrivilege(new Privilege(privilegeName, "Test privilege"));
			}
			role.addPrivilege(privilege);
		}
		return Context.getUserService().saveRole(role);
	}
	
	private User createUser(String username, Role role) {
		Person person = new Person();
		person.setGender("F");
		PersonName personName = new PersonName("Rule", "Test", "User");
		person.addName(personName);
		person = Context.getPersonService().savePerson(person);
		
		User user = new User();
		user.setPerson(person);
		user.setUsername(username);
		Set<Role> roles = new HashSet<>();
		roles.add(role);
		user.setRoles(roles);
		return Context.getUserService().createUser(user, "TestPassword123!");
	}
	
	private void grantLocationScope(User user, Role role, Location location) {
		UserRoleScope scope = new UserRoleScope();
		scope.setUser(user);
		scope.setRole(role);
		scope.setPermanent(true);
		scope.setEnabled(true);
		scope.setVoided(false);
		scope.setCreator(user);
		scope.setDateCreated(new Date());
		dao().saveUserRoleScope(scope);
		
		UserRoleScopeLocation scopeLocation = new UserRoleScopeLocation();
		scopeLocation.setUserRoleScope(scope);
		scopeLocation.setLocation(location);
		scopeLocation.setEnableDescendants(false);
		scopeLocation.setVoided(false);
		scopeLocation.setCreator(user);
		scopeLocation.setDateCreated(new Date());
		dao().saveUserRoleScopeLocation(scopeLocation);
		
		UserRoleScopeOperationType opType = new UserRoleScopeOperationType();
		opType.setUserRoleScope(scope);
		opType.setStockOperationType(StockOperationType.RECEIPT);
		opType.setVoided(false);
		opType.setCreator(user);
		opType.setDateCreated(new Date());
		dao().saveUserRoleScopeOperationType(opType);
	}
	
	private SimpleObject newCreatePayload(String name) {
		return new SimpleObject().add("stockItem", stockItem.getUuid()).add("location", primaryLocation.getUuid())
		        .add("stockItemPackagingUOM", packagingUom.getUuid()).add("name", name)
		        .add("description", "Alert when stock falls below threshold").add("quantity", 25.0).add("enabled", true)
		        .add("evaluationFrequency", 60).add("actionFrequency", 1440).add("alertRole", ruleManagerRole.getRole())
		        .add("mailRole", ruleManagerRole.getRole()).add("enableDescendants", false);
	}
	
	private SimpleObject createRuleViaController(String name) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/rest/v1/stockmanagement/" + RESOURCE);
		MockHttpServletResponse response = new MockHttpServletResponse();
		Object created = controller.create(RESOURCE, newCreatePayload(name), request, response);
		assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
		return (SimpleObject) created;
	}
	
	@Test
	public void save_shouldRejectAStockRuleWithoutAStockItem() {
		StockRuleResource resource = new StockRuleResource();
		StockRule stockRule = new StockRule();
		
		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(stockRule));
		
		assertNotNull(exception.getErrors().getFieldError("stockItem"));
	}
	
	@Test
	public void createAndRetrieve_shouldReturnStandardOpenmrsRepresentations() throws Exception {
		SimpleObject created = createRuleViaController("Minimum Paracetamol");
		String uuid = (String) created.get("uuid");
		assertNotNull(uuid);
		assertEquals("Minimum Paracetamol", created.get("display"));
		assertEquals("Minimum Paracetamol", created.get("name"));
		assertFalse(created.containsKey("stockItemUuid"));
		assertFalse(created.containsKey("locationUuid"));
		assertFalse(created.containsKey("stockItemPackagingUOMUuid"));
		assertFalse(created.containsKey("creatorGivenName"));
		
		// Default representation: nested entities are REF
		MockHttpServletRequest defaultReq = new MockHttpServletRequest("GET",
		        "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid);
		SimpleObject defaultRep = (SimpleObject) controller.retrieve(RESOURCE, uuid, defaultReq,
		    new MockHttpServletResponse());
		assertEquals(uuid, defaultRep.get("uuid"));
		assertEquals("Minimum Paracetamol", defaultRep.get("display"));
		assertEquals(false, defaultRep.get("voided"));
		
		Map<?, ?> stockItemRef = (Map<?, ?>) defaultRep.get("stockItem");
		assertEquals(stockItem.getUuid(), stockItemRef.get("uuid"));
		assertEquals("Paracetamol 500mg", stockItemRef.get("display"));
		assertNull(stockItemRef.get("commonName"));
		
		Map<?, ?> locationRef = (Map<?, ?>) defaultRep.get("location");
		assertEquals(primaryLocation.getUuid(), locationRef.get("uuid"));
		assertNotNull(locationRef.get("display"));
		assertNull(locationRef.get("name"));
		
		Map<?, ?> uomRef = (Map<?, ?>) defaultRep.get("stockItemPackagingUOM");
		assertEquals(packagingUom.getUuid(), uomRef.get("uuid"));
		assertNotNull(uomRef.get("display"));
		assertNull(uomRef.get("factor"));
		
		// Full representation: nested entities are DEFAULT + auditInfo + permission
		MockHttpServletRequest fullReq = new MockHttpServletRequest("GET",
		        "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid);
		fullReq.setParameter("v", "full");
		SimpleObject fullRep = (SimpleObject) controller.retrieve(RESOURCE, uuid, fullReq, new MockHttpServletResponse());
		Map<?, ?> stockItemDefault = (Map<?, ?>) fullRep.get("stockItem");
		assertEquals("Paracetamol 500mg", stockItemDefault.get("commonName"));
		Map<?, ?> locationDefault = (Map<?, ?>) fullRep.get("location");
		assertEquals(primaryLocation.getName(), locationDefault.get("name"));
		Map<?, ?> uomDefault = (Map<?, ?>) fullRep.get("stockItemPackagingUOM");
		assertEquals(0, BigDecimal.valueOf(10).compareTo((BigDecimal) uomDefault.get("factor")));
		Map<?, ?> auditInfo = (Map<?, ?>) fullRep.get("auditInfo");
		assertNotNull(auditInfo);
		assertNotNull(auditInfo.get("creator"));
		assertNotNull(auditInfo.get("dateCreated"));
		Map<?, ?> permission = (Map<?, ?>) fullRep.get("permission");
		assertEquals(true, permission.get("canView"));
		assertEquals(true, permission.get("canEdit"));
		
		// Ref representation
		MockHttpServletRequest refReq = new MockHttpServletRequest("GET",
		        "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid);
		refReq.setParameter("v", "ref");
		SimpleObject refRep = (SimpleObject) controller.retrieve(RESOURCE, uuid, refReq, new MockHttpServletResponse());
		assertEquals(uuid, refRep.get("uuid"));
		assertEquals("Minimum Paracetamol", refRep.get("display"));
		assertNull(refRep.get("quantity"));
		assertNotNull(refRep.get("links"));
		
		// Custom representation (including nested custom paths)
		MockHttpServletRequest customReq = new MockHttpServletRequest("GET",
		        "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid);
		customReq.setParameter("v",
		    "custom:(uuid,name,quantity,stockItem:(uuid,commonName),location:(uuid,name),stockItemPackagingUOM:(uuid,factor,packagingUom:(uuid,display)))");
		SimpleObject customRep = (SimpleObject) controller.retrieve(RESOURCE, uuid, customReq,
		    new MockHttpServletResponse());
		assertEquals(uuid, customRep.get("uuid"));
		assertEquals("Minimum Paracetamol", customRep.get("name"));
		assertNull(customRep.get("enabled"));
		Map<?, ?> customItem = (Map<?, ?>) customRep.get("stockItem");
		assertEquals("Paracetamol 500mg", customItem.get("commonName"));
		assertNull(customItem.get("display"));
		Map<?, ?> customUom = (Map<?, ?>) customRep.get("stockItemPackagingUOM");
		assertEquals(0, BigDecimal.valueOf(10).compareTo((BigDecimal) customUom.get("factor")));
		Map<?, ?> customPackagingConcept = (Map<?, ?>) customUom.get("packagingUom");
		assertNotNull(customPackagingConcept.get("uuid"));
		assertNotNull(customPackagingConcept.get("display"));
	}
	
	@Test
	public void update_shouldUpdateAllowedFieldsRecalculateNextEvaluationAndRejectReassignment() throws Exception {
		SimpleObject created = createRuleViaController("Initial Rule");
		String uuid = (String) created.get("uuid");
		
		StockRule persisted = dao().getStockRuleByUuid(uuid);
		Date lastEval = DateUtils.truncate(DateUtils.addMinutes(new Date(), -120), Calendar.SECOND);
		persisted.setLastEvaluation(lastEval);
		persisted.setNextEvaluation(DateUtils.addMinutes(lastEval, 60));
		dao().saveStockRule(persisted);
		Context.flushSession();
		Context.clearSession();
		
		SimpleObject updatePayload = new SimpleObject().add("name", "Updated Rule").add("quantity", 40.0)
		        .add("evaluationFrequency", 180);
		MockHttpServletRequest updateReq = new MockHttpServletRequest("POST",
		        "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid);
		MockHttpServletResponse updateResp = new MockHttpServletResponse();
		SimpleObject updated = (SimpleObject) controller.update(RESOURCE, uuid, updatePayload, updateReq, updateResp);
		
		assertEquals(HttpServletResponse.SC_OK, updateResp.getStatus());
		assertEquals("Updated Rule", updated.get("name"));
		assertEquals(0, BigDecimal.valueOf(40.0).compareTo((BigDecimal) updated.get("quantity")));
		assertEquals(Long.valueOf(180L), (Long) updated.get("evaluationFrequency"));
		
		StockRule afterUpdate = dao().getStockRuleByUuid(uuid);
		assertEquals(DateUtils.addMinutes(lastEval, 180).getTime(), afterUpdate.getNextEvaluation().getTime());
		
		// Non-updatable properties (stockItem / location) must be rejected with ConversionException (400)
		SimpleObject moveLocation = new SimpleObject().add("location", secondaryLocation.getUuid());
		assertThrows(ConversionException.class,
		    () -> controller.update(RESOURCE, uuid, moveLocation,
		        new MockHttpServletRequest("POST", "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid),
		        new MockHttpServletResponse()));
	}
	
	@Test
	public void searchAndGetAll_shouldTreatStartIndexAsRowOffsetAndFilterResults() throws Exception {
		List<String> createdUuids = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			SimpleObject created = createRuleViaController("Rule " + i);
			createdUuids.add((String) created.get("uuid"));
		}
		Context.flushSession();
		Context.clearSession();
		
		// Ordered by id desc: [Rule 5, Rule 4, Rule 3, Rule 2, Rule 1]
		MockHttpServletRequest page1Req = new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE);
		page1Req.setParameter("stockItemUuid", stockItem.getUuid());
		page1Req.setParameter("startIndex", "0");
		page1Req.setParameter("limit", "2");
		page1Req.setParameter("totalCount", "true");
		SimpleObject page1 = controller.get(RESOURCE, page1Req, new MockHttpServletResponse());
		List<?> page1Results = (List<?>) page1.get("results");
		assertEquals(2, page1Results.size());
		assertEquals(Long.valueOf(5L), (Long) page1.get("totalCount"));
		assertEquals(createdUuids.get(4), ((Map<?, ?>) page1Results.get(0)).get("uuid"));
		assertEquals(createdUuids.get(3), ((Map<?, ?>) page1Results.get(1)).get("uuid"));
		List<?> page1Links = (List<?>) page1.get("links");
		assertEquals(1, page1Links.size());
		assertTrue(((Hyperlink) page1Links.get(0)).getUri().contains("startIndex=2"));
		
		// Mid-page row offset: startIndex=1, limit=2 must return [Rule 4, Rule 3], NOT [Rule 3, Rule 2]
		MockHttpServletRequest offsetReq = new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE);
		offsetReq.setParameter("stockItemUuid", stockItem.getUuid());
		offsetReq.setParameter("startIndex", "1");
		offsetReq.setParameter("limit", "2");
		SimpleObject offsetPage = controller.get(RESOURCE, offsetReq, new MockHttpServletResponse());
		List<?> offsetResults = (List<?>) offsetPage.get("results");
		assertEquals(2, offsetResults.size());
		assertEquals(createdUuids.get(3), ((Map<?, ?>) offsetResults.get(0)).get("uuid"));
		assertEquals(createdUuids.get(2), ((Map<?, ?>) offsetResults.get(1)).get("uuid"));
		List<?> offsetLinks = (List<?>) offsetPage.get("links");
		assertEquals(2, offsetLinks.size());
		
		// Last page: startIndex=4, limit=2 returns [Rule 1] and only a prev link
		MockHttpServletRequest lastReq = new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE);
		lastReq.setParameter("stockItemUuid", stockItem.getUuid());
		lastReq.setParameter("startIndex", "4");
		lastReq.setParameter("limit", "2");
		SimpleObject lastPage = controller.get(RESOURCE, lastReq, new MockHttpServletResponse());
		List<?> lastResults = (List<?>) lastPage.get("results");
		assertEquals(1, lastResults.size());
		assertEquals(createdUuids.get(0), ((Map<?, ?>) lastResults.get(0)).get("uuid"));
		List<?> lastLinks = (List<?>) lastPage.get("links");
		assertEquals(1, lastLinks.size());
		assertTrue(((Hyperlink) lastLinks.get(0)).getUri().contains("startIndex=2"));
		
		// Name search via q and locationUuid filter
		MockHttpServletRequest qReq = new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE);
		qReq.setParameter("locationUuid", primaryLocation.getUuid());
		qReq.setParameter("q", "Rule 3");
		SimpleObject qPage = controller.get(RESOURCE, qReq, new MockHttpServletResponse());
		List<?> qResults = (List<?>) qPage.get("results");
		assertEquals(1, qResults.size());
		assertEquals(createdUuids.get(2), ((Map<?, ?>) qResults.get(0)).get("uuid"));
		
		// Negative startIndex must return 400 IllegalRequestException
		MockHttpServletRequest negativeReq = new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE);
		negativeReq.setParameter("startIndex", "-1");
		assertThrows(IllegalRequestException.class,
		    () -> controller.get(RESOURCE, negativeReq, new MockHttpServletResponse()));
	}
	
	@Test
	public void deleteAndUndelete_shouldVoidRestoreAndEnforceStandardErrors() throws Exception {
		SimpleObject created = createRuleViaController("Rule to Void");
		String uuid = (String) created.get("uuid");
		
		// Reason > 250 chars -> 400
		char[] longReason = new char[251];
		java.util.Arrays.fill(longReason, 'x');
		assertThrows(IllegalRequestException.class,
		    () -> controller.delete(RESOURCE, uuid, new String(longReason),
		        new MockHttpServletRequest("DELETE", "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid),
		        new MockHttpServletResponse()));
		
		// Valid void -> 204
		MockHttpServletResponse deleteResp = new MockHttpServletResponse();
		controller.delete(RESOURCE, uuid, "no longer needed",
		    new MockHttpServletRequest("DELETE", "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid), deleteResp);
		assertEquals(HttpServletResponse.SC_NO_CONTENT, deleteResp.getStatus());
		Context.flushSession();
		Context.clearSession();
		
		// Excluded from default search, included with includeAll=true
		MockHttpServletRequest searchReq = new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE);
		searchReq.setParameter("stockItemUuid", stockItem.getUuid());
		SimpleObject searchResult = controller.get(RESOURCE, searchReq, new MockHttpServletResponse());
		assertTrue(((List<?>) searchResult.get("results")).isEmpty());
		
		MockHttpServletRequest includeAllReq = new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE);
		includeAllReq.setParameter("stockItemUuid", stockItem.getUuid());
		includeAllReq.setParameter("includeAll", "true");
		SimpleObject includeAllResult = controller.get(RESOURCE, includeAllReq, new MockHttpServletResponse());
		assertEquals(1, ((List<?>) includeAllResult.get("results")).size());
		
		// Retrieve by UUID on voided rule returns voided=true even in ref representation
		MockHttpServletRequest refReq = new MockHttpServletRequest("GET",
		        "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid);
		refReq.setParameter("v", "ref");
		SimpleObject voidedRef = (SimpleObject) controller.retrieve(RESOURCE, uuid, refReq, new MockHttpServletResponse());
		assertEquals(true, voidedRef.get("voided"));
		
		// Undelete restores the rule (MainResourceController expects "deleted": "false")
		MockHttpServletResponse undeleteResp = new MockHttpServletResponse();
		SimpleObject restored = (SimpleObject) controller.update(RESOURCE, uuid, new SimpleObject().add("deleted", "false"),
		    new MockHttpServletRequest("POST", "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid), undeleteResp);
		assertEquals(false, restored.get("voided"));
		
		// Purge is unsupported (400)
		assertThrows(ResourceDoesNotSupportOperationException.class,
		    () -> controller.purge(RESOURCE, uuid,
		        new MockHttpServletRequest("DELETE", "/rest/v1/stockmanagement/" + RESOURCE + "/" + uuid),
		        new MockHttpServletResponse()));
	}
	
	@Test
	public void errors_shouldMapToStandardOpenmrsRestStatusCodes() throws Exception {
		// 404 Not Found on retrieve, update, delete of unknown uuid
		String unknownUuid = UUID.randomUUID().toString();
		ObjectNotFoundException notFound = assertThrows(ObjectNotFoundException.class,
		    () -> controller.retrieve(RESOURCE, unknownUuid,
		        new MockHttpServletRequest("GET", "/rest/v1/stockmanagement/" + RESOURCE + "/" + unknownUuid),
		        new MockHttpServletResponse()));
		MockHttpServletResponse notFoundResp = new MockHttpServletResponse();
		controller.handleException(notFound, new MockHttpServletRequest(), notFoundResp);
		assertEquals(HttpServletResponse.SC_NOT_FOUND, notFoundResp.getStatus());
		
		assertThrows(ObjectNotFoundException.class,
		    () -> controller.update(RESOURCE, unknownUuid, new SimpleObject().add("name", "Updated"),
		        new MockHttpServletRequest("POST", "/rest/v1/stockmanagement/" + RESOURCE + "/" + unknownUuid),
		        new MockHttpServletResponse()));
		assertThrows(ObjectNotFoundException.class,
		    () -> controller.delete(RESOURCE, unknownUuid, "reason",
		        new MockHttpServletRequest("DELETE", "/rest/v1/stockmanagement/" + RESOURCE + "/" + unknownUuid),
		        new MockHttpServletResponse()));
		
		// 400 Bad Request on validation failures
		SimpleObject invalidPayload = newCreatePayload("Invalid Qty").add("quantity", 0);
		org.openmrs.module.webservices.validation.ValidationException validationException = assertThrows(
		    org.openmrs.module.webservices.validation.ValidationException.class,
		    () -> controller.create(RESOURCE, invalidPayload,
		        new MockHttpServletRequest("POST", "/rest/v1/stockmanagement/" + RESOURCE), new MockHttpServletResponse()));
		assertNotNull(validationException.getErrors().getFieldError("quantity"));
		MockHttpServletResponse validationResp = new MockHttpServletResponse();
		controller.validationExceptionHandler(validationException, new MockHttpServletRequest(), validationResp);
		assertEquals(HttpServletResponse.SC_BAD_REQUEST, validationResp.getStatus());
		
		// 403 Forbidden when user lacks scope for target location
		SimpleObject unscopedPayload = newCreatePayload("Unscoped Location").add("location", secondaryLocation.getUuid());
		APIAuthenticationException forbiddenException = assertThrows(APIAuthenticationException.class,
		    () -> controller.create(RESOURCE, unscopedPayload,
		        new MockHttpServletRequest("POST", "/rest/v1/stockmanagement/" + RESOURCE), new MockHttpServletResponse()));
		MockHttpServletResponse forbiddenResp = new MockHttpServletResponse();
		controller.apiAuthenticationExceptionHandler(forbiddenException, new MockHttpServletRequest(), forbiddenResp);
		assertEquals(HttpServletResponse.SC_FORBIDDEN, forbiddenResp.getStatus());
		
		// 500 GenericRestException without leaking raw exception cause on unexpected errors
		StockRuleResource resource = new StockRuleResource();
		StockManagementService mockService = mock(StockManagementService.class);
		resource.setStockManagementService(mockService);
		when(mockService.saveStockRule(any(StockRule.class))).thenThrow(new NullPointerException("secret internal detail"));
		
		StockRule validRule = new StockRule();
		validRule.setStockItem(stockItem);
		validRule.setLocation(primaryLocation);
		validRule.setStockItemPackagingUOM(packagingUom);
		validRule.setName("Valid");
		validRule.setQuantity(BigDecimal.TEN);
		validRule.setEnabled(true);
		validRule.setEvaluationFrequency(60L);
		validRule.setActionFrequency(1440L);
		
		GenericRestException genericException = assertThrows(GenericRestException.class, () -> resource.save(validRule));
		assertEquals("Unable to save stock rule", genericException.getMessage());
		assertNull(genericException.getCause());
		MockHttpServletResponse serverErrorResp = new MockHttpServletResponse();
		controller.handleException(genericException, new MockHttpServletRequest(), serverErrorResp);
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, serverErrorResp.getStatus());
		verify(mockService).saveStockRule(any(StockRule.class));
	}
}
