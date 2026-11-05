package org.openmrs.module.stockmanagement.api.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * The persistent class for the stockmgmt_stock_item_packaging_uom_reference database table.
 * Represents an external reference (e.g. a terminology code) identifying a specific
 * stock item packaging configuration, as opposed to the parent stock item itself.
 */
@Entity(name = "stockmanagement.StockItemPackagingUomReference")
@Table(name = "stockmgmt_stock_item_packaging_uom_reference")
public class StockItemPackagingUomReference extends org.openmrs.BaseChangeableOpenmrsData implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stock_item_packaging_uom_reference_id")
	private Integer id;

	@JoinColumn(name = "stock_source_id")
	@ManyToOne(fetch = FetchType.LAZY)
	private StockSource referenceSource;

	@JoinColumn(name = "stock_item_packaging_uom_id")
	@ManyToOne(fetch = FetchType.EAGER)
	private StockItemPackagingUOM stockItemPackagingUOM;

	@Column(name = "stock_reference_code", length = 255)
	private String stockReferenceCode;

	@Column(name = "name", length = 255)
	private String name;

	@Column(name = "description", length = 1024)
	private String description;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public StockSource getReferenceSource() {
		return referenceSource;
	}

	public void setReferenceSource(StockSource referenceSource) {
		this.referenceSource = referenceSource;
	}

	public StockItemPackagingUOM getStockItemPackagingUOM() {
		return stockItemPackagingUOM;
	}

	public void setStockItemPackagingUOM(StockItemPackagingUOM stockItemPackagingUOM) {
		this.stockItemPackagingUOM = stockItemPackagingUOM;
	}

	public String getStockReferenceCode() {
		return stockReferenceCode;
	}

	public void setStockReferenceCode(String stockReferenceCode) {
		this.stockReferenceCode = stockReferenceCode;
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

}
