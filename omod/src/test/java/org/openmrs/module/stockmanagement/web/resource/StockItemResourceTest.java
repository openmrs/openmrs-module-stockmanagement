package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.dto.StockItemDTO;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

public class StockItemResourceTest extends BaseModuleWebContextSensitiveTest {

	private final StockItemResource resource = new StockItemResource();

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}

	@Test
	public void save_shouldRejectAReorderLevelWithoutAUnit() {
		StockItemDTO dto = newStockItem();
		dto.setReorderLevel(BigDecimal.ONE);

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));

		assertNotNull(exception.getErrors().getFieldError("reorderLevel"));
	}

	@Test
	public void save_shouldRejectANegativeReorderLevel() {
		StockItemDTO dto = newStockItem();
		dto.setReorderLevel(BigDecimal.ONE.negate());
		dto.setReorderLevelUoMUuid("some-packaging-uom-uuid");

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));

		assertNotNull(exception.getErrors().getFieldError("reorderLevel"));
	}

	private StockItemDTO newStockItem() {
		StockItemDTO dto = new StockItemDTO();
		dto.setConceptUuid("some-concept-uuid");
		dto.setHasExpiration(false);
		return dto;
	}
}
