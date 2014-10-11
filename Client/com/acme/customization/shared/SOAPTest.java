package com.acme.customization.shared;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader; 

public class SOAPTest {
	
	
	public static org.apache.axiom.soap.SOAPEnvelope createEnvelope(String sessionID)
	{
		try{
			SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
	        SOAPEnvelope envelope = fac.getDefaultEnvelope();
	        OMNamespace soapEnv = fac.createOMNamespace("http://schemas.xmlsoap.org/soap/envelope", "soapenv");
	        OMNamespace tem = fac.createOMNamespace("http://tempuri.org/", "tem");
	        OMNamespace efat = fac.createOMNamespace("http://schemas.datacontract.org/2004/07/eFaturaWebService", "efat");
	        envelope.declareNamespace(tem);
	        envelope.declareNamespace(efat);
	        /*
	        OMAttribute attribute1 = fac.createOMAttribute("tem", null, "http://tempuri.org/");
	        OMAttribute attribute2 = fac.createOMAttribute("efat", null, "http://schemas.datacontract.org/2004/07/eFaturaWebService");
	        
	        envelope.addAttribute(attribute1);
	        envelope.addAttribute(attribute2);*/
	        
	        OMElement sendInvoice = fac.createOMElement("tem:sendInvoice", null);
	        OMElement invoice = fac.createOMElement("tem:invoice", null);
	        
	        OMElement binaryDataOM = fac.createOMElement("efat:binaryData", null);
	        OMElement value = fac.createOMElement("efat:Value", null);
	        //value.setText("PD94bWwgdmVyc2lvbj0i");
	        binaryDataOM.addChild(value);
	        invoice.addChild(binaryDataOM);
	        OMElement fileName = fac.createOMElement("efat:fileName", null);
	        fileName.setText("abc.zip");
	        invoice.addChild(fileName);
	        
	        sendInvoice.addChild(invoice);
	        OMElement sessionIDElement = fac.createOMElement("tem:sessionID", null);
	        sessionIDElement.setText(sessionID);
	        sendInvoice.addChild(sessionIDElement);
	        
	        envelope.getBody().addChild(sendInvoice);
	        return envelope;

		}
	catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
}
