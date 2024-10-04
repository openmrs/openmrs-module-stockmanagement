package org.openmrs.module.stockmanagement.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import org.openmrs.module.stockmanagement.web.resource.InventoryItemProvider;
import org.openmrs.fhir.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirResourceConfig {

    @Autowired
    private FhirContext fhirContext;

    @Bean
    public InventoryItemProvider inventoryItemProvider() {
        return new InventoryItemProvider();
    }

    @Bean
    public FhirContext fhirR4Context() {
        FhirContext ctx = FhirContext.forR4();

        ctx.setDefaultTypeForProfile("http://hl7.org/fhir/StructureDefinition/InventoryItem", InventoryItem.class);

        IValidationSupport validationSupport = new DefaultProfileValidationSupport(ctx);
        ctx.setValidationSupport(validationSupport);

        return ctx;
    }

}