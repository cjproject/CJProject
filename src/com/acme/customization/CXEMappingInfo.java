package com.acme.customization;

import java.io.FileOutputStream;
import java.util.ArrayList;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.appobjects.GOBODeptAlias;
import com.lbs.appobjects.GOBOOrgUnitAlias;
import com.lbs.appobjects.GOConstants;
import com.lbs.appobjects.LbsUserLoginInfo;
import com.lbs.control.interfaces.ILbsComboBox;
import com.lbs.controls.JLbsComboBox;
import com.lbs.controls.JLbsComboEdit;
import com.lbs.data.objects.BusinessObjects;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.data.objects.CustomBusinessObjects;
import com.lbs.data.objects.ObjectPropertyList;
import com.lbs.data.query.IQueryFactory;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryBusinessObjects;
import com.lbs.data.query.QueryParams;
import com.lbs.grid.interfaces.ILbsGridBase;
import com.lbs.grids.JLbsObjectListGrid;
import com.lbs.remoteclient.IClientContext;
import com.lbs.unity.UnityConstants;
import com.lbs.unity.UnityHelper;
import com.lbs.unity.am.AMConstants;
import com.lbs.unity.bo.UNBOARPReference;
import com.lbs.unity.lo.LOConstants;
import com.lbs.unity.mm.MMConstants;
import com.lbs.unity.mm.bo.MMBOItemLink;
import com.lbs.unity.mm.bo.MMBOUomDefinition;
import com.lbs.util.JLbsStringListEx;
import com.lbs.util.QueryUtil;
import com.lbs.xui.ILbsXUIPane;
import com.lbs.xui.JLbsXUIAdapter;
import com.lbs.xui.JLbsXUIPane;
import com.lbs.xui.JLbsXUITypes;
import com.lbs.xui.customization.JLbsXUIControlEvent;
import com.lbs.xui.customization.JLbsXUIGridEvent;
import com.primavera.integration.client.bo.object.Project;

public class CXEMappingInfo extends JLbsXUIAdapter{
	
	private static final String FORM_NAME_MAPPING_INFO_RECEIVED = "CXFMappingInfoReceived.lfrm";
	private static final String FORM_NAME_MAPPING_INFO_SENDED = "CXFMappingInfoSended.lfrm";
	
	private String m_FormName = "";
	
	private JLbsObjectListGrid m_Grid;
	private JLbsXUIControlEvent m_Event = null;
	private JLbsXUIPane m_Container = null;
	private IClientContext m_Context = null;
	
	private boolean m_FromPostBox = false;
	private boolean m_Default = false;
	
	private int m_MapType = ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED;
	
	private CustomBusinessObjects m_MapInfoLines = new CustomBusinessObjects();
	
	public void onGetFormVariableValue(JLbsXUIControlEvent event)
	{
		LbsUserLoginInfo userLoginInfo = (LbsUserLoginInfo) m_Context.getVariable(GOConstants.ms_UserInfo);
		CustomBusinessObject mappingInfoLines = (CustomBusinessObject) m_Grid.getRowObject(m_Grid.getSelectedRow());
		switch (event.getTag())
		{
		
			case 1: // company
				event.setReturnObject(Integer.valueOf(userLoginInfo.getFirm().getLogicalRef()));
				break;
			case 3: 
				event.setReturnObject(Integer.valueOf(LOConstants.APPLY_TYPE_ROW));
				break;
			case 9999:
				event.setReturnObject(Integer.valueOf(UnityHelper.getCompanyParamInt(m_Context, UnityConstants.MM_ORGWHCONNECTION)));
				break;
			case 36:
				event.setReturnObject(ProjectUtilEInv.getBOIntFieldValue(mappingInfoLines, "MCardType"));
		}
	}

	public void onInitialize(JLbsXUIControlEvent event)
	{
		m_Event = event;
		m_Container = event.getContainer();
		m_Context = event.getClientContext();
		m_Grid =  (JLbsObjectListGrid) m_Container.getComponentByTag(1000);
		
		m_MapType = event.getContainer().getAuthorizationId();
		
		CustomBusinessObject mappingInfo = (CustomBusinessObject) m_Container.getData();
		if(ProjectUtilEInv.getMemberValue(mappingInfo, "FromPostBox") != null)
			m_FromPostBox = true;
		
		if(ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "IsDefault") == 1)
			m_Default = true;
		
