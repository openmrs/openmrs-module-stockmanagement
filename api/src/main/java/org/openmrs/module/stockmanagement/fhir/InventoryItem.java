package org.openmrs.module.stockmanagement.fhir;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.DomainResource;

import org.openmrs.fhir.InventoryItem.InventoryItemStatusCodes;

import java.util.List;

public class InventoryItem extends DomainResource {
    private InventoryItemStatusCodes status;
    private List<CodeableConcept> code;
    private Quantity netContent;
    private List<Identifier> identifier;

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
        return ResourceType.Basic; // Replace with the appropriate ResourceType if available
    }

    @Override
    public InventoryItem copy() {
        InventoryItem copy = new InventoryItem();
        copy.status = this.status;
        copy.code = this.code; // Assuming CodeableConcept is immutable or deep copy is not needed
        copy.netContent = this.netContent; // Assuming Quantity is immutable or deep copy is not needed
        copy.identifier = this.identifier; // Assuming Identifier is immutable or deep copy is not needed
        return copy;
    }
}