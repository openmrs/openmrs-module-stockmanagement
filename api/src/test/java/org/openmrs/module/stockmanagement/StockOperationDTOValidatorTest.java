package org.openmrs.module.stockmanagement;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.model.LocationType;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemDTO;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.validator.StockOperationDTOValidator;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link StockOperationDTOValidator}, the validator that derives
 * atLocationUuid and enforces stock operation business rules. webservices.rest 3.x no longer
 * invokes it implicitly (the resource now invokes it explicitly), so its messages must always be
 * populated and actionable.
 */
public class StockOperationDTOValidatorTest extends BaseModuleContextSensitiveTest {

	// mirrors the production Receipt type: hasSource=true with source_type Other, destination_type Location
	private static final String TEST_RECEIPT_TYPE_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000";

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

	@Autowired
	DbSessionFactory sessionFactory;

	private StockManagementDao daoInstance;

	private static EntityUtil entityUtil;

	private StockOperationDTOValidator validator;

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
		validator = new StockOperationDTOValidator();
	}

	private Errors validate(StockOperationDTO dto) {
		Errors errors = new MapBindingResult(new HashMap<>(), "stockOperationDTO");
		validator.validate(dto, errors);
		return errors;
	}

	/**
	 * The validator passes the human-readable text as the error code, so resolve the message the
	 * same way StockOperationResource does.
	 */
	private String messageOf(org.springframework.validation.ObjectError error) {
		String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode();
		return message == null ? null : message.trim();
	}

	private StockOperationDTO newReceiptDto() {
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
	public void validate_shouldRejectUnknownOperationTypeWithNonNullMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setOperationTypeUuid("00000000-0000-0000-0000-000000000000");
		Errors errors = validate(dto);
		assertTrue(errors.hasErrors());
		assertNotNull(messageOf(errors.getAllErrors().get(0)));
	}

	@Test
	public void validate_shouldRejectMissingOperationTypeUuidWithNonNullMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setOperationTypeUuid(null);
		Errors errors = validate(dto);
		assertTrue(errors.hasErrors());
		for (Object error : errors.getAllErrors()) {
			assertNotNull("Validation errors must never carry a null message", messageOf((org.springframework.validation.ObjectError) error));
			assertFalse(messageOf((org.springframework.validation.ObjectError) error).isEmpty());
		}
	}

	@Test
	public void validate_shouldRejectMissingDestinationWithNonNullMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setDestinationUuid(null);
		Errors errors = validate(dto);
		assertTrue(errors.hasErrors());
		assertEquals(1, errors.getErrorCount());
		assertNotNull(messageOf(errors.getAllErrors().get(0)));
	}

	@Test
	public void validate_shouldRejectEmptyItemsWithNonNullMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setStockOperationItems(new ArrayList<>());
		Errors errors = validate(dto);
		assertTrue(errors.hasErrors());
		assertNotNull(messageOf(errors.getAllErrors().get(0)));
	}

	@Test
	public void validate_shouldDeriveAtLocationUuidFromDestination() {
		StockOperationDTO dto = newReceiptDto();
		Errors errors = validate(dto);
		// the validator fills atLocationUuid from the destination party's location even when the
		// client did not send it; in this database the authenticated user has no matching user role
		// scope, so validation also ends with the permission error after the derivation
		assertNotNull(dto.getAtLocationUuid());
		for (Object error : errors.getAllErrors()) {
			assertNotNull(messageOf((org.springframework.validation.ObjectError) error));
		}
	}

	@Test
	public void validate_shouldRejectUnauthorizedUserWithNonNullMessage() {
		// the in-memory database grants no stock management user role scopes to any user, so
		// processing the operation must be rejected with the permission message rather than a raw
		// exception or a silent success
		StockOperationDTO dto = newReceiptDto();
		Errors errors = validate(dto);
		assertTrue(errors.hasErrors());
		assertNotNull(messageOf(errors.getAllErrors().get(0)));
	}
}
