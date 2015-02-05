package com.acme.customization.handlers;


import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.data.factory.BasicBusinessLogicHandler;
import com.lbs.data.factory.FactoryParams;
import com.lbs.data.factory.IBusinessLogicFactory;
import com.lbs.data.objects.BasicBusinessObject;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.platform.interfaces.IApplicationContext;

public class CLHApproval extends BasicBusinessLogicHandler {
	
	@Override
	public boolean afterDelete(IBusinessLogicFactory factory, FactoryParams params, BasicBusinessObject parentObj, BasicBusinessObject obj) 
	{
		CustomBusinessObject approval = (CustomBusinessObject)obj;
		if(ProjectUtilEInv.getBOIntFieldValue(approval, "OpType") == ProjectGlobalsEInv.OPTYPE_SEND
				&& ProjectUtilEInv.getBOIntFieldValue(approval, "EnvelopeType") == ProjectGlobalsEInv.ENVELOPE_TYPE_SENDER)
		{
			int invoiceRef = ProjectUtilEInv.getBOIntFieldValue(approval, "DocRef");
			if (invoiceRef != 0)
			{
				Object [] refList = {invoiceRef};
				ProjectUtilEInv.updateEInvoiceStatus((IApplicationContext) factory.getContext(), refList,
						ProjectGlobalsEInv.EINV_STATUS_GIBE_GONDERILECEK);
			}
		}
		return super.afterDelete(factory, params, parentObj, obj);
	}
	

}
