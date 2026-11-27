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

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.LocationService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.StockLocationTags;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.PartyDTO;
import org.openmrs.module.stockmanagement.api.dto.PrivilegeScope;
import org.openmrs.module.stockmanagement.api.dto.SessionInfo;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventorySearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockItemTransactionSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockOperationActionLineItem;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockRuleCurrentQuantity;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeLocationDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeOperationTypeDTO;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.StockBatch;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationItem;
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.model.UserRoleScope;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.module.stockmanagement.tasks.LocationTreeSynchronize;
import org.openmrs.module.stockmanagement.tasks.PartySynchronize;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Captures current behaviour before refactoring: 1. Stock operation lifecycle (submit, approve,
 * dispatch, complete, reject, return, cancel). 2. Permission checks including access granted
 * through a parent location hierarchy. 3. The three queries rewritten later: - users assigned to a
 * scope (getActiveUsersAssignedForScope) - user's flattened privilege scopes
 * (getFlattenedUserRoleScopesByUser / userHasStockManagementPrivilege) - stock balances including
 * child locations (setStockItemCurrentBalanceWithDescendants)
 */
public class StockManagementFlowsAndQueriesServiceTest extends BaseModuleContextSensitiveTest {
	
	private StockManagementService stockManagementService;
	
	private Location parentLocation;
	
	private Location childLocation;
	
	private Location grandchildLocation;
	
	private Party parentParty;
	
	private Party childParty;
	
	private Party grandchildParty;
	
	private Role managerRole;
	
	private User clerkUser;
	
	private StockItem sampleStockItem;
	
