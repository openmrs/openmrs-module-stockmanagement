// package org.openmrs.module.stockmanagement;


// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.openmrs.fhir.InventoryItem;
// import org.openmrs.fhir.InventoryItem.InventoryItemStatusCodes;
// import org.openmrs.module.stockmanagement.api.InventoryItemService;

// import java.util.ArrayList;
// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;

// public class InventoryItemServiceTest {

//     private InventoryItemService inventoryItemService;

//     @BeforeEach
//     public void setUp() {
//         List<InventoryItem> mockInventoryItems = new ArrayList<>();

//         InventoryItem item1 = new InventoryItem();
//         item1.setStatus(InventoryItemStatusCodes.ACTIVE);
//         item1.getIdentifierFirstRep().setValue("1");
//         mockInventoryItems.add(item1);

//         InventoryItem item2 = new InventoryItem();
//         item2.setStatus(InventoryItemStatusCodes.INACTIVE);
//         item2.getIdentifierFirstRep().setValue("2");
//         mockInventoryItems.add(item2);

//         inventoryItemService = new InventoryItemService(mockInventoryItems);
//     }

//     @Test
//     public void testRead() {
//         InventoryItem item = inventoryItemService.read("1");
//         assertNotNull(item);
//         assertEquals(InventoryItemStatusCodes.ACTIVE, item.getStatus());

//         item = inventoryItemService.read("2");
//         assertNotNull(item);
//         assertEquals(InventoryItemStatusCodes.INACTIVE, item.getStatus());

//         item = inventoryItemService.read("3");
//         assertNull(item);
//     }

//     // @Test
//     // public void testSearch() {
//     //     List<InventoryItem> items = inventoryItemService.search(null, null);
//     //     assertEquals(2, items.size());

//     //     items = inventoryItemService.search(null, "active");
//     //     assertEquals(1, items.size());
//     //     assertEquals(InventoryItemStatusCodes.ACTIVE, items.get(0).getStatus());

//     //     items = inventoryItemService.search(null, "inactive");
//     //     assertEquals(1, items.size());
//     //     assertEquals(InventoryItemStatusCodes.INACTIVE, items.get(0).getStatus());

//     //     items = inventoryItemService.search("someCode", null);
//     //     assertEquals(0, items.size());
//     // }
// }