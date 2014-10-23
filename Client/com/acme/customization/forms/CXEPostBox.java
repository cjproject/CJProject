package com.acme.customization.forms;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;






import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.sf.saxon.value.IntegerValue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.acme.customization.shared.ProjectGlobalsEInv;
import com.acme.customization.shared.ProjectUtilEInv;
import com.lbs.data.grids.JLbsQueryGrid;
import com.lbs.data.grids.JLbsQuerySelectionGrid;
import com.lbs.data.objects.BusinessObjects;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.data.objects.CustomBusinessObjects;
import com.lbs.data.query.QueryBuilderCopyHandler;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryParams;
import com.lbs.grid.interfaces.ILbsQueryGrid;
import com.lbs.grids.JLbsObjectListGrid;
import com.lbs.remoteclient.IClientContext;
import com.lbs.transport.RemoteMethodResponse;
import com.lbs.unity.UnityConstants;
import com.lbs.unity.lo.bo.LOBOInvoice;
import com.lbs.unity.mm.bo.MMBOItemLink;
import com.lbs.unity.mm.bo.MMBOUomDefinition;
import com.lbs.util.DateUtil;
import com.lbs.util.JLbsFileUtil;
import com.lbs.util.QueryUtil;
import com.lbs.xui.ILbsXUIPane;
import com.lbs.xui.JLbsXUILookupInfo;
import com.lbs.xui.JLbsXUIPane;
import com.lbs.xui.JLbsXUITypes;
import com.lbs.xui.customization.JLbsXUIDataGridEvent;
import com.lbs.xui.customization.JLbsXUIControlEvent;
import com.lbs.xui.customization.JLbsXUIGridEvent;

public class CXEPostBox {

	private static final int GRID_TAG_RECIEVED = 100;
	private static final int GRID_TAG_SENDING = 200;
	
	private JLbsXUIControlEvent m_Event = null;
	private JLbsXUIPane m_Container = null;
	private IClientContext m_Context = null;
	
	public void onReceivedGridModifyQuery(JLbsXUIDataGridEvent event)
	{
		JLbsQueryGrid grid = event.getQueryGrid();
		QueryParams params = grid.getQueryParams();
		params.getVariables().put("V_STATUSLIST", Integer.valueOf(0));
		params.getParameters().put("P_OPTYPE", ProjectGlobalsEInv.OPTYPE_RECEIVE);
		params.getEnabledTerms().disable("T_RECTYPE");
	}

	public void onClickSendRecieve(JLbsXUIControlEvent event)
	{
		m_Container.openChild("Forms/CXFSendRecieve.lfrm",	null, true, JLbsXUITypes.XUIMODE_DEFAULT);
		JLbsQuerySelectionGrid recivedGrid = (JLbsQuerySelectionGrid) event.getContainer().getComponentByTag(GRID_TAG_RECIEVED);
		JLbsQuerySelectionGrid sendingGrid = (JLbsQuerySelectionGrid) event.getContainer().getComponentByTag(GRID_TAG_SENDING);
		recivedGrid.queryParamsChanged();
		sendingGrid.queryParamsChanged();
	}

