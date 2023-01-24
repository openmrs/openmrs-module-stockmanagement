package org.openmrs.module.stockmanagement.api;

import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.module.stockmanagement.api.model.StockOperation;

import java.math.BigDecimal;

public interface StockOperationTypeProcessor {
	
	abstract boolean requiresReason();
	
	abstract boolean userCanProcess(User user, Location location);
	
	abstract boolean userCanProcess(User user, Location location, String privilege);
	
	abstract boolean requiresBatchUuid();
	
	abstract boolean requiresActualBatchInformation();
	
	abstract boolean requiresDispatchAcknowledgement();
	
	abstract boolean isQuantityOptional();
	
	abstract boolean canBeRelatedToRequisition();
	
	abstract boolean canCapturePurchasePrice();
	
	abstract boolean shouldVerifyNegativeStockAmountsAtSource();
	
	abstract BigDecimal getQuantityToApplyAtSource(BigDecimal quantity);
	
	/**
	 * Called when the {@link StockOperation} status is initially created and the status is
	 * StockOperationStatus.PENDING.
	 * 
	 * @param operation The associated stock operation.
	 */
	abstract void onPending(StockOperation operation);
	
	/**
	 * Called when the {@link StockOperation} status is changed to StockOperationStatus.CANCELLED.
	 * 
	 * @param operation The associated stock operation.
	 */
	abstract void onCancelled(StockOperation operation);
	
	/**
	 * Called when the {@link StockOperation} status is changed to StockOperationStatus.COMPLETED.
	 * 
	 * @param operation The associated stock operation.
	 */
	abstract void onCompleted(StockOperation operation);
	
	/**
	 * Determines weather or not negative quantities for items are allowed
	 * 
	 * @return true if negative quantities are allowed, else false
	 */
	abstract boolean isNegativeItemQuantityAllowed();
}
