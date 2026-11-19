package org.openmrs.module.stockmanagement.api.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * The persistent class for the stockmgmt_stock_item_packaging_uom_reference database table.
 * Represents an external reference (e.g. a terminology code) identifying a specific
 * stock item packaging configuration, as opposed to the parent stock item itself.
 */
@Entity(name = "stockmanagement.StockItemPackagingUomReference")
@Table(name = "stockmgmt_stock_item_packaging_uom_reference", uniqueConstraints = @UniqueConstraint(
        name = "stockmgmt_stock_item_packaging_uom_reference_source_code_uk",
        columnNames = { "stock_source_id", "stock_reference_code" }))
public class StockItemPackagingUomReference extends org.openmrs.BaseChangeableOpenmrsData implements Serializable {

	//the single source of truth for the void-suffix format: the DAO builds the suffixed code with
	//it and the service's code-length guard derives its limit from it, so the two cannot drift
	public static final int CODE_MAX_LENGTH = 255;
	private static final String VOIDED_SUFFIX_FORMAT = " (voided %d)";

	/**
	 * @return the stored code with the void suffix appended, which frees the original code for
	 * reuse on the same source (the (stock_source_id, stock_reference_code) unique constraint
	 * covers voided rows too)
	 */
	public static String voidedCode(String stockReferenceCode, Integer id) {
		return stockReferenceCode + String.format(VOIDED_SUFFIX_FORMAT, id);
	}

	/**
	 * @return the longest stock reference code that can still hold the void suffix, i.e. the
	 * longest code a reference may be saved with so it stays voidable
	 */
	public static int maxSaveableCodeLength() {
		return CODE_MAX_LENGTH - String.format(VOIDED_SUFFIX_FORMAT, Integer.MAX_VALUE).length();
	}

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

	@Column(name = "stock_reference_code", length = CODE_MAX_LENGTH)
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
