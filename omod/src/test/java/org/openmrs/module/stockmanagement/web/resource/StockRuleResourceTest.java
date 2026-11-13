package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.dto.StockRuleDTO;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

public class StockRuleResourceTest extends BaseModuleWebContextSensitiveTest {

	private final StockRuleResource resource = new StockRuleResource();

	@Before
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
