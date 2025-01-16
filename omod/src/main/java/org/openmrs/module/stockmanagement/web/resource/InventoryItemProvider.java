package org.openmrs.module.stockmanagement.web.resource;

import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;

import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.stockmanagement.api.InventoryItemService;
import org.openmrs.module.stockmanagement.fhir.InventoryItem;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@R4Provider
public class InventoryItemProvider {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Search
    public Bundle searchInventoryItems(@OptionalParam(name = InventoryItem.SP_CODE) TokenParam code,
            @OptionalParam(name = InventoryItem.SP_STATUS) TokenParam status,
            RequestDetails requestDetails) {

        List<InventoryItem> items = inventoryItemService.search(
                code != null ? code.getValue() : null,
                status != null ? status.getValue() : null);

        Bundle bundle = new Bundle();
        bundle.setType(BundleType.SEARCHSET);
        bundle.setTotal(items.size());

        for (InventoryItem item : items) {
            BundleEntryComponent entry = new BundleEntryComponent();
            entry.setResource(item);
            bundle.addEntry(entry);
        }

        return bundle;
    }
}
