package org.openmrs.module.stockmanagement.fhir;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.DomainResource;
import org.openmrs.fhir.InventoryItem.InventoryItemStatusCodes;
import ca.uhn.fhir.model.api.annotation.SearchParamDefinition;
import ca.uhn.fhir.rest.gclient.TokenClientParam;

import java.util.List;

public class InventoryItem extends DomainResource {
    private InventoryItemStatusCodes status;
    private List<CodeableConcept> code;
    private Quantity netContent;
    private List<Identifier> identifier;

    @SearchParamDefinition(name = "code", path = "InventoryItem.code", description = "Search for products that match this code", type = "token")
    public static final String SP_CODE = "code";
    
    @SearchParamDefinition(name = "status", path = "InventoryItem.status", description = "The status of the item", type = "token")
    public static final String SP_STATUS = "status";
    
    public static final TokenClientParam CODE = new TokenClientParam(SP_CODE);
    public static final TokenClientParam STATUS = new TokenClientParam(SP_STATUS);

    // Getters and Setters
    public InventoryItemStatusCodes getStatus() {
        return status;
    }

    public void setStatus(InventoryItemStatusCodes status) {
        this.status = status;
    }

    public List<CodeableConcept> getCode() {
        return code;
    }

    public void setCode(List<CodeableConcept> code) {
        this.code = code;
    }

    public Quantity getNetContent() {
        return netContent;
    }

    public void setNetContent(Quantity netContent) {
        this.netContent = netContent;
    }

    public List<Identifier> getIdentifier() {
        return identifier;
    }

    public void setIdentifier(List<Identifier> identifier) {
        this.identifier = identifier;
    }

    public Identifier getIdentifierFirstRep() {
        return identifier != null && !identifier.isEmpty() ? identifier.get(0) : null;
    }

    public CodeableConcept getCodeFirstRep() {
        return code != null && !code.isEmpty() ? code.get(0) : null;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Basic;
    }

    @Override
    public InventoryItem copy() {
        InventoryItem copy = new InventoryItem();
        copy.status = this.status;
        copy.code = this.code;
        copy.netContent = this.netContent;
        copy.identifier = this.identifier;
        return copy;
    }
}
