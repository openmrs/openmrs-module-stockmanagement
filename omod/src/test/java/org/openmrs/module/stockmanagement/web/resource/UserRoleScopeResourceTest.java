package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeLocationDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeOperationTypeDTO;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.api.ValidationException;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Context-sensitive test proving that a valid user role scope saves through
 * {@link UserRoleScopeResource#save}: the explicit validator run must bind to the delegate, or
 * rejectIfEmptyOrWhitespace reads the fields from an empty target and rejects every scope.
 */
public class UserRoleScopeResourceTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	DbSessionFactory sessionFactory;

	private StockManagementDao daoInstance;

	private UserRoleScopeResource resource = new UserRoleScopeResource();

	// the user a built scope DTO belongs to; set as a side effect of buildValidScopeDto
	private User targetUser;

	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
		}
		return daoInstance;
	}

	@Before
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
		grantAuthenticatedUserRoleScopesPrivileges();
	}

	private void grantAuthenticatedUserRoleScopesPrivileges() {
		Privilege mutate = Context.getUserService().getPrivilege(Privileges.TASK_STOCKMANAGEMENT_USERROLESCOPES_MUTATE);
		if (mutate == null) {
			mutate = Context.getUserService().savePrivilege(new Privilege(
			        Privileges.TASK_STOCKMANAGEMENT_USERROLESCOPES_MUTATE, "Able to create and update user role scopes"));
		}
		Privilege app = Context.getUserService().getPrivilege(Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES);
		if (app == null) {
			app = Context.getUserService()
			        .savePrivilege(new Privilege(Privileges.APP_STOCKMANAGEMENT_USERROLESCOPES,
			                "Able to view user role scopes"));
		}
		Role administrator = new Role();
		administrator.setRole("User role scope administrator");
		administrator.setDescription("Grants user role scope management");
		administrator.addPrivilege(mutate);
		administrator.addPrivilege(app);
		Context.getUserService().saveRole(administrator);
		// the authorization checks run against the user context's cached instance, so grant the
		// role there; persisting it on the user is unnecessary for these tests and saveUser
		// trips over the test dataset's admin person
		Set<Role> roles = new HashSet<>(Context.getAuthenticatedUser().getRoles());
		roles.add(administrator);
		Context.getAuthenticatedUser().setRoles(roles);
	}

	private User createScopedUser() {
		Person person = new Person();
		person.setGender("F");
		person.addName(new PersonName("Scope", null, "Target"));
		person = Context.getPersonService().savePerson(person);

		User user = new User();
		user.setPerson(person);
		user.setUsername("scopescope");
		user.addRole(Context.getUserService().getRole("Provider"));
		return Context.getUserService().createUser(user, " ScopeTest123 ");
	}

	private UserRoleScopeDTO buildValidScopeDto() {
		// a scope managed by the authenticated admin must belong to a different user
		targetUser = createScopedUser();

		StockOperationType operationType = dao().getAllStockOperationTypes().get(0);

		UserRoleScopeLocationDTO locationDto = new UserRoleScopeLocationDTO();
		locationDto.setLocationUuid(Context.getLocationService().getLocation(1).getUuid());
		locationDto.setEnableDescendants(false);

		UserRoleScopeOperationTypeDTO operationTypeDto = new UserRoleScopeOperationTypeDTO();
		operationTypeDto.setOperationTypeUuid(operationType.getUuid());

		UserRoleScopeDTO dto = new UserRoleScopeDTO();
		dto.setUserUuid(targetUser.getUuid());
		dto.setRole("Provider");
		dto.setPermanent(true);
		dto.setEnabled(true);
		List<UserRoleScopeLocationDTO> locations = new ArrayList<>();
		locations.add(locationDto);
		dto.setLocations(locations);
		List<UserRoleScopeOperationTypeDTO> operationTypes = new ArrayList<>();
		operationTypes.add(operationTypeDto);
		dto.setOperationTypes(operationTypes);
		return dto;
	}

	@Test
	public void save_shouldSaveAValidScope() {
		UserRoleScopeDTO saved = resource.save(buildValidScopeDto());

		assertNotNull(saved);
		assertNotNull(saved.getUuid());
		assertEquals(targetUser.getUuid(), saved.getUserUuid());
		assertEquals("Provider", saved.getRole());
		assertNotNull(saved.getLocations());
		assertEquals(1, saved.getLocations().size());
		assertNotNull(saved.getOperationTypes());
		assertEquals(1, saved.getOperationTypes().size());
	}

	@Test
	public void save_shouldStillRejectAnInvalidScope() {
		UserRoleScopeDTO dto = buildValidScopeDto();
		dto.setRole(null);

		try {
			resource.save(dto);
			fail("Expected a ValidationException to be thrown");
		} catch (ValidationException e) {
			assertNotNull(e.getMessage());
		}
	}
}
