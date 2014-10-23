package com.acme.customization.forms;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.contract.ContractParameter;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryObjectIdentifier;
import com.lbs.grid.interfaces.ILbsQueryGrid;
import com.lbs.par.gen.un.LEInvoicesDisplay;
import com.lbs.platform.interfaces.IApplicationContext;
import com.lbs.remoteclient.IClientContext;
import com.lbs.unity.UnityConstants;
import com.lbs.unity.UnityHelper;
import com.lbs.unity.dialogs.IUODMessageConstants;
import com.lbs.unity.lo.LOLogoConnHelper;
import com.lbs.util.QueryUtil;
import com.lbs.xui.ILbsXUIPane;
import com.lbs.xui.contract.AppletContractService;
import com.lbs.xui.customization.JLbsXUIControlEvent;

public class CXEInvoicesBrowser {

	public static int EINVOICE_USER_TYPE_COMPANY = 1;
	public static int EINVOICE_USER_TYPE_ORGUNIT = 2;
	
	private ILbsQueryGrid m_TransGrid;
	private static IApplicationContext m_Context = null;
	

	
	public void onPopupMenuAction(JLbsXUIControlEvent event)
	{
		/** onPopupMenuAction : This method is called when user selects any item in the form's popup menu. Event parameter object (JLbsXUIControlEvent) contains form object in 'container' and 'component' properties, form data object in 'data' property, selected popup item's id value in 'index' and 'tag' properties, and selected popup item object (JLbsPopupMenuItem) in 'ctxData' property. This method is expected to execute the action corresponding to the selected popup menu item. No return value is expected. */
		if (event.getIndex() == 100) 
		{
			sendEInvoiceToPostBox(event.getContainer(), event.getData(), event.getClientContext());
		}
	}
	
	
	
	public void sendEInvoiceToPostBox(ILbsXUIPane container, Object data, IClientContext context)
	{
		if (m_TransGrid != null && m_TransGrid.getMultiSelectionList() != null && m_TransGrid.getMultiSelectionList().size() > 0)
		{
			ArrayList invoiceRefList = new ArrayList();
			for (int i = 0; i < m_TransGrid.getMultiSelectionList().size(); i++)
			{
				QueryObjectIdentifier qId = (QueryObjectIdentifier) m_TransGrid.getMultiSelectionList().get(i);				
				QueryBusinessObject qbo = (QueryBusinessObject) qId.getAssociatedData();
				if (QueryUtil.getIntProp(qbo, "Status") != UnityConstants.INVSTAT_APPROVED
						//|| QueryUtil.getIntProp(qbo, "EInvoice") != 1
						|| QueryUtil.getIntProp(qbo, "EInvoiceStatus") != ProjectGlobalsEInv.EINV_STATUS_GIBE_GONDERILECEK
						||  !checkInvoiceType(QueryUtil.getIntProp(qbo, "InvoiceType")))
					continue;
				
				invoiceRefList.add(QueryUtil.getIntegerProp(qbo, "Reference"));
			}
			if (invoiceRefList.size() > 0)
			{
				try
				{
					context.callRemoteMethod("EInvoiceProcs", "sendEInvoices", new Object[]{null, invoiceRefList});
					JOptionPane.showMessageDialog(null, container.getMessage(500003, 1));
				}
				catch (Exception e)
				{
					context.getLogger().error("Remote method call sendEInvoices() caused an exception", e);
				}

			}
				
		}
		
	}
	
	private boolean checkInvoiceType(int invoiceType)
	{
		switch (invoiceType) 
		{
			case UnityConstants.INVC_WHOLESALE:
			case UnityConstants.INVC_SERVICESSOLD:
			case UnityConstants.INVC_SALESASSET:
			case UnityConstants.INVC_PURCHASERET:
				return true;
		}
		return false;
	}
	
	public void onInitialize(JLbsXUIControlEvent event)
	{
		m_TransGrid = (ILbsQueryGrid) event.getContainer().getComponentByTag(100);
		m_Context = event.getClientContext();
	}
	
	public boolean loEInvoice(ILbsXUIPane container, Object data, IClientContext context)
	{
		ILbsQueryGrid grid = (ILbsQueryGrid) container.getComponentByTag(100);
		if (grid == null)
			return false;

		QueryBusinessObject selectedRecord = (QueryBusinessObject) grid.getSelectedObject();
		if (selectedRecord == null)
			return false;

		boolean okay = true;

		int sourceRef = QueryUtil.getIntProp(selectedRecord, "Reference");
		if (context == null)
			return false;

		if (data == null)
			return false;

		byte[] logoObj = ProjectUtilEInv.findXMLContent(context, sourceRef);
		if (logoObj != null && logoObj.length > 0)
		{
			okay = LOLogoConnHelper.showLogoObjects(logoObj);
			if (okay)
			{
				LEInvoicesDisplay input = new LEInvoicesDisplay();
				String contractID = "LEInvoicesDisplay";

				try
				{
					AppletContractService.executeContract(context, contractID, null, null, new ContractParameter(input, "params"));
					return true;
				}
				catch (Exception e)
				{
					context.getLogger().error("Can not execute EInvoice Display", e);
					return false;
				}
			}

		}
		else
			container.showMessage(IUODMessageConstants.NO_SLIPOBJECTS_RECORD_FOUND, "", null);

		return okay;
	}

}
