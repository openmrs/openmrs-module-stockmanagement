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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.StockBatchDTO;
import org.openmrs.module.stockmanagement.api.dto.StockBatchSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockInventoryResult;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventorySearchFilter;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.web.StockInventoryPageableResult;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

public class StockItemInventoryResourceTest extends BaseModuleWebContextSensitiveTest {
	
	private StockItemInventoryResource resource;
	
	private StockManagementService service;
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		resource = new StockItemInventoryResource();
		service = mock(StockManagementService.class);
		resource.setStockManagementService(service);
		lenient().when(service.getStockInventory(any(StockItemInventorySearchFilter.class)))
		        .thenReturn(new StockInventoryResult(new ArrayList<>(), 0));
	}
	
	@Test
	public void doSearch_shouldSearchAllItemsWithPagingWhenNoItemFilterIsGiven() {
		RequestContext context = contextWith(Collections.emptyMap());
		
		PageableResult result = resource.doSearch(context);
		
		assertTrue(result instanceof StockInventoryPageableResult);
		StockItemInventorySearchFilter filter = capturedFilter();
		assertFalse(filter.isRequireItemGroupFilters());
		assertTrue(filter.getItemGroupFilters().isEmpty());
		assertEquals(context.getStartIndex(), filter.getStartIndex());
		assertEquals(context.getLimit(), filter.getLimit());
	}
	
	@Test
	public void doSearch_shouldReturnEmptyResultForNonNumericDrugId() {
		PageableResult result = resource.doSearch(contextWith(Collections.singletonMap("drugId", "abc")));
		
		assertFalse(result instanceof StockInventoryPageableResult);
		verify(service, never()).getStockInventory(any(StockItemInventorySearchFilter.class));
	}
	
	@Test
	public void doSearch_shouldReturnEmptyResultForNonNumericConceptId() {
		PageableResult result = resource.doSearch(contextWith(Collections.singletonMap("conceptId", "abc")));
		
		assertFalse(result instanceof StockInventoryPageableResult);
		verify(service, never()).getStockInventory(any(StockItemInventorySearchFilter.class));
	}
	
	@Test
	public void doSearch_shouldNotPagePerItemSearchesUnlessPagingIsRequested() {
		StockItem stockItem = new StockItem();
		stockItem.setId(1);
		when(service.getStockItemByUuid("item-uuid")).thenReturn(stockItem);
		RequestContext context = contextWith(Collections.singletonMap("stockItemUuid", "item-uuid"));
		
		resource.doSearch(context);
		
		StockItemInventorySearchFilter filter = capturedFilter();
		assertTrue(filter.isRequireItemGroupFilters());
		assertEquals(1, filter.getItemGroupFilters().size());
		assertEquals(Integer.valueOf(1), filter.getItemGroupFilters().get(0).getStockItemId());
		assertNull(filter.getStartIndex());
		assertNull(filter.getLimit());
	}
	
	@Test
	public void doSearch_shouldPagePerItemSearchWhenLimitIsRequested() {
		StockItem stockItem = new StockItem();
		stockItem.setId(1);
		when(service.getStockItemByUuid("item-uuid")).thenReturn(stockItem);
		Map<String, String> params = new HashMap<>();
		params.put("stockItemUuid", "item-uuid");
		params.put("limit", "2");
		RequestContext context = contextWith(params);
		context.setLimit(2);
		
		resource.doSearch(context);
		
		StockItemInventorySearchFilter filter = capturedFilter();
		assertEquals(Integer.valueOf(0), filter.getStartIndex());
		assertEquals(Integer.valueOf(2), filter.getLimit());
	}
	
	@Test
	public void doSearch_shouldPreserveExactRowOffset() {
		Map<String, String> params = new HashMap<>();
		params.put("startIndex", "1");
		params.put("limit", "2");
		RequestContext context = contextWith(params);
		context.setStartIndex(1);
		context.setLimit(2);
		
		resource.doSearch(context);
		
		StockItemInventorySearchFilter filter = capturedFilter();
		assertEquals(Integer.valueOf(1), filter.getStartIndex());
		assertEquals(Integer.valueOf(2), filter.getLimit());
	}
	
	@Test
	public void doSearch_shouldKeepFullCountWhenTotalCountIsRequestedWithPaging() {
		List<StockItemInventory> rows = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			rows.add(new StockItemInventory());
		}
		when(service.getStockInventory(any(StockItemInventorySearchFilter.class)))
		        .thenReturn(new StockInventoryResult(rows, 25));
		Map<String, String> params = new HashMap<>();
		params.put("totalBy", "StockItemOnly");
		params.put("limit", "10");
		params.put("totalCount", "true");
		RequestContext context = contextWith(params);
		context.setLimit(10);
		
		PageableResult result = resource.doSearch(context);
		
		assertTrue(result instanceof StockInventoryPageableResult);
		StockInventoryPageableResult pagedResult = (StockInventoryPageableResult) result;
		assertEquals(10, pagedResult.getPageOfResults().size());
		assertEquals(Long.valueOf(25), pagedResult.getTotalCount());
		assertTrue(pagedResult.hasMoreResults());
	}
	
	@Test
	public void doSearch_shouldNotCapPerItemSearchWhenOnlyTotalCountIsRequested() {
		StockItem stockItem = new StockItem();
		stockItem.setId(1);
		when(service.getStockItemByUuid("item-uuid")).thenReturn(stockItem);
		Map<String, String> params = new HashMap<>();
		params.put("stockItemUuid", "item-uuid");
		params.put("totalCount", "true");
		RequestContext context = contextWith(params);
		
		PageableResult result = resource.doSearch(context);
		
		StockItemInventorySearchFilter filter = capturedFilter();
		assertNull(filter.getStartIndex());
		assertNull(filter.getLimit());
		assertTrue(result instanceof StockInventoryPageableResult);
	}
	
	@Test
	public void doSearch_shouldKeepPartyRestrictionWhenNoItemFilterIsGiven() {
		Party party = new Party();
		party.setId(7);
		when(service.getPartyByUuid("party-uuid")).thenReturn(party);
		
		resource.doSearch(contextWith(Collections.singletonMap("partyUuid", "party-uuid")));
		
		StockItemInventorySearchFilter filter = capturedFilter();
		assertFalse(filter.isRequireItemGroupFilters());
		assertEquals(Arrays.asList(7), filter.getUnRestrictedPartyIds());
	}
	
	@Test
	public void doSearch_shouldKeepBatchRestrictionWhenNoItemFilterIsGiven() {
		StockBatchDTO batch = new StockBatchDTO();
		batch.setId(11);
		when(service.findStockBatches(any(StockBatchSearchFilter.class)))
		        .thenReturn(new Result<>(Collections.singletonList(batch), 1));
		
		resource.doSearch(contextWith(Collections.singletonMap("stockBatchUuid", "batch-uuid")));
		
		StockItemInventorySearchFilter filter = capturedFilter();
		assertTrue(filter.isRequireItemGroupFilters());
		assertEquals(1, filter.getItemGroupFilters().size());
		assertEquals(Integer.valueOf(11), filter.getItemGroupFilters().get(0).getFirstStockBatchId());
	}
	
	private RequestContext contextWith(Map<String, String> params) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter(anyString())).thenAnswer(invocation -> params.get(invocation.getArgument(0)));
		RequestContext context = new RequestContext();
		context.setRequest(request);
		return context;
	}
	
	private StockItemInventorySearchFilter capturedFilter() {
		ArgumentCaptor<StockItemInventorySearchFilter> captor = ArgumentCaptor
		        .forClass(StockItemInventorySearchFilter.class);
		verify(service).getStockInventory(captor.capture());
		return captor.getValue();
	}
}
