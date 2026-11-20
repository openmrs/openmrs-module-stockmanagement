package org.openmrs.module.stockmanagement;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.LocationService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.PrivilegeScope;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemDTO;
import org.openmrs.module.stockmanagement.api.dto.StockRuleCurrentQuantity;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeLocationDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeOperationTypeDTO;
import org.openmrs.module.stockmanagement.api.model.LocationTree;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.UserRoleScope;
import org.openmrs.module.stockmanagement.tasks.LocationTreeSynchronize;
import org.openmrs.module.stockmanagement.tasks.PartySynchronize;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Captures current behaviour before refactoring:
 * 1. Stock operation lifecycle (submit, approve, dispatch, complete, reject, return, cancel).
 * 2. Permission checks including access granted through a parent location hierarchy.
 * 3. The three queries rewritten later:
 *    - users assigned to a scope (getActiveUsersAssignedForScope)
 *    - user's flattened privilege scopes (getFlattenedUserRoleScopesByUser / userHasStockManagementPrivilege)
 *    - stock balances including child locations (setStockItemCurrentBalanceWithDescendants)
 */
public class StockManagementFlowsAndQueriesServiceTest extends BaseModuleContextSensitiveTest {

	private static final String RECEIPT_OPERATION_TYPE_UUID = "fce0b4fc-9402-424a-aacb-f99599e51a9f";

	private static final String ISSUE_OPERATION_TYPE_UUID = "db40707f-9175-4199-8df2-a5702f41ec7d";

	private StockManagementService stockManagementService;

	private Location parentLocation;

	private Location childLocation;

	private Location grandchildLocation;

	private Party parentParty;

	private Party childParty;

	private Party grandchildParty;

	private StockOperationType receiptType;

	private StockOperationType stockIssueType;

	private Role managerRole;

	private Role sysDevRole;

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

	private void setNotifyFlagsFalse(StockOperationType type) {
		type.setNotifySubmitted(false);
		type.setNotifyCompleted(false);
		type.setNotifyApproved(false);
		type.setNotifyDispatched(false);
		type.setNotifyReturned(false);
		type.setNotifyRejected(false);
		type.setNotifyCancelled(false);
	}

