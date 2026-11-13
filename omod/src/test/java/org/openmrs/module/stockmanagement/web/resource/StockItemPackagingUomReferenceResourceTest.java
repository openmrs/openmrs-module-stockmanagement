package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.dto.StockItemPackagingUomReferenceDTO;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUomReference;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Context-sensitive test exercising {@link StockItemPackagingUomReferenceResource} against the
 * transactional service proxy, so @Authorized privilege checks and service validation run as they
 * do for real REST requests.
 */
public class StockItemPackagingUomReferenceResourceTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	DbSessionFactory sessionFactory;

	private StockManagementDao daoInstance;

	private EntityUtil entityUtil;

	private StockItemPackagingUomReferenceResource resource = new StockItemPackagingUomReferenceResource();

	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
		}
		return daoInstance;
	}

	private EntityUtil eu() {
		if (entityUtil == null) {
			entityUtil = new EntityUtil(Context.getConceptService().getDrug(3), Context.getUserService().getUser(501),
			    Context.getLocationService().getLocation(1), Context.getUserService().getRole("Provider"),
			    Context.getConceptService().getDrug(3).getConcept(), Context.getPatientService().getPatient(2));
		}
		return entityUtil;
	}

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
		grantAuthenticatedUserStockItemsPrivilege();
	}

	private void grantAuthenticatedUserStockItemsPrivilege() {
		Privilege privilege = Context.getUserService().getPrivilege(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS);
		if (privilege == null) {
			privilege = Context.getUserService().savePrivilege(new Privilege(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
			        "Able to view and manage stock items"));
		}
		Privilege mutatePrivilege = Context.getUserService()
		        .getPrivilege(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE);
		if (mutatePrivilege == null) {
			mutatePrivilege = Context.getUserService().savePrivilege(new Privilege(
			        Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE, "Able to add, edit and void stock items"));
		}
		Role clerk = new Role();
		clerk.setRole("Stock item clerk");
		clerk.setDescription("Grants access to stock items");
		clerk.addPrivilege(privilege);
		clerk.addPrivilege(mutatePrivilege);
		Context.getUserService().saveRole(clerk);
		// the authorization checks run against the user context's cached instance, so grant the
		// role there; persisting it on the user is unnecessary for these tests and saveUser
		// trips over the test dataset's admin person
		Set<Role> roles = new HashSet<>(Context.getAuthenticatedUser().getRoles());
		roles.add(clerk);
		Context.getAuthenticatedUser().setRoles(roles);
	}

	private StockSource createNonVoidedStockSource() {
		StockSource stockSource = eu().newStockSource(dao());
		stockSource.setVoided(false);
		return dao().saveStockSource(stockSource);
	}

	private StockItemPackagingUOM createNonVoidedPackagingUom() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		packagingUom.setFactor(BigDecimal.valueOf(30));
		return dao().saveStockItemPackagingUOM(packagingUom);
	}

	private StockItemPackagingUomReferenceDTO createReferenceViaResource() {
		StockItemPackagingUOM packagingUom = createNonVoidedPackagingUom();
		StockSource stockSource = createNonVoidedStockSource();

		StockItemPackagingUomReferenceDTO dto = new StockItemPackagingUomReferenceDTO();
		dto.setStockItemPackagingUomUuid(packagingUom.getUuid());
		dto.setStockSourceUuid(stockSource.getUuid());
		dto.setReferenceCode("M221680001EP30T");
		dto.setName("UNHTS packaging reference");
		dto.setDescription("UNHTS packaging reference description");
		return resource.save(dto);
	}

	private RequestContext searchContext(MockHttpServletRequest request) {
		RequestContext context = new RequestContext();
		context.setRequest(request);
		return context;
	}

	@Test
	public void save_shouldCreateReferenceAndReturnConvertedDto() {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();

		assertNotNull(saved);
		assertNotNull(saved.getUuid());
		assertEquals("M221680001EP30T", saved.getReferenceCode());
		assertEquals("UNHTS packaging reference", saved.getName());
		assertNotNull(saved.getStockItemPackagingUomUuid());
		assertNotNull(saved.getStockSourceUuid());
		assertNotNull(saved.getStockSourceName());
	}

	@Test
	public void save_shouldRejectNonExistentPackagingUom() {
		StockSource stockSource = createNonVoidedStockSource();

		StockItemPackagingUomReferenceDTO dto = new StockItemPackagingUomReferenceDTO();
		dto.setStockItemPackagingUomUuid("non-existent-packaging-uom-uuid");
		dto.setStockSourceUuid(stockSource.getUuid());
		dto.setReferenceCode("M221680001EP30T");

		try {
			resource.save(dto);
			fail("Expected an APIException to be thrown");
		} catch (APIException e) {
			assertTrue(e.getMessage().contains("packaging UOM"));
		}
	}

	@Test
	public void save_shouldRejectDuplicateActiveSourceAndCode() {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();

		StockItemPackagingUomReferenceDTO duplicate = new StockItemPackagingUomReferenceDTO();
		duplicate.setStockItemPackagingUomUuid(saved.getStockItemPackagingUomUuid());
		duplicate.setStockSourceUuid(saved.getStockSourceUuid());
		duplicate.setReferenceCode("M221680001EP30T");

		try {
			resource.save(duplicate);
			fail("Expected an APIException to be thrown");
		} catch (APIException e) {
			assertTrue(e.getMessage().contains("already exists"));
		}
	}

	@Test
	public void getByUniqueId_shouldReturnTheReference() {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();

		StockItemPackagingUomReferenceDTO fetched = resource.getByUniqueId(saved.getUuid());
		assertNotNull(fetched);
		assertEquals(saved.getUuid(), fetched.getUuid());
		assertEquals("M221680001EP30T", fetched.getReferenceCode());
	}

	@Test
	public void getByUniqueId_shouldReturnNullForUnknownUuid() {
		assertNull(resource.getByUniqueId("unknown-uuid"));
	}

	@Test
	public void doSearch_shouldListReferencesOfAPackagingUom() throws ResponseException {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("stockItemPackagingUomUuid", saved.getStockItemPackagingUomUuid());
		AlreadyPaged<StockItemPackagingUomReferenceDTO> result = (AlreadyPaged<StockItemPackagingUomReferenceDTO>) resource
		        .doSearch(searchContext(request));
		List<StockItemPackagingUomReferenceDTO> references = result.getPageOfResults();
		assertEquals(1, references.size());
		assertEquals(saved.getUuid(), references.get(0).getUuid());
	}

	@Test
	public void doSearch_shouldReturnAnEmptyResultForAnUnknownPackagingUom() throws ResponseException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("stockItemPackagingUomUuid", "unknown-packaging-uom-uuid");
		AlreadyPaged<StockItemPackagingUomReferenceDTO> result = (AlreadyPaged<StockItemPackagingUomReferenceDTO>) resource
		        .doSearch(searchContext(request));
		assertTrue(result.getPageOfResults().isEmpty());
	}

	@Test
	public void doSearch_shouldResolveAPackagingUomBySourceAndCode() throws ResponseException {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("stockSourceUuid", saved.getStockSourceUuid());
		request.setParameter("referenceCode", "M221680001EP30T");
		AlreadyPaged<StockItemPackagingUomReferenceDTO> result = (AlreadyPaged<StockItemPackagingUomReferenceDTO>) resource
		        .doSearch(searchContext(request));
		List<StockItemPackagingUomReferenceDTO> references = result.getPageOfResults();
		assertEquals(1, references.size());
		assertEquals(saved.getStockItemPackagingUomUuid(), references.get(0).getStockItemPackagingUomUuid());

		//an unknown code resolves to nothing
		request = new MockHttpServletRequest();
		request.setParameter("stockSourceUuid", saved.getStockSourceUuid());
		request.setParameter("referenceCode", "UNKNOWN-CODE");
		result = (AlreadyPaged<StockItemPackagingUomReferenceDTO>) resource.doSearch(searchContext(request));
		assertTrue(result.getPageOfResults().isEmpty());
	}

	@Test
	public void delete_shouldVoidTheReference() throws ResponseException {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();

		resource.delete(saved, "test void", searchContext(new MockHttpServletRequest()));
		Context.flushSession();
		Context.clearSession();

		StockItemPackagingUomReferenceDTO voided = resource.getByUniqueId(saved.getUuid());
		assertNotNull(voided);
		assertTrue(voided.isVoided());
	}

	@Test
	public void save_shouldAllowRecreatingAReferenceAfterVoidingIt() throws ResponseException {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();

		resource.delete(saved, "test void", searchContext(new MockHttpServletRequest()));
		Context.flushSession();
		Context.clearSession();

		// voiding must release the source/code pair so a replacement reference can reuse it
		StockItemPackagingUomReferenceDTO replacement = new StockItemPackagingUomReferenceDTO();
		replacement.setStockItemPackagingUomUuid(saved.getStockItemPackagingUomUuid());
		replacement.setStockSourceUuid(saved.getStockSourceUuid());
		replacement.setReferenceCode("M221680001EP30T");
		StockItemPackagingUomReferenceDTO recreated = resource.save(replacement);
		assertNotNull(recreated);
		assertNotNull(recreated.getUuid());
		assertNotEquals(saved.getUuid(), recreated.getUuid());
	}

	@Test
	public void saveAndDelete_shouldBeRefusedForAUserWithoutTheMutatePrivilege() throws ResponseException {
		StockItemPackagingUOM packagingUom = createNonVoidedPackagingUom();
		StockSource stockSource = createNonVoidedStockSource();
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode("M221680001EP30T");
		reference = dao().saveStockItemPackagingUomReference(reference);
		StockItemPackagingUomReferenceDTO dto = resource.convertToDTO(reference);

		// swap the clerk role (which carries the mutate task privilege) for a view-only role, then
		// restore the original roles so later tests are unaffected
		Privilege viewPrivilege = Context.getUserService().getPrivilege(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS);
		Role viewer = new Role();
		viewer.setRole("Stock item viewer");
		viewer.setDescription("Grants read access to stock items");
		viewer.addPrivilege(viewPrivilege);
		Context.getUserService().saveRole(viewer);
		Set<Role> originalRoles = new HashSet<>(Context.getAuthenticatedUser().getRoles());
		try {
			Set<Role> roles = new HashSet<>();
			roles.add(viewer);
			Context.getAuthenticatedUser().setRoles(roles);

			try {
				resource.save(dto);
				fail("Expected APIAuthenticationException to be thrown");
			} catch (APIAuthenticationException e) {
				// expected
			}

			try {
				resource.delete(dto, "test void", searchContext(new MockHttpServletRequest()));
				fail("Expected APIAuthenticationException to be thrown");
			} catch (APIAuthenticationException e) {
				// expected
			}
		} finally {
			Context.getAuthenticatedUser().setRoles(originalRoles);
		}
	}

	@Test
	public void purge_shouldNotBeSupported() throws ResponseException {
		StockItemPackagingUomReferenceDTO saved = createReferenceViaResource();
		try {
			resource.purge(saved, searchContext(new MockHttpServletRequest()));
			fail("Expected ResourceDoesNotSupportOperationException to be thrown");
		} catch (ResourceDoesNotSupportOperationException e) {
			// expected
		}
	}

	@Test
	public void entities_shouldRoundTripThroughConvertToDtoAndBack() {
		StockItemPackagingUOM packagingUom = createNonVoidedPackagingUom();
		StockSource stockSource = createNonVoidedStockSource();

		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode("M221680001EP30T");
		reference.setName("UNHTS packaging reference");
		reference = dao().saveStockItemPackagingUomReference(reference);

		StockItemPackagingUomReferenceDTO dto = resource.convertToDTO(reference);
		assertEquals(reference.getUuid(), dto.getUuid());
		assertEquals("M221680001EP30T", dto.getReferenceCode());

		StockItemPackagingUomReference converted = resource.convertFromDTO(dto);
		assertEquals(reference.getUuid(), converted.getUuid());
		assertEquals(packagingUom.getUuid(), converted.getStockItemPackagingUOM().getUuid());
		assertEquals(stockSource.getUuid(), converted.getReferenceSource().getUuid());
		assertEquals("M221680001EP30T", converted.getStockReferenceCode());
	}
}
