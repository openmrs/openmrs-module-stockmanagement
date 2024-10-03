package org.openmrs.module.stockmanagement.api.impl;

import org.openmrs.module.stockmanagement.api.InventoryItemService;
import org.openmrs.module.stockmanagement.fhir.InventoryItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryItemServiceImpl implements InventoryItemService {

    private List<InventoryItem> inventoryItems;

    public InventoryItemServiceImpl() {
        this.inventoryItems = new ArrayList<>();
    }

    public InventoryItemServiceImpl(List<InventoryItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    @Override
    public InventoryItem read(String id) {
        return inventoryItems.stream()
                .filter(item -> item.getIdentifierFirstRep().getValue().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<InventoryItem> search(String code, String status) {
        return inventoryItems.stream()
                .filter(item -> (code == null || item.getCodeFirstRep().getText().equals(code)) &&
                        (status == null || (item.getStatus() != null && item.getStatus().toCode().equals(status))))
                .collect(Collectors.toList());
    }
}