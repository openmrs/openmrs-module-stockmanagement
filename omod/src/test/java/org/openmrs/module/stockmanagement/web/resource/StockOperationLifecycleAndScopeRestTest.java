package org.openmrs.module.stockmanagement.web.resource;

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
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.dto.StockOperationAction;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeLocationDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeOperationTypeDTO;
import org.openmrs.module.stockmanagement.api.model.LocationType;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.tasks.LocationTreeSynchronize;
import org.openmrs.module.stockmanagement.tasks.PartySynchronize;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.api.ValidationException;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * End-to-end regression tests verifying the stock operation lifecycle and permission checks
 * through the REST layer (StockOperationResource and StockOperationActionResource).
 */
public class StockOperationLifecycleAndScopeRestTest extends BaseModuleWebContextSensitiveTest {

	private static final String RECEIPT_OPERATION_TYPE_UUID = "fce0b4fc-9402-424a-aacb-f99599e51a9f";

	private static final String ISSUE_OPERATION_TYPE_UUID = "db40707f-9175-4199-8df2-a5702f41ec7d";

	@Autowired
	private DbSessionFactory sessionFactory;

	private StockManagementService stockManagementService;

	private StockOperationResource stockOperationResource;

	private StockOperationActionResource stockOperationActionResource;

	private StockManagementDao daoInstance;

	private Location parentLocation;

	private Location childLocation;

	private Location grandchildLocation;

	private Party parentParty;

	private Party childParty;

	private Party grandchildParty;

	private Party supplierParty;

	private StockOperationType receiptType;

	private StockOperationType stockIssueType;

	private Role managerRole;

	private Role sysDevRole;

	private User clerkUser;

	private StockItem sampleStockItem;