	/*
	 * Ticari profilde ve türü alýnan fatura ise (111) kaydetme formunu açýyorum yoksa direk kaydetme iþlemine geçiyorum..
	 * 
	 *  Ticari profilde Kabul/Red ekranýnda eðer kabul edilirse mesaj ekranýný açýyorum ve zarfla seçilirse postboxenvelope oluþturup 
	 *  gönderilmek üzere paketlenmiþ iþlemlerde gösterecek miyim ? Sonra Kaydedilecek faturayý da gösterecek miyim ? 
	 */
	public void onClickSaveRecieved(JLbsXUIControlEvent event)
	{
		
		CustomBusinessObject approvalBO = ProjectUtilEInv.createNewCBO("CBOApproval");
		approvalBO._setState(CustomBusinessObject.STATE_NEW);
		Calendar now = DateUtil.getToday();
		ProjectUtilEInv.setMemberValue(approvalBO, "Date_", ProjectUtilEInv.getToday(now));
		ProjectUtilEInv.setMemberValue(approvalBO, "Time_",  now.getTimeInMillis());
		ProjectUtilEInv.setMemberValue(approvalBO, "RecType", ProjectGlobalsEInv.RECTYPE_RECEIVED_SR);
		ProjectUtilEInv.setMemberValue(approvalBO, "Status", 0);
		ProjectUtilEInv.setMemberValue(approvalBO, "Sender", "1234567808");
		//FileName ve EnvelopeID alanlarýna gelen döküman türüne göre xml' de hangi alana bakýlmalý ? 
		ProjectUtilEInv.setMemberValue(approvalBO, "FileName", "74472467-65e3-4e52-ac9f-ef367e8967fc");
		ProjectUtilEInv.setMemberValue(approvalBO, "EnvelopeID", "Tc8330dd2-a6df-4335-9ec7-3c62a73a35ca");
		ProjectUtilEInv.setMemberValue(approvalBO, "EnvelopeType", ProjectGlobalsEInv.ENVELOPE_TYPE_SYSTEM);
		ProjectUtilEInv.setMemberValue(approvalBO, "TrCode", 0);
		ProjectUtilEInv.setMemberValue(approvalBO, "DocRef", 0);
		ProjectUtilEInv.setMemberValue(approvalBO, "OpType", ProjectGlobalsEInv.OPTYPE_INCOMING);
		ProjectUtilEInv.setMemberValue(approvalBO, "ProfileID", 0);
		ProjectUtilEInv.setMemberValue(approvalBO, "ReferenceID", "b2fcc68f-edb6-489d-999f-2c2cc303b8df");
		ProjectUtilEInv.setMemberValue(approvalBO, "RespCode", "1200");
		ProjectUtilEInv.setMemberValue(approvalBO, "Explain_", "BASARIYLA ISLENDI");

		String tmpDir = JLbsFileUtil.getTempDirectory();
		String xmlFileName = tmpDir + "SISTEM_YANITI_MERKEZ.XML";
		byte[] fileContents = null;
		try {
			fileContents = JLbsFileUtil.readFile(xmlFileName);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ProjectUtilEInv.setMemberValue(approvalBO, "LData", fileContents);
		
		ProjectUtilEInv.persistCBO(event.getClientContext(), approvalBO);
		
		ILbsQueryGrid recievedGrid = (ILbsQueryGrid) m_Container.getComponentByTag(100);
		QueryBusinessObject qbo = (QueryBusinessObject) recievedGrid.getSelectedObject();
		if (qbo == null)
			return;
		else if (QueryUtil.getIntProp(qbo, "RECTYPE") == ProjectGlobalsEInv.RECTYPE_RECEIVED_INV
				|| QueryUtil.getIntProp(qbo, "RECTYPE") == ProjectGlobalsEInv.RECTYPE_RECEIVED_RET_INV)
			checkAndOpenMappingInfo(qbo);
		try 
		{

			m_Container.openChild("Forms/CXFEDocument.lfrm", qbo, true, JLbsXUITypes.XUIMODE_DEFAULT);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		m_Container.refreshGrids();
	}


	public void onClickViewPackageContent(JLbsXUIControlEvent event)
	{
		JLbsXUILookupInfo info = new JLbsXUILookupInfo();
		QueryBusinessObject packedRecords= m_Container.getSelectedGridData(200);
		if (packedRecords != null)
		{
			String formName = "Forms/CXFSendedInvoicesBrowser.lfrm";
			info.setQueryParamValue("P_TRANSREF", QueryUtil.getIntegerProp(packedRecords, "LOGICALREF"));
			if(QueryUtil.getIntProp(packedRecords, "TRANS_TYPE") == ProjectGlobalsEInv.TRANSACTION_TYPE_APPRESP)
				formName = "Forms/CXFSendedPRBrowser.lfrm";
			m_Container.openChild(formName, info, true, JLbsXUITypes.XUIMODE_DEFAULT);
		}
	}


	public void onSendingGridGetCellValue(JLbsXUIGridEvent event)
	{
		if(event.getData() instanceof QueryBusinessObject)
		{
			if(event.getColumnTag() == 213) // Ýþlem Tipi
			{
				QueryBusinessObject qbo = (QueryBusinessObject) event.getData();
				event.setReturnObject(m_Container.getMessage(500001,	QueryUtil.getIntProp(qbo, "TRANS_TYPE")));	 
			}
		}
	}

	public void onRecievedGridGetCellValue(JLbsXUIGridEvent event)
	{
		if(event.getData() instanceof QueryBusinessObject)
		{
			if(event.getColumnTag() == 113) // Ýþlem Tipi
			{
				QueryBusinessObject qbo = (QueryBusinessObject) event.getData();
				int recType = QueryUtil.getIntProp(qbo, "RECTYPE");
				String value = m_Container.getMessage(500000,	recType);
				if(recType == ProjectGlobalsEInv.RECTYPE_RECEIVED_INV || recType == ProjectGlobalsEInv.RECTYPE_RECEIVED_RET_INV)	
					 value+= "-"+ m_Container.getMessage(12318,	QueryUtil.getIntProp(qbo, "TRCODE"));
				event.setReturnObject(value);	 
			}
			else if(event.getColumnTag() == 3000010) // Senaryo
			{
				QueryBusinessObject qbo = (QueryBusinessObject) event.getData();
				int profileID = QueryUtil.getIntProp(qbo, "PROFILEID");
				event.setReturnObject(m_Container.getMessage(70528, profileID));
			}
			
		}
	}

	public void onInitialize(JLbsXUIControlEvent event)
	{
		m_Event = event;
		m_Container = event.getContainer();
		m_Context = event.getClientContext();
		
	}

	public void onSendingGridCanUpdateObject(JLbsXUIDataGridEvent event)
	{
		onClickViewPackageContent(m_Event);
		event.setReturnObject(false);
	}
	
	public boolean onClickViewRecieved(ILbsXUIPane container, Object data, IClientContext context)
	{
		viewEDocument(container, container.getSelectedGridData(GRID_TAG_RECIEVED));
		return true;
	}
	
	public boolean onClickViewPackageContent(ILbsXUIPane container, Object data, IClientContext context)
	{
		onClickViewPackageContent(m_Event);
		return true;
	}
	

	private void viewEDocument(ILbsXUIPane container, QueryBusinessObject qbo)
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

	public void onPopupMenuFilter(JLbsXUIControlEvent event)
	{
		int focusCtrlTag = m_Container.getControlOfFocusOwner() == null ? 0 :m_Container.getControlOfFocusOwner().getTag();
		if (focusCtrlTag != GRID_TAG_RECIEVED && focusCtrlTag != GRID_TAG_SENDING)
			event.setReturnObject(false);
		else if(focusCtrlTag == GRID_TAG_RECIEVED && (event.getIndex() == 2 || event.getIndex() == 4))
			event.setReturnObject(false);
		else if(focusCtrlTag == GRID_TAG_SENDING && (event.getIndex() == 1 || event.getIndex() == 3 || event.getId() == 6))
			event.setReturnObject(false);
	}

	public void onRecievedGridCanUpdateObject(JLbsXUIDataGridEvent event)
	{
		QueryBusinessObject qbo = (QueryBusinessObject) m_Container.getSelectedGridData(GRID_TAG_RECIEVED);
		viewEDocument(m_Container, qbo);
		event.setReturnObject(false);
	}

	public void onClickViewRecieved(JLbsXUIControlEvent event)
	{
		QueryBusinessObject qbo = (QueryBusinessObject) m_Container.getSelectedGridData(GRID_TAG_RECIEVED);
		viewEDocument(m_Container, qbo);
	}

	public void onClickMappingInfo(ILbsXUIPane container, Object data, IClientContext context)
	{
		QueryBusinessObject qbo = (QueryBusinessObject) container.getSelectedGridData(GRID_TAG_RECIEVED);
		if(qbo == null)
			return ;
		checkAndOpenMappingInfo(qbo);
	}
	
	public void onClickSaveUBLToFile(ILbsXUIPane container, Object data, IClientContext context)
	{
		QueryBusinessObject qbo = (QueryBusinessObject) container.getSelectedGridData(GRID_TAG_RECIEVED);
		if(qbo == null)
			return ;
		String path = ProjectUtilEInv.getExportFilePath(container.getMessage(500008, 1), container.getMessage(500008, 2), "xml");
		if (path != null)
		{
			try
			{
				byte [] LData = QueryUtil.getByteArrProp(qbo.getProperties(), "LDATA");
				if (LData != null)
				{
					String lwPth = path.toLowerCase();
					int txtP = lwPth.indexOf(".xml");
					String filePath = (txtP > 0) ? path : path + ".xml";
					FileOutputStream file = new FileOutputStream(filePath);
					file.write(LData);
					file.close();
				}
			}
			catch (Exception e)
			{
				context.getLogger().error("Exception :", e);
			}

		}
			
	}
	
	
	
	private void checkAndOpenMappingInfo(QueryBusinessObject qbo) 
	{
		byte [] LData = QueryUtil.getByteArrProp(qbo.getProperties(), "LDATA");
		if (LData == null)
			return ;
		
		CustomBusinessObject approval = ProjectUtilEInv.readObject(m_Context, "CBOApproval", 
				QueryUtil.getIntProp(qbo, "LOGICALREF"));
		CustomBusinessObject mappingInfo = (CustomBusinessObject) ProjectUtilEInv.getMemberValue(approval, "MappingInfo");
		HashMap itemUnitMap = createItemUnitListByXML(LData);
		CustomBusinessObjects mappInfoLines = null;
		if (mappingInfo == null)
		{
			mappingInfo = ProjectUtilEInv.createNewCBO("CBOMappingInfo");
			mappingInfo.setMemberValue("Sender", QueryUtil.getStringProp(qbo, "SENDER"));
			Iterator iterator = itemUnitMap.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry element = (Entry) iterator.next();
				String itemName = (String) element.getKey();
				String itemGlobalUnit = (String) element.getValue();
				createNewMappingInfoLine(mappingInfo, itemName, itemGlobalUnit, ProjectGlobalsEInv.MAPPING_CARD_TYPE_ITEM);
			}
			mappInfoLines = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines");
		}
		else
		{
			ArrayList mappedItemCodeList = getMappedItemCodeList(mappingInfo);
			Iterator iterator = itemUnitMap.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry element = (Entry) iterator.next();
				String itemName = (String) element.getKey();
				String itemGlobalUnit = (String) element.getValue();
				if(!mappedItemCodeList.contains(itemName))
				{
					createNewMappingInfoLine(mappingInfo, itemName, itemGlobalUnit, ProjectGlobalsEInv.MAPPING_CARD_TYPE_ITEM);
					
				}
			}
			mappInfoLines = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines");
			for (int i = mappInfoLines.size() - 1; i >= 0; i--)
			{
				CustomBusinessObject line = (CustomBusinessObject) mappInfoLines.get(i);
				if(ProjectUtilEInv.getBOIntFieldValue(line, "LogicalRef") > 0 
						&& ProjectUtilEInv.getBOIntFieldValue(line, "MCardRef") > 0)
						mappInfoLines.remove(i);
				else
				{
					if (ProjectUtilEInv.getMemberValue(line, "Item") == null) {
						ProjectUtilEInv.setMemberValue(line, "Item",
								new MMBOItemLink());
					}
					if (ProjectUtilEInv.getMemberValue(line, "Units") == null) {
						ProjectUtilEInv.setMemberValue(line, "Units",
								new BusinessObjects<MMBOUomDefinition>());
					}
				}
			}
			
		}
		if (mappInfoLines != null && mappInfoLines.size() > 0)
		{
			mappInfoLines.getDeleted().clear();
			ProjectUtilEInv.setMemberValue(mappingInfo, "FromPostBox", Integer.valueOf(1));
			m_Container.openChild("Forms/CXFMappingInfo.lfrm", mappingInfo, true, JLbsXUITypes.XUIMODE_DBUPDATE);
		}
	}
	
