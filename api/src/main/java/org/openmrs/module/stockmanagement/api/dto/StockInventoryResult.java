package org.openmrs.module.stockmanagement.api.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StockInventoryResult extends Result<StockItemInventory> {
	
	List<StockItemInventory> totals;
	
	public StockInventoryResult() {
	}
	
	public StockInventoryResult(List<StockItemInventory> data, long totalRecordCount) {
		super(data, totalRecordCount);
	}
	
	public List<StockItemInventory> getTotals() {
        if (totals == null || totals.isEmpty()) {
            BigDecimal totalQuantity = this.getData().stream()
                .map(StockItemInventory::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            StockItemInventory totalInventory = new StockItemInventory();
            totalInventory.setQuantity(totalQuantity);
            totals = Arrays.asList(totalInventory);;
        }
        return totals;
	}
	
	public void setTotals(List<StockItemInventory> totals) {
		this.totals = totals;
	}
}
