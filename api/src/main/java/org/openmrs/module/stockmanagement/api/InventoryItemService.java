package org.openmrs.module.stockmanagement.api;

import org.openmrs.module.stockmanagement.fhir.InventoryItem;

import java.util.List;

public interface InventoryItemService {
    InventoryItem read(String id);
    List<InventoryItem> search(String code, String status);
}