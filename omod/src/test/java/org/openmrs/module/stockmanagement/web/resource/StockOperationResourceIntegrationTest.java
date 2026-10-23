package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemDTO;
import org.openmrs.module.stockmanagement.api.model.LocationType;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.UserRoleScope;
import org.openmrs.module.stockmanagement.api.model.UserRoleScopeLocation;
import org.openmrs.module.stockmanagement.api.model.UserRoleScopeOperationType;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;

/**
 * Context-sensitive test that saves through {@link StockOperationResource#save} end to end.
 * StockOperationResourceTest stubs the validator out, so it cannot prove that save() actually
 * runs StockOperationDTOValidator: webservices.rest 3.x no longer invokes it implicitly, and it
 * is what derives atLocationUuid (at_location_id is NOT NULL) and enforces the per-location
 * permission checks for clients that never send atLocationUuid.
 */
public class StockOperationResourceIntegrationTest extends BaseModuleWebContextSensitiveTest {

	// mirrors the production Receipt type: hasSource=true with source_type Other, destination_type Location
	private static final String TEST_RECEIPT_TYPE_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000";

	@Autowired
	DbSessionFactory sessionFactory;

	private StockManagementDao daoInstance;

	private EntityUtil entityUtil;

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

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
		createReceiptOperationType();
	}

	private void createReceiptOperationType() {
		StockOperationType receipt = new StockOperationType();
		receipt.setName("Test Receipt");
		receipt.setOperationType(StockOperationType.RECEIPT);
		receipt.setUuid(TEST_RECEIPT_TYPE_UUID);
		receipt.setHasSource(true);
		receipt.setSourceType(LocationType.Other);
		receipt.setHasDestination(true);
		receipt.setDestinationType(LocationType.Location);
		receipt.setAvailableWhenReserved(false);
		receipt.setAcronym("RCPT");
		dao().saveStockOperationType(receipt);
	}

	/**
	 * Gives the authenticated user a user role scope at the given location for the given
	 * operation type, backed by a role carrying the stock operations mutate privilege, the same
	 * way the UI sets a clerk up through the userrolescope REST resource.
	 */
	private void grantAuthenticatedUserScope(Location location, StockOperationType operationType) {
		// re-fetch the authenticated user: the cached instance is detached from the open session
		// and saveUser would fail with a NonUniqueObjectException
		User user = Context.getUserService().getUserByUuid(Context.getAuthenticatedUser().getUuid());
		Privilege privilege = Context.getUserService()
		        .getPrivilege(Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE);
		if (privilege == null) {
			privilege = Context.getUserService().savePrivilege(new Privilege(
			        Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE, "Able to create and update stock operations"));
		}
		// the save's read-back goes through findStockOperations, which requires the app view
		// privilege on the scope role
		Privilege appPrivilege = Context.getUserService()
		        .getPrivilege(Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS);
		if (appPrivilege == null) {
			appPrivilege = Context.getUserService().savePrivilege(new Privilege(
			        Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS, "Able to view stock operations"));
		}
		Role clerk = new Role();
		clerk.setRole("Stock operation clerk");
		clerk.setDescription(" Grants stock operation processing within a scoped location");
		clerk.addPrivilege(privilege);
		clerk.addPrivilege(appPrivilege);
		clerk = Context.getUserService().saveRole(clerk);
		Set<Role> roles = new HashSet<>(user.getRoles());
		roles.add(clerk);
		user.setRoles(roles);
		Context.getUserService().saveUser(user);
		// the permission checks run against the user context's cached instance, so grant the role
		// there too
		Context.getAuthenticatedUser().setRoles(roles);

		UserRoleScope scope = new UserRoleScope();
		scope.setUser(user);
		scope.setRole(clerk);
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

		UserRoleScopeOperationType scopeOperationType = new UserRoleScopeOperationType();
		scopeOperationType.setUserRoleScope(scope);
		scopeOperationType.setStockOperationType(operationType);
		scopeOperationType.setVoided(false);
		scopeOperationType.setCreator(user);
		scopeOperationType.setDateCreated(new Date());
		dao().saveUserRoleScopeOperationType(scopeOperationType);
	}

	/**
	 * A receipt the way the OWA and the SPA send it: no atLocationUuid at all.
	 */
	private StockOperationDTO newReceiptDtoWithoutAtLocationUuid() {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationTypeUuid(TEST_RECEIPT_TYPE_UUID);
		dto.setOperationDate(DateUtils.addDays(new Date(), -1));
		// receipts originate from a stock source (source_type Other), not a location
		org.openmrs.module.stockmanagement.api.model.StockSource stockSource = eu().newStockSource(dao());
		stockSource.setVoided(false);
		dao().saveStockSource(stockSource);
		org.openmrs.module.stockmanagement.api.model.Party sourceParty = new org.openmrs.module.stockmanagement.api.model.Party();
		sourceParty.setStockSource(stockSource);
		sourceParty.setCreator(eu().getUser());
		sourceParty.setDateCreated(new Date());
		dao().saveParty(sourceParty);
		dto.setSourceUuid(sourceParty.getUuid());
		dto.setDestinationUuid(eu().getParty(dao()).getUuid());

		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), true);
		packagingUom.setVoided(false);
		packagingUom.getStockItem().setVoided(false);
		dao().saveStockItem(packagingUom.getStockItem());
		dao().saveStockItemPackagingUOM(packagingUom);
		StockOperationItemDTO item = new StockOperationItemDTO();
		item.setStockItemUuid(packagingUom.getStockItem().getUuid());
		item.setStockItemPackagingUOMUuid(packagingUom.getUuid());
		item.setBatchNo("BATCH-" + System.currentTimeMillis());
		item.setExpiration(DateUtils.addDays(new Date(), 365));
		item.setQuantity(BigDecimal.TEN);
		dto.setStockOperationItems(new ArrayList<>(Arrays.asList(item)));
		return dto;
	}

	@Test
	public void save_shouldDeriveAtLocation_whenClientDoesNotSendAtLocationUuid() {
		StockOperationType receiptType = dao().getStockOperationTypeByUuid(TEST_RECEIPT_TYPE_UUID);
		grantAuthenticatedUserScope(eu().getLocation(), receiptType);

		StockOperationDTO saved = new StockOperationResource().save(newReceiptDtoWithoutAtLocationUuid());
		assertNotNull(saved.getUuid());

		StockOperation operation = dao().getStockOperationByUuid(saved.getUuid());
		assertNotNull(operation);
		assertNotNull("at_location_id is NOT NULL: the validator must have derived it", operation.getAtLocation());
		assertEquals(operation.getDestination().getLocation().getId(), operation.getAtLocation().getId());
	}

	@Test
	public void save_shouldRejectUserWithoutScopeAtTheLocation() {
		// no user role scope at the destination location: the validator save() runs must reject
		// the operation instead of silently creating it
		IllegalRequestException exception = assertThrows(IllegalRequestException.class,
		    () -> new StockOperationResource().save(newReceiptDtoWithoutAtLocationUuid()));
		assertNotNull(exception.getMessage());
		assertTrue(exception.getMessage().toLowerCase().contains("permission"));
	}
}
