package org.openmrs.module.stockmanagement.api.dto;

public class StockItemPackagingUomReferenceDTO {

	private Integer id;

	private String uuid;

	private boolean voided;

	private String referenceCode;

	private String name;

	private String description;

	private int stockSourceId;

	private String stockSourceUuid;

	private String stockSourceName;

	private int stockItemPackagingUomId;

	private String stockItemPackagingUomUuid;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public boolean isVoided() {
		return voided;
	}

	public void setVoided(boolean voided) {
		this.voided = voided;
	}

	public String getReferenceCode() {
		return referenceCode;
	}

	public void setReferenceCode(String referenceCode) {
		this.referenceCode = referenceCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getStockSourceId() {
		return stockSourceId;
	}

	public void setStockSourceId(int stockSourceId) {
		this.stockSourceId = stockSourceId;
	}

	public String getStockSourceUuid() {
		return stockSourceUuid;
	}

	public void setStockSourceUuid(String stockSourceUuid) {
		this.stockSourceUuid = stockSourceUuid;
	}

	public String getStockSourceName() {
		return stockSourceName;
	}

	public void setStockSourceName(String stockSourceName) {
		this.stockSourceName = stockSourceName;
	}

	public int getStockItemPackagingUomId() {
		return stockItemPackagingUomId;
	}

	public void setStockItemPackagingUomId(int stockItemPackagingUomId) {
		this.stockItemPackagingUomId = stockItemPackagingUomId;
	}

	public String getStockItemPackagingUomUuid() {
		return stockItemPackagingUomUuid;
	}

	public void setStockItemPackagingUomUuid(String stockItemPackagingUomUuid) {
		this.stockItemPackagingUomUuid = stockItemPackagingUomUuid;
	}

}
