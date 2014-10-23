package com.acme.customization;

import java.util.ArrayList;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.appobjects.GOBODeptAlias;
import com.lbs.appobjects.GOBOOrgUnitAlias;
import com.lbs.appobjects.GOConstants;
import com.lbs.appobjects.LbsUserLoginInfo;
import com.lbs.control.interfaces.ILbsComboBox;
import com.lbs.data.objects.BusinessObjects;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.data.objects.CustomBusinessObjects;
import com.lbs.grids.JLbsObjectListGrid;
import com.lbs.remoteclient.IClientContext;
import com.lbs.unity.UnityConstants;
import com.lbs.unity.UnityHelper;
import com.lbs.unity.am.AMConstants;
import com.lbs.unity.bo.UNBOARPReference;
import com.lbs.unity.mm.MMConstants;
import com.lbs.unity.mm.bo.MMBOItemLink;
import com.lbs.unity.mm.bo.MMBOUomDefinition;
import com.lbs.util.JLbsStringListEx;
import com.lbs.xui.ILbsXUIPane;
import com.lbs.xui.JLbsXUIAdapter;
import com.lbs.xui.JLbsXUIPane;
import com.lbs.xui.JLbsXUITypes;
import com.lbs.xui.customization.JLbsXUIControlEvent;
import com.lbs.xui.customization.JLbsXUIGridEvent;

public class CXEMappingInfo extends JLbsXUIAdapter{
	
	private JLbsObjectListGrid m_Grid;
	private JLbsXUIControlEvent m_Event = null;
	private JLbsXUIPane m_Container = null;
	private IClientContext m_Context = null;
	private boolean m_FromPostBox = false; 

	public void onGetFormVariableValue(JLbsXUIControlEvent event)
	{
		LbsUserLoginInfo userLoginInfo = (LbsUserLoginInfo) m_Context.getVariable(GOConstants.ms_UserInfo);
		CustomBusinessObject mappingInfoLines = (CustomBusinessObject) m_Grid.getRowObject(m_Grid.getSelectedRow());
		switch (event.getTag())
		{
		
			case 1: // company
				event.setReturnObject(Integer.valueOf(userLoginInfo.getFirm().getLogicalRef()));
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
		CustomBusinessObject mappingInfo = (CustomBusinessObject) m_Container.getData();
		if(ProjectUtilEInv.getMemberValue(mappingInfo, "FromPostBox") != null)
			m_FromPostBox = true;
		if(m_Container.getMode() == JLbsXUITypes.XUIMODE_DBENTRY)
		{
			setMappInfoProperties(mappingInfo);
			CustomBusinessObjects mappInfoLines = (CustomBusinessObjects)ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines");
			CustomBusinessObject mappInfoLine = ProjectUtilEInv.createNewCBO("CBOMappingInfoLine");
			mappInfoLines.add(setMappInfoLineProperties(mappInfoLine));
		}
		else
		{
			if (ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "LogicalRef") == 0)
				setMappInfoProperties(mappingInfo);
			CustomBusinessObjects mappInfoLines = (CustomBusinessObjects)ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines");
			for(int i=0; i< mappInfoLines.size();i++)
			{
				CustomBusinessObject mappInfoLine = (CustomBusinessObject) mappInfoLines.get(i);
				if (ProjectUtilEInv.getMemberValue(mappInfoLine, "Item") == null)
					ProjectUtilEInv.setMemberValue(mappInfoLine, "Item", new MMBOItemLink());
				if (ProjectUtilEInv.getMemberValue(mappInfoLine, "Units") == null)
					ProjectUtilEInv.setMemberValue(mappInfoLine, "Units", new BusinessObjects<MMBOUomDefinition>());
				BusinessObjects units = (BusinessObjects) ProjectUtilEInv.getMemberValue(mappInfoLine, "Units");
				fillUnitList(units.toArray(), mappInfoLine, ProjectUtilEInv.getBOIntFieldValue(mappInfoLine, "MUnitRef"));
			}
		}
		setGridColumnPermStates(event);
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
				case MMConstants.MMTRN_ITEM:
				case MMConstants.MMTRN_PROMOTION:
				case MMConstants.MMTRN_SUBCONTRACTED:
				case MMConstants.MMTRN_DEPOSIT:
				case MMConstants.MMTRN_OPTITEM:
				case MMConstants.MMTRN_BUNDLE:
					param += "-0";
					break;
	
				case MMConstants.MMTRN_CLASS:
					param += "-1";
					break;

				case MMConstants.MMTRN_DISCOUNT:
					param += "-2-1";
					break;
			case MMConstants.MMTRN_EXPENSE:
				param += "-2-3";
				break;
			case MMConstants.MMTRN_REALESTATE:
			case MMConstants.MMTRN_VEHICLE:
			case MMConstants.MMTRN_EQUIPMENT:
			case MMConstants.MMTRN_OTHER:
					param += "-3";

				param += "-" + getAssetCategory(mCardType);
				break;

			case MMConstants.MMTRN_SET:
				param += "-4";
				break;
			case MMConstants.MMTRN_SERVICE:
				param += "-6";
				break;
			case MMConstants.MMTRN_FIXEDASSET:
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
		CustomBusinessObject mappingLineInfo = (CustomBusinessObject) data;
		BusinessObjects<MMBOUomDefinition>uomDefList = ProjectUtilEInv.searchBOListByCond(m_Context,
				MMBOUomDefinition.class, "($this.UOMSETREF = '"+ ProjectUtilEInv.getBOIntFieldValue(mappingLineInfo, "MUnitSetRef") + "')", -1);
		fillUnitList(uomDefList.toArray(), mappingLineInfo, 0);
		m_Grid.rowListChanged();
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
		/** onGridCellDataChanged : This method is called when a cell value is changed. Event parameter object (JLbsXUIGridEvent) contains form object in 'container' property, grid row data object in 'data' property, grid component in 'grid' property, row number in 'row' property (starts from 0), column number in 'column' property (starts from 0), and column's tag value in 'columnTag' property. No return value is expected. */
		if(event.getColumnTag() == 1006) // eþleme tipi
		{
			CustomBusinessObject mappInfoLine = (CustomBusinessObject) event.getData();
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
			CustomBusinessObject line = (CustomBusinessObject) event.getData();
			int mUnitRef = ProjectUtilEInv.getBOIntFieldValue(line, "MUnitRef");
			JLbsStringListEx unitList = (JLbsStringListEx) ProjectUtilEInv.getMemberValue(line, "UnitList");
			if (mUnitRef > 0 && unitList != null
					&& unitList.getObjectAtTag(mUnitRef) != null)
			{
				MMBOUomDefinition unit = (MMBOUomDefinition) unitList.getObjectAtTag(mUnitRef);
				globalCode = unit.getGlobalCode();
			}
			ProjectUtilEInv.setMemberValue(line, "MGlobUnitCode", globalCode);
			m_Grid.rowListChanged();
		}
			
	}

