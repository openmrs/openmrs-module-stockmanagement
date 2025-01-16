package org.openmrs.module.stockmanagement.web.controller;

import org.openmrs.module.stockmanagement.api.InventoryItemService;
import org.openmrs.module.stockmanagement.fhir.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/openmrs/ws/fhir2/R4/InventoryItem")
public class InventoryItemController {

    @Autowired
    private InventoryItemService inventoryItemService;

    @GetMapping("/{id}")
    public InventoryItem getInventoryItem(@PathVariable String id) {
        return inventoryItemService.read(id);
    }

    @GetMapping
    public List<InventoryItem> searchInventoryItems(@RequestParam(required = false) String code,
            @RequestParam(required = false) String status) {
        return inventoryItemService.search(code, status);
    }
}