	private StockItemPackagingUOM samplePackagingUom;
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	private StockManagementDao daoInstance;
	
	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
		}
		return daoInstance;
	}
	
	@BeforeEach
	public void setUp() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_MANAGEMENT_DATA_SET);
		
		stockManagementService = Context.getService(StockManagementService.class);
		
		// 1. Build a small 3-tier location hierarchy: parent -> child -> grandchild
		LocationService locationService = Context.getLocationService();
		
		parentLocation = new Location();
		parentLocation.setName("Parent Hospital " + UUID.randomUUID());
		parentLocation = locationService.saveLocation(parentLocation);
		
		childLocation = new Location();
		childLocation.setName("Child Clinic " + UUID.randomUUID());
		childLocation.setParentLocation(parentLocation);
		childLocation = locationService.saveLocation(childLocation);
		
		grandchildLocation = new Location();
		grandchildLocation.setName("Grandchild Dispensary " + UUID.randomUUID());
		grandchildLocation.setParentLocation(childLocation);
		grandchildLocation = locationService.saveLocation(grandchildLocation);
		
		// Synchronize parties and location tree hierarchy into stockmanagement tables
		new PartySynchronize().execute();
		new LocationTreeSynchronize().execute();
		
		parentParty = stockManagementService.getPartyByLocation(parentLocation);
		childParty = stockManagementService.getPartyByLocation(childLocation);
		grandchildParty = stockManagementService.getPartyByLocation(grandchildLocation);
		
		assertNotNull(parentParty, "Parent party must exist");
		assertNotNull(childParty, "Child party must exist");
		assertNotNull(grandchildParty, "Grandchild party must exist");
		
		Context.flushSession();
		Context.clearSession();
		
		// Create test role carrying stock operations and user role scopes privileges
		managerRole = createRoleWithPrivileges("Stock Operations Manager", Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
		    Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS, Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE,
		    Privileges.TASK_STOCKMANAGEMENT_USERROLESCOPES_MUTATE, PrivilegeConstants.GET_LOCATIONS,
		    PrivilegeConstants.GET_CONCEPTS, PrivilegeConstants.GET_USERS, PrivilegeConstants.GET_GLOBAL_PROPERTIES,
		    PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
		
		sampleStockItem = createStockItem("Paracetamol 500mg " + UUID.randomUUID().toString().substring(0, 8));
		samplePackagingUom = createPackagingUOM(sampleStockItem, BigDecimal.ONE);
		
		clerkUser = createTestUser("clerk_operator_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(clerkUser, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		grantScope(clerkUser, managerRole, parentLocation, true, StockOperationType.STOCK_ISSUE);
		
		Context.getAdministrationService()
		        .saveGlobalProperty(new GlobalProperty("stockmanagement.allowStockIssueWithoutRequisition", "true"));
	}
	
	// ---------------------------------------------------------------------------------------------
	// Query 1: Users assigned to a scope (getActiveUsersAssignedForScope)
	// ---------------------------------------------------------------------------------------------
	
	@Test
	public void getActiveUsersAssignedForScope_shouldIncludeUserInheritedFromParentWhenDescendantsEnabled() {
		User scopedUser = createTestUser("clerk_desc_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(scopedUser, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		
		List<String> roles = Collections.singletonList(managerRole.getRole());
		
		// Query at parent location
		List<Integer> parentUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertTrue(parentUsers.contains(scopedUser.getId()), "Parent location query should include scoped user");
		
		// Query at child location -> should inherit through parent location
		List<Integer> childUsers = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertTrue(childUsers.contains(scopedUser.getId()),
		    "Child location query should include user scoped at parent with descendants");
		
		// Query at grandchild location -> should inherit through grandparent location
		List<Integer> grandchildUsers = stockManagementService.getActiveUsersAssignedForScope(grandchildLocation.getId(),
		    roles);
		assertTrue(grandchildUsers.contains(scopedUser.getId()),
		    "Grandchild location query should include user scoped at grandparent with descendants");
	}
	
	@Test
	public void getActiveUsersAssignedForScope_shouldExcludeChildLocationsWhenDescendantsDisabled() {
		User directUser = createTestUser("clerk_nodesc_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(directUser, managerRole, parentLocation, false, StockOperationType.RECEIPT);
		
		List<String> roles = Collections.singletonList(managerRole.getRole());
		
		// Query at parent location -> direct assignment matches
		List<Integer> parentUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertTrue(parentUsers.contains(directUser.getId()), "Parent location query should include direct user");
		
		// Query at child location -> should NOT inherit because enableDescendants is false
		List<Integer> childUsers = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertFalse(childUsers.contains(directUser.getId()),
		    "Child location query should NOT include user when descendants disabled");
		
		// Query at grandchild location -> should NOT inherit
		List<Integer> grandchildUsers = stockManagementService.getActiveUsersAssignedForScope(grandchildLocation.getId(),
		    roles);
		assertFalse(grandchildUsers.contains(directUser.getId()),
		    "Grandchild location query should NOT include user when descendants disabled");
	}
	
	@Test
	public void getActiveUsersAssignedForScope_shouldReturnUserAssignedDirectlyToChildLocation() {
		User childUser = createTestUser("clerk_child_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(childUser, managerRole, childLocation, true, StockOperationType.RECEIPT);
		
		List<String> roles = Collections.singletonList(managerRole.getRole());
		
		// Not at parent
		List<Integer> parentUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertFalse(parentUsers.contains(childUser.getId()), "Parent query should NOT include user scoped only at child");
		
		// Found at child
		List<Integer> childUsers = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertTrue(childUsers.contains(childUser.getId()), "Child query should include user scoped at child");
		
		// Found at grandchild (descendant of child)
		List<Integer> grandchildUsers = stockManagementService.getActiveUsersAssignedForScope(grandchildLocation.getId(),
		    roles);
		assertTrue(grandchildUsers.contains(childUser.getId()),
		    "Grandchild query should include user scoped at child with descendants");
	}
	
	@Test
	public void getActiveUsersAssignedForScope_shouldFilterByRoleAndExcludeVoidedScopesOrRetiredUsers() {
		User user = createTestUser("clerk_void_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope scope = grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		List<String> roles = Collections.singletonList(managerRole.getRole());
		
		// Initially active user with active scope is returned
		List<Integer> initialUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertTrue(initialUsers.contains(user.getId()), "Initially active user with active scope must be included");
		
		// Different role should return empty
		List<Integer> otherRoleUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(),
		    Collections.singletonList("NonExistentRole"));
		assertFalse(otherRoleUsers.contains(user.getId()));
		
		// Voiding the scope removes the active user from active scope users (exercises urs.voided = 0)
		stockManagementService.voidUserRoleScopes(Collections.singletonList(scope.getUuid()), "Testing voiding scope");
		Context.flushSession();
		Context.clearSession();
		List<Integer> usersAfterVoidScope = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(),
		    roles);
		assertFalse(usersAfterVoidScope.contains(user.getId()),
		    "User with voided scope must be excluded even when user is active");
		
		// Re-grant scope to verify retired user check
		grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		Context.flushSession();
		Context.clearSession();
		assertTrue(
		    stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles).contains(user.getId()),
		    "User with newly granted active scope must be included again");
		
		// Retiring the user removes them from active scope users even if scope is active
		Context.getUserService().retireUser(user, "Testing retirement");
		Context.flushSession();
		Context.clearSession();
		List<Integer> usersAfterRetire = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(),
		    roles);
		assertFalse(usersAfterRetire.contains(user.getId()), "Retired user must be excluded");
	}
	
	@Test
	public void getActiveUsersAssignedForScope_shouldExcludeUsersAtALocationOnceItIsRetired() {
		User inherited = createTestUser("ret_desc_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(inherited, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		User direct = createTestUser("ret_direct_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(direct, managerRole, childLocation, false, StockOperationType.RECEIPT);
		List<String> roles = Collections.singletonList(managerRole.getRole());
		assertTrue(stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles)
		        .containsAll(Arrays.asList(inherited.getId(), direct.getId())));
		retire(childLocation);
		List<Integer> users = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertFalse(users.contains(inherited.getId()));
		assertFalse(users.contains(direct.getId()));
	}
	
	@Test
	public void getActiveUsersAssignedForScope_shouldApplyTheSameFiltersToScopesWithoutDescendants() {
		User voided = createTestUser("direct_void_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope scope = grantScope(voided, managerRole, childLocation, false, StockOperationType.RECEIPT);
		User retired = createTestUser("direct_ret_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(retired, managerRole, childLocation, false, StockOperationType.RECEIPT);
		List<String> roles = Collections.singletonList(managerRole.getRole());
		assertTrue(stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles)
		        .containsAll(Arrays.asList(voided.getId(), retired.getId())));
		assertFalse(stockManagementService
		        .getActiveUsersAssignedForScope(childLocation.getId(), Collections.singletonList("NonExistentRole"))
		        .contains(voided.getId()));
		stockManagementService.voidUserRoleScopes(Collections.singletonList(scope.getUuid()), "Testing");
		Context.getUserService().retireUser(retired, "Testing");
		Context.flushSession();
		Context.clearSession();
		List<Integer> users = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertFalse(users.contains(voided.getId()));
		assertFalse(users.contains(retired.getId()));
	}
	
	@Test
	public void getActiveUsersAssignedForScope_shouldHandleNullOrEmptyInputsAndExcludeIncompleteDateBounds() {
		List<String> roles = Collections.singletonList(managerRole.getRole());
		assertTrue(stockManagementService.getActiveUsersAssignedForScope(null, roles).isEmpty());
		assertTrue(stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), null).isEmpty());
		assertTrue(stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), Collections.emptyList())
		        .isEmpty());
		
		User user = createTestUser("perm_nullbound_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope descScope = grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		UserRoleScope directScope = grantScope(user, managerRole, childLocation, false, StockOperationType.RECEIPT);
		Date today = DateUtil.today();
		Date[][] invalidWindows = new Date[][] { { today, null }, { null, today },
		        { DateUtils.addDays(today, 1), DateUtils.addDays(today, -1) } };
		for (Date[] window : invalidWindows) {
			for (UserRoleScope scope : Arrays.asList(descScope, directScope)) {
				scope.setPermanent(false);
				scope.setActiveFrom(window[0]);
				scope.setActiveTo(window[1]);
			}
			Context.flushSession();
			assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation,
			    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
			assertFalse(
			    stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles).contains(user.getId()));
		}
	}
	
	// ---------------------------------------------------------------------------------------------
	// Query 2: Flattened privilege scopes and permission checks (userHasStockManagementPrivilege)
	// ---------------------------------------------------------------------------------------------
	
	@Test
	public void userHasStockManagementPrivilege_shouldGrantPermissionThroughParentLocationWhenDescendantsEnabled() {
		User user = createTestUser("perm_user_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		
		// Parent location check
		boolean hasParent = stockManagementService.userHasStockManagementPrivilege(user, parentLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue(hasParent, "User should have privilege at parent location");
		
		// Child location check -> inherited through parent
		boolean hasChild = stockManagementService.userHasStockManagementPrivilege(user, childLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue(hasChild, "User should have privilege at child location inherited from parent");
		
		// Grandchild location check -> inherited through parent
		boolean hasGrandchild = stockManagementService.userHasStockManagementPrivilege(user, grandchildLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue(hasGrandchild, "User should have privilege at grandchild location inherited from parent");
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionToChildLocationsWhenDescendantsDisabled() {
		User user = createTestUser("perm_nodesc_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, false, StockOperationType.RECEIPT);
		
		boolean hasParent = stockManagementService.userHasStockManagementPrivilege(user, parentLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue(hasParent, "User should have privilege at parent location");
		
		boolean hasChild = stockManagementService.userHasStockManagementPrivilege(user, childLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertFalse(hasChild, "User should NOT have privilege at child location when descendants disabled");
		
		boolean hasGrandchild = stockManagementService.userHasStockManagementPrivilege(user, grandchildLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertFalse(hasGrandchild, "User should NOT have privilege at grandchild location when descendants disabled");
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionOnceTheScopesAreVoided() {
		User user = createTestUser("perm_void_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope inherited = grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		UserRoleScope direct = grantScope(user, managerRole, childLocation, false, StockOperationType.RECEIPT);
		assertTrue(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		stockManagementService.voidUserRoleScopes(Arrays.asList(inherited.getUuid(), direct.getUuid()), "Testing");
		Context.flushSession();
		Context.clearSession();
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionThroughDisabledScopes() {
		User user = createTestUser("perm_off_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT).setEnabled(false);
		grantScope(user, managerRole, childLocation, false, StockOperationType.RECEIPT).setEnabled(false);
		Context.flushSession();
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService
		        .getActiveUsersAssignedForScope(childLocation.getId(), Collections.singletonList(managerRole.getRole()))
		        .contains(user.getId()));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionThroughExpiredScopes() {
		User user = createTestUser("perm_exp_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		for (UserRoleScope scope : Arrays.asList(
		    grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT),
		    grantScope(user, managerRole, childLocation, false, StockOperationType.RECEIPT))) {
			scope.setPermanent(false);
			scope.setActiveFrom(DateUtils.addDays(new Date(), -10));
			scope.setActiveTo(DateUtils.addDays(new Date(), -1));
		}
		Context.flushSession();
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService
		        .getActiveUsersAssignedForScope(childLocation.getId(), Collections.singletonList(managerRole.getRole()))
		        .contains(user.getId()));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionThroughScopesNotYetActive() {
		User user = createTestUser("perm_new_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		for (UserRoleScope scope : Arrays.asList(
		    grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT),
		    grantScope(user, managerRole, childLocation, false, StockOperationType.RECEIPT))) {
			scope.setPermanent(false);
			scope.setActiveFrom(DateUtils.addDays(new Date(), 1));
			scope.setActiveTo(DateUtils.addDays(new Date(), 10));
		}
		Context.flushSession();
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService
		        .getActiveUsersAssignedForScope(childLocation.getId(), Collections.singletonList(managerRole.getRole()))
		        .contains(user.getId()));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldAllowPermissionOnExactActiveFromAndToBoundaries() {
		User descendantUser = createTestUser("perm_desc_b_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		User directUser = createTestUser("perm_dir_b_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope descendantScope = grantScope(descendantUser, managerRole, parentLocation, true,
		    StockOperationType.RECEIPT);
		UserRoleScope directScope = grantScope(directUser, managerRole, childLocation, false, StockOperationType.RECEIPT);
		List<String> roles = Collections.singletonList(managerRole.getRole());
		Date today = DateUtil.today();
		Date[][] windows = new Date[][] { { today, today }, { today, DateUtils.addDays(today, 5) },
		        { DateUtils.addDays(today, -5), today } };
		for (Date[] window : windows) {
			for (UserRoleScope scope : Arrays.asList(descendantScope, directScope)) {
				scope.setPermanent(false);
				scope.setActiveFrom(window[0]);
				scope.setActiveTo(window[1]);
			}
			Context.flushSession();
			assertTrue(stockManagementService.userHasStockManagementPrivilege(descendantUser, parentLocation,
			    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
			assertTrue(stockManagementService.userHasStockManagementPrivilege(descendantUser, childLocation,
			    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
			assertTrue(stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles)
			        .contains(descendantUser.getId()));
			assertTrue(stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles)
			        .contains(descendantUser.getId()));
			assertTrue(stockManagementService.userHasStockManagementPrivilege(directUser, childLocation,
			    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
			assertTrue(stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles)
			        .contains(directUser.getId()));
		}
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionAtALocationRemovedFromTheScopes() {
		User user = createTestUser("perm_less_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope inherited = grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		UserRoleScope direct = grantScope(user, managerRole, childLocation, false, StockOperationType.RECEIPT);
		rescope(inherited, grandchildLocation, false, StockOperationType.RECEIPT);
		rescope(direct, grandchildLocation, false, StockOperationType.RECEIPT);
		assertTrue(stockManagementService.userHasStockManagementPrivilege(user, grandchildLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService
		        .getActiveUsersAssignedForScope(childLocation.getId(), Collections.singletonList(managerRole.getRole()))
		        .contains(user.getId()));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionForAnOperationTypeRemovedFromTheScopes() {
		User user = createTestUser("perm_untype_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope inherited = grantScope(user, managerRole, parentLocation, true, StockOperationType.STOCK_ISSUE);
		UserRoleScope direct = grantScope(user, managerRole, childLocation, false, StockOperationType.STOCK_ISSUE);
		rescope(inherited, parentLocation, true, StockOperationType.RECEIPT);
		rescope(direct, childLocation, false, StockOperationType.RECEIPT);
		assertTrue(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation,
		    StockOperationType.STOCK_ISSUE, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	@Test
	public void saveUserRoleScope_shouldNotLetAUserGrantOrWidenTheirOwnScope() {
		UserRoleScope own = grantScope(clerkUser, managerRole, grandchildLocation, false, StockOperationType.ADJUSTMENT);
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		String expectedMessage = Context.getMessageSourceService()
		        .getMessage("stockmanagement.userrolescopes.userUuid.selfupdate");
		int refused = 0;
		for (Runnable selfGrant : Arrays.<Runnable> asList(
		    () -> grantScope(clerkUser, managerRole, parentLocation, true, StockOperationType.ADJUSTMENT),
		    () -> rescope(own, parentLocation, true, StockOperationType.ADJUSTMENT))) {
			try {
				selfGrant.run();
			}
			catch (StockManagementException e) {
				assertEquals(expectedMessage, e.getMessage());
				refused++;
			}
		}
		assertEquals(2, refused);
		Context.flushSession();
		Context.clearSession();
		assertFalse(stockManagementService.userHasStockManagementPrivilege(clerkUser, parentLocation,
		    StockOperationType.ADJUSTMENT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	@Test
	public void saveUserRoleScope_shouldRefuseAUserWhoCanOnlyViewScopes() {
		Role viewerRole = createRoleWithPrivileges("Stock Scope Viewer", Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES,
		    PrivilegeConstants.GET_LOCATIONS, PrivilegeConstants.GET_USERS);
		User viewer = createTestUser("scope_viewer_" + UUID.randomUUID().toString().substring(0, 8), viewerRole);
		User target = createTestUser("scope_target_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope existing = grantScope(target, managerRole, childLocation, false, StockOperationType.STOCK_ISSUE);
		Context.authenticate(viewer.getUsername(), "TestPassword123!");
		int refused = 0;
		for (Runnable change : Arrays.<Runnable> asList(
		    () -> grantScope(target, managerRole, parentLocation, true, StockOperationType.STOCK_ISSUE),
		    () -> stockManagementService.voidUserRoleScopes(Collections.singletonList(existing.getUuid()), "test"))) {
			try {
				change.run();
			}
			catch (APIAuthenticationException e) {
				refused++;
			}
		}
		assertEquals(2, refused);
		Context.flushSession();
		Context.clearSession();
		assertFalse(stockManagementService.userHasStockManagementPrivilege(target, parentLocation,
		    StockOperationType.STOCK_ISSUE, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertTrue(stockManagementService.userHasStockManagementPrivilege(target, childLocation,
		    StockOperationType.STOCK_ISSUE, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	private void rescope(UserRoleScope scope, Location location, boolean enableDescendants,
	        StockOperationType operationType) {
		UserRoleScopeDTO dto = new UserRoleScopeDTO();
		dto.setUuid(scope.getUuid());
		dto.setUserUuid(scope.getUser().getUuid());
		dto.setRole(scope.getRole().getRole());
		dto.setPermanent(true);
		dto.setEnabled(true);
		UserRoleScopeLocationDTO locDto = new UserRoleScopeLocationDTO();
		locDto.setLocationUuid(location.getUuid());
		locDto.setEnableDescendants(enableDescendants);
		dto.setLocations(Collections.singletonList(locDto));
		UserRoleScopeOperationTypeDTO opDto = new UserRoleScopeOperationTypeDTO();
		opDto.setOperationType(operationType.getOperationType());
		dto.setOperationTypes(Collections.singletonList(opDto));
		Context.flushSession();
		Context.clearSession();
		stockManagementService.saveUserRoleScope(dto);
		Context.flushSession();
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionForAnOperationTypeOutsideTheScopes() {
		User user = createTestUser("perm_type_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		grantScope(user, managerRole, childLocation, false, StockOperationType.RECEIPT);
		assertTrue(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation,
		    StockOperationType.STOCK_ISSUE, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldNotGrantPermissionThroughAnotherUsersScope() {
		User user = createTestUser("perm_mine_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(createTestUser("perm_other_" + UUID.randomUUID().toString().substring(0, 8), managerRole), managerRole,
		    childLocation, false, StockOperationType.RECEIPT);
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldNotGrantPermissionAboveTheScopedLocation() {
		User user = createTestUser("perm_up_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, childLocation, true, StockOperationType.RECEIPT);
		assertTrue(stockManagementService.userHasStockManagementPrivilege(user, grandchildLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertFalse(stockManagementService.userHasStockManagementPrivilege(user, parentLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldStillGrantPermissionAtARetiredLocation() {
		User user = createTestUser("perm_ret_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		grantScope(user, managerRole, grandchildLocation, false, StockOperationType.STOCK_ISSUE);
		retire(childLocation);
		retire(grandchildLocation);
		assertTrue(stockManagementService.userHasStockManagementPrivilege(user, childLocation, StockOperationType.RECEIPT,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
		assertTrue(stockManagementService.userHasStockManagementPrivilege(user, grandchildLocation,
		    StockOperationType.STOCK_ISSUE, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE));
	}
	
	@Test
	public void getPrivilegeScopes_shouldReturnFlattenedPrivilegeScopesAcrossHierarchy() {
		User user = createTestUser("flatten_user_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		
		// Check scopes at child location
		HashSet<PrivilegeScope> scopes = stockManagementService.getPrivilegeScopes(user, childLocation,
		    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertNotNull(scopes);
		assertFalse(scopes.isEmpty(), "Scopes should not be empty at child location");
		assertTrue(scopes.stream().anyMatch(s -> childLocation.getUuid().equals(s.getLocationUuid())),
		    "Scope should cover child location UUID");
	}
	
	@Test
	public void getCurrentUserSessionInfo_shouldReturnFlattenedScopesAcrossDescendantsAndPrivilegesWhenFiltersAreNull() {
		User user = createTestUser("session_user_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		Context.authenticate(user.getUsername(), "TestPassword123!");
		
		SessionInfo sessionInfo = stockManagementService.getCurrentUserSessionInfo();
		assertNotNull(sessionInfo);
		assertNotNull(sessionInfo.getPrivileges());
		assertTrue(sessionInfo.getPrivileges().stream()
		        .anyMatch(s -> parentLocation.getUuid().equals(s.getLocationUuid())
		                && StockOperationType.RECEIPT == s.getStockOperationType()
		                && Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE.equals(s.getPrivilege())));
		assertTrue(sessionInfo.getPrivileges().stream().anyMatch(s -> childLocation.getUuid().equals(s.getLocationUuid())
		        && Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS.equals(s.getPrivilege())));
		assertTrue(
		    sessionInfo.getPrivileges().stream().anyMatch(s -> grandchildLocation.getUuid().equals(s.getLocationUuid())
		            && Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE.equals(s.getPrivilege())));
		assertFalse(
		    sessionInfo.getPrivileges().stream().anyMatch(s -> StockOperationType.STOCK_ISSUE == s.getStockOperationType()));
	}
	
	@Test
	public void userHasStockManagementPrivilege_shouldFollowTheRolesTheScopedRoleInherits() {
		Role inheriting = createRoleWithPrivileges("Stock Inheriting Role", Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES);
		inheriting.setInheritedRoles(new HashSet<>(Collections.singleton(managerRole)));
		Context.getUserService().saveRole(inheriting);
		User scopedOnOwnRole = createTestUser("perm_own_" + UUID.randomUUID().toString().substring(0, 8), inheriting);
		grantScope(scopedOnOwnRole, inheriting, childLocation, false, StockOperationType.RECEIPT);
		User scopedOnInheritedRole = createTestUser("perm_parent_" + UUID.randomUUID().toString().substring(0, 8),
		    inheriting);
		grantScope(scopedOnInheritedRole, managerRole, childLocation, false, StockOperationType.RECEIPT);
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationSearchFilter filter = new StockOperationSearchFilter();
		filter.setStockOperationUuid(createAndCompleteReceipt(childParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(5), "BATCH-INHERITED").getUuid());
		for (User user : Arrays.asList(scopedOnOwnRole, scopedOnInheritedRole)) {
			assertTrue(stockManagementService.userHasStockManagementPrivilege(user, childLocation,
			    StockOperationType.RECEIPT, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE), user.getUsername());
			Context.authenticate(user.getUsername(), "TestPassword123!");
			assertEquals(1, stockManagementService.findStockOperations(filter).getData().size(), user.getUsername());
		}
	}
	
	@Test
	public void findStockOperations_shouldShowAnIssueToTheLocationItLeaves() {
		User sourceOnly = createTestUser("src_only_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(sourceOnly, managerRole, parentLocation, false, StockOperationType.STOCK_ISSUE);
		grantScope(sourceOnly, managerRole, childLocation, false, StockOperationType.RECEIPT);
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.TEN, "BATCH-OUTGOING");
		StockOperationSearchFilter filter = new StockOperationSearchFilter();
		filter.setStockOperationUuid(submitIssueOfTen(stockManagementService.getStockOperationByUuid(receipt.getUuid())
		        .getStockOperationItems().iterator().next().getStockBatch().getUuid()).getUuid());
		Context.authenticate(sourceOnly.getUsername(), "TestPassword123!");
		assertEquals(1, stockManagementService.findStockOperations(filter).getData().size());
		filter.setStockOperationUuid(receipt.getUuid());
		assertEquals(0, stockManagementService.findStockOperations(filter).getData().size());
	}
	
	@Test
	public void getCompletePartyList_shouldListTheLocationAndEveryLocationBelowIt() {
		List<Integer> partyIds = stockManagementService.getCompletePartyList(parentLocation.getId()).stream()
		        .map(PartyDTO::getId).collect(Collectors.toList());
		assertEquals(3, partyIds.size());
		assertTrue(partyIds.containsAll(Arrays.asList(parentParty.getId(), childParty.getId(), grandchildParty.getId())));
	}
	
	@Test
	public void getCompleteStockDispensingLocationPartyList_shouldListTheDispensingLocationsBelowIt() {
		LocationService locationService = Context.getLocationService();
		LocationTag dispensary = locationService.getLocationTagByName(StockLocationTags.DISPENSARY_LOCATION_TAG);
		if (dispensary == null) {
			dispensary = locationService
			        .saveLocationTag(new LocationTag(StockLocationTags.DISPENSARY_LOCATION_TAG, "Dispensary"));
		}
		for (Integer locationId : Arrays.asList(childLocation.getId(), grandchildLocation.getId())) {
			Location location = locationService.getLocation(locationId);
			location.addTag(dispensary);
			locationService.saveLocation(location);
		}
		List<Integer> partyIds = stockManagementService.getCompleteStockDispensingLocationPartyList(parentLocation.getId())
		        .stream().map(PartyDTO::getId).collect(Collectors.toList());
		assertEquals(2, partyIds.size());
		assertTrue(partyIds.containsAll(Arrays.asList(childParty.getId(), grandchildParty.getId())));
	}
	
	// ---------------------------------------------------------------------------------------------
	// Query 3: Stock balances including child locations
	// ---------------------------------------------------------------------------------------------
	
	@Test
	public void stockBalance_shouldAggregateBalancesIncludingChildLocations() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		
		// Receive stock at parent (10), child (20), grandchild (3 packs of 10), plus an expired batch at child
		StockItemPackagingUOM packOfTen = createPackagingUOM(sampleStockItem, BigDecimal.TEN);
		createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(10), "BATCH-P");
		createAndCompleteReceipt(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(20), "BATCH-C");
		createAndCompleteReceipt(grandchildParty, sampleStockItem, packOfTen, BigDecimal.valueOf(3), "BATCH-G");
		StockOperationDTO expired = createAndCompleteReceipt(childParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(5), "BATCH-X");
		stockManagementService.getStockOperationByUuid(expired.getUuid()).getStockOperationItems().iterator().next()
		        .getStockBatch().setExpiration(DateUtils.addDays(new Date(), -1));
		Context.flushSession();
		
		// 1. Stock balance with descendants: parent includes child + grandchild (10 + 20 + 30 = 60), child includes grandchild (20 + 30 = 50)
		StockRuleCurrentQuantity parentWithDesc = new StockRuleCurrentQuantity(sampleStockItem.getId(),
		        parentLocation.getId(), null, true);
		StockRuleCurrentQuantity childWithDesc = new StockRuleCurrentQuantity(sampleStockItem.getId(), childLocation.getId(),
		        null, true);
		StockRuleCurrentQuantity grandchildWithDesc = new StockRuleCurrentQuantity(sampleStockItem.getId(),
		        grandchildLocation.getId(), null, true);
		
		List<StockRuleCurrentQuantity> descendantQuantities = new ArrayList<>();
		descendantQuantities.add(parentWithDesc);
		descendantQuantities.add(childWithDesc);
		descendantQuantities.add(grandchildWithDesc);
		
		stockManagementService.setStockItemCurrentBalanceWithDescendants(descendantQuantities);
		
		assertNotNull(parentWithDesc.getQuantity(), "Parent balance with descendants should be calculated");
		assertEquals(0, BigDecimal.valueOf(60).compareTo(parentWithDesc.getQuantity()),
		    "Parent balance with descendants should be 60 (10 + 20 + 30)");
		assertNotNull(childWithDesc.getQuantity(), "Child balance with descendants should be calculated");
		assertEquals(0, BigDecimal.valueOf(50).compareTo(childWithDesc.getQuantity()),
		    "Child balance with descendants should be 50 (20 + 30)");
		assertNotNull(grandchildWithDesc.getQuantity(), "Grandchild balance with descendants should be calculated");
		assertEquals(0, BigDecimal.valueOf(30).compareTo(grandchildWithDesc.getQuantity()),
		    "Grandchild balance with descendants should be 30");
	}
	
	@Test
	public void stockBalance_shouldStillCountStockAtARetiredChildLocation() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(10), "BATCH-RP");
		createAndCompleteReceipt(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(20), "BATCH-RC");
		authenticate();
		retire(childLocation);
		StockRuleCurrentQuantity parent = new StockRuleCurrentQuantity(sampleStockItem.getId(), parentLocation.getId(), null,
		        true);
		stockManagementService.setStockItemCurrentBalanceWithDescendants(Collections.singletonList(parent));
		assertEquals(0, BigDecimal.valueOf(30).compareTo(parent.getQuantity()));
	}
	
	@Test
	public void stockBalance_shouldKeepEachItemApartAndCountBatchesWithoutAnExpiryDate() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockItem otherItem = new StockItem();
		otherItem.setDrug(Context.getConceptService().getDrug(2));
		otherItem.setConcept(otherItem.getDrug().getConcept());
		otherItem.setHasExpiration(true);
		otherItem.setCreator(Context.getAuthenticatedUser());
		otherItem.setDateCreated(new Date());
		Context.getService(StockItemService.class).saveStockItem(otherItem);
		createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(10), "BATCH-ONE");
		createAndCompleteReceipt(parentParty, otherItem, createPackagingUOM(otherItem, BigDecimal.ONE),
		    BigDecimal.valueOf(5), "BATCH-OTHER");
		StockOperationDTO noExpiry = createAndCompleteReceipt(childParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(7), "BATCH-NO-EXPIRY");
		stockManagementService.getStockOperationByUuid(noExpiry.getUuid()).getStockOperationItems().iterator().next()
		        .getStockBatch().setExpiration(null);
		Context.flushSession();
		StockRuleCurrentQuantity sample = new StockRuleCurrentQuantity(sampleStockItem.getId(), parentLocation.getId(),
		        BigDecimal.ZERO, true);
		StockRuleCurrentQuantity other = new StockRuleCurrentQuantity(otherItem.getId(), parentLocation.getId(),
		        BigDecimal.ZERO, true);
		stockManagementService.setStockItemCurrentBalanceWithDescendants(Arrays.asList(sample, other));
		assertEquals(0, BigDecimal.valueOf(17).compareTo(sample.getQuantity()));
		assertEquals(0, BigDecimal.valueOf(5).compareTo(other.getQuantity()));
	}
	
	@Test
	public void stockBalance_shouldExcludeBatchesExpiringTodayReflectIssuesAndHandleEmptyInputs() {
		stockManagementService.setStockItemCurrentBalanceWithDescendants(null);
		stockManagementService.setStockItemCurrentBalanceWithDescendants(Collections.emptyList());
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO parentReceipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(25), "BATCH-ACTIVE");
		StockOperationDTO expiringToday = createAndCompleteReceipt(grandchildParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(5), "BATCH-TODAY");
		stockManagementService.getStockOperationByUuid(expiringToday.getUuid()).getStockOperationItems().iterator().next()
		        .getStockBatch().setExpiration(DateUtil.today());
		Context.flushSession();
		
		// Issue 10 from parent -> child and leave in SUBMITTED so source is deducted (-10) while destination is not yet credited
		String batchUuid = stockManagementService.getStockOperationByUuid(parentReceipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		submitIssueOfTen(batchUuid);
		
		StockRuleCurrentQuantity parent = new StockRuleCurrentQuantity(sampleStockItem.getId(), parentLocation.getId(),
		        BigDecimal.ZERO, true);
		StockRuleCurrentQuantity child = new StockRuleCurrentQuantity(sampleStockItem.getId(), childLocation.getId(),
		        BigDecimal.ZERO, true);
		StockRuleCurrentQuantity grandchildNull = new StockRuleCurrentQuantity(sampleStockItem.getId(),
		        grandchildLocation.getId(), null, true);
		stockManagementService.setStockItemCurrentBalanceWithDescendants(Arrays.asList(parent, child, grandchildNull));
		
		assertEquals(0, BigDecimal.valueOf(15).compareTo(parent.getQuantity()));
		assertEquals(0, BigDecimal.ZERO.compareTo(child.getQuantity()));
		assertNull(grandchildNull.getQuantity());
	}
	
	@Test
	public void stockBalance_shouldOnlyShowAScopedUserTheStockAtTheirLocations() {
		User childOnly = createTestUser("bal_child_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(childOnly, managerRole, childLocation, false, StockOperationType.RECEIPT);
		Role operationsOnly = createRoleWithPrivileges("Stock Operations Only",
		    Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS);
		childOnly.addRole(operationsOnly);
		Context.getUserService().saveUser(childOnly);
		grantScope(childOnly, operationsOnly, parentLocation, false, StockOperationType.RECEIPT);
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.TEN, "BATCH-SCOPE-P");
		createAndCompleteReceipt(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(5), "BATCH-SCOPE-C");
		Context.authenticate(childOnly.getUsername(), "TestPassword123!");
		for (List<Integer> parties : Arrays.<List<Integer>> asList(null,
		    Arrays.asList(parentParty.getId(), childParty.getId()))) {
			StockItemInventorySearchFilter filter = new StockItemInventorySearchFilter();
			filter.setItemGroupFilters(Collections.singletonList(
			    new StockItemInventorySearchFilter.ItemGroupFilter(parties, sampleStockItem.getId(), null)));
			assertEquals(0, BigDecimal.valueOf(5).compareTo(stockManagementService.getStockInventory(filter).getData()
			        .stream().map(StockItemInventory::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)));
		}
		StockItemTransactionSearchFilter transactions = new StockItemTransactionSearchFilter();
		transactions.setStockItemId(sampleStockItem.getId());
		assertEquals(Collections.singletonList(childParty.getUuid()),
		    stockManagementService.findStockItemTransactions(transactions).getData().stream().map(t -> t.getPartyUuid())
		            .collect(Collectors.toList()));
	}
	
	// ---------------------------------------------------------------------------------------------
	// Stock Operation Lifecycle: Submit, Approve, Dispatch, Complete, Reject, Return, Cancel
	// ---------------------------------------------------------------------------------------------
	
	@Test
	public void stockOperationLifecycle_shouldSubmitAndApproveOperation() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		
		// Create a receipt operation at child location (access granted through parent location)
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(15));
		StockOperation saved = stockManagementService.saveStockOperation(dto);
		assertNotNull(saved.getUuid());
		assertEquals(StockOperationStatus.NEW, saved.getStatus());
		
		dto.setUuid(saved.getUuid());
		Context.flushSession();
		Context.clearSession();
		
		// 1. SUBMIT
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		StockOperation submitted = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.SUBMITTED, submitted.getStatus());
		assertNotNull(submitted.getSubmittedBy(), "submittedBy must be recorded");
		assertNotNull(submitted.getSubmittedDate(), "submittedDate must be recorded");
		
		// 2. APPROVE
		stockManagementService.approveStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		StockOperation approved = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.COMPLETED, approved.getStatus());
		assertNotNull(approved.getCompletedBy(), "completedBy must be recorded");
		assertNotNull(approved.getCompletedDate(), "completedDate must be recorded");
	}
	
	@Test
	public void stockOperationLifecycle_shouldSubmitDispatchAndCompleteOperationRequiringDispatch() {
		User sourceOnly = createTestUser("source_only_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(sourceOnly, managerRole, parentLocation, false, StockOperationType.STOCK_ISSUE);
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		
		// First ensure parent location has stock
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-ISSUE-INIT");
		StockOperation receiptOp = stockManagementService.getStockOperationByUuid(receipt.getUuid());
		String batchUuid = receiptOp.getStockOperationItems().iterator().next().getStockBatch().getUuid();
		
		// Create Stock Issue operation from parentLocation to childLocation
		StockOperationDTO issueDto = new StockOperationDTO();
		issueDto.setOperationType(StockOperationType.STOCK_ISSUE.getOperationType());
		issueDto.setOperationDate(DateUtils.addDays(new Date(), -1));
		issueDto.setSourceUuid(parentParty.getUuid());
		issueDto.setDestinationUuid(childParty.getUuid());
		issueDto.setAtLocationUuid(parentLocation.getUuid());
		
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(sampleStockItem.getUuid());
		item.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		item.setStockBatchUuid(batchUuid);
		item.setQuantity(BigDecimal.valueOf(10));
		issueDto.setStockOperationItems(Collections.singletonList(item));
		
		StockOperation saved = stockManagementService.saveStockOperation(issueDto);
		issueDto.setUuid(saved.getUuid());
		assertEquals(StockOperationStatus.NEW, saved.getStatus());
		Context.flushSession();
		Context.clearSession();
		
		// 1. SUBMIT
		stockManagementService.submitStockOperation(issueDto);
		Context.flushSession();
		Context.clearSession();
		StockOperation submitted = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.SUBMITTED, submitted.getStatus());
		
		// 2. DISPATCH
		stockManagementService.dispatchStockOperation(issueDto);
		Context.flushSession();
		Context.clearSession();
		StockOperation dispatched = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.DISPATCHED, dispatched.getStatus());
		assertNotNull(dispatched.getDispatchedBy(), "dispatchedBy must be recorded");
		assertNotNull(dispatched.getDispatchedDate(), "dispatchedDate must be recorded");
		
		// 3. COMPLETE
		Context.authenticate(sourceOnly.getUsername(), "TestPassword123!");
		try {
			stockManagementService.completeStockOperation(issueDto);
			fail("Only a user scoped at the destination can receive the items");
		}
		catch (StockManagementException e) {
			assertEquals(Context.getMessageSourceService().getMessage("stockmanagement.stockoperation.nopermission"),
			    e.getMessage());
		}
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.DISPATCHED,
		    stockManagementService.getStockOperationByUuid(saved.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		stockManagementService.completeStockOperation(issueDto);
		Context.flushSession();
		Context.clearSession();
		StockOperation completed = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.COMPLETED, completed.getStatus());
		assertNotNull(completed.getCompletedBy(), "completedBy must be recorded");
		assertNotNull(completed.getCompletedDate(), "completedDate must be recorded");
		
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)), "Parent should keep 40 after issuing 10");
		assertEquals(0, BigDecimal.TEN.compareTo(balanceAt(childParty)), "Child should receive the 10 issued");
	}
	
	@Test
	public void stockOperationLifecycle_shouldMoveEveryLineOfAnIssue() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receiptA = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.TEN, "BATCH-LINE-A");
		StockOperationDTO receiptB = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(5), "BATCH-LINE-B");
		String batchA = stockManagementService.getStockOperationByUuid(receiptA.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		String batchB = stockManagementService.getStockOperationByUuid(receiptB.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		
		StockOperationDTO issue = new StockOperationDTO();
		issue.setOperationType(StockOperationType.STOCK_ISSUE.getOperationType());
		issue.setOperationDate(DateUtils.addDays(new Date(), -1));
		issue.setSourceUuid(parentParty.getUuid());
		issue.setDestinationUuid(childParty.getUuid());
		issue.setAtLocationUuid(parentLocation.getUuid());
		issue.setStockOperationItems(
		    Arrays.asList(newBatchItem(batchA, BigDecimal.valueOf(4)), newBatchItem(batchB, BigDecimal.valueOf(5))));
		issue.setUuid(stockManagementService.saveStockOperation(issue).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.submitStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(6).compareTo(balanceAt(parentParty)));
		
		stockManagementService.dispatchStockOperation(issue);
		stockManagementService.completeStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(6).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.valueOf(9).compareTo(balanceAt(childParty)));
	}
	
	private BigDecimal balanceAt(Party party) {
		StockItemInventorySearchFilter filter = new StockItemInventorySearchFilter();
		filter.setItemGroupFilters(Collections.singletonList(new StockItemInventorySearchFilter.ItemGroupFilter(
		        Collections.singletonList(party.getId()), sampleStockItem.getId(), null)));
		return stockManagementService.getStockInventory(filter, null).getData().stream().map(StockItemInventory::getQuantity)
		        .reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
	@Test
	public void stockOperationLifecycle_shouldGiveTheReservedStockBackToTheSourceWhenAnIssueIsRejectedOrReturned() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-UNDO");
		String batchUuid = stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		StockOperationDTO rejected = submitIssueOfTen(batchUuid);
		StockOperationDTO returned = submitIssueOfTen(batchUuid);
		assertEquals(0, BigDecimal.valueOf(30).compareTo(balanceAt(parentParty)));
		
		stockManagementService.rejectStockOperation(rejected, "Not needed");
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		
		stockManagementService.returnStockOperation(returned, "Wrong quantity");
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(50).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
	}
	
	private StockOperationDTO submitIssueOfTen(String batchUuid) {
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(sampleStockItem.getUuid());
		item.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		item.setStockBatchUuid(batchUuid);
		item.setQuantity(BigDecimal.TEN);
		StockOperationDTO issue = new StockOperationDTO();
		issue.setOperationType(StockOperationType.STOCK_ISSUE.getOperationType());
		issue.setOperationDate(DateUtils.addDays(new Date(), -1));
		issue.setSourceUuid(parentParty.getUuid());
		issue.setDestinationUuid(childParty.getUuid());
		issue.setAtLocationUuid(parentLocation.getUuid());
		issue.setStockOperationItems(Collections.singletonList(item));
		issue.setUuid(stockManagementService.saveStockOperation(issue).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.submitStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		return issue;
	}
	
	@Test
	public void stockOperationLifecycle_shouldRefuseActionsTheStatusDoesNotAllow() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-STATUS");
		StockOperationDTO issue = submitIssueOfTen(stockManagementService.getStockOperationByUuid(receipt.getUuid())
		        .getStockOperationItems().iterator().next().getStockBatch().getUuid());
		assertRefused("notupdateable", () -> stockManagementService.submitStockOperation(issue));
		assertRefused("approvalactionnotallowed", () -> stockManagementService.approveStockOperation(issue));
		assertEquals(StockOperationStatus.SUBMITTED,
		    stockManagementService.getStockOperationByUuid(issue.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
		stockManagementService.dispatchStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertRefused("notdispatcheable", () -> stockManagementService.dispatchStockOperation(issue));
		assertRefused("approvalactionnotallowed", () -> stockManagementService.rejectStockOperation(issue, "No"));
		assertRefused("cancelactionnotallowed", () -> stockManagementService.cancelStockOperation(issue, "No"));
		assertEquals(StockOperationStatus.DISPATCHED,
		    stockManagementService.getStockOperationByUuid(issue.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
		stockManagementService.completeStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertRefused("notupdateable", () -> stockManagementService.completeStockOperation(issue));
		assertRefused("approvalactionnotallowed", () -> stockManagementService.returnStockOperation(issue, "No"));
		assertRefused("cancelactionnotallowed", () -> stockManagementService.cancelStockOperation(issue, "No"));
		assertEquals(StockOperationStatus.COMPLETED,
		    stockManagementService.getStockOperationByUuid(issue.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.TEN.compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldNotChangeBalancesWhenIssueSubmissionExceedsAvailableStock() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-INSUFFICIENT");
		String batchUuid = stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(sampleStockItem.getUuid());
		item.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		item.setStockBatchUuid(batchUuid);
		item.setQuantity(BigDecimal.valueOf(51));
		StockOperationDTO issue = new StockOperationDTO();
		issue.setOperationType(StockOperationType.STOCK_ISSUE.getOperationType());
		issue.setOperationDate(DateUtils.addDays(new Date(), -1));
		issue.setSourceUuid(parentParty.getUuid());
		issue.setDestinationUuid(childParty.getUuid());
		issue.setAtLocationUuid(parentLocation.getUuid());
		issue.setStockOperationItems(Collections.singletonList(item));
		issue.setUuid(stockManagementService.saveStockOperation(issue).getUuid());
		Context.flushSession();
		Context.clearSession();
		
		for (Runnable action : Arrays.<Runnable> asList(() -> stockManagementService.submitStockOperation(issue),
		    () -> stockManagementService.dispatchStockOperation(issue))) {
			try {
				action.run();
				fail("Expected insufficient stock balance exception");
			}
			catch (StockManagementException e) {
				assertTrue(e.getMessage().contains("negative balance"));
			}
		}
		assertRefused("notupdateable", () -> stockManagementService.completeStockOperation(issue));
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.NEW, stockManagementService.getStockOperationByUuid(issue.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(50).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
		
		StockOperationItemDTO packs = newBatchItem(batchUuid, BigDecimal.valueOf(6));
		packs.setStockItemPackagingUOMUuid(createPackagingUOM(sampleStockItem, BigDecimal.TEN).getUuid());
		assertSubmitRefusedAtSource(StockOperationType.STOCK_ISSUE, parentParty, childParty, parentLocation, null, packs);
	}
	
	private void assertRefused(String messageKey, Runnable action) {
		try {
			action.run();
			fail("Expected " + messageKey);
		}
		catch (StockManagementException e) {
			assertEquals(Context.getMessageSourceService().getMessage("stockmanagement.stockoperation." + messageKey),
			    e.getMessage());
		}
	}
	
	@Test
	public void stockOperationLifecycle_shouldLetTheDestinationReceiveOrReturnADispatchedIssue() {
		Role receiverRole = createRoleWithPrivileges("Stock Receiver", Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS);
		User receiver = createTestUser("receiver_" + UUID.randomUUID().toString().substring(0, 8), receiverRole);
		grantScope(receiver, receiverRole, childLocation, false, StockOperationType.STOCK_ISSUE);
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-BACK");
		String batchUuid = stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		StockOperationDTO received = submitIssueOfTen(batchUuid);
		StockOperationDTO returned = submitIssueOfTen(batchUuid);
		stockManagementService.dispatchStockOperation(received);
		stockManagementService.dispatchStockOperation(returned);
		Context.flushSession();
		Context.clearSession();
		Context.authenticate(receiver.getUsername(), "TestPassword123!");
		stockManagementService.completeStockOperation(received);
		stockManagementService.returnStockOperation(returned, "Damaged");
		Context.flushSession();
		Context.clearSession();
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.TEN.compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldSubmitAndRejectOperationWithReason() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(5));
		StockOperation saved = stockManagementService.saveStockOperation(dto);
		dto.setUuid(saved.getUuid());
		Context.flushSession();
		Context.clearSession();
		
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.SUBMITTED,
		    stockManagementService.getStockOperationByUuid(saved.getUuid()).getStatus());
		
		// REJECT
		String reason = "Damaged items on receipt";
		stockManagementService.rejectStockOperation(dto, reason);
		Context.flushSession();
		Context.clearSession();
		
		StockOperation rejected = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.REJECTED, rejected.getStatus());
		assertNotNull(rejected.getRejectedBy(), "rejectedBy must be recorded");
		assertNotNull(rejected.getRejectedDate(), "rejectedDate must be recorded");
		assertEquals(reason, rejected.getRejectionReason(), "Rejection reason must match");
	}
	
	@Test
	public void stockOperationLifecycle_shouldSubmitReturnAndResubmitOperation() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(8));
		StockOperation saved = stockManagementService.saveStockOperation(dto);
		dto.setUuid(saved.getUuid());
		Context.flushSession();
		Context.clearSession();
		
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		
		// RETURN
		String reason = "Please correct line item quantity";
		stockManagementService.returnStockOperation(dto, reason);
		Context.flushSession();
		Context.clearSession();
		
		StockOperation returned = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.RETURNED, returned.getStatus());
		assertNotNull(returned.getReturnedBy(), "returnedBy must be recorded");
		assertNotNull(returned.getReturnedDate(), "returnedDate must be recorded");
		assertEquals(reason, returned.getReturnReason(), "Return reason must match");
		assertTrue(returned.isUpdateable(), "Returned operation must be updateable again");
		
		// Edit returned operation quantity (8 -> 12) before re-submitting and approving
		StockOperationItem existingItem = returned.getStockOperationItems().iterator().next();
		dto.getStockOperationItems().iterator().next().setUuid(existingItem.getUuid());
		dto.getStockOperationItems().iterator().next().setQuantity(BigDecimal.valueOf(12));
		stockManagementService.saveStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		
		// Re-submit returned operation
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		StockOperation resubmitted = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.SUBMITTED, resubmitted.getStatus());
		
		stockManagementService.approveStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.COMPLETED,
		    stockManagementService.getStockOperationByUuid(saved.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(12).compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldCancelOperationWithReason() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		
		// Cancel from SUBMITTED status
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(12));
		StockOperation saved = stockManagementService.saveStockOperation(dto);
		dto.setUuid(saved.getUuid());
		Context.flushSession();
		Context.clearSession();
		
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		
		String reason = "Cancelled by supervisor request";
		stockManagementService.cancelStockOperation(dto, reason);
		Context.flushSession();
		Context.clearSession();
		
		StockOperation cancelled = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.CANCELLED, cancelled.getStatus());
		assertNotNull(cancelled.getCancelledBy(), "cancelledBy must be recorded");
		assertNotNull(cancelled.getCancelledDate(), "cancelledDate must be recorded");
		assertEquals(reason, cancelled.getCancelReason(), "Cancel reason must match");
		
		// Cancel directly from NEW status
		StockOperationDTO dtoNew = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(3));
		StockOperation savedNew = stockManagementService.saveStockOperation(dtoNew);
		dtoNew.setUuid(savedNew.getUuid());
		Context.flushSession();
		Context.clearSession();
		
		stockManagementService.cancelStockOperation(dtoNew, "Cancelled immediately");
		Context.flushSession();
		Context.clearSession();
		StockOperation cancelledNew = stockManagementService.getStockOperationByUuid(savedNew.getUuid());
		assertEquals(StockOperationStatus.CANCELLED, cancelledNew.getStatus());
	}
	
	@Test
	public void stockOperationLifecycle_shouldGiveTheReservedStockBackToTheSourceWhenAnIssueIsCancelled() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-CANCEL");
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(sampleStockItem.getUuid());
		item.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		item.setStockBatchUuid(stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid());
		item.setQuantity(BigDecimal.TEN);
		StockOperationDTO issue = new StockOperationDTO();
		issue.setOperationType(StockOperationType.STOCK_ISSUE.getOperationType());
		issue.setOperationDate(DateUtils.addDays(new Date(), -1));
		issue.setSourceUuid(parentParty.getUuid());
		issue.setDestinationUuid(childParty.getUuid());
		issue.setAtLocationUuid(parentLocation.getUuid());
		issue.setStockOperationItems(Collections.singletonList(item));
		issue.setUuid(stockManagementService.saveStockOperation(issue).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.submitStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		
		stockManagementService.cancelStockOperation(issue, "Not needed");
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(50).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldEnforcePermissionsInheritedThroughParentLocation() {
		// User A has scope at parentLocation with descendants enabled -> can process at childLocation
		User userA = createTestUser("user_a_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(userA, managerRole, parentLocation, true, StockOperationType.RECEIPT);
		
		// User B has scope only at grandchildLocation (not parent or child)
		User userB = createTestUser("user_b_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(userB, managerRole, grandchildLocation, false, StockOperationType.RECEIPT);
		
		// Switch auth context to User A
		Context.authenticate(userA.getUsername(), "TestPassword123!");
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(7));
		StockOperation saved = stockManagementService.saveStockOperation(dto);
		dto.setUuid(saved.getUuid());
		Context.flushSession();
		Context.clearSession();
		
		// User A can submit operation at child location
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.SUBMITTED,
		    stockManagementService.getStockOperationByUuid(saved.getUuid()).getStatus());
		
		// Switch auth context to User B -> cannot approve or process operation at childLocation
		Context.authenticate(userB.getUsername(), "TestPassword123!");
		assertRefused("nopermission", () -> stockManagementService.approveStockOperation(dto));
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.SUBMITTED,
		    stockManagementService.getStockOperationByUuid(saved.getUuid()).getStatus());
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldNotApproveWhereTheScopedRoleLacksApprove() {
		Role submitterRole = createRoleWithPrivileges("Stock Operations Submitter",
		    Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		User user = createTestUser("two_roles_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		user.addRole(submitterRole);
		Context.getUserService().saveUser(user);
		grantScope(user, managerRole, grandchildLocation, false, StockOperationType.RECEIPT);
		grantScope(user, submitterRole, childLocation, false, StockOperationType.RECEIPT);
		Context.authenticate(user.getUsername(), "TestPassword123!");
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(4));
		dto.setUuid(stockManagementService.saveStockOperation(dto).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		for (Runnable action : Arrays.<Runnable> asList(() -> stockManagementService.approveStockOperation(dto),
		    () -> stockManagementService.rejectStockOperation(dto, "No"),
		    () -> stockManagementService.returnStockOperation(dto, "No"),
		    () -> stockManagementService.cancelStockOperation(dto, "No"))) {
			try {
				action.run();
				fail("The role scoped to the child location can only submit");
			}
			catch (StockManagementException e) {
				assertEquals(Context.getMessageSourceService().getMessage("stockmanagement.stockoperation.nopermission"),
				    e.getMessage());
			}
		}
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.SUBMITTED,
		    stockManagementService.getStockOperationByUuid(dto.getUuid()).getStatus());
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldNotSubmitWhereTheScopedRoleLacksMutate() {
		Role viewerRole = createRoleWithPrivileges("Stock Operations Viewer",
		    Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS);
		User user = createTestUser("viewer_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		user.addRole(viewerRole);
		Context.getUserService().saveUser(user);
		grantScope(user, managerRole, grandchildLocation, false, StockOperationType.RECEIPT);
		grantScope(user, viewerRole, childLocation, false, StockOperationType.RECEIPT);
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(4));
		dto.setUuid(stockManagementService.saveStockOperation(dto).getUuid());
		Context.flushSession();
		Context.clearSession();
		Context.authenticate(user.getUsername(), "TestPassword123!");
		assertRefused("nopermission", () -> stockManagementService.submitStockOperation(dto));
	}
	
	@Test
	public void stockOperationLifecycle_shouldNotDispatchASubmittedIssueWithoutApprove() {
		Role submitterRole = createRoleWithPrivileges("Stock Operations Submitter",
		    Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		User submitter = createTestUser("dispatcher_" + UUID.randomUUID().toString().substring(0, 8), submitterRole);
		grantScope(submitter, submitterRole, parentLocation, false, StockOperationType.STOCK_ISSUE);
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-DISPATCH");
		StockOperationDTO issue = submitIssueOfTen(stockManagementService.getStockOperationByUuid(receipt.getUuid())
		        .getStockOperationItems().iterator().next().getStockBatch().getUuid());
		Context.authenticate(submitter.getUsername(), "TestPassword123!");
		assertRefused("nopermission", () -> stockManagementService.dispatchStockOperation(issue));
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.SUBMITTED,
		    stockManagementService.getStockOperationByUuid(issue.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldSupportPartialReceiveAtDestinationAndRejectExcessQuantity() {
		Context.getAdministrationService()
		        .saveGlobalProperty(new GlobalProperty("stockmanagement.excessReceivedItemThreshold", "25"));
		Role receiverRole = createRoleWithPrivileges("Stock Receiver", Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
		    Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS, PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		User receiver = createTestUser("part_recv_" + UUID.randomUUID().toString().substring(0, 8), receiverRole);
		grantScope(receiver, receiverRole, childLocation, false, StockOperationType.STOCK_ISSUE);
		User sourceOnly = createTestUser("part_src_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(sourceOnly, managerRole, parentLocation, false, StockOperationType.STOCK_ISSUE);
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.valueOf(50), "BATCH-PARTIAL");
		String batchUuid = stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		StockOperationDTO issue = submitIssueOfTen(batchUuid);
		stockManagementService.dispatchStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		
		String issueItemUuid = stockManagementService.getStockOperationByUuid(issue.getUuid()).getStockOperationItems()
		        .iterator().next().getUuid();
		StockOperationActionLineItem lineItem = new StockOperationActionLineItem();
		lineItem.setUuid(issueItemUuid);
		lineItem.setPackagingUoMUuId(samplePackagingUom.getUuid());
		lineItem.setAmount(BigDecimal.valueOf(7));
		
		Context.authenticate(sourceOnly.getUsername(), "TestPassword123!");
		assertRefused("nopermission",
		    () -> stockManagementService.stockOperationItemsReceived(issue, Collections.singletonList(lineItem)));
		
		Context.authenticate(receiver.getUsername(), "TestPassword123!");
		lineItem.setAmount(BigDecimal.valueOf(-1));
		assertRefused("nosupportreceivenegativeamount",
		    () -> stockManagementService.stockOperationItemsReceived(issue, Collections.singletonList(lineItem)));
		
		BigDecimal threshold = BigDecimal.valueOf(25);
		BigDecimal overThreshold = BigDecimal.TEN
		        .multiply(
		            BigDecimal.valueOf(100).add(threshold).divide(BigDecimal.valueOf(100), 5, BigDecimal.ROUND_HALF_EVEN))
		        .add(BigDecimal.ONE);
		lineItem.setAmount(overThreshold);
		try {
			stockManagementService.stockOperationItemsReceived(issue, Collections.singletonList(lineItem));
			fail("Expected cannotreceivemorethansent");
		}
		catch (StockManagementException e) {
			assertEquals(String.format(
			    Context.getMessageSourceService().getMessage("stockmanagement.stockoperation.cannotreceivemorethansent"),
			    threshold), e.getMessage());
		}
		
		lineItem.setAmount(BigDecimal.valueOf(7));
		stockManagementService.stockOperationItemsReceived(issue, Collections.singletonList(lineItem));
		stockManagementService.completeStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		assertEquals(StockOperationStatus.COMPLETED,
		    stockManagementService.getStockOperationByUuid(issue.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(40).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.valueOf(7).compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldAllowDirectCompleteAndDirectDispatchFromNewWithMutatePrivilege() {
		Role mutateOnlyRole = createRoleWithPrivileges("Stock Mutate Only Operator",
		    Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS, Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE,
		    PrivilegeConstants.GET_LOCATIONS, PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		User mutateOnly = createTestUser("mutate_only_" + UUID.randomUUID().toString().substring(0, 8), mutateOnlyRole);
		grantScope(mutateOnly, mutateOnlyRole, parentLocation, true, StockOperationType.RECEIPT);
		grantScope(mutateOnly, mutateOnlyRole, parentLocation, true, StockOperationType.STOCK_ISSUE);
		
		Context.authenticate(mutateOnly.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = newReceiptDto(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(20));
		receipt.setUuid(stockManagementService.saveStockOperation(receipt).getUuid());
		Context.flushSession();
		Context.clearSession();
		
		stockManagementService.completeStockOperation(receipt);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.COMPLETED,
		    stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStatus());
		
		String batchUuid = stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(sampleStockItem.getUuid());
		item.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		item.setStockBatchUuid(batchUuid);
		item.setQuantity(BigDecimal.valueOf(5));
		StockOperationDTO issue = new StockOperationDTO();
		issue.setOperationType(StockOperationType.STOCK_ISSUE.getOperationType());
		issue.setOperationDate(DateUtils.addDays(new Date(), -1));
		issue.setSourceUuid(parentParty.getUuid());
		issue.setDestinationUuid(childParty.getUuid());
		issue.setAtLocationUuid(parentLocation.getUuid());
		issue.setStockOperationItems(Collections.singletonList(item));
		issue.setUuid(stockManagementService.saveStockOperation(issue).getUuid());
		Context.flushSession();
		Context.clearSession();
		
		stockManagementService.dispatchStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.DISPATCHED,
		    stockManagementService.getStockOperationByUuid(issue.getUuid()).getStatus());
		
		StockOperationDTO unwanted = newReceiptDto(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.ONE);
		unwanted.setUuid(stockManagementService.saveStockOperation(unwanted).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.cancelStockOperation(unwanted, "Not needed");
		assertEquals(StockOperationStatus.CANCELLED,
		    stockManagementService.getStockOperationByUuid(unwanted.getUuid()).getStatus());
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO returned = newReceiptDto(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.ONE);
		returned.setUuid(stockManagementService.saveStockOperation(returned).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.submitStockOperation(returned);
		Context.flushSession();
		Context.clearSession();
		stockManagementService.returnStockOperation(returned, "Wrong quantity");
		Context.flushSession();
		Context.clearSession();
		Context.authenticate(mutateOnly.getUsername(), "TestPassword123!");
		stockManagementService.cancelStockOperation(returned, "Not needed");
		assertEquals(StockOperationStatus.CANCELLED,
		    stockManagementService.getStockOperationByUuid(returned.getUuid()).getStatus());
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		assertEquals(0, BigDecimal.valueOf(15).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(childParty)));
		
		stockManagementService.completeStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(15).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.valueOf(5).compareTo(balanceAt(childParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldCompleteOperationsAcrossAllRemainingOperationTypes() {
		for (StockOperationType type : Arrays.asList(StockOperationType.ADJUSTMENT, StockOperationType.DISPOSED,
		    StockOperationType.TRANSFER_OUT, StockOperationType.RETURN, StockOperationType.INITIAL,
		    StockOperationType.REQUISITION, StockOperationType.STOCKTAKE)) {
			grantScope(clerkUser, managerRole, parentLocation, true, type);
		}
		Party externalParty = createExternalParty();
		String reasonUuid = Context.getConceptService().getConcept(1067).getUuid();
		Context.flushSession();
		Context.clearSession();
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		
		// 1. INITIAL (+30 at source)
		StockOperationItemDTO initialItem = new StockOperationItemDTO();
		initialItem.setStockItemUuid(sampleStockItem.getUuid());
		initialItem.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		initialItem.setBatchNo("BATCH-TYPES");
		initialItem.setExpiration(DateUtils.addDays(new Date(), 180));
		initialItem.setQuantity(BigDecimal.valueOf(30));
		StockOperationDTO initial = submitAndApprove(StockOperationType.INITIAL, parentParty, null, parentLocation, null,
		    initialItem);
		assertEquals(0, BigDecimal.valueOf(30).compareTo(balanceAt(parentParty)));
		String batchUuid = stockManagementService.getStockOperationByUuid(initial.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch().getUuid();
		
		// 2. ADJUSTMENT (-5 at source -> 25)
		submitAndApprove(StockOperationType.ADJUSTMENT, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(-5)));
		assertEquals(0, BigDecimal.valueOf(25).compareTo(balanceAt(parentParty)));
		
		// 3. DISPOSED (4 removed from source -> 21)
		submitAndApprove(StockOperationType.DISPOSED, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(4)));
		assertEquals(0, BigDecimal.valueOf(21).compareTo(balanceAt(parentParty)));
		
		// 4. STOCKTAKE (counted 26 -> adjusted +5 -> 26)
		submitAndApprove(StockOperationType.STOCKTAKE, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(26)));
		assertEquals(0, BigDecimal.valueOf(26).compareTo(balanceAt(parentParty)));
		
		// 5. TRANSFER_OUT (6 from parent -> externalParty -> parent 20, external 6)
		submitAndApprove(StockOperationType.TRANSFER_OUT, parentParty, externalParty, parentLocation, null,
		    newBatchItem(batchUuid, BigDecimal.valueOf(6)));
		assertEquals(0, BigDecimal.valueOf(20).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.valueOf(6).compareTo(balanceAt(externalParty)));
		
		// Move 10 to child so child holds stock of BATCH-TYPES before testing RETURN
		StockOperationDTO issue = submitIssueOfTen(batchUuid);
		stockManagementService.dispatchStockOperation(issue);
		stockManagementService.completeStockOperation(issue);
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.TEN.compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.TEN.compareTo(balanceAt(childParty)));
		
		// 6. RETURN (4 from child -> parent via SUBMIT -> DISPATCH -> COMPLETE -> parent 14, child 6)
		StockOperationDTO ret = new StockOperationDTO();
		ret.setOperationType(StockOperationType.RETURN.getOperationType());
		ret.setOperationDate(DateUtils.addDays(new Date(), -1));
		ret.setSourceUuid(childParty.getUuid());
		ret.setDestinationUuid(parentParty.getUuid());
		ret.setAtLocationUuid(childLocation.getUuid());
		ret.setStockOperationItems(Collections.singletonList(newBatchItem(batchUuid, BigDecimal.valueOf(4))));
		ret.setUuid(stockManagementService.saveStockOperation(ret).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.submitStockOperation(ret);
		stockManagementService.dispatchStockOperation(ret);
		stockManagementService.completeStockOperation(ret);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.COMPLETED,
		    stockManagementService.getStockOperationByUuid(ret.getUuid()).getStatus());
		assertEquals(0, BigDecimal.valueOf(14).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.valueOf(6).compareTo(balanceAt(childParty)));
		
		// 7. REQUISITION (completes without changing stock balances)
		StockOperationItemDTO reqItem = new StockOperationItemDTO();
		reqItem.setStockItemUuid(sampleStockItem.getUuid());
		reqItem.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		reqItem.setQuantity(BigDecimal.valueOf(3));
		submitAndApprove(StockOperationType.REQUISITION, childParty, parentParty, childLocation, null, reqItem);
		assertEquals(0, BigDecimal.valueOf(14).compareTo(balanceAt(parentParty)));
		assertEquals(0, BigDecimal.valueOf(6).compareTo(balanceAt(childParty)));
		assertEquals(0, BigDecimal.valueOf(6).compareTo(balanceAt(externalParty)));
		
		// Verify submitting and cancelling each stock-moving type reverses its source reservation
		submitAndCancel(StockOperationType.ADJUSTMENT, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(-5)));
		submitAndCancel(StockOperationType.DISPOSED, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(4)));
		submitAndCancel(StockOperationType.STOCKTAKE, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(26)));
		submitAndCancel(StockOperationType.TRANSFER_OUT, parentParty, externalParty, parentLocation, null,
		    newBatchItem(batchUuid, BigDecimal.valueOf(6)));
		submitAndCancel(StockOperationType.RETURN, childParty, parentParty, childLocation, null,
		    newBatchItem(batchUuid, BigDecimal.valueOf(4)));
		
		// Submitting more than the source holds is refused for each type that takes stock from it
		assertSubmitRefusedAtSource(StockOperationType.ADJUSTMENT, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(-15)));
		assertSubmitRefusedAtSource(StockOperationType.DISPOSED, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batchUuid, BigDecimal.valueOf(15)));
		assertSubmitRefusedAtSource(StockOperationType.TRANSFER_OUT, parentParty, externalParty, parentLocation, null,
		    newBatchItem(batchUuid, BigDecimal.valueOf(15)));
		assertSubmitRefusedAtSource(StockOperationType.RETURN, childParty, parentParty, childLocation, null,
		    newBatchItem(batchUuid, BigDecimal.valueOf(7)));
		
		// Completing a new disposal straight away checks the source the same way
		StockOperationDTO direct = new StockOperationDTO();
		direct.setOperationType(StockOperationType.DISPOSED.getOperationType());
		direct.setOperationDate(DateUtils.addDays(new Date(), -1));
		direct.setSourceUuid(parentParty.getUuid());
		direct.setAtLocationUuid(parentLocation.getUuid());
		direct.setReasonUuid(reasonUuid);
		direct.setStockOperationItems(Collections.singletonList(newBatchItem(batchUuid, BigDecimal.valueOf(15))));
		direct.setUuid(stockManagementService.saveStockOperation(direct).getUuid());
		Context.flushSession();
		Context.clearSession();
		try {
			stockManagementService.completeStockOperation(direct);
			fail("Expected a negative balance refusal for a direct complete");
		}
		catch (StockManagementException e) {
			assertTrue(e.getMessage().contains("negative balance"));
		}
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.valueOf(14).compareTo(balanceAt(parentParty)));
	}
	
	@Test
	public void stockOperationLifecycle_shouldDisposeOfStockFromAnExpiredBatch() {
		grantScope(clerkUser, managerRole, parentLocation, true, StockOperationType.DISPOSED);
		String reasonUuid = Context.getConceptService().getConcept(1067).getUuid();
		Context.flushSession();
		Context.clearSession();
		
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom,
		    BigDecimal.TEN, "BATCH-EXPIRED");
		StockBatch batch = stockManagementService.getStockOperationByUuid(receipt.getUuid()).getStockOperationItems()
		        .iterator().next().getStockBatch();
		batch.setExpiration(DateUtils.addDays(new Date(), -1));
		Context.getService(StockItemService.class).saveStockBatch(batch);
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, BigDecimal.ZERO.compareTo(balanceAt(parentParty)));
		assertSubmitRefusedAtSource(StockOperationType.STOCK_ISSUE, parentParty, childParty, parentLocation, null,
		    newBatchItem(batch.getUuid(), BigDecimal.ONE));
		
		StockItemInventorySearchFilter withExpired = new StockItemInventorySearchFilter();
		withExpired.setItemGroupFilters(Collections.singletonList(new StockItemInventorySearchFilter.ItemGroupFilter(
		        Collections.singletonList(parentParty.getId()), sampleStockItem.getId(), null)));
		withExpired.setRequireNonExpiredStockBatches(false);
		assertEquals(0, BigDecimal.TEN.compareTo(stockManagementService.getStockInventory(withExpired, null).getData()
		        .stream().map(StockItemInventory::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)));
		
		submitAndApprove(StockOperationType.DISPOSED, parentParty, null, parentLocation, reasonUuid,
		    newBatchItem(batch.getUuid(), BigDecimal.TEN));
		assertEquals(0, BigDecimal.ZERO.compareTo(stockManagementService.getStockInventory(withExpired, null).getData()
		        .stream().map(StockItemInventory::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)));
	}
	
	// ---------------------------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------------------------
	
	private Party createExternalParty() {
		Concept sourceTypeConcept = Context.getConceptService().getConcept(1067);
		StockSource stockSource = new StockSource();
		stockSource.setName("External Party " + UUID.randomUUID());
		stockSource.setAcronym("EXT");
		stockSource.setSourceType(sourceTypeConcept);
		stockSource.setVoided(false);
		stockSource.setCreator(Context.getAuthenticatedUser());
		stockSource.setDateCreated(new Date());
		dao().saveStockSource(stockSource);
		Party party = new Party();
		party.setStockSource(stockSource);
		party.setVoided(false);
		party.setCreator(Context.getAuthenticatedUser());
		party.setDateCreated(new Date());
		dao().saveParty(party);
		return party;
	}
	
	private StockOperationItemDTO newBatchItem(String batchUuid, BigDecimal quantity) {
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(sampleStockItem.getUuid());
		item.setStockItemPackagingUOMUuid(samplePackagingUom.getUuid());
		item.setStockBatchUuid(batchUuid);
		item.setQuantity(quantity);
		return item;
	}
	
	private StockOperationDTO submitOperation(StockOperationType type, Party source, Party destination, Location atLocation,
	        String reasonUuid, StockOperationItemDTO item) {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationType(type.getOperationType());
		dto.setOperationDate(DateUtils.addDays(new Date(), -1));
		if (source != null) {
			dto.setSourceUuid(source.getUuid());
		}
		if (destination != null) {
			dto.setDestinationUuid(destination.getUuid());
		}
		dto.setAtLocationUuid(atLocation.getUuid());
		dto.setReasonUuid(reasonUuid);
		dto.setStockOperationItems(Collections.singletonList(item));
		dto.setUuid(stockManagementService.saveStockOperation(dto).getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		return dto;
	}
	
	private StockOperationDTO submitAndApprove(StockOperationType type, Party source, Party destination, Location atLocation,
	        String reasonUuid, StockOperationItemDTO item) {
		StockOperationDTO dto = submitOperation(type, source, destination, atLocation, reasonUuid, item);
		stockManagementService.approveStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.COMPLETED,
		    stockManagementService.getStockOperationByUuid(dto.getUuid()).getStatus());
		return dto;
	}
	
	private void submitAndCancel(StockOperationType type, Party source, Party destination, Location atLocation,
	        String reasonUuid, StockOperationItemDTO item) {
		BigDecimal before = balanceAt(source);
		StockOperationDTO dto = submitOperation(type, source, destination, atLocation, reasonUuid, item);
		stockManagementService.cancelStockOperation(dto, "Not needed");
		Context.flushSession();
		Context.clearSession();
		assertRefused("approvalactionnotallowed", () -> stockManagementService.approveStockOperation(dto));
		assertEquals(StockOperationStatus.CANCELLED,
		    stockManagementService.getStockOperationByUuid(dto.getUuid()).getStatus());
		assertEquals(0, before.compareTo(balanceAt(source)));
	}
	
	private void assertSubmitRefusedAtSource(StockOperationType type, Party source, Party destination, Location atLocation,
	        String reasonUuid, StockOperationItemDTO item) {
		BigDecimal before = balanceAt(source);
		try {
			submitOperation(type, source, destination, atLocation, reasonUuid, item);
			fail("Expected a negative balance refusal for " + type.getOperationType());
		}
		catch (StockManagementException e) {
			assertTrue(e.getMessage().contains("negative balance"));
		}
		Context.flushSession();
		Context.clearSession();
		assertEquals(0, before.compareTo(balanceAt(source)));
	}
	
	private Role createRoleWithPrivileges(String roleName, String... privileges) {
		UserService userService = Context.getUserService();
		Role role = userService.getRole(roleName);
		if (role == null) {
			role = new Role();
			role.setRole(roleName);
			role.setDescription("Role for " + roleName);
		}
		for (String privName : privileges) {
			Privilege priv = userService.getPrivilege(privName);
			if (priv == null) {
				priv = userService.savePrivilege(new Privilege(privName, privName));
			}
			role.addPrivilege(priv);
		}
		return userService.saveRole(role);
	}
	
	private User createTestUser(String username, Role role) {
		Person person = new Person();
		person.setGender("M");
		person.addName(new PersonName("Test", null, username));
		person = Context.getPersonService().savePerson(person);
		
		User user = new User();
		user.setPerson(person);
		user.setUsername(username);
		user.addRole(role);
		return Context.getUserService().createUser(user, "TestPassword123!");
	}
	
	private UserRoleScope grantScope(User user, Role role, Location location, boolean enableDescendants,
	        StockOperationType operationType) {
		UserRoleScopeDTO dto = new UserRoleScopeDTO();
		dto.setUserUuid(user.getUuid());
		dto.setRole(role.getRole());
		dto.setPermanent(true);
		dto.setEnabled(true);
		
		UserRoleScopeLocationDTO locDto = new UserRoleScopeLocationDTO();
		locDto.setLocationUuid(location.getUuid());
		locDto.setEnableDescendants(enableDescendants);
		dto.setLocations(Collections.singletonList(locDto));
		
		if (operationType != null) {
			UserRoleScopeOperationTypeDTO opDto = new UserRoleScopeOperationTypeDTO();
			opDto.setOperationType(operationType.getOperationType());
			dto.setOperationTypes(Collections.singletonList(opDto));
		}
		
		return stockManagementService.saveUserRoleScope(dto);
	}
	
	private StockItem createStockItem(String name) {
		Drug drug = Context.getConceptService().getDrug(3);
		StockItem stockItem = new StockItem();
		stockItem.setDrug(drug);
		stockItem.setConcept(drug.getConcept());
		stockItem.setCommonName(name);
		stockItem.setHasExpiration(true);
		stockItem.setVoided(false);
		stockItem.setCreator(Context.getAuthenticatedUser());
		stockItem.setDateCreated(new Date());
		return Context.getService(StockItemService.class).saveStockItem(stockItem);
	}
	
	private StockItemPackagingUOM createPackagingUOM(StockItem stockItem, BigDecimal factor) {
		Concept packagingConcept = Context.getConceptService().getConcept(1067);
		StockItemPackagingUOM packagingUom = new StockItemPackagingUOM();
		packagingUom.setStockItem(stockItem);
		packagingUom.setPackagingUom(packagingConcept);
		packagingUom.setFactor(factor);
		packagingUom.setVoided(false);
		packagingUom.setCreator(Context.getAuthenticatedUser());
		packagingUom.setDateCreated(new Date());
		return Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
	}
	
	private StockOperationDTO newReceiptDto(Party destinationParty, StockItem stockItem, StockItemPackagingUOM uom,
	        BigDecimal quantity) {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationType(StockOperationType.RECEIPT.getOperationType());
		dto.setOperationDate(DateUtils.addDays(new Date(), -1));
		dto.setDestinationUuid(destinationParty.getUuid());
		if (destinationParty.getLocation() != null) {
			dto.setAtLocationUuid(destinationParty.getLocation().getUuid());
		}
		
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(stockItem.getUuid());
		item.setStockItemPackagingUOMUuid(uom.getUuid());
		item.setBatchNo("BATCH-" + UUID.randomUUID().toString().substring(0, 8));
		item.setExpiration(DateUtils.addDays(new Date(), 180));
		item.setQuantity(quantity);
		dto.setStockOperationItems(Collections.singletonList(item));
		return dto;
	}
	
	private StockOperationDTO createAndCompleteReceipt(Party destinationParty, StockItem stockItem,
	        StockItemPackagingUOM uom, BigDecimal quantity, String batchNo) {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationType(StockOperationType.RECEIPT.getOperationType());
		dto.setOperationDate(DateUtils.addDays(new Date(), -1));
		dto.setDestinationUuid(destinationParty.getUuid());
		if (destinationParty.getLocation() != null) {
			dto.setAtLocationUuid(destinationParty.getLocation().getUuid());
		}
		
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(stockItem.getUuid());
		item.setStockItemPackagingUOMUuid(uom.getUuid());
		item.setBatchNo(batchNo != null ? batchNo : "BATCH-" + UUID.randomUUID().toString().substring(0, 8));
		item.setExpiration(DateUtils.addDays(new Date(), 180));
		item.setQuantity(quantity);
		dto.setStockOperationItems(Collections.singletonList(item));
		
		StockOperation saved = stockManagementService.saveStockOperation(dto);
		dto.setUuid(saved.getUuid());
		Context.flushSession();
		Context.clearSession();
		stockManagementService.completeStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		return dto;
	}
	
	private void retire(Location location) {
		LocationService locationService = Context.getLocationService();
		locationService.retireLocation(locationService.getLocationByUuid(location.getUuid()), "Testing");
		Context.flushSession();
	}
}
