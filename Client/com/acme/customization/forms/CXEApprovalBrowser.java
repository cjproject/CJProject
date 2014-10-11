package com.acme.customization.forms;

import java.util.ArrayList;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.data.grids.JLbsQueryGrid;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryParams;
import com.lbs.grids.JLbsObjectListGrid;
import com.lbs.remoteclient.IClientContext;
import com.lbs.util.JLbsFileUtil;
import com.lbs.util.QueryUtil;
import com.lbs.xui.ILbsXUIPane;
import com.lbs.xui.JLbsXUIPane;
import com.lbs.xui.JLbsXUITypes;
import com.lbs.xui.browser.JLbsWebBrowserDesigner;
import com.lbs.xui.customization.JLbsXUIControlEvent;
import com.lbs.xui.customization.JLbsXUIDataGridEvent;
import com.lbs.xui.customization.JLbsXUIGridEvent;
import com.lbs.xui.events.swing.JLbsCustomXUIEventListener;

public class CXEApprovalBrowser extends JLbsCustomXUIEventListener {
	
	private static final String FORM_NAME_SENDED_INV = "CXFSendedInvoicesBrowser.lfrm";
	private static final String FORM_NAME_SENDED_PR = "CXFSendedPRBrowser.lfrm";
	private static final String FORM_NAME_RECEIVED_INV = "CXFReceivedInvoicesBrowser.lfrm";
	private static final String FORM_NAME_RECEIVED_PR = "CXFReceivedPRBrowser.lfrm";
	private static final String FORM_NAME_RECEIVED_SR = "CXFReceivedSRBrowser.lfrm";
	
	public static int m_OpType = ProjectGlobalsEInv.OPTYPE_SEND;
	public static int m_RecType = ProjectGlobalsEInv.RECTYPE_SENDED_INV;
	
	private String m_FormName = "";
	
	private JLbsXUIControlEvent m_Event = null;
	private JLbsXUIPane m_Container = null;
	private IClientContext m_Context = null;
	
	@Override
	public void onGetAuthorizationMode(JLbsXUIControlEvent event) {
		if (event.getContainer() != null && event.getContainer().getFormName() != null)
		{
			m_FormName = event.getContainer().getFormName();
			if (m_FormName.contains(FORM_NAME_SENDED_INV)) 
			{
				m_OpType = ProjectGlobalsEInv.OPTYPE_SEND;
				m_RecType = ProjectGlobalsEInv.RECTYPE_SENDED_INV;
			}
			else if(m_FormName.contains(FORM_NAME_SENDED_PR))
			{
				m_OpType = ProjectGlobalsEInv.OPTYPE_SEND;
				m_RecType = ProjectGlobalsEInv.RECTYPE_SENDED_PR;
			}
			else if(m_FormName.contains(FORM_NAME_RECEIVED_INV))
			{
				m_OpType = ProjectGlobalsEInv.OPTYPE_RECEIVE;
				m_RecType = ProjectGlobalsEInv.RECTYPE_RECEIVED_INV;
			}
			else if(m_FormName.contains(FORM_NAME_RECEIVED_PR))
			{
				m_OpType = ProjectGlobalsEInv.OPTYPE_RECEIVE;
				m_RecType = ProjectGlobalsEInv.RECTYPE_RECEIVED_PR;
			}
			else if(m_FormName.contains(FORM_NAME_RECEIVED_SR))
			{
				m_OpType = ProjectGlobalsEInv.OPTYPE_RECEIVE;
				m_RecType = ProjectGlobalsEInv.RECTYPE_RECEIVED_SR;
			}
		}
		m_Event = event;
		m_Container = event.getContainer();
		m_Context = event.getClientContext();
		super.onGetAuthorizationMode(event);
	}

	@Override
	public void onInitializeXUIContainer(JLbsXUIControlEvent event) 
	{
		super.onInitializeXUIContainer(event);
	}
	
	@Override
	public void onDataGridModifyQuery(JLbsXUIDataGridEvent event) {
		JLbsQueryGrid grid = event.getQueryGrid();
		QueryParams params = grid.getQueryParams();
		ArrayList statusList = new ArrayList();
		if(params.getParameters() != null && params.getParameters().getObject("P_TRANSREF") != null)
		{
			params.getEnabledTerms().enable("T_TRANSREF"); 
		}
		switch (m_RecType) {
			case ProjectGlobalsEInv.RECTYPE_SENDED_INV:
				params.getParameters().put("P_RECTYPE", ProjectGlobalsEInv.RECTYPE_SENDED_INV);
				params.getParameters().put("P_OPTYPE", ProjectGlobalsEInv.OPTYPE_SEND);
				params.getEnabledTerms().enable("NT_ENVTYPE");
				params.getParameters().put("P_ENVTYPE", ProjectGlobalsEInv.ENVELOPE_TYPE_SYSTEM);
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_SENT));
				params.getVariables().put("V_STATUSLIST", statusList.toArray());
				break;
				
