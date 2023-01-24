package org.openmrs.module.stockmanagement.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.dataexchange.DataImporter;

//import org.openmrs.module.dataexchange.DataImporter;

public class DataImport implements StartupTask {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	@Override
	public void execute() {
		log.debug("Checking the synchronization status of the location tree table");
		try {
			
			DataImporter dataImporter = Context.getRegisteredComponent("dataImporter", DataImporter.class);
			log.info("Start import of stock management privileges");
			dataImporter.importData("stockmgmt/metadata/Role_Privilege.xml");
			log.info("stock management privileges imported");
		}
		catch (Exception exception) {
			log.error("Error while synchronizing location tree", exception);
		}
	}
	
	@Override
	public int getPriority() {
		return 10;
	}
}
