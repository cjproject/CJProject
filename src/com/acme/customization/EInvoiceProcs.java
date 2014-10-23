package com.acme.customization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.MimeType;
import javax.activation.MimeTypeParameterList;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.functions.Substring;

import org.apache.axiom.util.blob.Blob;
import org.apache.axiom.util.blob.BlobDataSource;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bsh.StringUtil;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.acme.customization.ws.einv.Base64BinaryData;
import com.acme.customization.ws.einv.DocumentType;
import com.acme.customization.ws.einv.GetAppRespStatus;
import com.acme.customization.ws.einv.GetAppRespStatusResponse;
import com.acme.customization.ws.einv.GetApplicationResponse;
import com.acme.customization.ws.einv.GetDocumentStatus;
import com.acme.customization.ws.einv.GetDocumentStatusResponse;
import com.acme.customization.ws.einv.GetInvoiceStatus;
import com.acme.customization.ws.einv.GetInvoiceStatusResponse;
import com.acme.customization.ws.einv.Login;
import com.acme.customization.ws.einv.LoginResponse;
import com.acme.customization.ws.einv.LoginType;
import com.acme.customization.ws.einv.PostBoxServiceStub;
import com.acme.customization.ws.einv.ReceiveDocument;
import com.acme.customization.ws.einv.ReceiveDocumentResponse;
import com.acme.customization.ws.einv.ReceiveDone;
import com.acme.customization.ws.einv.SendApplicationResponse;
import com.acme.customization.ws.einv.SendApplicationResponseResponse;
import com.acme.customization.ws.einv.SendInvoice;
import com.acme.customization.ws.einv.SendInvoiceResponse;
import com.lbs.appobjects.GOBODeptAlias;
import com.lbs.appobjects.GOBOOrgUnit;
import com.lbs.appobjects.GOBOOrgUnitAlias;
import com.lbs.appobjects.GOConstants;
import com.lbs.appobjects.ProcsHelper;
import com.lbs.contract.ContractParameter;
import com.lbs.controls.datedit.JLbsTimeDuration;
import com.lbs.data.objects.BusinessObject;
import com.lbs.data.objects.BusinessObjects;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.data.objects.CustomBusinessObjects;
import com.lbs.data.query.IQueryFactory;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryBusinessObjects;
import com.lbs.data.query.QueryParams;
import com.lbs.globalization.JLbsCurrenciesBase;
import com.lbs.par.gen.unity.lo.InvoiceInput;
import com.lbs.platform.interfaces.IApplicationContext;
import com.lbs.platform.interfaces.IServerContext;
import com.lbs.platform.server.LbsServerContext;
import com.lbs.unity.UnityConstants;
import com.lbs.unity.UnityHelper;
import com.lbs.unity.bo.UNBOARPReference;
import com.lbs.unity.bo.UNBOSlipObject;
import com.lbs.unity.bu.BUHelper;
import com.lbs.unity.fi.bo.FIBOConnectArpInfo;
import com.lbs.unity.gl.GLHelper;
import com.lbs.unity.lo.LOConstants;
import com.lbs.unity.lo.LOHelper;
import com.lbs.unity.lo.bo.LOBOConnectEInvoice;
import com.lbs.unity.lo.bo.LOBOInvoice;
import com.lbs.unity.lo.forms.LOOVInvoice;
import com.lbs.unity.mm.MMConstants;
import com.lbs.unity.mm.MMHelper;
import com.lbs.unity.mm.bo.MMBOAdditionalTaxes;
import com.lbs.unity.mm.bo.MMBOItemLink;
import com.lbs.unity.mm.bo.MMBOTransaction;
import com.lbs.unity.mm.bo.MMBOTransactionBase;
import com.lbs.unity.mm.bo.MMBOUomDefinition;
import com.lbs.unity.mm.bo.MMEOSlipMaster;
import com.lbs.unity.mm.bo.MMEOTransMaster;
import com.lbs.unity.mm.client.MMTransCalculator;
import com.lbs.util.DateUtil;
import com.lbs.util.JLbsDateUtil;
import com.lbs.util.JLbsFileUtil;
import com.lbs.util.QueryUtil;
import com.lbs.xui.JLbsXUITypes;

public class EInvoiceProcs {
	
	public static final String ENV_TYPE_NAME_SYSTEM= "SYSTEMENVELOPE";
	public static final String ENV_TYPE_NAME_POSTBOX = "POSTBOXENVELOPE";
	public static final String ENV_TYPE_NAME_SENDER= "SENDERENVELOPE";
	
	public static final  int ACCOUNTING_TYPE_SUPPLIER = 1;
	public static final int ACCOUNTING_TYPE_CUSTOMER = 2;
	
	public static final int EINVOICE_USER_TYPE_COMPANY = 1;
	public static final int EINVOICE_USER_TYPE_ORGUNIT = 2;
	
	static HashMap m_VatMapList = new HashMap();
	static HashMap m_AddTaxMapList = new HashMap();
	
	static HashMap m_InvoicePackageMap = new HashMap();
	static HashMap m_OrgUnitMap = new HashMap();
	static HashMap m_ArpInfoMap = new HashMap();
	
	static CustomBusinessObjects<CustomBusinessObject> m_InvoicesCBO = null;
	static ArrayList<File> m_InvoicesXML = null;
	static LOBOConnectEInvoice m_Invoice = null;
	static FIBOConnectArpInfo m_ArpInfo = null;
	static GOBOOrgUnit m_OrgUnit = null;
	static IApplicationContext m_Context = null;
	static String m_VATName = "";
	
	static Document m_PackageXML = null;
	static int m_EInvoiceUserType = EINVOICE_USER_TYPE_COMPANY;

	PostBoxServiceStub m_Service;
	String m_SessionID = "";
	
	static int m_ReceivedCnt = 0;
	static ArrayList<String> m_SendRecieveStrList = new ArrayList<String>();
	
	static JLbsCurrenciesBase currenciesBase;
	static  int rcCompanyIndex;
	static MMTransCalculator trCalculator = null;
	
	
	private static NodeList parseXMLAndReturnNodeList(byte [] LData, String elementTagName)
	{
		NodeList nodeList = null;
		try 
		{
			if (LData != null && LData.length > 0) 
			{
				String tmpDir = JLbsFileUtil.getTempDirectory();
				String xmlFileName = tmpDir + "WILLBEPARSED.XML";
				ProjectUtilEInv.writetoFile(LData, xmlFileName);
				File xmlFile = new File(xmlFileName);

				DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
				documentFactory.setNamespaceAware(true);

				DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
				Document doc = documentBuilder.parse(xmlFile);

				doc.getDocumentElement().normalize();
				nodeList = doc.getElementsByTagName(elementTagName);
				
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return nodeList;
	}
	
	
	private static String parseXMLAndReturnNodeValue(byte [] LData, String elementTagName, String nodeTagName)
	{
		String nodeValue = "";
		try
		{
			if (LData != null && LData.length > 0)
			{
				String tmpDir = JLbsFileUtil.getTempDirectory();
				String xmlFileName = tmpDir + "WILLBEPARSED.XML";
				ProjectUtilEInv.writetoFile(LData, xmlFileName);
				File xmlFile = new File(xmlFileName);
	
				DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
				documentFactory.setNamespaceAware(true);
				
				DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();				
				Document doc = documentBuilder.parse(xmlFile);
	
				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName(elementTagName);
	
				for (int temp = 0; temp < nodeList.getLength(); temp++) 
				{
					Node node = nodeList.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().compareTo(nodeTagName) == 0) 
					{
						nodeValue = node.getNodeValue();
					}
				}
			  }
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		return nodeValue;
	}
	
	public static boolean saveSysProdResponse(LbsServerContext context, QueryBusinessObject qBO)
	{
		boolean ok = false;
		QueryBusinessObjects referenceRecords = new QueryBusinessObjects();
		QueryParams params = new QueryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		IQueryFactory factory = (IQueryFactory) context.getQueryFactory();
		try
		{
			params.getEnabledTerms().disableAll();
			params.getEnabledTerms().enable("T_ENVELOPEID");
			params.getParameters().put("P_REFERENCEID", QueryUtil.getStringProp(qBO, "REFERENCEID"));
			ok = factory.select("CQOApprovalBrowser", params, referenceRecords, -1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		if (ok && referenceRecords.size() > 0) {
			for (int i = 0; i < referenceRecords.size(); i++) 
			{
				QueryBusinessObject referenceRecord = referenceRecords.get(i);
				/* burda ilgili dökümaný bulup türüne göre hareket edilmeli..
					1 .eðer faturaya istinaden geldiyse reference id' ye baðlý olan approval' ýn transrefini bul 
					sonra tüm baðlý approvaldaki docrefleri	uptate et
					2. eðer uygulama yanýtý için geldiyse ??
					 * */
				int recType = QueryUtil.getIntProp(referenceRecord, "RECTYPE") ;
				if(recType == ProjectGlobalsEInv.RECTYPE_SENDED_INV || recType == ProjectGlobalsEInv.RECTYPE_RECEIVED_RET_INV)
				{
					ArrayList<Integer> invoiceRefList = new ArrayList<Integer>();
					invoiceRefList.add(QueryUtil.getIntegerProp(referenceRecord, "DOCREF"));
					ProjectUtilEInv.updateEInvoiceStatus(context, invoiceRefList.toArray(), findStatus(QueryUtil.getIntProp(qBO, "RESPCODE")));
				}
				else if(recType == ProjectGlobalsEInv.RECTYPE_SENDED_PR)
				{
					params = new QueryParams();
					params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
					params.getEnabledTerms().enable("T_APPROVALREF");
					params.getParameters().put("P_APPROVALREF", QueryUtil.getIntegerProp(referenceRecord, "LOGICALREF"));
					params.getParameters().put("P_RESPCODE", QueryUtil.getIntProp(qBO, "RESPCODE"));
					params.getParameters().put("P_EXPLAIN_", QueryUtil.getStringProp(qBO, "EXPLAIN_"));
					try 
					{
						factory.executeServiceQuery("CQOUpdateApproval", params);
					} 
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
	
			}
		}
		return ok;
	}
	
	private static int findStatus(int respCode)
	{
		if (respCode == 1000)
			return ProjectGlobalsEInv.EINV_STATUS_SUNUCUYA_GONDERILDI;
		else if (respCode == 1100)
			return ProjectGlobalsEInv.EINV_STATUS_SUNUCUDA_ISLENDI;
		else if (respCode > 1100 && respCode < 1200)
			return ProjectGlobalsEInv.EINV_STATUS_GIBDE_ISLENEMEDI;
		else if (respCode == 1200)
			return ProjectGlobalsEInv.EINV_STATUS_GIBDE_ISLENDI_ALICIYA_ILETILECEK;
		else if (respCode == 1210)
			return ProjectGlobalsEInv.EINV_STATUS_ALICIYA_GONDERILEMEDI;
		else if (respCode == 1230)
			return ProjectGlobalsEInv.EINV_STATUS_ALICIDA_ISLENEMEDI;
		else if (respCode == 1300)
			return ProjectGlobalsEInv.EINV_STATUS_ALICIDA_ISLENDI_BASARIYLA_TAMAMLANDI;
		
		return 0;
	}


	public static LOBOInvoice initializeInvoice(LbsServerContext context, byte [] LData, Integer approvalRef)
	{
		
		return  getNewInvoiceInstance(LData, approvalRef);
	}
	
	private static UNBOARPReference getARPInfo(CustomBusinessObject mappingInfo,  String ID)
	{
		UNBOARPReference arpInfo = null;
		if (mappingInfo != null
				&& ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "ArpRef") > 0)
		{
			arpInfo = (UNBOARPReference)	UnityHelper.getBOByReference(m_Context, UNBOARPReference.class,
					ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "ArpRef"));
		}
		if (arpInfo == null)
		{
			String cond = ID.length() == 10 ? "($this.TAXNR = '" + ID + "')" : "($this.IDTCNO = '" + ID + "')"; 
			BusinessObjects arpList = ProjectUtilEInv.searchBOListByCond(m_Context, UNBOARPReference.class, cond, 0);
			if(arpList != null && arpList.size() > 0)
			{
				arpInfo = (UNBOARPReference) arpList.get(0);
			}
		}
		return arpInfo;
	}
	
	private static GOBOOrgUnitAlias getOrgUnitInfo(CustomBusinessObject mappingInfo, String ID)
	{
		GOBOOrgUnitAlias orgUnitInfo = null;
		if (mappingInfo != null
				&& ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "OrgUnitRef") > 0)
		{
			orgUnitInfo = (GOBOOrgUnitAlias) UnityHelper.getBOByReference(m_Context, GOBOOrgUnitAlias.class,
							ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "OrgUnitRef"));
		}
		if (orgUnitInfo == null)
		{
			BusinessObjects orgUnitList = ProjectUtilEInv.searchBOListByCond(m_Context, GOBOOrgUnitAlias.class, "($this.TAXNR = '" + ID + "')", 0);
			if(orgUnitList != null && orgUnitList.size() > 0)
			{
				orgUnitInfo = (GOBOOrgUnitAlias) orgUnitList.get(0);
			}
		}
		return orgUnitInfo;
	}
	
	private static GOBOOrgUnitAlias getWHInfo(CustomBusinessObject mappingInfo)
	{
		GOBOOrgUnitAlias whInfo = null;
		if (mappingInfo != null
				&& ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "WHRef") > 0)
		{
			whInfo = (GOBOOrgUnitAlias)	UnityHelper.getBOByReference(m_Context, GOBOOrgUnitAlias.class,
							ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "WHRef"));
		}
		return whInfo;
	}
	
	private static GOBODeptAlias getDeptInfo(CustomBusinessObject mappingInfo)
	{
		GOBODeptAlias deptInfo = null;
		if (mappingInfo != null
				&& ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "DeptRef") > 0)
		{
			deptInfo = (GOBODeptAlias) UnityHelper.getBOByReference(m_Context, GOBODeptAlias.class,
							ProjectUtilEInv.getBOIntFieldValue(mappingInfo, "DeptRef"));
		}
		return deptInfo;
	}
	
	private static int getInvoiceType(String typeCode)
	{
		switch (typeCode)
		{
			case "SATIS": 
	    		return UnityConstants.INVC_PURCHASE;
			case "IADE":
				return UnityConstants.INVC_WHOLESALRET;
		}
		return UnityConstants.INVC_PURCHASE;
	}
	
	public static void initializeVariables()
	{
		currenciesBase = UnityHelper.getCurrencies(m_Context);
		rcCompanyIndex = ProcsHelper.getCompanyRCIndex((IServerContext) m_Context);
		trCalculator = new MMTransCalculator((IServerContext) m_Context, null, currenciesBase);
	}
	
	private static LOBOInvoice getNewInvoiceInstance(byte [] LData, Integer approvalRef)
	{
		if(LData == null)
			return null;
		
		CustomBusinessObject approval = ProjectUtilEInv.readObject(m_Context, "CBOApproval", approvalRef.intValue());
		CustomBusinessObject mappingInfo = (CustomBusinessObject) ProjectUtilEInv.getMemberValue(approval, "MappingInfo");
		
		Document invoiceXML = ProjectUtilEInv.getXMLDocument(LData);
		
		LOBOInvoice invoice = new LOBOInvoice();

		invoice.setCategory(LOConstants.LOINVGRPCODE_PURCH);
		invoice.setEInvoice(1);
		invoice.setEInvoiceStatus(ProjectGlobalsEInv.EINV_STATUS_KABUL_EDILDI);
		invoice.setProfileId(getProfileID(getNodeTextByTagName(invoiceXML, "Invoice", "cbc:ProfileID")));
		invoice.setInvoiceType(getInvoiceType(getNodeTextByTagName(invoiceXML, "Invoice", "cbc:InvoiceTypeCode")));
		invoice.setInvoiceDate(ProjectUtilEInv.stringToDate(getNodeTextByTagName(invoiceXML, "Invoice", "cbc:IssueDate"),
				getNodeTextByTagName(invoiceXML, "Invoice", "cbc:IssueTime")));
		
		//set Arp Info
		UNBOARPReference arpInfo = getARPInfo(mappingInfo, getNodeTextByTagName(invoiceXML, "cac:AccountingSupplierParty", "cbc:ID"));
		if (arpInfo != null)
		{
		  invoice.setARPRef(arpInfo.getInternal_Reference());
		  invoice.setARPInfo(arpInfo);
		}
		else
		{
			//TODO HATA MESAJI DONDUR..
		}
		
		//set Org.Unit Info
		GOBOOrgUnitAlias orgUnitInfo = getOrgUnitInfo(mappingInfo, getNodeTextByTagName(invoiceXML, "cac:AccountingCustomerParty", "cbc:ID"));
		if (orgUnitInfo != null)
		{
			invoice.setSourceOU(orgUnitInfo);
			invoice.setDivisionRef(orgUnitInfo.getLogicalRef());
		}
		else
		{
			//TODO HATA MESAJI DONDUR..
		}
		
		//set WH.Info
		GOBOOrgUnitAlias WHInfo = getWHInfo(mappingInfo);
		if (WHInfo != null)
		{
			invoice.setSourceWH(WHInfo);
			invoice.setSourceWHRef(orgUnitInfo.getLogicalRef());
		}
		else
		{
			//TODO HATA MESAJI DONDUR..
		}
		
		//set Dept.Info
		GOBODeptAlias deptInfo = getDeptInfo(mappingInfo);
		if (deptInfo != null)
		{
			invoice.setSourceDP(deptInfo);
			invoice.setDepartmentRef(deptInfo.getLogicalRef());
		}
		else
		{
			//TODO HATA MESAJI DONDUR..
		}

		LOOVInvoice preProc = new LOOVInvoice();
		String contractId = LOHelper.getContract(JLbsXUITypes.XUIMODE_DBENTRY, LOConstants.CONTRACTS_INVOICE, invoice.getInvoiceType());
		InvoiceInput input = new InvoiceInput();
		input.setTypeInfo(invoice.getInvoiceType());
		ContractParameter[] contracts = new ContractParameter[] { new ContractParameter(input, "input") };
		preProc.preProcessData(m_Context, contractId, invoice, contracts, JLbsXUITypes.XUIMODE_DBENTRY);
		
		
		invoice.setPreassgNumber(getNodeTextByTagName(invoiceXML, "Invoice", "cbc:ID"));
		
		invoice.setBOStatus(getInvoiceStatus(getNodeTextByTagName(invoiceXML, "Invoice", "cbc:CopyIndicator")));
		
		invoice.setGUId(getNodeTextByTagName(invoiceXML, "Invoice", "cbc:UUID"));
		
		invoice.setFootnote(getNodeTextByTagName(invoiceXML, "Invoice", "cbc:Note"));
		
		//TODO.. para birimi ile ilgili ne yapýlacak ??
		
		
		BusinessObjects invoiceLines = createInvoiceLines(invoiceXML, invoice, mappingInfo);
		invoice.setTransactions(invoiceLines);
		createGlobTrans(invoice, invoiceXML);
		return invoice;
	}
	
	private static void createGlobTrans(LOBOInvoice invoice, Document invoiceXML) {
		
		LOHelper.addEmptyGlobalTrans(m_Context, invoice.getTransactions(),
				invoice.getInvoiceType(), invoice.getRC_Rate(),
				invoice.getDivisionRef(), invoice.getCategory(), invoice);
		
		int discExpType = -1;
		BigDecimal discExpRate = UnityConstants.bZero;
		BigDecimal discExpAmount = UnityConstants.bZero;
		if (invoiceXML.getElementsByTagName("cac:AllowanceCharge").item(0) != null)
		{
			Element cacAllowanceChargeElement = (Element) invoiceXML.getElementsByTagName("cac:AllowanceCharge").item(0);			
			if(cacAllowanceChargeElement.getElementsByTagName("cbc:ChargeIndicator").item(0) != null)
			{
				Node cbcChargeIndicatorNode  = (Node) cacAllowanceChargeElement.getElementsByTagName("cbc:ChargeIndicator").item(0);
				discExpType = cbcChargeIndicatorNode.getTextContent().compareTo("false") == 0 ? MMConstants.MMTRN_DISCOUNT : MMConstants.MMTRN_EXPENSE;
			}
			
			if(cacAllowanceChargeElement.getElementsByTagName("cbc:MultiplierFactorNumeric").item(0) != null)
			{
				Node cbcMultiplierFactorNumeric  = (Node) cacAllowanceChargeElement.getElementsByTagName("cbc:MultiplierFactorNumeric").item(0);
				discExpRate = new BigDecimal(cbcMultiplierFactorNumeric.getTextContent()).multiply(UnityConstants.bHundred);
			}
			
			if(cacAllowanceChargeElement.getElementsByTagName("cbc:Amount").item(0) != null)
			{
				Node cbcAmount  = (Node) cacAllowanceChargeElement.getElementsByTagName("cbc:Amount").item(0);
				discExpAmount = new BigDecimal(cbcAmount.getTextContent());
			}
			
			if (discExpType != -1
					&& (discExpRate.compareTo(UnityConstants.bZero) > 0 || discExpAmount
							.compareTo(UnityConstants.bZero) > 0))
			{
				MMBOTransaction discExpTrans = invoice.getTransactions().size() > 0 ? invoice.getTransactions().getLast() : null;
				if(discExpTrans != null && discExpTrans.getDetailCategory() == UnityConstants.DETCAT_GLOBAL)
				{
					discExpTrans.setDiscountRate(discExpRate);
					discExpTrans.setTotal(discExpAmount);
					discExpTrans.setTransType(discExpType);
					discExpTrans.setDECalcMethod(discExpRate.compareTo(UnityConstants.bZero) > 0 ? UnityConstants.DECALC_BYRATE
							: UnityConstants.DECALC_BYAMOUNT);
				}
			}
		}
		
	}
	
	private static CustomBusinessObject getMappingInfoLine(CustomBusinessObject mappingInfo, String itemCode)
	{
		ArrayList mappedItemCodeList = new ArrayList();
		CustomBusinessObjects mappInfoLines = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines");
		if (mappInfoLines != null && mappInfoLines.size() > 0)
			for (int i = 0; i < mappInfoLines.size(); i++)
			{
				CustomBusinessObject mappInfoLine = (CustomBusinessObject) mappInfoLines.get(i);
				String code = ProjectUtilEInv.getBOStringFieldValue(mappInfoLine, "Code");
				if(itemCode.compareTo(code) == 0)
					return mappInfoLine;
				
			}
		return null;
	}


	private static BusinessObjects createInvoiceLines(Document invoiceXML, LOBOInvoice invoice, CustomBusinessObject mappingInfo) 
	{
		BusinessObjects<MMBOTransaction> invoiceLines = new BusinessObjects<MMBOTransaction>();
		NodeList nodeList = invoiceXML.getElementsByTagName("cac:InvoiceLine");
		BigDecimal quantity = UnityConstants.bZero;
		BigDecimal price = UnityConstants.bZero;
		BigDecimal taxPercent = UnityConstants.bZero;
		BigDecimal taxAmount= UnityConstants.bZero;
		BigDecimal netTotal = UnityConstants.bZero;
		BigDecimal discExpRate = UnityConstants.bZero;
		BigDecimal discExpAmount = UnityConstants.bZero;
		BigDecimal discExpTotal = UnityConstants.bZero;
		int uomRef = 0;
		int unitSetRef = 0;
		int itemRef = 0;
		int discExpType = -1;
		boolean isReturnInv = false;
		LOBOConnectEInvoice srcInvoice = null;
		
		if (invoice.getInvoiceType() == UnityConstants.INVC_WHOLESALRET)
		{
			String addDocRefID = getNodeTextByTagName(invoiceXML,
					"cac:AdditionalDocumentReference", "cbc:ID");
			BusinessObjects invoiceList = ProjectUtilEInv.searchBOListByCond(
					m_Context, LOBOConnectEInvoice.class, "($this.GUID = '"
							+ addDocRefID + "')", 0);
			if (invoiceList != null && invoiceList.size() > 0)
			{
				srcInvoice = (LOBOConnectEInvoice) invoiceList
						.get(0);
			}
			isReturnInv = true;
		}
		for(int i=0; i < nodeList.getLength(); i++)
		{
			MMBOTransaction trans = getNewInvoiceTransaction(invoice, MMConstants.MMTRN_ITEM, UnityConstants.DETCAT_TRANSACTIONS);
			CustomBusinessObject mappingInfoLine = null;
			if (isReturnInv && srcInvoice != null
					&& srcInvoice.getTransactions().get(i) != null) 
			{
				MMBOTransaction srcTrans = srcInvoice.getTransactions().get(i);
				trans.setSourceTransRef(srcTrans.getInternal_Reference());

			}
			Element element = (Element) nodeList.item(i) ;
			if(element.getElementsByTagName("cac:Item").item(0) != null)
			{
				String name = getNodeTextByTagName(element, "cac:Item", "cbc:Name");
				MMBOItemLink itemLink  = null;
				mappingInfoLine = getMappingInfoLine(mappingInfo, name);
				if (mappingInfoLine != null)
				{
					itemLink = (MMBOItemLink) ProjectUtilEInv.getMemberValue(mappingInfoLine, "Item");
				}
				else
				{
					BusinessObjects itemLinkList = null;
					if (name.length() > 0)
						itemLinkList = ProjectUtilEInv.searchBOListByCond(m_Context,
							MMBOItemLink.class, "($this.CODE = '" + name + "')", 1);
					if(itemLinkList != null && itemLinkList.size() > 0)
					itemLink = (MMBOItemLink) itemLinkList.get(0);
				}
				if (itemLink != null)
				{
					trans.setMaster_Reference(itemLink.getInternal_Reference());
					trans.setitemLink(itemLink);
					MMEOTransMaster transMaster = (MMEOTransMaster) trans.getMasterData();
					if(transMaster == null)
						transMaster = new MMEOTransMaster();
					transMaster.setMasterCode(itemLink.getCode());
					transMaster.setMasterDescription(itemLink.getDescription());
				 }
				
			}
			if(element.getElementsByTagName("cbc:InvoicedQuantity").item(0) != null)
			{
				Node quantityNode = (Node) element.getElementsByTagName("cbc:InvoicedQuantity").item(0);
				//quanity 
				if(quantityNode.getTextContent().length() > 0)
				{
					quantity = new BigDecimal(quantityNode.getTextContent());
					trans.setQuantity(quantity);
				}
				
				//unit
				String globalUnitCode = null;
				if (quantityNode.getAttributes().getNamedItem("unitCode") != null)
					globalUnitCode = quantityNode.getAttributes().getNamedItem("unitCode").getNodeValue();
				
				if (globalUnitCode != null)
				{
					BusinessObjects uomDefList = null;
					MMBOUomDefinition uomDef = null;
					if(mappingInfoLine != null)
					{
						uomDef = (MMBOUomDefinition) ProjectUtilEInv.getMemberValue(mappingInfoLine, "Unit");
					}
					else
					{
						uomDefList = ProjectUtilEInv.searchBOListByCond(m_Context,
							MMBOUomDefinition.class, "($this.GLOBALCODE = '"+ globalUnitCode + "')", 0);
						if (uomDefList != null && uomDefList.size() > 0)
							uomDef = (MMBOUomDefinition) uomDefList.get(0);
					}
					if (uomDef != null) 
					{
						uomRef = uomDef.getInternal_Reference();
						unitSetRef = uomDef.getUnitSetRef();
						trans.setUOMRef(uomRef);
						trans.setUnitSetRef(unitSetRef);
					}
				 }
				
			}
			
			if(element.getElementsByTagName("cac:Price").item(0) != null)
			{
				String priceAmount = getNodeTextByTagName(element, "cac:Price", "cbc:PriceAmount");
				if (priceAmount.length() > 0)
				{
					price = new BigDecimal(priceAmount);
					trans.setPrice(price);
				}
				
			}
			if(element.getElementsByTagName("cbc:LineExtensionAmount").item(0) != null)
			{
				Node lineExtensionAmountNode = (Node) element.getElementsByTagName("cbc:LineExtensionAmount").item(0);
				if(lineExtensionAmountNode.getTextContent().length() > 0)
				{
					netTotal = new BigDecimal(lineExtensionAmountNode.getTextContent());
					trans.setTotal(netTotal);
				}
				
			}
			
			NodeList taxSubTotalNodeList = element.getElementsByTagName("cac:TaxSubtotal");
			if(taxSubTotalNodeList != null)
				for(int j = 0 ; j < taxSubTotalNodeList.getLength(); j++)
				{
						Element taxSubTotalElement = (Element) taxSubTotalNodeList.item(j);
						NodeList cbcTaxTypeCode  = taxSubTotalElement.getElementsByTagName("cbc:TaxTypeCode");
						NodeList cbcPercent  = taxSubTotalElement.getElementsByTagName("cbc:Percent");
						if (cbcTaxTypeCode.item(0) != null)
						{
							if (cbcPercent.item(0) != null && cbcPercent.item(0).getTextContent().length() > 0)
							{
								taxPercent = new BigDecimal(cbcPercent.item(0).getTextContent());
							}
							if (taxSubTotalElement.getElementsByTagName("cbc:TaxAmount").item(0) != null && 
									taxSubTotalElement.getElementsByTagName("cbc:TaxAmount").item(0).getTextContent().length() > 0)
							{
								taxAmount = new BigDecimal(taxSubTotalElement.getElementsByTagName("cbc:TaxAmount").item(0).getTextContent());
							}
							
							if(cbcTaxTypeCode.item(0).getTextContent().compareTo("0015") == 0)
							{
								trans.setVATRate(taxPercent);
								trans.setVATAmount(taxAmount);
							}
							else
							{
								trans.setAddTaxRate(taxPercent);
								trans.setAddTaxAmount(taxAmount);
							}
						}
				}
					
			
			invoiceLines.add(trans);			
			
			if(element.getElementsByTagName("cac:AllowanceCharge").item(0) != null)
			{
				Element cacAllowanceChargeElement = (Element) element.getElementsByTagName("cac:AllowanceCharge").item(0);
				
				if(cacAllowanceChargeElement.getElementsByTagName("cbc:ChargeIndicator").item(0) != null)
				{
					Node cbcChargeIndicatorNode  = (Node) cacAllowanceChargeElement.getElementsByTagName("cbc:ChargeIndicator").item(0);
					discExpType = cbcChargeIndicatorNode.getTextContent().compareTo("false") == 0 ? MMConstants.MMTRN_DISCOUNT : MMConstants.MMTRN_EXPENSE;
				}
				
				if(cacAllowanceChargeElement.getElementsByTagName("cbc:MultiplierFactorNumeric").item(0) != null)
				{
					Node cbcMultiplierFactorNumeric  = (Node) cacAllowanceChargeElement.getElementsByTagName("cbc:MultiplierFactorNumeric").item(0);
					discExpRate = new BigDecimal(cbcMultiplierFactorNumeric.getTextContent()).multiply(UnityConstants.bHundred);
				}
				
				if(cacAllowanceChargeElement.getElementsByTagName("cbc:Amount").item(0) != null)
				{
					Node cbcAmount  = (Node) cacAllowanceChargeElement.getElementsByTagName("cbc:Amount").item(0);
					discExpAmount = new BigDecimal(cbcAmount.getTextContent());
				}
				
				if(cacAllowanceChargeElement.getElementsByTagName("cbc:BaseAmount").item(0) != null)
				{
					Node cbcBaseAmount  = (Node) cacAllowanceChargeElement.getElementsByTagName("cbc:BaseAmount").item(0);
					discExpTotal = new BigDecimal(cbcBaseAmount.getTextContent());
				}
				
				if (discExpType != -1
						&& (discExpRate.compareTo(UnityConstants.bZero) > 0 || discExpAmount
								.compareTo(UnityConstants.bZero) > 0)) {
					MMBOTransaction discExpTrans = getNewInvoiceTransaction(invoice, discExpType, UnityConstants.DETCAT_TRANSACTIONS);
					discExpTrans.setDiscountRate(discExpRate);
					discExpTrans.setTotal(discExpAmount);
					discExpTrans.setDECalcMethod(discExpRate.compareTo(UnityConstants.bZero) > 0 ? UnityConstants.DECALC_BYRATE
							: UnityConstants.DECALC_BYAMOUNT);
					invoiceLines.add(discExpTrans);
				}
			
			}
			
			
		}
		
		return invoiceLines;
	}
	
	private static MMBOTransaction getNewInvoiceTransaction(LOBOInvoice invoice, int transType, int detailCategory)
	{
		MMBOTransaction trans = new MMBOTransaction();
		trans.setTransType(transType);
		trans.setDetailCategory(detailCategory);
		trans.setIOCategory(MMHelper.getMMTransIOCategory(invoice.getInvoiceType(), 0, 0, 0, 0, true));
		trans.setSlipDate(invoice.getInvoiceDate());
		trans.setInvoiceFlag(UnityConstants.DISPATCHSTAT_BILLED);
		
		MMEOSlipMaster slipMaster = (MMEOSlipMaster) invoice.getMasterData();
		boolean setLineCurrLC = false;
		int lineCurrLC = 0;
		int dispatchType = 0;
		if (slipMaster != null)
		{
			setLineCurrLC = slipMaster.getParamSetLineCurrLC();
			lineCurrLC = slipMaster.getLineCurrLC();
			dispatchType = slipMaster.getDispatchType();
		}else
			slipMaster = new MMEOSlipMaster();
		MMHelper.preProcessMMTrans(m_Context, trans, dispatchType, invoice.getSourceWHRef(), invoice
				.getSourceWH().getCode(), 0, "", invoice.getDivisionRef(), invoice.getSourceOU().getCode(), 0, "", 0,
				invoice.getRC_Rate(), setLineCurrLC, lineCurrLC, invoice.getTCOfInvoice(), invoice.getTC_Rate(), 
				invoice.getCategory(), invoice);
		
		trans.setFCOfTrans(invoice.getTCOfInvoice());
		trans.setFCRateTrans(invoice.getTC_Rate());
		trans.setRatePC(invoice.getTC_Rate());
		trans.setFCOfPrice(invoice.getTCOfInvoice());
		

		trans.createLinkedObjects();
		invoice.createLinkedObjects();

		trans._setState(BusinessObject.STATE_NEW);
		
		if(trans.getMasterData() == null)
			trans.setMasterData(new MMEOTransMaster());
		
		MMHelper.setItemMasterForChange(m_Context, trans, invoice.getCategory(), invoice.getSourceWHRef(), invoice.getSourceWH()
				.getCode(), 0, "", invoice.getDivisionRef(), invoice.getSourceOU().getCode(), 0, "");		
		MMHelper.setDefaultsForMasterChange(m_Context, currenciesBase, slipMaster.getDispatchType(),
				invoice.getCategory(), invoice.getInvoiceType(), invoice.getInvoiceDate(), new JLbsTimeDuration(),
				rcCompanyIndex, invoice.getRC_Rate(), 1, invoice.getPayPlanRef(), invoice.getShipmentType(), trans,
				invoice.getTradingGroup(), invoice.getARPInfo(), true, invoice.getPrListRef(), invoice.getLineExchangeType());
		
		return trans;
	}
	

	private static int getInvoiceStatus(String copyIndicator) {
		switch (copyIndicator) 
		{
			case "false":
				return UnityConstants.INVSTAT_APPROVED;
			case "true":
				return UnityConstants.INVSTAT_DRAFT;
		}
				
		return UnityConstants.INVSTAT_DRAFT;
	}


	private static int getProfileID(String profileID) {
		
		switch (profileID) 
		{
			case "TEMELFATURA":
				return ProjectGlobalsEInv.PROFILE_ID_BASIC;
			case "TICARIFATURA":
				return ProjectGlobalsEInv.PROFILE_ID_COMMERCIAL;
		}
				
		return ProjectGlobalsEInv.PROFILE_ID_BASIC;
	}


	public static boolean createAcceptRejectResponse(IApplicationContext context, String message, Integer approvalRef, Integer responseType)
	{
		m_Context = context;
		createApplicationResponse(message, approvalRef, responseType);
		return true;
	}
	
	private static void createApplicationResponse(String message, Integer approvalRef, Integer responseType) {
		
		CustomBusinessObjects appRespCBOList = new CustomBusinessObjects<CustomBusinessObject>();
		ArrayList<File> appRespXMLFileList = new ArrayList<File>();
		Document applicationResponse = null;
		String GUID ="";
		CustomBusinessObject approval =  ProjectUtilEInv.readObject(m_Context, "CBOApproval", approvalRef);
		//LOBOInvoice invoice = (LOBOInvoice) UnityHelper.getBOByReference(m_Context, LOBOInvoice.class, 
			//	ProjectUtilEInv.getBOIntFieldValue(approval, "DocRef"), 0);
		Document approvalDocument = null;
		if (approval != null)
		{
			approvalDocument = ProjectUtilEInv.getXMLDocument((byte []) ProjectUtilEInv.getMemberValue(approval, "LData"));
		}
		
		if(approvalDocument!= null)
		{
			try 
			{
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder;
				docBuilder = builderFactory.newDocumentBuilder();
				applicationResponse = docBuilder.newDocument();
				Element root = applicationResponse.createElement("ApplicationResponse");
				root.setAttribute("xmlns", "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2");
				root.setAttribute("xmlns:cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
				root.setAttribute("xmlns:xades", "http://uri.etsi.org/01903/v1.3.2#");
				root.setAttribute("xmlns:cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
				root.setAttribute("xmlns:ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
				root.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
				root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
				root.setAttribute("xsi:schemaLocation", "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2 UBLTR-ApplicationResponse-2.0.xsd");
				applicationResponse.setXmlStandalone(true);
				applicationResponse.appendChild(root);
				
				Element cbcUBLVersionID = applicationResponse.createElement("cbc:UBLVersionID");
				cbcUBLVersionID.appendChild(applicationResponse.createTextNode("2.0"));
				root.appendChild(cbcUBLVersionID);
				
				Element cbcCustomizationID = applicationResponse.createElement("cbc:CustomizationID");
				cbcCustomizationID.appendChild(applicationResponse.createTextNode("TR1.0"));
				root.appendChild(cbcCustomizationID);
				
				Element cbcProfileID = applicationResponse.createElement("cbc:ProfileID");
				cbcProfileID.appendChild(applicationResponse.createTextNode("TICARIFATURA"));
				root.appendChild(cbcProfileID);
				
				Element cbcID = applicationResponse.createElement("cbc:ID");
				LOBOInvoice requestDNInv = new LOBOInvoice();
				requestDNInv.setEInvoice(1);
				requestDNInv.setInvoiceType(UnityConstants.INVC_WHOLESALE);
				String docNr = LOHelper.requestNumberForLOInvoice(m_Context, requestDNInv,  true, false, null);
				cbcID.appendChild(applicationResponse.createTextNode(docNr));
				root.appendChild(cbcID);
				
							
				Element cbcUUID = applicationResponse.createElement("cbc:UUID");
				GUID = ProjectUtilEInv.generateGUID();
				cbcUUID.appendChild(applicationResponse.createTextNode(GUID));
				root.appendChild(cbcUUID);
				
				
				Calendar now = Calendar.getInstance();
				Element cbcIssueDate = applicationResponse.createElement("cbc:IssueDate");
				cbcIssueDate.appendChild(applicationResponse.createTextNode(getIssueDate(now)));
				root.appendChild(cbcIssueDate);
				
				//TODO issuetime tagi eklenebilir..
				
				Element cbcIssueTime = applicationResponse.createElement("cbc:IssueTime");
				cbcIssueTime.appendChild(applicationResponse.createTextNode(getIssueTime((now))));
				root.appendChild(cbcIssueTime);

				// cac:Signature
				Element cacSignature = applicationResponse.createElement("cac:Signature");

				String sender =	 getNodeTextByTagName(approvalDocument,	"cac:AccountingCustomerParty", "cbc:ID");
				Element signatureCbcID = applicationResponse.createElement("cbc:ID");
				signatureCbcID.setAttribute("schemeID", "VKN_TCKN");
				signatureCbcID.appendChild(applicationResponse.createTextNode(sender));
				cacSignature.appendChild(signatureCbcID);
				
				// cac:SignatoryParty
				Element cacSignatoryParty  = applicationResponse.createElement("cac:SignatoryParty");
				Element cacPartyIdentification = applicationResponse.createElement("cac:PartyIdentification");
				
				Element partyID = applicationResponse.createElement("cbc:ID");
				partyID.setAttribute("schemeID", "VKN");
				partyID.appendChild(applicationResponse.createTextNode(sender));
				cacPartyIdentification.appendChild(partyID);
				cacSignatoryParty.appendChild(cacPartyIdentification);
				
				cacSignatoryParty.appendChild(applicationResponse.importNode(getElementByTagName(approvalDocument,
						"cac:AccountingCustomerParty", "cac:PostalAddress"), true));
				cacSignature.appendChild(cacSignatoryParty);
				root.appendChild(cacSignature);
				
				/*
				// cac:DigitalSignatureAttachment
				Element cacDigitalSignatureAttachment  = applicationResponse.createElement("cac:DigitalSignatureAttachment");
				cacSignature.appendChild(cacDigitalSignatureAttachment);
				
				Element cacExternalReference  = applicationResponse.createElement("cac:ExternalReference");
				Element cbcURI  = applicationResponse.createElement("cbc:URI");
				cbcURI.appendChild(applicationResponse.createTextNode("#Signature_c3173268-2a56-48e7-a7c5-e8344cf283df"));
				cacExternalReference.appendChild(cbcURI);
				cacDigitalSignatureAttachment.appendChild(cacExternalReference);*/
				
				//cac:SenderParty
				Element cacSenderParty  = applicationResponse.createElement("cac:SenderParty");
				cacSenderParty.appendChild(cacPartyIdentification.cloneNode(true));
				cacSenderParty.appendChild(applicationResponse.importNode(getElementByTagName(approvalDocument,	
						"cac:AccountingCustomerParty","cac:PartyName"), true));
				cacSenderParty.appendChild(applicationResponse.importNode(getElementByTagName(approvalDocument,
						"cac:AccountingCustomerParty", "cac:PostalAddress"), true));
				root.appendChild(cacSenderParty);
				
				//cac:ReceiverParty
				cacPartyIdentification = applicationResponse.createElement("cac:PartyIdentification");
				partyID = applicationResponse.createElement("cbc:ID");
				partyID.setAttribute("schemeID", "VKN");
				partyID.appendChild(applicationResponse.createTextNode(getNodeTextByTagName(approvalDocument, "cac:AccountingSupplierParty", "cbc:ID")));
				cacPartyIdentification.appendChild(partyID);
				
				Element cacReceiverParty  = applicationResponse.createElement("cac:ReceiverParty");
				cacReceiverParty.appendChild(cacPartyIdentification);
				cacReceiverParty.appendChild(applicationResponse.importNode(getElementByTagName(approvalDocument,
						"cac:AccountingSupplierParty", "cac:PartyName"), true));
				cacReceiverParty.appendChild(applicationResponse.importNode(getElementByTagName(approvalDocument,
						"cac:AccountingSupplierParty", "cac:PostalAddress"), true));
				root.appendChild(cacReceiverParty);
				
				
				//cac:DocumentResponse
				Element cacDocumentResponse = applicationResponse.createElement("cac:DocumentResponse");
				
				//cac:Response
				Element cacResponse  = applicationResponse.createElement("cac:Response");
				
				Element cbcReferenceID  = applicationResponse.createElement("cbc:ReferenceID");
				cbcReferenceID.appendChild(applicationResponse.createTextNode(ProjectUtilEInv.generateGUID()));
				cacResponse.appendChild(cbcReferenceID);
				
				Element cbcResponseCode  = applicationResponse.createElement("cbc:ResponseCode");
				cbcResponseCode.appendChild(applicationResponse.createTextNode(responseType == ProjectGlobalsEInv.PR_ACCEPT ? "KABUL" : "RED"));
				cacResponse.appendChild(cbcResponseCode);
				
				Element cbcDescription  = applicationResponse.createElement("cbc:Description");
				cbcDescription.appendChild(applicationResponse.createTextNode(responseType == ProjectGlobalsEInv.PR_ACCEPT ? "ALINDI" : "RED EDÝLDÝ"));
				cacResponse.appendChild(cbcDescription);
				cacDocumentResponse.appendChild(cacResponse);
				
				//cac:DocumentReference
				Element cacDocumentReference = applicationResponse.createElement("cac:DocumentReference");
				
				Element docRefCbcID  = applicationResponse.createElement("cbc:ID");
				//QUESTION7 DOCREFID GELEN FATURANIN DOCNR SI DOGRU MUDUR ? 
				//guýd alanýdýr..
				docRefCbcID.appendChild(applicationResponse.createTextNode(ProjectUtilEInv.getBOStringFieldValue(approval, "FileName")));
				cacDocumentReference.appendChild(docRefCbcID);
				
				Element docRefIssueDate = applicationResponse.createElement("cbc:IssueDate");
				docRefIssueDate.appendChild(applicationResponse.createTextNode(getIssueDate(now)));
				cacDocumentReference.appendChild(docRefIssueDate);
				
				//QUESTION8 BURAYI NASIL DEGERLENDIRIYORUZ ? kalsýn fatura olarak deðgistir
				Element cbcDocumentTypeCode = applicationResponse.createElement("cbc:DocumentTypeCode");
				cbcDocumentTypeCode.appendChild(applicationResponse.createTextNode("SATIS"));
				cacDocumentReference.appendChild(cbcDocumentTypeCode);
				
				Element cbcDocumentType = applicationResponse.createElement("cbc:DocumentType");
				cbcDocumentType.appendChild(applicationResponse.createTextNode("SATIS"));
				cacDocumentReference.appendChild(cbcDocumentType);
				
				
				//QUESTION9 Burda attachement base64 olarak gönderme þekli doðru mu ? uygulamam yanýtnýn gorseli..
				Element cacAttachment = applicationResponse.createElement("cac:Attachment");
				Element cbcEmbeddedDocumentBinaryObject = applicationResponse.createElement("cbc:EmbeddedDocumentBinaryObject");
				cbcEmbeddedDocumentBinaryObject.setAttribute("characterSetCode", "UTF-8");
				cbcEmbeddedDocumentBinaryObject.setAttribute("encodingCode", "Base64");
				cbcEmbeddedDocumentBinaryObject.setAttribute("filename", GUID+".xslt");
				cbcEmbeddedDocumentBinaryObject.setAttribute("mimeCode", "application/xml");
				cbcEmbeddedDocumentBinaryObject.appendChild(applicationResponse.createTextNode(""));
				cacAttachment.appendChild(cbcEmbeddedDocumentBinaryObject);  
				cacDocumentReference.appendChild(cacAttachment);
				
				cacDocumentResponse.appendChild(cacDocumentReference);
				
				//QUESTION10 Satýr detayýný red ise ekliyorum ama hangi satýra ait cevap verdiðimi nasýl bulabilirim ?
				//Satýr detayýna ait referenceID nedir ? Þuanda red ise lineresponse documentresponse' un aynýsý.. 
				// bunu header response ile ayný yap satýr bazýnda diye bisy yok..
				//cac:LineResponse
				Element cacLineResponse = applicationResponse.createElement("cac:LineResponse");
				Element cacLineReference = applicationResponse.createElement("cac:LineReference");
				Element cbcLineID = applicationResponse.createElement("cbc:LineID");
				cacLineReference.appendChild(cbcLineID);
				cacLineResponse.appendChild(cacLineReference);
				
				//cac:Response
				cacResponse  = applicationResponse.createElement("cac:Response");
				cbcReferenceID  = applicationResponse.createElement("cbc:ReferenceID");
				cbcReferenceID.appendChild(applicationResponse.createTextNode(ProjectUtilEInv.generateGUID()));
				cacResponse.appendChild(cbcReferenceID);
				
				cbcResponseCode  = applicationResponse.createElement("cbc:ResponseCode");
				cbcResponseCode.appendChild(applicationResponse.createTextNode(responseType == ProjectGlobalsEInv.PR_ACCEPT ? "KABUL" : "RED"));
				cacResponse.appendChild(cbcResponseCode);
				
				cbcDescription  = applicationResponse.createElement("cbc:Description");
				cbcDescription.appendChild(applicationResponse.createTextNode(responseType == ProjectGlobalsEInv.PR_ACCEPT ? "ALINDI" : "RED EDÝLDÝ"));
				cacResponse.appendChild(cbcDescription);
				cacLineResponse.appendChild(cacResponse);
				
				cacDocumentResponse.appendChild(cacLineResponse);
				
				root.appendChild(cacDocumentResponse);
				
				File appRespXMLFile = ProjectUtilEInv.createTempFile(GUID, ".xml");
				DOMSource source = new DOMSource(applicationResponse);
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				JLbsFileUtil.write2File(appRespXMLFile.getAbsolutePath(), ProjectUtilEInv.documentToByte(source, transformer));
				appRespXMLFileList.add(appRespXMLFile);
				
				CustomBusinessObject approvalBO = ProjectUtilEInv.createNewCBO("CBOApproval");
				approvalBO._setState(CustomBusinessObject.STATE_NEW);
				
				//m_ArpInfo = (FIBOConnectArpInfo) UnityHelper.getBOByReference(m_Context, FIBOConnectArpInfo.class, invoice.getARPRef());
				//m_OrgUnit = findAndGetOrgUnit(invoice.getDivisionRef());	
				
				ProjectUtilEInv.setMemberValue(approvalBO, "DocNr", docNr);
				//ProjectUtilEInv.setMemberValue(approvalBO, "GenExp", m_ArpInfo.getDescription());
				ProjectUtilEInv.setMemberValue(approvalBO, "Time_",  now.getTimeInMillis());JLbsDateUtil.truncateDate(now);
				ProjectUtilEInv.setMemberValue(approvalBO, "Date_", now);
				ProjectUtilEInv.setMemberValue(approvalBO, "RecType", ProjectGlobalsEInv.RECTYPE_SENDED_PR);
				ProjectUtilEInv.setMemberValue(approvalBO, "EnvelopeType", ProjectGlobalsEInv.ENVELOPE_TYPE_POSTBOX);
				ProjectUtilEInv.setMemberValue(approvalBO, "Status", ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED);
				ProjectUtilEInv.setMemberValue(approvalBO, "Sender", getNodeTextByTagName(approvalDocument, "cac:AccountingSupplierParty", "cbc:ID"));
				ProjectUtilEInv.setMemberValue(approvalBO, "FileName", GUID);
				ProjectUtilEInv.setMemberValue(approvalBO, "OpType", ProjectGlobalsEInv.OPTYPE_OUTGOING);
				ProjectUtilEInv.setMemberValue(approvalBO, "ProfileID", ProjectGlobalsEInv.PROFILE_ID_COMMERCIAL);
				ProjectUtilEInv.setMemberValue(approvalBO, "PKLabel", getNodeTextByTagName(approvalDocument, "sh:Sender", "sh:Identifier"));
				ProjectUtilEInv.setMemberValue(approvalBO, "GBLabel", getNodeTextByTagName(approvalDocument, "sh:Receiver", "sh:Identifier"));
				//ProjectUtilEInv.setMemberValue(approvalBO, "DocDate", invoice.getInvoiceDate());
				//ProjectUtilEInv.setMemberValue(approvalBO, "DocTotal", invoice.getTotalNet());
				ProjectUtilEInv.setMemberValue(approvalBO, "Explain_", message);
				ProjectUtilEInv.setMemberValue(approvalBO, "ConfirmRes", responseType);
				ProjectUtilEInv.setMemberValue(approvalBO, "LData", ProjectUtilEInv.documentToByte(source, transformer));
				//ProjectUtilEInv.persistCBO(m_Context, approvalBO);
				appRespCBOList.add(approvalBO);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			try 
			{
				File zipFile = createZipFile(appRespXMLFileList.toArray(new File[1]));
				byte [] LData = convertFileToByteArray(zipFile);
				createAndPersistTransaction(appRespCBOList, zipFile, LData,
						approvalDocument,
						ProjectGlobalsEInv.TRANSACTION_TYPE_APPRESP,
						ProjectGlobalsEInv.RECTYPE_PACKED_FOR_SENDING_PR);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
	}


	/*QUESTION6
	 * Ticari fatura kaydedildiðinde uygulama yanýtý oluþturur. Burda xml' deki bazý alanlarý faturadan kullanýyorum doðrumu dur ? 
	 * (sender, receiver..) 
	 */
	/*private static void createPostBoxEnvelope(IApplicationContext context, String message, Integer approvalRef, Integer responseType)
	{
		CustomBusinessObject approval =  ProjectUtilEInv.readObject(context, "CBOApproval", approvalRef);
		Document approvalDocument = null;
		if (approval != null)
		{
			approvalDocument = getXMLDocument((byte []) ProjectUtilEInv.getMemberValue(approval, "LData"));
		}
		
		if(approvalDocument!= null)
		{
			try 
			{
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder;
				docBuilder = builderFactory.newDocumentBuilder();
				Document postBoxEnvelope = docBuilder.newDocument();
				Element root = postBoxEnvelope.createElement("sh:StandardBusinessDocument");
				root.setAttribute("xsi:schemaLocation", "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader PackageProxy.xsd");
				root.setAttribute("xmlns:sh", "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader");
				root.setAttribute("xmlns:ef", "http://www.efatura.gov.tr/package-namespace");
				root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
				postBoxEnvelope.appendChild(root);
				postBoxEnvelope.setXmlStandalone(true);
				
				Element documentHeader = postBoxEnvelope.createElement("sh:StandardBusinessDocumentHeader");
				
				//HeaderVersion
				Element headerVersion = postBoxEnvelope.createElement("sh:HeaderVersion");
				headerVersion.appendChild(postBoxEnvelope.createTextNode("1.0"));
				documentHeader.appendChild(headerVersion);
				
				//Sender
				Element sender = postBoxEnvelope.createElement("sh:Sender");
				NodeList nodeList = approvalDocument.getElementsByTagName("Recevier"); 
				for(int i=0;i<nodeList.getLength();i++)
				{
					sender.appendChild(nodeList.item(i));
				}
				documentHeader.appendChild(sender);
				
				//Reciever
				Element receiver = postBoxEnvelope.createElement("sh:Receiver");
				nodeList = approvalDocument.getElementsByTagName("Sender");
				for(int i=0;i<nodeList.getLength();i++)
				{
					sender.appendChild(nodeList.item(i));
				}
				documentHeader.appendChild(receiver);
				
				//Document Identification
				Element docIdent = postBoxEnvelope.createElement("sh:DocumentIdentification");
				
				Element standart = postBoxEnvelope.createElement("sh:Standard");
				docIdent.appendChild(standart);
				
				Element typeVersion = postBoxEnvelope.createElement("sh:TypeVersion");
				typeVersion.appendChild(postBoxEnvelope.createTextNode("1.0"));
				docIdent.appendChild(typeVersion);
				
				Element instanceIdent = postBoxEnvelope.createElement("sh:InstanceIdentifier");
				String transId = ProjectUtilEInv.generateGUID();
				instanceIdent.appendChild(postBoxEnvelope.createTextNode(transId));
				docIdent.appendChild(instanceIdent);
				
				Element type = postBoxEnvelope.createElement("sh:Type");
				type.appendChild(postBoxEnvelope.createTextNode("POSTBOXENVELOPE"));
				docIdent.appendChild(type);
				
				Element creationDateTime = postBoxEnvelope.createElement("sh:CreationDateAndTime");
				creationDateTime.appendChild(postBoxEnvelope.createTextNode(ProjectUtilEInv.convertDateToXSDDateTime()));
				docIdent.appendChild(creationDateTime);
				
				documentHeader.appendChild(docIdent);
				
				root.appendChild(documentHeader);
				
				//Package
				Element efPackage = postBoxEnvelope.createElement("ef:Package");
				
					Element elements = postBoxEnvelope.createElement("Elements");
					
						Element elementType = postBoxEnvelope.createElement("ElementType");
						elementType.appendChild(postBoxEnvelope.createTextNode("APPLICATIONRESPONSE"));
						elements.appendChild(elementType);
						
						Element elementCount = postBoxEnvelope.createElement("ElementCount");
						elementCount.appendChild(postBoxEnvelope.createTextNode("1"));
						elements.appendChild(elementCount);
						Element elementList = postBoxEnvelope.createElement("ElementList");
												
						Element applicationResponse = createApplicationResponse(postBoxEnvelope, approvalDocument, approval, responseType);
								elementList.appendChild(applicationResponse);
							elements.appendChild(elementList);
				
				efPackage.appendChild(elements);	
			root.appendChild(efPackage);
		
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
		*/

	/*private static Element createApplicationResponse(Document postBoxEnvelope, Document approvalDocument, 
			CustomBusinessObject approvalCBO, int respType) 
	{
		
		Element root = null;
		try
		{
			root = postBoxEnvelope.createElement("ApplicationResponse");
			root.setAttribute("xmlns", "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2");
			root.setAttribute("xmlns:cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
			root.setAttribute("xmlns:xades", "http://uri.etsi.org/01903/v1.3.2#");
			root.setAttribute("xmlns:cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
			root.setAttribute("xmlns:ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
			root.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
			root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			root.setAttribute("xsi:schemaLocation", "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2 UBLTR-ApplicationResponse-2.0.xsd");
			
			Element cbcUBLVersionID = postBoxEnvelope.createElement("cbc:UBLVersionID");
			cbcUBLVersionID.appendChild(postBoxEnvelope.createTextNode("2.0"));
			root.appendChild(cbcUBLVersionID);
			
			Element cbcCustomizationID = postBoxEnvelope.createElement("cbc:CustomizationID");
			cbcCustomizationID.appendChild(postBoxEnvelope.createTextNode("TR1.0"));
			root.appendChild(cbcCustomizationID);
			
			Element cbcProfileID = postBoxEnvelope.createElement("cbc:ProfileID");
			cbcProfileID.appendChild(postBoxEnvelope.createTextNode("TICARIFATURA"));
			root.appendChild(cbcProfileID);
			
			Element cbcID = postBoxEnvelope.createElement("cbc:ID");
			cbcID.appendChild(postBoxEnvelope.createTextNode("GIB2014000000001"));
			//TODO Bu numarayý üreten biþey yazman lazým..
			root.appendChild(cbcID);
			
						
			Element cbcUUID = postBoxEnvelope.createElement("cbc:UUID");
			cbcUUID.appendChild(postBoxEnvelope.createTextNode(ProjectUtilEInv.generateGUID()));
			root.appendChild(cbcUUID);
			
			
			Element cbcIssueDate = postBoxEnvelope.createElement("cbc:IssueDate");
			cbcIssueDate.appendChild(postBoxEnvelope.createTextNode(getIssueDate(Calendar.getInstance())));
			root.appendChild(cbcIssueDate);
			
			//TODO issuetime tagi eklenecek..

			// cac:Signature
			Element cacSignature = postBoxEnvelope.createElement("cac:Signature");

			Element signatureCbcID = postBoxEnvelope.createElement("cbc:ID");
			NodeList nodeList = approvalDocument.getElementsByTagName("cac:AccountingCustomerParty");
			signatureCbcID.setAttribute("schemeID", "VKN_TCKN");
			String vkn = getVKNForAppResponse(nodeList);
			signatureCbcID.appendChild(postBoxEnvelope.createTextNode(vkn));
			cacSignature.appendChild(signatureCbcID);
			
			// cac:SignatoryParty
			Element cacSignatoryParty  = postBoxEnvelope.createElement("cac:SignatoryParty");
			Element cacPartyIdentification = postBoxEnvelope.createElement("cac:PartyIdentification");
			
			Element partyID = postBoxEnvelope.createElement("cbc:ID");
			partyID.setAttribute("schemeID", "VKN");
			cacPartyIdentification.appendChild(partyID);
			cacSignatoryParty.appendChild(cacPartyIdentification);
			for(int i=0;i<nodeList.getLength();i++)
			{
				Node node = nodeList.item(i);
				if(node.getNodeName().compareTo("cac:PostalAddress") == 0)
				{
					cacSignature.appendChild(node);
					break;
				}
			}
			cacSignature.appendChild(cacSignatoryParty);
			root.appendChild(cacSignature);
			
			// cac:DigitalSignatureAttachment
			Element cacDigitalSignatureAttachment  = postBoxEnvelope.createElement("cac:DigitalSignatureAttachment");
			cacSignature.appendChild(cacDigitalSignatureAttachment);
			
			Element cacExternalReference  = postBoxEnvelope.createElement("cac:ExternalReference");
			Element cbcURI  = postBoxEnvelope.createElement("cbc:URI");
			cbcURI.appendChild(postBoxEnvelope.createTextNode("#Signature_c3173268-2a56-48e7-a7c5-e8344cf283df"));
			cacExternalReference.appendChild(cbcURI);
			cacDigitalSignatureAttachment.appendChild(cacExternalReference);
			
			//cac:SenderParty
			Element cacSenderParty  = postBoxEnvelope.createElement("cac:SenderParty");
			cacSenderParty.appendChild(cacPartyIdentification);
			for(int i=0;i<nodeList.getLength();i++)
			{
				Node node = nodeList.item(i);
				if(node.getNodeName().compareTo("cac:PartyName") == 0)
				{
					cacSenderParty.appendChild(node);
				}
				if(node.getNodeName().compareTo("cac:PostalAddress") == 0)
				{
					cacSenderParty.appendChild(node);
				}
			}
			root.appendChild(cacSenderParty);
			
			//cac:ReceiverParty
			nodeList = approvalDocument.getElementsByTagName("cac:AccountingSupplierParty");
			Element cacReceiverParty  = postBoxEnvelope.createElement("cac:ReceiverParty");
			cacReceiverParty.appendChild(cacPartyIdentification);
			for(int i=0;i<nodeList.getLength();i++)
			{
				Node node = nodeList.item(i);
				if(node.getNodeName().compareTo("cac:PartyName") == 0)
				{
					cacReceiverParty.appendChild(node);
				}
				if(node.getNodeName().compareTo("cac:PostalAddress") == 0)
				{
					cacReceiverParty.appendChild(node);
				}
			}
			root.appendChild(cacReceiverParty);
			
			//cac:DocumentResponse
			Element cacDocumentResponse = postBoxEnvelope.createElement("cac:DocumentResponse");
			
			//cac:Response
			Element cacResponse  = postBoxEnvelope.createElement("cac:Response");
			
			Element cbcReferenceID  = postBoxEnvelope.createElement("cbcReferenceID");
			cbcReferenceID.appendChild(postBoxEnvelope.createTextNode(ProjectUtilEInv.generateGUID()));
			cacResponse.appendChild(cbcReferenceID);
			
			Element cbcResponseCode  = postBoxEnvelope.createElement("cbcResponseCode");
			cbcResponseCode.appendChild(postBoxEnvelope.createTextNode(respType == ProjectGlobalsEInv.PR_ACCEPT ? "KABUL" : "RED"));
			cacResponse.appendChild(cbcResponseCode);
			
			Element cbcDescription  = postBoxEnvelope.createElement("cbcDescription");
			cbcDescription.appendChild(postBoxEnvelope.createTextNode(respType == ProjectGlobalsEInv.PR_ACCEPT ? "ALINDI" : "RED EDÝLDÝ"));
			cacResponse.appendChild(cbcDescription);
			cacDocumentResponse.appendChild(cacResponse);
			
			//cac:DocumentReference
			Element cacDocumentReference = postBoxEnvelope.createElement("cac:DocumentReference");
			
			Element docRefCbcID  = postBoxEnvelope.createElement("cbc:ID");
			//QUESTION7 DOCREFID GELEN FATURANIN DOCNR SI DOGRU MUDUR ? 
			//guýd alanýdýr..
			docRefCbcID.appendChild(postBoxEnvelope.createTextNode(ProjectUtilEInv.getBOStringFieldValue(approvalCBO, "DocNr")));
			cacDocumentReference.appendChild(docRefCbcID);
			
			Element docRefIssueDate = postBoxEnvelope.createElement("cbc:IssueDate");
			docRefIssueDate.appendChild(postBoxEnvelope.createTextNode(getIssueDate(Calendar.getInstance())));
			cacDocumentReference.appendChild(docRefIssueDate);
			
			//QUESTION8 BURAYI NASIL DEGERLENDIRIYORUZ ? kalsýn fatura olarak deðgistir
			Element cbcDocumentTypeCode = postBoxEnvelope.createElement("cbc:DocumentTypeCode");
			cbcDocumentTypeCode.appendChild(postBoxEnvelope.createTextNode("SATIS"));
			cacDocumentReference.appendChild(cbcDocumentTypeCode);
			
			Element cbcDocumentType = postBoxEnvelope.createElement("cbc:DocumentType");
			cbcDocumentType.appendChild(postBoxEnvelope.createTextNode("SATIS"));
			cacDocumentReference.appendChild(cbcDocumentType);
			
			//QUESTION9 Burda attachement base64 olarak gönderme þekli doðru mu ? uygulamam yanýtnýn gorseli..
			Element cacAttachment = postBoxEnvelope.createElement("cac:Attachment");
			Element cbcEmbeddedDocumentBinaryObject = postBoxEnvelope.createElement("cbc:EmbeddedDocumentBinaryObject");
			cbcEmbeddedDocumentBinaryObject.setAttribute("characterSetCode", "UTF-8");
			cbcEmbeddedDocumentBinaryObject.setAttribute("encodingCode", "Base64");
			cbcEmbeddedDocumentBinaryObject.setAttribute("filename", "c3173268-2a56-48e7-a7c5-e8344cf283df.xslt");
			cbcEmbeddedDocumentBinaryObject.setAttribute("mimeCode", "application/xml");
			Base64BinaryData data = new Base64BinaryData();
			DataHandler handler = new DataHandler(ProjectUtilEInv.getMemberValue(approvalCBO, "LData"), null);
			data.setValue(handler);
			cbcEmbeddedDocumentBinaryObject.appendChild(postBoxEnvelope.createTextNode(data.toString()));
			cacAttachment.appendChild(cbcEmbeddedDocumentBinaryObject);  
			cacDocumentReference.appendChild(cacAttachment);
			
			cacDocumentResponse.appendChild(cacDocumentReference);
			
			//QUESTION10 Satýr detayýný red ise ekliyorum ama hangi satýra ait cevap verdiðimi nasýl bulabilirim ?
			//Satýr detayýna ait referenceID nedir ? Þuanda red ise lineresponse documentresponse' un aynýsý.. 
			// bunu header response ile ayný yap satýr bazýnda diye bisy yok..
			if(respType == ProjectGlobalsEInv.PR_REJECT)
			{
				//cac:LineResponse
				Element cacLineResponse = postBoxEnvelope.createElement("cac:LineResponse");
				Element cacLineReference = postBoxEnvelope.createElement("cac:LineReference");
				Element cacLineID = postBoxEnvelope.createElement("cacLineID");
				cacLineID.appendChild(postBoxEnvelope.createTextNode("1"));
				cacLineReference.appendChild(cacLineID);
				cacLineResponse.appendChild(cacLineReference);
				
				nodeList = postBoxEnvelope.getElementsByTagName("cac:DocumentResponse");
				for(int i=0; i<nodeList.getLength();i++)
				{
					if(nodeList.item(i).getNodeName().compareTo("cac:Response") == 0)
					{
						cacLineResponse.appendChild(nodeList.item(i));
						break;
					}
				}
				cacDocumentReference.appendChild(cacLineResponse);
			}
			
			root.appendChild(cacDocumentResponse);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return root;
	}

*/
	private static String getVKNForAppResponse(NodeList nodeList) 
	{
		for(int i=0;i<nodeList.getLength();i++)
		{
			Node node = nodeList.item(i);
			if(node.getNodeName().compareTo("cbc:ID") == 0)
			{
				return node.getTextContent();
			}
		}
		return "";
	}
	
	private void initializeService()
	{
		if (m_Service == null)
		{
			try
			{
				m_Service = new PostBoxServiceStub();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}


	public ArrayList sendRecieve(IApplicationContext context)
	{
		m_SendRecieveStrList.clear();
		m_Context = context;
		initializeService();
		if(login())
		{
			send();
			recieve();
		}
		return m_SendRecieveStrList;
	}
	
	private boolean login() 
	{
		QueryBusinessObject connParamRec = ProjectUtilEInv.getConnParamRecord(m_Context);
		Login login = new Login();
		LoginType loginType = new LoginType();
		loginType.setUserName(QueryUtil.getStringProp(connParamRec, "ACCOUNTUSER"));
		loginType.setPassWord(QueryUtil.getStringProp(connParamRec, "LOGINPASSWORD"));
		loginType.setAppStr("");
		loginType.setVersion("");
		login.setLogin(loginType);
		try 
		{
			LoginResponse loginResponse = m_Service.login(login);
			m_SessionID = loginResponse.getSessionID();
			return loginResponse.getLoginResult();
		}
		catch (Exception e)
		{
			m_SendRecieveStrList.add(ProjectUtilEInv.dateToString(Calendar.getInstance(), "HH:mm:ss")+ " Baðlantý saðlanamadý... ("
					+e.getLocalizedMessage()+")");
			e.printStackTrace();
			return false;
		}
	}


	private void send()
	{
		/*QUESTION3
		 * gönderilecek paketleri buluyor, burda transaction tablosundaki hangi kayýtlara bakýlmalý ?
		 * query' de transtype = 2 gidiyor bu ne anlama geliyor ? */
		QueryBusinessObjects results = new QueryBusinessObjects();
		int sendedPackSize = 0;
		try 
		{
			QueryParams params = new QueryParams();
			params.getEnabledTableLinks().enable("Approvals");
			params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			params.getEnabledTerms().enable("T1");
			params.getParameters().put("P_STATUS", ProjectGlobalsEInv.TRANSACTION_STATUS_WILL_BE_SEND);
			IQueryFactory factory = (IQueryFactory) m_Context.getQueryFactory();
			factory.select("CQOTransactionBrowser", params, results, -1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		

		int oldTransRef = 0;
		//her bir paketi gönderiyor
		for (int i = 0; i < results.size(); i++)
		{
			QueryBusinessObject qbo = results.get(i);
			int transRef =  QueryUtil.getIntProp(qbo, "LOGICALREF");
			if (oldTransRef != 0 && oldTransRef == transRef)
				continue;
			
			byte[] LData = null;
			try 
			{
			   LData = qbo.getProperties().getByteArray("LDATA");
			}
			catch (Exception e1) 
			{
			   e1.printStackTrace();
			}
			if (LData != null && LData.length > 0)
			{
				
				//paketler ziplenip mi gönderilmeli ?
				
				///evet
				DocumentType docType = new DocumentType();
				docType.setFileName(QueryUtil.getStringProp(qbo, "FILENAME")+".zip");
				Base64BinaryData data = new Base64BinaryData();
				DataHandler handler = new DataHandler(new ByteArrayDataSource(LData, "application/octet-stream"));
				data.setValue(handler);
				docType.setBinaryData(data);
				/*send invoice ya da send application resposnse*/
				boolean ok  = false;
			 	if (QueryUtil.getIntProp(qbo, "TRANS_TYPE") == ProjectGlobalsEInv.TRANSACTION_TYPE_EINV)
			 	{
			 		SendInvoice sendInvoice = new SendInvoice();
			 		sendInvoice.setSessionID(m_SessionID);
					sendInvoice.setInvoice(docType);
					sendInvoice.setAlias(QueryUtil.getStringProp(qbo, "PKLABEL"));
					SendInvoiceResponse result = new SendInvoiceResponse();
					try 
					{
						result = m_Service.sendInvoice(sendInvoice);
					}
					catch (RemoteException e) 
					{
						e.printStackTrace();
					}
				 	ok = result.getSendInvoiceResult();
			 		int status = ok ? ProjectGlobalsEInv.EINV_STATUS_GIBE_GONDERILDI : ProjectGlobalsEInv.EINV_STATUS_GIBE_GONDERILEMEDI; 
			 		ProjectUtilEInv.updateEInvoiceStatus(m_Context, findInvoiceRefs(results, transRef), status);
			 	}
			 	else if (QueryUtil.getIntProp(qbo, "TRANS_TYPE") == ProjectGlobalsEInv.TRANSACTION_TYPE_APPRESP)
			 	{
			 		SendApplicationResponse sendAppResp = new SendApplicationResponse();
			 		sendAppResp.setSessionID(m_SessionID);
			 		sendAppResp.setAlias(QueryUtil.getStringProp(qbo, "PKLABEL"));
			 		sendAppResp.setAppResp(docType);
			 		SendApplicationResponseResponse result = new SendApplicationResponseResponse();
					try 
					{
						result = m_Service.sendApplicationResponse(sendAppResp);
					}
					catch (RemoteException e)
					{
						e.printStackTrace();
					}
			 		ok = result.getSendApplicationResponseResult();
			 	}
			 	if (ok)
			 	{
			 		updateTransactionStatus(transRef, ProjectGlobalsEInv.TRANSACTION_STATUS_SENT);
			 		updateApprovalStatus(transRef, ProjectGlobalsEInv.STATUS_SENT);
			 		sendedPackSize++;
			 	}
			 	//ilgili faturanýn durumunu gönderildi yap.
			 	// gönderildekten sonra transactionlarýn gridde listelenmemesi için ne yapmak lazým ?
					//status degistýr	
			}
				
		}
		m_SendRecieveStrList.add(ProjectUtilEInv.dateToString(Calendar.getInstance(), "HH:mm:ss")+ 
				" Gönderilen paket sayýsý:"+ sendedPackSize);
		m_SendRecieveStrList.add(ProjectUtilEInv.dateToString(Calendar.getInstance(), "HH:mm:ss")+
				" Gönderilemeyen paket sayýsý:"+ (results.size()-sendedPackSize));
	}
	
	private static void updateApprovalStatus(int transRef,	int status)
	{
		QueryParams params = new QueryParams();
		params.getMainTableParams().getEnabledColumns().enable("STATUS");
		params.getEnabledTerms().enable("T_TRANSREF");
		params.getParameters().put("P_STATUS", Integer.valueOf(status));
		params.getParameters().put("P_TRANSREF", Integer.valueOf(transRef));
		
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		IQueryFactory factory = (IQueryFactory) m_Context.getQueryFactory();
		try {
			factory.executeServiceQuery("CQOUpdateApproval", params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	private static void updateTransactionStatus(int transRef, int status) {
		QueryParams params = new QueryParams();
		params.getParameters().put("P_STATUS", Integer.valueOf(status));
		params.getParameters().put("P_TRANSREF", Integer.valueOf(transRef));
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		IQueryFactory factory = (IQueryFactory) m_Context.getQueryFactory();
		try {
			factory.executeServiceQuery("CQOUpdateTransactionStatus", params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	private static Object [] findInvoiceRefs(QueryBusinessObjects results, int currentTransRef) {
		
		ArrayList invoiceRefList = new ArrayList();
		for (int i = 0; i < results.size(); i++)
		{
			QueryBusinessObject qbo = results.get(i);
			int transRef =  QueryUtil.getIntProp(qbo, "LOGICALREF");
			if (currentTransRef == transRef)
			{
				invoiceRefList.add(QueryUtil.getIntegerProp(qbo, "DOCREF"));
			}
		}
		return invoiceRefList.toArray();
	}



	public static File writeStreamContentsToTempFile(InputStream inputStream,  String prefix)
			throws IOException
	{
		byte[] buf = new byte[4096];
		int bytesRead;
		File temp = ProjectUtilEInv.createTempFile(prefix, ".zip");
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp));
		BufferedInputStream in = new BufferedInputStream(inputStream);
		while ((bytesRead = in.read(buf)) > 0)
		{
			out.write(buf, 0, bytesRead);
		}
		out.close();
		in.close();
		return temp;
	}
	
	private static void unZipFileAndPersistApprovals(File file) throws ZipException, IOException
	{
		int BUFFER = 2048;
		ZipFile zip = new ZipFile(file);
		String newPath = file.getParent();
		new File(newPath).mkdir();
		Enumeration zipFileEntries = zip.entries();

		File destFile = null;
		// Process each entry
		while (zipFileEntries.hasMoreElements())
		{
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

			String currentEntry = entry.getName();

			destFile = new File(newPath, currentEntry);
			destFile = new File(newPath, destFile.getName());
			File destinationParent = destFile.getParentFile();

			// create the parent directory structure if needed
			destinationParent.mkdirs();
			if (!entry.isDirectory())
			{
				BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
				int currentByte;
				// establish buffer for writing file
				byte data[] = new byte[BUFFER];

				// write the current file to disk
				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

				// read and write until last byte is encountered
				while ((currentByte = is.read(data, 0, BUFFER)) != -1)
				{
					dest.write(data, 0, currentByte);
				}
				dest.flush();
				dest.close();
				is.close();
			}
			try 
			{
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(destFile);
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("sh:DocumentIdentification");
				for (int temp = 0; temp < nList.getLength(); temp++)
				{
					Node nNode = nList.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE)
					{
						Element eElement = (Element) nNode;
						String type = eElement.getElementsByTagName("sh:Type").item(0).getTextContent();
						if (type != null && type.length() > 0)
						{
							parseRecievedDocument(doc, file.getName().substring(0, file.getName().lastIndexOf("."))								
									, destFile.getName().substring(0, destFile.getName().lastIndexOf("."))	
									, getRecievedEnvelopeType(type), getRecievedRecType(type));
						}
					}
				}
			} 
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static int getRecievedEnvelopeType(String envType)
	{
		if (envType != null && envType.length() > 0)
		{
			switch (envType) {
			case ENV_TYPE_NAME_SYSTEM:
				return ProjectGlobalsEInv.ENVELOPE_TYPE_SYSTEM;
			case ENV_TYPE_NAME_POSTBOX:
				return ProjectGlobalsEInv.ENVELOPE_TYPE_POSTBOX;
			case ENV_TYPE_NAME_SENDER:
				return ProjectGlobalsEInv.ENVELOPE_TYPE_SENDER;
			}
		}
		return -1;
	}
	
	private static int getRecievedRecType(String envType)
	{
		if (envType != null && envType.length() > 0)
		{
			switch (envType) {
			case ENV_TYPE_NAME_SYSTEM:
				return ProjectGlobalsEInv.RECTYPE_RECEIVED_SR;
			case ENV_TYPE_NAME_POSTBOX:
				return ProjectGlobalsEInv.RECTYPE_RECEIVED_PR;
			case ENV_TYPE_NAME_SENDER:
				return ProjectGlobalsEInv.RECTYPE_RECEIVED_INV;
			}
		}
		return -1;
	}
	
	/*QUESTION4
	 * Burda alýnan kayýtlar ziplenerek geldiðini kabul ettim doðru mudur ?
	 * Alýnan dokuman tipleri sistem yanýtý, uygulama yanýtý ve fatura olabilir ?
	 * DocumentIdentification <sh:Type> tag' ine bakarak karar veriyorum ? 
	 */
	/* recievedocument (bu false olana kadar devam) sonra recievedone*/
	private void recieve()
	{
		updateSendedEnvelopeID();
		ReceiveDocument reciveDocument = new ReceiveDocument();
		reciveDocument.setSessionID(m_SessionID);
		ReceiveDocumentResponse recieveDocResp = new ReceiveDocumentResponse();
		try
		{
			recieveDocResp = m_Service.receiveDocument(reciveDocument);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		while(recieveDocResp.getReceiveDocumentResult())
		{
			try 
			{
				m_SendRecieveStrList.add(ProjectUtilEInv.dateToString(Calendar.getInstance(), "HH:mm:ss")+ " Gelen iþlemler alýnýyor...");
				DocumentType docType = recieveDocResp.getDocument();
				Base64BinaryData binaryData = docType.getBinaryData();
				InputStream inputStream = binaryData.getValue().getInputStream();
				File recievedZipFile = writeStreamContentsToTempFile(inputStream,
						docType.getFileName().substring(0, docType.getFileName().lastIndexOf(".")));
				//gelen zipin filename i kullan..
				ReceiveDone recieveDone = new ReceiveDone();
				recieveDone.setSessionID(m_SessionID);
				recieveDone.setFileID(docType.getFileName());
				m_Service.receiveDone(recieveDone);
				unZipFileAndPersistApprovals(recievedZipFile);
				
				reciveDocument = new ReceiveDocument();
				reciveDocument.setSessionID(m_SessionID);
				recieveDocResp = m_Service.receiveDocument(reciveDocument);
			}
			catch (Exception e)
			{
			    e.printStackTrace();
			}
			
		}
	 	
		m_SendRecieveStrList.add(ProjectUtilEInv.dateToString(Calendar.getInstance(), "HH:mm:ss")+ " Alýnan iþlem sayýsý:"+ m_ReceivedCnt);
	}
	
	
	private void updateSendedEnvelopeID()
	{
		boolean ok = false;
		QueryBusinessObjects results = new QueryBusinessObjects();
		try 
		{
			QueryParams params = new QueryParams();
			params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			params.getEnabledTerms().disableAll();
			params.getEnabledTerms().enable("T_ENVIDNULL");
			params.getEnabledTerms().enable("T_OPTYPE");
			params.getParameters().put("P_OPTYPE", ProjectGlobalsEInv.OPTYPE_SEND);
			IQueryFactory factory = (IQueryFactory) m_Context.getQueryFactory();
			ok = factory.select("CQOApprovalBrowser", params, results, -1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if (ok && results.size() > 0)
		{
			//GetDocumentStatus docStatus = new GetDocumentStatus();
			//docStatus.setDocType(-1);
			for (int i = 0; i < results.size(); i++)
			{
				QueryBusinessObject result = results.get(i);
				String Uuid = QueryUtil.getStringProp(result, "FILENAME");
				if(Uuid == null || Uuid.length() == 0)
					continue;
				int recType = QueryUtil.getIntProp(result, "RECTYPE") ;
				String envelopeId = null;
				if(recType == ProjectGlobalsEInv.RECTYPE_SENDED_INV || recType == ProjectGlobalsEInv.RECTYPE_RECEIVED_RET_INV)
				{
					GetInvoiceStatus invStatus = new GetInvoiceStatus();
					invStatus.setSessionID(m_SessionID);
					invStatus.setUuid(Uuid);
					GetInvoiceStatusResponse invStatusResponse = new GetInvoiceStatusResponse();
					try 
					{
						invStatusResponse = m_Service.getInvoiceStatus(invStatus);
					} 
					catch (RemoteException e) 
					{
						e.printStackTrace();
					}
					envelopeId = invStatusResponse.getEnvelopeId();
				}
				else if(recType ==  ProjectGlobalsEInv.RECTYPE_SENDED_PR)
				{
					GetAppRespStatus appRespStatus = new GetAppRespStatus();
					appRespStatus.setSessionID(m_SessionID);
					appRespStatus.setUuid(Uuid);
					GetAppRespStatusResponse appRespStatusResponse = new GetAppRespStatusResponse();
					try
					{
						appRespStatusResponse = m_Service.getAppRespStatus(appRespStatus);
					} 
					catch (RemoteException e)
					{
						e.printStackTrace();
					}
					envelopeId = appRespStatusResponse.getEnvelopeId();
				}
				
				if(envelopeId == null || envelopeId.length() == 0)
						continue;
					QueryParams params = new QueryParams();
					params.getMainTableParams().getEnabledColumns().enable("ENVELOPEID");
					params.getEnabledTerms().enable("T_APPROVALREF");
					params.getParameters().put("P_ENVELOPEID", envelopeId);
					params.getParameters().put("P_APPROVALREF", Integer.valueOf(QueryUtil.getIntProp(result, "LOGICALREF")));
					params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
					IQueryFactory factory = (IQueryFactory) m_Context.getQueryFactory();
					try 
					{
						factory.executeServiceQuery("CQOUpdateApproval", params);
					} 
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}
				
		}
				
	}
	
	/*QUESTION5
	 * Sistem yanýtýný approval' a kaydederken REFERENCEID alanýna yanýt alýnan paketin ID' sini yazýyorum. (Paket satýrý olmadýðý için
	 * bu ID transaction tablosundaki TRANSID' alanýna denk geliyor bu yapýda sýkýntý var mý ?)
	 * Gelen xml' i bytearray' e çeviriyorum ve approval' a status 0 olacak þekilde kaydediyorum doðru mu ?
	  zip olarak kaydet*/
	private static void parseRecievedDocument(Document doc, String zipFileName, String fileName, int envType, int recType) 
	{
		try
		{
			Element elementList = (Element) doc.getElementsByTagName("ElementList").item(0);
			for (int i = 0; i < elementList.getChildNodes().getLength(); i++) {
				Node node = elementList.getChildNodes().item(i);
				if (node.getNodeName().compareTo("ApplicationResponse") == 0 || node.getNodeName().compareTo("Invoice") == 0) 
				{
					persistApproval(doc, (Element) node, zipFileName, fileName, envType, recType);
				}
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void persistApproval(Document doc, Element element, String zipFileName, String fileName, int envType, int recType) throws TransformerConfigurationException
	{
		CustomBusinessObject approvalBO = ProjectUtilEInv.createNewCBO("CBOApproval");
		approvalBO._setState(CustomBusinessObject.STATE_NEW);
		Calendar now = Calendar.getInstance();
		ProjectUtilEInv.setMemberValue(approvalBO, "Date_", ProjectUtilEInv.getToday(now));
		ProjectUtilEInv.setMemberValue(approvalBO, "Time_",  now.getTimeInMillis());
		ProjectUtilEInv.setMemberValue(approvalBO, "RecType", recType);
		ProjectUtilEInv.setMemberValue(approvalBO, "Status", 0);
		ProjectUtilEInv.setMemberValue(approvalBO, "Sender", getNodeTextByTagName(doc, "sh:ContactInformation", "sh:Contact"));
		/*FileName ve EnvelopeID alanlarýna gelen döküman türüne göre xml' de hangi alana bakýlmalý ? */
		ProjectUtilEInv.setMemberValue(approvalBO, "FileName", fileName);
		ProjectUtilEInv.setMemberValue(approvalBO, "EnvelopeID", zipFileName);
		ProjectUtilEInv.setMemberValue(approvalBO, "EnvelopeType", envType);
		ProjectUtilEInv.setMemberValue(approvalBO, "TrCode", findTrCode(element, envType));
		ProjectUtilEInv.setMemberValue(approvalBO, "DocRef", 0);
		setDocumentProperties(doc, approvalBO);
		ProjectUtilEInv.setMemberValue(approvalBO, "OpType", ProjectGlobalsEInv.OPTYPE_INCOMING);
		ProjectUtilEInv.setMemberValue(approvalBO, "ProfileID", findProfileID(element, envType));
		ProjectUtilEInv.setMemberValue(approvalBO, "PKLabel", getNodeTextByTagName(doc, "sh:Sender", "sh:Identifier"));
		ProjectUtilEInv.setMemberValue(approvalBO, "GBLabel", getNodeTextByTagName(doc, "sh:Receiver", "sh:Identifier"));
		ProjectUtilEInv.setMemberValue(approvalBO, "Explain_", getNodeTextByTagName(element, "cac:LineResponse", "cbc:Description"));
		ProjectUtilEInv.setMemberValue(approvalBO, "RespCode", getNodeTextByTagName(element, "cac:LineResponse", "cbc:ResponseCode"));
		ProjectUtilEInv.setMemberValue(approvalBO, "ReferenceID", getNodeTextByTagName(element, "cac:DocumentReference", "cbc:ID"));
		ProjectUtilEInv.setMemberValue(approvalBO, "ConfirmRes", findConfirmRes(element, envType));
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);					
		ProjectUtilEInv.setMemberValue(approvalBO, "LData", ProjectUtilEInv.documentToByte(source, transformer));
		if(ProjectUtilEInv.persistCBO(m_Context, approvalBO));
			m_ReceivedCnt++;
	}
	
	private static void setDocumentProperties(Document document, CustomBusinessObject approvalBO)
	{
		if (document.getElementsByTagName("cbc:ID").item(0) != null)
		{
			ProjectUtilEInv.setMemberValue(approvalBO, "DocNr", document.getElementsByTagName("cbc:ID").item(0).getTextContent());
		}
		if (document.getElementsByTagName("cbc:IssueDate").item(0) != null)
		{
			ProjectUtilEInv.setMemberValue(approvalBO, "DocDate", 
					ProjectUtilEInv.stringToDate(document.getElementsByTagName("cbc:IssueDate").item(0).getTextContent(), ""));
		}
		ProjectUtilEInv.setMemberValue(approvalBO, "DocTotal",
				new BigDecimal(getNodeTextByTagName(document, "cac:LegalMonetaryTotal", "cbc:TaxInclusiveAmount")));
		
	}
	
	
	private static int findConfirmRes(Element element, int envType) {
		if (!(envType == ProjectGlobalsEInv.ENVELOPE_TYPE_POSTBOX))
			return 0;
		String responseCode = getNodeTextByTagName(element, "cac:DocumentResponse", "cbc:ResponseCode");
		if (responseCode.compareTo("KABUL") == 0)
			return ProjectGlobalsEInv.PR_ACCEPT;
		else if (responseCode.compareTo("RED") == 0)
			return ProjectGlobalsEInv.PR_REJECT;
		return 0;
	}


	private static int findProfileID(Element element, int envType)
	{
		if (!(envType == ProjectGlobalsEInv.ENVELOPE_TYPE_SENDER))
			return 0;

		String profileID = element.getElementsByTagName("cbc:ProfileID").item(0).getTextContent();
		
		if (profileID.compareTo("TEMELFATURA") == 0)
			return 1;
		else if (profileID.compareTo("TICARIFATURA") == 0)
			return 2;

		return 0;
	}
	
	private static int findTrCode(Element element, int envType)
	{
		if (!(envType == ProjectGlobalsEInv.ENVELOPE_TYPE_SENDER))
			return 0;
		
		 String invoiceTypeCode = element.getElementsByTagName("cbc:InvoiceTypeCode").item(0).getTextContent();
		 if(invoiceTypeCode.compareTo("SATIS") == 0)
			 return UnityConstants.INVC_PURCHASE;
		 else if(invoiceTypeCode.compareTo("IADE") == 0)
			 return UnityConstants.INVC_PURCHASERET;
		return 0;
	}
	
	private static String getNodeTextByTagName(Element element, String elementName, String nodeName)
	{
		NodeList nList = element.getElementsByTagName(elementName);
		if(nList != null)
			for (int temp = 0; temp < nList.getLength(); temp++)
			{
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element eElement = (Element) nNode;
					if(eElement.getElementsByTagName(nodeName).item(0) != null)
						return eElement.getElementsByTagName(nodeName).item(0).getTextContent();
				}
			}
		
		return "";
	}

	private static Element getElementByTagName(Document doc, String elementName, String nodeName)
	{
		NodeList nList = doc.getElementsByTagName(elementName);
		for (int temp = 0; temp < nList.getLength(); temp++)
		{
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element eElement = (Element) nNode;
				if (eElement.getElementsByTagName(nodeName).item(0) != null)
					return (Element)eElement.getElementsByTagName(nodeName).item(0).cloneNode(true);
			}
		}
		
		return null;
	}
	
	
	private static String getNodeTextByTagName(Document doc, String elementName, String nodeName)
	{
		NodeList nList = doc.getElementsByTagName(elementName);
		for (int temp = 0; temp < nList.getLength(); temp++)
		{
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element eElement = (Element) nNode;
				if (eElement.getElementsByTagName(nodeName).item(0) != null)
					return eElement.getElementsByTagName(nodeName).item(0).getTextContent();
			}
		}
		
		return "";
	}

	public static boolean sendEInvoices(IApplicationContext context, ArrayList invoiceRefList)
	{
		m_Context = context;
		findEInvoiceUserType();
		HashMap packedInvoiceMap = preparePackages(context, invoiceRefList);
		if (packedInvoiceMap.size() > 0)
			createPackagesByInvoices(packedInvoiceMap);
		return true;
	}
	
	/*QUESTION1
	 Paketlerken org.birim ve cari hesap bazýnda gruplama yapýlýyor doðrumu dur ?
	 */
	private static HashMap preparePackages(IApplicationContext context, ArrayList invoiceRefList)
	{
		HashMap packedInvoiceMap = new HashMap();
		for (int i = 0; i < invoiceRefList.size(); i++)
		{
			Integer invoiceRef = (Integer) invoiceRefList.get(i);
			LOBOConnectEInvoice invoice = (LOBOConnectEInvoice) UnityHelper.getBOByReference(context, LOBOConnectEInvoice.class, invoiceRef);
			
			GOBOOrgUnit orgUnit = findAndGetOrgUnit(invoice.getDivisionRef());	
			FIBOConnectArpInfo arpInfo = invoice.getARPInfo();
			if (orgUnit != null)
				m_OrgUnitMap.put(Integer.valueOf(invoice.getDivisionRef()), orgUnit);
			if (arpInfo != null)
				m_ArpInfoMap.put(Integer.valueOf(invoice.getARPRef()), arpInfo);
			
			String key = invoice.getDivisionRef() + "-" + invoice.getARPRef();
			if(packedInvoiceMap.containsKey(key))
			{
				ArrayList packedInvoiceList = (ArrayList) packedInvoiceMap.get(key);
				packedInvoiceList.add(invoice);
				packedInvoiceMap.put(key, packedInvoiceList);
			}
			else
			{
				ArrayList packedInvoiceList  = new ArrayList();
				packedInvoiceList.add(invoice);
				packedInvoiceMap.put(key, packedInvoiceList);
			}	
			
		}
		return packedInvoiceMap;
	}
	
	private static void findEInvoiceUserType() {
		QueryParams params = new QueryParams();
		params.getParameters().put("P_LOGICALREF", ProjectUtilEInv.getCompanyRef(m_Context));
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		QueryBusinessObjects results = new QueryBusinessObjects();
		IQueryFactory factory = (IQueryFactory) m_Context.getQueryFactory();
		try {
			factory.select("CQOFindEInvoiceUserType", params, results, -1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (results != null && results.size() > 0) {
			QueryBusinessObject result = results.get(0);
		    m_EInvoiceUserType =  QueryUtil.getIntProp(result, "EINVOICEUSER");
		}
		
	}
	
	
	private static void createInvoice()
	{
		
		m_ArpInfo = m_Invoice.getARPInfo();
		setVatName();
		calculateTaxTotal(m_Invoice.getTransactions());
		Element root = null;
		try
		{
			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
			Document invoiceXML = docBuilder.newDocument();
			
			root = invoiceXML.createElement("Invoice");
			root.setAttribute("xmlns", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2");
			root.setAttribute("xmlns:cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
			root.setAttribute("xmlns:xades", "http://uri.etsi.org/01903/v1.3.2#");
			root.setAttribute("xmlns:udt", "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2");
			root.setAttribute("xmlns:cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
			root.setAttribute("xmlns:ccts", "urn:un:unece:uncefact:documentation:2");
			root.setAttribute("xmlns:ubltr", "urn:oasis:names:specification:ubl:schema:xsd:TurkishCustomizationExtensionComponents");
			root.setAttribute("xmlns:qdt", "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2");
			root.setAttribute("xmlns:ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
			root.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
			root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			root.setAttribute("xsi:schemaLocation", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2 UBLTR-Invoice-2.0.xsd");
			invoiceXML.setXmlStandalone(true);
			invoiceXML.appendChild(root);
			//invoiceXML.appendChild(root);
			
			Node cbcUBLVersionID = (Node) invoiceXML.createElement("cbc:UBLVersionID");
			cbcUBLVersionID.appendChild(invoiceXML.createTextNode("2.0"));
			root.appendChild(cbcUBLVersionID);
			
			Element cbcCustomizationID = invoiceXML.createElement("cbc:CustomizationID");
			cbcCustomizationID.appendChild(invoiceXML.createTextNode("TR1.0"));
			root.appendChild(cbcCustomizationID);
			
			Element cbcProfileID = invoiceXML.createElement("cbc:ProfileID");
			cbcProfileID.appendChild(invoiceXML.createTextNode(getProfileID()));
			root.appendChild(cbcProfileID);
			
			Element cbcID = invoiceXML.createElement("cbc:ID");
			cbcID.appendChild(invoiceXML.createTextNode(m_Invoice
					.getInvoiceNumber() == null ? "" : m_Invoice
					.getInvoiceNumber()));
			root.appendChild(cbcID);
			
			Element cbcCopyIndicator = invoiceXML.createElement("cbc:CopyIndicator");
			cbcCopyIndicator.appendChild(invoiceXML.createTextNode(getCopyIndicator()));
			root.appendChild(cbcCopyIndicator);
			
			Element cbcUUID = invoiceXML.createElement("cbc:UUID");
			cbcUUID.appendChild(invoiceXML.createTextNode(m_Invoice.getGUId() == null ? "" : m_Invoice.getGUId() ));
			root.appendChild(cbcUUID);
			
			
			Element cbcIssueDate = invoiceXML.createElement("cbc:IssueDate");
			cbcIssueDate.appendChild(invoiceXML.createTextNode(getIssueDate(m_Invoice.getInvoiceDate())));
			root.appendChild(cbcIssueDate);
			
			Element cbcInvoiceTypeCode = invoiceXML.createElement("cbc:InvoiceTypeCode");
			cbcInvoiceTypeCode.appendChild(invoiceXML.createTextNode(getInvoiceTypeCode()));
			root.appendChild(cbcInvoiceTypeCode);
			
			
			root.appendChild(createNoteElement(m_Context, invoiceXML));
			
			Element cbcDocumentCurrencyCode = invoiceXML.createElement("cbc:DocumentCurrencyCode");
			cbcDocumentCurrencyCode.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");
			cbcDocumentCurrencyCode.setAttribute("listID", "ISO 4217 Alpha");
			cbcDocumentCurrencyCode.setAttribute("listName", "Currency");
			cbcDocumentCurrencyCode.setAttribute("listVersionID", "2001");
			cbcDocumentCurrencyCode.appendChild(invoiceXML.createTextNode(getDocumentCurrencyCode()));
			root.appendChild(cbcDocumentCurrencyCode);
			
			Element cbcLineCountNumeric = invoiceXML.createElement("cbc:LineCountNumeric");
			cbcLineCountNumeric.appendChild(invoiceXML.createTextNode(getLineCountNumeric()));
			root.appendChild(cbcLineCountNumeric);
			
			if (m_Invoice.getInvoiceType() == UnityConstants.INVC_PURCHASERET)
			{
				LOBOConnectEInvoice srcInvoice = findSrcInvoice();
				Element cacAddDocRef = invoiceXML.createElement("cac:AdditionalDocumentReference");
				Element addDocRefID =  invoiceXML.createElement("cbc:ID");
				addDocRefID.appendChild(invoiceXML.createTextNode(srcInvoice != null ? srcInvoice.getInvoiceNumber() : ""));
				cacAddDocRef.appendChild(addDocRefID);
				
				Element addDocRefIssueDate = invoiceXML.createElement("cbc:IssueDate");
				addDocRefIssueDate.appendChild(invoiceXML.createTextNode(getIssueDate(srcInvoice.getInvoiceDate())));
				cacAddDocRef.appendChild(addDocRefIssueDate);
				
				Element addDocRefDocTypeCode = invoiceXML.createElement("cbc:DocumentTypeCode");
				addDocRefDocTypeCode.appendChild(invoiceXML.createTextNode("IADE"));
				cacAddDocRef.appendChild(addDocRefDocTypeCode);
				root.appendChild(cacAddDocRef);
				
			}
				
			Element cacAdditionalDocumentReference = invoiceXML.createElement("cac:AdditionalDocumentReference");
			Element cbcAddDocRefID =  invoiceXML.createElement("cbc:ID");
			cbcAddDocRefID.appendChild(invoiceXML.createTextNode(ProjectUtilEInv.generateGUID()));
			cacAdditionalDocumentReference.appendChild(cbcAddDocRefID);
			
			Element cbcAddDocRefIssueDate = invoiceXML.createElement("cbc:IssueDate");
			cbcAddDocRefIssueDate.appendChild(invoiceXML.createTextNode(getIssueDate(m_Invoice.getInvoiceDate())));
			cacAdditionalDocumentReference.appendChild(cbcAddDocRefIssueDate);
			
			root.appendChild(cacAdditionalDocumentReference);
			
			Element cacAttachment = invoiceXML.createElement("cac:Attachment");
			
			cacAdditionalDocumentReference.appendChild(cacAttachment);
			Element cbcEmbeddedDocumentBinaryObject = invoiceXML.createElement("cbc:EmbeddedDocumentBinaryObject");
			cbcEmbeddedDocumentBinaryObject.setAttribute("characterSetCode", "UTF-8");
			cbcEmbeddedDocumentBinaryObject.setAttribute("encodingCode", "Base64");
			cbcEmbeddedDocumentBinaryObject.setAttribute("filename", m_Invoice.getInvoiceNumber()+".xslt");
			cbcEmbeddedDocumentBinaryObject.setAttribute("mimeCode", "application/xml");
			cbcEmbeddedDocumentBinaryObject
					.appendChild(invoiceXML
							.createTextNode("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4NCjx4c2w6c3R5bGVzaGVldCB2ZXJzaW9uPSIyLjAiIHhtbG5zOnhzbD0iaHR0cDovL3d3dy53My5vcmcvMTk5OS9YU0wvVHJhbnNmb3JtIg0KCXhtbG5zOmNhYz0idXJuOm9hc2lzOm5hbWVzOnNwZWNpZmljYXRpb246dWJsOnNjaGVtYTp4c2Q6Q29tbW9uQWdncmVnYXRlQ29tcG9uZW50cy0yIg0KCXhtbG5zOmNiYz0idXJuOm9hc2lzOm5hbWVzOnNwZWNpZmljYXRpb246dWJsOnNjaGVtYTp4c2Q6Q29tbW9uQmFzaWNDb21wb25lbnRzLTIiDQoJeG1sbnM6Y2N0cz0idXJuOnVuOnVuZWNlOnVuY2VmYWN0OmRvY3VtZW50YXRpb246MiINCgl4bWxuczpjbG01NDIxNz0idXJuOnVuOnVuZWNlOnVuY2VmYWN0OmNvZGVsaXN0OnNwZWNpZmljYXRpb246NTQyMTc6MjAwMSINCgl4bWxuczpjbG01NjM5PSJ1cm46dW46dW5lY2U6dW5jZWZhY3Q6Y29kZWxpc3Q6c3BlY2lmaWNhdGlvbjo1NjM5OjE5ODgiDQoJeG1sbnM6Y2xtNjY0MTE9InVybjp1bjp1bmVjZTp1bmNlZmFjdDpjb2RlbGlzdDpzcGVjaWZpY2F0aW9uOjY2NDExOjIwMDEiDQoJeG1sbnM6Y2xtSUFOQU1JTUVNZWRpYVR5cGU9InVybjp1bjp1bmVjZTp1bmNlZmFjdDpjb2RlbGlzdDpzcGVjaWZpY2F0aW9uOklBTkFNSU1FTWVkaWFUeXBlOjIwMDMiDQoJeG1sbnM6Zm49Imh0dHA6Ly93d3cudzMub3JnLzIwMDUveHBhdGgtZnVuY3Rpb25zIiB4bWxuczpsaW5rPSJodHRwOi8vd3d3Lnhicmwub3JnLzIwMDMvbGlua2Jhc2UiDQoJeG1sbnM6bjE9InVybjpvYXNpczpuYW1lczpzcGVjaWZpY2F0aW9uOnVibDpzY2hlbWE6eHNkOkludm9pY2UtMiINCgl4bWxuczpxZHQ9InVybjpvYXNpczpuYW1lczpzcGVjaWZpY2F0aW9uOnVibDpzY2hlbWE6eHNkOlF1YWxpZmllZERhdGF0eXBlcy0yIg0KCXhtbG5zOnVkdD0idXJuOnVuOnVuZWNlOnVuY2VmYWN0OmRhdGE6c3BlY2lmaWNhdGlvbjpVbnF1YWxpZmllZERhdGFUeXBlc1NjaGVtYU1vZHVsZToyIg0KCXhtbG5zOnhicmxkaT0iaHR0cDovL3hicmwub3JnLzIwMDYveGJybGRpIiB4bWxuczp4YnJsaT0iaHR0cDovL3d3dy54YnJsLm9yZy8yMDAzL2luc3RhbmNlIg0KCXhtbG5zOnhkdD0iaHR0cDovL3d3dy53My5vcmcvMjAwNS94cGF0aC1kYXRhdHlwZXMiIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIg0KCXhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNkPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSINCgl4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIg0KCWV4Y2x1ZGUtcmVzdWx0LXByZWZpeGVzPSJjYWMgY2JjIGNjdHMgY2xtNTQyMTcgY2xtNTYzOSBjbG02NjQxMSBjbG1JQU5BTUlNRU1lZGlhVHlwZSBmbiBsaW5rIG4xIHFkdCB1ZHQgeGJybGRpIHhicmxpIHhkdCB4bGluayB4cyB4c2QgeHNpIj4NCgk8eHNsOmRlY2ltYWwtZm9ybWF0IG5hbWU9ImV1cm9wZWFuIiBkZWNpbWFsLXNlcGFyYXRvcj0iLCIgZ3JvdXBpbmctc2VwYXJhdG9yPSIuIiBOYU49IiIvPg0KCTx4c2w6b3V0cHV0IHZlcnNpb249IjQuMCIgbWV0aG9kPSJodG1sIiBpbmRlbnQ9Im5vIiBlbmNvZGluZz0iVVRGLTgiDQoJCWRvY3R5cGUtcHVibGljPSItLy9XM0MvL0RURCBIVE1MIDQuMDEgVHJhbnNpdGlvbmFsLy9FTiINCgkJZG9jdHlwZS1zeXN0ZW09Imh0dHA6Ly93d3cudzMub3JnL1RSL2h0bWw0L2xvb3NlLmR0ZCIvPg0KCTx4c2w6cGFyYW0gbmFtZT0iU1ZfT3V0cHV0Rm9ybWF0IiBzZWxlY3Q9IidIVE1MJyIvPg0KCTx4c2w6dmFyaWFibGUgbmFtZT0iWE1MIiBzZWxlY3Q9Ii8iLz4NCgk8eHNsOnRlbXBsYXRlIG1hdGNoPSIvIj4NCgkJPGh0bWw+DQoJCQk8aGVhZD4NCgkJCQk8dGl0bGUvPg0KCQkJCTxzdHlsZSB0eXBlPSJ0ZXh0L2NzcyI+DQoJCQkJCWJvZHkgew0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjogI0ZGRkZGRjsNCgkJCQkJICAgIGZvbnQtZmFtaWx5OiAnVGFob21hJywgIlRpbWVzIE5ldyBSb21hbiIsIFRpbWVzLCBzZXJpZjsNCgkJCQkJICAgIGZvbnQtc2l6ZTogMTFweDsNCgkJCQkJICAgIGNvbG9yOiAjNjY2NjY2Ow0KCQkJCQl9DQoJCQkJCWgxLCBoMiB7DQoJCQkJCSAgICBwYWRkaW5nLWJvdHRvbTogM3B4Ow0KCQkJCQkgICAgcGFkZGluZy10b3A6IDNweDsNCgkJCQkJICAgIG1hcmdpbi1ib3R0b206IDVweDsNCgkJCQkJICAgIHRleHQtdHJhbnNmb3JtOiB1cHBlcmNhc2U7DQoJCQkJCSAgICBmb250LWZhbWlseTogQXJpYWwsIEhlbHZldGljYSwgc2Fucy1zZXJpZjsNCgkJCQkJfQ0KCQkJCQloMSB7DQoJCQkJCSAgICBmb250LXNpemU6IDEuNGVtOw0KCQkJCQkgICAgdGV4dC10cmFuc2Zvcm06bm9uZTsNCgkJCQkJfQ0KCQkJCQloMiB7DQoJCQkJCSAgICBmb250LXNpemU6IDFlbTsNCgkJCQkJICAgIGNvbG9yOiBicm93bjsNCgkJCQkJfQ0KCQkJCQloMyB7DQoJCQkJCSAgICBmb250LXNpemU6IDFlbTsNCgkJCQkJICAgIGNvbG9yOiAjMzMzMzMzOw0KCQkJCQkgICAgdGV4dC1hbGlnbjoganVzdGlmeTsNCgkJCQkJICAgIG1hcmdpbjogMDsNCgkJCQkJICAgIHBhZGRpbmc6IDA7DQoJCQkJCX0NCgkJCQkJaDQgew0KCQkJCQkgICAgZm9udC1zaXplOiAxLjFlbTsNCgkJCQkJICAgIGZvbnQtc3R5bGU6IGJvbGQ7DQoJCQkJCSAgICBmb250LWZhbWlseTogQXJpYWwsIEhlbHZldGljYSwgc2Fucy1zZXJpZjsNCgkJCQkJICAgIGNvbG9yOiAjMDAwMDAwOw0KCQkJCQkgICAgbWFyZ2luOiAwOw0KCQkJCQkgICAgcGFkZGluZzogMDsNCgkJCQkJfQ0KCQkJCQlociB7DQoJCQkJCSAgICBoZWlnaHQ6MnB4Ow0KCQkJCQkgICAgY29sb3I6ICMwMDAwMDA7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOiAjMDAwMDAwOw0KCQkJCQkgICAgYm9yZGVyLWJvdHRvbTogMXB4IHNvbGlkICMwMDAwMDA7DQoJCQkJCX0NCgkJCQkJcCwgdWwsIG9sIHsNCgkJCQkJICAgIG1hcmdpbi10b3A6IDEuNWVtOw0KCQkJCQl9DQoJCQkJCXVsLCBvbCB7DQoJCQkJCSAgICBtYXJnaW4tbGVmdDogM2VtOw0KCQkJCQl9DQoJCQkJCWJsb2NrcXVvdGUgew0KCQkJCQkgICAgbWFyZ2luLWxlZnQ6IDNlbTsNCgkJCQkJICAgIG1hcmdpbi1yaWdodDogM2VtOw0KCQkJCQkgICAgZm9udC1zdHlsZTogaXRhbGljOw0KCQkJCQl9DQoJCQkJCWEgew0KCQkJCQkgICAgdGV4dC1kZWNvcmF0aW9uOiBub25lOw0KCQkJCQkgICAgY29sb3I6ICM3MEEzMDA7DQoJCQkJCX0NCgkJCQkJYTpob3ZlciB7DQoJCQkJCSAgICBib3JkZXI6IG5vbmU7DQoJCQkJCSAgICBjb2xvcjogIzcwQTMwMDsNCgkJCQkJfQ0KCQkJCQkjZGVzcGF0Y2hUYWJsZSB7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6Y29sbGFwc2U7DQoJCQkJCSAgICBmb250LXNpemU6MTFweDsNCgkJCQkJICAgIGZsb2F0OnJpZ2h0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOmdyYXk7DQoJCQkJCX0NCgkJCQkJI2V0dG5UYWJsZSB7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6Y29sbGFwc2U7DQoJCQkJCSAgICBmb250LXNpemU6MTFweDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjpncmF5Ow0KCQkJCQl9DQoJCQkJCSNjdXN0b21lclBhcnR5VGFibGUgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBncmF5Ow0KCQkJCQkgICAgYm9yZGVyLWNvbGxhcHNlOiBjb2xsYXBzZTsNCgkJCQkJICAgIGJhY2tncm91bmQtY29sb3I6DQoJCQkJCX0NCgkJCQkJI2N1c3RvbWVySURUYWJsZSB7DQoJCQkJCSAgICBib3JkZXItd2lkdGg6IDJweDsNCgkJCQkJICAgIGJvcmRlci1zcGFjaW5nOjsNCgkJCQkJICAgIGJvcmRlci1zdHlsZTogaW5zZXQ7DQoJCQkJCSAgICBib3JkZXItY29sb3I6IGdyYXk7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6IGNvbGxhcHNlOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjoNCgkJCQkJfQ0KCQkJCQkjY3VzdG9tZXJJRFRhYmxlVGQgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAycHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBncmF5Ow0KCQkJCQkgICAgYm9yZGVyLWNvbGxhcHNlOiBjb2xsYXBzZTsNCgkJCQkJICAgIGJhY2tncm91bmQtY29sb3I6DQoJCQkJCX0NCgkJCQkJI2xpbmVUYWJsZSB7DQoJCQkJCSAgICBib3JkZXItd2lkdGg6MnB4Ow0KCQkJCQkgICAgYm9yZGVyLXNwYWNpbmc6Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6IGNvbGxhcHNlOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjo7DQoJCQkJCX0NCgkJCQkJI2xpbmVUYWJsZVRkIHsNCgkJCQkJICAgIGJvcmRlci13aWR0aDogMXB4Ow0KCQkJCQkgICAgcGFkZGluZzogMXB4Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOiB3aGl0ZTsNCgkJCQkJfQ0KCQkJCQkjbGluZVRhYmxlVHIgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAxcHg7DQoJCQkJCSAgICBwYWRkaW5nOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBibGFjazsNCgkJCQkJICAgIGJhY2tncm91bmQtY29sb3I6IHdoaXRlOw0KCQkJCQkgICAgLW1vei1ib3JkZXItcmFkaXVzOjsNCgkJCQkJfQ0KCQkJCQkjbGluZVRhYmxlRHVtbXlUZCB7DQoJCQkJCSAgICBib3JkZXItd2lkdGg6IDFweDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjp3aGl0ZTsNCgkJCQkJICAgIHBhZGRpbmc6IDFweDsNCgkJCQkJICAgIGJvcmRlci1zdHlsZTogaW5zZXQ7DQoJCQkJCSAgICBib3JkZXItY29sb3I6IGJsYWNrOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjogd2hpdGU7DQoJCQkJCX0NCgkJCQkJI2xpbmVUYWJsZUJ1ZGdldFRkIHsNCgkJCQkJICAgIGJvcmRlci13aWR0aDogMnB4Ow0KCQkJCQkgICAgYm9yZGVyLXNwYWNpbmc6MHB4Ow0KCQkJCQkgICAgcGFkZGluZzogMXB4Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOiB3aGl0ZTsNCgkJCQkJICAgIC1tb3otYm9yZGVyLXJhZGl1czo7DQoJCQkJCX0NCgkJCQkJI25vdGVzVGFibGUgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAycHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBibGFjazsNCgkJCQkJICAgIGJvcmRlci1jb2xsYXBzZTogY29sbGFwc2U7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOg0KCQkJCQl9DQoJCQkJCSNub3Rlc1RhYmxlVGQgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBibGFjazsNCgkJCQkJICAgIGJvcmRlci1jb2xsYXBzZTogY29sbGFwc2U7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOg0KCQkJCQl9DQoJCQkJCXRhYmxlIHsNCgkJCQkJICAgIGJvcmRlci1zcGFjaW5nOjBweDsNCgkJCQkJfQ0KCQkJCQkjYnVkZ2V0Q29udGFpbmVyVGFibGUgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzogMHB4Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6IGNvbGxhcHNlOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjo7DQoJCQkJCX0NCgkJCQkJdGQgew0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOmdyYXk7DQoJCQkJCX08L3N0eWxlPg0KCQkJCTx0aXRsZT5lLUZhdHVyYTwvdGl0bGU+DQoJCQk8L2hlYWQ+DQoJCQk8Ym9keQ0KCQkJCXN0eWxlPSJtYXJnaW4tbGVmdD0wLjZpbjsgbWFyZ2luLXJpZ2h0PTAuNmluOyBtYXJnaW4tdG9wPTAuNzlpbjsgbWFyZ2luLWJvdHRvbT0wLjc5aW4iPg0KCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSIkWE1MIj4NCgkJCQkJPHRhYmxlIHN0eWxlPSJib3JkZXItY29sb3I6Ymx1ZTsgIiBib3JkZXI9IjAiIGNlbGxzcGFjaW5nPSIwcHgiIHdpZHRoPSI4MDAiDQoJCQkJCQljZWxscGFkZGluZz0iMHB4Ij4NCgkJCQkJCTx0Ym9keT4NCgkJCQkJCQk8dHIgdmFsaWduPSJ0b3AiPg0KCQkJCQkJCQk8dGQgd2lkdGg9IjQwJSI+DQoJCQkJCQkJCQk8YnIvPg0KCQkJCQkJCQkJPHRhYmxlIGFsaWduPSJjZW50ZXIiIGJvcmRlcj0iMCIgd2lkdGg9IjEwMCUiPg0KCQkJCQkJCQkJCTx0Ym9keT4NCgkJCQkJCQkJCQkJPGhyLz4NCgkJCQkJCQkJCQkJPHRyIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6QWNjb3VudGluZ1N1cHBsaWVyUGFydHkiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSJjYWM6UGFydHlOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYWM6UGFydHlOYW1lL2NiYzpOYW1lIi8+DQoJCQkJCQkJCQkJCQk8YnIvPg0KCQkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBlcnNvbiI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRpdGxlIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6Rmlyc3ROYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6TWlkZGxlTmFtZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkZhbWlseU5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpOYW1lU3VmZml4Ij4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBvc3RhbEFkZHJlc3MiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpTdHJlZXROYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6QnVpbGRpbmdOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSJjYmM6QnVpbGRpbmdOdW1iZXIiPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+IE5vOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkJ1aWxkaW5nTnVtYmVyIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UG9zdGFsWm9uZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlTdWJkaXZpc2lvbk5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4vIDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eS9jYWM6UGFydHkvY2FjOkNvbnRhY3QvY2JjOlRlbGVwaG9uZSBvciAvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdTdXBwbGllclBhcnR5L2NhYzpQYXJ0eS9jYWM6Q29udGFjdC9jYmM6VGVsZWZheCI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkNvbnRhY3QiPg0KCQkJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSJjYmM6VGVsZXBob25lIj4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PlRlbDogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6VGVsZXBob25lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9ImNiYzpUZWxlZmF4Ij4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiBGYXg6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRlbGVmYXgiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdTdXBwbGllclBhcnR5L2NhYzpQYXJ0eS9jYmM6V2Vic2l0ZVVSSSI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHRkPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PldlYiBTaXRlc2k6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLiIvPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ1N1cHBsaWVyUGFydHkvY2FjOlBhcnR5L2NhYzpDb250YWN0L2NiYzpFbGVjdHJvbmljTWFpbCI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHRkPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkUtUG9zdGE6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLiIvPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5WZXJnaSBEYWlyZXNpOiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpQYXJ0eVRheFNjaGVtZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlRheFNjaGVtZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOk5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOyA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ1N1cHBsaWVyUGFydHkvY2FjOlBhcnR5L2NhYzpQYXJ0eUlkZW50aWZpY2F0aW9uIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iY2JjOklEL0BzY2hlbWVJRCIvPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PjogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6SUQiLz4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQk8L3Rib2R5Pg0KCQkJCQkJCQkJPC90YWJsZT4NCgkJCQkJCQkJCTxoci8+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCTx0ZCB3aWR0aD0iMjAlIiBhbGlnbj0iY2VudGVyIiB2YWxpZ249Im1pZGRsZSI+DQoJCQkJCQkJCQk8YnIvPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCTxpbWcgc3R5bGU9IndpZHRoOjkxcHg7IiBhbGlnbj0ibWlkZGxlIiBhbHQ9IkUtRmF0dXJhIExvZ28iDQoJCQkJCQkJCQkJc3JjPSJkYXRhOmltYWdlL2pwZWc7YmFzZTY0LC85ai80QUFRU2taSlJnQUJBUUVCTEFFc0FBRC80UUR3UlhocFpnQUFTVWtxQUFnQUFBQUtBQUFCQXdBQkFBQUF3QWxqQUFFQkF3QUJBQUFBWlFsekFBSUJBd0FFQUFBQWhnQUFBQU1CQXdBQkFBQUFBUUJuQUFZQkF3QUJBQUFBQWdCMUFCVUJBd0FCQUFBQUJBQnpBQndCQXdBQkFBQUFBUUJuQURFQkFnQWNBQUFBamdBQUFESUJBZ0FVQUFBQXFnQUFBR21IQkFBQkFBQUF2Z0FBQUFBQUFBQUlBQWdBQ0FBSUFFRmtiMkpsSUZCb2IzUnZjMmh2Y0NCRFV6UWdWMmx1Wkc5M2N3QXlNREE1T2pBNE9qSTRJREUyT2pRM09qRTNBQU1BQWFBREFBRUFBQUFCQVAvL0FxQUVBQUVBQUFDV0FBQUFBNkFFQUFFQUFBQ1JBQUFBQUFBQUFQL2JBRU1BQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQWYvYkFFTUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBZi9BQUJFSUFHWUFhUU1CSWdBQ0VRRURFUUgveEFBZkFBQUJCUUVCQVFFQkFRQUFBQUFBQUFBQUFRSURCQVVHQndnSkNndi94QUMxRUFBQ0FRTURBZ1FEQlFVRUJBQUFBWDBCQWdNQUJCRUZFaUV4UVFZVFVXRUhJbkVVTW9HUm9RZ2pRckhCRlZMUjhDUXpZbktDQ1FvV0Z4Z1pHaVVtSnlncEtqUTFOamM0T1RwRFJFVkdSMGhKU2xOVVZWWlhXRmxhWTJSbFptZG9hV3B6ZEhWMmQzaDVlb09FaFlhSGlJbUtrcE9VbFphWG1KbWFvcU9rcGFhbnFLbXFzck8wdGJhM3VMbTZ3c1BFeGNiSHlNbkswdFBVMWRiWDJObmE0ZUxqNU9YbTUranA2dkh5OC9UMTl2ZjQrZnIveEFBZkFRQURBUUVCQVFFQkFRRUJBQUFBQUFBQUFRSURCQVVHQndnSkNndi94QUMxRVFBQ0FRSUVCQU1FQndVRUJBQUJBbmNBQVFJREVRUUZJVEVHRWtGUkIyRnhFeUl5Z1FnVVFwR2hzY0VKSXpOUzhCVmljdEVLRmlRMDRTWHhGeGdaR2lZbktDa3FOVFkzT0RrNlEwUkZSa2RJU1VwVFZGVldWMWhaV21Oa1pXWm5hR2xxYzNSMWRuZDRlWHFDZzRTRmhvZUlpWXFTazVTVmxwZVltWnFpbzZTbHBxZW9xYXF5czdTMXRyZTR1YnJDdzhURnhzZkl5Y3JTMDlUVjF0ZlkyZHJpNCtUbDV1Zm82ZXJ5OC9UMTl2ZjQrZnIvMmdBTUF3RUFBaEVERVFBL0FQNytLS0tRL3dBaC9ubnArSDVrVUFMWGpmeGsvYUIrRFg3UCtnSjRqK0wvQU1RL0RuZ214dUg4alM3UFU3Nk50ZDhRWHJZRVdtZUdmRGx0NSt1K0k5VW1abFdIVHRGMCs5dTNMRDkxdHl3K1VQaTUrMWg0eThkZUxQRlh3WS9aTlBoVjlUOEdYQzZYOFovMm1QSHN5Ui9CTDRBM0UyMUo5SlZwTG13aitKUHhTdDRwNGJpRHdQcGVwMk9sNldaSW44VytJTkg4MjN0YnI4MExuNHhlQ3ZCUGlYeDliL3NoZUdyajlybjl2LzRiL3REK0RmZzk4Uy9pRiswZFlUYWo0cDhRV212Mi9pdVdYVi9odGV5MzJuK0hQaDU4TE5SOFErRE5ZOENIV2ZCYWFQNFo4TFBiVDZucWRyckYzWjZjbXFmWTVUd25pTVU0enhpcVUxYWxPV0hqT25RZENsWG5DblJyNXBqYTZsaHNud3RTZFdtb1R4RWF1SW43U2xKWVZVYXNLNTVPS3pPRlAzYVBMTDRrcWpUbnp5aW5LVU1QUmcxVXhFNHhVbTFIbGdyUDM1U2k0bjZCL0VEOXQ3NDgzM2cvV1BIUHduL1pnMWI0ZmZEYlNZNEpydjR6ZnRjNm5xWHdoOE93V1Z6Y1JXMFdxV253dThQYUo0eStNRnpaUDlvaG5qbDEzd3o0VGpqUlpHMU45TXRFYTlYeUhWUGkzOGR0YjhVeStEUEZQL0JTYjRMZURmR2lSMnQ3Yy9ENzltLzluZlNmRjJ1V21pYWw0TDFUNGhXT3VQZWZFbnhGNDZ2cm53emQrRE5Idjlac3ZHMXZwTmg0ZnZJMGlTMWtGMWMyMXJKNkg0VS9aOC9hbCtPL2dYOXBENGVmdEVMb3ZocjRKL3RRMnQ1NGt0ZkIzeEE4UUw4VHZpOThCci94cDhNOUw4TmVKZmhoNFpPaFR5L0QyWHc3NEw4ZDZXZkdmZ254SEQ0bjFJUWkrdkxhUHc5WnkzVU0rbGZWbmhqOWo3NFhhWHEvd244WmVLNWRZK0lIeE8rRlB3UzFyNEJ3L0ViVzVMUFRkYzhYK0J2RVZyb2xwck1maTYzOFAybW1XRi9mWENhRmJ5V3M4TU5zTlBsdTlUbHMwamUvbVk5TThYa09YVTUwWTBNRzYwWFVpdnFWR2htVGtuaDZGVER6cVl6TktXTHBxcFR4S3hHSHhhd2ZzSVN0U3E0ZURwODNQbXFPTnhEVW5LcHl0UmI5dE9kRko4OGxOS2xoNVUzWnc1SjArZm1rdFlUbGZiNEgrQ0g5cC90Ri9DUHhEOGZmaHgvd1UzL2FoMUg0ZmVHdE5tMWpWZkVqZUNmMmVyTFQwdGJid3ZhZU1McTZUdzliL0RlL3V0UDhqUTc2MXZwOUQxV094MWV6RnhIYjNWbERJeTdzRDRWZkhENDBlT2ZocjRwK012d2Qvd0NDb0h3bjhZL0Ryd05Qb2tmaXUvOEEyc1AyYmZEZmdIUmZEbzhSYVJwMnZhQkRybmlyd2hyM3dtYlRJTmIwalZkTnZMTFdKNGRSaWpndjRwbnRyaHRrQi9VVDRmOEE3THZ3VCtGM3doMWY0RCtDdkRXdWFmOEFDYld2REUvZ3U1OElhbDhRL2lSNG50clB3bmNhQ2ZERCtIdEExRHhUNHQxcldQQytreDZFZnNGclplR3RSMHFDeVFMTlpwQmNJa3kvSlBpei9na3QreVRyL3dBS1BFSHdkME94K0l2Z3Z3ZDRqV1M0MUN3MGI0a2VLOVNndTlYc2ZoMi93eDhHYW5xY0hpWFVOWkdyUmVBUERMQ0x3NW8xN0kyaXozQ3JjNjlZYXhjUlcwdHZwUXp2SUsrSXhVTVhMRzA4TExNS0gxQ3BWeWJJY1k2R1djMHZyS3hXSFdHZ3F1TmxEbFZHZEN0VHB3a20ycEtYdVRQQlkyRUtUcEtqS29xTXZiS09KeGRLK0kwNUhUbnpTU3BMVnlVMDVQb1hvdjJwdjJ3UGhGREhjL3RCZnNsUi9GSHdoOW5ndkgrS2Y3RlBpNlQ0dXdSNmJjeEdhMzFPOStFWGl2VC9BQWY4U1h0cG9ObHdSNFJpOFp5c3JsYkNEVUk0ek9mcXY0RmZ0UmZBWDlwVFNyclUvZzE4U3ZEM2k2NTBwekI0aThNcGNQcGZqandqZXJnUzZkNHc4RDZ2SFkrSy9DOS9FN0NON2JXOUpzblpzbUx6RXd4L1AxLzJNLzJqdmcxOGFyZjQwZUdQakY4Ui9qUDRIaDhIZUVmQ2VyL0J6d2JyT2lmQ2p4RHEyay9CVDRiNmRwM3drc0cxM1Z0V2ZUdFdiWGZIeitOTDd4L2F3NjM0TDBYV05QOEFGK2pqVWJPK3QvQjYydXJmSWVvL0ZYNFhmRnlOdkZ2N2FmZ2U5L1pCL2JDdS93QnI2OS9adStCM3hJL1owdDlXc1BpOTRXdDdqUS9ocGNhVnJ2akh4UnBVbDNwdmp2NGM2UDQ3K0lscjRJOFM2eDRpdHRWK0dldVR2b3R5K2syLzI1cExlbmtlV1pyVGRUQXlvMVpLbGhuT3RrL3RmYXdyMXFWU3BVaFh5TEYxWjR5cEhEZXdxeXhXSndNNk9IcFUzQ3BTb1ZuTDJiU3htSXdyNWEzUEhXZkxIRldhbEdNb1JpNFl1bkZVNHlxYzZWT25XVG5LVithVVZxZjBlVVYrWVB3di9hMytKZndQOFUrRVBnMysydFA0YjFYU1BHK3F4K0dmZ2orMmI0RGpnZytEM3hsMVI1WGdzdkRYeEIwdXhtdjdYNE4vRkM1ZFZzNExLKzFHZndaNHQxSkxpRHd4cTZYMGNta3gvcDZDQ0FRY2c4Z2pvUjZqMUI3SHYxRmZHNDdMOFJsODR4cktFNlZWT1dIeFZHWHRNTmlZUmRwU28xTEozZy9kcTBxa1lWNkU3MDY5S25VVGl2V29ZaW5pSXR4dkdVV2xVcHpWcDA1Tlh0SmJOTmF4bEZ1RTFhVVpOTzR0RkZGY0p1RmZtbisxaDhjL0VQanZ4cHJIN0xQd2Y4YlA4UExQUWZEc1BpNzlyRDlvR3hkUko4QS9oYmV4U3pXSGgvd3Zkc3Mxci93dUw0bFIydHhZZUdMZWFDNmZ3NW9yMzNpbDdTNHVZZEtzN3I2Zy9hcytQVnAremg4RHZHUHhMV3dmWGZGRWNOcDRaK0d2aEdETFgvamo0cCtMYnFQdy93RER6d1pwc0FEU3ozZmlIeFRmNmJZaElZNVpWZ2ttbFNLUm94RzM1K2VBUGhKOFBQRS83TVg3UnY3TEZ4NGo4UmZFajlwSzUxL3dqNDAvYWcxejRXZU52Q25oMzRtNmg4ZnZHbW8rRS9pQk5yM2gyODFYVkpWMFRUdmh4UGIrSHJYUmJmVzdHTFIxOEwrR2JmUVk0ZFhuR293VGZWNUJncWRDbC9iV0xwVGxScDRtamg4TkpVbFZoaDVPdmg2ZUt6V3RDZHFVcU9YTEVVVlJoV2txVmJINGpEeG56VXFWYUV2TXgxWnprOEpUa2xKMDVWS2k1dVYxTkpPbmg0TlhrcFZ1U2JtNCs5R2xDYmphVW90ZlQxNyt6eCt5dDhUZjJkbC9ZaXNmQVd1Nlg4SnZIM3duMUhXRTArRHd4NGkwdTYwYTFOM29VaStJTmY4QUUycmFXVjB2NHRUYXo0aTA3eFhIWitMSm04V2ExZVJhbHJHb2FkZldsdHFSSHRuN1BmN01Yd2cvWnM4RmVGL0Nudzc4R2VGdFAxUFFQREZ2NGExRHhwWitFL0RXaCtLUEUwZjIrNjFyVTd2Vjd2UXRNc0VWTlg4UjZocWZpQ2ZTck5MZlI3VFVkUnVHc0xHMWoyUnIxZndhK0VlbWZCM3dwTG9OdjRpOFVlTmRkMWpVbjhRK05QSDNqaSt0TlM4WStPUEZNOWhwK2wzR3YrSUxyVDdMVE5NVzRHbWFUcFdrMlZqcE9tNmRwV2w2VHBlbjZkcDlsQmJXcUxYclZlUmk4eXhVNFY4SFR4K01yNEdwaXA0cWNhdFdweTRuRlRTalV4VTZjbmZucXhqQlNjN3lrb1FsTmN5U2owMGNQVFRoV2xScFJyS25HQ2NZcTlPbXRWVFVrbGRSYmJ1bHB6TkxUVm96S2lzekVLcWdzek1RRkFBeVNTZUFBT1NlMWZ6cmY4Rk92K0NrTi9IZGF2OEFBdjREZUs3M1FFMGE0OHZ4ejhSL0QrcTNlbDZoSGUyNGpsT2grRzlYMDI2Z25nOGg5eWFuZXd5QmpJcldzVEFDVTE5amY4RlRQMnluK0FIdzNqK0dYZ2pVbHQvaWY4UnJLNGlXNWdrak0vaHZ3dS9tMjEvcXpLZHpSM04weXZaNmVTcWxYTXM2dCs1citLdjR1L0VXYTZubjBld3VYZFRJN1hjNWZkSlBOSWR6eVNPY3M3c3hZc3hKTEVrbk9hL0RmRWJqS1dYd25rdVhWSEhFU2l2cnVJcHl0T2xHVm5IRDA1SnB4bkplOVZrbW5HTFVWWnQyL3dCUnZvSi9SVW84YllqQytLM0hHWHd4T1RZZkVTWENlVVkyaXFtRngxYkR6NUsyZDQyalVUaFh3bENwR1ZIQVVLaWRPdlhqVXJ6aktGS2x6ZTg2eisyZiswTEZlWEFqL2FzK1BLb2p2eEg4WHZIZ1VZWWo3cTY3eDB4MHh4NlY1TnJ2N2ZuN1Q3MzF0b3ZocjlwVDlvclY5WXY1NDdPeHRiVDR0ZkVLYWU1dVozRWNVVVVFZXZGNUhaM1ZSOG9HU0RuQU5mRUhpUFdib1N3Nlpwa1U5N3F1b1RSMnRyYTI4YlRYTnpjenY1Y2NVVWNlWGtlUmpzUlZYcVFRY1lOZjBxZjhFci8rQ1h1bitEOVBYNDZmSFd5dGYrRW1qMDV0Y2xHcXFSWStDZEhoWDdYS0dFeEVJMUlRUitaYzNEcis0NWpqWmNNVCtZOE40TGlEaVRHZXlwWmpqYUdFcDJsaXNTOFZpT1NqRFJ0WGRWSnphVGFqcGRKeWRrbmIrL2ZwQThiZURQZ0R3NURGNHJnamhMT09KTWRmQzhQNUJEaDNKSGlNeHhyNUl4YmhEQXVjTU5UcVRnNnRTemJjbzBvUmxVbEZQM1QvQUlKbi9CTDlyYnhKNG04T2ZGTDlvNzlwRDlwRFVWamVIVk5JK0hDL0YzeHhjNkdxU3dTR0pmRnR2ZWF2UEhxREVTSTRzRkhrUnN1SmhMZ0FmMEZmdEJmc3MvQ3o5cXI0WitJdkEzeENzTlEwUy84QUV1aDZkb1krSXZnMyt5dEYrSitnNmZwdmliUS9HRnRiK0h2R04xcEdwM3VseC84QUNRK0hOSDFLU0pJNVlqZDJOdmV4SkhmVzFwZFFmaVQ0cy80TFJmQXo5bmo0cWFENEswZjRSWHVzZkM0Nm9kSDFYNGhSYXJEYjM2eFF5L1pXMWpUdEphM2RibXdSMldZckplMjhyMnhhUlVMaFViK2pMd1g0dThQK092REdoK0xQQzk3RHFHaGVJTkxzdFgweTdnWU5IUFpYOENYTnRLckFuNzBjaWtnbklKSVBJcitodUNjeXkzQktWTGgzTnE5WEdaWFhwVHJZbjIxZVdKamlJTlNoV1ZXcS9maTVSOTEwNzB0TEpkLzhWdnBKWkQ0czFzMnlqaTd4VDROdy9DdUM0dXdkYXZ3N2djRGdNcndHVjBjREdTbExCVThIbGlVY0ppS01hc0pWYVdNaXNaSlRWU3BlN3QrTTF4QjhNZjJYZmdKOGN2aGIrM0RheitKL0IzeEU4ZGFYOEt2Zzkrekw0VjB3ZUkvQzEvOEFDVFJwdEwwSHdIWi9zMytFTGRycnh4NHE4VnBwR3QyWGl2NGo2MVBIQjRuZytJMW5jdmJlU3RocEd0NnQ3cCt6TDhWUEhQN05QeFg4TWZzV2ZIbnhQclBqYndaNDUwTzY4US9zWS9IdnhWNThldmVOL0JtbTJjVjFjZkE3NHJYZCtsck8zeG84QjZXUHRXbmFsUGEya25qandta2R6TEJINGkwclY0WmZ1ZjQzL0NhMytLWGhEVUJvNTBuUlBpcG9HZ2VOQjhIL0FJa1htbDIrb2FyOE12R3ZpandocS9oU0x4Um9jc3NVczFyTWxwcXNzRjZzSC9IMVpzOFRwSmhBUHdxOE5mc3hhNzR0OEthOThLUGp2OFJQRnZ3UCtKZmlpLzAvd24reWZwUHhSK05lbGZGYjRuMi83Ukh3Y3V2R1h4QjhML0ZyUmRabmZYL0VWbDRha25PcTZ2NGUwbC9GR2xHN3R2RjNqdlFiM3d5bmgzWHZCSGgzdy84QXRlQnJZTFBjQmpYamF5cFZLbFIxY2ZSVnFzNFYzQ0ZPaG1lVzRXbFRoT2pUd2RDaktwbUw1c1JMRlVmckt4VXFMaGhhNS9LRmFGYkExNktwUjVvcFJqUm0yNEtVTHR6dzlhbzIxT2RXYnRSVm9xbkwyZklwZS9GLzBlVVY4bC9zUy90RTMzN1RIN1AzaGp4MTRvMHVQdzE4VXRCdjlkK0hIeHM4RmpDWEhnejR2L0Q3VkxqdzE0MzBXYTMrOUJhM09vMkkxN1FpNEF1L0Rlc2FQZlI1aXVWTmZXbGZCWXZDMXNGaWNSaE1SRlJyWWF0VW8xVW56UjU2Y25GdU10cFFsYm1oSmFTaTFKYU81N2RLcEN0VHAxWU84S2tJeWo2TlhzMTBhMmE2Tk5INXMvR1ZSOGMvK0NnWDdPL3dVbHhQNE8vWnE4RDZ6KzFyNDJ0eVBNdDdyeDVxTjlQOE0vZ25wMTdDK1l4SmFUWG5qdnhmcDBySzdSWFhob1NxRW5qdFpsK2wvQ243STM3Ti9nbjRwMjN4eThML0FBajhKNlY4WklOUDhWYVhQOFQ3ZTFtWHhyck5uNDAxZVhYZkVVZmlYWEJPTHJ4UkplYXBQY1hGdmMrSVcxSzYwdExpNXR0S21zcmE2dUlaUG1mOWtrbnhmKzJqL3dBRkh2aVhPQzdhWjhRdmd2OEFBL1NuT0NMZlRQaHQ4S2RQMXUvdEZQVWg5ZDhiMzk4eThCWHV5Tm96ay9wUFh0NXppTVJnNTRYTGFGYXRRbzRiS01CUnJVcWRTZE9OV3BqTU9zeHhhcktEaXFzWllqSFZZZS96SjBvd2k5SXBMa3drSVZZMU1ST0VaenFZbXRVaktVVTNGVTUreHBjcmF2RnhwMG9iZmE1dGRXRllmaWJ4QnB2aFB3OXJYaWJXYmhiWFN0QjB5OTFYVUxsODdZYlN4dDN1SjNPQVQ4c2NiRUFBa25nY2tWdVYrWWYvQUFWdStMMDN3dC9aQjhXNmRwOTE5bTFqNGozK24rQ2JNcklVbE5uZnpyTnJEUkVNR0JYVG9abEpYT1BNNXdEbXZqYzB4c010eTdHNCtkdVhDWWFyV3M5cFNoRnVFZjhBdDZmTEg1bjZENGVjSlluanpqbmhQZzNDY3lyY1I1OWx1VmM4VmQwcU9LeE1JWW12YmI5eGh2YTFuZlMwTldrZnlwL3R1L3RMNno4YVBpbDhRZmlycWwzSS93RGJtcVhlbStGN1ozY3g2ZDRYc3JtNGgwYTBnUitZMSt6RVhFcUFLRGNYRXJIT1RYNUxhOXF6UnhYVi9jT1M3QjIzTnlTY0gxeitQWEErZ3IzRDR2YTAxenFVR21vNThxMmpHNFp5TnhMWjYvamdlbWNZeFh6N0g0ZjFQeDU0djhNZUFkRmphYlV2RSt0YWRvdHRIR3U1ak5mM01VR1FBTnhDQ1F1Y2pJQ2s0OVA0OHgySXhHYlpuT3BPVXExZkZZaHR2NG5PclZtcjJTYjNrK1ZMcG9rbHNmOEFVYnd4bE9SK0duaC9oY1BoS1ZITHNweURKYWRHakZLTUtlR3kvTGNLa205RWx5MGFVcWxTVGZ2U2NwU2NtMjMrcFA4QXdTSS9ZMm0rT3Z4SWwrTm5qSFJaTlEwRHc5cUxhYjRLczd1Slh0THpWd0FiblZIamt5SkYwK045dHNTb1VUdVhCT3dWL1ViL0FNRkdyaTUvWjMvNEo4L0VTODhQTExaM09xTG9maGpWTHExVXJNbW1hOWZKWjZpQzhYektrdHU3UXUzWldPVDJyNVMrQlh4Ly9aWC9BT0NjWGhUd1Q4SGZIR2tlTnJ6eEg0ZThGZUg3Ni9QaFB3OVphdGFXOCtwV0VVN3ZkeXphcFpUaSt1SmQ5eklwaEpXT1NMTGs4SDBqNDBmOEZYUDJBUDJrdmhONDArRUhqblJQaTNONFk4WWFOYzZYZUxMNFBzTGE0dFdraVlXOTlheXZycmlLN3NwaWx4YnlZTzJSQWNFWkIvZmNDc2h5UGgzR1pGRE9NQmhjMXE0T3ZTclNxVlZHcEhHMUtUVWxOcGFjczJxYTF2R0tWdGQvOFZlSjRlTTNpMzQ3Y0wrTWVOOEwrTStJdkR2QThWWk5tbVZVc0hsMDhSaHNSd3BnTXhwVmFEd2RPYytTVHhPSGc4WHFrcTlhbzIvZDViZnhYL0h6NGdTK01kUTBuVE5MTWx5NVNPenRJSTBZeVRYVjFOR3FxcTRCTE0rMVY2Y25uMUgraFYvd1RIWHhMcHY3TFB3cDhPZUtwSjVOVzBQd1JvZG5jaWNreVJ5SmFSTjVMWko1Z1ZoRWVlQ3VDT0svbEMvWkcrQm43RUh4RS9iQzBid1Q0QzFmNHAvRUx4R3Mrc2FwNFZ0L0YvaGpSdE84TzZaYmFOYnoza3R4cVV0bnF0M05jWE52Q29FRWd0ZkthZFVKalRPUi9icjhHL0FrSGdid3ZaNmZDcXFSQWdiYU1LZUZ3QU1EQUczMHJtOEw4bHFZT0dOekdwaXFHSW5pWktnL3ExV05hbkZVV3BTNXB4WEs1dHlpK1ZOMlRWM2R0SHQvdEN2RmpEY1ZacHd0d05oT0g4NXlYRDhQMEpadEQvV0RMNVpiajZ6ektuR25UZExDVlc2dE9qQ0ZHb3BWS2lnNnRTL0xIbGdwUzlncjV3dWYyU1AyZGIvNDY2cCswbHEvd284SDY3OFk5UzBud3BwVVhqSFg5RjA3V3I3UWo0T3ViNjUwdlZmREQ2bGJYTCtHOWN1VGRXY09yYXRvNzJsMXFjR2dlSGt1WFp0SmdjL1I5RmZzbEhFWWpEKzA5aFdxMGZiVW5ScSt5cVRwKzBveWxHVXFVM0JybXB5Y0l1VUhlTW5GWFdoL21iS0VKOHZQQ00rV1NsSG1pcGNzbGRLU3VuWnE3czFxajh2Zmg5SC9BTUtCL3dDQ252eGUrSDBRRmw0RC9iVStEK2svSHJ3M1pJQkZwOXQ4YVBneEpwbncrK0o2V051bUkxdS9GdmdyVS9CZmlUVm5WRU11b2FKZDMwMGsxMXFrcEg2aFYrWkg3ZHFEd3ArMFgvd1RTK0xkdU5sMW92N1ZPcWZDRFVKUUFyUDRiK1BId3c4VWVHWjdQZUFHQ1MrSzlHOEdYQlFuWS8yVGxTd1FyK20yUjcva2Y4SzlmT2YzMkh5VEh1M1BpOHFoUnJPOTI2dVc0aXZsc1pTZldVc0poc0xKdTJyZXJsTG1aeDRQM0o0eWd2aHBZbVVvTG9vMTRRcjJTNkpUcVQ2djVLeCthZjhBd1Q4bkVYeFEvd0NDa09qM0ROL2FWciszYjR3MWFXTnlDNjZicm53cCtFNzZSSm5yNWNzVmpjZVVDT0VRYzVOZnBiWDVkL3M3emY4QUN2UCtDbUg3ZXZ3dXVqOW50dmkzNEUvWjcvYVg4S1FNZmx1b0lmRDkvd0RDTHgxSmJIT0NiSHhCNFgwaTQxQVlESTJ1MkJZbEpFeCtqK2crTXZDWGltNzF1eDhOZUp0QThRWGZoblVuMGZ4RmJhTnJGaHFkeG9XcnhvSkpOTDFlQ3lubWswNi9SR0RQYVhpd3pxcHlZeGlqaVNTZWFScXRwTEY1ZmxHSm9wdFhsQ3BsT0Rsb3VyZythTTByOHNveVRkMHpYTEtGYVdEcXloU3FUcDRTcldqaUtrS2M1UW8zeFZTbkIxcHBPTk5WSnRSZzV1S2xLU2pIVnBIU24yL3oraC9sWDg0UC9CZmp4b1lJUDJlZkE2ek1xejNmakx4UE5EdXdyaTFnMHJUWW5aZjR0cHVuQ0U4QWxzQUhtdjZQZWUzNS9qN2crLzhBazVyK1YvOEE0T0RoYzIzeFYvWnl1MjNDMG44RitOclZXSkd3WEVXcjZQSXkvd0IzYzBjcUU5TWhldkhQNVo0aDFKVStFczBjSGJtZUVoSzM4azhaUWpKUHljWC9BRXJuOWY4QTBHOERoOHcrazE0ZVVzUkdNbzBZOFNZdWtwSk5mV01Od3htOVdpMWZhU21rMDkwMXBycWZ5LzhBak83YTYxL1VaU2M3WlhVRTRKQVhJeHdTT01kT3h5SytpLzhBZ21ONERIeEkvYmc4QUxjV3EzVmw0VGU2OFVUTElwZU5KZFBqMjJwWVo0M1NPQUMzeTd0cElKMjE4d2VJYy8ybnFaSTZ6VG44Q1dJLyt0WDZiLzhBQkNuU0l0VS9hOThhVFNxQzlsNE10VEVyY25FK3NSUlAyUEJYcjBPT005YS9ubmd6RHd4UEUrVjBxbXErdHhxTk8xcjByMUZwMWQ0K255My9BTnUvcFo1emlPSHZvOWNlNHJCeWxUcXZoeXBnb3lpMm5HR09uUXdOV3pUVC9oVjVyU3pzM2Zxajc3L2FyLzRKaGZ0bC9GajQyZU5maWZwZnhNOEcyK2orTXRXRnhvV2pMRnFyTnBlaFJwSGJhWll5N3Jab2c4RnNpSzZvU203Y1FjWXI4TFBIbi9DWitBZFI4WCtHZFYxS3cxRzU4TWFycUdnWEdwMlVSU0M2dWJHZVMwbmtneXFOdDgyT1JSdVVFWXllcE5mNlFIaXR0SThNZkRuWFBFdC9IQkhENGY4QUMybzZtMDBpcmlNV2VuU1RCamxUdCthTUhPYzg5YzhWL25HL0h6V2Y3UnM5ZTErVkVqdS9FMnY2cHJFNnFmdXlhamRYTjY0endTQThweGs4Z0RtdnRmRXZJY3N5ZVdEcjRPTlpZek1hdUt4R0psT3ZVcWM2VHBYdEdVclI1cWxXNmFpdmg1Vm9qK1Vmb0FlTW5pRjRuME9LY240cXJaWlg0WDRIeXZodkplSDhMaE1vd1dBZENwT09MUzVxK0hwUW5XZExCWmZHTFZTY25lcXB5MWtqN0cvNEliYU5mNi8rMko0ajhXS3JNM2hud3RMREZjRlNjVGExY05aeVJxL3pZWjdjeU13UDhLODR6WDk5bWhxeTZYWmgvdm1GTjMxd0IrbU1mL1hyK01QL0FJTjNQQWpYdXIvRlR4bk5BcFc5OFNhUnBkdE1WQlBsV1ZsZFRUSXBPY0w1c2lad2NaQTlTYS90S3RVOHUzZ1FEaFkxSDA0L3AwcjlMOE9NSzhOd3RnVzFaMTNWcnZUViswcU96ZjhBMjdGSCtmbjA1ZUl2OVlQcEM4WHRWSFVobGYxREthZXQrVllQQTBGT0s3SlZxbFYyMjVuS3hZb29yeno0aS9GbjRhZkNMVGRMMWo0bitPUERQZ1BTTmExcTE4T2FYcW5pclZyUFJkUHU5YnZZTG01dGRPanZMNldHM1c0bXQ3TzZtVVBJaWlPQ1JtWUJhKzZuT0VJdWM1UmhDT3NwVGtveFMydTVOcExYVFZuOGk0ZkRZakdWNmVHd2xDdGljUldseTBxR0hwVHJWcXNyTjh0T2xUaktjNVdUZG94YnNtN2FId24vQU1GS01UUWZzUDJFUkJ2YnYvZ29mK3lkTmFSZnh5eDZWNCtpMWZVeWhJNEVPbFdON2NTY2pNVVRqdmcvcGZYNWkvdFlYVVB4SS9iWC93Q0NjbndrMDZhSFVMUFFQR254Vy9hYjhSTGJ5Q1dLUFIvaHg4T2I3d3A0UnZaR1FtT1MxdWZFL3dBUUlwcldRRmtOM3A4REljbGMvcHprK2gvVC9Hdm9NMGlxZVY4T1UyLzNrOEJqTVZLT3Z1d3I1cGpJVWIzdDhjS0h0RmJSeG5GcHU1NW1HdThUbUVyTkpWNlZPNzZ5cDRlbHorZnV5bHl0UFpwN081K1VmN2ZNci9zOWZ0QmZzZy90MFc2UGIrRS9CbmpDOS9adC9hRzFDSlQ1T21mQlA0OVhlbTJPbCtMOVljWVdQUlBBSHhOMDN3eHJHclRPUXR2WVgxeGVmTzFrc1VuSy9zN2ZEclNQMldmMnVOWDhNZUsvR1B3VThCd2ZGcTU4YW40VmFacE9xWEgvQUFzdjQvYUhyR3QzUGpSZGE4Y1JycGxscHJhcjRNMUxVWmRJOFBhbHFHcjZ6cTJxaTkxMnkwcjdCcDAxbnA3ZnAvOEFHSDRWZUR2amw4Sy9pRDhIZmlEcGtlcitDdmlWNFIxM3diNGtzSkFOMG1tYTlwODloTk5iU2ZldDc2ek15M21uWGtSU2V5dnJlM3U3ZVNPZUdOMS9ETDRYK0hmRVBpU0hWZjJhL2pMNGIxajRnL3R2ZnNCNmZwdHY4S3JaZkYxbDRBbi9BR3FmZ0ZENG8wVFZmaEQ4UWg0dXZvOXFhZlkzWGhyUnJUNGgyMXRkRzd0dGEwWFVyRFVUbnhLQzNEbW1Hbm0rUllMSFlhQ3FacHdvNXdxMHZmYzYyUjRtdjdYMjBZMDR5cVRsZzhSVnEwYW5JcFNqR3RndmRsU2hVaWZjOERaelF5M0g1enczbW1LcVlUSWVOc0pIQ1Y2MUpZVytIempDMDZ2OWwxWjFNYlZvNFNoUWRlcCsvcVlpcENuSEQxTVhOVmNOVlZQRlVQNkZQVHFNbi9INi9YL09LL25GL3dDRGlMd1RkM0h3dCtCSHhMdFlDOEhoZnhwcldoYXJPRlA3bTE4UWFmYU5hNzJDa0FOZDJJVUJtR1NjQUh0K3VQN0gzeDgxcjR4K0d0YzBueFY0ZzhPK08vR2ZnalY5UzBmeHY0MytIbWpYZWwvQ3lMeFdiK1c2dS9BUGhIVWRVdlpyenhYUDRGc0x6VHRIMWp4TlpRTHB1bzM4VTBqTFkzaGwwK0xpditDblh3R2I5b2Y5akg0eGVDYksxRjNyOWhvTGVLL0RLQlN6L3dCdCtHWFhWTFpZOEVOdWxTQ2FJaFQ4d2NxYzV4WHcvRXVHV2VjTFpuUnc2Y3BWOEZLclFpN09YdHFFbzE0UWZLNVJjdWVseU96a3IzU2sxcWZyWGdEbjlUd2gra1I0ZTVyblU0VWFHVWNWWVhBWnBXWFBDai9adWNRcVpWaU1TdmIwNk5SVUhoTWU4UkYxYVZLYXBwU25DRHVsL25vK0pFemZ6U0xnZmFFTWluSVAzeG4rby9LdjBlLzRJZCtLN0x3dCszSGNhSmVnYi9HSGhDOHNiTWxnb0Z4cDl6RGZqcXdCTEtyQUQ1bXkzQUJ6WDVvYW5xY0NLTGE4Wm9MMnllUzF1SXBRVmRKSUhaSkVjSEJWMFpTR1VqSVlFRTlLOUQvWk8rTGtId1IvYXkrQ254TVc4RVduYVg0MzBpMjFkbGZDblNkU3VFc2IwU0huRWF4ekNSL1FKazQ1citZdUdNV3N1NGh5ekZWUGRqVHhsS05SdFc1WTFKS25POTd0T1BOZHEvUnJxZjhBUVI5STdoZVhIUGdoeDNrR0NsSEVZckY4Tlk2cGdZVTVweXIxOExSampzS3FmTGUvdHA0ZUVJOUc1cnBxdjlBei9nb1Y0OC80Vi84QXNTL0dQV29waERjMzNnLyt3TEZ5d1VtNjE2ZTMwNk1MbGxKY2k0WUtGSlBQRmY1ODN4L3Z4RFpXVm1HSUVjRWtoVUU5U3BBQlBKeWNuZ2tlL2F2N0gvOEFnczU4WXRHc1AyTlBoMW82NmhHdHI4U2ZGZmgyOWh1VWsvZHk2ZHBGaWRiV1Q1VDg4Y2ptMklBSXlUeURqRmZ4SS9HL3hUcCtzYWpNYks1V2FFSWtFWkc0YmowT01qT0dKeDBHUU00d1JYM1hpcmpWaU04d3VFaEpTV0d3T0hTU2FmdlZweXF0OWJXaTZiZnkwUDQrL1p4Y0x6eUh3YTRqNGt4TkNWS1dmY1Y1eE5WSndjRzZXVTRUQzViVGh6TldiaFhqaTNiVGxmTnAxUDYzUCtEZXY0ZmpTZjJlN0R4QTBiZVo0bDhSYXhyRHV5bkpqMy9ab0NDZXFsSTJVRUFkTUROZjA5QVlBSHA3WXI4Wi93RGdqZDhQeDRNL1pXK0UxbTF0OW5sSGc3U3JxZGRoUXRMZnd0ZXM3RHB1WkxoTTV5VDE3bXYyWnp4azhmNTk4ViszOE40YjZwa2VXMEdyT25nOE9tdjd5cFI1di9KbS9PKzc3ZjVEK04yZXZpVHhXNDh6bm5kU09ONG56aXJUazJwWHBmWGEwS05tbTAxN0tNRXZKYkNFNEJQb0QvS3Z3LzhBMnNQaVArMGo0cS9haThKL0ExZmhmNE0rTG53TDhTZU0vQnNtbytIZkdYd2d2ZmlGOExkUThINjFxWjhPK0oyWDRzd2FQYmFiNE8rSkhnS1B3OXFIaU5QRDJwTGZYalA0c3Uwa25rMFBRWWRTcjdnL2JPL2FLOEsvREh3NXAzd3owNzQxSjhHL2kvOEFFYTYwM1R2QW5pdFBCY3Z4QjA3d3JxRStzNlpaNlZxSGpyUllJWjR0SjhJZUl0WXVySHdqTnF1b05acDUrczRzYnFLNWhNOXY4TmVNckx4bDhBUGgzQit6L3dEQ2ZRZkR2aGo5dlg5dkRWN3VYeFJvWGdIeGI0cDhUZkRiNGIyamZiTlArSlg3UnVtYVJyVFJEd2Y0ZDAzUjVwOWZ1Yk95aDA4YXA0enY3SFJiZS91cnFHMWxIbzBzdnI4UjVuaDhsd2RlV0hqQ3BIRVpqallWSXFqaE1MUmk2dGQ0cGU5YWxUb1hyMW8xZVNMcEs4UGJTVTZTdzRheFdINEN5YXZ4cm5HVjRQTWErYVlYRTVad3psR1pZUEV4cVlpdFdsR2s4N3dPS2s4UEdFY05VVTZPSHhlWFN4bUlwWW1FcWRiK3puWHdlTHFmUVA3SHBYNCsvdFpmdFZmdGZRSWsvd0FQdEIvc2o5a2o0QVhhNGUxdXZESHd2djVkUytNZmlYU0pZeWJlZlQvRW54U2VIUTB1TGZjb0hnSmJVc3NzTnlwL1VXdkp2Z1Q4R2ZCMzdQWHdmK0h2d1Y4QTJ6VzNoUDRkZUdySHc5cGhsQy9hcjZTQU5OcWVzNmk2OFRhcnIyclQzMnQ2dGNITFhPcDZoZDNEbG1rSlByTmZRWjFqYVdPekNyVXcwWlF3VkNGSEJaZlRscEtPQXdWS0dHd3JtdEVxdFNsVFZiRU5KYzJJcVZaMjk0L0tjTFNuU29wVlh6VnFrcFZxOHQrYXZXazZsVnB1N2FVNU9NZjdrWXJvRmZDWDdhZjdJV3AvSHkxOEdmRnI0TWVLb2ZoUisxdjhDYmk5MXY0Ri9GWXd2SnB6dGVvc2V2OEF3MitJdHBiSjlxOFJmRER4ellyTHBldmFQNWl5V00wOE9zMkdiaTJrdDd2N3Rvcmx3T054R1hZcW5pOExOUnEwM0pXbEZUcFZhYzR1RldqV3BTVGhWb1ZxYmxTclVacHdxVTV5akpOTXV0UnAxNmNxVlZOeGxiVlBsbEdTYWNaeGtyT000eVNsR1NzMDBtajhkdjJRdkZ2d3MvYUsrTjF4cm54QWorSWY3UFg3WTM3UG1pZjhJOThRdjJUWS9FOXY0YzhEK0ZIdTlTbTFEeFA4UmZBZmgzU2JPMXRmaUg0QStLbDdmV04zUDRzbXU5YXRaNDdiU29wWTlMMWJ6THErK3QvaDMrMWhvSHhlK0xQeFU4RmFScDJtRDRQZkR1VzM4RjNmeGExTFZkT3RQRC9pYjRuWGtPbnpYL2dMUkZ2cjIxbnY3L1JyVytsajFRV3RoZVdndWd0bjl1anZFbHN6Sisxait4TDhNdjJwWS9Ebml5ZlUvRUh3cStQUHcza2UrK0VYN1FudzN1aG8vd0FTUGgvcUlFeFMyRjJtTGJ4TjRTdkpaNURyWGd6eEZIZTZIcWNVa2hNRnZkK1ZkeGZrWCswYlovRmZ3ZDRjc3ZoNy93QUZFdmhOcjkxNGEwSFdkZDF6d3orMzUreUg4UExmeFo0T2wxanhCNFl1dkJkLzR3L2FFK0JwMExWcm53WDRqT2dYbHVxK0o0ZE4xclI5TzFxMWd1ZkQycTZUSlpXY3R6MTR2SmFlYXhlTDRUaGg2V01sVWxpY1p3eldxeHBWOFJXY1ZGd3lyRTEyNFl6RFMrS0dHYldZVStTbmgxR3RTaExFeityeUxQOEFMOFJpVmd2RURFNWhVd3F3ZUd5cktlSmFVSjRxSER1RnA0bU5lV0txNWJoM1JxVnE2dEtrcCsxbFFnc1ZqTVpLaGlNWEtsQmVHL3RHZjhFR2ZoRjhSL0gzaWI0bmZEYjRvK01MZnc3NC93QmF2L0ZGblllSEkvRE9wK0hyUWF4ZHkzY3NXaVgwRURyY2FmNTBraHQzRXNxaFNVM0VLQ1BuQmY4QWczcjBScm1HVC9oYVh4TlV4T3JLeTZaNGZ5cktRUXl0OW1BREt3eU1jWjdnOVAyUStCSHhGK0tZMU81MXo5ay80aS9BNzlvRDlqejRmL0IzeExwL3cxK0cvd0FLZkUraStJZkZjdC80UDhGK0c3RDRjZUV0ZTBxOFcyOFYrSHZpQnFuaWlUVzdyeFhjWEdxdHByNlpEYnhhaHBkdDRpdmZOVDZLdXYydnZpTjhPZkdYd1IrRi93QVlmMmVyNGVOUGlmcFhoUzk4UTY3NEoxTHlmQXZoM1VQRmZpS3gwQmRCMGpVZkZrR21qeEw0ZzhNTGZEVlBGK2hXZC9IcWRscDhEemFMYjY4WmJkSmZ5eXZ3bHc1UXIxbzVwdzdVeTNGeHJTalhwNG5DWWlINzZkU01YS0RWMm8xS2tuS0hOR25KUmkzS01GcS82b3BlUG4waDQ0VENZTGhieGhseE5sVlBMS1ZYQjA4TG5XVnJHNGJMc1BnNVZ2cXVQd3VQbzBLa2NYZ01IU3B4eHNhYzhUUzl0VWhSbzRqRVRrMHZpZjQ3ZjhFdXJuOXBmNENmQkQ0YmVQOEE0eS9FeUEvQXp3eko0ZjBtYXlzdENlWHhHelJXOEZ2cXV0cGMyY2dHb1cxbmJKWm9iVm9vakRrc3JPU2EvTUc3L3dDRGVyUUxqVUk1VytKM3hLbWlpdW81QWttbStId0pWU1ZYS3VmczJRR1VZWWdjQStvcitoZlJQK0Nnbmc3eG5CYlA0VStIM2k3U1RaZnRMK0EvMmY4QVg0dkVXazJHb0dTTHhvK3RMYmVKTk11TkI4U3ZZUmFkTEZwSzNhWHozbW9TV2xwY1c4dHpvOHh1WTFURS9hOCtPbjdXUHd6K1BIdzQ4RC9BVDRNemZFRHdWcTNoclR2R0d2M3RwNEo4VDY1L2FrK2wrUGRCMHp4SjRDSGl2VDdhWHd2NE4xclcvQjk5cU4xNGIxVHhUZWFWcFZyZDJrdDdmM2pXMXNiVzUweGVSOEo0dm16R3BsOGNiVWk4UFJsVXAwcTFhcTdKVWFOb3FYdktLcHFMc3RMV2V0MC9KNFo4VnZwSThPeG84RFlMalhFY0tZR3JETzh6b1pkajh4eWpMY3VwdWMvN1R6U1h0ZlpTcFFxNHFlTytzd1RtbFVWWk9EakNONC9TMzdLdnd1LzRWRjhNOUE4TFRreFFhQm8ybTZWRk5Oc2pKdHRMc1lyT09TVWhValVtT0ZXY2pDZzU0QXJ5cjRpL3Q5L0M3Ui9qTHJYN0xYaCs5dk5IK1BWN1ozRnA0TkhpelI1TGZ3cHErc2FyNGJzOVg4RzNHbDNhWHNKMTZ5OFMzMStkTjB2eUo3R0dhNjBYeEFidTdzclhUbG11dm5QNDRXM3h0dTlWK1BscisxbDhldmhWOERmMlAvRW5oYldORjhNNmRyM2piUnZDdml5MjFDUFZ2RC9pRHdacitsNm40WGc4TytKSklrZTIxUHc1NHIwQzk4WVN6YTFGNWR0WTJPb1d0L0t0ZUwvcy93RGpUNHRlT2ZDZmc3d1grdzE4SzI4WGVKZkQzZ2ViNGE2dC93QUZFLzJodkJlcytEdkFrUGdrK0liM1dJZEorRnVpNnpCTjQwK0xscG9OemNRUDRmc2JQN0o0TUZ4cDBFTjlxVm9wbEZ0OXRsMlRaOW0wSVBCNFQreHNub1M1TVRuT2JwWWJDUnAwcHlwVHBVWnVjVzZsU21vMXNOS2k4UmlhaVRqSENPWExmOFJ4YjRLeUg2N21mRVdjME9NK0k4ZFJwNGpBWkZ3MWlLdjFmQzQzSDRQRDVoaDhibWVZWW5CdWxpNDRIRmZXTXR6bko0VU1QRlZaVTZsRE5LbFBuaXR1NThXZUovZ0ZhZkQ3NGsvdFcrR05MK09QL0JRZnhWZitNTkEvWmcrRG5ncE5QYjRuM1BoN3hVdGpPL2cvNGxYM2cvVXYrRU0xcndsNFExT0dmVzV2RmQ5YkR3MzRQMDFaYml4dnB0Umd1TCt2dmI5a1Q5bHZ4UDhBQy9VZkdQeDYrUDhBNGlzZmlIKzFmOFo0YktUNGhlS0xHTmo0YStIM2htMjIzR2pmQmo0VngzRVMzVmg0QjhMVHRKTE5jems2ajRwMXg3ald0U1pJUnBlbmFiMFA3TWY3R25nZjluZlVQRVh4RDFqeEQ0aCtNbjdRM3hCZ3QwK0p2eDkrSWNxWG5qRHhHc0ROSkZvbWdXTVIvc253SjRLc3BISTAvd0FKZUY3ZXpzZHNjTStxUzZwcUNHOWI3RXIyNVZzdnlqTDVaSmtNcXRhbFdVUDdWenJFUjVjYm5FNmZJNDAwbmVkSEFRblRqTlFuTDZ4aTV3cDFzVjdOUW9ZWERmQlo1bldaOFZadFBPczRqaGNNMDZpeTNKc3VwdWhrK1I0YXBWcVZsaE10d2lsS25oNk1KMXFyaFNwKzVUZFNvNHVkU2RXdFVLS0tLOGM0Z29vb29BS1pKSEhMRzhVcUpKRklqUnlSeUtIUjBjRldSMVlGV1ZsSkRLUVFRU0NNVVVVYmJBZkFQeGUvNEpnL3NaZkY3eEhjZU9tK0ZuL0NxdmlaY01acFBpaDhCTmYxcjRLK09wN291MG92ZFMxVHdCZDZOYTY1ZXh5dHZqdTlmc05WdUl5RkVjaUtBSzhwai9ZRi9hdThFbFkvZzMvd1ZGL2FPMDNUb3NpMzBqNDdlQnZodCswTGJRSXB6RkVOWTFTMThGK01KMVFFcTczM2llOGxrVGFQTVhZcEJSWHUwZUk4NnBVNFllV09saXFFT1dNS0dZVWNObWRHRVZ0R0ZQTWFPS2hHSzZLTVVsMFNPR3BnTUkzS2FvcW5OdTdsUmxPaEp0MlRiZEdWTnR2cTk5KzdKNGYyYi84QWdxQkVCWS84TisvQWY3SUpqTWI4ZnNWV0M2bEpMaGsvdEY0RStNY2RxTlNZSHpIZFpOcGtKL2VZcTEvd3d4KzFyNHdZcDhYZitDbmZ4N3ZiRnY4QVc2WjhEZmh0OE12Z1JGS3JjU1J0cTBjSGo3eFJDaklXVlRaYS9hU3hIYTZTN2xCb29yMGNWbitZWWRVM2g2ZVU0YVRYTjdURGNQNURoNnFhNVZlTldqbHNLc0hadldFMXV6R09GcFZHdmF6eE5WSnBXcTQzR1ZZNjcrN1VyeWpyWlgwMXRxZWtmRFQvQUlKbGZzaC9EN3hCYStOdGU4QmFuOGNmaU5hU2k1dC9pTCswVjRwMTM0MStLTFM4eDgxN3BTK09MdlU5QzBHOWR0ek5lYURvdW1YVGJpSG5aUW9IMzFEREZieFJ3UVJSd1FRb3NVTU1LTEZGRkdpaFVqampRS2lJaWdLcUtBcXFBQUFCUlJYejJOekhINWxVVlhINDNFNHljVTR3ZUlyVktxcHhidnkwNHprNDA0MzE1YWNZeFhSSGZTb1VhRWVXalNwMG85VkNLamZ6azByeWZtMjJTVVVVVnhHb1VVVVVBZi9aIi8+DQoNCgkJCQkJCQkJCTxoMSBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJPHhzbDp0ZXh0PmUtRkFUVVJBPC94c2w6dGV4dD4NCgkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQk8L2gxPg0KCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQk8dGQgd2lkdGg9IjQwJSIvPg0KCQkJCQkJCTwvdHI+DQoJCQkJCQkJPHRyIHN0eWxlPSJoZWlnaHQ6MTE4cHg7ICIgdmFsaWduPSJ0b3AiPg0KCQkJCQkJCQk8dGQgd2lkdGg9IjQwJSIgYWxpZ249InJpZ2h0IiB2YWxpZ249ImJvdHRvbSI+DQoJCQkJCQkJCQk8dGFibGUgaWQ9ImN1c3RvbWVyUGFydHlUYWJsZSIgYWxpZ249ImxlZnQiIGJvcmRlcj0iMCINCgkJCQkJCQkJCQloZWlnaHQ9IjUwJSI+DQoJCQkJCQkJCQkJPHRib2R5Pg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDo3MXB4OyAiPg0KCQkJCQkJCQkJCQkJPHRkPg0KCQkJCQkJCQkJCQkJPGhyLz4NCgkJCQkJCQkJCQkJCTx0YWJsZSBhbGlnbj0iY2VudGVyIiBib3JkZXI9IjAiPg0KCQkJCQkJCQkJCQkJPHRib2R5Pg0KCQkJCQkJCQkJCQkJPHRyPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nQ3VzdG9tZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBzdHlsZT0id2lkdGg6NDY5cHg7ICIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+U0FZSU48L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCQk8dHI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UGFydHkiPg0KCQkJCQkJCQkJCQkJPHRkIHN0eWxlPSJ3aWR0aDo0NjlweDsgIiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9ImNhYzpQYXJ0eU5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9ImNhYzpQYXJ0eU5hbWUvY2JjOk5hbWUiLz4NCgkJCQkJCQkJCQkJCTxici8+DQoJCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UGVyc29uIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6VGl0bGUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpGaXJzdE5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpNaWRkbGVOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOyA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkZhbWlseU5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpOYW1lU3VmZml4Ij4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTx0cj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8dGQgc3R5bGU9IndpZHRoOjQ2OXB4OyAiIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UG9zdGFsQWRkcmVzcyI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlN0cmVldE5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpCdWlsZGluZ05hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkJ1aWxkaW5nTnVtYmVyIj4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6Um9vbSI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5LYXDEsSBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UG9zdGFsWm9uZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlTdWJkaXZpc2lvbk5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+LyA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkvY2FjOlBhcnR5L2NiYzpXZWJzaXRlVVJJIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+V2ViIFNpdGVzaTogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIuIi8+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5L2NhYzpQYXJ0eS9jYWM6Q29udGFjdC9jYmM6RWxlY3Ryb25pY01haWwiPg0KCQkJCQkJCQkJCQkJPHRyIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx0ZD4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5FLVBvc3RhOiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Ii4iLz4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UGFydHkiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpDb250YWN0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6aWYgdGVzdD0iY2JjOlRlbGVwaG9uZSBvciBjYmM6VGVsZWZheCI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHRkIHN0eWxlPSJ3aWR0aDo0NjlweDsgIiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRlbGVwaG9uZSI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5UZWw6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6VGVsZWZheCI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4gRmF4OiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkvY2FjOlBhcnR5L2NhYzpQYXJ0eVRheFNjaGVtZS9jYWM6VGF4U2NoZW1lL2NiYzpOYW1lIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5WZXJnaSBEYWlyZXNpOiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5L2NhYzpQYXJ0eS9jYWM6UGFydHlUYXhTY2hlbWUvY2FjOlRheFNjaGVtZS9jYmM6TmFtZSINCgkJCQkJCQkJCQkJCS8+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkvY2FjOlBhcnR5L2NhYzpQYXJ0eUlkZW50aWZpY2F0aW9uIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iY2JjOklEL0BzY2hlbWVJRCIvPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PjogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6SUQiLz4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3Rib2R5Pg0KCQkJCQkJCQkJCQkJPC90YWJsZT4NCgkJCQkJCQkJCQkJCTxoci8+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCTwvdGJvZHk+DQoJCQkJCQkJCQk8L3RhYmxlPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIHdpZHRoPSI2MCUiIGFsaWduPSJjZW50ZXIiIHZhbGlnbj0iYm90dG9tIiBjb2xzcGFuPSIyIj4NCgkJCQkJCQkJCTx0YWJsZSBib3JkZXI9IjEiIGhlaWdodD0iMTMiIGlkPSJkZXNwYXRjaFRhYmxlIj4NCgkJCQkJCQkJCQk8dGJvZHk+DQoJCQkJCQkJCQkJCTx0cj4NCgkJCQkJCQkJCQkJCTx0ZCBzdHlsZT0id2lkdGg6MTA1cHg7IiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD7DlnplbGxlxZ90aXJtZSBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBzdHlsZT0id2lkdGg6MTEwcHg7IiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkN1c3RvbWl6YXRpb25JRCI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPHRyIHN0eWxlPSJoZWlnaHQ6MTNweDsgIj4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5TZW5hcnlvOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UHJvZmlsZUlEIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyAiPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkZhdHVyYSBUaXBpOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6SW52b2ljZVR5cGVDb2RlIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyAiPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkZhdHVyYSBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklEIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyAiPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkZhdHVyYSBUYXJpaGk6PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpJc3N1ZURhdGUiPg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9InN1YnN0cmluZyguLDksMikiDQoJCQkJCQkJCQkJCQkvPi08eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sNiwyKSINCgkJCQkJCQkJCQkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiwxLDQpIi8+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpEZXNwYXRjaERvY3VtZW50UmVmZXJlbmNlIj4NCgkJCQkJCQkJCQkJCTx0ciBzdHlsZT0iaGVpZ2h0OjEzcHg7ICI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+xLByc2FsaXllIE5vOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iY2JjOklEIi8+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTx0ciBzdHlsZT0iaGVpZ2h0OjEzcHg7ICI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+xLByc2FsaXllIFRhcmloaTo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklzc3VlRGF0ZSI+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sOSwyKSINCgkJCQkJCQkJCQkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiw2LDIpIg0KCQkJCQkJCQkJCQkJLz4tPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9InN1YnN0cmluZyguLDEsNCkiLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6T3JkZXJSZWZlcmVuY2UiPg0KCQkJCQkJCQkJCQkJPHRyIHN0eWxlPSJoZWlnaHQ6MTNweCI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+U2lwYXJpxZ8gTm86PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpPcmRlclJlZmVyZW5jZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklEIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6T3JkZXJSZWZlcmVuY2UvY2JjOklzc3VlRGF0ZSI+DQoJCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5TaXBhcmnFnyBUYXJpaGk6PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpPcmRlclJlZmVyZW5jZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklzc3VlRGF0ZSI+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sOSwyKSINCgkJCQkJCQkJCQkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiw2LDIpIg0KCQkJCQkJCQkJCQkJLz4tPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9InN1YnN0cmluZyguLDEsNCkiLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJPC90Ym9keT4NCgkJCQkJCQkJCTwvdGFibGU+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJPC90cj4NCgkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQk8dGFibGUgaWQ9ImV0dG5UYWJsZSI+DQoJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyI+DQoJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0IiB2YWxpZ249InRvcCI+DQoJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkVUVE46PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0IiB3aWR0aD0iMjQwcHgiPg0KCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlVVSUQiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQk8L3RhYmxlPg0KCQkJCQkJCTwvdHI+DQoJCQkJCQk8L3Rib2R5Pg0KCQkJCQk8L3RhYmxlPg0KCQkJCQk8ZGl2IGlkPSJsaW5lVGFibGVBbGlnbmVyIj4NCgkJCQkJCTxzcGFuPg0KCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJPC9zcGFuPg0KCQkJCQk8L2Rpdj4NCgkJCQkJPHRhYmxlIGJvcmRlcj0iMSIgaWQ9ImxpbmVUYWJsZSIgd2lkdGg9IjgwMCI+DQoJCQkJCQk8dGJvZHk+DQoJCQkJCQkJPHRyIGlkPSJsaW5lVGFibGVUciI+DQoJCQkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIHN0eWxlPSJ3aWR0aDozJSI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+U8SxcmEgTm88L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBzdHlsZT0id2lkdGg6MjAlIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5NYWwgSGl6bWV0PC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjcuNCUiIGFsaWduPSJjZW50ZXIiPg0KCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7Ij4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TWlrdGFyPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjklIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5CaXJpbSBGaXlhdDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIHN0eWxlPSJ3aWR0aDo3JSIgYWxpZ249ImNlbnRlciI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+xLBza29udG8gT3JhbsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjklIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD7EsHNrb250byBUdXRhcsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjclIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5LRFYgT3JhbsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjEwJSIgYWxpZ249ImNlbnRlciI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+S0RWIFR1dGFyxLE8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBzdHlsZT0id2lkdGg6MTclOyAiIGFsaWduPSJjZW50ZXIiPg0KCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PkRpxJ9lciBWZXJnaWxlcjwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIHN0eWxlPSJ3aWR0aDoxMC42JSIgYWxpZ249ImNlbnRlciI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TWFsIEhpem1ldCBUdXRhcsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQk8L3RyPg0KCQkJCQkJCTx4c2w6aWYgdGVzdD0iY291bnQoLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZSkgJmd0Oz0gMjAiPg0KCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZSI+DQoJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii4iLz4NCgkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJPHhzbDppZiB0ZXN0PSJjb3VudCgvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lKSAmbHQ7IDIwIj4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxXSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzFdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzJdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMl0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbM10iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVszXSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs0XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzRdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzVdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbNV0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbNl0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs2XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs3XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzddIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzhdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbOF0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbOV0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs5XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxMF0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxMF0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTFdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTFdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzEyXSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzEyXSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxM10iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxM10iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTRdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTRdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE1XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE1XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxNl0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxNl0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTddIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTddIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE4XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE4XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxOV0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxOV0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMjBdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMjBdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJPC90Ym9keT4NCgkJCQkJPC90YWJsZT4NCgkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQk8dGFibGUgaWQ9ImJ1ZGdldENvbnRhaW5lclRhYmxlIiB3aWR0aD0iODAwcHgiPg0KCQkJCQk8dHIgaWQ9ImJ1ZGdldENvbnRhaW5lclRyIiBhbGlnbj0icmlnaHQiPg0KCQkJCQkJPHRkIGlkPSJidWRnZXRDb250YWluZXJEdW1teVRkIi8+DQoJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZUJ1ZGdldFRkIiBhbGlnbj0icmlnaHQiIHdpZHRoPSIyMDBweCI+DQoJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCTx4c2w6dGV4dD5NYWwgSGl6bWV0IFRvcGxhbSBUdXRhcsSxPC94c2w6dGV4dD4NCgkJCQkJCQk8L3NwYW4+DQoJCQkJCQk8L3RkPg0KCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVCdWRnZXRUZCIgc3R5bGU9IndpZHRoOjgxcHg7ICIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJc2VsZWN0PSJmb3JtYXQtbnVtYmVyKC8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50LCAnIyMjLiMjMCwwMCcsICdldXJvcGVhbicpIi8+DQoJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6TGluZUV4dGVuc2lvbkFtb3VudC9AY3VycmVuY3lJRCA9ICdUUkwnIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+VEw8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQk8eHNsOmlmDQoJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOkxpbmVFeHRlbnNpb25BbW91bnQvQGN1cnJlbmN5SUQgIT0gJ1RSTCciPg0KCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6TGluZUV4dGVuc2lvbkFtb3VudC9AY3VycmVuY3lJRCINCgkJCQkJCQkJCQkvPg0KCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCTwvdGQ+DQoJCQkJCTwvdHI+DQoJCQkJCTx0ciBpZD0iYnVkZ2V0Q29udGFpbmVyVHIiIGFsaWduPSJyaWdodCI+DQoJCQkJCQk8dGQgaWQ9ImJ1ZGdldENvbnRhaW5lckR1bW15VGQiLz4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIGFsaWduPSJyaWdodCIgd2lkdGg9IjIwMHB4Ij4NCgkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJPHhzbDp0ZXh0PlRvcGxhbSDEsHNrb250bzwveHNsOnRleHQ+DQoJCQkJCQkJPC9zcGFuPg0KCQkJCQkJPC90ZD4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIHN0eWxlPSJ3aWR0aDo4MXB4OyAiIGFsaWduPSJyaWdodCI+DQoJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlcigvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6QWxsb3dhbmNlVG90YWxBbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiLz4NCgkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOkFsbG93YW5jZVRvdGFsQW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6QWxsb3dhbmNlVG90YWxBbW91bnQvQGN1cnJlbmN5SUQgPSAnVFJMJyI+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpBbGxvd2FuY2VUb3RhbEFtb3VudC9AY3VycmVuY3lJRCAhPSAnVFJMJyI+DQoJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpBbGxvd2FuY2VUb3RhbEFtb3VudC9AY3VycmVuY3lJRCINCgkJCQkJCQkJCQkvPg0KCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCTwvdGQ+DQoJCQkJCTwvdHI+DQoJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpUYXhUb3RhbC9jYWM6VGF4U3VidG90YWwiPg0KCQkJCQkJPHRyIGlkPSJidWRnZXRDb250YWluZXJUciIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8dGQgaWQ9ImJ1ZGdldENvbnRhaW5lckR1bW15VGQiLz4NCgkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZUJ1ZGdldFRkIiB3aWR0aD0iMjExcHgiIGFsaWduPSJyaWdodCI+DQoJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJPHhzbDp0ZXh0Pkhlc2FwbGFuYW4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYWM6VGF4Q2F0ZWdvcnkvY2FjOlRheFNjaGVtZS9jYmM6TmFtZSIvPg0KCQkJCQkJCQkJPHhzbDp0ZXh0PiglPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6UGVyY2VudCIvPg0KCQkJCQkJCQkJPHhzbDp0ZXh0Pik8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJPC90ZD4NCgkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZUJ1ZGdldFRkIiBzdHlsZT0id2lkdGg6ODJweDsgIiBhbGlnbj0icmlnaHQiPg0KCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlRheENhdGVnb3J5L2NhYzpUYXhTY2hlbWUiPg0KCQkJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguLi8uLi9jYmM6VGF4QW1vdW50LCAnIyMjLiMjMCwwMCcsICdldXJvcGVhbicpIi8+DQoJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQgPSAnVFJMJyI+DQoJCQkJCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQiLz4NCgkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQk8L3RkPg0KCQkJCQkJPC90cj4NCgkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCTx0ciBpZD0iYnVkZ2V0Q29udGFpbmVyVHIiIGFsaWduPSJyaWdodCI+DQoJCQkJCQk8dGQgaWQ9ImJ1ZGdldENvbnRhaW5lckR1bW15VGQiLz4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIHdpZHRoPSIyMDBweCIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJPHhzbDp0ZXh0PlZlcmdpbGVyIERhaGlsIFRvcGxhbSBUdXRhcjwveHNsOnRleHQ+DQoJCQkJCQkJPC9zcGFuPg0KCQkJCQkJPC90ZD4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIHN0eWxlPSJ3aWR0aDo4MnB4OyAiIGFsaWduPSJyaWdodCI+DQoJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbCI+DQoJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRheEluY2x1c2l2ZUFtb3VudCI+DQoJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLiwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSIvPg0KCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlRheEluY2x1c2l2ZUFtb3VudC9AY3VycmVuY3lJRCI+DQoJCQkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlRheEluY2x1c2l2ZUFtb3VudC9AY3VycmVuY3lJRCA9ICdUUkwnIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCQk8eHNsOmlmDQoJCQkJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6VGF4SW5jbHVzaXZlQW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlRheEluY2x1c2l2ZUFtb3VudC9AY3VycmVuY3lJRCINCgkJCQkJCQkJCQkJCS8+DQoJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJPC90ZD4NCgkJCQkJPC90cj4NCgkJCQkJPHRyIGlkPSJidWRnZXRDb250YWluZXJUciIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCTx0ZCBpZD0iYnVkZ2V0Q29udGFpbmVyRHVtbXlUZCIvPg0KCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVCdWRnZXRUZCIgd2lkdGg9IjIwMHB4IiBhbGlnbj0icmlnaHQiPg0KCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQk8eHNsOnRleHQ+w5ZkZW5lY2VrIFR1dGFyPC94c2w6dGV4dD4NCgkJCQkJCQk8L3NwYW4+DQoJCQkJCQk8L3RkPg0KCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVCdWRnZXRUZCIgc3R5bGU9IndpZHRoOjgycHg7ICIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6TGVnYWxNb25ldGFyeVRvdGFsIj4NCgkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UGF5YWJsZUFtb3VudCI+DQoJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLiwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSIvPg0KCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlBheWFibGVBbW91bnQvQGN1cnJlbmN5SUQiPg0KCQkJCQkJCQkJCQk8eHNsOnRleHQ+IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpQYXlhYmxlQW1vdW50L0BjdXJyZW5jeUlEID0gJ1RSTCciPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpQYXlhYmxlQW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlBheWFibGVBbW91bnQvQGN1cnJlbmN5SUQiDQoJCQkJCQkJCQkJCQkvPg0KCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCTwvdGQ+DQoJCQkJCTwvdHI+DQoJCQkJPC90YWJsZT4NCgkJCQk8YnIvPg0KCQkJCTx0YWJsZSBpZD0ibm90ZXNUYWJsZSIgd2lkdGg9IjgwMCIgYWxpZ249ImxlZnQiIGhlaWdodD0iMTAwIj4NCgkJCQkJPHRib2R5Pg0KCQkJCQkJPHRyIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQk8dGQgaWQ9Im5vdGVzVGFibGVUZCI+DQoJCQkJCQkJCTx4c2w6aWYgdGVzdD0iLy9uMTpJbnZvaWNlL2NiYzpOb3RlIj4NCgkJCQkJCQkJCTxiPiYjMTYwOyYjMTYwOyYjMTYwOyYjMTYwOyYjMTYwOyBOb3Q6IDwvYj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIvL24xOkludm9pY2UvY2JjOk5vdGUiLz4NCgkJCQkJCQkJCTxici8+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6UGF5bWVudE1lYW5zL2NiYzpJbnN0cnVjdGlvbk5vdGUiPg0KCQkJCQkJCQkJPGI+JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7IMOWZGVtZQ0KCQkJCQkJCQkJCU5vdHU6IDwvYj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6UGF5bWVudE1lYW5zL2NiYzpJbnN0cnVjdGlvbk5vdGUiLz4NCgkJCQkJCQkJCTxici8+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQk8eHNsOmlmDQoJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOlBheW1lbnRNZWFucy9jYWM6UGF5ZWVGaW5hbmNpYWxBY2NvdW50L2NiYzpQYXltZW50Tm90ZSI+DQoJCQkJCQkJCQk8Yj4mIzE2MDsmIzE2MDsmIzE2MDsmIzE2MDsmIzE2MDsgSGVzYXANCgkJCQkJCQkJCQlBw6fEsWtsYW1hc8SxOiA8L2I+DQoJCQkJCQkJCQk8eHNsOnZhbHVlLW9mDQoJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOlBheW1lbnRNZWFucy9jYWM6UGF5ZWVGaW5hbmNpYWxBY2NvdW50L2NiYzpQYXltZW50Tm90ZSIvPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTx4c2w6aWYgdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpQYXltZW50VGVybXMvY2JjOk5vdGUiPg0KCQkJCQkJCQkJPGI+JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7IMOWZGVtZQ0KCQkJCQkJCQkJCUtvxZ91bHU6IDwvYj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOlBheW1lbnRUZXJtcy9jYmM6Tm90ZSIvPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJPC90ZD4NCgkJCQkJCTwvdHI+DQoJCQkJCTwvdGJvZHk+DQoJCQkJPC90YWJsZT4NCgkJCTwvYm9keT4NCgkJPC9odG1sPg0KCTwveHNsOnRlbXBsYXRlPg0KCTx4c2w6dGVtcGxhdGUgbWF0Y2g9ImRhdGVGb3JtYXR0ZXIiPg0KCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sOSwyKSIvPi08eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sNiwyKSINCgkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiwxLDQpIi8+DQoJPC94c2w6dGVtcGxhdGU+DQoJPHhzbDp0ZW1wbGF0ZSBtYXRjaD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZSI+DQoJCTx0ciBpZD0ibGluZVRhYmxlVHIiPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYmM6SUQiLz4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6TmFtZSIvPg0KCQkJCQk8IS0tCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6QnJhbmROYW1lIi8+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6TW9kZWxOYW1lIi8+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6RGVzY3JpcHRpb24iLz4tLT4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguL2NiYzpJbnZvaWNlZFF1YW50aXR5LCAnIyMjLiMjIywjIycsICdldXJvcGVhbicpIi8+DQoJCQkJCTx4c2w6aWYgdGVzdD0iLi9jYmM6SW52b2ljZWRRdWFudGl0eS9AdW5pdENvZGUiPg0KCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Ii4vY2JjOkludm9pY2VkUXVhbnRpdHkiPg0KCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnMjYnIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5Ub248L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdCWCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pkt1dHU8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdMVFInIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5MVDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoNCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnTklVJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+QWRldDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ0tHTSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PktHPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnS0pPJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+a0o8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdHUk0nIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5HPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnTUdNJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TUc8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdOVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk5ldCBUb248L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdHVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PkdUPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnTVRSJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TTwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ01NVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk1NPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnS1RNJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+S008L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdNTFQnIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5NTDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ01NUSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk1NMzwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ0NMVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PkNMPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnQ01LJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+Q00yPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnQ01RJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+Q00zPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnQ01UJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+Q008L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdNVEsnIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5NMjwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ01UUSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk0zPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnREFZJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+IEfDvG48L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdNT04nIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD4gQXk8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdQQSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PiBQYWtldDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ0tXSCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PiBLV0g8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQk8L3hzbDppZj4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguL2NhYzpQcmljZS9jYmM6UHJpY2VBbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiLz4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NhYzpQcmljZS9jYmM6UHJpY2VBbW91bnQvQGN1cnJlbmN5SUQiPg0KCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJPHhzbDppZg0KCQkJCQkJCXRlc3Q9Ii4vY2FjOlByaWNlL2NiYzpQcmljZUFtb3VudC9AY3VycmVuY3lJRCA9ICZxdW90O1RSTCZxdW90OyAiPg0KCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJCTx4c2w6aWYNCgkJCQkJCQl0ZXN0PSIuL2NhYzpQcmljZS9jYmM6UHJpY2VBbW91bnQvQGN1cnJlbmN5SUQgIT0gJnF1b3Q7VFJMJnF1b3Q7Ij4NCgkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6UHJpY2UvY2JjOlByaWNlQW1vdW50L0BjdXJyZW5jeUlEIi8+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJPC94c2w6aWY+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOmlmIHRlc3Q9Ii4vY2FjOkFsbG93YW5jZUNoYXJnZS9jYmM6TXVsdGlwbGllckZhY3Rvck51bWVyaWMiPg0KCQkJCQkJPHhzbDp0ZXh0PiAlPC94c2w6dGV4dD4NCgkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpNdWx0aXBsaWVyRmFjdG9yTnVtZXJpYyAqIDEwMCwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSINCgkJCQkJCS8+DQoJCQkJCTwveHNsOmlmPg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NhYzpBbGxvd2FuY2VDaGFyZ2UiPg0KCQkJCQkJPCEtLTx4c2w6aWYgdGVzdD0iLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpDaGFyZ2VJbmRpY2F0b3IgPSB0cnVlKCkgIj4rDQoJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4vY2FjOkFsbG93YW5jZUNoYXJnZS9jYmM6Q2hhcmdlSW5kaWNhdG9yID0gZmFsc2UoKSAiPi0NCgkJCQkJCQkJCQk8L3hzbDppZj4tLT4NCgkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpBbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiDQoJCQkJCQkvPg0KCQkJCQk8L3hzbDppZj4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NhYzpBbGxvd2FuY2VDaGFyZ2UvY2JjOkFtb3VudC9AY3VycmVuY3lJRCI+DQoJCQkJCQk8eHNsOnRleHQ+IDwveHNsOnRleHQ+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4vY2FjOkFsbG93YW5jZUNoYXJnZS9jYmM6QW1vdW50L0BjdXJyZW5jeUlEID0gJ1RSTCciPg0KCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJCTx4c2w6aWYgdGVzdD0iLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpBbW91bnQvQGN1cnJlbmN5SUQgIT0gJ1RSTCciPg0KCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIuL2NhYzpBbGxvd2FuY2VDaGFyZ2UvY2JjOkFtb3VudC9AY3VycmVuY3lJRCIvPg0KCQkJCQkJPC94c2w6aWY+DQoJCQkJCTwveHNsOmlmPg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJc2VsZWN0PSIuL2NhYzpUYXhUb3RhbC9jYWM6VGF4U3VidG90YWwvY2FjOlRheENhdGVnb3J5L2NhYzpUYXhTY2hlbWUiPg0KCQkJCQkJPHhzbDppZiB0ZXN0PSJjYmM6VGF4VHlwZUNvZGU9JzAwMTUnICI+DQoJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCTx4c2w6aWYgdGVzdD0iLi4vLi4vY2JjOlBlcmNlbnQiPg0KCQkJCQkJCQk8eHNsOnRleHQ+ICU8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8eHNsOnZhbHVlLW9mDQoJCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi4vLi4vY2JjOlBlcmNlbnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiDQoJCQkJCQkJCS8+DQoJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQlzZWxlY3Q9Ii4vY2FjOlRheFRvdGFsL2NhYzpUYXhTdWJ0b3RhbC9jYWM6VGF4Q2F0ZWdvcnkvY2FjOlRheFNjaGVtZSI+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9ImNiYzpUYXhUeXBlQ29kZT0nMDAxNScgIj4NCgkJCQkJCQk8eHNsOnRleHQ+IDwveHNsOnRleHQ+DQoJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi4vLi4vY2JjOlRheEFtb3VudCwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSIvPg0KCQkJCQkJCTx4c2w6aWYgdGVzdD0iLi4vLi4vY2JjOlRheEFtb3VudC9AY3VycmVuY3lJRCI+DQoJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEID0gJ1RSTCciPg0KCQkJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTx4c2w6aWYgdGVzdD0iLi4vLi4vY2JjOlRheEFtb3VudC9AY3VycmVuY3lJRCAhPSAnVFJMJyI+DQoJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi4vLi4vY2JjOlRheEFtb3VudC9AY3VycmVuY3lJRCIvPg0KCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCTwveHNsOmlmPg0KCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9ImZvbnQtc2l6ZTogeHgtc21hbGwiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQlzZWxlY3Q9Ii4vY2FjOlRheFRvdGFsL2NhYzpUYXhTdWJ0b3RhbC9jYWM6VGF4Q2F0ZWdvcnkvY2FjOlRheFNjaGVtZSI+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9ImNiYzpUYXhUeXBlQ29kZSE9JzAwMTUnICI+DQoJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6TmFtZSIvPg0KCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpQZXJjZW50Ij4NCgkJCQkJCQkJCTx4c2w6dGV4dD4gKCU8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguLi8uLi9jYmM6UGVyY2VudCwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSINCgkJCQkJCQkJCS8+DQoJCQkJCQkJCQk8eHNsOnRleHQ+KT08L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQk8eHNsOnZhbHVlLW9mDQoJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguLi8uLi9jYmM6VGF4QW1vdW50LCAnIyMjLiMjMCwwMCcsICdldXJvcGVhbicpIi8+DQoJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQgPSAnVFJMJyI+DQoJCQkJCQkJCQk8eHNsOnRleHQ+VEw8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEIi8+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJPC94c2w6aWY+DQoJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJc2VsZWN0PSJmb3JtYXQtbnVtYmVyKC4vY2JjOkxpbmVFeHRlbnNpb25BbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiLz4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCTx4c2w6aWYgdGVzdD0iLi9jYmM6TGluZUV4dGVuc2lvbkFtb3VudC9AY3VycmVuY3lJRCA9ICdUUkwnICI+DQoJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCTwveHNsOmlmPg0KCQkJCQkJPHhzbDppZiB0ZXN0PSIuL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnICI+DQoJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Ii4vY2JjOkxpbmVFeHRlbnNpb25BbW91bnQvQGN1cnJlbmN5SUQiLz4NCgkJCQkJCTwveHNsOmlmPg0KCQkJCQk8L3hzbDppZj4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQk8L3RyPg0KCTwveHNsOnRlbXBsYXRlPg0KCTx4c2w6dGVtcGxhdGUgbWF0Y2g9Ii8vbjE6SW52b2ljZSI+DQoJCTx0ciBpZD0ibGluZVRhYmxlVHIiPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIj4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQk8L3RyPg0KCTwveHNsOnRlbXBsYXRlPg0KPC94c2w6c3R5bGVzaGVldD4NCg=="));
			cacAttachment.appendChild(cbcEmbeddedDocumentBinaryObject);
			
			
			// cac:Signature
			Element cacSignature = invoiceXML.createElement("cac:Signature");
			root.appendChild(cacSignature);
			
			cacSignature.appendChild(createID(invoiceXML, ACCOUNTING_TYPE_SUPPLIER));
			
			// cac:SignatoryParty
			Element cacSignatoryParty  = invoiceXML.createElement("cac:SignatoryParty");
			cacSignature.appendChild(cacSignatoryParty);
				
			cacSignatoryParty.appendChild(createPartyIdentification(invoiceXML, ACCOUNTING_TYPE_SUPPLIER));
			
			//cac:PostalAddress
			cacSignatoryParty.appendChild(createPostalAdress(invoiceXML, ACCOUNTING_TYPE_SUPPLIER));
			
			
			// cac:DigitalSignatureAttachment
			Element cacDigitalSignatureAttachment  = invoiceXML.createElement("cac:DigitalSignatureAttachment");
			cacSignature.appendChild(cacDigitalSignatureAttachment);
			
			Element cacExternalReference  = invoiceXML.createElement("cac:ExternalReference");
			Element cbcURI  = invoiceXML.createElement("cbc:URI");
			cbcURI.appendChild(invoiceXML.createTextNode("#Signature_TST2013000000045"));
			cacExternalReference.appendChild(cbcURI);
			cacDigitalSignatureAttachment.appendChild(cacExternalReference);
			
			
			//cac:AccountingSupplierParty
			Element cacAccountingSupplierParty  = invoiceXML.createElement("cac:AccountingSupplierParty");
			root.appendChild(cacAccountingSupplierParty);
			cacAccountingSupplierParty.appendChild(createParty(invoiceXML, ACCOUNTING_TYPE_SUPPLIER));
			
			//cac:AccountingCustomerParty
			Element cacAccountingCustomerParty  = invoiceXML.createElement("cac:AccountingCustomerParty");
			root.appendChild(cacAccountingCustomerParty);
			cacAccountingCustomerParty.appendChild(createParty(invoiceXML, ACCOUNTING_TYPE_CUSTOMER));
			
			//cac:AllowanceCharge
			Element invoiceAllowanceCharge = getInvoiceAllowanceCharge(invoiceXML);
			if (invoiceAllowanceCharge != null)
				root.appendChild(invoiceAllowanceCharge);
			
			//cac:TaxTotal
			root.appendChild(createTaxTotals(invoiceXML));
			
			//cac:LegalMonetaryTotal
			Element cacLegalMonetaryTotal  = invoiceXML.createElement("cac:LegalMonetaryTotal");
			root.appendChild(cacLegalMonetaryTotal);
			
			Element cbcLineExtensionAmount  = invoiceXML.createElement("cbc:LineExtensionAmount");
			cbcLineExtensionAmount.setAttribute("currencyID", "TRL");
			cbcLineExtensionAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(m_Invoice.getTotalGross())));
			cacLegalMonetaryTotal.appendChild(cbcLineExtensionAmount);
			
			Element cbcTaxExclusiveAmount  = invoiceXML.createElement("cbc:TaxExclusiveAmount");
			cbcTaxExclusiveAmount.setAttribute("currencyID", "TRL");
			cbcTaxExclusiveAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(m_Invoice.getTotalDiscounted())));
			cacLegalMonetaryTotal.appendChild(cbcTaxExclusiveAmount);
			
			Element cbcTaxInclusiveAmount  = invoiceXML.createElement("cbc:TaxInclusiveAmount");
			cbcTaxInclusiveAmount.setAttribute("currencyID", "TRL");
			cbcTaxInclusiveAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(m_Invoice.getTotalNet())));
			cacLegalMonetaryTotal.appendChild(cbcTaxInclusiveAmount);
			
			Element cbcAllowanceTotalAmount  = invoiceXML.createElement("cbc:AllowanceTotalAmount");
			cbcAllowanceTotalAmount.setAttribute("currencyID", "TRL");
			cbcAllowanceTotalAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(m_Invoice.getTotalDiscounts())));
			cacLegalMonetaryTotal.appendChild(cbcAllowanceTotalAmount);
			
			Element cbcChargeTotalAmount  = invoiceXML.createElement("cbc:ChargeTotalAmount");
			cbcChargeTotalAmount.setAttribute("currencyID", "TRL");
			cbcChargeTotalAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(m_Invoice.getTotalExpenses())));
			cacLegalMonetaryTotal.appendChild(cbcChargeTotalAmount);
			
			Element cbcPayableAmount  = invoiceXML.createElement("cbc:PayableAmount");
			cbcPayableAmount.setAttribute("currencyID", "TRL");
			cbcPayableAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(m_Invoice.getTotalNet())));
			cacLegalMonetaryTotal.appendChild(cbcPayableAmount);
			
			
			int transactionsSize = m_Invoice.getTransactions().size();
			//cac:InvoiceLine
			for (int i = 0; i < transactionsSize; i++)
			{
				MMBOTransaction trans = (MMBOTransaction) m_Invoice.getTransactions().get(i);
				if (trans.getMaster_Reference() == 0)
					continue;
				Element cacInvoiceLine  = invoiceXML.createElement("cac:InvoiceLine");
				root.appendChild(cacInvoiceLine);
				
				cacInvoiceLine.appendChild(invoiceXML.createElement("cbc:ID"));
				Element cbcInvoicedQuantity  = invoiceXML.createElement("cbc:InvoicedQuantity");
				cbcInvoicedQuantity.setAttribute("unitCode", trans.getUnits().getGlobalCode());
				cbcInvoicedQuantity.appendChild(invoiceXML.createTextNode(getStringOfValue(trans.getQuantity())));
				cacInvoiceLine.appendChild(cbcInvoicedQuantity);
				
				cbcLineExtensionAmount  = invoiceXML.createElement("cbc:LineExtensionAmount");
				cbcLineExtensionAmount.setAttribute("currencyID", "TRL");
				cbcLineExtensionAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(trans.getTotal())));
				cacInvoiceLine.appendChild(cbcLineExtensionAmount);
				
				if ((i + 1) <= (transactionsSize - 1)
						&& m_Invoice.getTransactions().get(i + 1) != null)
				{
					MMBOTransaction allowanceTrans = m_Invoice.getTransactions().get(i+1);
					Element invoiceLineAllowanceCharge = getInvoiceLineAllowanceCharge(invoiceXML, allowanceTrans, trans.getTotal());
					if (invoiceLineAllowanceCharge != null)
						cacInvoiceLine.appendChild(invoiceLineAllowanceCharge);
				}
				
				BusinessObjects<MMBOTransaction> transList = new BusinessObjects<MMBOTransaction>();
				transList.add(trans);
				calculateTaxTotal(transList);
				cacInvoiceLine.appendChild(createTaxTotals(invoiceXML));
				
				Element cacItem = invoiceXML.createElement("cac:Item");
				Element cbcName = getElement(invoiceXML, "cbc:Name");
				cbcName.appendChild(invoiceXML.createTextNode(trans.getitemLink().getDescription()));
				cacItem.appendChild(cbcName);
				cacInvoiceLine.appendChild(cacItem);
				
				Element cacPrice = invoiceXML.createElement("cac:Price");
				Element cbcPriceAmount = invoiceXML.createElement("cbc:PriceAmount");
				cbcPriceAmount.setAttribute("currencyID", "TRL");
				cbcPriceAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(trans.getPrice())));
				cacPrice.appendChild(cbcPriceAmount);
				cacInvoiceLine.appendChild(cacPrice);
			}
			
			try
			{
				
				File invoiceXMLFile = ProjectUtilEInv.createTempFile(m_Invoice.getGUId(), ".xml");
				DOMSource source = new DOMSource(invoiceXML);
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				JLbsFileUtil.write2File(invoiceXMLFile.getAbsolutePath(), ProjectUtilEInv.documentToByte(source, transformer));
				
				m_InvoicesXML.add(invoiceXMLFile);
				
				CustomBusinessObject approvalBO = ProjectUtilEInv.createNewCBO("CBOApproval");
				approvalBO._setState(CustomBusinessObject.STATE_NEW);
				byte [] LData = ProjectUtilEInv.documentToByte(source, transformer);
				Calendar now = DateUtil.getToday();
				ProjectUtilEInv.setMemberValue(approvalBO, "DocNr", m_Invoice.getInvoiceNumber());
				ProjectUtilEInv.setMemberValue(approvalBO, "GenExp", m_ArpInfo.getDescription());
				ProjectUtilEInv.setMemberValue(approvalBO, "Date_", now);
				ProjectUtilEInv.setMemberValue(approvalBO, "Time_",  now.getTimeInMillis());
				ProjectUtilEInv.setMemberValue(approvalBO, "RecType", ProjectGlobalsEInv.RECTYPE_SENDED_INV);
				ProjectUtilEInv.setMemberValue(approvalBO, "Status", ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED);
				ProjectUtilEInv.setMemberValue(approvalBO, "Sender", m_ArpInfo.getIsPersComp() == 1 ? m_ArpInfo.getIDTCNo() : m_ArpInfo.getTax_Id());
				ProjectUtilEInv.setMemberValue(approvalBO, "FileName", m_Invoice.getGUId());
				ProjectUtilEInv.setMemberValue(approvalBO, "TrCode", m_Invoice.getInvoiceType());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocRef", m_Invoice.getInternal_Reference());
				ProjectUtilEInv.setMemberValue(approvalBO, "OpType", getOpType());
				ProjectUtilEInv.setMemberValue(approvalBO, "ProfileID", m_Invoice.getProfileId());
				ProjectUtilEInv.setMemberValue(approvalBO, "PKLabel", m_ArpInfo.getPkurn());
				ProjectUtilEInv.setMemberValue(approvalBO, "GBLabel", m_ArpInfo.getGburn());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocDate", m_Invoice.getInvoiceDate());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocTotal", m_Invoice.getTotalNet());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocExplain", BUHelper.byteArrToString(m_Invoice.getNotes()!= null ? m_Invoice.getNotes().getDocument():null));
				ProjectUtilEInv.setMemberValue(approvalBO, "LData", LData);
				//ProjectUtilEInv.persistCBO(m_Context, approvalBO);
				m_InvoicesCBO.add(approvalBO);
				
				persistSlipObject(LData);
				
			}
			catch (TransformerException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		catch (Exception e)
		{
			m_Context.getLogger().error("error while creatin invoiceXML", e);
		}
	}
	
	private static String getStringOfValue(BigDecimal value)
	{
		return value.compareTo(UnityConstants.bEpsilon) > 0 ? value.setScale(2,
				RoundingMode.HALF_UP).toString() : UnityConstants.bZero
				.toString();
	}
	
	
	private static Element getInvoiceLineAllowanceCharge(Document invoiceXML, MMBOTransaction trans, BigDecimal baseAmount)
	{
		if (!(trans.getDetailCategory() == UnityConstants.DETCAT_TRANSACTIONS) ||
				!(trans.getTransType() == MMConstants.MMTRN_EXPENSE 
				   || trans.getTransType() == MMConstants.MMTRN_DISCOUNT))
			return null;
		
		Element cacAllowanceCharge =  invoiceXML.createElement("cac:AllowanceCharge");
		Element cbcChargeIndicator  = invoiceXML.createElement("cbc:ChargeIndicator");
		cbcChargeIndicator.appendChild(invoiceXML.createTextNode(trans.getTransType() == MMConstants.MMTRN_DISCOUNT ? "false":"true"));
		cacAllowanceCharge.appendChild(cbcChargeIndicator);
		
		Element cbcMultiplierFactorNumeric  = invoiceXML.createElement("cbc:MultiplierFactorNumeric");
		cbcMultiplierFactorNumeric.appendChild(invoiceXML.createTextNode(getStringOfValue(trans.getDiscountRate().
				divide(UnityConstants.bHundred, 20, BigDecimal.ROUND_UP))));
		cacAllowanceCharge.appendChild(cbcMultiplierFactorNumeric);
				
		Element cbcAmount  = invoiceXML.createElement("cbc:Amount");
		cbcAmount.setAttribute("currencyID", "TRL");
		cbcAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(trans.getTotal())));
		cacAllowanceCharge.appendChild(cbcAmount);
		
		Element cbcBaseAmount   = invoiceXML.createElement("cbc:BaseAmount");
		cbcBaseAmount.setAttribute("currencyID", "TRL");
		cbcBaseAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(baseAmount)));
		cacAllowanceCharge.appendChild(cbcBaseAmount);
		
		return cacAllowanceCharge;
	}


	private static Element getInvoiceAllowanceCharge(Document invoiceXML) 
	{
		Element cacAllowanceCharge = null;
		for (int i = 0; i < m_Invoice.getTransactions().size(); i++)
		{
			MMBOTransaction trans = (MMBOTransaction) m_Invoice.getTransactions().get(i);
			if (trans.getDetailCategory() == UnityConstants.DETCAT_GLOBAL
					&& (trans.getTransType() == MMConstants.MMTRN_EXPENSE || trans
							.getTransType() == MMConstants.MMTRN_DISCOUNT)) 
			{
				cacAllowanceCharge = invoiceXML.createElement("cac:AllowanceCharge");
				Element cbcChargeIndicator  = invoiceXML.createElement("cbc:ChargeIndicator");
				cbcChargeIndicator.appendChild(invoiceXML.createTextNode(trans.getTransType() == MMConstants.MMTRN_DISCOUNT ? "false":"true"));
				cacAllowanceCharge.appendChild(cbcChargeIndicator);
				
				Element cbcAmount  = invoiceXML.createElement("cbc:Amount");
				cbcAmount.setAttribute("currencyID", "TRL");
				cbcAmount.appendChild(invoiceXML.createTextNode(getStringOfValue(trans.getTotal())));
				cacAllowanceCharge.appendChild(cbcAmount);
			}
		}
		
		return cacAllowanceCharge;
	}


	private static void persistSlipObject(byte [] LData)
	{
		UNBOSlipObject slipObject = null;
		String cond = "($this.SLIPREF = " + m_Invoice.getInternal_Reference() + " AND $this.MODULENR = 1"+ " AND $this.OBJTYPE  = 0"+")" ;
		BusinessObjects slipObjList = ProjectUtilEInv.searchBOListByCond(m_Context, UNBOSlipObject.class, cond, 0);
		if(slipObjList != null && slipObjList.size() > 0)
		{
			 slipObject = (UNBOSlipObject) slipObjList.get(0);
			 slipObject._setState(BusinessObject.STATE_MODIFIED);
			 slipObject.setLogoObject(LData);
		}
		else
		{
			slipObject._setState(BusinessObject.STATE_NEW);
			slipObject.setSlipReference(m_Invoice.getInternal_Reference());
			slipObject.setModule(1);
			slipObject.setObjectType(0);
			slipObject.setSlipType(m_Invoice.getInvoiceType());
			slipObject.setLogoObject(LData);
		}
		UnityHelper.persistBO(m_Context, slipObject);
	}
	
	private static byte[] convertFileToByteArray(File file) throws FileNotFoundException, IOException
	{
		 
	    FileInputStream fis = new FileInputStream(file);
	    //System.out.println(file.exists() + "!!");
	    //InputStream in = resource.openStream();
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    byte[] buf = new byte[1024];
	    try {
	        for (int readNum; (readNum = fis.read(buf)) != -1;) {
	            bos.write(buf, 0, readNum); //no doubt here is 0
	            //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
	            System.out.println("read " + readNum + " bytes,");
	        }
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
	    byte[] bytes = bos.toByteArray();
	
	    /*below is the different part
	    File someFile = new File("java2.pdf");
	    FileOutputStream fos = new FileOutputStream(someFile);
	    fos.write(bytes);
	    fos.flush();
	    fos.close();*/
	    fis.close();
	    return bytes;
	}
	
	private static File createZipFile(File [] fs)
	{
		File zipFile = null;
		try
		{
			zipFile = ProjectUtilEInv.createTempFile(ProjectUtilEInv.generateGUID(),	".zip");
			File f = new File(fs[0].getParent());
			if (f.exists()) 
			{
				JLbsFileUtil.zip(fs, f, zipFile, true);
				File renamedZipFile = new File(zipFile.getAbsolutePath().substring(0, zipFile.getAbsolutePath().length() - 4));
				//zipFile.renameTo(renamedZipFile);
			} 
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return zipFile;
	}
	
	public static void addToZipFile(String fileName, ZipOutputStream zos)
			throws FileNotFoundException, IOException {

		System.out.println("Writing '" + fileName + "' to zip file");

		File file = new File(fileName);
		FileInputStream fis = new FileInputStream(file);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zos.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}

	
	
	   private static void createPackagesByInvoices(HashMap packedInvoiceMap) {
		
		for (Iterator i = packedInvoiceMap.keySet().iterator(); i.hasNext();)
		{
			String key = (String) i.next();
			String[] keyArr = StringUtil.split(key, "-");
			m_OrgUnit = (GOBOOrgUnit) m_OrgUnitMap.get(Integer.parseInt(keyArr[0]));
			m_ArpInfo = (FIBOConnectArpInfo) m_ArpInfoMap.get(Integer.parseInt(keyArr[1]));
			
			ArrayList invoiceList = (ArrayList) packedInvoiceMap.get(key);
			m_InvoicesCBO = new CustomBusinessObjects<CustomBusinessObject>();
			m_InvoicesXML = new ArrayList<File>();
			for (int j = 0; j < invoiceList.size(); j++) 
			{
				m_Invoice = (LOBOConnectEInvoice) invoiceList.get(j);
				createInvoice();
			}
			
			try 
			{
				File zipFile = createZipFile(m_InvoicesXML.toArray(new File[m_InvoicesXML.size()]));
				byte [] LData = convertFileToByteArray(zipFile);
				createAndPersistTransaction(m_InvoicesCBO, zipFile, LData,
						null, ProjectGlobalsEInv.TRANSACTION_TYPE_EINV,
						ProjectGlobalsEInv.RECTYPE_PACKED_FOR_SENDING_EINV);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
				
		}
		
	}
	   
	   
	 private static void createAndPersistTransaction(CustomBusinessObjects lines, File zipFile, byte [] LData, Document approvalDoc,
			 int transType , int docType)
	 {
		 	CustomBusinessObject transaction = ProjectUtilEInv.createNewCBO("CBOTransaction");
		 	transaction._setState(CustomBusinessObject.STATE_NEW);
			ProjectUtilEInv.setMemberValueUn(transaction, "Approvals", lines);
			
			Calendar now = DateUtil.getToday();
			ProjectUtilEInv.setMemberValue(transaction, "TransId", zipFile.getName().substring(0, zipFile.getName().length() - 4));
			ProjectUtilEInv.setMemberValue(transaction, "DateStarted", now);
			ProjectUtilEInv.setMemberValue(transaction, "TimeStarted",  now.getTimeInMillis());
			ProjectUtilEInv.setMemberValue(transaction, "TransType", transType);
			ProjectUtilEInv.setMemberValue(transaction, "PackCount", 1);
			ProjectUtilEInv.setMemberValue(transaction, "DocType", docType);
			ProjectUtilEInv.setMemberValue(transaction, "RecCount", transType == ProjectGlobalsEInv.TRANSACTION_TYPE_EINV ?  m_InvoicesCBO.size() : 1);
			ProjectUtilEInv.setMemberValue(transaction, "ClientId", getTransactionClientID(approvalDoc, transType));
			ProjectUtilEInv.setMemberValue(transaction, "Alias_", getTransactionAlias(approvalDoc, transType));
			ProjectUtilEInv.setMemberValue(transaction, "LData", LData);

			try 
			{
				ProjectUtilEInv.persistCBO(m_Context, transaction);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
	 }

	private static Object getTransactionAlias(Document approvalDoc,
			int transType)
	{
		if (transType == ProjectGlobalsEInv.TRANSACTION_TYPE_EINV)
			return m_ArpInfo != null ? m_ArpInfo.getPkurn() : "";
		else if (transType == ProjectGlobalsEInv.TRANSACTION_TYPE_APPRESP) 
		{
			return getNodeTextByTagName(approvalDoc, "sh:Sender", "sh:Identifier");
		}
			 
		return "";
	}


	private static String getTransactionClientID(Document approvalDoc, int transType) 
	{
		if (transType == ProjectGlobalsEInv.TRANSACTION_TYPE_EINV)
			return m_ArpInfo.getIsPersComp() == 1 ? m_ArpInfo.getIDTCNo() : m_ArpInfo.getTax_Id();
		else if (transType == ProjectGlobalsEInv.TRANSACTION_TYPE_APPRESP) 
		{
			return getNodeTextByTagName(approvalDoc, "cac:AccountingSupplierParty", "cbc:ID");
		}
		return "";
	}
	
	/*QUESTION2
 	   Connect paket(117) ve fatura(110) için approval tablosuna iki kayýt atýyor. Bunun yerine transaction tablosuna "LData"(paket) alaný 
 	   ekledim ve paket satýrýný oluþturmadým sýkýntý olur mu? 
	 */
	/*private static void createPackagesByEnvelope(HashMap packedInvoiceMap) {
		
		for (Iterator i = packedInvoiceMap.keySet().iterator(); i.hasNext();)
		{
			String key = (String) i.next();
			String[] keyArr = StringUtil.split(key, "-");
			m_OrgUnit = (GOBOOrgUnit) m_OrgUnitMap.get(Integer.parseInt(keyArr[0]));
			m_ArpInfo = (FIBOConnectArpInfo) m_ArpInfoMap.get(Integer.parseInt(keyArr[1]));
			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder;
			try 
			{
				docBuilder = builderFactory.newDocumentBuilder();
				m_PackageXML = docBuilder.newDocument();
				Element root = m_PackageXML.createElement("sh:StandardBusinessDocument");
				root.setAttribute("xsi:schemaLocation", "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader PackageProxy.xsd");
				root.setAttribute("xmlns:sh", "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader");
				root.setAttribute("xmlns:ef", "http://www.efatura.gov.tr/package-namespace");
				root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
				m_PackageXML.appendChild(root);
				m_PackageXML.setXmlStandalone(true);
				
				Element documentHeader = m_PackageXML.createElement("sh:StandardBusinessDocumentHeader");
				
				//HeaderVersion
				Element headerVersion = m_PackageXML.createElement("sh:HeaderVersion");
				headerVersion.appendChild(m_PackageXML.createTextNode("1.0"));
				documentHeader.appendChild(headerVersion);
				
				//Sender
				Element sender = m_PackageXML.createElement("sh:Sender");
				Element senderIdentifier = m_PackageXML.createElement("sh:Identifier");
				senderIdentifier.appendChild(m_PackageXML.createTextNode(m_OrgUnit.getGburn()));
				sender.appendChild(senderIdentifier);
				
				//ContactInformation UNVAN
				Element senderContInfoUnvan = m_PackageXML.createElement("sh:ContactInformation");
				
				Element sendContUnvan = m_PackageXML.createElement("sh:Contact");
				sendContUnvan.appendChild(m_PackageXML.createTextNode(m_OrgUnit.getTitle()));
				senderContInfoUnvan.appendChild(sendContUnvan);
				
				Element senderContTypeIdentUnvan = m_PackageXML.createElement("sh:ContactTypeIdentifier");
				senderContTypeIdentUnvan.appendChild(m_PackageXML.createTextNode("UNVAN"));
				senderContInfoUnvan.appendChild(senderContTypeIdentUnvan);
				
				//ContactInformation VKN_TCKN
				Element senderContInfoVKN = m_PackageXML.createElement("sh:ContactInformation");
				
				Element senderContVKN = m_PackageXML.createElement("sh:Contact");
				senderContVKN.appendChild(m_PackageXML.createTextNode(m_OrgUnit.getTaxNr()));
				senderContInfoVKN.appendChild(senderContVKN);
				
				Element senderContTypeIdentVKN = m_PackageXML.createElement("sh:ContactTypeIdentifier");
				senderContTypeIdentVKN.appendChild(m_PackageXML.createTextNode("VKN_TCKN"));
				senderContInfoVKN.appendChild(senderContTypeIdentVKN);
				
				sender.appendChild(senderContInfoUnvan);
				sender.appendChild(senderContInfoVKN);
				documentHeader.appendChild(sender);
				
				//Reciever
				Element receiver = m_PackageXML.createElement("sh:receiver");
				Element receiverIdentifier = m_PackageXML.createElement("sh:Identifier");
				receiverIdentifier.appendChild(m_PackageXML.createTextNode(m_ArpInfo.getPkurn()));
				receiver.appendChild(receiverIdentifier);
				
				//ContactInformation UNVAN
				Element receiverContInfoUnvan = m_PackageXML.createElement("sh:ContactInformation");
				
				Element receivContUnvan = m_PackageXML.createElement("sh:Contact");
				receivContUnvan.appendChild(m_PackageXML.createTextNode(getRecieverTitle()));
				receiverContInfoUnvan.appendChild(receivContUnvan);
				
				Element receiverContTypeIdentUnvan = m_PackageXML.createElement("sh:ContactTypeIdentifier");
				receiverContTypeIdentUnvan.appendChild(m_PackageXML.createTextNode("UNVAN"));
				receiverContInfoUnvan.appendChild(receiverContTypeIdentUnvan);
				
				//ContactInformation VKN_TCKN
				Element receiverContInfoVKN = m_PackageXML.createElement("sh:ContactInformation");
				
				Element receiverContVKN = m_PackageXML.createElement("sh:Contact");
				receiverContVKN.appendChild(m_PackageXML.createTextNode(getReciverVKN_TCKN()));
				receiverContInfoVKN.appendChild(receiverContVKN);
				
				Element receiverContTypeIdentVKN = m_PackageXML.createElement("sh:ContactTypeIdentifier");
				receiverContTypeIdentVKN.appendChild(m_PackageXML.createTextNode("VKN_TCKN"));
				receiverContInfoVKN.appendChild(receiverContTypeIdentVKN);
				
				receiver.appendChild(receiverContInfoUnvan);
				receiver.appendChild(receiverContInfoVKN);
				documentHeader.appendChild(receiver);
				
				//Document Identification
				Element docIdent = m_PackageXML.createElement("sh:DocumentIdentification");
				
				Element standart = m_PackageXML.createElement("sh:Standard");
				docIdent.appendChild(standart);
				
				Element typeVersion = m_PackageXML.createElement("sh:TypeVersion");
				typeVersion.appendChild(m_PackageXML.createTextNode("1.0"));
				docIdent.appendChild(typeVersion);
				
				Element instanceIdent = m_PackageXML.createElement("sh:InstanceIdentifier");
				String transId = ProjectUtilEInv.generateGUID();
				instanceIdent.appendChild(m_PackageXML.createTextNode(transId));
				docIdent.appendChild(instanceIdent);
				
				Element type = m_PackageXML.createElement("sh:Type");
				type.appendChild(m_PackageXML.createTextNode("SENDERENVELOPE"));
				docIdent.appendChild(type);
				
				Element creationDateTime = m_PackageXML.createElement("sh:CreationDateAndTime");
				creationDateTime.appendChild(m_PackageXML.createTextNode(ProjectUtilEInv.convertDateToXSDDateTime()));
				docIdent.appendChild(creationDateTime);
				
				documentHeader.appendChild(docIdent);
				
				root.appendChild(documentHeader);
				
				//Package
				Element efPackage = m_PackageXML.createElement("ef:Package");
				
					Element elements = m_PackageXML.createElement("Elements");
					
						Element elementType = m_PackageXML.createElement("ElementType");
						elementType.appendChild(m_PackageXML.createTextNode("INVOICE"));
						elements.appendChild(elementType);
		
						Element elementCount = m_PackageXML.createElement("ElementCount");
						ArrayList invoiceList = (ArrayList) packedInvoiceMap.get(key);
						elementCount.appendChild(m_PackageXML.createTextNode(String.valueOf(invoiceList.size())));
						elements.appendChild(elementCount);
						Element elementList = m_PackageXML.createElement("ElementList");
						m_InvoicesCBO = new CustomBusinessObjects<CustomBusinessObject>();
							for (int j = 0; j < invoiceList.size(); j++)
							{
								m_Invoice =  (LOBOConnectEInvoice) invoiceList.get(j);
								Element invoice = createInvoiceByEnvelope();
								elementList.appendChild(invoice);
							}
							elements.appendChild(elementList);
				
				efPackage.appendChild(elements);	
			root.appendChild(efPackage);
			
				String tmpDir = JLbsFileUtil.getTempDirectory();
				String xmlFileName = tmpDir + "JGUARENVLOPE.XML";
				TransformerFactory transformerFactory = TransformerFactory
						.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(m_PackageXML);
				StreamResult result = new StreamResult(new File(xmlFileName));
				transformer.transform(source, result);
				
				CustomBusinessObject approvalBO = ProjectUtilEInv.createNewCBO("CBOTransaction");
				approvalBO._setState(CustomBusinessObject.STATE_NEW);
				
				Calendar now = DateUtil.getToday();
				ProjectUtilEInv.setMemberValue(approvalBO, "TransId", transId);
				ProjectUtilEInv.setMemberValue(approvalBO, "DateStarted", now);
				ProjectUtilEInv.setMemberValue(approvalBO, "TimeStarted",  now.getTimeInMillis());
				ProjectUtilEInv.setMemberValue(approvalBO, "TransType", ProjectGlobalsEInv.TRANSACTION_TYPE_EINV);
				ProjectUtilEInv.setMemberValue(approvalBO, "PackCount", 1);
				ProjectUtilEInv.setMemberValue(approvalBO, "DocType", ProjectGlobalsEInv.RECTYPE_PACKED_FOR_SENDING_EINV);
				ProjectUtilEInv.setMemberValue(approvalBO, "RecCount", m_InvoicesCBO.size());
				ProjectUtilEInv.setMemberValue(approvalBO, "ClientId", m_ArpInfo.getIsPersComp() == 1 ? m_ArpInfo.getIDTCNo() : m_ArpInfo.getTax_Id());
				ProjectUtilEInv.setMemberValue(approvalBO, "Alias_", m_ArpInfo.getPkurn());
				ProjectUtilEInv.setMemberValue(approvalBO, "LData", ProjectUtilEInv.documentToByte(source, transformer));

				CustomBusinessObjects approvals = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(approvalBO, "Approvals");
				approvals.addAll(m_InvoicesCBO);
				ProjectUtilEInv.persistCBO(m_Context, approvalBO);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			
				
		}
		
	}
	*/
	private static Element createIdentification(Document invoiceUBLXML, int accountingType)
	{
		Element cacPartyIdentification = invoiceUBLXML.createElement("cac:PartyIdentification");
		cacPartyIdentification.appendChild(createID(invoiceUBLXML, accountingType));
		return cacPartyIdentification;
	}

	
	/*private static Element createInvoiceByEnvelope()
	{
		m_ArpInfo = m_Invoice.getARPInfo();
		setVatName();
		calculateTaxTotal(m_Invoice.getTransactions());
		Element root = null;
		try
		{
			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
			m_PackageXML = docBuilder.newDocument();
			
			root = m_PackageXML.createElement("Invoice");
			root.setAttribute("xmlns", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2");
			root.setAttribute("xmlns:cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
			root.setAttribute("xmlns:xades", "http://uri.etsi.org/01903/v1.3.2#");
			root.setAttribute("xmlns:udt", "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2");
			root.setAttribute("xmlns:cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
			root.setAttribute("xmlns:ccts", "urn:un:unece:uncefact:documentation:2");
			root.setAttribute("xmlns:ubltr", "urn:oasis:names:specification:ubl:schema:xsd:TurkishCustomizationExtensionComponents");
			root.setAttribute("xmlns:qdt", "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2");
			root.setAttribute("xmlns:ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
			root.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
			root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			root.setAttribute("xsi:schemaLocation", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2 UBLTR-Invoice-2.0.xsd");
			//m_PackageXML.appendChild(root);
			
			Element cbcUBLVersionID = m_PackageXML.createElement("cbc:UBLVersionID");
			cbcUBLVersionID.appendChild(m_PackageXML.createTextNode("2.0"));
			root.appendChild(cbcUBLVersionID);
			
			Element cbcCustomizationID = m_PackageXML.createElement("cbc:CustomizationID");
			cbcCustomizationID.appendChild(m_PackageXML.createTextNode("TR1.0"));
			root.appendChild(cbcCustomizationID);
			
			Element cbcProfileID = m_PackageXML.createElement("cbc:ProfileID");
			cbcProfileID.appendChild(m_PackageXML.createTextNode(getProfileID()));
			root.appendChild(cbcProfileID);
			
			Element cbcID = m_PackageXML.createElement("cbc:ID");
			cbcID.appendChild(m_PackageXML.createTextNode(m_Invoice
					.getInvoiceNumber() == null ? "" : m_Invoice
					.getInvoiceNumber()));
			root.appendChild(cbcID);
			
			Element cbcCopyIndicator = m_PackageXML.createElement("cbc:CopyIndicator");
			cbcCopyIndicator.appendChild(m_PackageXML.createTextNode(getCopyIndicator()));
			root.appendChild(cbcCopyIndicator);
			
			Element cbcUUID = m_PackageXML.createElement("cbc:UUID");
			cbcUUID.appendChild(m_PackageXML.createTextNode(m_Invoice.getGUId() == null ? "" : m_Invoice.getGUId() ));
			root.appendChild(cbcUUID);
			
			
			Element cbcIssueDate = m_PackageXML.createElement("cbc:IssueDate");
			cbcIssueDate.appendChild(m_PackageXML.createTextNode(getIssueDate(m_Invoice.getInvoiceDate())));
			root.appendChild(cbcIssueDate);
			
			Element cbcInvoiceTypeCode = m_PackageXML.createElement("cbc:InvoiceTypeCode");
			String invoiceTypeCode = getInvoiceTypeCode();
			cbcInvoiceTypeCode.appendChild(m_PackageXML.createTextNode(invoiceTypeCode));
			root.appendChild(cbcInvoiceTypeCode);
			
			
			root.appendChild(createNoteElement(m_Context, m_PackageXML));
			
			Element cbcDocumentCurrencyCode = m_PackageXML.createElement("cbc:DocumentCurrencyCode");
			cbcDocumentCurrencyCode.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");
			cbcDocumentCurrencyCode.setAttribute("listID", "ISO 4217 Alpha");
			cbcDocumentCurrencyCode.setAttribute("listName", "Currency");
			cbcDocumentCurrencyCode.setAttribute("listVersionID", "2001");
			cbcDocumentCurrencyCode.appendChild(m_PackageXML.createTextNode(getDocumentCurrencyCode()));
			root.appendChild(cbcDocumentCurrencyCode);
			
			Element cbcLineCountNumeric = m_PackageXML.createElement("cbc:LineCountNumeric");
			cbcLineCountNumeric.appendChild(m_PackageXML.createTextNode(getLineCountNumeric()));
			root.appendChild(cbcLineCountNumeric);
			
			if (m_Invoice.getInvoiceType() == UnityConstants.INVC_PURCHASERET)
			{
				LOBOConnectEInvoice srcInvoice = findSrcInvoice();
				if (srcInvoice != null)
				{
					Element cacAdditionalDocumentReference = m_PackageXML.createElement("cac:AdditionalDocumentReference");
					Element cbcAddDocRefID =  m_PackageXML.createElement("cbc:ID");
					cbcAddDocRefID.appendChild(m_PackageXML.createTextNode(srcInvoice.getGUId()));
					cacAdditionalDocumentReference.appendChild(cbcAddDocRefID);
					
					Element cbcAddDocRefIssueDate = m_PackageXML.createElement("cbc:IssueDate");
					cbcAddDocRefIssueDate.appendChild(m_PackageXML.createTextNode(getIssueDate(srcInvoice.getInvoiceDate())));
					cacAdditionalDocumentReference.appendChild(cbcIssueDate);
					
					root.appendChild(cacAdditionalDocumentReference);
				}
				
				Element cacAttachment = m_PackageXML.createElement("cac:Attachment");
				
				cacAdditionalDocumentReference.appendChild(cacAttachment);
				Element cbcEmbeddedDocumentBinaryObject = m_PackageXML.createElement("cbc:EmbeddedDocumentBinaryObject");
				cbcEmbeddedDocumentBinaryObject.setAttribute("characterSetCode", "UTF-8");
				cbcEmbeddedDocumentBinaryObject.setAttribute("encodingCode", "Base64");
				cbcEmbeddedDocumentBinaryObject.setAttribute("filename", "TST2013000000045.xslt");
				cbcEmbeddedDocumentBinaryObject.setAttribute("mimeCode", "application/xml");
				cbcEmbeddedDocumentBinaryObject
						.appendChild(m_PackageXML
								.createTextNode("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4NCjx4c2w6c3R5bGVzaGVldCB2ZXJzaW9uPSIyLjAiIHhtbG5zOnhzbD0iaHR0cDovL3d3dy53My5vcmcvMTk5OS9YU0wvVHJhbnNmb3JtIg0KCXhtbG5zOmNhYz0idXJuOm9hc2lzOm5hbWVzOnNwZWNpZmljYXRpb246dWJsOnNjaGVtYTp4c2Q6Q29tbW9uQWdncmVnYXRlQ29tcG9uZW50cy0yIg0KCXhtbG5zOmNiYz0idXJuOm9hc2lzOm5hbWVzOnNwZWNpZmljYXRpb246dWJsOnNjaGVtYTp4c2Q6Q29tbW9uQmFzaWNDb21wb25lbnRzLTIiDQoJeG1sbnM6Y2N0cz0idXJuOnVuOnVuZWNlOnVuY2VmYWN0OmRvY3VtZW50YXRpb246MiINCgl4bWxuczpjbG01NDIxNz0idXJuOnVuOnVuZWNlOnVuY2VmYWN0OmNvZGVsaXN0OnNwZWNpZmljYXRpb246NTQyMTc6MjAwMSINCgl4bWxuczpjbG01NjM5PSJ1cm46dW46dW5lY2U6dW5jZWZhY3Q6Y29kZWxpc3Q6c3BlY2lmaWNhdGlvbjo1NjM5OjE5ODgiDQoJeG1sbnM6Y2xtNjY0MTE9InVybjp1bjp1bmVjZTp1bmNlZmFjdDpjb2RlbGlzdDpzcGVjaWZpY2F0aW9uOjY2NDExOjIwMDEiDQoJeG1sbnM6Y2xtSUFOQU1JTUVNZWRpYVR5cGU9InVybjp1bjp1bmVjZTp1bmNlZmFjdDpjb2RlbGlzdDpzcGVjaWZpY2F0aW9uOklBTkFNSU1FTWVkaWFUeXBlOjIwMDMiDQoJeG1sbnM6Zm49Imh0dHA6Ly93d3cudzMub3JnLzIwMDUveHBhdGgtZnVuY3Rpb25zIiB4bWxuczpsaW5rPSJodHRwOi8vd3d3Lnhicmwub3JnLzIwMDMvbGlua2Jhc2UiDQoJeG1sbnM6bjE9InVybjpvYXNpczpuYW1lczpzcGVjaWZpY2F0aW9uOnVibDpzY2hlbWE6eHNkOkludm9pY2UtMiINCgl4bWxuczpxZHQ9InVybjpvYXNpczpuYW1lczpzcGVjaWZpY2F0aW9uOnVibDpzY2hlbWE6eHNkOlF1YWxpZmllZERhdGF0eXBlcy0yIg0KCXhtbG5zOnVkdD0idXJuOnVuOnVuZWNlOnVuY2VmYWN0OmRhdGE6c3BlY2lmaWNhdGlvbjpVbnF1YWxpZmllZERhdGFUeXBlc1NjaGVtYU1vZHVsZToyIg0KCXhtbG5zOnhicmxkaT0iaHR0cDovL3hicmwub3JnLzIwMDYveGJybGRpIiB4bWxuczp4YnJsaT0iaHR0cDovL3d3dy54YnJsLm9yZy8yMDAzL2luc3RhbmNlIg0KCXhtbG5zOnhkdD0iaHR0cDovL3d3dy53My5vcmcvMjAwNS94cGF0aC1kYXRhdHlwZXMiIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIg0KCXhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNkPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSINCgl4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIg0KCWV4Y2x1ZGUtcmVzdWx0LXByZWZpeGVzPSJjYWMgY2JjIGNjdHMgY2xtNTQyMTcgY2xtNTYzOSBjbG02NjQxMSBjbG1JQU5BTUlNRU1lZGlhVHlwZSBmbiBsaW5rIG4xIHFkdCB1ZHQgeGJybGRpIHhicmxpIHhkdCB4bGluayB4cyB4c2QgeHNpIj4NCgk8eHNsOmRlY2ltYWwtZm9ybWF0IG5hbWU9ImV1cm9wZWFuIiBkZWNpbWFsLXNlcGFyYXRvcj0iLCIgZ3JvdXBpbmctc2VwYXJhdG9yPSIuIiBOYU49IiIvPg0KCTx4c2w6b3V0cHV0IHZlcnNpb249IjQuMCIgbWV0aG9kPSJodG1sIiBpbmRlbnQ9Im5vIiBlbmNvZGluZz0iVVRGLTgiDQoJCWRvY3R5cGUtcHVibGljPSItLy9XM0MvL0RURCBIVE1MIDQuMDEgVHJhbnNpdGlvbmFsLy9FTiINCgkJZG9jdHlwZS1zeXN0ZW09Imh0dHA6Ly93d3cudzMub3JnL1RSL2h0bWw0L2xvb3NlLmR0ZCIvPg0KCTx4c2w6cGFyYW0gbmFtZT0iU1ZfT3V0cHV0Rm9ybWF0IiBzZWxlY3Q9IidIVE1MJyIvPg0KCTx4c2w6dmFyaWFibGUgbmFtZT0iWE1MIiBzZWxlY3Q9Ii8iLz4NCgk8eHNsOnRlbXBsYXRlIG1hdGNoPSIvIj4NCgkJPGh0bWw+DQoJCQk8aGVhZD4NCgkJCQk8dGl0bGUvPg0KCQkJCTxzdHlsZSB0eXBlPSJ0ZXh0L2NzcyI+DQoJCQkJCWJvZHkgew0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjogI0ZGRkZGRjsNCgkJCQkJICAgIGZvbnQtZmFtaWx5OiAnVGFob21hJywgIlRpbWVzIE5ldyBSb21hbiIsIFRpbWVzLCBzZXJpZjsNCgkJCQkJICAgIGZvbnQtc2l6ZTogMTFweDsNCgkJCQkJICAgIGNvbG9yOiAjNjY2NjY2Ow0KCQkJCQl9DQoJCQkJCWgxLCBoMiB7DQoJCQkJCSAgICBwYWRkaW5nLWJvdHRvbTogM3B4Ow0KCQkJCQkgICAgcGFkZGluZy10b3A6IDNweDsNCgkJCQkJICAgIG1hcmdpbi1ib3R0b206IDVweDsNCgkJCQkJICAgIHRleHQtdHJhbnNmb3JtOiB1cHBlcmNhc2U7DQoJCQkJCSAgICBmb250LWZhbWlseTogQXJpYWwsIEhlbHZldGljYSwgc2Fucy1zZXJpZjsNCgkJCQkJfQ0KCQkJCQloMSB7DQoJCQkJCSAgICBmb250LXNpemU6IDEuNGVtOw0KCQkJCQkgICAgdGV4dC10cmFuc2Zvcm06bm9uZTsNCgkJCQkJfQ0KCQkJCQloMiB7DQoJCQkJCSAgICBmb250LXNpemU6IDFlbTsNCgkJCQkJICAgIGNvbG9yOiBicm93bjsNCgkJCQkJfQ0KCQkJCQloMyB7DQoJCQkJCSAgICBmb250LXNpemU6IDFlbTsNCgkJCQkJICAgIGNvbG9yOiAjMzMzMzMzOw0KCQkJCQkgICAgdGV4dC1hbGlnbjoganVzdGlmeTsNCgkJCQkJICAgIG1hcmdpbjogMDsNCgkJCQkJICAgIHBhZGRpbmc6IDA7DQoJCQkJCX0NCgkJCQkJaDQgew0KCQkJCQkgICAgZm9udC1zaXplOiAxLjFlbTsNCgkJCQkJICAgIGZvbnQtc3R5bGU6IGJvbGQ7DQoJCQkJCSAgICBmb250LWZhbWlseTogQXJpYWwsIEhlbHZldGljYSwgc2Fucy1zZXJpZjsNCgkJCQkJICAgIGNvbG9yOiAjMDAwMDAwOw0KCQkJCQkgICAgbWFyZ2luOiAwOw0KCQkJCQkgICAgcGFkZGluZzogMDsNCgkJCQkJfQ0KCQkJCQlociB7DQoJCQkJCSAgICBoZWlnaHQ6MnB4Ow0KCQkJCQkgICAgY29sb3I6ICMwMDAwMDA7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOiAjMDAwMDAwOw0KCQkJCQkgICAgYm9yZGVyLWJvdHRvbTogMXB4IHNvbGlkICMwMDAwMDA7DQoJCQkJCX0NCgkJCQkJcCwgdWwsIG9sIHsNCgkJCQkJICAgIG1hcmdpbi10b3A6IDEuNWVtOw0KCQkJCQl9DQoJCQkJCXVsLCBvbCB7DQoJCQkJCSAgICBtYXJnaW4tbGVmdDogM2VtOw0KCQkJCQl9DQoJCQkJCWJsb2NrcXVvdGUgew0KCQkJCQkgICAgbWFyZ2luLWxlZnQ6IDNlbTsNCgkJCQkJICAgIG1hcmdpbi1yaWdodDogM2VtOw0KCQkJCQkgICAgZm9udC1zdHlsZTogaXRhbGljOw0KCQkJCQl9DQoJCQkJCWEgew0KCQkJCQkgICAgdGV4dC1kZWNvcmF0aW9uOiBub25lOw0KCQkJCQkgICAgY29sb3I6ICM3MEEzMDA7DQoJCQkJCX0NCgkJCQkJYTpob3ZlciB7DQoJCQkJCSAgICBib3JkZXI6IG5vbmU7DQoJCQkJCSAgICBjb2xvcjogIzcwQTMwMDsNCgkJCQkJfQ0KCQkJCQkjZGVzcGF0Y2hUYWJsZSB7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6Y29sbGFwc2U7DQoJCQkJCSAgICBmb250LXNpemU6MTFweDsNCgkJCQkJICAgIGZsb2F0OnJpZ2h0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOmdyYXk7DQoJCQkJCX0NCgkJCQkJI2V0dG5UYWJsZSB7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6Y29sbGFwc2U7DQoJCQkJCSAgICBmb250LXNpemU6MTFweDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjpncmF5Ow0KCQkJCQl9DQoJCQkJCSNjdXN0b21lclBhcnR5VGFibGUgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBncmF5Ow0KCQkJCQkgICAgYm9yZGVyLWNvbGxhcHNlOiBjb2xsYXBzZTsNCgkJCQkJICAgIGJhY2tncm91bmQtY29sb3I6DQoJCQkJCX0NCgkJCQkJI2N1c3RvbWVySURUYWJsZSB7DQoJCQkJCSAgICBib3JkZXItd2lkdGg6IDJweDsNCgkJCQkJICAgIGJvcmRlci1zcGFjaW5nOjsNCgkJCQkJICAgIGJvcmRlci1zdHlsZTogaW5zZXQ7DQoJCQkJCSAgICBib3JkZXItY29sb3I6IGdyYXk7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6IGNvbGxhcHNlOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjoNCgkJCQkJfQ0KCQkJCQkjY3VzdG9tZXJJRFRhYmxlVGQgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAycHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBncmF5Ow0KCQkJCQkgICAgYm9yZGVyLWNvbGxhcHNlOiBjb2xsYXBzZTsNCgkJCQkJICAgIGJhY2tncm91bmQtY29sb3I6DQoJCQkJCX0NCgkJCQkJI2xpbmVUYWJsZSB7DQoJCQkJCSAgICBib3JkZXItd2lkdGg6MnB4Ow0KCQkJCQkgICAgYm9yZGVyLXNwYWNpbmc6Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6IGNvbGxhcHNlOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjo7DQoJCQkJCX0NCgkJCQkJI2xpbmVUYWJsZVRkIHsNCgkJCQkJICAgIGJvcmRlci13aWR0aDogMXB4Ow0KCQkJCQkgICAgcGFkZGluZzogMXB4Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOiB3aGl0ZTsNCgkJCQkJfQ0KCQkJCQkjbGluZVRhYmxlVHIgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAxcHg7DQoJCQkJCSAgICBwYWRkaW5nOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBibGFjazsNCgkJCQkJICAgIGJhY2tncm91bmQtY29sb3I6IHdoaXRlOw0KCQkJCQkgICAgLW1vei1ib3JkZXItcmFkaXVzOjsNCgkJCQkJfQ0KCQkJCQkjbGluZVRhYmxlRHVtbXlUZCB7DQoJCQkJCSAgICBib3JkZXItd2lkdGg6IDFweDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjp3aGl0ZTsNCgkJCQkJICAgIHBhZGRpbmc6IDFweDsNCgkJCQkJICAgIGJvcmRlci1zdHlsZTogaW5zZXQ7DQoJCQkJCSAgICBib3JkZXItY29sb3I6IGJsYWNrOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjogd2hpdGU7DQoJCQkJCX0NCgkJCQkJI2xpbmVUYWJsZUJ1ZGdldFRkIHsNCgkJCQkJICAgIGJvcmRlci13aWR0aDogMnB4Ow0KCQkJCQkgICAgYm9yZGVyLXNwYWNpbmc6MHB4Ow0KCQkJCQkgICAgcGFkZGluZzogMXB4Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOiB3aGl0ZTsNCgkJCQkJICAgIC1tb3otYm9yZGVyLXJhZGl1czo7DQoJCQkJCX0NCgkJCQkJI25vdGVzVGFibGUgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAycHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBibGFjazsNCgkJCQkJICAgIGJvcmRlci1jb2xsYXBzZTogY29sbGFwc2U7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOg0KCQkJCQl9DQoJCQkJCSNub3Rlc1RhYmxlVGQgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzo7DQoJCQkJCSAgICBib3JkZXItc3R5bGU6IGluc2V0Ow0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOiBibGFjazsNCgkJCQkJICAgIGJvcmRlci1jb2xsYXBzZTogY29sbGFwc2U7DQoJCQkJCSAgICBiYWNrZ3JvdW5kLWNvbG9yOg0KCQkJCQl9DQoJCQkJCXRhYmxlIHsNCgkJCQkJICAgIGJvcmRlci1zcGFjaW5nOjBweDsNCgkJCQkJfQ0KCQkJCQkjYnVkZ2V0Q29udGFpbmVyVGFibGUgew0KCQkJCQkgICAgYm9yZGVyLXdpZHRoOiAwcHg7DQoJCQkJCSAgICBib3JkZXItc3BhY2luZzogMHB4Ow0KCQkJCQkgICAgYm9yZGVyLXN0eWxlOiBpbnNldDsNCgkJCQkJICAgIGJvcmRlci1jb2xvcjogYmxhY2s7DQoJCQkJCSAgICBib3JkZXItY29sbGFwc2U6IGNvbGxhcHNlOw0KCQkJCQkgICAgYmFja2dyb3VuZC1jb2xvcjo7DQoJCQkJCX0NCgkJCQkJdGQgew0KCQkJCQkgICAgYm9yZGVyLWNvbG9yOmdyYXk7DQoJCQkJCX08L3N0eWxlPg0KCQkJCTx0aXRsZT5lLUZhdHVyYTwvdGl0bGU+DQoJCQk8L2hlYWQ+DQoJCQk8Ym9keQ0KCQkJCXN0eWxlPSJtYXJnaW4tbGVmdD0wLjZpbjsgbWFyZ2luLXJpZ2h0PTAuNmluOyBtYXJnaW4tdG9wPTAuNzlpbjsgbWFyZ2luLWJvdHRvbT0wLjc5aW4iPg0KCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSIkWE1MIj4NCgkJCQkJPHRhYmxlIHN0eWxlPSJib3JkZXItY29sb3I6Ymx1ZTsgIiBib3JkZXI9IjAiIGNlbGxzcGFjaW5nPSIwcHgiIHdpZHRoPSI4MDAiDQoJCQkJCQljZWxscGFkZGluZz0iMHB4Ij4NCgkJCQkJCTx0Ym9keT4NCgkJCQkJCQk8dHIgdmFsaWduPSJ0b3AiPg0KCQkJCQkJCQk8dGQgd2lkdGg9IjQwJSI+DQoJCQkJCQkJCQk8YnIvPg0KCQkJCQkJCQkJPHRhYmxlIGFsaWduPSJjZW50ZXIiIGJvcmRlcj0iMCIgd2lkdGg9IjEwMCUiPg0KCQkJCQkJCQkJCTx0Ym9keT4NCgkJCQkJCQkJCQkJPGhyLz4NCgkJCQkJCQkJCQkJPHRyIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6QWNjb3VudGluZ1N1cHBsaWVyUGFydHkiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSJjYWM6UGFydHlOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYWM6UGFydHlOYW1lL2NiYzpOYW1lIi8+DQoJCQkJCQkJCQkJCQk8YnIvPg0KCQkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBlcnNvbiI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRpdGxlIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6Rmlyc3ROYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6TWlkZGxlTmFtZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkZhbWlseU5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpOYW1lU3VmZml4Ij4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBvc3RhbEFkZHJlc3MiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpTdHJlZXROYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6QnVpbGRpbmdOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSJjYmM6QnVpbGRpbmdOdW1iZXIiPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+IE5vOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkJ1aWxkaW5nTnVtYmVyIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UG9zdGFsWm9uZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlTdWJkaXZpc2lvbk5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4vIDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eS9jYWM6UGFydHkvY2FjOkNvbnRhY3QvY2JjOlRlbGVwaG9uZSBvciAvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdTdXBwbGllclBhcnR5L2NhYzpQYXJ0eS9jYWM6Q29udGFjdC9jYmM6VGVsZWZheCI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkNvbnRhY3QiPg0KCQkJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSJjYmM6VGVsZXBob25lIj4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PlRlbDogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6VGVsZXBob25lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9ImNiYzpUZWxlZmF4Ij4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiBGYXg6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRlbGVmYXgiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdTdXBwbGllclBhcnR5L2NhYzpQYXJ0eS9jYmM6V2Vic2l0ZVVSSSI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHRkPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PldlYiBTaXRlc2k6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLiIvPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ1N1cHBsaWVyUGFydHkvY2FjOlBhcnR5L2NhYzpDb250YWN0L2NiYzpFbGVjdHJvbmljTWFpbCI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHRkPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkUtUG9zdGE6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLiIvPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nU3VwcGxpZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5WZXJnaSBEYWlyZXNpOiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpQYXJ0eVRheFNjaGVtZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlRheFNjaGVtZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOk5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOyA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ1N1cHBsaWVyUGFydHkvY2FjOlBhcnR5L2NhYzpQYXJ0eUlkZW50aWZpY2F0aW9uIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iY2JjOklEL0BzY2hlbWVJRCIvPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PjogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6SUQiLz4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQk8L3Rib2R5Pg0KCQkJCQkJCQkJPC90YWJsZT4NCgkJCQkJCQkJCTxoci8+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCTx0ZCB3aWR0aD0iMjAlIiBhbGlnbj0iY2VudGVyIiB2YWxpZ249Im1pZGRsZSI+DQoJCQkJCQkJCQk8YnIvPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCTxpbWcgc3R5bGU9IndpZHRoOjkxcHg7IiBhbGlnbj0ibWlkZGxlIiBhbHQ9IkUtRmF0dXJhIExvZ28iDQoJCQkJCQkJCQkJc3JjPSJkYXRhOmltYWdlL2pwZWc7YmFzZTY0LC85ai80QUFRU2taSlJnQUJBUUVCTEFFc0FBRC80UUR3UlhocFpnQUFTVWtxQUFnQUFBQUtBQUFCQXdBQkFBQUF3QWxqQUFFQkF3QUJBQUFBWlFsekFBSUJBd0FFQUFBQWhnQUFBQU1CQXdBQkFBQUFBUUJuQUFZQkF3QUJBQUFBQWdCMUFCVUJBd0FCQUFBQUJBQnpBQndCQXdBQkFBQUFBUUJuQURFQkFnQWNBQUFBamdBQUFESUJBZ0FVQUFBQXFnQUFBR21IQkFBQkFBQUF2Z0FBQUFBQUFBQUlBQWdBQ0FBSUFFRmtiMkpsSUZCb2IzUnZjMmh2Y0NCRFV6UWdWMmx1Wkc5M2N3QXlNREE1T2pBNE9qSTRJREUyT2pRM09qRTNBQU1BQWFBREFBRUFBQUFCQVAvL0FxQUVBQUVBQUFDV0FBQUFBNkFFQUFFQUFBQ1JBQUFBQUFBQUFQL2JBRU1BQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQWYvYkFFTUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBUUVCQVFFQkFRRUJBZi9BQUJFSUFHWUFhUU1CSWdBQ0VRRURFUUgveEFBZkFBQUJCUUVCQVFFQkFRQUFBQUFBQUFBQUFRSURCQVVHQndnSkNndi94QUMxRUFBQ0FRTURBZ1FEQlFVRUJBQUFBWDBCQWdNQUJCRUZFaUV4UVFZVFVXRUhJbkVVTW9HUm9RZ2pRckhCRlZMUjhDUXpZbktDQ1FvV0Z4Z1pHaVVtSnlncEtqUTFOamM0T1RwRFJFVkdSMGhKU2xOVVZWWlhXRmxhWTJSbFptZG9hV3B6ZEhWMmQzaDVlb09FaFlhSGlJbUtrcE9VbFphWG1KbWFvcU9rcGFhbnFLbXFzck8wdGJhM3VMbTZ3c1BFeGNiSHlNbkswdFBVMWRiWDJObmE0ZUxqNU9YbTUranA2dkh5OC9UMTl2ZjQrZnIveEFBZkFRQURBUUVCQVFFQkFRRUJBQUFBQUFBQUFRSURCQVVHQndnSkNndi94QUMxRVFBQ0FRSUVCQU1FQndVRUJBQUJBbmNBQVFJREVRUUZJVEVHRWtGUkIyRnhFeUl5Z1FnVVFwR2hzY0VKSXpOUzhCVmljdEVLRmlRMDRTWHhGeGdaR2lZbktDa3FOVFkzT0RrNlEwUkZSa2RJU1VwVFZGVldWMWhaV21Oa1pXWm5hR2xxYzNSMWRuZDRlWHFDZzRTRmhvZUlpWXFTazVTVmxwZVltWnFpbzZTbHBxZW9xYXF5czdTMXRyZTR1YnJDdzhURnhzZkl5Y3JTMDlUVjF0ZlkyZHJpNCtUbDV1Zm82ZXJ5OC9UMTl2ZjQrZnIvMmdBTUF3RUFBaEVERVFBL0FQNytLS0tRL3dBaC9ubnArSDVrVUFMWGpmeGsvYUIrRFg3UCtnSjRqK0wvQU1RL0RuZ214dUg4alM3UFU3Nk50ZDhRWHJZRVdtZUdmRGx0NSt1K0k5VW1abFdIVHRGMCs5dTNMRDkxdHl3K1VQaTUrMWg0eThkZUxQRlh3WS9aTlBoVjlUOEdYQzZYOFovMm1QSHN5Ui9CTDRBM0UyMUo5SlZwTG13aitKUHhTdDRwNGJpRHdQcGVwMk9sNldaSW44VytJTkg4MjN0YnI4MExuNHhlQ3ZCUGlYeDliL3NoZUdyajlybjl2LzRiL3REK0RmZzk4Uy9pRiswZFlUYWo0cDhRV212Mi9pdVdYVi9odGV5MzJuK0hQaDU4TE5SOFErRE5ZOENIV2ZCYWFQNFo4TFBiVDZucWRyckYzWjZjbXFmWTVUd25pTVU0enhpcVUxYWxPV0hqT25RZENsWG5DblJyNXBqYTZsaHNud3RTZFdtb1R4RWF1SW43U2xKWVZVYXNLNTVPS3pPRlAzYVBMTDRrcWpUbnp5aW5LVU1QUmcxVXhFNHhVbTFIbGdyUDM1U2k0bjZCL0VEOXQ3NDgzM2cvV1BIUHduL1pnMWI0ZmZEYlNZNEpydjR6ZnRjNm5xWHdoOE93V1Z6Y1JXMFdxV253dThQYUo0eStNRnpaUDlvaG5qbDEzd3o0VGpqUlpHMU45TXRFYTlYeUhWUGkzOGR0YjhVeStEUEZQL0JTYjRMZURmR2lSMnQ3Yy9ENzltLzluZlNmRjJ1V21pYWw0TDFUNGhXT3VQZWZFbnhGNDZ2cm53emQrRE5Idjlac3ZHMXZwTmg0ZnZJMGlTMWtGMWMyMXJKNkg0VS9aOC9hbCtPL2dYOXBENGVmdEVMb3ZocjRKL3RRMnQ1NGt0ZkIzeEE4UUw4VHZpOThCci94cDhNOUw4TmVKZmhoNFpPaFR5L0QyWHc3NEw4ZDZXZkdmZ254SEQ0bjFJUWkrdkxhUHc5WnkzVU0rbGZWbmhqOWo3NFhhWHEvd244WmVLNWRZK0lIeE8rRlB3UzFyNEJ3L0ViVzVMUFRkYzhYK0J2RVZyb2xwck1maTYzOFAybW1XRi9mWENhRmJ5V3M4TU5zTlBsdTlUbHMwamUvbVk5TThYa09YVTUwWTBNRzYwWFVpdnFWR2htVGtuaDZGVER6cVl6TktXTHBxcFR4S3hHSHhhd2ZzSVN0U3E0ZURwODNQbXFPTnhEVW5LcHl0UmI5dE9kRko4OGxOS2xoNVUzWnc1SjArZm1rdFlUbGZiNEgrQ0g5cC90Ri9DUHhEOGZmaHgvd1UzL2FoMUg0ZmVHdE5tMWpWZkVqZUNmMmVyTFQwdGJid3ZhZU1McTZUdzliL0RlL3V0UDhqUTc2MXZwOUQxV094MWV6RnhIYjNWbERJeTdzRDRWZkhENDBlT2ZocjRwK012d2Qvd0NDb0h3bjhZL0Ryd05Qb2tmaXUvOEEyc1AyYmZEZmdIUmZEbzhSYVJwMnZhQkRybmlyd2hyM3dtYlRJTmIwalZkTnZMTFdKNGRSaWpndjRwbnRyaHRrQi9VVDRmOEE3THZ3VCtGM3doMWY0RCtDdkRXdWFmOEFDYld2REUvZ3U1OElhbDhRL2lSNG50clB3bmNhQ2ZERCtIdEExRHhUNHQxcldQQytreDZFZnNGclplR3RSMHFDeVFMTlpwQmNJa3kvSlBpei9na3QreVRyL3dBS1BFSHdkME94K0l2Z3Z3ZDRqV1M0MUN3MGI0a2VLOVNndTlYc2ZoMi93eDhHYW5xY0hpWFVOWkdyUmVBUERMQ0x3NW8xN0kyaXozQ3JjNjlZYXhjUlcwdHZwUXp2SUsrSXhVTVhMRzA4TExNS0gxQ3BWeWJJY1k2R1djMHZyS3hXSFdHZ3F1TmxEbFZHZEN0VHB3a20ycEtYdVRQQlkyRUtUcEtqS29xTXZiS09KeGRLK0kwNUhUbnpTU3BMVnlVMDVQb1hvdjJwdjJ3UGhGREhjL3RCZnNsUi9GSHdoOW5ndkgrS2Y3RlBpNlQ0dXdSNmJjeEdhMzFPOStFWGl2VC9BQWY4U1h0cG9ObHdSNFJpOFp5c3JsYkNEVUk0ek9mcXY0RmZ0UmZBWDlwVFNyclUvZzE4U3ZEM2k2NTBwekI0aThNcGNQcGZqandqZXJnUzZkNHc4RDZ2SFkrSy9DOS9FN0NON2JXOUpzblpzbUx6RXd4L1AxLzJNLzJqdmcxOGFyZjQwZUdQakY4Ui9qUDRIaDhIZUVmQ2VyL0J6d2JyT2lmQ2p4RHEyay9CVDRiNmRwM3drc0cxM1Z0V2ZUdFdiWGZIeitOTDd4L2F3NjM0TDBYV05QOEFGK2pqVWJPK3QvQjYydXJmSWVvL0ZYNFhmRnlOdkZ2N2FmZ2U5L1pCL2JDdS93QnI2OS9adStCM3hJL1owdDlXc1BpOTRXdDdqUS9ocGNhVnJ2akh4UnBVbDNwdmp2NGM2UDQ3K0lscjRJOFM2eDRpdHRWK0dldVR2b3R5K2syLzI1cExlbmtlV1pyVGRUQXlvMVpLbGhuT3RrL3RmYXdyMXFWU3BVaFh5TEYxWjR5cEhEZXdxeXhXSndNNk9IcFUzQ3BTb1ZuTDJiU3htSXdyNWEzUEhXZkxIRldhbEdNb1JpNFl1bkZVNHlxYzZWT25XVG5LVithVVZxZjBlVVYrWVB3di9hMytKZndQOFUrRVBnMysydFA0YjFYU1BHK3F4K0dmZ2orMmI0RGpnZytEM3hsMVI1WGdzdkRYeEIwdXhtdjdYNE4vRkM1ZFZzNExLKzFHZndaNHQxSkxpRHd4cTZYMGNta3gvcDZDQ0FRY2c4Z2pvUjZqMUI3SHYxRmZHNDdMOFJsODR4cktFNlZWT1dIeFZHWHRNTmlZUmRwU28xTEozZy9kcTBxa1lWNkU3MDY5S25VVGl2V29ZaW5pSXR4dkdVV2xVcHpWcDA1Tlh0SmJOTmF4bEZ1RTFhVVpOTzR0RkZGY0p1RmZtbisxaDhjL0VQanZ4cHJIN0xQd2Y4YlA4UExQUWZEc1BpNzlyRDlvR3hkUko4QS9oYmV4U3pXSGgvd3Zkc3Mxci93dUw0bFIydHhZZUdMZWFDNmZ3NW9yMzNpbDdTNHVZZEtzN3I2Zy9hcytQVnAremg4RHZHUHhMV3dmWGZGRWNOcDRaK0d2aEdETFgvamo0cCtMYnFQdy93RER6d1pwc0FEU3ozZmlIeFRmNmJZaElZNVpWZ2ttbFNLUm94RzM1K2VBUGhKOFBQRS83TVg3UnY3TEZ4NGo4UmZFajlwSzUxL3dqNDAvYWcxejRXZU52Q25oMzRtNmg4ZnZHbW8rRS9pQk5yM2gyODFYVkpWMFRUdmh4UGIrSHJYUmJmVzdHTFIxOEwrR2JmUVk0ZFhuR293VGZWNUJncWRDbC9iV0xwVGxScDRtamg4TkpVbFZoaDVPdmg2ZUt6V3RDZHFVcU9YTEVVVlJoV2txVmJINGpEeG56VXFWYUV2TXgxWnprOEpUa2xKMDVWS2k1dVYxTkpPbmg0TlhrcFZ1U2JtNCs5R2xDYmphVW90ZlQxNyt6eCt5dDhUZjJkbC9ZaXNmQVd1Nlg4SnZIM3duMUhXRTArRHd4NGkwdTYwYTFOM29VaStJTmY4QUUycmFXVjB2NHRUYXo0aTA3eFhIWitMSm04V2ExZVJhbHJHb2FkZldsdHFSSHRuN1BmN01Yd2cvWnM4RmVGL0Nudzc4R2VGdFAxUFFQREZ2NGExRHhwWitFL0RXaCtLUEUwZjIrNjFyVTd2Vjd2UXRNc0VWTlg4UjZocWZpQ2ZTck5MZlI3VFVkUnVHc0xHMWoyUnIxZndhK0VlbWZCM3dwTG9OdjRpOFVlTmRkMWpVbjhRK05QSDNqaSt0TlM4WStPUEZNOWhwK2wzR3YrSUxyVDdMVE5NVzRHbWFUcFdrMlZqcE9tNmRwV2w2VHBlbjZkcDlsQmJXcUxYclZlUmk4eXhVNFY4SFR4K01yNEdwaXA0cWNhdFdweTRuRlRTalV4VTZjbmZucXhqQlNjN3lrb1FsTmN5U2owMGNQVFRoV2xScFJyS25HQ2NZcTlPbXRWVFVrbGRSYmJ1bHB6TkxUVm96S2lzekVLcWdzek1RRkFBeVNTZUFBT1NlMWZ6cmY4Rk92K0NrTi9IZGF2OEFBdjREZUs3M1FFMGE0OHZ4ejhSL0QrcTNlbDZoSGUyNGpsT2grRzlYMDI2Z25nOGg5eWFuZXd5QmpJcldzVEFDVTE5amY4RlRQMnluK0FIdzNqK0dYZ2pVbHQvaWY4UnJLNGlXNWdrak0vaHZ3dS9tMjEvcXpLZHpSM04weXZaNmVTcWxYTXM2dCs1citLdjR1L0VXYTZubjBld3VYZFRJN1hjNWZkSlBOSWR6eVNPY3M3c3hZc3hKTEVrbk9hL0RmRWJqS1dYd25rdVhWSEhFU2l2cnVJcHl0T2xHVm5IRDA1SnB4bkplOVZrbW5HTFVWWnQyL3dCUnZvSi9SVW84YllqQytLM0hHWHd4T1RZZkVTWENlVVkyaXFtRngxYkR6NUsyZDQyalVUaFh3bENwR1ZIQVVLaWRPdlhqVXJ6aktGS2x6ZTg2eisyZiswTEZlWEFqL2FzK1BLb2p2eEg4WHZIZ1VZWWo3cTY3eDB4MHh4NlY1TnJ2N2ZuN1Q3MzF0b3ZocjlwVDlvclY5WXY1NDdPeHRiVDR0ZkVLYWU1dVozRWNVVVVFZXZGNUhaM1ZSOG9HU0RuQU5mRUhpUFdib1N3Nlpwa1U5N3F1b1RSMnRyYTI4YlRYTnpjenY1Y2NVVWNlWGtlUmpzUlZYcVFRY1lOZjBxZjhFci8rQ1h1bitEOVBYNDZmSFd5dGYrRW1qMDV0Y2xHcXFSWStDZEhoWDdYS0dFeEVJMUlRUitaYzNEcis0NWpqWmNNVCtZOE40TGlEaVRHZXlwWmpqYUdFcDJsaXNTOFZpT1NqRFJ0WGRWSnphVGFqcGRKeWRrbmIrL2ZwQThiZURQZ0R3NURGNHJnamhMT09KTWRmQzhQNUJEaDNKSGlNeHhyNUl4YmhEQXVjTU5UcVRnNnRTemJjbzBvUmxVbEZQM1QvQUlKbi9CTDlyYnhKNG04T2ZGTDlvNzlwRDlwRFVWamVIVk5JK0hDL0YzeHhjNkdxU3dTR0pmRnR2ZWF2UEhxREVTSTRzRkhrUnN1SmhMZ0FmMEZmdEJmc3MvQ3o5cXI0WitJdkEzeENzTlEwUy84QUV1aDZkb1krSXZnMyt5dEYrSitnNmZwdmliUS9HRnRiK0h2R04xcEdwM3VseC84QUNRK0hOSDFLU0pJNVlqZDJOdmV4SkhmVzFwZFFmaVQ0cy80TFJmQXo5bmo0cWFENEswZjRSWHVzZkM0Nm9kSDFYNGhSYXJEYjM2eFF5L1pXMWpUdEphM2RibXdSMldZckplMjhyMnhhUlVMaFViK2pMd1g0dThQK092REdoK0xQQzk3RHFHaGVJTkxzdFgweTdnWU5IUFpYOENYTnRLckFuNzBjaWtnbklKSVBJcitodUNjeXkzQktWTGgzTnE5WEdaWFhwVHJZbjIxZVdKamlJTlNoV1ZXcS9maTVSOTEwNzB0TEpkLzhWdnBKWkQ0czFzMnlqaTd4VDROdy9DdUM0dXdkYXZ3N2djRGdNcndHVjBjREdTbExCVThIbGlVY0ppS01hc0pWYVdNaXNaSlRWU3BlN3QrTTF4QjhNZjJYZmdKOGN2aGIrM0RheitKL0IzeEU4ZGFYOEt2Zzkrekw0VjB3ZUkvQzEvOEFDVFJwdEwwSHdIWi9zMytFTGRycnh4NHE4VnBwR3QyWGl2NGo2MVBIQjRuZytJMW5jdmJlU3RocEd0NnQ3cCt6TDhWUEhQN05QeFg4TWZzV2ZIbnhQclBqYndaNDUwTzY4US9zWS9IdnhWNThldmVOL0JtbTJjVjFjZkE3NHJYZCtsck8zeG84QjZXUHRXbmFsUGEya25qandta2R6TEJINGkwclY0WmZ1ZjQzL0NhMytLWGhEVUJvNTBuUlBpcG9HZ2VOQjhIL0FJa1htbDIrb2FyOE12R3ZpandocS9oU0x4Um9jc3NVczFyTWxwcXNzRjZzSC9IMVpzOFRwSmhBUHdxOE5mc3hhNzR0OEthOThLUGp2OFJQRnZ3UCtKZmlpLzAvd24reWZwUHhSK05lbGZGYjRuMi83Ukh3Y3V2R1h4QjhML0ZyUmRabmZYL0VWbDRha25PcTZ2NGUwbC9GR2xHN3R2RjNqdlFiM3d5bmgzWHZCSGgzdy84QXRlQnJZTFBjQmpYamF5cFZLbFIxY2ZSVnFzNFYzQ0ZPaG1lVzRXbFRoT2pUd2RDaktwbUw1c1JMRlVmckt4VXFMaGhhNS9LRmFGYkExNktwUjVvcFJqUm0yNEtVTHR6dzlhbzIxT2RXYnRSVm9xbkwyZklwZS9GLzBlVVY4bC9zUy90RTMzN1RIN1AzaGp4MTRvMHVQdzE4VXRCdjlkK0hIeHM4RmpDWEhnejR2L0Q3VkxqdzE0MzBXYTMrOUJhM09vMkkxN1FpNEF1L0Rlc2FQZlI1aXVWTmZXbGZCWXZDMXNGaWNSaE1SRlJyWWF0VW8xVW56UjU2Y25GdU10cFFsYm1oSmFTaTFKYU81N2RLcEN0VHAxWU84S2tJeWo2TlhzMTBhMmE2Tk5INXMvR1ZSOGMvK0NnWDdPL3dVbHhQNE8vWnE4RDZ6KzFyNDJ0eVBNdDdyeDVxTjlQOE0vZ25wMTdDK1l4SmFUWG5qdnhmcDBySzdSWFhob1NxRW5qdFpsK2wvQ243STM3Ti9nbjRwMjN4eThML0FBajhKNlY4WklOUDhWYVhQOFQ3ZTFtWHhyck5uNDAxZVhYZkVVZmlYWEJPTHJ4UkplYXBQY1hGdmMrSVcxSzYwdExpNXR0S21zcmE2dUlaUG1mOWtrbnhmKzJqL3dBRkh2aVhPQzdhWjhRdmd2OEFBL1NuT0NMZlRQaHQ4S2RQMXUvdEZQVWg5ZDhiMzk4eThCWHV5Tm96ay9wUFh0NXppTVJnNTRYTGFGYXRRbzRiS01CUnJVcWRTZE9OV3BqTU9zeHhhcktEaXFzWllqSFZZZS96SjBvd2k5SXBMa3drSVZZMU1ST0VaenFZbXRVaktVVTNGVTUreHBjcmF2RnhwMG9iZmE1dGRXRllmaWJ4QnB2aFB3OXJYaWJXYmhiWFN0QjB5OTFYVUxsODdZYlN4dDN1SjNPQVQ4c2NiRUFBa25nY2tWdVYrWWYvQUFWdStMMDN3dC9aQjhXNmRwOTE5bTFqNGozK24rQ2JNcklVbE5uZnpyTnJEUkVNR0JYVG9abEpYT1BNNXdEbXZqYzB4c010eTdHNCtkdVhDWWFyV3M5cFNoRnVFZjhBdDZmTEg1bjZENGVjSlluanpqbmhQZzNDY3lyY1I1OWx1VmM4VmQwcU9LeE1JWW12YmI5eGh2YTFuZlMwTldrZnlwL3R1L3RMNno4YVBpbDhRZmlycWwzSS93RGJtcVhlbStGN1ozY3g2ZDRYc3JtNGgwYTBnUitZMSt6RVhFcUFLRGNYRXJIT1RYNUxhOXF6UnhYVi9jT1M3QjIzTnlTY0gxeitQWEErZ3IzRDR2YTAxenFVR21vNThxMmpHNFp5TnhMWjYvamdlbWNZeFh6N0g0ZjFQeDU0djhNZUFkRmphYlV2RSt0YWRvdHRIR3U1ak5mM01VR1FBTnhDQ1F1Y2pJQ2s0OVA0OHgySXhHYlpuT3BPVXExZkZZaHR2NG5PclZtcjJTYjNrK1ZMcG9rbHNmOEFVYnd4bE9SK0duaC9oY1BoS1ZITHNweURKYWRHakZLTUtlR3kvTGNLa205RWx5MGFVcWxTVGZ2U2NwU2NtMjMrcFA4QXdTSS9ZMm0rT3Z4SWwrTm5qSFJaTlEwRHc5cUxhYjRLczd1Slh0THpWd0FiblZIamt5SkYwK045dHNTb1VUdVhCT3dWL1ViL0FNRkdyaTUvWjMvNEo4L0VTODhQTExaM09xTG9maGpWTHExVXJNbW1hOWZKWjZpQzhYektrdHU3UXUzWldPVDJyNVMrQlh4Ly9aWC9BT0NjWGhUd1Q4SGZIR2tlTnJ6eEg0ZThGZUg3Ni9QaFB3OVphdGFXOCtwV0VVN3ZkeXphcFpUaSt1SmQ5eklwaEpXT1NMTGs4SDBqNDBmOEZYUDJBUDJrdmhONDArRUhqblJQaTNONFk4WWFOYzZYZUxMNFBzTGE0dFdraVlXOTlheXZycmlLN3NwaWx4YnlZTzJSQWNFWkIvZmNDc2h5UGgzR1pGRE9NQmhjMXE0T3ZTclNxVlZHcEhHMUtUVWxOcGFjczJxYTF2R0tWdGQvOFZlSjRlTTNpMzQ3Y0wrTWVOOEwrTStJdkR2QThWWk5tbVZVc0hsMDhSaHNSd3BnTXhwVmFEd2RPYytTVHhPSGc4WHFrcTlhbzIvZDViZnhYL0h6NGdTK01kUTBuVE5MTWx5NVNPenRJSTBZeVRYVjFOR3FxcTRCTE0rMVY2Y25uMUgraFYvd1RIWHhMcHY3TFB3cDhPZUtwSjVOVzBQd1JvZG5jaWNreVJ5SmFSTjVMWko1Z1ZoRWVlQ3VDT0svbEMvWkcrQm43RUh4RS9iQzBid1Q0QzFmNHAvRUx4R3Mrc2FwNFZ0L0YvaGpSdE84TzZaYmFOYnoza3R4cVV0bnF0M05jWE52Q29FRWd0ZkthZFVKalRPUi9icjhHL0FrSGdid3ZaNmZDcXFSQWdiYU1LZUZ3QU1EQUczMHJtOEw4bHFZT0dOekdwaXFHSW5pWktnL3ExV05hbkZVV3BTNXB4WEs1dHlpK1ZOMlRWM2R0SHQvdEN2RmpEY1ZacHd0d05oT0g4NXlYRDhQMEpadEQvV0RMNVpiajZ6ektuR25UZExDVlc2dE9qQ0ZHb3BWS2lnNnRTL0xIbGdwUzlncjV3dWYyU1AyZGIvNDY2cCswbHEvd284SDY3OFk5UzBud3BwVVhqSFg5RjA3V3I3UWo0T3ViNjUwdlZmREQ2bGJYTCtHOWN1VGRXY09yYXRvNzJsMXFjR2dlSGt1WFp0SmdjL1I5RmZzbEhFWWpEKzA5aFdxMGZiVW5ScSt5cVRwKzBveWxHVXFVM0JybXB5Y0l1VUhlTW5GWFdoL21iS0VKOHZQQ00rV1NsSG1pcGNzbGRLU3VuWnE3czFxajh2Zmg5SC9BTUtCL3dDQ252eGUrSDBRRmw0RC9iVStEK2svSHJ3M1pJQkZwOXQ4YVBneEpwbncrK0o2V051bUkxdS9GdmdyVS9CZmlUVm5WRU11b2FKZDMwMGsxMXFrcEg2aFYrWkg3ZHFEd3ArMFgvd1RTK0xkdU5sMW92N1ZPcWZDRFVKUUFyUDRiK1BId3c4VWVHWjdQZUFHQ1MrSzlHOEdYQlFuWS8yVGxTd1FyK20yUjcva2Y4SzlmT2YzMkh5VEh1M1BpOHFoUnJPOTI2dVc0aXZsc1pTZldVc0poc0xKdTJyZXJsTG1aeDRQM0o0eWd2aHBZbVVvTG9vMTRRcjJTNkpUcVQ2djVLeCthZjhBd1Q4bkVYeFEvd0NDa09qM0ROL2FWciszYjR3MWFXTnlDNjZicm53cCtFNzZSSm5yNWNzVmpjZVVDT0VRYzVOZnBiWDVkL3M3emY4QUN2UCtDbUg3ZXZ3dXVqOW50dmkzNEUvWjcvYVg4S1FNZmx1b0lmRDkvd0RDTHgxSmJIT0NiSHhCNFgwaTQxQVlESTJ1MkJZbEpFeCtqK2crTXZDWGltNzF1eDhOZUp0QThRWGZoblVuMGZ4RmJhTnJGaHFkeG9XcnhvSkpOTDFlQ3lubWswNi9SR0RQYVhpd3pxcHlZeGlqaVNTZWFScXRwTEY1ZmxHSm9wdFhsQ3BsT0Rsb3VyZythTTByOHNveVRkMHpYTEtGYVdEcXloU3FUcDRTcldqaUtrS2M1UW8zeFZTbkIxcHBPTk5WSnRSZzV1S2xLU2pIVnBIU24yL3oraC9sWDg0UC9CZmp4b1lJUDJlZkE2ek1xejNmakx4UE5EdXdyaTFnMHJUWW5aZjR0cHVuQ0U4QWxzQUhtdjZQZWUzNS9qN2crLzhBazVyK1YvOEE0T0RoYzIzeFYvWnl1MjNDMG44RitOclZXSkd3WEVXcjZQSXkvd0IzYzBjcUU5TWhldkhQNVo0aDFKVStFczBjSGJtZUVoSzM4azhaUWpKUHljWC9BRXJuOWY4QTBHOERoOHcrazE0ZVVzUkdNbzBZOFNZdWtwSk5mV01Od3htOVdpMWZhU21rMDkwMXBycWZ5LzhBak83YTYxL1VaU2M3WlhVRTRKQVhJeHdTT01kT3h5SytpLzhBZ21ONERIeEkvYmc4QUxjV3EzVmw0VGU2OFVUTElwZU5KZFBqMjJwWVo0M1NPQUMzeTd0cElKMjE4d2VJYy8ybnFaSTZ6VG44Q1dJLyt0WDZiLzhBQkNuU0l0VS9hOThhVFNxQzlsNE10VEVyY25FK3NSUlAyUEJYcjBPT005YS9ubmd6RHd4UEUrVjBxbXErdHhxTk8xcjByMUZwMWQ0K255My9BTnUvcFo1emlPSHZvOWNlNHJCeWxUcXZoeXBnb3lpMm5HR09uUXdOV3pUVC9oVjVyU3pzM2Zxajc3L2FyLzRKaGZ0bC9GajQyZU5maWZwZnhNOEcyK2orTXRXRnhvV2pMRnFyTnBlaFJwSGJhWll5N3Jab2c4RnNpSzZvU203Y1FjWXI4TFBIbi9DWitBZFI4WCtHZFYxS3cxRzU4TWFycUdnWEdwMlVSU0M2dWJHZVMwbmtneXFOdDgyT1JSdVVFWXllcE5mNlFIaXR0SThNZkRuWFBFdC9IQkhENGY4QUMybzZtMDBpcmlNV2VuU1RCamxUdCthTUhPYzg5YzhWL25HL0h6V2Y3UnM5ZTErVkVqdS9FMnY2cHJFNnFmdXlhamRYTjY0endTQThweGs4Z0RtdnRmRXZJY3N5ZVdEcjRPTlpZek1hdUt4R0psT3ZVcWM2VHBYdEdVclI1cWxXNmFpdmg1Vm9qK1Vmb0FlTW5pRjRuME9LY240cXJaWlg0WDRIeXZodkplSDhMaE1vd1dBZENwT09MUzVxK0hwUW5XZExCWmZHTFZTY25lcXB5MWtqN0cvNEliYU5mNi8rMko0ajhXS3JNM2hud3RMREZjRlNjVGExY05aeVJxL3pZWjdjeU13UDhLODR6WDk5bWhxeTZYWmgvdm1GTjMxd0IrbU1mL1hyK01QL0FJTjNQQWpYdXIvRlR4bk5BcFc5OFNhUnBkdE1WQlBsV1ZsZFRUSXBPY0w1c2lad2NaQTlTYS90S3RVOHUzZ1FEaFkxSDA0L3AwcjlMOE9NSzhOd3RnVzFaMTNWcnZUViswcU96ZjhBMjdGSCtmbjA1ZUl2OVlQcEM4WHRWSFVobGYxREthZXQrVllQQTBGT0s3SlZxbFYyMjVuS3hZb29yeno0aS9GbjRhZkNMVGRMMWo0bitPUERQZ1BTTmExcTE4T2FYcW5pclZyUFJkUHU5YnZZTG01dGRPanZMNldHM1c0bXQ3TzZtVVBJaWlPQ1JtWUJhKzZuT0VJdWM1UmhDT3NwVGtveFMydTVOcExYVFZuOGk0ZkRZakdWNmVHd2xDdGljUldseTBxR0hwVHJWcXNyTjh0T2xUaktjNVdUZG94YnNtN2FId24vQU1GS01UUWZzUDJFUkJ2YnYvZ29mK3lkTmFSZnh5eDZWNCtpMWZVeWhJNEVPbFdON2NTY2pNVVRqdmcvcGZYNWkvdFlYVVB4SS9iWC93Q0NjbndrMDZhSFVMUFFQR254Vy9hYjhSTGJ5Q1dLUFIvaHg4T2I3d3A0UnZaR1FtT1MxdWZFL3dBUUlwcldRRmtOM3A4REljbGMvcHprK2gvVC9Hdm9NMGlxZVY4T1UyLzNrOEJqTVZLT3Z1d3I1cGpJVWIzdDhjS0h0RmJSeG5GcHU1NW1HdThUbUVyTkpWNlZPNzZ5cDRlbHorZnV5bHl0UFpwN081K1VmN2ZNci9zOWZ0QmZzZy90MFc2UGIrRS9CbmpDOS9adC9hRzFDSlQ1T21mQlA0OVhlbTJPbCtMOVljWVdQUlBBSHhOMDN3eHJHclRPUXR2WVgxeGVmTzFrc1VuSy9zN2ZEclNQMldmMnVOWDhNZUsvR1B3VThCd2ZGcTU4YW40VmFacE9xWEgvQUFzdjQvYUhyR3QzUGpSZGE4Y1JycGxscHJhcjRNMUxVWmRJOFBhbHFHcjZ6cTJxaTkxMnkwcjdCcDAxbnA3ZnAvOEFHSDRWZUR2amw4Sy9pRDhIZmlEcGtlcitDdmlWNFIxM3diNGtzSkFOMG1tYTlwODloTk5iU2ZldDc2ek15M21uWGtSU2V5dnJlM3U3ZVNPZUdOMS9ETDRYK0hmRVBpU0hWZjJhL2pMNGIxajRnL3R2ZnNCNmZwdHY4S3JaZkYxbDRBbi9BR3FmZ0ZENG8wVFZmaEQ4UWg0dXZvOXFhZlkzWGhyUnJUNGgyMXRkRzd0dGEwWFVyRFVUbnhLQzNEbW1Hbm0rUllMSFlhQ3FacHdvNXdxMHZmYzYyUjRtdjdYMjBZMDR5cVRsZzhSVnEwYW5JcFNqR3RndmRsU2hVaWZjOERaelF5M0g1enczbW1LcVlUSWVOc0pIQ1Y2MUpZVytIempDMDZ2OWwxWjFNYlZvNFNoUWRlcCsvcVlpcENuSEQxTVhOVmNOVlZQRlVQNkZQVHFNbi9INi9YL09LL25GL3dDRGlMd1RkM0h3dCtCSHhMdFlDOEhoZnhwcldoYXJPRlA3bTE4UWFmYU5hNzJDa0FOZDJJVUJtR1NjQUh0K3VQN0gzeDgxcjR4K0d0YzBueFY0ZzhPK08vR2ZnalY5UzBmeHY0MytIbWpYZWwvQ3lMeFdiK1c2dS9BUGhIVWRVdlpyenhYUDRGc0x6VHRIMWp4TlpRTHB1bzM4VTBqTFkzaGwwK0xpditDblh3R2I5b2Y5akg0eGVDYksxRjNyOWhvTGVLL0RLQlN6L3dCdCtHWFhWTFpZOEVOdWxTQ2FJaFQ4d2NxYzV4WHcvRXVHV2VjTFpuUnc2Y3BWOEZLclFpN09YdHFFbzE0UWZLNVJjdWVseU96a3IzU2sxcWZyWGdEbjlUd2gra1I0ZTVyblU0VWFHVWNWWVhBWnBXWFBDai9adWNRcVpWaU1TdmIwNk5SVUhoTWU4UkYxYVZLYXBwU25DRHVsL25vK0pFemZ6U0xnZmFFTWluSVAzeG4rby9LdjBlLzRJZCtLN0x3dCszSGNhSmVnYi9HSGhDOHNiTWxnb0Z4cDl6RGZqcXdCTEtyQUQ1bXkzQUJ6WDVvYW5xY0NLTGE4Wm9MMnllUzF1SXBRVmRKSUhaSkVjSEJWMFpTR1VqSVlFRTlLOUQvWk8rTGtId1IvYXkrQ254TVc4RVduYVg0MzBpMjFkbGZDblNkU3VFc2IwU0huRWF4ekNSL1FKazQ1citZdUdNV3N1NGh5ekZWUGRqVHhsS05SdFc1WTFKS25POTd0T1BOZHEvUnJxZjhBUVI5STdoZVhIUGdoeDNrR0NsSEVZckY4Tlk2cGdZVTVweXIxOExSampzS3FmTGUvdHA0ZUVJOUc1cnBxdjlBei9nb1Y0OC80Vi84QXNTL0dQV29waERjMzNnLyt3TEZ5d1VtNjE2ZTMwNk1MbGxKY2k0WUtGSlBQRmY1ODN4L3Z4RFpXVm1HSUVjRWtoVUU5U3BBQlBKeWNuZ2tlL2F2N0gvOEFnczU4WXRHc1AyTlBoMW82NmhHdHI4U2ZGZmgyOWh1VWsvZHk2ZHBGaWRiV1Q1VDg4Y2ptMklBSXlUeURqRmZ4SS9HL3hUcCtzYWpNYks1V2FFSWtFWkc0YmowT01qT0dKeDBHUU00d1JYM1hpcmpWaU04d3VFaEpTV0d3T0hTU2FmdlZweXF0OWJXaTZiZnkwUDQrL1p4Y0x6eUh3YTRqNGt4TkNWS1dmY1Y1eE5WSndjRzZXVTRUQzViVGh6TldiaFhqaTNiVGxmTnAxUDYzUCtEZXY0ZmpTZjJlN0R4QTBiZVo0bDhSYXhyRHV5bkpqMy9ab0NDZXFsSTJVRUFkTUROZjA5QVlBSHA3WXI4Wi93RGdqZDhQeDRNL1pXK0UxbTF0OW5sSGc3U3JxZGRoUXRMZnd0ZXM3RHB1WkxoTTV5VDE3bXYyWnp4azhmNTk4ViszOE40YjZwa2VXMEdyT25nOE9tdjd5cFI1di9KbS9PKzc3ZjVEK04yZXZpVHhXNDh6bm5kU09ONG56aXJUazJwWHBmWGEwS05tbTAxN0tNRXZKYkNFNEJQb0QvS3Z3LzhBMnNQaVArMGo0cS9haThKL0ExZmhmNE0rTG53TDhTZU0vQnNtbytIZkdYd2d2ZmlGOExkUThINjFxWjhPK0oyWDRzd2FQYmFiNE8rSkhnS1B3OXFIaU5QRDJwTGZYalA0c3Uwa25rMFBRWWRTcjdnL2JPL2FLOEsvREh3NXAzd3owNzQxSjhHL2kvOEFFYTYwM1R2QW5pdFBCY3Z4QjA3d3JxRStzNlpaNlZxSGpyUllJWjR0SjhJZUl0WXVySHdqTnF1b05acDUrczRzYnFLNWhNOXY4TmVNckx4bDhBUGgzQit6L3dEQ2ZRZkR2aGo5dlg5dkRWN3VYeFJvWGdIeGI0cDhUZkRiNGIyamZiTlArSlg3UnVtYVJyVFJEd2Y0ZDAzUjVwOWZ1Yk95aDA4YXA0enY3SFJiZS91cnFHMWxIbzBzdnI4UjVuaDhsd2RlV0hqQ3BIRVpqallWSXFqaE1MUmk2dGQ0cGU5YWxUb1hyMW8xZVNMcEs4UGJTVTZTdzRheFdINEN5YXZ4cm5HVjRQTWErYVlYRTVad3psR1pZUEV4cVlpdFdsR2s4N3dPS2s4UEdFY05VVTZPSHhlWFN4bUlwWW1FcWRiK3puWHdlTHFmUVA3SHBYNCsvdFpmdFZmdGZRSWsvd0FQdEIvc2o5a2o0QVhhNGUxdXZESHd2djVkUytNZmlYU0pZeWJlZlQvRW54U2VIUTB1TGZjb0hnSmJVc3NzTnlwL1VXdkp2Z1Q4R2ZCMzdQWHdmK0h2d1Y4QTJ6VzNoUDRkZUdySHc5cGhsQy9hcjZTQU5OcWVzNmk2OFRhcnIyclQzMnQ2dGNITFhPcDZoZDNEbG1rSlByTmZRWjFqYVdPekNyVXcwWlF3VkNGSEJaZlRscEtPQXdWS0dHd3JtdEVxdFNsVFZiRU5KYzJJcVZaMjk0L0tjTFNuU29wVlh6VnFrcFZxOHQrYXZXazZsVnB1N2FVNU9NZjdrWXJvRmZDWDdhZjdJV3AvSHkxOEdmRnI0TWVLb2ZoUisxdjhDYmk5MXY0Ri9GWXd2SnB6dGVvc2V2OEF3MitJdHBiSjlxOFJmRER4ellyTHBldmFQNWl5V00wOE9zMkdiaTJrdDd2N3Rvcmx3T054R1hZcW5pOExOUnEwM0pXbEZUcFZhYzR1RldqV3BTVGhWb1ZxYmxTclVacHdxVTV5akpOTXV0UnAxNmNxVlZOeGxiVlBsbEdTYWNaeGtyT000eVNsR1NzMDBtajhkdjJRdkZ2d3MvYUsrTjF4cm54QWorSWY3UFg3WTM3UG1pZjhJOThRdjJUWS9FOXY0YzhEK0ZIdTlTbTFEeFA4UmZBZmgzU2JPMXRmaUg0QStLbDdmV04zUDRzbXU5YXRaNDdiU29wWTlMMWJ6THErK3QvaDMrMWhvSHhlK0xQeFU4RmFScDJtRDRQZkR1VzM4RjNmeGExTFZkT3RQRC9pYjRuWGtPbnpYL2dMUkZ2cjIxbnY3L1JyVytsajFRV3RoZVdndWd0bjl1anZFbHN6Sisxait4TDhNdjJwWS9Ebml5ZlUvRUh3cStQUHcza2UrK0VYN1FudzN1aG8vd0FTUGgvcUlFeFMyRjJtTGJ4TjRTdkpaNURyWGd6eEZIZTZIcWNVa2hNRnZkK1ZkeGZrWCswYlovRmZ3ZDRjc3ZoNy93QUZFdmhOcjkxNGEwSFdkZDF6d3orMzUreUg4UExmeFo0T2wxanhCNFl1dkJkLzR3L2FFK0JwMExWcm53WDRqT2dYbHVxK0o0ZE4xclI5TzFxMWd1ZkQycTZUSlpXY3R6MTR2SmFlYXhlTDRUaGg2V01sVWxpY1p3eldxeHBWOFJXY1ZGd3lyRTEyNFl6RFMrS0dHYldZVStTbmgxR3RTaExFeityeUxQOEFMOFJpVmd2RURFNWhVd3F3ZUd5cktlSmFVSjRxSER1RnA0bU5lV0txNWJoM1JxVnE2dEtrcCsxbFFnc1ZqTVpLaGlNWEtsQmVHL3RHZjhFR2ZoRjhSL0gzaWI0bmZEYjRvK01MZnc3NC93QmF2L0ZGblllSEkvRE9wK0hyUWF4ZHkzY3NXaVgwRURyY2FmNTBraHQzRXNxaFNVM0VLQ1BuQmY4QWczcjBScm1HVC9oYVh4TlV4T3JLeTZaNGZ5cktRUXl0OW1BREt3eU1jWjdnOVAyUStCSHhGK0tZMU81MXo5ay80aS9BNzlvRDlqejRmL0IzeExwL3cxK0cvd0FLZkUraStJZkZjdC80UDhGK0c3RDRjZUV0ZTBxOFcyOFYrSHZpQnFuaWlUVzdyeFhjWEdxdHByNlpEYnhhaHBkdDRpdmZOVDZLdXYydnZpTjhPZkdYd1IrRi93QVlmMmVyNGVOUGlmcFhoUzk4UTY3NEoxTHlmQXZoM1VQRmZpS3gwQmRCMGpVZkZrR21qeEw0ZzhNTGZEVlBGK2hXZC9IcWRscDhEemFMYjY4WmJkSmZ5eXZ3bHc1UXIxbzVwdzdVeTNGeHJTalhwNG5DWWlINzZkU01YS0RWMm8xS2tuS0hOR25KUmkzS01GcS82b3BlUG4waDQ0VENZTGhieGhseE5sVlBMS1ZYQjA4TG5XVnJHNGJMc1BnNVZ2cXVQd3VQbzBLa2NYZ01IU3B4eHNhYzhUUzl0VWhSbzRqRVRrMHZpZjQ3ZjhFdXJuOXBmNENmQkQ0YmVQOEE0eS9FeUEvQXp3eko0ZjBtYXlzdENlWHhHelJXOEZ2cXV0cGMyY2dHb1cxbmJKWm9iVm9vakRrc3JPU2EvTUc3L3dDRGVyUUxqVUk1VytKM3hLbWlpdW81QWttbStId0pWU1ZYS3VmczJRR1VZWWdjQStvcitoZlJQK0Nnbmc3eG5CYlA0VStIM2k3U1RaZnRMK0EvMmY4QVg0dkVXazJHb0dTTHhvK3RMYmVKTk11TkI4U3ZZUmFkTEZwSzNhWHozbW9TV2xwY1c4dHpvOHh1WTFURS9hOCtPbjdXUHd6K1BIdzQ4RC9BVDRNemZFRHdWcTNoclR2R0d2M3RwNEo4VDY1L2FrK2wrUGRCMHp4SjRDSGl2VDdhWHd2NE4xclcvQjk5cU4xNGIxVHhUZWFWcFZyZDJrdDdmM2pXMXNiVzUweGVSOEo0dm16R3BsOGNiVWk4UFJsVXAwcTFhcTdKVWFOb3FYdktLcHFMc3RMV2V0MC9KNFo4VnZwSThPeG84RFlMalhFY0tZR3JETzh6b1pkajh4eWpMY3VwdWMvN1R6U1h0ZlpTcFFxNHFlTytzd1RtbFVWWk9EakNONC9TMzdLdnd1LzRWRjhNOUE4TFRreFFhQm8ybTZWRk5Oc2pKdHRMc1lyT09TVWhValVtT0ZXY2pDZzU0QXJ5cjRpL3Q5L0M3Ui9qTHJYN0xYaCs5dk5IK1BWN1ozRnA0TkhpelI1TGZ3cHErc2FyNGJzOVg4RzNHbDNhWHNKMTZ5OFMzMStkTjB2eUo3R0dhNjBYeEFidTdzclhUbG11dm5QNDRXM3h0dTlWK1BscisxbDhldmhWOERmMlAvRW5oYldORjhNNmRyM2piUnZDdml5MjFDUFZ2RC9pRHdacitsNm40WGc4TytKSklrZTIxUHc1NHIwQzk4WVN6YTFGNWR0WTJPb1d0L0t0ZUwvcy93RGpUNHRlT2ZDZmc3d1grdzE4SzI4WGVKZkQzZ2ViNGE2dC93QUZFLzJodkJlcytEdkFrUGdrK0liM1dJZEorRnVpNnpCTjQwK0xscG9OemNRUDRmc2JQN0o0TUZ4cDBFTjlxVm9wbEZ0OXRsMlRaOW0wSVBCNFQreHNub1M1TVRuT2JwWWJDUnAwcHlwVHBVWnVjVzZsU21vMXNOS2k4UmlhaVRqSENPWExmOFJ4YjRLeUg2N21mRVdjME9NK0k4ZFJwNGpBWkZ3MWlLdjFmQzQzSDRQRDVoaDhibWVZWW5CdWxpNDRIRmZXTXR6bko0VU1QRlZaVTZsRE5LbFBuaXR1NThXZUovZ0ZhZkQ3NGsvdFcrR05MK09QL0JRZnhWZitNTkEvWmcrRG5ncE5QYjRuM1BoN3hVdGpPL2cvNGxYM2cvVXYrRU0xcndsNFExT0dmVzV2RmQ5YkR3MzRQMDFaYml4dnB0Umd1TCt2dmI5a1Q5bHZ4UDhBQy9VZkdQeDYrUDhBNGlzZmlIKzFmOFo0YktUNGhlS0xHTmo0YStIM2htMjIzR2pmQmo0VngzRVMzVmg0QjhMVHRKTE5jems2ajRwMXg3ald0U1pJUnBlbmFiMFA3TWY3R25nZjluZlVQRVh4RDFqeEQ0aCtNbjdRM3hCZ3QwK0p2eDkrSWNxWG5qRHhHc0ROSkZvbWdXTVIvc253SjRLc3BISTAvd0FKZUY3ZXpzZHNjTStxUzZwcUNHOWI3RXIyNVZzdnlqTDVaSmtNcXRhbFdVUDdWenJFUjVjYm5FNmZJNDAwbmVkSEFRblRqTlFuTDZ4aTV3cDFzVjdOUW9ZWERmQlo1bldaOFZadFBPczRqaGNNMDZpeTNKc3VwdWhrK1I0YXBWcVZsaE10d2lsS25oNk1KMXFyaFNwKzVUZFNvNHVkU2RXdFVLS0tLOGM0Z29vb29BS1pKSEhMRzhVcUpKRklqUnlSeUtIUjBjRldSMVlGV1ZsSkRLUVFRU0NNVVVVYmJBZkFQeGUvNEpnL3NaZkY3eEhjZU9tK0ZuL0NxdmlaY01acFBpaDhCTmYxcjRLK09wN291MG92ZFMxVHdCZDZOYTY1ZXh5dHZqdTlmc05WdUl5RkVjaUtBSzhwai9ZRi9hdThFbFkvZzMvd1ZGL2FPMDNUb3NpMzBqNDdlQnZodCswTGJRSXB6RkVOWTFTMThGK01KMVFFcTczM2llOGxrVGFQTVhZcEJSWHUwZUk4NnBVNFllV09saXFFT1dNS0dZVWNObWRHRVZ0R0ZQTWFPS2hHSzZLTVVsMFNPR3BnTUkzS2FvcW5OdTdsUmxPaEp0MlRiZEdWTnR2cTk5KzdKNGYyYi84QWdxQkVCWS84TisvQWY3SUpqTWI4ZnNWV0M2bEpMaGsvdEY0RStNY2RxTlNZSHpIZFpOcGtKL2VZcTEvd3d4KzFyNHdZcDhYZitDbmZ4N3ZiRnY4QVc2WjhEZmh0OE12Z1JGS3JjU1J0cTBjSGo3eFJDaklXVlRaYS9hU3hIYTZTN2xCb29yMGNWbitZWWRVM2g2ZVU0YVRYTjdURGNQNURoNnFhNVZlTldqbHNLc0hadldFMXV6R09GcFZHdmF6eE5WSnBXcTQzR1ZZNjcrN1VyeWpyWlgwMXRxZWtmRFQvQUlKbGZzaC9EN3hCYStOdGU4QmFuOGNmaU5hU2k1dC9pTCswVjRwMTM0MStLTFM4eDgxN3BTK09MdlU5QzBHOWR0ek5lYURvdW1YVGJpSG5aUW9IMzFEREZieFJ3UVJSd1FRb3NVTU1LTEZGRkdpaFVqampRS2lJaWdLcUtBcXFBQUFCUlJYejJOekhINWxVVlhINDNFNHljVTR3ZUlyVktxcHhidnkwNHprNDA0MzE1YWNZeFhSSGZTb1VhRWVXalNwMG85VkNLamZ6azByeWZtMjJTVVVVVnhHb1VVVVVBZi9aIi8+DQoNCgkJCQkJCQkJCTxoMSBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJPHhzbDp0ZXh0PmUtRkFUVVJBPC94c2w6dGV4dD4NCgkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQk8L2gxPg0KCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQk8dGQgd2lkdGg9IjQwJSIvPg0KCQkJCQkJCTwvdHI+DQoJCQkJCQkJPHRyIHN0eWxlPSJoZWlnaHQ6MTE4cHg7ICIgdmFsaWduPSJ0b3AiPg0KCQkJCQkJCQk8dGQgd2lkdGg9IjQwJSIgYWxpZ249InJpZ2h0IiB2YWxpZ249ImJvdHRvbSI+DQoJCQkJCQkJCQk8dGFibGUgaWQ9ImN1c3RvbWVyUGFydHlUYWJsZSIgYWxpZ249ImxlZnQiIGJvcmRlcj0iMCINCgkJCQkJCQkJCQloZWlnaHQ9IjUwJSI+DQoJCQkJCQkJCQkJPHRib2R5Pg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDo3MXB4OyAiPg0KCQkJCQkJCQkJCQkJPHRkPg0KCQkJCQkJCQkJCQkJPGhyLz4NCgkJCQkJCQkJCQkJCTx0YWJsZSBhbGlnbj0iY2VudGVyIiBib3JkZXI9IjAiPg0KCQkJCQkJCQkJCQkJPHRib2R5Pg0KCQkJCQkJCQkJCQkJPHRyPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpBY2NvdW50aW5nQ3VzdG9tZXJQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx0ZCBzdHlsZT0id2lkdGg6NDY5cHg7ICIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+U0FZSU48L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCQk8dHI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UGFydHkiPg0KCQkJCQkJCQkJCQkJPHRkIHN0eWxlPSJ3aWR0aDo0NjlweDsgIiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9ImNhYzpQYXJ0eU5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9ImNhYzpQYXJ0eU5hbWUvY2JjOk5hbWUiLz4NCgkJCQkJCQkJCQkJCTxici8+DQoJCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UGVyc29uIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6VGl0bGUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpGaXJzdE5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpNaWRkbGVOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOyA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkZhbWlseU5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpOYW1lU3VmZml4Ij4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTx0cj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpQYXJ0eSI+DQoJCQkJCQkJCQkJCQk8dGQgc3R5bGU9IndpZHRoOjQ2OXB4OyAiIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UG9zdGFsQWRkcmVzcyI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlN0cmVldE5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpCdWlsZGluZ05hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkJ1aWxkaW5nTnVtYmVyIj4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6Um9vbSI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5LYXDEsSBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UG9zdGFsWm9uZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlTdWJkaXZpc2lvbk5hbWUiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+LyA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkNpdHlOYW1lIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkvY2FjOlBhcnR5L2NiYzpXZWJzaXRlVVJJIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+V2ViIFNpdGVzaTogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIuIi8+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5L2NhYzpQYXJ0eS9jYWM6Q29udGFjdC9jYmM6RWxlY3Ryb25pY01haWwiPg0KCQkJCQkJCQkJCQkJPHRyIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx0ZD4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5FLVBvc3RhOiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Ii4iLz4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6UGFydHkiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNhYzpDb250YWN0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6aWYgdGVzdD0iY2JjOlRlbGVwaG9uZSBvciBjYmM6VGVsZWZheCI+DQoJCQkJCQkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHRkIHN0eWxlPSJ3aWR0aDo0NjlweDsgIiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRlbGVwaG9uZSI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5UZWw6IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6VGVsZWZheCI+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4gRmF4OiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkvY2FjOlBhcnR5L2NhYzpQYXJ0eVRheFNjaGVtZS9jYWM6VGF4U2NoZW1lL2NiYzpOYW1lIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5WZXJnaSBEYWlyZXNpOiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkFjY291bnRpbmdDdXN0b21lclBhcnR5L2NhYzpQYXJ0eS9jYWM6UGFydHlUYXhTY2hlbWUvY2FjOlRheFNjaGVtZS9jYmM6TmFtZSINCgkJCQkJCQkJCQkJCS8+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6QWNjb3VudGluZ0N1c3RvbWVyUGFydHkvY2FjOlBhcnR5L2NhYzpQYXJ0eUlkZW50aWZpY2F0aW9uIj4NCgkJCQkJCQkJCQkJCTx0ciBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8dGQ+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iY2JjOklEL0BzY2hlbWVJRCIvPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PjogPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6SUQiLz4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3Rib2R5Pg0KCQkJCQkJCQkJCQkJPC90YWJsZT4NCgkJCQkJCQkJCQkJCTxoci8+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCTwvdGJvZHk+DQoJCQkJCQkJCQk8L3RhYmxlPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIHdpZHRoPSI2MCUiIGFsaWduPSJjZW50ZXIiIHZhbGlnbj0iYm90dG9tIiBjb2xzcGFuPSIyIj4NCgkJCQkJCQkJCTx0YWJsZSBib3JkZXI9IjEiIGhlaWdodD0iMTMiIGlkPSJkZXNwYXRjaFRhYmxlIj4NCgkJCQkJCQkJCQk8dGJvZHk+DQoJCQkJCQkJCQkJCTx0cj4NCgkJCQkJCQkJCQkJCTx0ZCBzdHlsZT0id2lkdGg6MTA1cHg7IiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD7DlnplbGxlxZ90aXJtZSBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBzdHlsZT0id2lkdGg6MTEwcHg7IiBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOkN1c3RvbWl6YXRpb25JRCI+DQoJCQkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcy8+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPHRyIHN0eWxlPSJoZWlnaHQ6MTNweDsgIj4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5TZW5hcnlvOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UHJvZmlsZUlEIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyAiPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkZhdHVyYSBUaXBpOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlIj4NCgkJCQkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6SW52b2ljZVR5cGVDb2RlIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyAiPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkZhdHVyYSBObzo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklEIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyAiPg0KCQkJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkZhdHVyYSBUYXJpaGk6PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9ImNiYzpJc3N1ZURhdGUiPg0KCQkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9InN1YnN0cmluZyguLDksMikiDQoJCQkJCQkJCQkJCQkvPi08eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sNiwyKSINCgkJCQkJCQkJCQkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiwxLDQpIi8+DQoJCQkJCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpEZXNwYXRjaERvY3VtZW50UmVmZXJlbmNlIj4NCgkJCQkJCQkJCQkJCTx0ciBzdHlsZT0iaGVpZ2h0OjEzcHg7ICI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+xLByc2FsaXllIE5vOjwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iY2JjOklEIi8+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJCTx0ciBzdHlsZT0iaGVpZ2h0OjEzcHg7ICI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+xLByc2FsaXllIFRhcmloaTo8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklzc3VlRGF0ZSI+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sOSwyKSINCgkJCQkJCQkJCQkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiw2LDIpIg0KCQkJCQkJCQkJCQkJLz4tPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9InN1YnN0cmluZyguLDEsNCkiLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJCQkJCTwvdHI+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6T3JkZXJSZWZlcmVuY2UiPg0KCQkJCQkJCQkJCQkJPHRyIHN0eWxlPSJoZWlnaHQ6MTNweCI+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJCQk8eHNsOnRleHQ+U2lwYXJpxZ8gTm86PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpPcmRlclJlZmVyZW5jZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklEIj4NCgkJCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6T3JkZXJSZWZlcmVuY2UvY2JjOklzc3VlRGF0ZSI+DQoJCQkJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4Ij4NCgkJCQkJCQkJCQkJCTx0ZCBhbGlnbj0ibGVmdCI+DQoJCQkJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5TaXBhcmnFnyBUYXJpaGk6PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJCQk8dGQgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJCQkJCQkJc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpPcmRlclJlZmVyZW5jZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOklzc3VlRGF0ZSI+DQoJCQkJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sOSwyKSINCgkJCQkJCQkJCQkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiw2LDIpIg0KCQkJCQkJCQkJCQkJLz4tPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9InN1YnN0cmluZyguLDEsNCkiLz4NCgkJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQkJCQkJPC90cj4NCgkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJPC90Ym9keT4NCgkJCQkJCQkJCTwvdGFibGU+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJPC90cj4NCgkJCQkJCQk8dHIgYWxpZ249ImxlZnQiPg0KCQkJCQkJCQk8dGFibGUgaWQ9ImV0dG5UYWJsZSI+DQoJCQkJCQkJCQk8dHIgc3R5bGU9ImhlaWdodDoxM3B4OyI+DQoJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0IiB2YWxpZ249InRvcCI+DQoJCQkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PkVUVE46PC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQkJPHRkIGFsaWduPSJsZWZ0IiB3aWR0aD0iMjQwcHgiPg0KCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlVVSUQiPg0KCQkJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMvPg0KCQkJCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCQk8L3RyPg0KCQkJCQkJCQk8L3RhYmxlPg0KCQkJCQkJCTwvdHI+DQoJCQkJCQk8L3Rib2R5Pg0KCQkJCQk8L3RhYmxlPg0KCQkJCQk8ZGl2IGlkPSJsaW5lVGFibGVBbGlnbmVyIj4NCgkJCQkJCTxzcGFuPg0KCQkJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQkJPC9zcGFuPg0KCQkJCQk8L2Rpdj4NCgkJCQkJPHRhYmxlIGJvcmRlcj0iMSIgaWQ9ImxpbmVUYWJsZSIgd2lkdGg9IjgwMCI+DQoJCQkJCQk8dGJvZHk+DQoJCQkJCQkJPHRyIGlkPSJsaW5lVGFibGVUciI+DQoJCQkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIHN0eWxlPSJ3aWR0aDozJSI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+U8SxcmEgTm88L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBzdHlsZT0id2lkdGg6MjAlIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5NYWwgSGl6bWV0PC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjcuNCUiIGFsaWduPSJjZW50ZXIiPg0KCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7Ij4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TWlrdGFyPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjklIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5CaXJpbSBGaXlhdDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIHN0eWxlPSJ3aWR0aDo3JSIgYWxpZ249ImNlbnRlciI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+xLBza29udG8gT3JhbsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjklIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD7EsHNrb250byBUdXRhcsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjclIiBhbGlnbj0iY2VudGVyIj4NCgkJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5LRFYgT3JhbsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9IndpZHRoOjEwJSIgYWxpZ249ImNlbnRlciI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+S0RWIFR1dGFyxLE8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3RkPg0KCQkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBzdHlsZT0id2lkdGg6MTclOyAiIGFsaWduPSJjZW50ZXIiPg0KCQkJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PkRpxJ9lciBWZXJnaWxlcjwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwvdGQ+DQoJCQkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIHN0eWxlPSJ3aWR0aDoxMC42JSIgYWxpZ249ImNlbnRlciI+DQoJCQkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TWFsIEhpem1ldCBUdXRhcsSxPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC90ZD4NCgkJCQkJCQk8L3RyPg0KCQkJCQkJCTx4c2w6aWYgdGVzdD0iY291bnQoLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZSkgJmd0Oz0gMjAiPg0KCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZSI+DQoJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii4iLz4NCgkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJPHhzbDppZiB0ZXN0PSJjb3VudCgvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lKSAmbHQ7IDIwIj4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxXSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzFdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzJdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMl0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbM10iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVszXSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs0XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzRdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzVdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbNV0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbNl0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs2XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs3XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzddIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzhdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbOF0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbOV0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVs5XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxMF0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxMF0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTFdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTFdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzEyXSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzEyXSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxM10iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxM10iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTRdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTRdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE1XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE1XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxNl0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxNl0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTddIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMTddIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE4XSI+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkludm9pY2VMaW5lWzE4XSIvPg0KCQkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJCTx4c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzIHNlbGVjdD0iLy9uMTpJbnZvaWNlIi8+DQoJCQkJCQkJCQk8L3hzbDpvdGhlcndpc2U+DQoJCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDpjaG9vc2U+DQoJCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxOV0iPg0KCQkJCQkJCQkJCTx4c2w6YXBwbHktdGVtcGxhdGVzDQoJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZVsxOV0iLz4NCgkJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCQk8eHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcyBzZWxlY3Q9Ii8vbjE6SW52b2ljZSIvPg0KCQkJCQkJCQkJPC94c2w6b3RoZXJ3aXNlPg0KCQkJCQkJCQk8L3hzbDpjaG9vc2U+DQoJCQkJCQkJCTx4c2w6Y2hvb3NlPg0KCQkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMjBdIj4NCgkJCQkJCQkJCQk8eHNsOmFwcGx5LXRlbXBsYXRlcw0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6SW52b2ljZUxpbmVbMjBdIi8+DQoJCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQkJPHhzbDpvdGhlcndpc2U+DQoJCQkJCQkJCQkJPHhzbDphcHBseS10ZW1wbGF0ZXMgc2VsZWN0PSIvL24xOkludm9pY2UiLz4NCgkJCQkJCQkJCTwveHNsOm90aGVyd2lzZT4NCgkJCQkJCQkJPC94c2w6Y2hvb3NlPg0KCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJPC90Ym9keT4NCgkJCQkJPC90YWJsZT4NCgkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQk8dGFibGUgaWQ9ImJ1ZGdldENvbnRhaW5lclRhYmxlIiB3aWR0aD0iODAwcHgiPg0KCQkJCQk8dHIgaWQ9ImJ1ZGdldENvbnRhaW5lclRyIiBhbGlnbj0icmlnaHQiPg0KCQkJCQkJPHRkIGlkPSJidWRnZXRDb250YWluZXJEdW1teVRkIi8+DQoJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZUJ1ZGdldFRkIiBhbGlnbj0icmlnaHQiIHdpZHRoPSIyMDBweCI+DQoJCQkJCQkJPHNwYW4gc3R5bGU9ImZvbnQtd2VpZ2h0OmJvbGQ7ICI+DQoJCQkJCQkJCTx4c2w6dGV4dD5NYWwgSGl6bWV0IFRvcGxhbSBUdXRhcsSxPC94c2w6dGV4dD4NCgkJCQkJCQk8L3NwYW4+DQoJCQkJCQk8L3RkPg0KCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVCdWRnZXRUZCIgc3R5bGU9IndpZHRoOjgxcHg7ICIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJc2VsZWN0PSJmb3JtYXQtbnVtYmVyKC8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50LCAnIyMjLiMjMCwwMCcsICdldXJvcGVhbicpIi8+DQoJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6TGluZUV4dGVuc2lvbkFtb3VudC9AY3VycmVuY3lJRCA9ICdUUkwnIj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+VEw8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQk8eHNsOmlmDQoJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOkxpbmVFeHRlbnNpb25BbW91bnQvQGN1cnJlbmN5SUQgIT0gJ1RSTCciPg0KCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6TGluZUV4dGVuc2lvbkFtb3VudC9AY3VycmVuY3lJRCINCgkJCQkJCQkJCQkvPg0KCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCTwvdGQ+DQoJCQkJCTwvdHI+DQoJCQkJCTx0ciBpZD0iYnVkZ2V0Q29udGFpbmVyVHIiIGFsaWduPSJyaWdodCI+DQoJCQkJCQk8dGQgaWQ9ImJ1ZGdldENvbnRhaW5lckR1bW15VGQiLz4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIGFsaWduPSJyaWdodCIgd2lkdGg9IjIwMHB4Ij4NCgkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJPHhzbDp0ZXh0PlRvcGxhbSDEsHNrb250bzwveHNsOnRleHQ+DQoJCQkJCQkJPC9zcGFuPg0KCQkJCQkJPC90ZD4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIHN0eWxlPSJ3aWR0aDo4MXB4OyAiIGFsaWduPSJyaWdodCI+DQoJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlcigvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6QWxsb3dhbmNlVG90YWxBbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiLz4NCgkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOkFsbG93YW5jZVRvdGFsQW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6QWxsb3dhbmNlVG90YWxBbW91bnQvQGN1cnJlbmN5SUQgPSAnVFJMJyI+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpBbGxvd2FuY2VUb3RhbEFtb3VudC9AY3VycmVuY3lJRCAhPSAnVFJMJyI+DQoJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpBbGxvd2FuY2VUb3RhbEFtb3VudC9AY3VycmVuY3lJRCINCgkJCQkJCQkJCQkvPg0KCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCTwvdGQ+DQoJCQkJCTwvdHI+DQoJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJuMTpJbnZvaWNlL2NhYzpUYXhUb3RhbC9jYWM6VGF4U3VidG90YWwiPg0KCQkJCQkJPHRyIGlkPSJidWRnZXRDb250YWluZXJUciIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8dGQgaWQ9ImJ1ZGdldENvbnRhaW5lckR1bW15VGQiLz4NCgkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZUJ1ZGdldFRkIiB3aWR0aD0iMjExcHgiIGFsaWduPSJyaWdodCI+DQoJCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQkJPHhzbDp0ZXh0Pkhlc2FwbGFuYW4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYWM6VGF4Q2F0ZWdvcnkvY2FjOlRheFNjaGVtZS9jYmM6TmFtZSIvPg0KCQkJCQkJCQkJPHhzbDp0ZXh0PiglPC94c2w6dGV4dD4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6UGVyY2VudCIvPg0KCQkJCQkJCQkJPHhzbDp0ZXh0Pik8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJPC90ZD4NCgkJCQkJCQk8dGQgaWQ9ImxpbmVUYWJsZUJ1ZGdldFRkIiBzdHlsZT0id2lkdGg6ODJweDsgIiBhbGlnbj0icmlnaHQiPg0KCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOlRheENhdGVnb3J5L2NhYzpUYXhTY2hlbWUiPg0KCQkJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguLi8uLi9jYmM6VGF4QW1vdW50LCAnIyMjLiMjMCwwMCcsICdldXJvcGVhbicpIi8+DQoJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQiPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQgPSAnVFJMJyI+DQoJCQkJCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQiLz4NCgkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQk8L3RkPg0KCQkJCQkJPC90cj4NCgkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCTx0ciBpZD0iYnVkZ2V0Q29udGFpbmVyVHIiIGFsaWduPSJyaWdodCI+DQoJCQkJCQk8dGQgaWQ9ImJ1ZGdldENvbnRhaW5lckR1bW15VGQiLz4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIHdpZHRoPSIyMDBweCIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8c3BhbiBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDsgIj4NCgkJCQkJCQkJPHhzbDp0ZXh0PlZlcmdpbGVyIERhaGlsIFRvcGxhbSBUdXRhcjwveHNsOnRleHQ+DQoJCQkJCQkJPC9zcGFuPg0KCQkJCQkJPC90ZD4NCgkJCQkJCTx0ZCBpZD0ibGluZVRhYmxlQnVkZ2V0VGQiIHN0eWxlPSJ3aWR0aDo4MnB4OyAiIGFsaWduPSJyaWdodCI+DQoJCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Im4xOkludm9pY2UiPg0KCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbCI+DQoJCQkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0iY2JjOlRheEluY2x1c2l2ZUFtb3VudCI+DQoJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLiwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSIvPg0KCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlRheEluY2x1c2l2ZUFtb3VudC9AY3VycmVuY3lJRCI+DQoJCQkJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJPHhzbDppZg0KCQkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlRheEluY2x1c2l2ZUFtb3VudC9AY3VycmVuY3lJRCA9ICdUUkwnIj4NCgkJCQkJCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCQk8eHNsOmlmDQoJCQkJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOkxlZ2FsTW9uZXRhcnlUb3RhbC9jYmM6VGF4SW5jbHVzaXZlQW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlRheEluY2x1c2l2ZUFtb3VudC9AY3VycmVuY3lJRCINCgkJCQkJCQkJCQkJCS8+DQoJCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJPC90ZD4NCgkJCQkJPC90cj4NCgkJCQkJPHRyIGlkPSJidWRnZXRDb250YWluZXJUciIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCTx0ZCBpZD0iYnVkZ2V0Q29udGFpbmVyRHVtbXlUZCIvPg0KCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVCdWRnZXRUZCIgd2lkdGg9IjIwMHB4IiBhbGlnbj0icmlnaHQiPg0KCQkJCQkJCTxzcGFuIHN0eWxlPSJmb250LXdlaWdodDpib2xkOyAiPg0KCQkJCQkJCQk8eHNsOnRleHQ+w5ZkZW5lY2VrIFR1dGFyPC94c2w6dGV4dD4NCgkJCQkJCQk8L3NwYW4+DQoJCQkJCQk8L3RkPg0KCQkJCQkJPHRkIGlkPSJsaW5lVGFibGVCdWRnZXRUZCIgc3R5bGU9IndpZHRoOjgycHg7ICIgYWxpZ249InJpZ2h0Ij4NCgkJCQkJCQk8eHNsOmZvci1lYWNoIHNlbGVjdD0ibjE6SW52b2ljZSI+DQoJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYWM6TGVnYWxNb25ldGFyeVRvdGFsIj4NCgkJCQkJCQkJCTx4c2w6Zm9yLWVhY2ggc2VsZWN0PSJjYmM6UGF5YWJsZUFtb3VudCI+DQoJCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLiwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSIvPg0KCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlBheWFibGVBbW91bnQvQGN1cnJlbmN5SUQiPg0KCQkJCQkJCQkJCQk8eHNsOnRleHQ+IDwveHNsOnRleHQ+DQoJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpQYXlhYmxlQW1vdW50L0BjdXJyZW5jeUlEID0gJ1RSTCciPg0KCQkJCQkJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCQkJCTx4c2w6aWYNCgkJCQkJCQkJCQkJCXRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6TGVnYWxNb25ldGFyeVRvdGFsL2NiYzpQYXlhYmxlQW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQkJCXNlbGVjdD0iLy9uMTpJbnZvaWNlL2NhYzpMZWdhbE1vbmV0YXJ5VG90YWwvY2JjOlBheWFibGVBbW91bnQvQGN1cnJlbmN5SUQiDQoJCQkJCQkJCQkJCQkvPg0KCQkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQkJCTwvdGQ+DQoJCQkJCTwvdHI+DQoJCQkJPC90YWJsZT4NCgkJCQk8YnIvPg0KCQkJCTx0YWJsZSBpZD0ibm90ZXNUYWJsZSIgd2lkdGg9IjgwMCIgYWxpZ249ImxlZnQiIGhlaWdodD0iMTAwIj4NCgkJCQkJPHRib2R5Pg0KCQkJCQkJPHRyIGFsaWduPSJsZWZ0Ij4NCgkJCQkJCQk8dGQgaWQ9Im5vdGVzVGFibGVUZCI+DQoJCQkJCQkJCTx4c2w6aWYgdGVzdD0iLy9uMTpJbnZvaWNlL2NiYzpOb3RlIj4NCgkJCQkJCQkJCTxiPiYjMTYwOyYjMTYwOyYjMTYwOyYjMTYwOyYjMTYwOyBOb3Q6IDwvYj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIvL24xOkludm9pY2UvY2JjOk5vdGUiLz4NCgkJCQkJCQkJCTxici8+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii8vbjE6SW52b2ljZS9jYWM6UGF5bWVudE1lYW5zL2NiYzpJbnN0cnVjdGlvbk5vdGUiPg0KCQkJCQkJCQkJPGI+JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7IMOWZGVtZQ0KCQkJCQkJCQkJCU5vdHU6IDwvYj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQkJCQlzZWxlY3Q9Ii8vbjE6SW52b2ljZS9jYWM6UGF5bWVudE1lYW5zL2NiYzpJbnN0cnVjdGlvbk5vdGUiLz4NCgkJCQkJCQkJCTxici8+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCQk8eHNsOmlmDQoJCQkJCQkJCQl0ZXN0PSIvL24xOkludm9pY2UvY2FjOlBheW1lbnRNZWFucy9jYWM6UGF5ZWVGaW5hbmNpYWxBY2NvdW50L2NiYzpQYXltZW50Tm90ZSI+DQoJCQkJCQkJCQk8Yj4mIzE2MDsmIzE2MDsmIzE2MDsmIzE2MDsmIzE2MDsgSGVzYXANCgkJCQkJCQkJCQlBw6fEsWtsYW1hc8SxOiA8L2I+DQoJCQkJCQkJCQk8eHNsOnZhbHVlLW9mDQoJCQkJCQkJCQkJc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOlBheW1lbnRNZWFucy9jYWM6UGF5ZWVGaW5hbmNpYWxBY2NvdW50L2NiYzpQYXltZW50Tm90ZSIvPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTx4c2w6aWYgdGVzdD0iLy9uMTpJbnZvaWNlL2NhYzpQYXltZW50VGVybXMvY2JjOk5vdGUiPg0KCQkJCQkJCQkJPGI+JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7JiMxNjA7IMOWZGVtZQ0KCQkJCQkJCQkJCUtvxZ91bHU6IDwvYj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIvL24xOkludm9pY2UvY2FjOlBheW1lbnRUZXJtcy9jYmM6Tm90ZSIvPg0KCQkJCQkJCQkJPGJyLz4NCgkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJPC90ZD4NCgkJCQkJCTwvdHI+DQoJCQkJCTwvdGJvZHk+DQoJCQkJPC90YWJsZT4NCgkJCTwvYm9keT4NCgkJPC9odG1sPg0KCTwveHNsOnRlbXBsYXRlPg0KCTx4c2w6dGVtcGxhdGUgbWF0Y2g9ImRhdGVGb3JtYXR0ZXIiPg0KCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sOSwyKSIvPi08eHNsOnZhbHVlLW9mIHNlbGVjdD0ic3Vic3RyaW5nKC4sNiwyKSINCgkJCS8+LTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJzdWJzdHJpbmcoLiwxLDQpIi8+DQoJPC94c2w6dGVtcGxhdGU+DQoJPHhzbDp0ZW1wbGF0ZSBtYXRjaD0iLy9uMTpJbnZvaWNlL2NhYzpJbnZvaWNlTGluZSI+DQoJCTx0ciBpZD0ibGluZVRhYmxlVHIiPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYmM6SUQiLz4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6TmFtZSIvPg0KCQkJCQk8IS0tCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6QnJhbmROYW1lIi8+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6TW9kZWxOYW1lIi8+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6SXRlbS9jYmM6RGVzY3JpcHRpb24iLz4tLT4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguL2NiYzpJbnZvaWNlZFF1YW50aXR5LCAnIyMjLiMjIywjIycsICdldXJvcGVhbicpIi8+DQoJCQkJCTx4c2w6aWYgdGVzdD0iLi9jYmM6SW52b2ljZWRRdWFudGl0eS9AdW5pdENvZGUiPg0KCQkJCQkJPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Ii4vY2JjOkludm9pY2VkUXVhbnRpdHkiPg0KCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQk8eHNsOmNob29zZT4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnMjYnIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5Ub248L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdCWCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pkt1dHU8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdMVFInIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5MVDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoNCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnTklVJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+QWRldDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ0tHTSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PktHPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnS0pPJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+a0o8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdHUk0nIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5HPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnTUdNJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TUc8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdOVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk5ldCBUb248L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdHVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PkdUPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnTVRSJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+TTwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ01NVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk1NPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnS1RNJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+S008L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdNTFQnIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5NTDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ01NUSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk1NMzwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ0NMVCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PkNMPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnQ01LJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+Q00yPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnQ01RJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+Q00zPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnQ01UJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+Q008L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdNVEsnIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD5NMjwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ01UUSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0Pk0zPC94c2w6dGV4dD4NCgkJCQkJCQkJCTwvc3Bhbj4NCgkJCQkJCQkJPC94c2w6d2hlbj4NCgkJCQkJCQkJPHhzbDp3aGVuIHRlc3Q9IkB1bml0Q29kZSAgPSAnREFZJyI+DQoJCQkJCQkJCQk8c3Bhbj4NCgkJCQkJCQkJCQk8eHNsOnRleHQ+IEfDvG48L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdNT04nIj4NCgkJCQkJCQkJCTxzcGFuPg0KCQkJCQkJCQkJCTx4c2w6dGV4dD4gQXk8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCQk8eHNsOndoZW4gdGVzdD0iQHVuaXRDb2RlICA9ICdQQSciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PiBQYWtldDwveHNsOnRleHQ+DQoJCQkJCQkJCQk8L3NwYW4+DQoJCQkJCQkJCTwveHNsOndoZW4+DQoJCQkJCQkJCTx4c2w6d2hlbiB0ZXN0PSJAdW5pdENvZGUgID0gJ0tXSCciPg0KCQkJCQkJCQkJPHNwYW4+DQoJCQkJCQkJCQkJPHhzbDp0ZXh0PiBLV0g8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPC9zcGFuPg0KCQkJCQkJCQk8L3hzbDp3aGVuPg0KCQkJCQkJCTwveHNsOmNob29zZT4NCgkJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCQk8L3hzbDppZj4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguL2NhYzpQcmljZS9jYmM6UHJpY2VBbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiLz4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NhYzpQcmljZS9jYmM6UHJpY2VBbW91bnQvQGN1cnJlbmN5SUQiPg0KCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJPHhzbDppZg0KCQkJCQkJCXRlc3Q9Ii4vY2FjOlByaWNlL2NiYzpQcmljZUFtb3VudC9AY3VycmVuY3lJRCA9ICZxdW90O1RSTCZxdW90OyAiPg0KCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJCTx4c2w6aWYNCgkJCQkJCQl0ZXN0PSIuL2NhYzpQcmljZS9jYmM6UHJpY2VBbW91bnQvQGN1cnJlbmN5SUQgIT0gJnF1b3Q7VFJMJnF1b3Q7Ij4NCgkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi9jYWM6UHJpY2UvY2JjOlByaWNlQW1vdW50L0BjdXJyZW5jeUlEIi8+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJPC94c2w6aWY+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOmlmIHRlc3Q9Ii4vY2FjOkFsbG93YW5jZUNoYXJnZS9jYmM6TXVsdGlwbGllckZhY3Rvck51bWVyaWMiPg0KCQkJCQkJPHhzbDp0ZXh0PiAlPC94c2w6dGV4dD4NCgkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpNdWx0aXBsaWVyRmFjdG9yTnVtZXJpYyAqIDEwMCwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSINCgkJCQkJCS8+DQoJCQkJCTwveHNsOmlmPg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NhYzpBbGxvd2FuY2VDaGFyZ2UiPg0KCQkJCQkJPCEtLTx4c2w6aWYgdGVzdD0iLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpDaGFyZ2VJbmRpY2F0b3IgPSB0cnVlKCkgIj4rDQoJCQkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4vY2FjOkFsbG93YW5jZUNoYXJnZS9jYmM6Q2hhcmdlSW5kaWNhdG9yID0gZmFsc2UoKSAiPi0NCgkJCQkJCQkJCQk8L3hzbDppZj4tLT4NCgkJCQkJCTx4c2w6dmFsdWUtb2YNCgkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpBbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiDQoJCQkJCQkvPg0KCQkJCQk8L3hzbDppZj4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NhYzpBbGxvd2FuY2VDaGFyZ2UvY2JjOkFtb3VudC9AY3VycmVuY3lJRCI+DQoJCQkJCQk8eHNsOnRleHQ+IDwveHNsOnRleHQ+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4vY2FjOkFsbG93YW5jZUNoYXJnZS9jYmM6QW1vdW50L0BjdXJyZW5jeUlEID0gJ1RSTCciPg0KCQkJCQkJCTx4c2w6dGV4dD5UTDwveHNsOnRleHQ+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJCTx4c2w6aWYgdGVzdD0iLi9jYWM6QWxsb3dhbmNlQ2hhcmdlL2NiYzpBbW91bnQvQGN1cnJlbmN5SUQgIT0gJ1RSTCciPg0KCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIuL2NhYzpBbGxvd2FuY2VDaGFyZ2UvY2JjOkFtb3VudC9AY3VycmVuY3lJRCIvPg0KCQkJCQkJPC94c2w6aWY+DQoJCQkJCTwveHNsOmlmPg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJPHhzbDpmb3ItZWFjaA0KCQkJCQkJc2VsZWN0PSIuL2NhYzpUYXhUb3RhbC9jYWM6VGF4U3VidG90YWwvY2FjOlRheENhdGVnb3J5L2NhYzpUYXhTY2hlbWUiPg0KCQkJCQkJPHhzbDppZiB0ZXN0PSJjYmM6VGF4VHlwZUNvZGU9JzAwMTUnICI+DQoJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCTx4c2w6aWYgdGVzdD0iLi4vLi4vY2JjOlBlcmNlbnQiPg0KCQkJCQkJCQk8eHNsOnRleHQ+ICU8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8eHNsOnZhbHVlLW9mDQoJCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi4vLi4vY2JjOlBlcmNlbnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiDQoJCQkJCQkJCS8+DQoJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQk8L3hzbDppZj4NCgkJCQkJPC94c2w6Zm9yLWVhY2g+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQlzZWxlY3Q9Ii4vY2FjOlRheFRvdGFsL2NhYzpUYXhTdWJ0b3RhbC9jYWM6VGF4Q2F0ZWdvcnkvY2FjOlRheFNjaGVtZSI+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9ImNiYzpUYXhUeXBlQ29kZT0nMDAxNScgIj4NCgkJCQkJCQk8eHNsOnRleHQ+IDwveHNsOnRleHQ+DQoJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQlzZWxlY3Q9ImZvcm1hdC1udW1iZXIoLi4vLi4vY2JjOlRheEFtb3VudCwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSIvPg0KCQkJCQkJCTx4c2w6aWYgdGVzdD0iLi4vLi4vY2JjOlRheEFtb3VudC9AY3VycmVuY3lJRCI+DQoJCQkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEID0gJ1RSTCciPg0KCQkJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCQkJPC94c2w6aWY+DQoJCQkJCQkJCTx4c2w6aWYgdGVzdD0iLi4vLi4vY2JjOlRheEFtb3VudC9AY3VycmVuY3lJRCAhPSAnVFJMJyI+DQoJCQkJCQkJCQk8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLi4vLi4vY2JjOlRheEFtb3VudC9AY3VycmVuY3lJRCIvPg0KCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCTwveHNsOmlmPg0KCQkJCQk8L3hzbDpmb3ItZWFjaD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgc3R5bGU9ImZvbnQtc2l6ZTogeHgtc21hbGwiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCQk8eHNsOmZvci1lYWNoDQoJCQkJCQlzZWxlY3Q9Ii4vY2FjOlRheFRvdGFsL2NhYzpUYXhTdWJ0b3RhbC9jYWM6VGF4Q2F0ZWdvcnkvY2FjOlRheFNjaGVtZSI+DQoJCQkJCQk8eHNsOmlmIHRlc3Q9ImNiYzpUYXhUeXBlQ29kZSE9JzAwMTUnICI+DQoJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjYmM6TmFtZSIvPg0KCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpQZXJjZW50Ij4NCgkJCQkJCQkJCTx4c2w6dGV4dD4gKCU8L3hzbDp0ZXh0Pg0KCQkJCQkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguLi8uLi9jYmM6UGVyY2VudCwgJyMjIy4jIzAsMDAnLCAnZXVyb3BlYW4nKSINCgkJCQkJCQkJCS8+DQoJCQkJCQkJCQk8eHNsOnRleHQ+KT08L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQk8eHNsOnZhbHVlLW9mDQoJCQkJCQkJCXNlbGVjdD0iZm9ybWF0LW51bWJlciguLi8uLi9jYmM6VGF4QW1vdW50LCAnIyMjLiMjMCwwMCcsICdldXJvcGVhbicpIi8+DQoJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCQkJPHhzbDp0ZXh0PiA8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8eHNsOmlmIHRlc3Q9Ii4uLy4uL2NiYzpUYXhBbW91bnQvQGN1cnJlbmN5SUQgPSAnVFJMJyI+DQoJCQkJCQkJCQk8eHNsOnRleHQ+VEw8L3hzbDp0ZXh0Pg0KCQkJCQkJCQk8L3hzbDppZj4NCgkJCQkJCQkJPHhzbDppZiB0ZXN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnIj4NCgkJCQkJCQkJCTx4c2w6dmFsdWUtb2Ygc2VsZWN0PSIuLi8uLi9jYmM6VGF4QW1vdW50L0BjdXJyZW5jeUlEIi8+DQoJCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJCTwveHNsOmlmPg0KCQkJCQkJPC94c2w6aWY+DQoJCQkJCTwveHNsOmZvci1lYWNoPg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQkJPHhzbDp2YWx1ZS1vZg0KCQkJCQkJc2VsZWN0PSJmb3JtYXQtbnVtYmVyKC4vY2JjOkxpbmVFeHRlbnNpb25BbW91bnQsICcjIyMuIyMwLDAwJywgJ2V1cm9wZWFuJykiLz4NCgkJCQkJPHhzbDppZiB0ZXN0PSIuL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50L0BjdXJyZW5jeUlEIj4NCgkJCQkJCTx4c2w6dGV4dD4gPC94c2w6dGV4dD4NCgkJCQkJCTx4c2w6aWYgdGVzdD0iLi9jYmM6TGluZUV4dGVuc2lvbkFtb3VudC9AY3VycmVuY3lJRCA9ICdUUkwnICI+DQoJCQkJCQkJPHhzbDp0ZXh0PlRMPC94c2w6dGV4dD4NCgkJCQkJCTwveHNsOmlmPg0KCQkJCQkJPHhzbDppZiB0ZXN0PSIuL2NiYzpMaW5lRXh0ZW5zaW9uQW1vdW50L0BjdXJyZW5jeUlEICE9ICdUUkwnICI+DQoJCQkJCQkJPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Ii4vY2JjOkxpbmVFeHRlbnNpb25BbW91bnQvQGN1cnJlbmN5SUQiLz4NCgkJCQkJCTwveHNsOmlmPg0KCQkJCQk8L3hzbDppZj4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQk8L3RyPg0KCTwveHNsOnRlbXBsYXRlPg0KCTx4c2w6dGVtcGxhdGUgbWF0Y2g9Ii8vbjE6SW52b2ljZSI+DQoJCTx0ciBpZD0ibGluZVRhYmxlVHIiPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIj4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQkJPHRkIGlkPSJsaW5lVGFibGVUZCIgYWxpZ249InJpZ2h0Ij4NCgkJCQk8c3Bhbj4NCgkJCQkJPHhzbDp0ZXh0PiYjMTYwOzwveHNsOnRleHQ+DQoJCQkJPC9zcGFuPg0KCQkJPC90ZD4NCgkJCTx0ZCBpZD0ibGluZVRhYmxlVGQiIGFsaWduPSJyaWdodCI+DQoJCQkJPHNwYW4+DQoJCQkJCTx4c2w6dGV4dD4mIzE2MDs8L3hzbDp0ZXh0Pg0KCQkJCTwvc3Bhbj4NCgkJCTwvdGQ+DQoJCQk8dGQgaWQ9ImxpbmVUYWJsZVRkIiBhbGlnbj0icmlnaHQiPg0KCQkJCTxzcGFuPg0KCQkJCQk8eHNsOnRleHQ+JiMxNjA7PC94c2w6dGV4dD4NCgkJCQk8L3NwYW4+DQoJCQk8L3RkPg0KCQk8L3RyPg0KCTwveHNsOnRlbXBsYXRlPg0KPC94c2w6c3R5bGVzaGVldD4NCg=="));
				cacAttachment.appendChild(cbcEmbeddedDocumentBinaryObject);
			}

			// cac:Signature
			Element cacSignature = m_PackageXML.createElement("cac:Signature");
			root.appendChild(cacSignature);
			
			cacSignature.appendChild(createID(m_PackageXML, ACCOUNTING_TYPE_SUPPLIER));
			
			// cac:SignatoryParty
			Element cacSignatoryParty  = m_PackageXML.createElement("cac:SignatoryParty");
			cacSignature.appendChild(cacSignatoryParty);
				
			cacSignatoryParty.appendChild(createPartyIdentification(m_PackageXML, ACCOUNTING_TYPE_SUPPLIER));
			
			//cac:PostalAddress
			cacSignatoryParty.appendChild(createPostalAdress(m_PackageXML, ACCOUNTING_TYPE_SUPPLIER));
			
			// cac:DigitalSignatureAttachment
			Element cacDigitalSignatureAttachment  = m_PackageXML.createElement("cac:DigitalSignatureAttachment");
			cacSignature.appendChild(cacDigitalSignatureAttachment);
			
			Element cacExternalReference  = m_PackageXML.createElement("cac:ExternalReference");
			Element cbcURI  = m_PackageXML.createElement("cbc:URI");
			cbcURI.appendChild(m_PackageXML.createTextNode("#Signature_TST2013000000045"));
			cacExternalReference.appendChild(cbcURI);
			cacDigitalSignatureAttachment.appendChild(cacExternalReference);
			
			//cac:AccountingSupplierParty
			Element cacAccountingSupplierParty  = m_PackageXML.createElement("cac:AccountingSupplierParty");
			root.appendChild(cacAccountingSupplierParty);
			cacAccountingSupplierParty.appendChild(createParty(m_PackageXML, ACCOUNTING_TYPE_SUPPLIER));
			
			//cac:AccountingCustomerParty
			Element cacAccountingCustomerParty  = m_PackageXML.createElement("cac:AccountingCustomerParty");
			root.appendChild(cacAccountingCustomerParty);
			cacAccountingCustomerParty.appendChild(createParty(m_PackageXML, ACCOUNTING_TYPE_CUSTOMER));
			
			//cac:TaxTotal
			root.appendChild(createTaxTotals(m_PackageXML));
			
			//cac:LegalMonetaryTotal
			Element cacLegalMonetaryTotal  = m_PackageXML.createElement("cac:LegalMonetaryTotal");
			root.appendChild(cacLegalMonetaryTotal);
			
			Element cbcLineExtensionAmount  = m_PackageXML.createElement("cbc:LineExtensionAmount");
			cbcLineExtensionAmount.setAttribute("currencyID", "TRL");
			cbcLineExtensionAmount.appendChild(m_PackageXML.createTextNode(m_Invoice.getTotalGross().toString()));
			cacLegalMonetaryTotal.appendChild(cbcLineExtensionAmount);
			
			Element cbcTaxExclusiveAmount  = m_PackageXML.createElement("cbc:TaxExclusiveAmount");
			cbcTaxExclusiveAmount.setAttribute("currencyID", "TRL");
			cbcTaxExclusiveAmount.appendChild(m_PackageXML.createTextNode(m_Invoice.getTotalGross()
					.subtract(m_Invoice.getTotalDiscounts()).toString()));
			cacLegalMonetaryTotal.appendChild(cbcTaxExclusiveAmount);
			
			Element cbcTaxInclusiveAmount  = m_PackageXML.createElement("cbc:TaxInclusiveAmount");
			cbcTaxInclusiveAmount.setAttribute("currencyID", "TRL");
			cbcTaxInclusiveAmount.appendChild(m_PackageXML.createTextNode(m_Invoice.getTotalNet().toString()));
			cacLegalMonetaryTotal.appendChild(cbcTaxInclusiveAmount);
			
			Element cbcAllowanceTotalAmount  = m_PackageXML.createElement("cbc:AllowanceTotalAmount");
			cbcAllowanceTotalAmount.setAttribute("currencyID", "TRL");
			cbcAllowanceTotalAmount.appendChild(m_PackageXML.createTextNode(m_Invoice.getTotalDiscounts().toString()));
			cacLegalMonetaryTotal.appendChild(cbcAllowanceTotalAmount);
			
			Element cbcPayableAmount  = m_PackageXML.createElement("cbc:PayableAmount");
			cbcPayableAmount.setAttribute("currencyID", "TRL");
			cbcPayableAmount.appendChild(m_PackageXML.createTextNode(m_Invoice.getTotalNet().toString()));
			cacLegalMonetaryTotal.appendChild(cbcPayableAmount);
			
			//cac:InvoiceLine
			for (int i = 0; i < m_Invoice.getTransactions().size(); i++)
			{
				MMBOTransaction trans = (MMBOTransaction) m_Invoice.getTransactions().get(i);
				if (trans.getMaster_Reference() == 0)
					continue;
				Element cacInvoiceLine  = m_PackageXML.createElement("cac:InvoiceLine");
				root.appendChild(cacInvoiceLine);
				
				cacInvoiceLine.appendChild(m_PackageXML.createElement("cbc:ID"));
				Element cbcInvoicedQuantity  = m_PackageXML.createElement("cbc:InvoicedQuantity");
				cbcInvoicedQuantity.setAttribute("unitCode", trans.getUnits().getGlobalCode());
				cbcInvoicedQuantity.appendChild(m_PackageXML.createTextNode(trans.getQuantity().toString()));
				cacInvoiceLine.appendChild(cbcInvoicedQuantity);
				
				cbcLineExtensionAmount  = m_PackageXML.createElement("cbc:LineExtensionAmount");
				cbcLineExtensionAmount.setAttribute("currencyID", "TRL");
				cbcLineExtensionAmount.appendChild(m_PackageXML.createTextNode(trans.getTotal().toString()));
				cacInvoiceLine.appendChild(cbcLineExtensionAmount);
				
				BusinessObjects<MMBOTransaction> transList = new BusinessObjects<MMBOTransaction>();
				transList.add(trans);
				calculateTaxTotal(transList);
				cacInvoiceLine.appendChild(createTaxTotals(m_PackageXML));
				
				Element cacItem = m_PackageXML.createElement("cac:Item");
				Element cbcName = getElement(m_PackageXML, "cbc:Name");
				cbcName.appendChild(m_PackageXML.createTextNode(trans.getitemLink().getDescription()));
				cacItem.appendChild(cbcName);
				cacInvoiceLine.appendChild(cacItem);
				
				Element cacPrice = m_PackageXML.createElement("cac:Price");
				Element cbcPriceAmount = m_PackageXML.createElement("cbc:PriceAmount");
				cbcPriceAmount.setAttribute("currencyID", "TRL");
				cbcPriceAmount.appendChild(m_PackageXML.createTextNode(trans.getPrice().toString()));
				cacPrice.appendChild(cbcPriceAmount);
				cacInvoiceLine.appendChild(cacPrice);
			}
			
			try
			{
				String tmpDir = JLbsFileUtil.getTempDirectory();
				String xmlFileName = tmpDir + "JGUARUBL.XML";
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(m_PackageXML);					
				StreamResult result = new StreamResult(new File(xmlFileName));
				transformer.transform(source, result);
				
				CustomBusinessObject approvalBO = ProjectUtilEInv.createNewCBO("CBOApproval");
				approvalBO._setState(CustomBusinessObject.STATE_NEW);
				
				Calendar now = DateUtil.getToday();
				ProjectUtilEInv.setMemberValue(approvalBO, "DocNr", m_Invoice.getInvoiceNumber());
				ProjectUtilEInv.setMemberValue(approvalBO, "GenExp", m_ArpInfo.getDescription());
				ProjectUtilEInv.setMemberValue(approvalBO, "Date_", now);
				ProjectUtilEInv.setMemberValue(approvalBO, "Time_",  now.getTimeInMillis());
				ProjectUtilEInv.setMemberValue(approvalBO, "RecType", ProjectGlobalsEInv.RECTYPE_SENDED_INV);
				ProjectUtilEInv.setMemberValue(approvalBO, "Status", ProjectGlobalsEInv.STATUS_PACKED_OR_SAVED);
				ProjectUtilEInv.setMemberValue(approvalBO, "Sender", m_ArpInfo.getIsPersComp() == 1 ? m_ArpInfo.getIDTCNo() : m_ArpInfo.getTax_Id());
				ProjectUtilEInv.setMemberValue(approvalBO, "FileName", m_Invoice.getGUId());
				ProjectUtilEInv.setMemberValue(approvalBO, "TrCode", m_Invoice.getInvoiceType());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocRef", m_Invoice.getInternal_Reference());
				ProjectUtilEInv.setMemberValue(approvalBO, "OpType", getOpType());
				ProjectUtilEInv.setMemberValue(approvalBO, "ProfileID", m_Invoice.getProfileId());
				ProjectUtilEInv.setMemberValue(approvalBO, "PKLabel", m_OrgUnit.getPkurn());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocDate", m_Invoice.getInvoiceDate());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocTotal", m_Invoice.getTotalNet());
				ProjectUtilEInv.setMemberValue(approvalBO, "DocExplain", BUHelper.byteArrToString(m_Invoice.getNotes()!= null ? m_Invoice.getNotes().getDocument():null));
				ProjectUtilEInv.setMemberValue(approvalBO, "LData", ProjectUtilEInv.documentToByte(source, transformer));
				//ProjectUtilEInv.persistCBO(m_Context, approvalBO);
				m_InvoicesCBO.add(approvalBO);
				
				 Bu kýsým ya da yukarýdaki Ldata' dan biri olmamalý..
				UNBOSlipObject slipObject = new UNBOSlipObject();
				slipObject._setState(BusinessObject.STATE_NEW);
				slipObject.setSlipReference(m_Invoice.getInternal_Reference());
				slipObject.setModule(1);
				slipObject.setObjectType(0);
				slipObject.setSlipType(m_Invoice.getInvoiceType());
				slipObject.setLogoObject(ProjectUtilEInv.documentToByte(source, transformer));
				UnityHelper.persistBO(m_Context, slipObject);
				
			}
			catch (TransformerException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		catch (Exception e)
		{
			m_Context.getLogger().error("error while creatin m_PackageXML", e);
		}
		return root;
	}
	*/
	private static LOBOConnectEInvoice findSrcInvoice() 
	{
		int srcTransRef = 0;
		for (int i = 0; i < m_Invoice.getTransactions().size(); i++)
		{
			MMBOTransaction trans = m_Invoice.getTransactions().get(i);
			if (trans.getSourceTransRef() != 0)
			{
				srcTransRef = trans.getSourceTransRef();
				break;
			}
		}
		if(srcTransRef > 0)
		{
			MMBOTransactionBase srcTrans = (MMBOTransactionBase) UnityHelper.getBOFieldsByRef(m_Context,
					MMBOTransactionBase.class, srcTransRef, new String[] { "InvoiceRef" });
			if(srcTrans.getInvoiceRef() > 0)
			{
				LOBOConnectEInvoice srcInv = (LOBOConnectEInvoice) UnityHelper.getBOFieldsByRef(m_Context,
						LOBOConnectEInvoice.class, srcTrans.getInvoiceRef(), new String[] { "GUId", "InvoiceDate" });
				if(srcInv != null)
					return srcInv;
			}
		}
		return null;
	}


	private static int getOpType()
	{
		switch (m_Invoice.getInvoiceType())
		{
			case UnityConstants.INVC_RETAIL:
			case UnityConstants.INVC_WHOLESALE:
			case UnityConstants.INVC_SERVICESSOLD:
			case UnityConstants.INVC_PURCHASERET:
				return ProjectGlobalsEInv.OPTYPE_OUTGOING;
			case UnityConstants.INVC_PURCHASE:
				return ProjectGlobalsEInv.OPTYPE_INCOMING;				
		}
		return ProjectGlobalsEInv.OPTYPE_OUTGOING;
	}
	
	private static GOBOOrgUnit findAndGetOrgUnit(int divisionRef)
	{
		GOBOOrgUnit orgUnit = null;
		if (divisionRef != 0)
		{
			GOBOOrgUnit ou = (GOBOOrgUnit) UnityHelper.getBOByReference(m_Context, GOBOOrgUnit.class, divisionRef);
			if(m_EInvoiceUserType == EINVOICE_USER_TYPE_COMPANY)
			{
				if (ou.getOrgUnitType() == GOConstants.ORGUNIT_COMPANY)
					orgUnit = ou;
				else
				{
					GOBOOrgUnit boundOu = GLHelper.getBoundFirm(m_Context, ou);
					orgUnit = boundOu;
				}
			}
			else if(m_EInvoiceUserType == EINVOICE_USER_TYPE_ORGUNIT)
			{
				if (ou.getOrgUnitType() == GOConstants.ORGUNIT_DIVISION || ou.getOrgUnitType() == GOConstants.ORGUNIT_COMPANY)
					orgUnit = ou;
				else
				{
					GOBOOrgUnit boundDivision = ProjectUtilEInv.getBoundDivision(m_Context, ou);
					orgUnit = boundDivision;
				}
			}
		}
		return orgUnit;
	}
	
	/*private static String getReciverVKN_TCKN(int arpRef) {
		QueryParams params = new QueryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		params.getParameters().put("T1", arpRef);
		QueryBusinessObjects results = new QueryBusinessObjects();
		IQueryFactory factory = (IQueryFactory) m_Context.getQueryFactory();
		try {
			factory.select("CQOEInvoiceArpInfo.lqry", params, results, -1);
		} catch (Exception e) {
			m_Context
					.getLogger()
					.error("CQOEInvoiceArpInfo query could not be executed properly :",
							e);

		}
		if (results != null && results.size() > 0) {
			QueryBusinessObject result = results.get(0);
			return QueryUtil.getIntProp(result, "ISPERSCOMP") == 1 ? QueryUtil
					.getStringProp(result, "IDTCNO") : QueryUtil.getStringProp(
					result, "TAXNR");
		}
		return "";
	}
	
	
	private static void findAndSetOrgUnit()
	{
		if (m_Invoice.getDivisionRef() != 0)
		{
			GOBOOrgUnit ou = (GOBOOrgUnit) UnityHelper.getBOByReference(m_Context, GOBOOrgUnit.class, m_Invoice.getDivisionRef());
			if (ou.getOrgUnitType() == GOConstants.ORGUNIT_COMPANY)
				m_OrgUnit = ou;
			else
			{
				GOBOOrgUnit boundOu = GLHelper.getBoundFirm(m_Context, ou);
				m_OrgUnit = boundOu;
			}
		}
	}*/
	
	private static BigDecimal getTaxAmountTotal()
	{
		BigDecimal taxAmountTotal = UnityConstants.bZero;
		Set set = m_VatMapList.keySet();
		Iterator iter = set.iterator();
		while (iter.hasNext())
		{
			BigDecimal key = (BigDecimal) iter.next();
			TaxSubTotal taxSubTotal = (TaxSubTotal) m_VatMapList.get(key);
			taxAmountTotal = taxAmountTotal.add(taxSubTotal.taxAmount);
		}
		set = m_AddTaxMapList.keySet();
		iter = set.iterator();
		while (iter.hasNext())
		{
			String key = (String) iter.next();
			TaxSubTotal taxSubTotal = (TaxSubTotal) m_AddTaxMapList.get(key);
			taxAmountTotal = taxAmountTotal.add(taxSubTotal.taxAmount);
		}

		return taxAmountTotal;
	}
	
	private static Element createTaxTotals(Document invoiceUBLXML)
	{
		Element cacTaxTotal = invoiceUBLXML.createElement("cac:TaxTotal");
		// cbc:TaxAmount
		Element cbcTaxAmount = getElement(invoiceUBLXML, "cbc:TaxAmount");
		cbcTaxAmount.appendChild(invoiceUBLXML.createTextNode(getStringOfValue(getTaxAmountTotal())));
		cacTaxTotal.appendChild(cbcTaxAmount);
		createTaxTotal(invoiceUBLXML, m_VatMapList, cacTaxTotal);
		createTaxTotal(invoiceUBLXML, m_AddTaxMapList, cacTaxTotal);
		return cacTaxTotal;
	}
	
	private static void createTaxTotal(Document invoiceUBLXML, HashMap mapList, Element cacTaxTotal)
	{
		Set set = mapList.keySet();
		Iterator iter = set.iterator();
		while (iter.hasNext())
		{
			// cac:TaxSubtotal
			Element cacTaxSubtotal = invoiceUBLXML.createElement("cac:TaxSubtotal");
			TaxSubTotal taxSubTotal = (TaxSubTotal) mapList.get(iter.next());

			// Element cbcTaxableAmount =
			// invoiceUBLXML.createElement("cbc:TaxableAmount");
			// cbcTaxableAmount.setAttribute("currencyID", "TRL");
			// cacTaxSubtotal.appendChild(cbcTaxableAmount);

			// Element cbcCalculationSequenceNumeric =
			// invoiceUBLXML.createElement("cbc:CalculationSequenceNumeric");
			// cacTaxSubtotal.appendChild(cbcCalculationSequenceNumeric);

			Element cbcTaxAmount = getElement(invoiceUBLXML, "cbc:TaxAmount");
			cbcTaxAmount.appendChild(invoiceUBLXML.createTextNode(getStringOfValue(taxSubTotal.taxAmount)));
			cacTaxSubtotal.appendChild(cbcTaxAmount);

			Element cbcPercent = invoiceUBLXML.createElement("cbc:Percent");
			cbcPercent.appendChild(invoiceUBLXML.createTextNode(getStringOfValue(taxSubTotal.percent)));
			cacTaxSubtotal.appendChild(cbcPercent);

			Element cacTaxCategory = invoiceUBLXML.createElement("cac:TaxCategory");
			Element cacTaxCategoryScheme = invoiceUBLXML.createElement("cac:TaxScheme");

			Element cbcName = invoiceUBLXML.createElement("cbc:Name");
			cbcName.appendChild(invoiceUBLXML.createTextNode(taxSubTotal.taxTypeName));
			cacTaxCategoryScheme.appendChild(cbcName);

			Element cbcTaxTypeCode = invoiceUBLXML.createElement("cbc:TaxTypeCode");
			cbcTaxTypeCode.appendChild(invoiceUBLXML.createTextNode(taxSubTotal.taxTypeCode));
			cacTaxCategoryScheme.appendChild(cbcTaxTypeCode);

			cacTaxCategory.appendChild(cacTaxCategoryScheme);
			cacTaxSubtotal.appendChild(cacTaxCategory);
			cacTaxTotal.appendChild(cacTaxSubtotal);
		}
	}
	
	private static Element getElement(Document invoiceUBLXML, String elementCode)
	{
		Element element = null;
		if (elementCode.compareTo("cbc:Name") == 0)
		{
			element = invoiceUBLXML.createElement("cbc:Name");
		}
		else if (elementCode.compareTo("cac:TaxTotal") == 0)
		{
			// cac:TaxTotal
			
		}
		else if (elementCode.compareTo("cbc:TaxAmount") == 0)
		{
			element = invoiceUBLXML.createElement("cbc:TaxAmount");
			element.setAttribute("currencyID", "TRL");
		}
		return element;
	}
	
	private static String getProfileID()
	{
		String profileIDName = "";
		if (m_Invoice.getProfileId() == ProjectGlobalsEInv.PROFILE_ID_BASIC)
		{
			profileIDName = "TEMELFATURA";
		}
		else if (m_Invoice.getProfileId() == ProjectGlobalsEInv.PROFILE_ID_COMMERCIAL)
		{
			profileIDName = "TICARIFATURA";
		}

		return profileIDName;
	}
	
	private static String getCopyIndicator()
	{
		String copyIndicator = "";
		if (m_Invoice.getBOStatus() == UnityConstants.INVSTAT_DRAFT)
		{
			copyIndicator = "true";
		}
		if (m_Invoice.getBOStatus() == UnityConstants.INVSTAT_APPROVED)
		{
			copyIndicator = "false";
		}
		return copyIndicator;
	}
	
	private static String getIssueDate(Calendar date)
	{
		if (date != null)
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			return dateFormat.format(date.getTime());
		}
		return "";
	}
	
	private static String getIssueTime(Calendar date)
	{
		if (date != null)
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
			return dateFormat.format(date.getTime());
		}
		return "";
	}
	
	private static String getInvoiceTypeCode()
	{
		int invoiceType = m_Invoice.getInvoiceType();
		switch (invoiceType)
		{
			case UnityConstants.INVC_RETAIL:
			case UnityConstants.INVC_WHOLESALE:
			case UnityConstants.INVC_SERVICESSOLD:
				return "SATIS";
			case UnityConstants.INVC_PURCHASERET:
				return "IADE";
			default:
				break;
		}
		return "";
	}
	
	private static Element createNoteElement(IApplicationContext context, Document invoiceUBLXML)
	{
		Element cbcNote = invoiceUBLXML.createElement("cbc:Note");
		String notes  = m_Invoice.getFootnote();
		cbcNote.appendChild(invoiceUBLXML.createTextNode(notes));
		return cbcNote;
	}
	
	private static String getDocumentCurrencyCode()
	{
		return "TRL"; //container.getTaggedList(401).getValueAtTag(QueryUtil.getIntProp(qbo,"TCOfInvoice");
	}
	
	private static String getLineCountNumeric()
	{
		ArrayList codeList = new ArrayList();
		for (int i = 0; i < m_Invoice.getTransactions().size(); i++)
		{
			MMBOTransaction trans = m_Invoice.getTransactions().get(i);
			if (!codeList.contains(Integer.valueOf(trans.getMaster_Reference())))
				codeList.add(Integer.valueOf(trans.getMaster_Reference()));
		}
		return String.valueOf(codeList.size());
	}
	
	
	private static Element createID(Document invoiceUBLXML, int accountingType)
	{
		Element cbcID = invoiceUBLXML.createElement("cbc:ID");
		String attribute = "";
		if (accountingType == ACCOUNTING_TYPE_SUPPLIER)
			attribute = m_OrgUnit.getTaxNr().length() == 10 ? "VKN"
					: "VKN_TCKN";
		else if (accountingType == ACCOUNTING_TYPE_CUSTOMER)
			attribute = m_ArpInfo.getIsPersComp() == 1 ? "VKN_TCKN" : "VKN";
		
		cbcID.setAttribute("schemeID", attribute);
		cbcID.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getTaxNr()
				: getReciverVKN_TCKN()));
		return cbcID;
	}
	
	private static String getReciverVKN_TCKN()
	{
		 return m_ArpInfo.getIsPersComp() == 1 ? m_ArpInfo.getIDTCNo() : m_ArpInfo.getTax_Id();
	}
	
	
	private static Element createParty(Document invoiceUBLXML, int accountingType)
	{
		Element cacParty = invoiceUBLXML.createElement("cac:Party");
		cacParty.appendChild(createPartyIdentification(invoiceUBLXML, accountingType));
    				
		Element cacPartyName = createPartyName(invoiceUBLXML, accountingType);
		cacParty.appendChild(cacPartyName);
		
		cacParty.appendChild(createPostalAdress(invoiceUBLXML, accountingType));

		// cac:PartyTaxScheme
		Element cacPartyTaxScheme = invoiceUBLXML.createElement("cac:PartyTaxScheme");
		Element cacTaxScheme = invoiceUBLXML.createElement("cac:TaxScheme");
		Element cbcName =  getElement(invoiceUBLXML, "cbc:Name");
		cbcName.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getTaxOffice() : m_ArpInfo.getTax_Office()));
		cacTaxScheme.appendChild(cbcName);
		cacPartyTaxScheme.appendChild(cacTaxScheme);
		cacParty.appendChild(cacPartyTaxScheme);

		// cac:Contact
		Element cacContact = invoiceUBLXML.createElement("cac:Contact");
		
		Element cbcTelephone = invoiceUBLXML.createElement("cbc:Telephone");
		cbcTelephone.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getPhone() : m_ArpInfo.getTelephone1()));
		cacContact.appendChild(cbcTelephone);
		
		Element cbcTelefax = invoiceUBLXML.createElement("cbc:Telefax");
		cbcTelefax.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getFax() : m_ArpInfo.getFax()));
		cacContact.appendChild(cbcTelefax);
		
		Element cbcElectronicMail = invoiceUBLXML.createElement("cbc:ElectronicMail");
		cbcElectronicMail.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getEmployerIdEMail() : m_ArpInfo.getE_Mail()));
		cacContact.appendChild(cbcElectronicMail);
		
		cacParty.appendChild(cacContact);
		
		return cacParty;
	}
	
	private static Element createPartyIdentification(Document invoiceUBLXML, int accountingType)
	{
		Element cacPartyIdentification = invoiceUBLXML.createElement("cac:PartyIdentification");
		cacPartyIdentification.appendChild(createID(invoiceUBLXML, accountingType));
		return cacPartyIdentification;
	}

	private static Element createPartyName(Document invoiceUBLXML, int accountingType)
	{
		Element cacPartyName = invoiceUBLXML.createElement("cac:PartyName");
		Element cbcName = getElement(invoiceUBLXML, "cbc:Name");
		cbcName.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getTitle() : getRecieverTitle()));
		cacPartyName.appendChild(cbcName);
		return cacPartyName;
	}
	
	private static String getRecieverTitle()
	{
		return m_ArpInfo.getIsPersComp() == 1 ? "" : m_ArpInfo.getDescription();
	}
	
	private static Element createPostalAdress(Document invoiceUBLXML, int accountingType)
	{
		Element cacPostalAdress = invoiceUBLXML.createElement("cac:PostalAddress");
		Element cbcRoom = invoiceUBLXML.createElement("cbc:Room");
		
		Element cbcStreetName = invoiceUBLXML.createElement("cbc:StreetName");
		cbcStreetName.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getStreet() : m_ArpInfo.getAddress1()));
		
		Element cbcBuildingName = invoiceUBLXML.createElement("cbc:BuildingName");
		Element cbcBuildingNumber = invoiceUBLXML.createElement("cbc:BuildingNumber");
		cbcBuildingNumber.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getDoorNr() : ""));
		
		Element cbcCitySubdivisionName = invoiceUBLXML.createElement("cbc:CitySubdivisionName");
		cbcCitySubdivisionName.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getDistrict() : m_ArpInfo.getTown()));
		
		Element cbcCityName = invoiceUBLXML.createElement("cbc:CityName");
		cbcCityName.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getCity() : m_ArpInfo.getCity()));
		
		Element cbcPostalZone = invoiceUBLXML.createElement("cbc:PostalZone");
		cbcPostalZone.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getZipCode() : m_ArpInfo.getPostal_Code()));
		
		Element cbcRegion = invoiceUBLXML.createElement("cbc:Region");
		Element cbcCountry = invoiceUBLXML.createElement("cac:Country");
		
		Element cbcName = getElement(invoiceUBLXML, "cbc:Name");
		cbcName.appendChild(invoiceUBLXML.createTextNode(accountingType == 1 ? m_OrgUnit.getCountry() : m_ArpInfo.getCountry()));
		cbcCountry.appendChild(cbcName);
		
		cacPostalAdress.appendChild(cbcRoom);
		cacPostalAdress.appendChild(cbcStreetName);
		cacPostalAdress.appendChild(cbcBuildingName);
		cacPostalAdress.appendChild(cbcBuildingNumber);
		cacPostalAdress.appendChild(cbcCitySubdivisionName);
		cacPostalAdress.appendChild(cbcCityName);
		cacPostalAdress.appendChild(cbcPostalZone);
		cacPostalAdress.appendChild(cbcRegion);
		cacPostalAdress.appendChild(cbcCountry);
		
		return cacPostalAdress;
	}	
	
	
	

	private static void calculateTaxTotal(BusinessObjects<MMBOTransaction> transactions)
	{
		m_VatMapList.clear();
		m_AddTaxMapList.clear();
		for (int i = 0; i < transactions.size(); i++)
		{
			MMBOTransaction trans = transactions.get(i);
			MMBOItemLink item = (MMBOItemLink) trans.getitemLink();
			if (trans.getVATAmount().compareTo(UnityConstants.bZero) > 0)
			{
				if (m_VatMapList.containsKey(trans.getVATRate()))
				{
					TaxSubTotal taxSubTotal = (TaxSubTotal) m_VatMapList.get(trans.getVATRate());
					taxSubTotal.taxAmount = taxSubTotal.taxAmount.add(trans.getVATAmount());
					m_VatMapList.put(trans.getVATRate(), taxSubTotal);
				}
				else
				{
					TaxSubTotal taxSubTotal = new TaxSubTotal();
					taxSubTotal.taxAmount = trans.getVATAmount();
					taxSubTotal.taxTypeName = m_VATName;
					taxSubTotal.percent = trans.getVATRate();
					m_VatMapList.put(trans.getVATRate(), taxSubTotal);
				}
			}
			if (item != null && item.getAddTaxRef() != 0)
			{
				MMBOAdditionalTaxes addTax = (MMBOAdditionalTaxes) UnityHelper.getBOFieldsByRef(m_Context,
						MMBOAdditionalTaxes.class, item.getAddTaxRef(), new String[] { "GlobalCode" });
				if (m_AddTaxMapList.containsKey(addTax.getGlobalCode()))
				{
					TaxSubTotal taxSubTotal = (TaxSubTotal) m_AddTaxMapList.get(addTax.getGlobalCode());
					taxSubTotal.taxAmount = taxSubTotal.taxAmount.add(trans.getAddTaxAmount());
					m_VatMapList.put(addTax.getGlobalCode(), taxSubTotal);

				}
				else
				{
					TaxSubTotal taxSubTotal = new TaxSubTotal();
					taxSubTotal.taxAmount = trans.getAddTaxAmount();
					taxSubTotal.taxTypeName = addTax.getGlobalCodes().getGCDescription();
					taxSubTotal.percent = trans.getAddTaxRate();
					m_AddTaxMapList.put(addTax.getGlobalCode(), taxSubTotal);
				}

			}

		}

	}

	private static void setVatName()
	{
		ArrayList vatNameList = new ArrayList();
		try
		{
			vatNameList = UnityHelper.getFieldValueListByCondArray(m_Context, "GOGlobalCodes", "DESCRIPTION", "TAB.CODE = '0015'", "", 0, false);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_VATName = vatNameList.size() > 0 ? (String) vatNameList.get(0) : "";
	}
	

}
