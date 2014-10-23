package com.acme.customization.forms;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.data.objects.BusinessObjects;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.data.objects.CustomBusinessObjects;
import com.lbs.data.query.IQueryFactory;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryParams;
import com.lbs.transport.RemoteMethodResponse;
import com.lbs.unity.UnityConstants;
import com.lbs.unity.lo.bo.LOBOInvoice;
import com.lbs.unity.mm.MMConstants;
import com.lbs.unity.mm.bo.MMBOItemLink;
import com.lbs.unity.mm.bo.MMBOTransaction;
import com.lbs.unity.mm.bo.MMBOUomDefinition;
import com.lbs.unity.mm.bo.MMEOTransMaster;
import com.lbs.util.JLbsFileUtil;
import com.lbs.util.QueryUtil;
import com.lbs.xui.JLbsXUILookupInfo;
import com.lbs.xui.JLbsXUITypes;
import com.lbs.xui.browser.JLbsWebBrowser;
import com.lbs.xui.customization.JLbsXUIControlEvent;



public class CXEEDocument {

	QueryBusinessObject m_QueryBO = null;
	byte[] m_LData = null;
	int m_RecType = 0;
	
	public void onInitialize(JLbsXUIControlEvent event)
	{
		 System.out.println("initiliazed.." + "mode:"+event.getContainer().getMode());
		 if(event.getData() instanceof QueryBusinessObject && event.getData() != null)
		 {
			 m_QueryBO = (QueryBusinessObject) event.getData();
			 m_RecType = QueryUtil.getIntProp(m_QueryBO, "RECTYPE");
			 setButtonPermStates(event);
		 }
			 
		if(m_QueryBO != null && QueryUtil.getByteArrProp(m_QueryBO.getProperties(), "LDATA") != null)
		{
			System.out.println("webbrowser...");
			m_LData = (byte[]) QueryUtil.getByteArrProp(m_QueryBO.getProperties(), "LDATA") ;
			ProjectUtilEInv.showEInvoiceObjects(m_LData);
			JLbsWebBrowser  webBrowser = (JLbsWebBrowser) event.getContainer().getComponentByTag(2000001);
			System.out.println("LdATA"+m_LData);
			// Initial Values
			webBrowser.setBarsVisible(false);
			webBrowser.setJavascriptEnabled(true);
	
			String tmpDir = JLbsFileUtil.getTempDirectory();
			String xmlFileName = tmpDir + "EINVOICE.XML";
			xmlFileName = "file://localhost/" + xmlFileName;
			xmlFileName = xmlFileName.replace("\\", "/");
			webBrowser.navigate(xmlFileName);
		}
	 
	}
	
	private void setButtonPermStates(JLbsXUIControlEvent event)
	{
		boolean isInvoice = false;
		if(m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_INV ||
				m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_RET_INV)
			isInvoice = true;
		
		if(event.getContainer().getMode() == JLbsXUITypes.XUIMODE_VIEWONLY)
		{
			event.getContainer().setPermanentStateByTag(2000007, JLbsXUITypes.XUISTATE_ACTIVE); //close button
		}
		else if(isInvoice)
		{
			if(QueryUtil.getIntProp(m_QueryBO, "PROFILEID") == ProjectGlobalsEInv.PROFILE_ID_COMMERCIAL)
			{
			  event.getContainer().setPermanentStateByTag(2000004, JLbsXUITypes.XUISTATE_ACTIVE); //accept button
			  event.getContainer().setPermanentStateByTag(2000005, JLbsXUITypes.XUISTATE_ACTIVE); //deny button
			  event.getContainer().setPermanentStateByTag(2000006, JLbsXUITypes.XUISTATE_ACTIVE); //cancel button
			}
			else
			{
				 event.getContainer().setPermanentStateByTag(2000006, JLbsXUITypes.XUISTATE_ACTIVE); //cancel button
				 event.getContainer().setPermanentStateByTag(2000008, JLbsXUITypes.XUISTATE_ACTIVE); //save button
			}
		}
		else if(m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_SR || m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_PR)
		{
			 event.getContainer().setPermanentStateByTag(2000006, JLbsXUITypes.XUISTATE_ACTIVE); //cancel button
			 event.getContainer().setPermanentStateByTag(2000008, JLbsXUITypes.XUISTATE_ACTIVE); //save button
		}
			
		
	}
	
	private void saveEInvoice(JLbsXUIControlEvent event) 
	{
		initializeInvoice(event);
	}
	

