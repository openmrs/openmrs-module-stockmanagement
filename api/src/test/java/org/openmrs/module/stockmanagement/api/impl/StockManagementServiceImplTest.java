/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.impl;

import org.junit.jupiter.api.Test;
import org.openmrs.User;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.util.OpenmrsConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StockManagementServiceImplTest {
	
	private static class EmailDao extends StockManagementDao {
		
		private String email;
		
		@Override
		public String getUserEmail(Integer userId) {
			return email;
		}
	}
	
	@Test
	public void shouldPreserveAcceptedEmailAddresses() {
		EmailDao dao = new EmailDao();
		StockManagementServiceImpl service = new StockManagementServiceImpl();
		service.setDao(dao);
		User user = new User(1);
		for (String email : new String[] { "a@example.org", "First.Last+tag@example.co.in", "_+&*-@sub.example.abcdefg",
		        "a@-example.org" }) {
			dao.email = email;
			assertEquals(email, service.getUserEmailAddress(user), email);
		}
	}
	
	@Test
	public void shouldPreserveRejectedEmailAddresses() {
		EmailDao dao = new EmailDao();
		StockManagementServiceImpl service = new StockManagementServiceImpl();
		service.setDao(dao);
		User user = new User(1);
		for (String email : new String[] { null, "", "plain-address", "a@localhost", "a@example.c", "a@example.abcdefgh",
		        "a@example.org.", ".a@example.org", "a..b@example.org", "a@b..org", " a@example.org", "a@example.org ",
		        "a@example.org\n", "a'b@example.org", "a@exam_ple.org" }) {
			dao.email = email;
			assertNull(service.getUserEmailAddress(user), email);
		}
	}
	
	@Test
	public void shouldHandleVeryLongAddressesWithoutStackOverflow() {
		EmailDao dao = new EmailDao();
		StockManagementServiceImpl service = new StockManagementServiceImpl();
		service.setDao(dao);
		User user = new User(1);
		StringBuilder local = new StringBuilder("a");
		for (int i = 0; i < 50000; i++) {
			local.append(".a");
		}
		dao.email = local + "@example.org";
		assertEquals(dao.email, service.getUserEmailAddress(user));
		dao.email = local + "@example.org.";
		assertNull(service.getUserEmailAddress(user));
		// a very long domain, as in the StringUtilsTest case from #76
		StringBuilder domain = new StringBuilder();
		for (int i = 0; i < 100000; i++) {
			domain.append("a.");
		}
		dao.email = "a@" + domain + "com";
		assertEquals(dao.email, service.getUserEmailAddress(user));
	}
	
	@Test
	public void shouldFallBackToNotificationAddressThenUsername() {
		EmailDao dao = new EmailDao();
		StockManagementServiceImpl service = new StockManagementServiceImpl();
		service.setDao(dao);
		User user = new User(1);
		user.setUsername("username@example.org");
		user.setUserProperty(OpenmrsConstants.USER_PROPERTY_NOTIFICATION_ADDRESS, "notification@example.org");
		dao.email = "invalid";
		assertEquals("notification@example.org", service.getUserEmailAddress(user));
		user.setUserProperty(OpenmrsConstants.USER_PROPERTY_NOTIFICATION_ADDRESS, "invalid");
		assertEquals("username@example.org", service.getUserEmailAddress(user));
		dao.email = "primary@example.org";
		assertEquals("primary@example.org", service.getUserEmailAddress(user));
	}
}