	@Before
	public void setUp() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);

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

		assertNotNull("Parent party must exist", parentParty);
		assertNotNull("Child party must exist", childParty);
		assertNotNull("Grandchild party must exist", grandchildParty);

		receiptType = stockManagementService.getStockOperationTypeByUuid(RECEIPT_OPERATION_TYPE_UUID);
		stockIssueType = stockManagementService.getStockOperationTypeByUuid(ISSUE_OPERATION_TYPE_UUID);

		assertNotNull("Receipt operation type must exist", receiptType);
		assertNotNull("Stock issue operation type must exist", stockIssueType);

		setNotifyFlagsFalse(receiptType);
		setNotifyFlagsFalse(stockIssueType);
		dao().saveStockOperationType(receiptType);
		dao().saveStockOperationType(stockIssueType);
		Context.flushSession();
		Context.clearSession();

		// Create test role carrying stock operations and user role scopes privileges
		managerRole = createRoleWithPrivileges("Stock Operations Manager",
				Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
				Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
				Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS,
				Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE,
				Privileges.TASK_STOCKMANAGEMENT_USERROLESCOPES_MUTATE,
				PrivilegeConstants.GET_LOCATIONS,
				PrivilegeConstants.GET_CONCEPTS,
				PrivilegeConstants.GET_USERS,
				PrivilegeConstants.GET_GLOBAL_PROPERTIES,
				PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);

		// Ensure System Developer role has stock management privileges
		sysDevRole = Context.getUserService().getRole("System Developer");
		if (sysDevRole != null) {
			for (String privName : Arrays.asList(
					Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
					Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
					Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES,
					Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE,
					Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE,
					Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS,
					Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE,
					Privileges.TASK_STOCKMANAGEMENT_USERROLESCOPES_MUTATE)) {
				Privilege priv = Context.getUserService().getPrivilege(privName);
				if (priv == null) {
					priv = Context.getUserService().savePrivilege(new Privilege(privName, privName));
				}
				sysDevRole.addPrivilege(priv);
			}
			Context.getUserService().saveRole(sysDevRole);
		}

		sampleStockItem = createStockItem("Paracetamol 500mg " + UUID.randomUUID().toString().substring(0, 8));
		samplePackagingUom = createPackagingUOM(sampleStockItem, BigDecimal.ONE);

		clerkUser = createTestUser("clerk_operator_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(clerkUser, managerRole, parentLocation, true, receiptType);
		grantScope(clerkUser, managerRole, parentLocation, true, stockIssueType);

		Context.getAdministrationService().saveGlobalProperty(
				new GlobalProperty("stockmanagement.allowStockIssueWithoutRequisition", "true"));
	}

	// ---------------------------------------------------------------------------------------------
	// Query 1: Users assigned to a scope (getActiveUsersAssignedForScope)
	// ---------------------------------------------------------------------------------------------

	@Test
	public void getActiveUsersAssignedForScope_shouldIncludeUserInheritedFromParentWhenDescendantsEnabled() {
		User scopedUser = createTestUser("clerk_desc_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(scopedUser, managerRole, parentLocation, true, receiptType);

		List<String> roles = Collections.singletonList(managerRole.getRole());

		// Query at parent location
		List<Integer> parentUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertTrue("Parent location query should include scoped user", parentUsers.contains(scopedUser.getId()));

		// Query at child location -> should inherit through parent location
		List<Integer> childUsers = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertTrue("Child location query should include user scoped at parent with descendants",
				childUsers.contains(scopedUser.getId()));

		// Query at grandchild location -> should inherit through grandparent location
		List<Integer> grandchildUsers = stockManagementService.getActiveUsersAssignedForScope(grandchildLocation.getId(), roles);
		assertTrue("Grandchild location query should include user scoped at grandparent with descendants",
				grandchildUsers.contains(scopedUser.getId()));
	}

	@Test
	public void getActiveUsersAssignedForScope_shouldExcludeChildLocationsWhenDescendantsDisabled() {
		User directUser = createTestUser("clerk_nodesc_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(directUser, managerRole, parentLocation, false, receiptType);

		List<String> roles = Collections.singletonList(managerRole.getRole());

		// Query at parent location -> direct assignment matches
		List<Integer> parentUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertTrue("Parent location query should include direct user", parentUsers.contains(directUser.getId()));

		// Query at child location -> should NOT inherit because enableDescendants is false
		List<Integer> childUsers = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertFalse("Child location query should NOT include user when descendants disabled",
				childUsers.contains(directUser.getId()));

		// Query at grandchild location -> should NOT inherit
		List<Integer> grandchildUsers = stockManagementService.getActiveUsersAssignedForScope(grandchildLocation.getId(), roles);
		assertFalse("Grandchild location query should NOT include user when descendants disabled",
				grandchildUsers.contains(directUser.getId()));
	}

	@Test
	public void getActiveUsersAssignedForScope_shouldReturnUserAssignedDirectlyToChildLocation() {
		User childUser = createTestUser("clerk_child_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(childUser, managerRole, childLocation, true, receiptType);

		List<String> roles = Collections.singletonList(managerRole.getRole());

		// Not at parent
		List<Integer> parentUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertFalse("Parent query should NOT include user scoped only at child", parentUsers.contains(childUser.getId()));

		// Found at child
		List<Integer> childUsers = stockManagementService.getActiveUsersAssignedForScope(childLocation.getId(), roles);
		assertTrue("Child query should include user scoped at child", childUsers.contains(childUser.getId()));

		// Found at grandchild (descendant of child)
		List<Integer> grandchildUsers = stockManagementService.getActiveUsersAssignedForScope(grandchildLocation.getId(), roles);
		assertTrue("Grandchild query should include user scoped at child with descendants", grandchildUsers.contains(childUser.getId()));
	}

	@Test
	public void getActiveUsersAssignedForScope_shouldFilterByRoleAndExcludeVoidedScopesOrRetiredUsers() {
		User user = createTestUser("clerk_void_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		UserRoleScope scope = grantScope(user, managerRole, parentLocation, true, receiptType);
		List<String> roles = Collections.singletonList(managerRole.getRole());

		// Initially active user with active scope is returned
		List<Integer> initialUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertTrue("Initially active user with active scope must be included", initialUsers.contains(user.getId()));

		// Different role should return empty
		List<Integer> otherRoleUsers = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(),
				Collections.singletonList("NonExistentRole"));
		assertFalse(otherRoleUsers.contains(user.getId()));

		// Voiding the scope removes the active user from active scope users (exercises urs.voided = 0)
		stockManagementService.voidUserRoleScopes(Collections.singletonList(scope.getUuid()), "Testing voiding scope");
		Context.flushSession();
		Context.clearSession();
		List<Integer> usersAfterVoidScope = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertFalse("User with voided scope must be excluded even when user is active", usersAfterVoidScope.contains(user.getId()));

		// Re-grant scope to verify retired user check
		grantScope(user, managerRole, parentLocation, true, receiptType);
		Context.flushSession();
		Context.clearSession();
		assertTrue("User with newly granted active scope must be included again",
				stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles).contains(user.getId()));

		// Retiring the user removes them from active scope users even if scope is active
		Context.getUserService().retireUser(user, "Testing retirement");
		Context.flushSession();
		Context.clearSession();
		List<Integer> usersAfterRetire = stockManagementService.getActiveUsersAssignedForScope(parentLocation.getId(), roles);
		assertFalse("Retired user must be excluded", usersAfterRetire.contains(user.getId()));
	}

	// ---------------------------------------------------------------------------------------------
	// Query 2: Flattened privilege scopes and permission checks (userHasStockManagementPrivilege)
	// ---------------------------------------------------------------------------------------------

	@Test
	public void userHasStockManagementPrivilege_shouldGrantPermissionThroughParentLocationWhenDescendantsEnabled() {
		User user = createTestUser("perm_user_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, receiptType);

		// Parent location check
		boolean hasParent = stockManagementService.userHasStockManagementPrivilege(user, parentLocation, receiptType,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue("User should have privilege at parent location", hasParent);

		// Child location check -> inherited through parent
		boolean hasChild = stockManagementService.userHasStockManagementPrivilege(user, childLocation, receiptType,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue("User should have privilege at child location inherited from parent", hasChild);

		// Grandchild location check -> inherited through parent
		boolean hasGrandchild = stockManagementService.userHasStockManagementPrivilege(user, grandchildLocation, receiptType,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue("User should have privilege at grandchild location inherited from parent", hasGrandchild);
	}

	@Test
	public void userHasStockManagementPrivilege_shouldDenyPermissionToChildLocationsWhenDescendantsDisabled() {
		User user = createTestUser("perm_nodesc_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, false, receiptType);

		boolean hasParent = stockManagementService.userHasStockManagementPrivilege(user, parentLocation, receiptType,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertTrue("User should have privilege at parent location", hasParent);

		boolean hasChild = stockManagementService.userHasStockManagementPrivilege(user, childLocation, receiptType,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertFalse("User should NOT have privilege at child location when descendants disabled", hasChild);

		boolean hasGrandchild = stockManagementService.userHasStockManagementPrivilege(user, grandchildLocation, receiptType,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertFalse("User should NOT have privilege at grandchild location when descendants disabled", hasGrandchild);
	}

	@Test
	public void getPrivilegeScopes_shouldReturnFlattenedPrivilegeScopesAcrossHierarchy() {
		User user = createTestUser("flatten_user_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(user, managerRole, parentLocation, true, receiptType);

		// Check scopes at child location
		HashSet<PrivilegeScope> scopes = stockManagementService.getPrivilegeScopes(user, childLocation, receiptType,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		assertNotNull(scopes);
		assertFalse("Scopes should not be empty at child location", scopes.isEmpty());
		assertTrue("Scope should cover child location UUID",
				scopes.stream().anyMatch(s -> childLocation.getUuid().equals(s.getLocationUuid())));
	}

	// ---------------------------------------------------------------------------------------------
	// Query 3: Stock balances including child locations
	// ---------------------------------------------------------------------------------------------

	@Test
	public void stockBalance_shouldAggregateBalancesIncludingChildLocations() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		// Receive stock at parent (10), child (20), grandchild (30)
		createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(10), "BATCH-P");
		createAndCompleteReceipt(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(20), "BATCH-C");
		createAndCompleteReceipt(grandchildParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(30), "BATCH-G");

		// 1. Stock balance with descendants: parent includes child + grandchild (10 + 20 + 30 = 60), child includes grandchild (20 + 30 = 50)
		StockRuleCurrentQuantity parentWithDesc = new StockRuleCurrentQuantity(sampleStockItem.getId(), parentLocation.getId(), null, true);
		StockRuleCurrentQuantity childWithDesc = new StockRuleCurrentQuantity(sampleStockItem.getId(), childLocation.getId(), null, true);
		StockRuleCurrentQuantity grandchildWithDesc = new StockRuleCurrentQuantity(sampleStockItem.getId(), grandchildLocation.getId(), null, true);

		List<StockRuleCurrentQuantity> descendantQuantities = new ArrayList<>();
		descendantQuantities.add(parentWithDesc);
		descendantQuantities.add(childWithDesc);
		descendantQuantities.add(grandchildWithDesc);

		stockManagementService.setStockItemCurrentBalanceWithDescendants(descendantQuantities);

		assertNotNull("Parent balance with descendants should be calculated", parentWithDesc.getQuantity());
		assertEquals("Parent balance with descendants should be 60 (10 + 20 + 30)", 0, BigDecimal.valueOf(60).compareTo(parentWithDesc.getQuantity()));
		assertNotNull("Child balance with descendants should be calculated", childWithDesc.getQuantity());
		assertEquals("Child balance with descendants should be 50 (20 + 30)", 0, BigDecimal.valueOf(50).compareTo(childWithDesc.getQuantity()));
		assertNotNull("Grandchild balance with descendants should be calculated", grandchildWithDesc.getQuantity());
		assertEquals("Grandchild balance with descendants should be 30", 0, BigDecimal.valueOf(30).compareTo(grandchildWithDesc.getQuantity()));
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
		assertNotNull("submittedBy must be recorded", submitted.getSubmittedBy());
		assertNotNull("submittedDate must be recorded", submitted.getSubmittedDate());

		// 2. APPROVE
		stockManagementService.approveStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		StockOperation approved = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.COMPLETED, approved.getStatus());
		assertNotNull("completedBy must be recorded", approved.getCompletedBy());
		assertNotNull("completedDate must be recorded", approved.getCompletedDate());
	}

	@Test
	public void stockOperationLifecycle_shouldSubmitDispatchAndCompleteOperationRequiringDispatch() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		// First ensure parent location has stock
		StockOperationDTO receipt = createAndCompleteReceipt(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(50), "BATCH-ISSUE-INIT");
		StockOperation receiptOp = stockManagementService.getStockOperationByUuid(receipt.getUuid());
		String batchUuid = receiptOp.getStockOperationItems().iterator().next().getStockBatch().getUuid();

		// Create Stock Issue operation from parentLocation to childLocation
		StockOperationDTO issueDto = new StockOperationDTO();
		issueDto.setOperationTypeUuid(stockIssueType.getUuid());
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
		assertNotNull("dispatchedBy must be recorded", dispatched.getDispatchedBy());
		assertNotNull("dispatchedDate must be recorded", dispatched.getDispatchedDate());

		// 3. COMPLETE
		stockManagementService.completeStockOperation(issueDto);
		Context.flushSession();
		Context.clearSession();
		StockOperation completed = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.COMPLETED, completed.getStatus());
		assertNotNull("completedBy must be recorded", completed.getCompletedBy());
		assertNotNull("completedDate must be recorded", completed.getCompletedDate());
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
		assertEquals(StockOperationStatus.SUBMITTED, stockManagementService.getStockOperationByUuid(saved.getUuid()).getStatus());

		// REJECT
		String reason = "Damaged items on receipt";
		stockManagementService.rejectStockOperation(dto, reason);
		Context.flushSession();
		Context.clearSession();

		StockOperation rejected = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.REJECTED, rejected.getStatus());
		assertNotNull("rejectedBy must be recorded", rejected.getRejectedBy());
		assertNotNull("rejectedDate must be recorded", rejected.getRejectedDate());
		assertEquals("Rejection reason must match", reason, rejected.getRejectionReason());
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
		assertNotNull("returnedBy must be recorded", returned.getReturnedBy());
		assertNotNull("returnedDate must be recorded", returned.getReturnedDate());
		assertEquals("Return reason must match", reason, returned.getReturnReason());
		assertTrue("Returned operation must be updateable again", returned.isUpdateable());

		// Re-submit returned operation
		stockManagementService.submitStockOperation(dto);
		Context.flushSession();
		Context.clearSession();
		StockOperation resubmitted = stockManagementService.getStockOperationByUuid(saved.getUuid());
		assertEquals(StockOperationStatus.SUBMITTED, resubmitted.getStatus());
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
		assertNotNull("cancelledBy must be recorded", cancelled.getCancelledBy());
		assertNotNull("cancelledDate must be recorded", cancelled.getCancelledDate());
		assertEquals("Cancel reason must match", reason, cancelled.getCancelReason());

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
	public void stockOperationLifecycle_shouldEnforcePermissionsInheritedThroughParentLocation() {
		// User A has scope at parentLocation with descendants enabled -> can process at childLocation
		User userA = createTestUser("user_a_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(userA, managerRole, parentLocation, true, receiptType);

		// User B has scope only at grandchildLocation (not parent or child)
		User userB = createTestUser("user_b_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(userB, managerRole, grandchildLocation, false, receiptType);

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
		assertEquals(StockOperationStatus.SUBMITTED, stockManagementService.getStockOperationByUuid(saved.getUuid()).getStatus());

		// Switch auth context to User B -> cannot approve or process operation at childLocation
		Context.authenticate(userB.getUsername(), "TestPassword123!");
		try {
			stockManagementService.approveStockOperation(dto);
			fail("User B without scope at child location must be denied");
		} catch (StockManagementException e) {
			assertNotNull("Exception should be thrown for missing permission", e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------------------------

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
			opDto.setOperationTypeUuid(operationType.getUuid());
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
		return stockManagementService.saveStockItem(stockItem);
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
		return stockManagementService.saveStockItemPackagingUOM(packagingUom);
	}

	private StockOperationDTO newReceiptDto(Party destinationParty, StockItem stockItem, StockItemPackagingUOM uom,
			BigDecimal quantity) {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationTypeUuid(receiptType.getUuid());
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
		dto.setOperationTypeUuid(receiptType.getUuid());
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
}