		if (!(m_Container.getMode() == JLbsXUITypes.XUIMODE_VIEWONLY))
			setPermanentStates();
		
		m_MapInfoLines = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(mappingInfo,
				m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED ? "MappInfoReceivedLines"
						: "MappInfoSendedLines");
		
		if(m_Container.getMode() == JLbsXUITypes.XUIMODE_DBENTRY)
		{
			if(m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED)
			{
				JLbsComboBox searchOrder1 = (JLbsComboBox) m_Container.getComponentByTag(3000018);
				searchOrder1.setSelectedIndex(ProjectGlobalsEInv.ITEM_MAPPING_SEARCH_FIELD_CODE);//eþlemesi olmayan malzemeler için arama sýrasý
			}
			setMappInfoProperties(mappingInfo);
			CustomBusinessObject mappInfoLine = ProjectUtilEInv.createNewCBO("CBOMappingInfoLine");
			m_MapInfoLines.add(setMappInfoLineProperties(mappInfoLine));
		}
		else
		{
			ArrayList itemRefs = new ArrayList();
			ArrayList discExpRefs = new ArrayList();
			for(int i=0; i< m_MapInfoLines.size();i++)
			{
				CustomBusinessObject mappInfoLine = (CustomBusinessObject) m_MapInfoLines.get(i);
				int mCardType = ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MCardType");
				int mCardRef = ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MCardRef");
				switch (mCardType) 
				{
					case ProjectGlobalsEInv.MAPPING_CARD_TYPE_DISCOUNT_PURCH:
					case ProjectGlobalsEInv.MAPPING_CARD_TYPE_DISCOUNT_SALES:
					case ProjectGlobalsEInv.MAPPING_CARD_TYPE_EXPENSE_PURCH:
					case ProjectGlobalsEInv.MAPPING_CARD_TYPE_EXPENSE_SALES:
						discExpRefs.add(mCardRef);;
						break;
					default:
						itemRefs.add(mCardRef);
						break;

				}
				
				if (ProjectUtilEInv.getMemberValue(mappInfoLine, "Item") == null)
					ProjectUtilEInv.setMemberValue(mappInfoLine, "Item", new MMBOItemLink());
				if (ProjectUtilEInv.getMemberValue(mappInfoLine, "Units") == null)
					ProjectUtilEInv.setMemberValue(mappInfoLine, "Units", new BusinessObjects<MMBOUomDefinition>());
				CustomBusinessObjects units = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(mappInfoLine, "Units");
				fillUnitList(units.toArray(), mappInfoLine, ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MUnitRef"));
			}
			setMappingCodes(itemRefs, discExpRefs);
		}
		if(m_Default)
		{
		  ProjectUtilEInv.setMemberValue(mappingInfo, "Sender", "[Öndeðer]");
		  ProjectUtilEInv.setMemberValue(mappingInfo, "ArpInfo", new UNBOARPReference());
		  UNBOARPReference arpInfo = (UNBOARPReference) ProjectUtilEInv.getMemberValue(mappingInfo, "ArpInfo");
		  arpInfo.setCode("[Öndeðer]");
		  arpInfo.setDescription("[Öndeðer]");
		  m_Container.resetValueByTag(101);
		  m_Container.resetValueByTag(102);
		  m_Container.resetValueByTag(103);
		  
		}
	}
	
