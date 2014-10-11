package com.acme.customization.forms;

import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.util.QueryUtil;
import com.lbs.xui.customization.JLbsXUIControlEvent;


public class CXEConnParam {
	
	private int m_ConnParamRef = 0;
	private CustomBusinessObject m_Data = null;

	public void onSaveData(JLbsXUIControlEvent event)
	{
		event.setReturnObject(ProjectUtilEInv.persistCBO(event.getClientContext(), m_Data));
	}

	public void onInitialize(JLbsXUIControlEvent event)
	{
		m_Data = (CustomBusinessObject) event.getData();
		m_ConnParamRef = QueryUtil.getIntProp(ProjectUtilEInv.getConnParamRecord(event.getClientContext()), "LOGICALREF");  
		if (m_ConnParamRef == 0)
		{
			m_Data._setState(CustomBusinessObject.STATE_NEW);
		}
		else
		{
			m_Data._setState(CustomBusinessObject.STATE_MODIFIED);
			readConnParamObject(event);
			event.getContainer().refreshXUIData();
		}
		
	}

	private void readConnParamObject(JLbsXUIControlEvent event)
	{
		CustomBusinessObject connParamBO = ProjectUtilEInv.readObject(event.getClientContext(), "CBOConnParam", m_ConnParamRef);
		if (connParamBO != null)
		{
			ProjectUtilEInv.setMemberValue(m_Data, "LogicalRef", ProjectUtilEInv.getMemberValue(connParamBO, "LogicalRef"));
			ProjectUtilEInv.setMemberValue(m_Data, "LoginUrl", ProjectUtilEInv.getMemberValue(connParamBO, "LoginUrl"));
			ProjectUtilEInv.setMemberValue(m_Data, "AccountUser", ProjectUtilEInv.getMemberValue(connParamBO, "AccountUser"));
			ProjectUtilEInv.setMemberValue(m_Data, "LoginUrl", ProjectUtilEInv.getMemberValue(connParamBO, "LoginUrl"));
			ProjectUtilEInv.setMemberValue(m_Data, "ProxyServer", ProjectUtilEInv.getMemberValue(connParamBO, "ProxyServer"));
			ProjectUtilEInv.setMemberValue(m_Data, "LoginPassword", ProjectUtilEInv.getMemberValue(connParamBO, "LoginPassword"));
		}
		
	}


}
