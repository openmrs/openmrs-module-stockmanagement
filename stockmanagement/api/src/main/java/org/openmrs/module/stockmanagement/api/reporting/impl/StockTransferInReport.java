package org.openmrs.module.stockmanagement.api.reporting.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItem;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItemFilter;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.reporting.ReportGenerator;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.module.stockmanagement.api.utils.GlobalProperties;
import org.openmrs.module.stockmanagement.api.utils.csv.CSVWriter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.function.Function;

public class StockTransferInReport extends StockOperationLineItemReport {
	
	@Override
	protected void setLocationFilter(StockOperationLineItemFilter filter, Location location, Boolean childLocations) {
		Party party = stockManagementService.getPartyByLocation(location);
		if (party == null) {
			stockManagementService.failBatchJob(batchJob.getUuid(), "Party related to location parameter not found");
			return;
		}
		filter.setDestinationPartyId(party.getId());
		filter.setDestinationPartyChildLocations(childLocations);
	}
	
	@Override
	protected void setFilters(StockOperationLineItemFilter filter, Properties parameters) {
		StockOperationType stockOperationType = stockManagementService
		        .getStockOperationTypeByType(StockOperationType.STOCK_ISSUE);
		filter.setStockOperationTypes(Arrays.asList(stockOperationType));
		filter.setIncludeRequisitionInfo(true);
	}
	
	@Override
	protected void writeRow(CSVWriter csvWriter, StockOperationLineItem row) {
		writeLineToCsv(
		    csvWriter,
		    TIMESTAMP_FORMATTER.format(row.getDateCreated()),
		    DATE_FORMATTER.format(row.getOperationDate()),
		    row.getOperationTypeName(),
		    row.getOperationNumber(),
		    row.getRequisitionOperationNumber(),
		    String.format("%1$s %2$s", row.getCreatorFamilyName(), row.getCreatorGivenName()),
		    emptyIfNull(row.getResponsiblePerson() != null ? String.format("%1$s %2$s",
		        row.getResponsiblePersonFamilyName(), row.getResponsiblePersonGivenName()) : row.getResponsiblePersonOther()),
		    row.getCompletedDate() == null ? "" : TIMESTAMP_FORMATTER.format(row.getCompletedDate()), String.format(
		        "%1$s %2$s", row.getCompletedByFamilyName(), row.getCompletedByGivenName()), emptyIfNull(row.getRemarks()),
		    emptyIfNull(row.getSourceName()), emptyIfNull(row.getDestinationName()),
		    row.getStockItemDrugName() == null ? row.getStockItemConceptName() : row.getStockItemDrugName(), row
		            .getStockItemDrugName() == null ? "" : row.getStockItemConceptName(), emptyIfNull(row.getCommonName()),
		    emptyIfNull(row.getAcronym()), emptyIfNull(row.getStockItemCategoryName()), emptyIfNull(row.getBatchNo()), row
		            .getExpiration() != null ? DATE_FORMATTER.format(row.getExpiration()) : "",
		    row.getQuantityRequested() == null ? null : row.getQuantityRequested().toPlainString(), row
		            .getQuantityRequestedPackagingUOMName() == null ? null : row.getQuantityRequestedPackagingUOMName(), row
		            .getQuantityRequestedPackagingUOMFactor() == null ? null : row.getQuantityRequestedPackagingUOMFactor()
		            .toPlainString(), row.getQuantity().toPlainString(), row.getStockItemPackagingUOMName(), row
		            .getStockItemPackagingUOMFactor().toPlainString(), row.getStockItemDrugId() == null ? "" : row
		            .getStockItemDrugId().toString(), row.getStockItemConceptId() == null ? "" : row.getStockItemConceptId()
		            .toString());
	}
	
	@Override
	protected void writeHeaders(CSVWriter csvWriter) {
		MessageSourceService messageSourceService = Context.getMessageSourceService();
		writeLineToCsv(csvWriter, messageSourceService.getMessage("stockmanagement.report.datecreated"),
		    messageSourceService.getMessage("stockmanagement.report.operationdate"),
		    messageSourceService.getMessage("stockmanagement.report.operationtype"),
		    messageSourceService.getMessage("stockmanagement.report.operationnumber"),
		    messageSourceService.getMessage("stockmanagement.report.requisitionoperationnumber"),
		    messageSourceService.getMessage("stockmanagement.report.startedby"),
		    messageSourceService.getMessage("stockmanagement.report.responsibleperson"),
		    messageSourceService.getMessage("stockmanagement.report.completeddate"),
		    messageSourceService.getMessage("stockmanagement.report.completedby"),
		    messageSourceService.getMessage("stockmanagement.report.remarks"),
		    messageSourceService.getMessage("stockmanagement.report.source"),
		    messageSourceService.getMessage("stockmanagement.report.destination"),
		    messageSourceService.getMessage("stockmanagement.report.genericname"),
		    messageSourceService.getMessage("stockmanagement.report.tradename"),
		    messageSourceService.getMessage("stockmanagement.report.commonname"),
		    messageSourceService.getMessage("stockmanagement.report.acronym"),
		    messageSourceService.getMessage("stockmanagement.report.category"),
		    messageSourceService.getMessage("stockmanagement.report.batchno"),
		    messageSourceService.getMessage("stockmanagement.report.batchexpiry"),
		    messageSourceService.getMessage("stockmanagement.report.quantityrequested"),
		    messageSourceService.getMessage("stockmanagement.report.qtyunitrequested"),
		    messageSourceService.getMessage("stockmanagement.report.qtyunitrequestedpacksize"),
		    messageSourceService.getMessage("stockmanagement.report.quantity"),
		    messageSourceService.getMessage("stockmanagement.report.qtyunit"),
		    messageSourceService.getMessage("stockmanagement.report.packsize"),
		    messageSourceService.getMessage("stockmanagement.report.drugid"),
		    messageSourceService.getMessage("stockmanagement.report.conceptid"));
	}
	
}
