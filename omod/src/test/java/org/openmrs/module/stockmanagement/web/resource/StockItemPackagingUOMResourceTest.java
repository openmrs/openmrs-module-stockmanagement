package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.dto.StockItemPackagingUOMDTO;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

/**
 * Proves that {@link StockItemPackagingUOMResource#save} runs the StockItemPackagingUOMValidator
 * explicitly: webservices.rest 3.x no longer invokes it implicitly, and without it a packaging
 * unit with a zero or negative factor saved with a 201.
 */
public class StockItemPackagingUOMResourceTest extends BaseModuleWebContextSensitiveTest {

	private final StockItemPackagingUOMResource resource = new StockItemPackagingUOMResource();

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}

	@Test
	public void save_shouldRejectAPackagingUomWithANonPositiveFactor() {
		StockItemPackagingUOMDTO dto = new StockItemPackagingUOMDTO();
		dto.setPackagingUomUuid("some-packaging-uom-uuid");
		dto.setFactor(BigDecimal.ZERO);

		assertThrows(ValidationException.class, () -> resource.save(dto));
	}
}
