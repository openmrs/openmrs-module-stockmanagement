package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.dto.BatchJobDTO;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

public class BatchJobResourceTest extends BaseModuleWebContextSensitiveTest {

	private final BatchJobResource resource = new BatchJobResource();

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}

	@Test
	public void save_shouldRejectABatchJobWithoutABatchJobType() {
		BatchJobDTO dto = new BatchJobDTO();

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));

		assertNotNull(exception.getErrors().getFieldError("batchJobType"));
	}
}
