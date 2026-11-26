/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.dao.HibernateStockInventoryDao;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.openmrs.module.stockmanagement.api.model.*;

import java.math.BigDecimal;

public class StockInventoryDaoTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private StockManagementDao daoInstance;
	
	private HibernateStockInventoryDao inventoryDaoInstance;
	
	private static EntityUtil entityUtil;
	
	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
		}
		return daoInstance;
	}
	
	private HibernateStockInventoryDao inventoryDao() {
		if (inventoryDaoInstance == null) {
			inventoryDaoInstance = new HibernateStockInventoryDao();
			inventoryDaoInstance.setSessionFactory(sessionFactory);
		}
		return inventoryDaoInstance;
	}
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
	}
	
	private EntityUtil eu() {
		if (entityUtil == null) {
			Drug drug = Context.getConceptService().getDrug(3);
			Concept concept = drug.getConcept();
			Role role = Context.getUserService().getRole("Provider");
			Patient patient = Context.getPatientService().getPatient(2);
			User user = Context.getUserService().getUser(501);
			Location location = Context.getLocationService().getLocation(1);
			entityUtil = new EntityUtil(drug, user, location, role, concept, patient);
		}
		return entityUtil;
	}
	
	@Test
	public void saveOrderItem_shouldSaveAllProperties() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		//Given
		OrderItem orderItem = eu().newOrderItem(dao(), stockItem, packagingUom);
		
		//When
		inventoryDao().saveOrderItem(orderItem);
		
		//Let's clean up the cache to be sure getOrderItemByUuid fetches from DB and not from cache
		Context.flushSession();
		Context.flushSession();
		
		//Then
		OrderItem savedOrderItem = inventoryDao().getOrderItemByUuid(orderItem.getUuid());
		assertThat(savedOrderItem, hasProperty("uuid", is(orderItem.getUuid())));
		assertThat(savedOrderItem, hasProperty("creator", is(orderItem.getCreator())));
		assertThat(savedOrderItem, hasProperty("dateCreated", is(orderItem.getDateCreated())));
		assertThat(savedOrderItem, hasProperty("changedBy", is(orderItem.getChangedBy())));
		assertThat(savedOrderItem, hasProperty("dateChanged", is(orderItem.getDateChanged())));
		assertThat(savedOrderItem, hasProperty("voided", is(orderItem.getVoided())));
		assertThat(savedOrderItem, hasProperty("dateVoided", is(orderItem.getDateVoided())));
		assertThat(savedOrderItem, hasProperty("voidedBy", is(orderItem.getVoidedBy())));
		assertThat(savedOrderItem, hasProperty("voidReason", is(orderItem.getVoidReason())));
		assertThat(savedOrderItem, hasProperty("order", is(orderItem.getOrder())));
		assertThat(savedOrderItem, hasProperty("stockItem", is(orderItem.getStockItem())));
		assertThat(savedOrderItem, hasProperty("stockItemPackagingUOM", is(orderItem.getStockItemPackagingUOM())));
		assertThat(savedOrderItem, hasProperty("createdFrom", is(orderItem.getCreatedFrom())));
		assertThat(savedOrderItem, hasProperty("fulfilmentLocation", is(orderItem.getFulfilmentLocation())));
	}
}
