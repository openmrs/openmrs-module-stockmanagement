/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

package org.openmrs.module.stockmanagement.api.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.openmrs.*;
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.stockmanagement.StockLocationTags;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockInventoryService;
import org.openmrs.module.stockmanagement.api.dao.StockInventoryDao;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.StockOperationPrivelegeTarget;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.dto.reporting.*;
import org.openmrs.module.stockmanagement.api.jobs.AsyncTasksBatchJob;
import org.openmrs.module.stockmanagement.api.jobs.StockBatchExpiryJob;
import org.openmrs.module.stockmanagement.api.jobs.StockRuleEvaluationJob;
import org.openmrs.module.stockmanagement.api.reporting.Report;
import org.openmrs.module.stockmanagement.api.utils.*;
import org.openmrs.module.stockmanagement.api.jobs.StockItemImportJob;
import org.openmrs.module.stockmanagement.api.model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.stockmanagement.tasks.StockOperationNotificationTask;
import org.openmrs.notification.Alert;
import org.openmrs.notification.Template;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.util.Assert;

import javax.mail.Session;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StockInventoryServiceImpl extends BaseOpenmrsService implements StockInventoryService {
	
	StockInventoryDao dao;
	
	private static final UUID DISPENSING_PROCESSING_LOCK = UUID.randomUUID();
	
	private static final Map<String, UUID> LOCATION_DISPENSING_OPERATION_LOCKS = new HashMap<>();
	
	public void setDao(StockInventoryDao dao) {
		this.dao = dao;
	}
	
	private StockManagementService stockManagementService() {
		return Context.getService(StockManagementService.class);
	}
	
	public List<PartyDTO> getCompleteStockDispensingLocationPartyList(Integer atLocationId) {
		List<LocationTree> locations = stockManagementService().getCompleteLocationTree(atLocationId);
		if (locations == null || locations.isEmpty())
			return new ArrayList<>();
		LocationService locationService = Context.getLocationService();
		List<LocationTag> locationTags = new ArrayList<>();
		LocationTag locationTag = locationService.getLocationTagByName(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG);
		if (locationTag != null)
			locationTags.add(locationTag);
		locationTag = locationService.getLocationTagByName(StockLocationTags.DISPENSARY_LOCATION_TAG);
		if (locationTag != null)
			locationTags.add(locationTag);
		
		final List<Location> stockLocations = locationService.getLocationsHavingAnyTag(locationTags);
		locations.removeIf(p -> !stockLocations.stream().anyMatch(x -> x.getId().equals(p.getChildLocationId())));
		if (locations == null || locations.isEmpty())
			return new ArrayList<>();
		
		PartySearchFilter partySearchFilter = new PartySearchFilter();
		partySearchFilter.setIncludeVoided(false);
		partySearchFilter.setLocationIds(locations.stream().map(p -> p.getChildLocationId()).collect(Collectors.toList()));
		return stockManagementService().findParty(partySearchFilter).getData();
	}
	
	public void validateStockInventoryAfterOperation(StockOperation stockOperation, StockOperationType stockOperationType,
	        MessageSourceService messageSourceService) {
		List<StockOperationItem> applicableStockOperationItems = stockOperation.getStockOperationItems().stream()
		        .filter(p -> !p.getVoided()).collect(Collectors.toList());
		List<StockItemInventorySearchFilter.ItemGroupFilter> itemsToSearch = applicableStockOperationItems.stream()
		        .filter(p -> !p.getVoided()).map(p -> {
			        StockItemInventorySearchFilter.PartyStockItemBatch group = new StockItemInventorySearchFilter.PartyStockItemBatch(
			                stockOperation.getSource().getId(), p.getStockItem().getId(), p.getStockBatch().getId());
			        return group;
		        }).collect(Collectors.groupingBy(p -> p)).entrySet().stream()
		        .map(p -> new StockItemInventorySearchFilter.ItemGroupFilter(Arrays.asList(p.getKey().getPartyId()),
		                p.getKey().getStockItemId(), p.getKey().getStockBatchId()))
		        .collect(Collectors.toList());
		
		StockItemInventorySearchFilter searchFilter = new StockItemInventorySearchFilter();
		searchFilter.setItemGroupFilters(itemsToSearch);
		if (stockOperationType.getAllowExpiredBatchNumbers() != null && stockOperationType.getAllowExpiredBatchNumbers()) {
			searchFilter.setRequireNonExpiredStockBatches(false);
		}
		List<StockItemInventory> stockItemInventories = dao.getStockItemInventory(searchFilter, null).getData();
		
		List<String> errors = new ArrayList<>();
		
		boolean uomPriorityIsBigToSmall = GlobalProperties.uomPriorityIsBigToSmall();
		
		for (StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter : itemsToSearch) {
			
			List<StockOperationItem> itemGroupItems = applicableStockOperationItems.stream()
			        .filter(p -> !p.getVoided() && p.getStockItem().getId().equals(itemGroupFilter.getStockItemId())
			                && p.getStockBatch().getId().equals(itemGroupFilter.getFirstStockBatchId()))
			        .collect(Collectors.toList());
			
			BigDecimal batchEffect = itemGroupItems.stream()
			        .map(p -> stockOperationType
			                .getQuantityToApplyAtSource(p.getQuantity().multiply(p.getStockItemPackagingUOM().getFactor())))
			        .reduce(BigDecimal.ZERO, BigDecimal::add);
			
			Optional<StockItemInventory> batchCurrentBalance = stockItemInventories.stream()
			        .filter(p -> p.getStockItemId().equals(itemGroupFilter.getStockItemId())
			                && p.getStockBatchId().equals(itemGroupFilter.getFirstStockBatchId()))
			        .findAny();
			
			BigDecimal balance = BigDecimal.ZERO;
			if (batchCurrentBalance.isPresent()) {
				balance = batchCurrentBalance.get().getQuantity();
			}
			BigDecimal netEffectBalance = balance.add(batchEffect);
			if (netEffectBalance.compareTo(BigDecimal.ZERO) < 0) {
				StockOperationItem stockOperationItem = itemGroupItems.get(0);
				String stockItemName = null;
				if (stockOperationItem.getStockItem().getDrug() != null) {
					stockItemName = stockOperationItem.getStockItem().getDrug().getName();
					if (stockOperationItem.getStockItem().getConcept() != null) {
						stockItemName = stockItemName + " ("
						        + stockOperationItem.getStockItem().getConcept().getDisplayString() + ")";
					}
				} else if (stockOperationItem.getStockItem().getConcept() != null) {
					stockItemName = stockOperationItem.getStockItem().getConcept().getDisplayString();
				}
				
				BigDecimal displayUnitValue = netEffectBalance;
				String displayUom = null;
				for (StockOperationItem itemInGroup : itemGroupItems) {
					displayUnitValue = netEffectBalance.divide(itemInGroup.getStockItemPackagingUOM().getFactor(), 5,
					    BigDecimal.ROUND_HALF_EVEN);
					if (displayUnitValue.abs().compareTo(BigDecimal.ZERO) > 0) {
						displayUom = itemInGroup.getStockItemPackagingUOM().getPackagingUom().getDisplayString();
						break;
					}
				}
				if (StringUtils.isBlank(displayUom)) {
					StockItemPackagingUOMSearchFilter uomFilter = new StockItemPackagingUOMSearchFilter();
					uomFilter.setStockItemIds(Arrays.asList(stockOperationItem.getStockItem().getId()));
					Result<StockItemPackagingUOMDTO> uoms = dao.findStockItemPackagingUOMs(uomFilter);
					if (!uoms.getData().isEmpty()) {
						StockItemPackagingUOMDTO preferredUoM = getPreferredPackagingUoM(netEffectBalance, uoms.getData(),
						    false, uomPriorityIsBigToSmall,
						    stockOperationItem.getStockItemPackagingUOM() != null
						            ? stockOperationItem.getStockItemPackagingUOM().getId()
						            : null);
						displayUnitValue = netEffectBalance.divide(preferredUoM.getFactor(), 5, BigDecimal.ROUND_HALF_EVEN);
						displayUom = preferredUoM.getPackagingUomName();
					}
				}
				
				if (displayUom == null) {
					displayUnitValue = netEffectBalance;
				}
				
				errors.add(String.format(
				    messageSourceService.getMessage("stockmanagement.stockoperation.itembatchbalancenegativeafteroperation"),
				    stockItemName, stockOperationItem.getStockBatch().getBatchNo(),
				    NumberFormatUtil.qtyDisplayFormat(displayUnitValue), displayUom));
			}
		}
		if (!errors.isEmpty()) {
			throw new StockManagementException(String.join(", ", errors));
		}
	}
	
	public List<ReservedTransaction> calculateStockIssueAdjustments(StockOperation stockOperation,
	        StockOperationType stockOperationType, MessageSourceService messageSourceService) {
		List<StockOperationItem> applicableStockOperationItems = stockOperation.getStockOperationItems().stream()
		        .filter(p -> !p.getVoided()).collect(Collectors.toList());
		List<StockItemInventorySearchFilter.ItemGroupFilter> itemsToSearch = applicableStockOperationItems.stream()
		        .filter(p -> !p.getVoided()).map(p -> {
			        StockItemInventorySearchFilter.PartyStockItemBatch group = new StockItemInventorySearchFilter.PartyStockItemBatch(
			                stockOperation.getSource().getId(), p.getStockItem().getId(), p.getStockBatch().getId());
			        return group;
		        }).collect(Collectors.groupingBy(p -> p)).entrySet().stream()
		        .map(p -> new StockItemInventorySearchFilter.ItemGroupFilter(Arrays.asList(p.getKey().getPartyId()),
		                p.getKey().getStockItemId(), p.getKey().getStockBatchId()))
		        .collect(Collectors.toList());
		
		StockItemInventorySearchFilter searchFilter = new StockItemInventorySearchFilter();
		searchFilter.setItemGroupFilters(itemsToSearch);
		List<StockItemInventory> stockItemInventories = dao.getStockItemInventory(searchFilter, null).getData();
		boolean uomPriorityIsBigToSmall = GlobalProperties.uomPriorityIsBigToSmall();
		List<ReservedTransaction> reservedTransactions = new ArrayList<ReservedTransaction>();
		for (StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter : itemsToSearch) {
			
			List<StockOperationItem> itemGroupItems = applicableStockOperationItems.stream()
			        .filter(p -> !p.getVoided() && p.getStockItem().getId().equals(itemGroupFilter.getStockItemId())
			                && p.getStockBatch().getId().equals(itemGroupFilter.getFirstStockBatchId()))
			        .collect(Collectors.toList());
			
			BigDecimal expectedBalance = itemGroupItems.stream()
			        .map(p -> stockOperationType
			                .getQuantityToApplyAtSource(p.getQuantity().multiply(p.getStockItemPackagingUOM().getFactor())))
			        .reduce(BigDecimal.ZERO, BigDecimal::add);
			
			Optional<StockItemInventory> currentBatchBalance = stockItemInventories.stream()
			        .filter(p -> p.getStockItemId().equals(itemGroupFilter.getStockItemId())
			                && p.getStockBatchId().equals(itemGroupFilter.getFirstStockBatchId()))
			        .findAny();
			
			BigDecimal currentBalance = BigDecimal.ZERO;
			if (currentBatchBalance.isPresent()) {
				currentBalance = currentBatchBalance.get().getQuantity();
			}
			
			BigDecimal adjustment = expectedBalance.subtract(currentBalance);
			BigDecimal finalAdjustmentUnitValue = null;
			StockItemPackagingUOM finalAdjustmentUom = null;
			BigDecimal absAdjustment = adjustment.abs();
			for (StockOperationItem itemInGroup : itemGroupItems) {
				
				if (absAdjustment.remainder(itemInGroup.getStockItemPackagingUOM().getFactor())
				        .compareTo(BigDecimal.ZERO) == 0) {
					finalAdjustmentUom = itemInGroup.getStockItemPackagingUOM();
					finalAdjustmentUnitValue = adjustment.divide(itemInGroup.getStockItemPackagingUOM().getFactor(), 5,
					    BigDecimal.ROUND_HALF_EVEN);
					break;
				}
			}
			if (finalAdjustmentUom == null) {
				StockItemPackagingUOMSearchFilter uomFilter = new StockItemPackagingUOMSearchFilter();
				uomFilter.setStockItemIds(Arrays.asList(itemGroupItems.get(0).getStockItem().getId()));
				Result<StockItemPackagingUOMDTO> uoms = dao.findStockItemPackagingUOMs(uomFilter);
				if (!uoms.getData().isEmpty()) {
					StockItemPackagingUOMDTO preferredUoM = getPreferredPackagingUoM(absAdjustment, uoms.getData(), false,
					    uomPriorityIsBigToSmall,
					    itemGroupItems.get(0).getStockItemPackagingUOM() != null
					            ? itemGroupItems.get(0).getStockItemPackagingUOM().getId()
					            : null);
					finalAdjustmentUom = dao.getStockItemPackagingUOMByUuid(preferredUoM.getUuid());
					finalAdjustmentUnitValue = adjustment.divide(preferredUoM.getFactor(), 5, BigDecimal.ROUND_HALF_EVEN);
				}
			}
			
			if (finalAdjustmentUom == null) {
				StockOperationItem stockOperationItem = itemGroupItems.get(0);
				String stockItemName = null;
				if (stockOperationItem.getStockItem().getDrug() != null) {
					stockItemName = stockOperationItem.getStockItem().getDrug().getName();
					if (stockOperationItem.getStockItem().getConcept() != null) {
						stockItemName = stockItemName + " ("
						        + stockOperationItem.getStockItem().getConcept().getDisplayString() + ")";
					}
				} else if (stockOperationItem.getStockItem().getConcept() != null) {
					stockItemName = stockOperationItem.getStockItem().getConcept().getDisplayString();
				}
				throw new StockManagementException(
				        String.format(messageSourceService.getMessage("stockmanagement.stockoperation.nocompatibleuom"),
				            stockItemName, adjustment.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
			}
			
			boolean assignedFirstItem = false;
			for (StockOperationItem stockOperationItem : itemGroupItems) {
				ReservedTransaction tx = new ReservedTransaction(stockOperation, stockOperationItem);
				tx.setCreator(Context.getAuthenticatedUser());
				tx.setDateCreated(new Date());
				tx.setParty(stockOperation.getSource());
				stockOperation.addReservedTransaction(tx);
				if (assignedFirstItem) {
					tx.setQuantity(BigDecimal.ZERO);
				} else {
					tx.setQuantity(finalAdjustmentUnitValue);
					tx.setStockItemPackagingUOM(finalAdjustmentUom);
					assignedFirstItem = true;
				}
				reservedTransactions.add(tx);
			}
		}
		
		return reservedTransactions;
	}
	
	public void dispenseStockItems(List<DispenseRequest> dispenseRequests) {
		if (dispenseRequests == null || dispenseRequests.isEmpty())
			return;
		MessageSourceService messageSourceService = Context.getMessageSourceService();
		
		Map<String, StockItem> stockItemMap = new HashMap<>();
		Map<String, StockBatch> stockBatchMap = new HashMap<>();
		Map<String, StockItemPackagingUOM> stockItemPackagingUOMMap = new HashMap<>();
		Map<Integer, Patient> patientMap = new HashMap<>();
		Map<Integer, Order> orderMap = new HashMap<>();
		Map<Integer, Encounter> encounterMap = new HashMap<>();
		Map<String, Location> locationMap = new HashMap<>();
		boolean uomPriorityIsBigToSmall = GlobalProperties.uomPriorityIsBigToSmall();
		List<DispenseRequestProcessingInfo> processingInformation = new ArrayList<>();
		
		for (DispenseRequest dispenseRequest : dispenseRequests) {
			DispenseRequestProcessingInfo processingInfo = new DispenseRequestProcessingInfo();
			Location location = null;
			if (dispenseRequest.getLocationUuid() != null) {
				location = locationMap.getOrDefault(dispenseRequest.getLocationUuid(), null);
				if (location == null) {
					location = Context.getLocationService().getLocationByUuid(dispenseRequest.getLocationUuid());
					if (location != null) {
						locationMap.putIfAbsent(dispenseRequest.getLocationUuid(), location);
					}
				}
			}
			if (location == null) {
				throw new StockManagementException(
				        messageSourceService.getMessage("stockmanagement.dispenseoperation.locationrequired"));
			}
			processingInfo.setLocation(location);
			processingInfo.setParty(stockManagementService().getPartyByLocation(location));
			
			StockItem stockItem = null;
			if (!StringUtils.isBlank(dispenseRequest.getStockItemUuid())) {
				stockItem = stockItemMap.getOrDefault(dispenseRequest.getStockItemUuid(), null);
				if (stockItem == null) {
					stockItem = dao.getStockItemByUuid(dispenseRequest.getStockItemUuid());
					if (stockItem != null) {
						stockItemMap.putIfAbsent(dispenseRequest.getStockItemUuid(), stockItem);
					}
				}
			}
			if (stockItem == null) {
				throw new StockManagementException(
				        messageSourceService.getMessage("stockmanagement.dispenseoperation.stockitemrequired"));
			}
			processingInfo.setStockItem(stockItem);
			
			StockBatch stockBatch = null;
			if (!StringUtils.isBlank(dispenseRequest.getStockBatchUuid())) {
				stockBatch = stockBatchMap.getOrDefault(dispenseRequest.getStockBatchUuid(), null);
				if (stockBatch == null) {
					stockBatch = dao.getStockBatchByUuid(dispenseRequest.getStockBatchUuid());
					if (stockBatch != null) {
						stockItemMap.putIfAbsent(dispenseRequest.getStockBatchUuid(), stockItem);
					}
				}
			}
			if (stockBatch == null) {
				throw new StockManagementException(
				        messageSourceService.getMessage("stockmanagement.dispenseoperation.stockbatchrequired"));
			}
			processingInfo.setStockBatch(stockBatch);
			
			Patient patient = null;
			if (dispenseRequest.getPatientId() != null) {
				patient = patientMap.getOrDefault(dispenseRequest.getPatientId(), null);
				if (patient == null) {
					patient = Context.getPatientService().getPatient(dispenseRequest.getPatientId());
					if (patient != null) {
						patientMap.putIfAbsent(dispenseRequest.getPatientId(), patient);
					}
				}
			}
			if (patient == null) {
				throw new StockManagementException(
				        messageSourceService.getMessage("stockmanagement.dispenseoperation.patientrequired"));
			}
			processingInfo.setPatient(patient);
			
			if (dispenseRequest.getOrderId() != null) {
				Order order = orderMap.getOrDefault(dispenseRequest.getOrderId(), null);
				if (order == null) {
					order = Context.getOrderService().getOrder(dispenseRequest.getOrderId());
					if (order != null) {
						orderMap.putIfAbsent(dispenseRequest.getOrderId(), order);
					}
				}
				if (order == null) {
					throw new StockManagementException(
					        messageSourceService.getMessage("stockmanagement.dispenseoperation.validorderrequired"));
				}
				processingInfo.setOrder(order);
			}
			
			if (dispenseRequest.getEncounterId() != null) {
				Encounter encounter = encounterMap.getOrDefault(dispenseRequest.getEncounterId(), null);
				if (encounter == null) {
					encounter = Context.getEncounterService().getEncounter(dispenseRequest.getEncounterId());
					if (encounter != null) {
						encounterMap.putIfAbsent(dispenseRequest.getEncounterId(), encounter);
					}
				}
				if (encounter == null) {
					throw new StockManagementException(
					        messageSourceService.getMessage("stockmanagement.dispenseoperation.validencounterrequired"));
				}
				processingInfo.setEncounter(encounter);
			}
			
			if (dispenseRequest.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
				throw new StockManagementException(
				        messageSourceService.getMessage("stockmanagement.dispenseoperation.quantitygreaterthanzero"));
			}
			processingInfo.setQuantity(dispenseRequest.getQuantity());
			
			StockItemPackagingUOM stockItemPackagingUOM = null;
			if (dispenseRequest.getStockItemPackagingUOMUuid() != null) {
				stockItemPackagingUOM = stockItemPackagingUOMMap.getOrDefault(dispenseRequest.getStockItemPackagingUOMUuid(),
				    null);
				if (stockItemPackagingUOM == null) {
					stockItemPackagingUOM = dao
					        .getStockItemPackagingUOMByUuid(dispenseRequest.getStockItemPackagingUOMUuid());
					if (stockItemPackagingUOM != null) {
						stockItemPackagingUOMMap.putIfAbsent(dispenseRequest.getStockItemPackagingUOMUuid(),
						    stockItemPackagingUOM);
					}
				}
			}
			if (stockItemPackagingUOM == null) {
				throw new StockManagementException(
				        messageSourceService.getMessage("stockmanagement.dispenseoperation.packaginguomrequired"));
			}
			processingInfo.setPackagingUOM(stockItemPackagingUOM);
			processingInformation.add(processingInfo);
		}
		
		// get corresponding locations
		final Map<String, List<Party>> partyMap = dao.getPartyListByLocations(locationMap.values()).stream()
		        .collect(Collectors.groupingBy(p -> p.getLocation().getUuid().toLowerCase()));
		
		// prepare the stock inventory search filters
		List<StockItemInventorySearchFilter.ItemGroupFilter> itemsToSearch = processingInformation.stream().map(p -> {
			StockItemInventorySearchFilter.PartyStockItemBatch group = new StockItemInventorySearchFilter.PartyStockItemBatch(
			        p.getParty().getId(), p.getStockItem().getId(), p.getStockBatch().getId());
			return group;
		}).collect(Collectors.groupingBy(p -> p)).entrySet().stream()
		        .map(p -> new StockItemInventorySearchFilter.ItemGroupFilter(Arrays.asList(p.getKey().getPartyId()),
		                p.getKey().getStockItemId(), p.getKey().getStockBatchId()))
		        .collect(Collectors.toList());
		
		// Ensure all the location dispensing locks are setup
		for (Map.Entry<String, Location> location : locationMap.entrySet()) {
			if (!LOCATION_DISPENSING_OPERATION_LOCKS.containsKey(location.getKey().toLowerCase())) {
				synchronized (DISPENSING_PROCESSING_LOCK) {
					if (!LOCATION_DISPENSING_OPERATION_LOCKS.containsKey(location.getKey().toLowerCase())) {
						LOCATION_DISPENSING_OPERATION_LOCKS.putIfAbsent(location.getKey().toLowerCase(), UUID.randomUUID());
					}
				}
			}
		}
		
		Map<String, List<DispenseRequestProcessingInfo>> dispenseLocationGroups = processingInformation.stream()
		        .collect(Collectors.groupingBy(p -> p.getLocation().getUuid().toLowerCase()));
		for (Map.Entry<String, List<DispenseRequestProcessingInfo>> dispenseGroup : dispenseLocationGroups.entrySet()) {
			int locationPartyId = partyMap.get(dispenseGroup.getKey()).get(0).getId();
			StockItemInventorySearchFilter searchFilter = new StockItemInventorySearchFilter();
			searchFilter.setItemGroupFilters(
			    itemsToSearch.stream().filter(p -> p.getPartyIds().contains(locationPartyId)).collect(Collectors.toList()));
			if (searchFilter.getItemGroupFilters().isEmpty()) {
				throw new StockManagementException("Item group filters mismatch");
			}
			
			UUID lockObject = LOCATION_DISPENSING_OPERATION_LOCKS.get(dispenseGroup.getKey());
			synchronized (lockObject) {
				List<StockItemInventory> stockItemInventories = dao.getStockItemInventory(searchFilter, null).getData();
				List<String> errors = new ArrayList<>();
				for (StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter : searchFilter.getItemGroupFilters()) {
					List<DispenseRequestProcessingInfo> itemGroupItems = dispenseGroup.getValue().stream()
					        .filter(p -> p.getStockItem().getId().equals(itemGroupFilter.getStockItemId())
					                && p.getStockBatch().getId().equals(itemGroupFilter.getFirstStockBatchId()))
					        .collect(Collectors.toList());
					
					StockItem stockItem = itemGroupItems.get(0).getStockItem();
					StockBatch stockBatch = itemGroupItems.get(0).getStockBatch();
					
					BigDecimal batchEffect = itemGroupItems.stream().map(
					    p -> p.getQuantity().multiply(p.getPackagingUOM().getFactor()).multiply(BigDecimal.valueOf(-1)))
					        .reduce(BigDecimal.ZERO, BigDecimal::add);
					
					Optional<StockItemInventory> batchCurrentBalance = stockItemInventories.stream()
					        .filter(p -> p.getStockItemId().equals(itemGroupFilter.getStockItemId())
					                && p.getStockBatchId().equals(itemGroupFilter.getFirstStockBatchId()))
					        .findAny();
					
					BigDecimal balance = BigDecimal.ZERO;
					if (batchCurrentBalance.isPresent()) {
						balance = batchCurrentBalance.get().getQuantity();
					}
					BigDecimal netEffectBalance = balance.add(batchEffect);
					if (netEffectBalance.compareTo(BigDecimal.ZERO) < 0) {
						String stockItemName = null;
						if (stockItem.getDrug() != null) {
							stockItemName = stockItem.getDrug().getName();
							if (stockItem.getConcept() != null) {
								stockItemName = stockItemName + " (" + stockItem.getConcept().getDisplayString() + ")";
							}
						} else if (stockItem.getConcept() != null) {
							stockItemName = stockItem.getConcept().getDisplayString();
						}
						
						BigDecimal displayUnitValue = netEffectBalance;
						String displayUom = null;
						for (DispenseRequestProcessingInfo itemInGroup : itemGroupItems) {
							displayUnitValue = netEffectBalance.divide(itemInGroup.getPackagingUOM().getFactor(), 5,
							    BigDecimal.ROUND_HALF_EVEN);
							if (displayUnitValue.abs().compareTo(BigDecimal.ZERO) > 0) {
								displayUom = itemInGroup.getPackagingUOM().getPackagingUom().getDisplayString();
								break;
							}
						}
						if (StringUtils.isBlank(displayUom)) {
							StockItemPackagingUOMSearchFilter uomFilter = new StockItemPackagingUOMSearchFilter();
							uomFilter.setStockItemIds(Arrays.asList(stockItem.getId()));
							Result<StockItemPackagingUOMDTO> uoms = dao.findStockItemPackagingUOMs(uomFilter);
							if (!uoms.getData().isEmpty()) {
								StockItemPackagingUOMDTO preferredUoM = getPreferredPackagingUoM(netEffectBalance,
								    uoms.getData(), true, uomPriorityIsBigToSmall, null);
								displayUnitValue = netEffectBalance.divide(preferredUoM.getFactor(), 5,
								    BigDecimal.ROUND_HALF_EVEN);
								displayUom = preferredUoM.getPackagingUomName();
							}
						}
						
						if (displayUom == null) {
							displayUnitValue = netEffectBalance;
						}
						
						errors.add(String.format(
						    messageSourceService.getMessage(
						        "stockmanagement.stockoperation.itembatchbalancenegativeafteroperation"),
						    stockItemName, stockBatch.getBatchNo(), NumberFormatUtil.qtyDisplayFormat(displayUnitValue),
						    displayUom));
					}
				}
				if (!errors.isEmpty()) {
					throw new StockManagementException(String.join(", ", errors));
				}
				
				Party party = partyMap.get(dispenseGroup.getKey()).get(0);
				for (DispenseRequestProcessingInfo item : dispenseGroup.getValue()) {
					StockItemTransaction stockItemTransaction = new StockItemTransaction();
					stockItemTransaction.setParty(party);
					stockItemTransaction.setPatient(item.getPatient());
					stockItemTransaction.setCreator(Context.getAuthenticatedUser());
					stockItemTransaction.setDateCreated(new Date());
					stockItemTransaction.setStockItem(item.getStockItem());
					stockItemTransaction.setStockBatch(item.getStockBatch());
					stockItemTransaction.setQuantity(item.getQuantity().multiply(BigDecimal.valueOf(-1)));
					stockItemTransaction.setStockItemPackagingUOM(item.getPackagingUOM());
					if (item.getOrder() != null) {
						stockItemTransaction.setOrder(item.getOrder());
					}
					if (item.getEncounter() != null) {
						stockItemTransaction.setEncounter(item.getEncounter());
					}
					dao.saveStockItemTransaction(stockItemTransaction);
					
				}
			}
		}
	}
	
	public void deleteReservedTransations(Integer stockOperationId) {
		dao.deleteReservedTransations(stockOperationId);
	}
	
	public List<StockItemInventory> getStockBatchLocationInventory(List<Integer> stockBatchIds) {
		List<StockItemInventory> result = dao.getStockBatchLocationInventory(stockBatchIds);
		if (!result.isEmpty()) {
			StockItemPackagingUOMSearchFilter uomFilter = new StockItemPackagingUOMSearchFilter();
			uomFilter.setStockItemIds(
			    result.stream().map(p -> p.getStockItemId()).filter(p -> p != null).collect(Collectors.toList()));
			Map<Integer, List<StockItemPackagingUOMDTO>> uoms = dao.findStockItemPackagingUOMs(uomFilter).getData().stream()
			        .collect(Collectors.groupingBy(StockItemPackagingUOMDTO::getStockItemId));
			if (!uoms.isEmpty()) {
				boolean uomPriorityIsBigToSmall = GlobalProperties.uomPriorityIsBigToSmall();
				for (Map.Entry<Integer, List<StockItemPackagingUOMDTO>> entry : uoms.entrySet()) {
					for (StockItemInventory stockItemInventory : result.stream()
					        .filter(p -> p.getStockItemId().equals(entry.getKey())).collect(Collectors.toList())) {
						List<StockItemPackagingUOMDTO> uomList = entry.getValue();
						StockItemPackagingUOMDTO preferredUoM = getPreferredPackagingUoM(stockItemInventory.getQuantity(),
						    uomList, false, uomPriorityIsBigToSmall, null);
						stockItemInventory.setQuantity(stockItemInventory.getQuantity().divide(preferredUoM.getFactor(), 5,
						    BigDecimal.ROUND_HALF_EVEN));
						stockItemInventory.setQuantityUoM(preferredUoM.getPackagingUomName());
						stockItemInventory.setQuantityUoMUuid(preferredUoM.getUuid());
					}
				}
			}
		}
		
		return result;
	}
	
	public StockInventoryResult getStockItemInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		return dao.getStockItemInventory(filter, recordPrivilegeFilters);
	}
	
	public StockInventoryResult getStockInventory(StockItemInventorySearchFilter filter) {
		HashSet<RecordPrivilegeFilter> recordPrivilegeFilters = null;
		
		if (!filter.dispensing()) {
			recordPrivilegeFilters = getRecordPrivilegeFilters(Context.getAuthenticatedUser(), null, null,
			    Privileges.APP_STOCKMANAGEMENT_STOCKITEMS);
			if (recordPrivilegeFilters == null || recordPrivilegeFilters.isEmpty())
				return new StockInventoryResult(new ArrayList<>(), 0);
		}
		return getStockInventory(filter, recordPrivilegeFilters);
	}
	
	public StockInventoryResult getStockInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		if (filter == null || (recordPrivilegeFilters != null && recordPrivilegeFilters.isEmpty())) {
			return new StockInventoryResult(new ArrayList<>(), 0);
		}
		StockInventoryResult result = dao.getStockItemInventory(filter, recordPrivilegeFilters);
		return postProcessInventoryResult(filter, result);
	}
	
	public StockInventoryResult postProcessInventoryResult(StockItemInventorySearchFilter filter,
	        StockInventoryResult result) {
		Map<Integer, String> partyNames = null;
		Map<Integer, StockBatchDTO> stockBatchNames = null;
		if (!result.getData().isEmpty()) {
			if (filter.getDoSetPartyNameField()) {
				List<Integer> partyIds = result.getData().stream().filter(p -> p.getPartyId() != null)
				        .map(p -> p.getPartyId()).distinct().collect(Collectors.toList());
				if (!partyIds.isEmpty()) {
					partyNames = dao.getPartyNames(partyIds);
				}
			}
			
			if (filter.getDoSetBatchFields()) {
				List<Integer> batchIds = result.getData().stream().filter(p -> p.getStockBatchId() != null)
				        .map(p -> p.getStockBatchId()).distinct().collect(Collectors.toList());
				if (!batchIds.isEmpty()) {
					StockBatchSearchFilter stockBatchSearchFilter = new StockBatchSearchFilter();
					stockBatchSearchFilter.setStockBatchIds(batchIds);
					Result<StockBatchDTO> stockBatches = stockManagementService().findStockBatches(stockBatchSearchFilter);
					if (!stockBatches.getData().isEmpty()) {
						stockBatchNames = stockBatches.getData().stream()
						        .collect(Collectors.toMap(StockBatchDTO::getId, p -> p));
					}
				}
			}
			
			if (partyNames != null || stockBatchNames != null) {
				for (StockItemInventory stockItemInventory : result.getData()) {
					if (partyNames != null && stockItemInventory.getPartyId() != null) {
						stockItemInventory.setPartyName(partyNames.getOrDefault(stockItemInventory.getPartyId(), null));
					}
					if (stockBatchNames != null && stockItemInventory.getStockBatchId() != null) {
						StockBatchDTO batch = stockBatchNames.getOrDefault(stockItemInventory.getStockBatchId(), null);
						if (batch != null) {
							stockItemInventory.setBatchNumber(batch.getBatchNo());
							stockItemInventory.setExpiration(batch.getExpiration());
							stockItemInventory.setStockBatchUuid(batch.getUuid());
						}
					}
				}
			}
			
			if (filter.getTotalBy() != null) {
				switch (filter.getTotalBy()) {
					case LocationStockItem:
						result.setTotals(result.getData().stream()
						        .collect(Collectors.groupingBy(p -> new Pair<>(p.getPartyId(), p.getStockItemId())))
						        .entrySet().stream().map(p -> {
							        StockItemInventory stockItemInventory = new StockItemInventory();
							        StockItemInventory inventory = p.getValue().get(0);
							        stockItemInventory.setPartyId(inventory.getPartyId());
							        stockItemInventory.setLocationUuid(inventory.getLocationUuid());
							        stockItemInventory.setStockItemId(inventory.getStockItemId());
							        stockItemInventory.setQuantity(p.getValue().stream().map(x -> x.getQuantity())
							                .reduce(BigDecimal.ZERO, BigDecimal::add));
							        return stockItemInventory;
						        }).collect(Collectors.toList()));
						break;
					case LocationStockItemBatchNo:
						result.setTotals(result.getData());
						break;
					case StockItemOnly:
						result.setTotals(result.getData().stream().collect(Collectors.groupingBy(p -> p.getStockItemId()))
						        .entrySet().stream().map(p -> {
							        StockItemInventory stockItemInventory = new StockItemInventory();
							        StockItemInventory inventory = p.getValue().get(0);
							        stockItemInventory.setStockItemId(inventory.getStockItemId());
							        stockItemInventory.setQuantity(p.getValue().stream().map(x -> x.getQuantity())
							                .reduce(BigDecimal.ZERO, BigDecimal::add));
							        return stockItemInventory;
						        }).collect(Collectors.toList()));
						break;
				}
			}
			
			if (filter.getDoSetQuantityUoM()) {
				boolean uomPriorityIsBigToSmall = GlobalProperties.uomPriorityIsBigToSmall();
				StockItemPackagingUOMSearchFilter uomFilter = new StockItemPackagingUOMSearchFilter();
				uomFilter.setStockItemIds(result.getData().stream().map(p -> p.getStockItemId()).filter(p -> p != null)
				        .collect(Collectors.toList()));
				if (filter.dispensing()) {
					uomFilter.setIncludeDispensingUnit(true);
				}
				if (!uomFilter.getStockItemIds().isEmpty()) {
					Map<Integer, List<StockItemPackagingUOMDTO>> uoms = dao.findStockItemPackagingUOMs(uomFilter).getData()
					        .stream().collect(Collectors.groupingBy(StockItemPackagingUOMDTO::getStockItemId));
					if (!uoms.isEmpty()) {
						
						for (Map.Entry<Integer, List<StockItemPackagingUOMDTO>> entry : uoms.entrySet()) {
							// When dispensing, trim non-dispensing units if the stock item has a unit for dispensing already setup.
							if (filter.dispensing()) {
								Optional<StockItemPackagingUOMDTO> dispensingUnit = entry.getValue().stream()
								        .filter(p -> p.getIsDispensingUnit() && p.getStockItemDispensingUnitId() != null)
								        .findFirst();
								if (dispensingUnit.isPresent()) {
									// remove the rest of other units and leave only the dispensing unit.
									entry.getValue().removeIf(p -> !p.getId().equals(dispensingUnit.get().getId()));
									// override the UoM name with the dispensing name.
									dispensingUnit.get()
									        .setPackagingUomName(dispensingUnit.get().getStockItemDispensingUnitName());
								}
							}
							
							for (StockItemInventory stockItemInventory : result.getData().stream()
							        .filter(p -> p.getStockItemId().equals(entry.getKey())).collect(Collectors.toList())) {
								List<StockItemPackagingUOMDTO> uomList = entry.getValue();
								StockItemPackagingUOMDTO preferredUoM = getPreferredPackagingUoM(
								    stockItemInventory.getQuantity(), uomList, false, uomPriorityIsBigToSmall, null);
								stockItemInventory.setQuantity(stockItemInventory.getQuantity()
								        .divide(preferredUoM.getFactor(), 5, BigDecimal.ROUND_HALF_EVEN));
								stockItemInventory.setQuantityUoM(preferredUoM.getPackagingUomName());
								stockItemInventory.setQuantityUoMUuid(preferredUoM.getUuid());
								stockItemInventory.setQuantityFactor(preferredUoM.getFactor());
							}
							
							if (filter.getTotalBy() != null && filter
							        .getTotalBy() != StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo) {
								for (StockItemInventory stockItemInventory : result.getTotals().stream()
								        .filter(p -> p.getStockItemId().equals(entry.getKey()))
								        .collect(Collectors.toList())) {
									List<StockItemPackagingUOMDTO> uomList = entry.getValue();
									StockItemPackagingUOMDTO preferredUoM = getPreferredPackagingUoM(
									    stockItemInventory.getQuantity(), uomList, false, uomPriorityIsBigToSmall, null);
									stockItemInventory.setQuantity(stockItemInventory.getQuantity()
									        .divide(preferredUoM.getFactor(), 5, BigDecimal.ROUND_HALF_EVEN));
									stockItemInventory.setQuantityUoM(preferredUoM.getPackagingUomName());
									stockItemInventory.setQuantityUoMUuid(preferredUoM.getUuid());
									stockItemInventory.setQuantityFactor(preferredUoM.getFactor());
								}
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	@Override
	public StockItemTransaction getStockItemTransactionByUuid(String uuid) {
		return dao.getStockItemTransactionByUuid(uuid);
	}
	
	@Override
	public StockItemTransaction saveStockItemTransaction(StockItemTransaction stockItemTransaction) {
		return dao.saveStockItemTransaction(stockItemTransaction);
	}
	
	public Result<StockItemTransactionDTO> findStockItemTransactions(StockItemTransactionSearchFilter filter) {
		HashSet<RecordPrivilegeFilter> recordPrivilegeFilters = getRecordPrivilegeFilters(Context.getAuthenticatedUser(),
		    null, null, Privileges.APP_STOCKMANAGEMENT_STOCKITEMS);
		if (recordPrivilegeFilters == null || recordPrivilegeFilters.isEmpty())
			return new Result<>(new ArrayList<>(), 0);
		Result<StockItemTransactionDTO> result = dao.findStockItemTransactions(filter, recordPrivilegeFilters);
		return result;
	}
	
	public List<OrderItem> getOrderItemsByOrder(Integer... orderIds) {
		return dao.getOrderItemsByOrder(orderIds);
	}
	
	public List<OrderItem> getOrderItemsByEncounter(Integer... encounterIds) {
		return dao.getOrderItemsByEncounter(encounterIds);
	}
	
	public Result<OrderItemDTO> findOrderItems(OrderItemSearchFilter filter) {
		HashSet<RecordPrivilegeFilter> recordPrivilegeFilters = getRecordPrivilegeFilters(Context.getAuthenticatedUser(),
		    null, null, Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_DISPENSE_QTY);
		if (recordPrivilegeFilters == null || recordPrivilegeFilters.isEmpty())
			return new Result<>(new ArrayList<>(), 0);
		return dao.findOrderItems(filter, recordPrivilegeFilters);
	}
	
	public Result<OrderItemDTO> findOrderItems(OrderItemSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		if (recordPrivilegeFilters != null && recordPrivilegeFilters.isEmpty())
			return new Result<>(new ArrayList<>(), 0);
		return dao.findOrderItems(filter, recordPrivilegeFilters);
	}
	
	public OrderItem saveOrderItem(OrderItem orderItem) {
		return dao.saveOrderItem(orderItem);
	}
	
	public void setStockItemCurrentBalanceWithDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities) {
		dao.setStockItemCurrentBalanceWithDescendants(stockRuleCurrentQuantities);
	}
	
	public void setStockItemCurrentBalanceWithoutDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities) {
		dao.setStockItemCurrentBalanceWithoutDescendants(stockRuleCurrentQuantities);
	}
	
	public <T extends StockItemInventory> void getStockInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters, Function<T, Boolean> consumer, Class<T> resultClass) {
		dao.getStockInventory(filter, recordPrivilegeFilters, consumer, resultClass);
	}
	
	public void setStockItemInformation(List<StockItemInventory> reportStockItemInventories) {
		dao.setStockItemInformation(reportStockItemInventories);
	}
	
	public Result<DispensingLineItem> findDispensingLineItems(DispensingLineFilter filter) {
		return dao.findDispensingLineItems(filter);
	}
	
	public Result<PrescriptionLineItem> findPrescriptionLineItems(PrescriptionLineFilter filter) {
		return dao.findPrescriptionLineItems(filter);
	}
	
	public Result<StockItemInventory> getLeastMovingStockInventory(StockItemInventorySearchFilter filter) {
		return dao.getLeastMovingStockInventory(filter);
	}
	
	public Result<StockItemInventory> getMostMovingStockInventory(StockItemInventorySearchFilter filter) {
		return dao.getMostMovingStockInventory(filter);
	}
	
	public void getStockInventoryForecastData(StockItemInventorySearchFilter filter, Function<Object[], Boolean> consumer) {
		dao.getStockInventoryForecastData(filter, consumer);
	}
	
	public void getStockInventoryExpiryForecastData(StockItemInventorySearchFilter filter,
	        Function<Object[], Boolean> consumer) {
		dao.getStockInventoryExpiryForecastData(filter, consumer);
	}
	
	private StockItemPackagingUOMDTO getPreferredPackagingUoM(BigDecimal value, List<StockItemPackagingUOMDTO> uoms,
	        boolean isDispensing, boolean uomPriorityIsBigToSmall, Integer preferredStockItemPackagingUOMId) {
		if (isDispensing || !uomPriorityIsBigToSmall) {
			// When dispensing or the priority is small to big, we order the units from small pack size to biggest
			uoms.sort(Comparator.comparing(StockItemPackagingUOMDTO::getIsDefaultStockOperationsUoM).reversed()
			        .thenComparing(StockItemPackagingUOMDTO::getFactor));
		} else {
			// When not dispensing or the priority is big to small, we order the units from small pack size to biggest
			uoms.sort(Comparator.comparing(StockItemPackagingUOMDTO::getIsDefaultStockOperationsUoM).reversed()
			        .thenComparing(Comparator.comparing(StockItemPackagingUOMDTO::getFactor).reversed()));
		}
		StockItemPackagingUOMDTO foundUoM = null;
		boolean checkedPreferred = false;
		boolean checkPreferred = preferredStockItemPackagingUOMId != null;
		boolean thisIsThePreferred = false;
		for (StockItemPackagingUOMDTO uom : uoms) {
			if (checkPreferred) {
				thisIsThePreferred = preferredStockItemPackagingUOMId.equals(uom.getId());
			}
			if (value.divide(uom.getFactor(), 5, BigDecimal.ROUND_HALF_EVEN).abs().compareTo(BigDecimal.valueOf(1)) >= 0) {
				// If we have no preferred, we return the first uom
				if (!checkPreferred)
					return uom;
				// If this is the preferred, return it immediately
				if (thisIsThePreferred)
					return uom;
				// If we already checked the preferred and it did not quality, return the first uom that was found or the current one
				if (checkedPreferred)
					return foundUoM != null ? foundUoM : uom;
				if (foundUoM == null) {
					foundUoM = uom;
				}
			}
			if (thisIsThePreferred) {
				checkedPreferred = true;
			}
		}
		return (checkedPreferred && foundUoM != null) ? foundUoM : (uoms.isEmpty() ? null : uoms.get(0));
	}
	
	public Map<Integer, Boolean> checkStockBatchHasTransactionsAfterOperation(Integer stockOperationId,
	        List<Integer> stockBatchIds) {
		return dao.checkStockBatchHasTransactionsAfterOperation(stockOperationId, stockBatchIds);
	}
	
	public HashSet<RecordPrivilegeFilter> getRecordPrivilegeFilters(User user, Location location,
	        StockOperationType stockOperationType, String stockManagementPrivilege) {
		HashSet<RecordPrivilegeFilter> target = new HashSet<>();
		List<Location> allLocations = Context.getLocationService().getAllLocations(true);
		HashMap<String, Integer> operationTypes = new HashMap<>();
		Set<Role> allRoles = user.getAllRoles();
		List<PrivilegeScope> privilegeScopes = dao.getFlattenedUserRoleScopesByUser(user, allRoles, location,
		    stockOperationType);
		Map<String, List<PrivilegeScope>> roleGroups = privilegeScopes.stream()
		        .collect(Collectors.groupingBy(PrivilegeScope::getRole));
		for (Map.Entry<String, List<PrivilegeScope>> entry : roleGroups.entrySet()) {
			Optional<Role> role = allRoles.stream().filter(p -> p.getRole().equals(entry.getKey())).findFirst();
			if (!role.isPresent()) {
				continue;
			}
			boolean hasPrivilege = Stream
			        .concat(role.get().getPrivileges().stream(),
			            role.get().getAllParentRoles().stream().map(p -> p.getPrivileges()).flatMap(Collection::stream))
			        .map(p -> p.getPrivilege()).filter(p -> (stockManagementPrivilege == null
			                || p.equals(stockManagementPrivilege) && Privileges.ALL.contains(p)))
			        .count() > 0;
			if (!hasPrivilege)
				continue;
			
			for (PrivilegeScope scope : entry.getValue()) {
				RecordPrivilegeFilter recordPrivilegeFilter = new RecordPrivilegeFilter();
				Optional<Location> scopeLocation = allLocations.stream()
				        .filter(p -> p.getUuid().equals(scope.getLocationUuid())).findFirst();
				if (!scopeLocation.isPresent())
					continue;
				recordPrivilegeFilter.setLocationId(scopeLocation.get().getId());
				if (!operationTypes.containsKey(scope.getOperationTypeUuid())) {
					StockOperationType stockOperationTypeEntity = dao
					        .getStockOperationTypeByUuid(scope.getOperationTypeUuid());
					if (stockOperationTypeEntity != null) {
						recordPrivilegeFilter.setOperationTypeId(stockOperationTypeEntity.getId());
						operationTypes.putIfAbsent(scope.getOperationTypeUuid(), stockOperationTypeEntity.getId());
					} else {
						continue;
					}
				} else {
					recordPrivilegeFilter.setOperationTypeId(operationTypes.get(scope.getOperationTypeUuid()));
				}
				target.add(recordPrivilegeFilter);
			}
		}
		return target;
	}
}
