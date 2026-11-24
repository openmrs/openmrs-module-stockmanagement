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
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.model.LocationType;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemDTO;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.validator.StockOperationDTOValidator;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link StockOperationDTOValidator}, the validator that derives
 * atLocationUuid and enforces stock operation business rules. webservices.rest 3.x no longer
 * invokes it implicitly (the resource now invokes it explicitly), so its messages must always be
 * populated and actionable.
 */
public class StockOperationDTOValidatorTest extends BaseModuleContextSensitiveTest {
	
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
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_MANAGEMENT_DATA_SET);
		validator = new StockOperationDTOValidator();
	}
	
	private Errors validate(StockOperationDTO dto) {
		Errors errors = new MapBindingResult(new HashMap<>(), "stockOperationDTO");
		validator.validate(dto, errors);
		return errors;
	}
	
	/**
	 * The validator passes the human-readable text as the error code, so resolve the message the same
	 * way StockOperationResource does.
	 */
	private String messageOf(org.springframework.validation.ObjectError error) {
		String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode();
		return message == null ? null : message.trim();
	}
	
	@Test
	public void validate_shouldReportMissingOperationDate() {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationType(StockOperationType.RECEIPT.getOperationType());
		
		Errors errors = validate(dto);
		
		assertNotNull(errors.getFieldError("operationDate"));
		assertEquals("Operation date is required", messageOf(errors.getFieldError("operationDate")));
	}
	
	private StockOperationDTO newReceiptDto() {
		StockOperationDTO dto = new StockOperationDTO();
		dto.setOperationType(StockOperationType.RECEIPT.getOperationType());
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
	public void validate_shouldReportAnUnknownSourceWithAResolvedMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setSourceUuid("unknown-party-uuid");
		
		Errors errors = validate(dto);
		
		assertNotNull(errors.getFieldError("sourceUuid"));
		assertEquals("Source is invalid", messageOf(errors.getFieldError("sourceUuid")));
	}
	
	@Test
	public void validate_shouldReportAVoidedSourceWithAResolvedMessage() {
		StockOperationDTO dto = newReceiptDto();
		dao().getPartyByUuid(dto.getSourceUuid()).setVoided(true);
		
		Errors errors = validate(dto);
		
		assertNotNull(errors.getFieldError("sourceUuid"));
		assertEquals("Source is invalid", messageOf(errors.getFieldError("sourceUuid")));
	}
	
	@Test
	public void validate_shouldReportAnUnknownDestinationWithAResolvedMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setDestinationUuid("unknown-party-uuid");
		
		Errors errors = validate(dto);
		
		assertNotNull(errors.getFieldError("destinationUuid"));
		assertEquals("Destination is invalid", messageOf(errors.getFieldError("destinationUuid")));
	}
	
	@Test
	public void validate_shouldReportAVoidedDestinationWithAResolvedMessage() {
		StockOperationDTO dto = newReceiptDto();
		dao().getPartyByUuid(dto.getDestinationUuid()).setVoided(true);
		
		Errors errors = validate(dto);
		
		assertNotNull(errors.getFieldError("destinationUuid"));
		assertEquals("Destination is invalid", messageOf(errors.getFieldError("destinationUuid")));
	}
	
	@Test
	public void validate_shouldRejectUnknownOperationTypeWithNonNullMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setOperationType("unknown");
		Errors errors = validate(dto);
		assertTrue(errors.hasErrors());
		assertNotNull(messageOf(errors.getAllErrors().get(0)));
	}
	
	@Test
	public void validate_shouldRejectMissingOperationTypeWithNonNullMessage() {
		StockOperationDTO dto = newReceiptDto();
		dto.setOperationType(null);
		Errors errors = validate(dto);
		assertTrue(errors.hasErrors());
		for (Object error : errors.getAllErrors()) {
			assertNotNull(messageOf((org.springframework.validation.ObjectError) error),
			    "Validation errors must never carry a null message");
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
	
	// clears the session so the validator loads the location and its tags from the database
	private Errors validateReceiptWithFreshSession() {
		StockOperationDTO dto = newReceiptDto();
		Context.flushSession();
		Context.clearSession();
		return validate(dto);
	}
	
	@Test
	public void validate_shouldRejectADestinationWithoutARequiredLocationTag() {
		Context.getAdministrationService().setGlobalProperty("stockmanagement.receipt.destinationLocationTags",
		    "Some Store Tag, Another Tag");
		
		Errors errors = validateReceiptWithFreshSession();
		
		assertNotNull(errors.getFieldError("destinationUuid"));
		assertEquals("Destination location specified does not match required operation tags",
		    messageOf(errors.getFieldError("destinationUuid")));
	}
	
	@Test
	public void validate_shouldAcceptADestinationWithOneOfTheRequiredLocationTags() {
		LocationTag tag = Context.getLocationService().saveLocationTag(new LocationTag("Some Store Tag", "Marks a store"));
		Location location = Context.getLocationService().getLocation(eu().getLocation().getId());
		location.addTag(tag);
		Context.getLocationService().saveLocation(location);
		Context.getAdministrationService().setGlobalProperty("stockmanagement.receipt.destinationLocationTags",
		    "Some Store Tag, Another Tag");
		
		Errors errors = validateReceiptWithFreshSession();
		
		assertNull(errors.getFieldError("destinationUuid"));
	}
	
	@Test
	public void validate_shouldAcceptAnyDestinationWhenNoLocationTagsAreConfigured() {
		Errors errors = validateReceiptWithFreshSession();
		
		assertNull(errors.getFieldError("destinationUuid"));
	}
	
	// an adjustment happens at a location, which is its source, and has no destination
	private Errors validateAdjustmentWithFreshSession() {
		StockOperationDTO dto = newReceiptDto();
		dto.setOperationType(StockOperationType.ADJUSTMENT.getOperationType());
		dto.setSourceUuid(dto.getDestinationUuid());
		dto.setDestinationUuid(null);
		Context.flushSession();
		Context.clearSession();
		return validate(dto);
	}
	
	@Test
	public void validate_shouldRejectASourceWithoutARequiredLocationTag() {
		Context.getAdministrationService().setGlobalProperty("stockmanagement.adjustment.sourceLocationTags",
		    "Some Store Tag");
		
		Errors errors = validateAdjustmentWithFreshSession();
		
		assertNotNull(errors.getFieldError("sourceUuid"));
		assertEquals("Source location specified does not match required operation tags",
		    messageOf(errors.getFieldError("sourceUuid")));
	}
	
	@Test
	public void validate_shouldAcceptASourceWithARequiredLocationTag() {
		LocationTag tag = Context.getLocationService().saveLocationTag(new LocationTag("Some Store Tag", "Marks a store"));
		Location location = Context.getLocationService().getLocation(eu().getLocation().getId());
		location.addTag(tag);
		Context.getLocationService().saveLocation(location);
		Context.getAdministrationService().setGlobalProperty("stockmanagement.adjustment.sourceLocationTags",
		    "Some Store Tag");
		
		Errors errors = validateAdjustmentWithFreshSession();
		
		assertNull(errors.getFieldError("sourceUuid"));
	}
}
