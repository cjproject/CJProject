package com.acme.customization;

import java.util.ArrayList;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.lbs.data.grids.JLbsQueryGrid;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryParams;
import com.lbs.remoteclient.IClientContext;
import com.lbs.util.QueryUtil;
import com.lbs.xui.JLbsXUIPane;
import com.lbs.xui.customization.JLbsXUIGridEvent;
import com.lbs.xui.events.swing.JLbsCustomXUIEventListener;
import com.lbs.xui.customization.JLbsXUIControlEvent;
import com.lbs.xui.customization.JLbsXUIDataGridEvent;

public class CXEMappingInfoBrowser extends JLbsCustomXUIEventListener {

	private static final String FORM_NAME_MAPPING_INFO_RECEIVED_BROWSER = "CXFMappingInfoReceivedBrowser.lfrm";
	private static final String FORM_NAME_MAPPING_INFO_SENDED_BROWSER = "CXFMappingInfoSendedBrowser.lfrm";
	
	private String m_FormName = "";
	
	private int m_MapType = ProjectGlobalsEInv.MAPPING_TYPE_RECEIVED;
	
	private JLbsXUIControlEvent m_Event = null;
	private JLbsXUIPane m_Container = null;
	private IClientContext m_Context = null;
	
	@Override
	public void onDataGridModifyQuery(JLbsXUIDataGridEvent event) {
		JLbsQueryGrid grid = event.getQueryGrid();
		QueryParams params = grid.getQueryParams();
		params.getParameters().put("P_MAPTYPE", m_MapType);
		params.getEnabledTerms().enable("T_MAPTYPE");
		super.onDataGridModifyQuery(event);
	}
	

	public void onGridGetCellValue(JLbsXUIGridEvent event)
	{
		/** onGridGetCellValue : This method is called to get the cell value of each grid cell. It is called once for each visible grid cell in the form. Grid cell's value is bound to a property most of the times, but there are some situations where the cell's display value is different than its value or the cell's value is a calculated value; in these situations this mehtod supplies the display value of the cell. Event parameter object (JLbsXUIGridEvent) contains form object in 'container' property, grid row data object in 'data' property, grid component in 'grid' property, row number in 'row' property (starts from 0), column number in 'column' property (starts from 0), and column's tag value in 'columnTag' property. An object representing cell value is expected as the return value. */
		if(event.getData() instanceof QueryBusinessObject)
		{
			QueryBusinessObject qbo = (QueryBusinessObject) event.getData();
			if(QueryUtil.getIntProp(qbo, "ISDEFAULT") == 1)
				event.setReturnObject("[Öndeðer]");
		}
		
	}

	@Override
	public void onGetAuthorizationMode(JLbsXUIControlEvent event) {
		if (event.getContainer() != null && event.getContainer().getFormName() != null)
		{
			m_FormName = event.getContainer().getFormName();
			if (m_FormName.contains(FORM_NAME_MAPPING_INFO_SENDED_BROWSER)) 
			{
				m_MapType = ProjectGlobalsEInv.MAPPING_TYPE_SENDED;
			}
		}
		m_Event = event;
		m_Container = event.getContainer();
		m_Context = event.getClientContext();
		event.setReturnObject(m_MapType);
		super.onGetAuthorizationMode(event);
	}

	public void onGridCanDeleteObject(JLbsXUIDataGridEvent event)
	{
		/** onGridCanDeleteObject : This method is called to determine whether a row's business object can be deleted for the query or query tree grids of the form. Event parameter object contains form object in 'container' property, selected row object (QueryBusinessObject) to be deleted in 'data' property, and grid component in 'grid' property. A boolean ('true' if the object can be deleted) return value is expected. If no return value is specified or the return value is not of type boolean, default value is 'true'. */
		QueryBusinessObject qbo =  (QueryBusinessObject) event.getData();
		if(QueryUtil.getIntProp(qbo, "ISDEFAULT") == 1)
			event.setReturnObject(false);
	}


	public void onGridCellSelected(JLbsXUIGridEvent event)
	{
		/** onGridCellSelected : This method is called when a grid cell is selected. Event parameter object (JLbsXUIGridEvent) contains form object in 'container' property, grid component in 'grid' property, row number in 'row' property (starts from 0), column number in 'column' property (starts from 0), and column's tag value in 'columnTag' property. 'data' property is empty! No return value is expected. */
	}
		

}