	private StockItemPackagingUOM samplePackagingUom;

	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
		}
		return daoInstance;
	}

	private void setNotifyFlagsFalse(StockOperationType type) {
		type.setNotifySubmitted(false);
		type.setNotifyApproved(false);
		type.setNotifyDispatched(false);
		type.setNotifyReturned(false);
		type.setNotifyRejected(false);
		type.setNotifyCancelled(false);
		type.setNotifyCompleted(false);
	}

	@Before
	public void setUp() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);

		stockManagementService = Context.getService(StockManagementService.class);
		stockOperationResource = new StockOperationResource();
		stockOperationActionResource = new StockOperationActionResource();

		// 1. Build 3-tier location hierarchy: parent -> child -> grandchild
		LocationService locationService = Context.getLocationService();

		parentLocation = new Location();
		parentLocation.setName("Rest Parent Hospital " + UUID.randomUUID());
		parentLocation = locationService.saveLocation(parentLocation);

		childLocation = new Location();
		childLocation.setName("Rest Child Clinic " + UUID.randomUUID());
		childLocation.setParentLocation(parentLocation);
		childLocation = locationService.saveLocation(childLocation);

		grandchildLocation = new Location();
		grandchildLocation.setName("Rest Grandchild Dispensary " + UUID.randomUUID());
		grandchildLocation.setParentLocation(childLocation);
		grandchildLocation = locationService.saveLocation(grandchildLocation);

		new PartySynchronize().execute();
		new LocationTreeSynchronize().execute();

		parentParty = stockManagementService.getPartyByLocation(parentLocation);
		childParty = stockManagementService.getPartyByLocation(childLocation);
		grandchildParty = stockManagementService.getPartyByLocation(grandchildLocation);

		assertNotNull("Parent party must exist", parentParty);
		assertNotNull("Child party must exist", childParty);
		assertNotNull("Grandchild party must exist", grandchildParty);

		supplierParty = createSupplierParty();

		receiptType = stockManagementService.getStockOperationTypeByUuid(RECEIPT_OPERATION_TYPE_UUID);
		stockIssueType = stockManagementService.getStockOperationTypeByUuid(ISSUE_OPERATION_TYPE_UUID);

		receiptType.setHasSource(true);
		receiptType.setSourceType(LocationType.Other);
		receiptType.setHasDestination(true);
		receiptType.setDestinationType(LocationType.Location);
		setNotifyFlagsFalse(receiptType);
		setNotifyFlagsFalse(stockIssueType);
		dao().saveStockOperationType(receiptType);
		dao().saveStockOperationType(stockIssueType);
		Context.flushSession();
		Context.clearSession();

		// Configure test roles
		managerRole = createRoleWithPrivileges("Rest Stock Operations Manager",
				Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
				Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONTYPE,
				Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
				Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE,
				Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS,
				Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE,
				Privileges.TASK_STOCKMANAGEMENT_USERROLESCOPES_MUTATE,
				Privileges.TASK_STOCKMANAGEMENT_PARTY_READ,
				PrivilegeConstants.GET_LOCATIONS,
				PrivilegeConstants.GET_CONCEPTS,
				PrivilegeConstants.GET_USERS,
				PrivilegeConstants.GET_GLOBAL_PROPERTIES,
				PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);

		sysDevRole = Context.getUserService().getRole("System Developer");
		if (sysDevRole != null) {
			for (String privName : Privileges.ALL) {
				Privilege priv = Context.getUserService().getPrivilege(privName);
				if (priv == null) {
					priv = Context.getUserService().savePrivilege(new Privilege(privName, privName));
				}
				sysDevRole.addPrivilege(priv);
			}
			Context.getUserService().saveRole(sysDevRole);
		}

		sampleStockItem = createStockItem("Rest Paracetamol 500mg " + UUID.randomUUID().toString().substring(0, 8));
		samplePackagingUom = createPackagingUOM(sampleStockItem, BigDecimal.ONE);

		clerkUser = createTestUser("rest_clerk_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(clerkUser, managerRole, parentLocation, true, receiptType);
		grantScope(clerkUser, managerRole, parentLocation, true, stockIssueType);

		Context.getAdministrationService().saveGlobalProperty(
				new GlobalProperty("stockmanagement.allowStockIssueWithoutRequisition", "true"));
	}

	// ---------------------------------------------------------------------------------------------
	// REST Lifecycle Tests
	// ---------------------------------------------------------------------------------------------

	@Test
	public void restLifecycle_shouldCreateReceiptAndActionThroughJsonPayloadAndCreatableProperties() throws Exception {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		// Exercise the REST request path via JSON payload deserialized into SimpleObject and passed to create()
		String jsonPayload = "{\n"
				+ "  \"operationTypeUuid\": \"" + receiptType.getUuid() + "\",\n"
				+ "  \"operationDate\": \"2026-11-19\",\n"
				+ "  \"sourceUuid\": \"" + supplierParty.getUuid() + "\",\n"
				+ "  \"destinationUuid\": \"" + childParty.getUuid() + "\",\n"
				+ "  \"stockOperationItems\": [{\n"
				+ "    \"stockItemUuid\": \"" + sampleStockItem.getUuid() + "\",\n"
				+ "    \"stockItemPackagingUOMUuid\": \"" + samplePackagingUom.getUuid() + "\",\n"
				+ "    \"batchNo\": \"REST-JSON-BATCH-1\",\n"
				+ "    \"expiration\": \"2027-11-19\",\n"
				+ "    \"quantity\": 25\n"
				+ "  }]\n"
				+ "}";

		SimpleObject post = SimpleObject.parseJson(jsonPayload);
		RequestContext requestContext = new RequestContext();
		requestContext.setRequest(new MockHttpServletRequest());
		requestContext.setResponse(new MockHttpServletResponse());

		// Verify create() reads request fields and converts nested stockOperationItems via getCreatableProperties()
		Object created = stockOperationResource.create(post, requestContext);
		assertNotNull("create() must return representation", created);
		assertTrue("create() must return SimpleObject", created instanceof SimpleObject);
		SimpleObject createdObj = (SimpleObject) created;
		String createdUuid = (String) createdObj.get("uuid");
		assertNotNull("Created operation must have uuid in response", createdUuid);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO retrieved = stockOperationResource.getByUniqueId(createdUuid);
		assertNotNull(retrieved);
		assertEquals(StockOperationStatus.NEW, retrieved.getStatus());

		// Verify nested items were processed by the property setter and persisted
		Collection<StockOperationItemDTO> items = stockOperationResource.getStockOperationItems(retrieved);
		assertNotNull("Property getter getStockOperationItems must return line items", items);
		assertEquals(1, items.size());
		StockOperationItemDTO item = items.iterator().next();
		assertEquals("REST-JSON-BATCH-1", item.getBatchNo());
		assertEquals(0, BigDecimal.valueOf(25).compareTo(item.getQuantity()));

		// Verify retrieve() via REST request path with Representation.FULL returns nested items
		RequestContext fullContext = new RequestContext();
		fullContext.setRequest(new MockHttpServletRequest());
		fullContext.setResponse(new MockHttpServletResponse());
		fullContext.setRepresentation(Representation.FULL);
		Object retrievedFull = stockOperationResource.retrieve(createdUuid, fullContext);
		assertTrue(retrievedFull instanceof SimpleObject);
		List<?> restItems = (List<?>) ((SimpleObject) retrievedFull).get("stockOperationItems");
		assertNotNull("Full representation must include stockOperationItems", restItems);
		assertEquals(1, restItems.size());

		// Exercise the action REST request path via JSON payload and create()
		String submitActionJson = "{\n"
				+ "  \"uuid\": \"" + createdUuid + "\",\n"
				+ "  \"name\": \"SUBMIT\"\n"
				+ "}";
		SimpleObject actionPost = SimpleObject.parseJson(submitActionJson);
		Object actionResult = stockOperationActionResource.create(actionPost, requestContext);
		assertNotNull("Action create() must return representation", actionResult);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO submitted = stockOperationResource.getByUniqueId(createdUuid);
		assertEquals(StockOperationStatus.SUBMITTED, submitted.getStatus());
		assertNotNull("submittedBy must be recorded", submitted.getSubmittedBy());
	}

	@Test
	public void restLifecycle_shouldSaveSubmitAndApproveReceiptOperation() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		// Save via StockOperationResource at child location (access granted through parent location)
		StockOperationDTO receiptDto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(15));
		StockOperationDTO saved = stockOperationResource.save(receiptDto);
		assertNotNull(saved.getUuid());
		assertEquals(StockOperationStatus.NEW, saved.getStatus());

		Context.flushSession();
		Context.clearSession();

		// 1. SUBMIT via StockOperationActionResource
		StockOperationAction submitAction = new StockOperationAction();
		submitAction.setUuid(saved.getUuid());
		submitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(submitAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO submitted = stockOperationResource.getByUniqueId(saved.getUuid());
		assertEquals(StockOperationStatus.SUBMITTED, submitted.getStatus());
		assertNotNull("submittedBy must be recorded", submitted.getSubmittedBy());

		// 2. APPROVE via StockOperationActionResource
		StockOperationAction approveAction = new StockOperationAction();
		approveAction.setUuid(saved.getUuid());
		approveAction.setName(StockOperationAction.Action.APPROVE);
		stockOperationActionResource.save(approveAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO approved = stockOperationResource.getByUniqueId(saved.getUuid());
		assertEquals(StockOperationStatus.COMPLETED, approved.getStatus());
		assertNotNull("completedBy must be recorded", approved.getCompletedBy());
		assertNotNull("completedDate must be recorded", approved.getCompletedDate());
	}

	@Test
	public void restLifecycle_shouldSaveSubmitDispatchAndCompleteStockIssue() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		// Initial stock receipt at parent location
		StockOperationDTO initialReceipt = createAndCompleteReceiptViaRest(parentParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(40), "REST-BATCH-INIT");
		StockOperation receiptOp = stockManagementService.getStockOperationByUuid(initialReceipt.getUuid());
		String batchUuid = receiptOp.getStockOperationItems().iterator().next().getStockBatch().getUuid();

		// Save Stock Issue via StockOperationResource from parentLocation to childLocation
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

		StockOperationDTO savedIssue = stockOperationResource.save(issueDto);
		assertNotNull(savedIssue.getUuid());
		assertEquals(StockOperationStatus.NEW, savedIssue.getStatus());

		Context.flushSession();
		Context.clearSession();

		// 1. SUBMIT via StockOperationActionResource
		StockOperationAction submitAction = new StockOperationAction();
		submitAction.setUuid(savedIssue.getUuid());
		submitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(submitAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO submitted = stockOperationResource.getByUniqueId(savedIssue.getUuid());
		assertEquals(StockOperationStatus.SUBMITTED, submitted.getStatus());

		// 2. DISPATCH via StockOperationActionResource
		StockOperationAction dispatchAction = new StockOperationAction();
		dispatchAction.setUuid(savedIssue.getUuid());
		dispatchAction.setName(StockOperationAction.Action.DISPATCH);
		stockOperationActionResource.save(dispatchAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO dispatched = stockOperationResource.getByUniqueId(savedIssue.getUuid());
		assertEquals(StockOperationStatus.DISPATCHED, dispatched.getStatus());
		assertNotNull("dispatchedBy must be recorded", dispatched.getDispatchedBy());
		assertNotNull("dispatchedDate must be recorded", dispatched.getDispatchedDate());

		// 3. COMPLETE via StockOperationActionResource
		StockOperationAction completeAction = new StockOperationAction();
		completeAction.setUuid(savedIssue.getUuid());
		completeAction.setName(StockOperationAction.Action.COMPLETE);
		stockOperationActionResource.save(completeAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO completed = stockOperationResource.getByUniqueId(savedIssue.getUuid());
		assertEquals(StockOperationStatus.COMPLETED, completed.getStatus());
		assertNotNull("completedBy must be recorded", completed.getCompletedBy());
		assertNotNull("completedDate must be recorded", completed.getCompletedDate());
	}

	@Test
	public void restLifecycle_shouldRejectOperationWithReason() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		StockOperationDTO receiptDto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(5));
		StockOperationDTO saved = stockOperationResource.save(receiptDto);
		Context.flushSession();
		Context.clearSession();

		StockOperationAction submitAction = new StockOperationAction();
		submitAction.setUuid(saved.getUuid());
		submitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(submitAction);
		Context.flushSession();
		Context.clearSession();

		// REJECT via StockOperationActionResource
		String reason = "Damaged items on delivery";
		StockOperationAction rejectAction = new StockOperationAction();
		rejectAction.setUuid(saved.getUuid());
		rejectAction.setName(StockOperationAction.Action.REJECT);
		rejectAction.setReason(reason);
		stockOperationActionResource.save(rejectAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO rejected = stockOperationResource.getByUniqueId(saved.getUuid());
		assertEquals(StockOperationStatus.REJECTED, rejected.getStatus());
		assertNotNull("rejectedBy must be recorded", rejected.getRejectedBy());
		assertNotNull("rejectedDate must be recorded", rejected.getRejectedDate());
		assertEquals("Rejection reason must match", reason, rejected.getRejectionReason());
	}

	@Test
	public void restLifecycle_shouldReturnAndResubmitOperation() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		StockOperationDTO receiptDto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(8));
		StockOperationDTO saved = stockOperationResource.save(receiptDto);
		Context.flushSession();
		Context.clearSession();

		StockOperationAction submitAction = new StockOperationAction();
		submitAction.setUuid(saved.getUuid());
		submitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(submitAction);
		Context.flushSession();
		Context.clearSession();

		// RETURN via StockOperationActionResource
		String reason = "Batch number missing documentation";
		StockOperationAction returnAction = new StockOperationAction();
		returnAction.setUuid(saved.getUuid());
		returnAction.setName(StockOperationAction.Action.RETURN);
		returnAction.setReason(reason);
		stockOperationActionResource.save(returnAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO returned = stockOperationResource.getByUniqueId(saved.getUuid());
		assertEquals(StockOperationStatus.RETURNED, returned.getStatus());
		assertNotNull("returnedBy must be recorded", returned.getReturnedBy());
		assertNotNull("returnedDate must be recorded", returned.getReturnedDate());
		assertEquals("Return reason must match", reason, returned.getReturnReason());
		assertTrue("Returned operation must be updateable again", returned.isUpdateable());

		// Re-submit returned operation
		StockOperationAction resubmitAction = new StockOperationAction();
		resubmitAction.setUuid(saved.getUuid());
		resubmitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(resubmitAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO resubmitted = stockOperationResource.getByUniqueId(saved.getUuid());
		assertEquals(StockOperationStatus.SUBMITTED, resubmitted.getStatus());
	}

	@Test
	public void restLifecycle_shouldCancelOperationWithReason() {
		Context.authenticate(clerkUser.getUsername(), "TestPassword123!");

		// 1. Cancel from SUBMITTED status
		StockOperationDTO receiptDto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(12));
		StockOperationDTO saved = stockOperationResource.save(receiptDto);
		Context.flushSession();
		Context.clearSession();

		StockOperationAction submitAction = new StockOperationAction();
		submitAction.setUuid(saved.getUuid());
		submitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(submitAction);
		Context.flushSession();
		Context.clearSession();

		String reason = "Duplicate entry created";
		StockOperationAction cancelAction = new StockOperationAction();
		cancelAction.setUuid(saved.getUuid());
		cancelAction.setName(StockOperationAction.Action.CANCEL);
		cancelAction.setReason(reason);
		stockOperationActionResource.save(cancelAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO cancelled = stockOperationResource.getByUniqueId(saved.getUuid());
		assertEquals(StockOperationStatus.CANCELLED, cancelled.getStatus());
		assertNotNull("cancelledBy must be recorded", cancelled.getCancelledBy());
		assertNotNull("cancelledDate must be recorded", cancelled.getCancelledDate());
		assertEquals("Cancel reason must match", reason, cancelled.getCancelReason());

		// 2. Cancel directly from NEW status
		StockOperationDTO newDto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(3));
		StockOperationDTO savedNew = stockOperationResource.save(newDto);
		Context.flushSession();
		Context.clearSession();

		StockOperationAction cancelNewAction = new StockOperationAction();
		cancelNewAction.setUuid(savedNew.getUuid());
		cancelNewAction.setName(StockOperationAction.Action.CANCEL);
		cancelNewAction.setReason("Abandoned before submission");
		stockOperationActionResource.save(cancelNewAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationDTO cancelledNew = stockOperationResource.getByUniqueId(savedNew.getUuid());
		assertEquals(StockOperationStatus.CANCELLED, cancelledNew.getStatus());
	}

	// ---------------------------------------------------------------------------------------------
	// REST Permission / Scope Inheritance Tests
	// ---------------------------------------------------------------------------------------------

	@Test
	public void restPermissions_shouldAllowUserInheritingScopeFromParentAndDenyUnscopedUser() {
		// User A has scope at parentLocation with descendants enabled -> can process at childLocation
		User userA = createTestUser("rest_user_a_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(userA, managerRole, parentLocation, true, receiptType);

		// User B has scope only at grandchildLocation (not parent or child)
		User userB = createTestUser("rest_user_b_" + UUID.randomUUID().toString().substring(0, 8), managerRole);
		grantScope(userB, managerRole, grandchildLocation, false, receiptType);

		// Switch to User A
		Context.authenticate(userA.getUsername(), "TestPassword123!");
		StockOperationDTO dto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(7));
		StockOperationDTO saved = stockOperationResource.save(dto);
		assertNotNull(saved.getUuid());
		Context.flushSession();
		Context.clearSession();

		// User A submits successfully
		StockOperationAction submitAction = new StockOperationAction();
		submitAction.setUuid(saved.getUuid());
		submitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(submitAction);
		Context.flushSession();
		Context.clearSession();
		assertEquals(StockOperationStatus.SUBMITTED, stockOperationResource.getByUniqueId(saved.getUuid()).getStatus());

		// Switch to User B (unscoped for childLocation)
		Context.authenticate(userB.getUsername(), "TestPassword123!");

		// User B attempting to save an operation at childLocation must be rejected by validator
		StockOperationDTO unauthDto = newReceiptDto(childParty, sampleStockItem, samplePackagingUom, BigDecimal.valueOf(5));
		try {
			stockOperationResource.save(unauthDto);
			fail("User B must not be permitted to create operation at child location without scope");
		} catch (ValidationException e) {
			assertNotNull("Validation error must be reported", e.getMessage());
			assertTrue("Validation message must indicate permission issue",
					e.getMessage().toLowerCase().contains("permission"));
		}

		// User B attempting to act on operation at childLocation must be rejected (operation not visible or permission denied)
		StockOperationAction approveAction = new StockOperationAction();
		approveAction.setUuid(saved.getUuid());
		approveAction.setName(StockOperationAction.Action.APPROVE);
		try {
			stockOperationActionResource.save(approveAction);
			fail("User B must not be permitted to approve operation at child location without scope");
		} catch (ResponseException e) {
			assertNotNull("Exception message should indicate not found or not permitted", e.getMessage());
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Helper Methods
	// ---------------------------------------------------------------------------------------------

	private User createTestUser(String username, Role role) {
		UserService userService = Context.getUserService();
		User user = new User();
		user.setUsername(username);
		user.setSystemId(username);

		Person person = new Person();
		PersonName name = new PersonName("Test", "Clerk", username);
		person.addName(name);
		person.setGender("M");
		person = Context.getPersonService().savePerson(person);

		user.setPerson(person);
		user.addRole(role);
		return userService.createUser(user, "TestPassword123!");
	}

	private Role createRoleWithPrivileges(String roleName, String... privilegeNames) {
		UserService userService = Context.getUserService();
		Role role = userService.getRole(roleName);
		if (role == null) {
			role = new Role();
			role.setRole(roleName);
			role.setDescription("Test role for " + roleName);
		}
		for (String privName : privilegeNames) {
			Privilege priv = userService.getPrivilege(privName);
			if (priv == null) {
				priv = userService.savePrivilege(new Privilege(privName, privName));
			}
			role.addPrivilege(priv);
		}
		return userService.saveRole(role);
	}

	private void grantScope(User user, Role role, Location location, boolean enableDescendants,
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

		stockManagementService.saveUserRoleScope(dto);
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

	private Party createSupplierParty() {
		Concept sourceTypeConcept = Context.getConceptService().getConcept(1067);
		StockSource stockSource = new StockSource();
		stockSource.setName("External Supplier " + UUID.randomUUID());
		stockSource.setAcronym("SUPP");
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

	private StockOperationDTO newReceiptDto(Party destinationParty, StockItem stockItem, StockItemPackagingUOM uom,
			BigDecimal quantity) {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationTypeUuid(receiptType.getUuid());
		dto.setOperationDate(DateUtils.addDays(new Date(), -1));
		dto.setSourceUuid(supplierParty.getUuid());
		dto.setDestinationUuid(destinationParty.getUuid());
		if (destinationParty.getLocation() != null) {
			dto.setAtLocationUuid(destinationParty.getLocation().getUuid());
		}

		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(stockItem.getUuid());
		item.setStockItemPackagingUOMUuid(uom.getUuid());
		item.setBatchNo("REST-BATCH-" + UUID.randomUUID().toString().substring(0, 8));
		item.setExpiration(DateUtils.addDays(new Date(), 180));
		item.setQuantity(quantity);
		dto.setStockOperationItems(Collections.singletonList(item));
		return dto;
	}

	private StockOperationDTO createAndCompleteReceiptViaRest(Party destinationParty, StockItem stockItem,
			StockItemPackagingUOM uom, BigDecimal quantity, String batchNo) {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationTypeUuid(receiptType.getUuid());
		dto.setOperationDate(DateUtils.addDays(new Date(), -1));
		dto.setSourceUuid(supplierParty.getUuid());
		dto.setDestinationUuid(destinationParty.getUuid());
		if (destinationParty.getLocation() != null) {
			dto.setAtLocationUuid(destinationParty.getLocation().getUuid());
		}

		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(stockItem.getUuid());
		item.setStockItemPackagingUOMUuid(uom.getUuid());
		item.setBatchNo(batchNo != null ? batchNo : "REST-BATCH-" + UUID.randomUUID().toString().substring(0, 8));
		item.setExpiration(DateUtils.addDays(new Date(), 180));
		item.setQuantity(quantity);
		dto.setStockOperationItems(Collections.singletonList(item));

		StockOperationDTO saved = stockOperationResource.save(dto);
		Context.flushSession();
		Context.clearSession();

		StockOperationAction submitAction = new StockOperationAction();
		submitAction.setUuid(saved.getUuid());
		submitAction.setName(StockOperationAction.Action.SUBMIT);
		stockOperationActionResource.save(submitAction);

		Context.flushSession();
		Context.clearSession();

		StockOperationAction approveAction = new StockOperationAction();
		approveAction.setUuid(saved.getUuid());
		approveAction.setName(StockOperationAction.Action.APPROVE);
		stockOperationActionResource.save(approveAction);

		Context.flushSession();
		Context.clearSession();
		return saved;
	}
}
