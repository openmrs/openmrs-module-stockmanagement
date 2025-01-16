
package org.openmrs.module.stockmanagement.fhir;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;
import org.openmrs.fhir.InventoryItem.InventoryItemStatusCodes;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.hl7.fhir.r4.model.ResourceType;


public class InventoryItemTest {

    @Test
    public void testGetStatus() {
        InventoryItem item = new InventoryItem();
        item.setStatus(InventoryItemStatusCodes.ACTIVE);
        assertEquals(InventoryItemStatusCodes.ACTIVE, item.getStatus());
    }

    @Test
    public void testSetStatus() {
        InventoryItem item = new InventoryItem();
        item.setStatus(InventoryItemStatusCodes.INACTIVE);
        assertEquals(InventoryItemStatusCodes.INACTIVE, item.getStatus());
    }

    @Test
    public void testGetCode() {
        InventoryItem item = new InventoryItem();
        List<CodeableConcept> codes = new ArrayList<>();
        codes.add(new CodeableConcept().setText("Code1"));
        item.setCode(codes);
        assertEquals(codes, item.getCode());
    }

    @Test
    public void testSetCode() {
        InventoryItem item = new InventoryItem();
        List<CodeableConcept> codes = new ArrayList<>();
        codes.add(new CodeableConcept().setText("Code2"));
        item.setCode(codes);
        assertEquals(codes, item.getCode());
    }

    @Test
    public void testGetNetContent() {
        InventoryItem item = new InventoryItem();
        Quantity quantity = new Quantity().setValue(10);
        item.setNetContent(quantity);
        assertEquals(quantity, item.getNetContent());
    }

    @Test
    public void testSetNetContent() {
        InventoryItem item = new InventoryItem();
        Quantity quantity = new Quantity().setValue(20);
        item.setNetContent(quantity);
        assertEquals(quantity, item.getNetContent());
    }

    @Test
    public void testGetIdentifier() {
        InventoryItem item = new InventoryItem();
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(new Identifier().setSystem("System1").setValue("Value1"));
        item.setIdentifier(identifiers);
        assertEquals(identifiers, item.getIdentifier());
    }

    @Test
    public void testSetIdentifier() {
        InventoryItem item = new InventoryItem();
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(new Identifier().setSystem("System2").setValue("Value2"));
        item.setIdentifier(identifiers);
        assertEquals(identifiers, item.getIdentifier());
    }

    @Test
    public void testGetIdentifierFirstRep() {
        InventoryItem item = new InventoryItem();
        List<Identifier> identifiers = new ArrayList<>();
        Identifier identifier = new Identifier().setSystem("System3").setValue("Value3");
        identifiers.add(identifier);
        item.setIdentifier(identifiers);
        assertEquals(identifier, item.getIdentifierFirstRep());
    }

    @Test
    public void testGetCodeFirstRep() {
        InventoryItem item = new InventoryItem();
        List<CodeableConcept> codes = new ArrayList<>();
        CodeableConcept code = new CodeableConcept().setText("Code3");
        codes.add(code);
        item.setCode(codes);
        assertEquals(code, item.getCodeFirstRep());
    }

    @Test
    public void testCopy() {
        InventoryItem item = new InventoryItem();
        item.setStatus(InventoryItemStatusCodes.ACTIVE);
        List<CodeableConcept> codes = new ArrayList<>();
        codes.add(new CodeableConcept().setText("Code4"));
        item.setCode(codes);
        Quantity quantity = new Quantity().setValue(30);
        item.setNetContent(quantity);
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(new Identifier().setSystem("System4").setValue("Value4"));
        item.setIdentifier(identifiers);

        InventoryItem copy = item.copy();
        assertEquals(item.getStatus(), copy.getStatus());
        assertEquals(item.getCode(), copy.getCode());
        assertEquals(item.getNetContent(), copy.getNetContent());
        assertEquals(item.getIdentifier(), copy.getIdentifier());
    }

    @Test
    public void testGetResourceType() {
        InventoryItem item = new InventoryItem();
        assertEquals(ResourceType.Basic, item.getResourceType());
    }
}