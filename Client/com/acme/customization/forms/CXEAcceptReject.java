package com.acme.customization.forms;

import com.lbs.controls.maskededit.JLbsTextEdit;
import com.lbs.xui.JLbsXUILookupInfo;
import com.lbs.xui.customization.JLbsXUIControlEvent;




public class CXEAcceptReject {

	public void onClickPackage(JLbsXUIControlEvent event)
	{
		JLbsTextEdit message = (JLbsTextEdit) event.getContainer().getComponentByTag(2000001);
		JLbsXUILookupInfo lookupInfo = (JLbsXUILookupInfo) event.getData();
		lookupInfo.setParameter("Message", message.getText());
		lookupInfo.setResult(JLbsXUILookupInfo.XUILU_OK);
		event.getContainer().saveDataAndClose();
	}

	

}