	private void setMappingCodes(ArrayList itemRefs, ArrayList discExpRefs) 
	{

		QueryBusinessObjects items = new QueryBusinessObjects();
		QueryParams params = new QueryParams();
		params.getMainTableParams().setOrder("byReference", false);
		params.getVariables().put("V_REFLST", itemRefs.toArray());
		IQueryFactory qryFactory = m_Context.getQueryFactory();
		try
		{
			boolean ok = qryFactory.select("MMQOReferencedItems", params, items, -1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		QueryBusinessObjects discExps = new QueryBusinessObjects();
		params = new QueryParams();
		params.getMainTableParams().setOrder("byReference", false);
		params.getVariables().put("V_REFLST", discExpRefs.toArray());
		try
		{
			boolean ok = qryFactory.select("MMQOReferencedDECards", params, discExps, -1);
			if (UnityHelper.hasValue(discExps))
				for (int i = 0; i < m_MapInfoLines.size(); i++)
				{
					CustomBusinessObject mappInfoLine = (CustomBusinessObject) m_MapInfoLines.get(i);
					int mCardType = ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MCardType");
					switch (mCardType) 
					{
						case ProjectGlobalsEInv.MAPPING_CARD_TYPE_DISCOUNT_PURCH:
						case ProjectGlobalsEInv.MAPPING_CARD_TYPE_DISCOUNT_SALES:
						case ProjectGlobalsEInv.MAPPING_CARD_TYPE_EXPENSE_PURCH:
						case ProjectGlobalsEInv.MAPPING_CARD_TYPE_EXPENSE_SALES:
							setDiscExpCode(mappInfoLine, discExps);
							break;
						default:
							setItemCode(mappInfoLine, items);
							break;

					}
				}
		}
		catch (Exception ex)
		{
			m_Context.getLogger().error("MMQOReferencedDECards query could not be executed properly :", ex);
		}
		
	}

	private void setItemCode(CustomBusinessObject mappInfoLine,	QueryBusinessObjects items) 
	{
		for (int i = 0; i < items.size(); i++)
		{
			QueryBusinessObject item = (QueryBusinessObject) items.get(i);
			ObjectPropertyList props = item.getProperties();
			int logRef = QueryUtil.getIntProp(props, "Reference");
			if (logRef == ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MCardRef"))
			{
				ProjectUtilEInv.setMemberValue(mappInfoLine, "ItemCode", QueryUtil.getStringProp(props, "Code"));
				ProjectUtilEInv.setMemberValue(mappInfoLine, "ItemDesc", QueryUtil.getStringProp(props, "Description"));
				return;
			}
		}
		
	}

	private void setDiscExpCode(CustomBusinessObject mappInfoLine,	QueryBusinessObjects discExps)
	{
		for (int i = 0; i < discExps.size(); i++)
		{
			QueryBusinessObject discExp = (QueryBusinessObject) discExps.get(i);
			ObjectPropertyList props = discExp.getProperties();
			int logRef = QueryUtil.getIntProp(props, "Reference");
			if (logRef == ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MCardRef"))
			{
				ProjectUtilEInv.setMemberValue(mappInfoLine, "ItemCode", QueryUtil.getStringProp(props, "Code"));
				ProjectUtilEInv.setMemberValue(mappInfoLine, "ItemDesc", QueryUtil.getStringProp(props, "Description"));
				return;
			}
		}
		
	}

	private void setPermanentStates()
	{
		if (m_Default)
		{
			m_Container.setPermanentStateByTag(101, JLbsXUITypes.XUISTATE_RESTRICTED); //arp code
			
		}
		else
		{
			m_Container.setPermanentStateByTag(3000022, JLbsXUITypes.XUISTATE_EXCLUDED); // görüntüleme þablonu label
			m_Container.setPermanentStateByTag(3000023, JLbsXUITypes.XUISTATE_EXCLUDED); // görüntüleme þablonu
			m_Container.setPermanentStateByTag(3000021, JLbsXUITypes.XUISTATE_EXCLUDED); // görüntüleme þablonu grubu
			if (m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED)
			{
				m_Container.setPermanentStateByTag(3000009, JLbsXUITypes.XUISTATE_RESTRICTED); // Eþleme Ýçin Kullanýlacak Alan
				m_Container.setPermanentStateByTag(3000012, JLbsXUITypes.XUISTATE_RESTRICTED); // Arama Sýrasý
			}
		}
	}
	
	
	private void setGridColumnPermStates(JLbsXUIControlEvent event)
	{
		//
		
	}
	
	private CustomBusinessObject setMappInfoProperties(CustomBusinessObject mappingInfo)
	{
		ProjectUtilEInv.setMemberValue(mappingInfo, "Sender", "");
		ProjectUtilEInv.setMemberValue(mappingInfo, "ArpRef", 0);
		ProjectUtilEInv.setMemberValue(mappingInfo, "OrgUnitRef", 0);
		ProjectUtilEInv.setMemberValue(mappingInfo, "WHRef", 0); 
		ProjectUtilEInv.setMemberValue(mappingInfo, "DeptRef", 0);
		ProjectUtilEInv.setMemberValue(mappingInfo, "MapType", m_MapType);
		ProjectUtilEInv.setMemberValue(mappingInfo, "ArpInfo", new UNBOARPReference());
		ProjectUtilEInv.setMemberValue(mappingInfo, "SourceDP", new GOBODeptAlias());
		ProjectUtilEInv.setMemberValue(mappingInfo, "SourceOU", new GOBOOrgUnitAlias());
		ProjectUtilEInv.setMemberValue(mappingInfo, "SourceWH", new GOBOOrgUnitAlias());
		return mappingInfo;
	}

	private CustomBusinessObject setMappInfoLineProperties(CustomBusinessObject mappInfoLine)
	{
		ProjectUtilEInv.setMemberValue(mappInfoLine, "CardType", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "Code", ""); 
		ProjectUtilEInv.setMemberValue(mappInfoLine, "UnitCode", "");
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MCardType", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MCardRef", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MUnitRef", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MUnitSetRef", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MGlobUnitCode", "");
		ProjectUtilEInv.setMemberValue(mappInfoLine, "Item", new MMBOItemLink());
		ProjectUtilEInv.setMemberValue(mappInfoLine, "Units", new BusinessObjects<MMBOUomDefinition>());
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MapType", m_MapType);
		if(m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED)
		{
			ProjectUtilEInv.setMemberValue(mappInfoLine, "UblField", ProjectGlobalsEInv.ITEM_MAPPING_UBL_FIELD_CODE);
			ProjectUtilEInv.setMemberValue(mappInfoLine, "MField", ProjectGlobalsEInv.ITEM_MAPPING_SEARCH_FIELD_CODE);
			ProjectUtilEInv.setMemberValue(mappInfoLine, "ItemCode", "");
			ProjectUtilEInv.setMemberValue(mappInfoLine, "ItemDesc", "");
		}
		else
		{
			ProjectUtilEInv.setMemberValue(mappInfoLine, "MapSendedType", 1); //Fatura
			ProjectUtilEInv.setMemberValue(mappInfoLine, "UblField", 1);
			ProjectUtilEInv.setMemberValue(mappInfoLine, "MField", 1);
			ProjectUtilEInv.setMemberValue(mappInfoLine, "SchemaID", 1);
			ProjectUtilEInv.setMemberValue(mappInfoLine, "DocumentType", "");
		}
		return mappInfoLine;
	}
	
	public boolean processNewArp(ILbsXUIPane container, Object data, IClientContext context)
	{
		CustomBusinessObject mappingInfo = (CustomBusinessObject) data;
		UNBOARPReference arpInfo = (UNBOARPReference) ProjectUtilEInv.getMemberValue(mappingInfo, "ArpInfo");
		if(arpInfo != null)
		{
			if(arpInfo.getIsPersComp() == 1)
				ProjectUtilEInv.setMemberValue(mappingInfo,"Sender", arpInfo.getIDTCNo());
			else
				ProjectUtilEInv.setMemberValue(mappingInfo,"Sender", arpInfo.getTax_Id());
		}
		return true;
	}

	public void onModifyActionParameter(JLbsXUIControlEvent event)
	{
		/** onModifyActionParameter : This method is called before control action execution and lets the user to change action parameter that is used in action execution. Event parameter object (JLbsXUIControlEvent) contains form object in 'container' and 'component' properties, form data object or grid row object or data for action's owner component in 'data' property, action's parameter value in 'stringData' property, and action's context value in 'index' and 'tag' properties. A string (action's parameter value) return value is expected. If no return value is specified or the return value is not of type String, default value is the action's original parameter value. */
		String param = event.getStringData();
		if (param != null &&param.compareTo("4002") == 0)
		{
			CustomBusinessObject mappLine = (CustomBusinessObject) event.getData();
			int mCardType = ((Integer)ProjectUtilEInv.getMemberValue(mappLine, "MCardType")).intValue();
			switch (mCardType)
			{
				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_ITEM:
				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_PROMOTION:
				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_DEPOSIT:
				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_SETITEM:
					param += "-0";
					break;
	
				case  ProjectGlobalsEInv.MAPPING_CARD_TYPE_ITEMCLASS:
					param += "-1";
					break;

				 case ProjectGlobalsEInv.MAPPING_CARD_TYPE_DISCOUNT_PURCH:
						param += "-2-1";
				 		break;
				 case ProjectGlobalsEInv.MAPPING_CARD_TYPE_DISCOUNT_SALES:
				 	param += "-2-2";
				 	break;
			
				case  ProjectGlobalsEInv.MAPPING_CARD_TYPE_EXPENSE_PURCH:
					param += "-2-3";
					break;
				case  ProjectGlobalsEInv.MAPPING_CARD_TYPE_EXPENSE_SALES:
					param += "-2-4";
					break;
				

				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_SET:
					param += "-4";
					break;
				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_SERVICE_PURCH:
				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_SERVICE_SALES:
					param += "-6";
					break;
				case ProjectGlobalsEInv.MAPPING_CARD_TYPE_FIXEDASSET:
					param += "-7";
					break;
			}
		}
		event.setReturnObject(param);
	}
	
	private String getAssetCategory(int transType)
	{
		String category = "1";
		switch (transType)
		{
		case MMConstants.MMTRN_REALESTATE:
			category = Integer.valueOf(AMConstants.AMASSETTYPE_REALESTATE).toString();
			break;
		case MMConstants.MMTRN_VEHICLE:
			category = Integer.valueOf(AMConstants.AMASSETTYPE_VEHICLE).toString();
			break;
		case MMConstants.MMTRN_EQUIPMENT:
			category = Integer.valueOf(AMConstants.AMASSETTYPE_EQUIPMENT).toString();
			break;
		case MMConstants.MMTRN_OTHER:
			category = Integer.valueOf(AMConstants.AMASSETTYPE_OTHER).toString();
			break;
		}
		return category;
	}

	public void onGridRowInserted(JLbsXUIGridEvent event)
	{
		CustomBusinessObject mappInfoLine = (CustomBusinessObject) event.getData();
		mappInfoLine.setObjectName("CBOMappingInfoLine");
		mappInfoLine.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		setMappInfoLineProperties(mappInfoLine);
	}

	public boolean processItemChange(ILbsXUIPane container, Object data, IClientContext context)
	{
		/*CustomBusinessObject mappingLineInfo = (CustomBusinessObject) data;
		BusinessObjects<MMBOUomDefinition>uomDefList = ProjectUtilEInv.searchBOListByCond(m_Context,
				MMBOUomDefinition.class, "($this.UOMSETREF = '"+ ProjectUtilEInv.getBOIntFieldValue(mappingLineInfo, "MUnitSetRef") + "')", -1);
		fillUnitList(uomDefList.toArray(), mappingLineInfo, 0);
		m_Grid.rowListChanged();*/
		return true;
	}
	
	private void fillUnitList(Object [] unitsOfMeasures, CustomBusinessObject mappingLineInfo, int mUnitRef)
	{
		JLbsStringListEx unitList = new JLbsStringListEx();
		for (int i = 0; i < unitsOfMeasures.length; i++)
		{
			MMBOUomDefinition unit = (MMBOUomDefinition) unitsOfMeasures[i];
			if (mUnitRef == 0 && i == 0) 
			{
				ProjectUtilEInv.setMemberValue(mappingLineInfo, "MUnitRef", unit.getInternal_Reference());
				ProjectUtilEInv.setMemberValue(mappingLineInfo,	"MGlobUnitCode", unit.getGlobalCode());
			}
			else if(mUnitRef == unit.getInternal_Reference())
			{
				ProjectUtilEInv.setMemberValue(mappingLineInfo, "MUnitRef", unit.getInternal_Reference());
				ProjectUtilEInv.setMemberValue(mappingLineInfo,	"MGlobUnitCode", unit.getGlobalCode());
			}
			unitList.add(unit.getCode(), unit.getInternal_Reference(), unit);
		}
		ProjectUtilEInv.setMemberValue(mappingLineInfo, "UnitList", unitList);
	}

	public void onGridCellInitCombo(JLbsXUIGridEvent event)
	{
		/** onGridCellInitCombo : This method is called to get the resource list of a combobox grid cell. If the resource list is not defined in the form definition or the list cannot be found in runtime, this method is called, otherwise the list that is specified in the form definition is used. Event parameter object (JLbsXUIGridEvent) contains form object in 'container' property, grid row data object in 'data' property, grid component in 'grid' property, row number in 'row' property (starts from 0), column number in 'column' property (starts from 0), and column's tag value in 'columnTag' property. A JLbsStringList return value is expected. */
		switch (event.getColumnTag())
		{
			case 3000001:
				CustomBusinessObject line = (CustomBusinessObject) event.getData();
				event.setReturnObject(ProjectUtilEInv.getMemberValue(line, "UnitList"));
				break;
		}
	}

	public void onGridCellDataChanged(JLbsXUIGridEvent event)
	{
		CustomBusinessObject mappInfoLine = (CustomBusinessObject) event.getData();
		if(m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED)
		{
			/** onGridCellDataChanged : This method is called when a cell value is changed. Event parameter object (JLbsXUIGridEvent) contains form object in 'container' property, grid row data object in 'data' property, grid component in 'grid' property, row number in 'row' property (starts from 0), column number in 'column' property (starts from 0), and column's tag value in 'columnTag' property. No return value is expected. */
			if(event.getColumnTag() == 1006) // eþleme tipi
			{
				
				ProjectUtilEInv.setMemberValue(mappInfoLine, "MCardRef", 0);
				ProjectUtilEInv.setMemberValue(mappInfoLine, "MUnitRef", 0);
				ProjectUtilEInv.setMemberValue(mappInfoLine, "MUnitSetRef", 0);
				ProjectUtilEInv.setMemberValue(mappInfoLine, "MGlobUnitCode", "");
				ProjectUtilEInv.setMemberValue(mappInfoLine, "Item", new MMBOItemLink());
				ProjectUtilEInv.setMemberValue(mappInfoLine, "Units", new BusinessObjects<MMBOUomDefinition>());
				m_Grid.rowListChanged();
			}
			
			if(event.getColumnTag() == 3000001) // eþleme birimi
			{
				String globalCode="";
				int mUnitRef = ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MUnitRef");
				JLbsStringListEx unitList = (JLbsStringListEx) ProjectUtilEInv.getMemberValue(mappInfoLine, "UnitList");
				if (mUnitRef > 0 && unitList != null
						&& unitList.getObjectAtTag(mUnitRef) != null)
				{
					MMBOUomDefinition unit = (MMBOUomDefinition) unitList.getObjectAtTag(mUnitRef);
					globalCode = unit.getGlobalCode();
				}
				ProjectUtilEInv.setMemberValue(mappInfoLine, "MGlobUnitCode", globalCode);
				m_Grid.rowListChanged();
			}
		}
		else if(m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_SENDED)
		{
			if(event.getColumnTag() == 3000038) // alýcý eþleme alaný
			{
				if (ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "UblField") >= 1
						&& ProjectUtilEInv.getBOIntFieldValue(mappInfoLine,	"UblField") < 5) 
				{
					
					ProjectUtilEInv.setMemberValue(mappInfoLine, "DocumentType", "");
				}
				else if (ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "UblField") == 5)
				{
					ProjectUtilEInv.setMemberValue(mappInfoLine, "SchemaID", 0);
				}
					
				m_Grid.rowListChanged();
			}
		}
			
	}

	public void onGridCanEditRow(JLbsXUIGridEvent event)
	{
		if (m_Default)
		{
			event.setReturnObject(false);
			return;
		}
		if(m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED)
		{
			switch (event.getColumnTag()) 
			{
				case 1001: // durum
				case 1004: // kod
				case 1005: // açýklama
				case 3000007: // birim
					event.setReturnObject(!m_FromPostBox);
					break;
		
				default:
					break;
			}	
			
		}
		
		else if(m_MapType == ProjectGlobalsEInv.MAPPING_TYPE_SENDED)
		{
			CustomBusinessObject line =  (CustomBusinessObject) event.getData();
			switch (event.getColumnTag()) 
			{
				case 3000041: // schemaID
					if(ProjectUtilEInv.getBOIntFieldValue(line, "UblField") >= 1 && ProjectUtilEInv.getBOIntFieldValue(line, "UblField") < 5)
						event.setReturnObject(true);
					else
						event.setReturnObject(false);
					break;
						
				case 3000042: // documentType
					if(ProjectUtilEInv.getBOIntFieldValue(line, "UblField") == 5) // additiniol document reference
						event.setReturnObject(true);
					else
						event.setReturnObject(false);
					break;
		
				default:
					break;
			}	
			
		}

		
	}

	public void onGridGetColumnPermanentState(JLbsXUIControlEvent event)
	{
		/*if(tag == 1001) // durum
			event.setReturnObject(JLbsXUITypes.XUISTATE_RESTRICTED);*/
	}

	public void onGridCanInsertRow(JLbsXUIGridEvent event)
	{
		event.setReturnObject(!m_Default);
	}

	public void onGridCanDeleteRow(JLbsXUIGridEvent event)
	{
		event.setReturnObject(!m_Default);
	}

	public void onGridComboFilter(JLbsXUIGridEvent event)
	{
		/** onGridComboFilter : This method is called to determine which items of combobox controls in grid cells will be filtered. Event parameter object (JLbsXUIGridEvent) contains form object in 'container' property, grid row data object in 'data' property, grid component in 'grid' property, row number in 'row' property (starts from 0), column number in 'column' property (starts from 0), column's tag value in 'columnTag' property, the combobox control whose items are being filtered in 'editor' property, combobox item's index (starts from 0) in 'index' property, and combobox item's tag value in 'tag' property. A boolean ('true' if the combobox item will be visible) return value is expected. If no return value is specified or the return value is not of type boolean, default value is 'true'. */
/*		if(event.getColumnTag() == 1006)
			switch (event.getTag())
			{
				case MMConstants.MMTRN_ITEM:
				case MMConstants.MMTRN_SERVICE:
				case MMConstants.MMTRN_FIXEDASSET:
					event.setReturnObject(true);
					break;
				default:
					event.setReturnObject(false);
					
			}*/
	}
	
	@Override
	public void comboItemSelected(ILbsComboBox combo, ILbsXUIPane container,
			Object data, IClientContext context, int iTag, boolean flag) {
		// TODO Auto-generated method stub
		super.comboItemSelected(combo, container, data, context, iTag, flag);
	}
	
	public boolean selectSchema(ILbsXUIPane container, Object data, IClientContext context)
	{
		CustomBusinessObject mappingInfo = (CustomBusinessObject) data;
		String path = ProjectUtilEInv.getExportFilePath(container.getMessage(500008, 3), container.getMessage(500008, 4), "xslt");
		if (path != null)
		{
			try
			{
				byte[] docSchema = null;
				String lwPth = path.toLowerCase();
				int txtP = lwPth.indexOf(".xslt");
				String filePath = (txtP > 0) ? path : path + ".xslt";
				FileOutputStream file = new FileOutputStream(filePath);
				file.write(docSchema);
				file.close();
				ProjectUtilEInv.setMemberValue(mappingInfo, "docSchema", docSchema);
				container.setComponentValueByTag(3000023, filePath);
			}
			catch (Exception e)
			{
				context.getLogger().error("Exception :", e);
			}

		}
		return true;
	}
	

}
