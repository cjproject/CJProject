package com.acme.customization;

public class UBLItemInfo {
	
	private String m_Name = null;
	private String m_Description = null;
	private String m_BrandName = null;
	private String m_ModelName = null;
	private String m_BuyersItemId = null;
	private String m_SellersItemId = null;
	private String m_ManufacturersItemId = null;
	private String m_CommodityClassification = null;
	private int m_MappingFieldBy = 0;
	private int m_MapInfoLineRef = 0;

	
	/**
	 * @return the Name
	 */
	public String getName() {
		return m_Name;
	}
	/**
	 * @param Name the Name to set
	 */
	public void setName(String Name) {
		this.m_Name = Name;
	}
	/**
	 * @return the Descripiton
	 */
	public String getDescription() {
		return m_Description;
	}
	/**
	 * @param Descripiton the Descripiton to set
	 */
	public void setDescription(String description) {
		this.m_Description = description;
	}
	/**
	 * @return the BrandName
	 */
	public String getBrandName() {
		return m_BrandName;
	}
	/**
	 * @param BrandName the BrandName to set
	 */
	public void setBrandName(String BrandName) {
		this.m_BrandName = BrandName;
	}
	/**
	 * @return the ModelName
	 */
	public String getModelName() {
		return m_ModelName;
	}
	/**
	 * @param ModelName the ModelName to set
	 */
	public void setModelName(String ModelName) {
		this.m_ModelName = ModelName;
	}
	/**
	 * @return the BuyersItemId
	 */
	public String getBuyersItemId() {
		return m_BuyersItemId;
	}
	/**
	 * @param BuyersItemId the BuyersItemId to set
	 */
	public void setBuyersItemId(String BuyersItemId) {
		this.m_BuyersItemId = BuyersItemId;
	}
	/**
	 * @return the SellersItemId
	 */
	public String getSellersItemId() {
		return m_SellersItemId;
	}
	/**
	 * @param SellersItemId the SellersItemId to set
	 */
	public void setSellersItemId(String SellersItemId) {
		this.m_SellersItemId = SellersItemId;
	}
	/**
	 * @return the ManufacturersItemId
	 */
	public String getManufacturersItemId() {
		return m_ManufacturersItemId;
	}
	/**
	 * @param ManufacturersItemId the ManufacturersItemId to set
	 */
	public void setManufacturersItemId(String ManufacturersItemId) {
		this.m_ManufacturersItemId = ManufacturersItemId;
	}
	public String getCommodityClassification() {
		return m_CommodityClassification;
	}
	public void setCommodityClassification(String commodityClassification) {
		this.m_CommodityClassification = commodityClassification;
	}
	public int getMappingFieldBy() {
		return m_MappingFieldBy;
	}
	public void setMappingFieldBy(int mappingFieldBy) {
		this.m_MappingFieldBy = mappingFieldBy;
	}
	public int getMapInfoLineRef() {
		return m_MapInfoLineRef;
	}
	public void setMapInfoLineRef(int mapInfoLineRef) {
		this.m_MapInfoLineRef = mapInfoLineRef;
	}

}
