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
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.db.StockSourceDAO;
import org.openmrs.module.stockmanagement.api.db.hibernate.HibernateStockSourceDAO;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.search.StockSourceSearch;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StockSourceDAOTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	private StockSourceDAO stockSourceDao;
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
		HibernateStockSourceDAO dao = new HibernateStockSourceDAO();
		dao.setSessionFactory(sessionFactory);
		stockSourceDao = dao;
	}
	
	@Test
	public void saveStockSource_shouldSaveAndRetrieveStockSource() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Alpha Medical");
		stockSource.setAcronym("AM");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		stockSource.setCreator(Context.getAuthenticatedUser());
		stockSource.setDateCreated(new java.util.Date());
		
		stockSourceDao.saveStockSource(stockSource);
		Context.flushSession();
		
		StockSource saved = stockSourceDao.getStockSourceByUuid(stockSource.getUuid());
		assertNotNull(saved);
		assertEquals("Alpha Medical", saved.getName());
		assertEquals("AM", saved.getAcronym());
		assertEquals(stockSource.getSourceType(), saved.getSourceType());
	}
	
	@Test
	public void getStockSources_shouldFilterBySearchCriteria() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Beta Pharma Supply");
		stockSource.setAcronym("BPS");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		stockSource.setCreator(Context.getAuthenticatedUser());
		stockSource.setDateCreated(new java.util.Date());
		stockSourceDao.saveStockSource(stockSource);
		Context.flushSession();
		
		StockSourceSearch search = new StockSourceSearch();
		search.setSearch("Beta");
		List<StockSource> results = stockSourceDao.getStockSources(search, null);
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertTrue(results.stream().anyMatch(s -> s.getName().equals("Beta Pharma Supply")));
		
		search = new StockSourceSearch();
		search.setSearch("BPS");
		results = stockSourceDao.getStockSources(search, null);
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertTrue(results.stream().anyMatch(s -> s.getAcronym().equals("BPS")));
	}
}