	public void onGridCanEditRow(JLbsXUIGridEvent event)
	{
		switch (event.getColumnTag()) {
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

	public void onGridGetColumnPermanentState(JLbsXUIControlEvent event)
	{
		/*if(tag == 1001) // durum
			event.setReturnObject(JLbsXUITypes.XUISTATE_RESTRICTED);*/
	}

	public void onGridCanInsertRow(JLbsXUIGridEvent event)
	{
		event.setReturnObject(!m_FromPostBox);
	}

	public void onGridCanDeleteRow(JLbsXUIGridEvent event)
	{
		event.setReturnObject(!m_FromPostBox);
	}

	public void onGridComboFilter(JLbsXUIGridEvent event)
	{
		/** onGridComboFilter : This method is called to determine which items of combobox controls in grid cells will be filtered. Event parameter object (JLbsXUIGridEvent) contains form object in 'container' property, grid row data object in 'data' property, grid component in 'grid' property, row number in 'row' property (starts from 0), column number in 'column' property (starts from 0), column's tag value in 'columnTag' property, the combobox control whose items are being filtered in 'editor' property, combobox item's index (starts from 0) in 'index' property, and combobox item's tag value in 'tag' property. A boolean ('true' if the combobox item will be visible) return value is expected. If no return value is specified or the return value is not of type boolean, default value is 'true'. */
		if(event.getColumnTag() == 1006)
			switch (event.getTag())
			{
				case MMConstants.MMTRN_ITEM:
				case MMConstants.MMTRN_SERVICE:
				case MMConstants.MMTRN_FIXEDASSET:
					event.setReturnObject(true);
					break;
				default:
					event.setReturnObject(false);
					
			}
	}
	
	@Override
	public void comboItemSelected(ILbsComboBox combo, ILbsXUIPane container,
			Object data, IClientContext context, int iTag, boolean flag) {
		// TODO Auto-generated method stub
		super.comboItemSelected(combo, container, data, context, iTag, flag);
	}
	

}