	public void onClickAccept(JLbsXUIControlEvent event)
	{
		JLbsXUILookupInfo lookupInfo = new JLbsXUILookupInfo();
	 	event.getContainer().openChild("Forms/CXFAcceptReject.lfrm", lookupInfo, true, JLbsXUITypes.XUIMODE_DEFAULT);
		if (lookupInfo.getResult() == JLbsXUILookupInfo.XUILU_OK)
	 	{
			String message = lookupInfo.getParameter("Message") != null ? (String) lookupInfo.getParameter("Message") : "";
			createAcceptRejectResponse(event, message, ProjectGlobalsEInv.PR_ACCEPT);
			if (m_LData != null)
			{
				System.out.println("onclickaceept initializeinvoice..");
				initializeInvoice(event);
			}
	 	}
		event.getContainer().saveDataAndClose();
	}
	
	private void initializeInvoice(JLbsXUIControlEvent event) 
	{
		try
		{
			RemoteMethodResponse response = event.getContainer().getContext().
					callRemoteMethod("EInvoiceProcs", "initializeInvoice", new Object[] { null,
							m_LData, QueryUtil.getIntegerProp(m_QueryBO, "LOGICALREF") });
			 LOBOInvoice invoice = (LOBOInvoice) response.Result;
			if (invoice != null)
			 {
				event.getContainer().openChild(
						"LOXFInvoice%" + invoice.getInvoiceType() + ".jfm",
						invoice, true, JLbsXUITypes.XUIMODE_DBENTRY, -1, null);
				if (invoice.getInternal_Reference() > 0)
				{
					System.out.println("fatura ref:"+invoice.getInternal_Reference());
					Object [] refList = {invoice.getInternal_Reference()};
					ProjectUtilEInv.updateEInvoiceStatus(event.getClientContext(), refList, ProjectGlobalsEInv.EINV_STATUS_KABUL_EDILDI);
					ProjectUtilEInv.updateApprovalStatus(event.getClientContext(), QueryUtil.getIntegerProp(m_QueryBO, "LOGICALREF"),
							ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED);
				}
			 }
		} 
		catch (Exception e)
		{
			event.getContainer()
					.getContext()
					.getLogger()
					.error("Remote method call initializeInvoice() caused an exception",
							e);
		}
		
	}
	

	public void onClickReject(JLbsXUIControlEvent event) 
	{
		JLbsXUILookupInfo lookupInfo = new JLbsXUILookupInfo();
		event.getContainer().openChild("Forms/CXFAcceptReject.lfrm", lookupInfo, true, JLbsXUITypes.XUIMODE_DEFAULT);
		if (lookupInfo.getResult() == JLbsXUILookupInfo.XUILU_OK)
	 	{
			String message = lookupInfo.getParameter("Message") != null ? (String) lookupInfo.getParameter("Message") : "";
			createAcceptRejectResponse(event, message, ProjectGlobalsEInv.PR_REJECT);
	 	}
		event.getContainer().saveDataAndClose();
		
	}
	
	private void createAcceptRejectResponse(JLbsXUIControlEvent event, String message, int responseType)
	{
		try
		{
			event.getContainer().getContext().callRemoteMethod("EInvoiceProcs", "createAcceptRejectResponse", new Object[] { null, message,
					QueryUtil.getIntegerProp(m_QueryBO, "LOGICALREF"), Integer.valueOf(responseType) });
		} 
		catch (Exception e)
		{
			event.getContainer()
					.getContext()
					.getLogger()
					.error("Remote method call createAcceptRejectResponse() caused an exception",
							e);
		}
	}

	public void onClickSaveData(JLbsXUIControlEvent event)
	{
		System.out.println("onSaveData..");
		if (m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_INV
				|| m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_RET_INV)
			saveEInvoice(event);
		else if (m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_SR
				|| m_RecType == ProjectGlobalsEInv.RECTYPE_RECEIVED_PR)
			saveSysProdResponse(event);
		event.getContainer().saveDataAndClose();
	}
	
	private void saveSysProdResponse(JLbsXUIControlEvent event)
	{
		try 
		{
			RemoteMethodResponse response = event.getClientContext().callRemoteMethod("EInvoiceProcs", "saveSysProdResponse",
							new Object[] { null, m_QueryBO });
			if (((Boolean) response.Result).booleanValue())
				ProjectUtilEInv.updateApprovalStatus(event.getClientContext(),
						QueryUtil.getIntegerProp(m_QueryBO, "LOGICALREF"),
						ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED);
		} 
		catch (Exception e) 
		{
			event.getContainer().getContext().getLogger().error("Remote method call initializeInvoice() caused an exception", e);
		}
		
	}

}
