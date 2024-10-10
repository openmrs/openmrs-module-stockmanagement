package org.openmrs.module.stockmanagement.config;

import ca.uhn.fhir.context.FhirContext;
import org.openmrs.module.stockmanagement.web.resource.InventoryItemProvider;
import org.openmrs.fhir.InventoryItem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StockManagementFhirResourceConfig {

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = FhirContext.forR4(); 
        fhirContext.getResourceDefinition(InventoryItem.class);
        return fhirContext;
    }

    @Bean
    public InventoryItemProvider InventoryItemProvider() {
        return new InventoryItemProvider();
    }
}