	private ArrayList getMappedItemCodeList(CustomBusinessObject mappingInfo)
	{
		ArrayList mappedItemCodeList = new ArrayList();
		CustomBusinessObjects mappInfoLines = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines");
		if (mappInfoLines != null && mappInfoLines.size() > 0)
			for (int i = 0; i < mappInfoLines.size(); i++)
			{
				CustomBusinessObject mappInfoLine = (CustomBusinessObject) mappInfoLines.get(i);
				String code = ProjectUtilEInv.getBOStringFieldValue(mappInfoLine, "Code");
				if(!mappedItemCodeList.contains(code))
					mappedItemCodeList.add(code);
				
			}
		return mappedItemCodeList;
	}
	
	private void createNewMappingInfoLine(CustomBusinessObject mappingInfo, String itemCode, String unitCode, int cardType)
	{
		CustomBusinessObjects mappInfoLines = null;
		if (ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines") == null)
			mappInfoLines = new CustomBusinessObjects();
		else
			mappInfoLines = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(mappingInfo, "MappInfoLines");
		CustomBusinessObject mappInfoLine = ProjectUtilEInv.createNewCBO("CBOMappingInfoLine");
		ProjectUtilEInv.setMemberValue(mappInfoLine, "CardType", cardType);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "Code", itemCode); 
		ProjectUtilEInv.setMemberValue(mappInfoLine, "UnitCode", unitCode);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MCardType", cardType);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MCardRef", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MUnitRef", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "MUnitSetRef", 0);
		ProjectUtilEInv.setMemberValue(mappInfoLine, "Item", new MMBOItemLink());
		ProjectUtilEInv.setMemberValue(mappInfoLine, "Units", new BusinessObjects<MMBOUomDefinition>());
		mappInfoLines.add(mappInfoLine);
		ProjectUtilEInv.setMemberValue(mappingInfo, "MappInfoLines", mappInfoLines);
	}
		
