package org.openmrs.module.stockmanagement.web.resource;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.stockmanagement.api.InventoryItemService;
import org.openmrs.module.stockmanagement.fhir.InventoryItem;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class InventoryItemProviderTest {

    @Mock
    private InventoryItemService inventoryItemService;

    @InjectMocks
    private InventoryItemProvider inventoryItemProvider;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void searchInventoryItems_shouldReturnBundleWithItems() {
        // Arrange
        TokenParam code = new TokenParam("testCode");
        TokenParam status = new TokenParam("testStatus");
        RequestDetails requestDetails = mock(RequestDetails.class);

        InventoryItem item1 = new InventoryItem();
        InventoryItem item2 = new InventoryItem();
        List<InventoryItem> items = Arrays.asList(item1, item2);

        when(inventoryItemService.search("testCode", "testStatus")).thenReturn(items);

        // Act
        Bundle result = inventoryItemProvider.searchInventoryItems(code, status, requestDetails);

        // Assert
        assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getEntry().size());
        assertEquals(item1, ((InventoryItem) result.getEntry().get(0).getResource()));
        assertEquals(item2, ((InventoryItem) result.getEntry().get(1).getResource()));

        verify(inventoryItemService, times(1)).search("testCode", "testStatus");
    }

    @Test
    public void searchInventoryItems_shouldReturnEmptyBundleWhenNoItemsFound() {
        // Arrange
        TokenParam code = new TokenParam("testCode");
        TokenParam status = new TokenParam("testStatus");
        RequestDetails requestDetails = mock(RequestDetails.class);

        when(inventoryItemService.search("testCode", "testStatus")).thenReturn(Arrays.asList());

        // Act
        Bundle result = inventoryItemProvider.searchInventoryItems(code, status, requestDetails);

        // Assert
        assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getEntry().size());

        verify(inventoryItemService, times(1)).search("testCode", "testStatus");
    }

    @Test
    public void searchInventoryItems_shouldHandleNullParameters() {
        // Arrange
        RequestDetails requestDetails = mock(RequestDetails.class);

        InventoryItem item1 = new InventoryItem();
        List<InventoryItem> items = Arrays.asList(item1);

        when(inventoryItemService.search(null, null)).thenReturn(items);

        // Act
        Bundle result = inventoryItemProvider.searchInventoryItems(null, null, requestDetails);

        // Assert
        assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getEntry().size());
        assertEquals(item1, ((InventoryItem) result.getEntry().get(0).getResource()));

        verify(inventoryItemService, times(1)).search(null, null);
    }
}