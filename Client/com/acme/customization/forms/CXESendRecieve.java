package com.acme.customization.forms;

import java.util.ArrayList;
import java.util.Calendar;

import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.controls.JLbsEditorPane;
import com.lbs.controls.JLbsScrollPane;
import com.lbs.remoteclient.IClientContext;
import com.lbs.transport.RemoteMethodResponse;
import com.lbs.util.QueryUtil;
import com.lbs.xui.JLbsXUIPane;
import com.lbs.xui.RunnableWithReturn;
import com.lbs.xui.customization.JLbsXUIControlEvent;


public class CXESendRecieve {
	
	private JLbsEditorPane m_Pane;

	public void onClickSendRecieve(JLbsXUIControlEvent event)
	{
		sendRecieve(event);

	}

	public void onInitialize(JLbsXUIControlEvent event)
	{
		m_Pane = (JLbsEditorPane)((JLbsScrollPane)event.getContainer().getComponentByTag(2000006)).getInnerComponent();
		String loginUrl = QueryUtil.getStringProp(ProjectUtilEInv.getConnParamRecord(event.getClientContext()), "LOGINURL");  
		event.getContainer().setComponentValueByTag(2000002, loginUrl);
		event.getContainer().resetValueByTag(2000002);
	}
	
	private void sendRecieve(JLbsXUIControlEvent event)
	{
		final IClientContext context = event.getContainer().getContext();
		JLbsXUIPane container = event.getContainer();
		m_Pane.setText(m_Pane.getText()+ "_______________"+ProjectUtilEInv.dateToString(Calendar.getInstance(), "dd.MM.yyyy HH:mm:ss")+"_______________"+"\r\n ");
		m_Pane.setText(m_Pane.getText()+ProjectUtilEInv.dateToString(Calendar.getInstance(), "HH:mm:ss")+" Baðlanýyor..."+"\r\n ");
		Object result = null;
		try
		{
			
			result = container.runWithProgress(-1, -1, "Ýþlem Devam Ediyor...", new RunnableWithReturn()
			{
				public Object run() throws Exception
				{
					RemoteMethodResponse response = context.callRemoteMethod("EInvoiceProcs", "sendRecieve", new Object[]{null});
					return response.Result;
				}
			});
		}
		catch (Exception e)
		{
			context.getLogger().error("Remote method call sendRecieve() caused an exception", e);
		}
		
		if (result != null)
		{
			ArrayList<String> sendRecieveRespList = (ArrayList<String>) result;
			for (int i = 0; i < sendRecieveRespList.size(); i++) 
			{
				String str = (String) sendRecieveRespList.get(i);
				m_Pane.setText(m_Pane.getText() + str + "\r\n ");
			}
		}
		
		m_Pane.setText(m_Pane.getText()+"_______________"+ProjectUtilEInv.dateToString(Calendar.getInstance(), "dd.MM.yyyy HH:mm:ss")+"_______________"+"\r\n"+"\r\n");
	}

	public void onContainerOpened(JLbsXUIControlEvent event)
	{
		sendRecieve(event);
	}


}
