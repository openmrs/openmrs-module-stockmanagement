package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.dto.StockRuleDTO;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

public class StockRuleResourceTest extends BaseModuleWebContextSensitiveTest {

	private final StockRuleResource resource = new StockRuleResource();

	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}

	@Test
	public void save_shouldRejectAStockRuleWithoutAStockItemUuid() {
		StockRuleDTO dto = new StockRuleDTO();

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));

		assertNotNull(exception.getErrors().getFieldError("stockItemUuid"));
	}
}