	private HashMap createItemUnitListByXML(byte [] LData)
	{
		Document invoiceXML = ProjectUtilEInv.getXMLDocument(LData);
		NodeList nodeList = invoiceXML.getElementsByTagName("cac:InvoiceLine");
		HashMap itemUnitMap = new HashMap();
		for(int i=0; i < nodeList.getLength(); i++)
		{
			Element element = (Element) nodeList.item(i) ;
			String name = null;
			String globalUnitCode = null;
			if(element.getElementsByTagName("cbc:InvoicedQuantity").item(0) != null)
			{
				Node quantityNode = (Node) element.getElementsByTagName("cbc:InvoicedQuantity").item(0);
				//unit
				if (quantityNode.getAttributes().getNamedItem("unitCode") != null)
					globalUnitCode = quantityNode.getAttributes().getNamedItem("unitCode").getNodeValue();  
				
			}
			if(element.getElementsByTagName("cac:Item").item(0) != null)
			{
				name = getNodeTextByTagName(element, "cac:Item", "cbc:Name");
			}
			if (name != null && globalUnitCode != null)
			{
				if(itemUnitMap.containsKey(name))
				{
					String unitCode = (String) itemUnitMap.get(name);
					unitCode = globalUnitCode;
					itemUnitMap.put(name, unitCode);
					
				}
				else
				{
					itemUnitMap.put(name, globalUnitCode);
				}
			}
		}
		return itemUnitMap;
	}
		
	private  String getNodeTextByTagName(Element element, String elementName, String nodeName)
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


}
