package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.dto.BatchJobDTO;
import org.openmrs.module.stockmanagement.api.model.BatchJobType;
import org.openmrs.module.stockmanagement.api.reporting.Report;
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
	@Test
	public void save_shouldReportMalformedParameterNamesWithAResolvedMessage() {
		BatchJobDTO dto = new BatchJobDTO();
		dto.setBatchJobType(BatchJobType.Report);
		dto.setDescription("Test report");
		dto.setParameters("param.report=" + Report.getAllReports().get(0).getSystemName() + "\ninvalid=value");

		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));

		assertNotNull(exception.getErrors().getFieldError("parameters"));
		assertEquals("Batch job parameters are invalid properties",
		    exception.getErrors().getFieldError("parameters").getCode());
	}

}