			case ProjectGlobalsEInv.RECTYPE_SENDED_PR:
				params.getParameters().put("P_RECTYPE", ProjectGlobalsEInv.RECTYPE_SENDED_PR);
				params.getParameters().put("P_OPTYPE", ProjectGlobalsEInv.OPTYPE_SEND);
				params.getEnabledTerms().enable("NT_ENVTYPE");
				params.getParameters().put("P_ENVTYPE", ProjectGlobalsEInv.ENVELOPE_TYPE_SYSTEM);
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_SENT));
				params.getVariables().put("V_STATUSLIST", statusList.toArray());
				break;
				
			case ProjectGlobalsEInv.RECTYPE_RECEIVED_INV:
				params.getParameters().put("P_RECTYPE", ProjectGlobalsEInv.RECTYPE_RECEIVED_INV);
				params.getParameters().put("P_OPTYPE", ProjectGlobalsEInv.OPTYPE_RECEIVE);
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_ARCHIVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_DENIED));
				params.getVariables().put("V_STATUSLIST", statusList.toArray());
				break;
				
			case ProjectGlobalsEInv.RECTYPE_RECEIVED_PR:
				params.getParameters().put("P_RECTYPE", ProjectGlobalsEInv.RECTYPE_RECEIVED_PR);
				params.getParameters().put("P_OPTYPE", ProjectGlobalsEInv.OPTYPE_RECEIVE);
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_ARCHIVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_DENIED));
				params.getVariables().put("V_STATUSLIST", statusList.toArray());
				break;
	
			case ProjectGlobalsEInv.RECTYPE_RECEIVED_SR:
				params.getParameters().put("P_RECTYPE", ProjectGlobalsEInv.RECTYPE_RECEIVED_SR);
				params.getParameters().put("P_OPTYPE", ProjectGlobalsEInv.OPTYPE_RECEIVE);
				params.getEnabledTerms().enable("T_ENVTYPE");
				params.getParameters().put("P_ENVTYPE", ProjectGlobalsEInv.ENVELOPE_TYPE_SYSTEM);
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_ARCHIVED));
				statusList.add(Integer.valueOf(ProjectGlobalsEInv.STATUS_DENIED));
				params.getVariables().put("V_STATUSLIST", statusList.toArray());
				break;
				
			default:
				break;
		}
		super.onDataGridModifyQuery(event);
	}
	
	@Override
	public void onGridGetCellValue(JLbsXUIGridEvent event) {
		if(event.getData() instanceof QueryBusinessObject)
		{
			QueryBusinessObject qbo = (QueryBusinessObject) event.getData();
				switch (event.getColumnTag()) 
				{
					case 3000022:
						event.setReturnObject(event.getContainer().getMessage(70528, QueryUtil.getIntProp(qbo, "PROFILEID")));
						return ;
					case 3000017:
						if(m_FormName.contains(FORM_NAME_SENDED_PR) || m_FormName.contains(FORM_NAME_RECEIVED_PR))
							event.setReturnObject(event.getContainer().getMessage(500002, QueryUtil.getIntProp(qbo, "CONFIRMRES")));
						else 
							event.setReturnObject(event.getContainer().getMessage(12318, QueryUtil.getIntProp(qbo, "TRCODE")));
						return;
					case 3000016:
						int listID = m_OpType == ProjectGlobalsEInv.OPTYPE_RECEIVE ? 500004 : 500005;
						event.setReturnObject(event.getContainer().getMessage(listID, QueryUtil.getIntProp(qbo, "STATUS")));
						return;
					default:
						break;
				}
		}
		super.onGridGetCellValue(event);
	}

	public void onClickViewEDocument(JLbsXUIControlEvent event)
	{
		viewEDocument(event.getContainer(), event.getContainer().getSelectedGridData(100));
	}
	
	public boolean onClickViewEDocument(ILbsXUIPane container, Object data, IClientContext context)
	{
		viewEDocument(m_Container, m_Container.getSelectedGridData(100));
		return true;
	}
	
	@Override
	public void onCanUpdateBusinessObject(JLbsXUIDataGridEvent event) {
		viewEDocument(event.getContainer(), (QueryBusinessObject) event.getData());
		event.setReturnObject(false);
	}
	
	private void viewEDocument(JLbsXUIPane container, QueryBusinessObject qbo)
	{
		if (qbo == null)
			return;
		try 
		{
			container.openChild("Forms/CXFEDocument.lfrm", qbo, true, JLbsXUITypes.XUIMODE_VIEWONLY);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void onPopupMenuFilterSendedInvoice(JLbsXUIControlEvent event)
	{
		if(event.getIndex() == 2)
		{
			event.setReturnObject(false);
		}
	}

	public void onPopupMenuFilterSendedPR(JLbsXUIControlEvent event)
	{
		if(event.getIndex() == 2)
		{
			event.setReturnObject(false);
		}
	}

}
