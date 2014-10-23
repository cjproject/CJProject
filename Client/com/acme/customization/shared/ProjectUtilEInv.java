package com.acme.customization.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gwt.thirdparty.guava.common.io.Files;
import com.lbs.appobjects.GOBOOrgUnit;
import com.lbs.appobjects.GOConstants;
import com.lbs.appobjects.LbsUserLoginInfo;
import com.lbs.data.database.DBConnection;
import com.lbs.data.database.DBConnectionInfo;
import com.lbs.data.database.DBConnectionManager;
import com.lbs.data.database.DBException;
import com.lbs.data.factory.FactoryParams;
import com.lbs.data.factory.IFactorySearchConstants;
import com.lbs.data.factory.IObjectFactory;
import com.lbs.data.factory.ObjectFactoryException;
import com.lbs.data.objects.BasicBusinessObject;
import com.lbs.data.objects.BusinessObject;
import com.lbs.data.objects.BusinessObjects;
import com.lbs.data.objects.CustomBusinessObject;
import com.lbs.data.objects.CustomBusinessObjects;
import com.lbs.data.objects.ObjectValueManager;
import com.lbs.data.query.IQueryFactory;
import com.lbs.data.query.IServerQueryFactory;
import com.lbs.data.query.QueryBusinessObject;
import com.lbs.data.query.QueryBusinessObjects;
import com.lbs.data.query.QueryParams;
import com.lbs.grids.JLbsObjectListGrid;
import com.lbs.hr.em.bo.EMBOPerson;
import com.lbs.invoke.MethodInvoker;
import com.lbs.invoke.SessionReestablishedException;
import com.lbs.invoke.SessionTimeoutException;
import com.lbs.platform.interfaces.IApplicationContext;
import com.lbs.platform.interfaces.IServerContext;
import com.lbs.platform.server.LbsServerContext;
import com.lbs.remoteclient.IClientContext;
import com.lbs.tm.client.IOperationContext;
import com.lbs.tm.client.TaskClientContext;
import com.lbs.tm.server.ServerClientContext;
import com.lbs.transport.RemoteMethodResponse;
import com.lbs.util.Base64;
import com.lbs.util.ConvertUtil;
import com.lbs.util.JLbsFileUtil;
import com.lbs.util.JLbsStringList;
import com.lbs.util.JLbsStringListItemEx;
import com.lbs.util.QueryUtil;
import com.lbs.util.StringUtil;
import com.lbs.util.StringUtilExtra;
import com.lbs.xui.JLbsXUILookupInfo;
import com.lbs.xui.JLbsXUIPane;
import com.lbs.xui.JLbsXUITypes;
import com.lbs.xui.customization.JLbsXUIControlEvent;
import com.lbs.xui.customization.JLbsXUIEventBase;
import com.lbs.xui.customization.JLbsXUIGridEvent;
import com.lbs.unity.UnityConstants;
import com.lbs.unity.UnityFileExtFilter;
import com.lbs.unity.UnityHelper;
import com.lbs.unity.bo.UNEORecInfo;
import com.lbs.data.grids.JLbsQuerySelectionGrid;

public class ProjectUtilEInv
{
	
	public static String getExportFilePath(String dlgTitle, String fltDesc, String extension)
	{
		JFileChooser dialog = new JFileChooser("");
		dialog.setDialogTitle(dlgTitle);
		String[] gExts = new String[] { extension };
		UnityFileExtFilter filter = new UnityFileExtFilter(gExts);
		filter.setDescription(fltDesc);
		dialog.setFileFilter(filter);

		int returnVal = dialog.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File f = dialog.getSelectedFile();
			String fPath = f.getAbsolutePath();
			return fPath;
		}

		return null;
	}
	
	public static File createTempFile(String fileName, String extension)
	{
		return new File(Files.createTempDir(), fileName + extension);
	}
	
	public static SOAPMessage getSoapMessageFromString(String xml) throws SOAPException, IOException {
	    MessageFactory factory = MessageFactory.newInstance();
	    SOAPMessage message = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
	    return message;
	}
	
	public static void updateApprovalStatus(IApplicationContext context, int approvalRef, int status)
	{
		QueryParams params = new QueryParams();
		params.getMainTableParams().getEnabledColumns().enable("STATUS");
		params.getEnabledTerms().enable("T_APPROVALREF");
		params.getParameters().put("P_STATUS", Integer.valueOf(status));
		params.getParameters().put("P_APPROVALREF", Integer.valueOf(approvalRef));
		
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		IQueryFactory factory = (IQueryFactory) context.getQueryFactory();
		try {
			factory.executeServiceQuery("CQOUpdateApproval", params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	public static Document getXMLDocument(byte [] LData)
	{
		Document document = null;
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
				document = documentBuilder.parse(xmlFile);
				
				document.getDocumentElement().normalize();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return document;
	}
	
	public static Calendar getToday(Calendar now)
	{
		Calendar dt = new GregorianCalendar();
		dt.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE), 0, 0, 0);
		dt.set(Calendar.MILLISECOND, 0);
		return dt;
	}
	
	public static byte[] documentToByte(DOMSource source, Transformer transformer)
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); 
		StreamResult result = new StreamResult(outputStream); 
		try
		{
			transformer.transform (source, result);
		}
		catch (TransformerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		byte [] resultArray = outputStream.toByteArray();  
		return resultArray; 
	}
	
	public static QueryBusinessObject getConnParamRecord(IApplicationContext context) {
		QueryParams params = new QueryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		QueryBusinessObjects results = new QueryBusinessObjects();
		IQueryFactory factory = (IQueryFactory) context.getQueryFactory();
		try
		{
			factory.select("CQOGetConnParamRecord", params, results, -1);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		if(results.size() > 0)
			return results.get(0);
		return null;
	}
	
	
	public static byte[] findXMLContent(IApplicationContext context, int docRef)
	{
		byte[] logoObj = null;
		QueryBusinessObject qbo = null;
		try
		{
			IQueryFactory queryFactory = context.getQueryFactory();
			QueryParams queryParams = new QueryParams();
			queryParams.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			queryParams.getEnabledTerms().disableAll();
			queryParams.setOrder("byApprovalRef", true);
			queryParams.getParameters().put("P_DOCREF", docRef);
			queryParams.getEnabledTerms().enable("T_DOCREF");

			QueryBusinessObjects queryResult = new QueryBusinessObjects();

			boolean ok = queryFactory.select("CQOApprovalBrowser", queryParams, queryResult, -1);
			if (ok && (queryResult.size() > 0))
				qbo = queryResult.get(0);

			if (qbo != null)
				logoObj = qbo.getProperties().getByteArray("LDATA");

		}
		catch (Exception e)
		{
			context.getLogger().error("CQOApprovalBrowser query can not be executed properly", e);
		}

		return logoObj;

	}
	
	public static void updateEInvoiceStatus(IApplicationContext context, Object [] invoiceRefList, int eInvStatus) 
	{
		QueryParams params = new QueryParams();
		params.getEnabledTerms().enable("T2");
		params.getParameters().put("P_EINVOICESSTATUS", Integer.valueOf(eInvStatus));
		params.getVariables().put("V_INVOICEREFS", invoiceRefList);
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		IQueryFactory factory = (IQueryFactory) context.getQueryFactory();
		try {
			factory.executeServiceQuery("CQOUpdateEInvoiceStatus", params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	public static Calendar stringToDate(String issueDate, String issueTime) 
	{
		String format = issueTime.length() > 0 ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd"; 
		String date = issueDate + ' ' + issueTime;
		SimpleDateFormat curFormater = new SimpleDateFormat(format);
		Date dateObj = null;
		try 
		{
			dateObj = curFormater.parse(date);
		} 
		catch (ParseException e) 
		{
			e.printStackTrace();
		} 
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateObj);
		return calendar;
	}
	
	public static BusinessObjects searchBOListByCond(IApplicationContext context, Class boClass, String whereStr, int cascade)
	{
		FactoryParams fParam = new FactoryParams();
		fParam.setRequestLock(false);
		fParam.setMaxCascadeDepth(cascade == 0 ? 1 : cascade);
		fParam.setActiveFilter(whereStr); 
		
		try
		{
			BusinessObjects objList = new BusinessObjects();
			BusinessObject refObj = (BusinessObject) boClass.newInstance();
			if (context.getObjectFactory().search(objList, refObj, fParam, 0, IObjectFactory.SEARCH_GT, false))
			{
				if (objList != null && objList.size() > 0)
				{
					return objList;
				}
			}
		}
		catch (Exception e)
		{
			context.getLogger().error("readItemListByKey can not be executed properly: ", e);
		}
		
		return null;
	}
	
	public static void writetoFile(byte[] data, String fileName)
	{
		try
		{
			FileOutputStream out = new FileOutputStream(fileName);
			out.write(data);
			out.close();
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	
	public static boolean showEInvoiceObjects(byte[] data)
	{
		boolean okay = false;
		if (data != null && data.length > 0)
		{
			String tmpDir = JLbsFileUtil.getTempDirectory();
			String xmlFileName = tmpDir + "EINVOICE.XML";
			writetoFile(data, xmlFileName);
			String fileName = null;
			String encodedText = null;
			File xmlFile = new File(xmlFileName);

			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			documentFactory.setNamespaceAware(true);
			try
			{
				DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();				
				Document doc = documentBuilder.parse(xmlFile);

				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("cbc:EmbeddedDocumentBinaryObject");

				for (int temp = 0; temp < nodeList.getLength(); temp++)
				{
					Node node = nodeList.item(temp);
					encodedText = node.getFirstChild().getNodeValue();

					if (node.getNodeType() == Node.ELEMENT_NODE)
					{

						Element fileNode = (Element) node;
						NamedNodeMap map = fileNode.getAttributes();

						int i = 0;
						while (map != null && fileName == null && i < map.getLength())
						{
							Node aNode = map.item(i);
							if (aNode.getNodeName().compareTo("filename") == 0)							
							fileName = aNode.getNodeValue();
							
							i++;
						}

					}
				}			
				 
				if (fileName != null)
				{
					String styleSheet = "type=\"text/xsl\" href=\"" + fileName + "\"";					
					doc.insertBefore(doc.createProcessingInstruction("xml-stylesheet", styleSheet), doc.getFirstChild());				   
				    TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					transformer.setOutputProperty(OutputKeys.METHOD,"xml");
					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					transformer.setOutputProperty(OutputKeys.INDENT, "no");
					DOMSource source = new DOMSource(doc);					
					StreamResult result = new StreamResult(new File(xmlFileName));
					transformer.transform(source, result);
					
					fileName = tmpDir + fileName;
					byte[] decodedText = Base64.decode(encodedText);
					writetoFile(decodedText, fileName);
					okay = true;
				}    
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

		}
      return okay;
	}
	
	public static GOBOOrgUnit getBoundDivision(IApplicationContext context, GOBOOrgUnit orgUnit)
	{
		if (orgUnit.getOrgUnitType() == GOConstants.ORGUNIT_DIVISION)
			return orgUnit;
		
		GOBOOrgUnit division = null;
		try
		{
			division = (GOBOOrgUnit) UnityHelper.getBOByReference(context, GOBOOrgUnit.class, orgUnit.getParentRef());
			if (division != null)
			{
				if (division.getOrgUnitType() == GOConstants.ORGUNIT_DIVISION)
					return division;
				else
					return getBoundDivision(context, division);
			}
		}
		catch (Exception e)
		{
			context.getLogger().error("getBoundFirm() caused an exception", e);
		}
		return division;

	}

	public static String convertDateToXSDDateTime()
	{
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return sdf.format(new java.util.Date());
	}
	
	public static String generateGUID()
	{
		UUID uuid = UUID.randomUUID();
		String randomUUIDString = (uuid.toString().toUpperCase());
		return randomUUIDString;
	}
	
	public static int getIntValueOfCheckBox(Object obj, String fieldName)
	{
		if(ProjectUtilEInv.getBOFieldValue(obj, fieldName) instanceof Boolean)
			return ((Boolean)ProjectUtilEInv.getBOFieldValue(obj, fieldName)).booleanValue() == true ? 1 : 0;
		else
			return ProjectUtilEInv.getBOIntFieldValue(obj, fieldName); 
	}

	
	public static Calendar concatDates(Calendar date, Calendar time)
	{
		if(date != null){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, date.get(Calendar.YEAR));
			cal.set(Calendar.MONTH, date.get(Calendar.MONTH));
			cal.set(Calendar.DATE, date.get(Calendar.DATE));
			if(time != null){
				cal.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
				cal.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
				cal.set(Calendar.SECOND, time.get(Calendar.SECOND));
				cal.set(Calendar.MILLISECOND, 0);
			}
			return cal;
		}else
			return null;		
	}

	
	public static String CalendarToStr (Calendar date) 
	{
		if (date == null)
			return "...";
		else 
		{
			String dText = StringUtilExtra.padLeft("" + date.get(Calendar.DAY_OF_MONTH), '0', 2);
			dText = dText + "/" + StringUtilExtra.padLeft("" + ConvertUtil.intToStr(date.get(Calendar.MONTH) + 1), '0', 2);
			dText = dText + "/" + date.get(Calendar.YEAR);
			return dText;
		}
	}

	public static IServerContext getServerContext(IOperationContext context)
	{
		IServerContext serverContext = null;
		
		IClientContext clientContext = context.getClientContext();
		TaskClientContext taskClientContext = null;
		ServerClientContext serverClientContext = null;
	
		
		if (clientContext instanceof TaskClientContext)
		{
	
				taskClientContext = (TaskClientContext) clientContext;
				clientContext = taskClientContext.getInnerClientContext();
				
			
				if (clientContext instanceof ServerClientContext)
				{
					serverClientContext = (ServerClientContext) clientContext;
					serverContext = serverClientContext.getServerContext();
				}	
		}
		
		return serverContext;
	}
	public static final String ms_UserInfo = "UserLoginInfo";//GOConstants.ms_UserInfo
	private static Hashtable applicationClasses = null;
	
	public static String convertion(int number)
	{
		
		if (Integer.toString(number).length() == 1)
		{
			return "0" + Integer.toString(number);
		}
		else
		{
			return  Integer.toString(number);
		}
	}
	public static  String getSlipNumber()
	{

		Calendar cal=Calendar.getInstance();
		String result = "";
		
		int year=cal.get(Calendar.YEAR)%1000;
		int month =cal.get(Calendar.MONTH);
		int day  =cal.get(Calendar.DAY_OF_MONTH);
		int hour=cal.get(Calendar.HOUR_OF_DAY);
		int MINUTE=cal.get(Calendar.MINUTE);
		int SECOND = cal.get(Calendar.SECOND);
		int MILLISECOND=cal.get(Calendar.MILLISECOND);
		
		result = convertion(year)+convertion(month)
		+convertion(day)+convertion(hour)+
		
		convertion(MINUTE)+convertion(SECOND)+convertion(MILLISECOND);
		return result;
	
 	}
	public static void setMemberValue(CustomBusinessObject lineObj, String memberName, Object value)
	{
		try
		{
			ObjectValueManager.setGridMemberValue(lineObj, memberName, value, false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void setMemberValueUn(CustomBusinessObject lineObj, String memberName, Object value)
	{
		try
		{
			ObjectValueManager.setMemberValue(lineObj, memberName, value);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static Object getMemberValue(CustomBusinessObject lineObj, String memberName)
	{
		try
		{
			return ObjectValueManager.getMemberValue(lineObj, memberName);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public static QueryBusinessObjects runQuery(JLbsXUIEventBase event, String queryName, String[] paramNames, Object[] paramVals,
			String[] enabledTerms, String[] disabledTerms)
	{
		if (paramNames == null || paramVals == null || paramNames.length != paramNames.length)
			return null;
		IClientContext context = event.getContainer().getContext();
		QueryParams params = new QueryParams();
		if (enabledTerms != null && enabledTerms.length > 0)
			for (int i = 0; i < enabledTerms.length; i++)
				params.getEnabledTerms().enable(enabledTerms[i]);
		if (disabledTerms != null && disabledTerms.length > 0)
			for (int i = 0; i < disabledTerms.length; i++)
				params.getEnabledTerms().disable(disabledTerms[i]);
		QueryBusinessObjects items = new QueryBusinessObjects();
		try
		{
			for (int i = 0; i < paramNames.length; i++)
				params.getParameters().put(paramNames[i], paramVals[i]);
			params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			boolean ok = context.getQueryFactory().first(queryName, params, items, -1, false);
			if (ok && items.size() > 0)
				return items;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static QueryBusinessObjects runQuery(JLbsXUIEventBase event, String queryName, String[] paramNames, Object[] paramVals,
			String[] enabledTerms, String[] disabledTerms,int rowCount)
	{
		if (paramNames == null || paramVals == null || paramNames.length != paramNames.length)
			return null;
		IClientContext context = event.getContainer().getContext();
		QueryParams params = new QueryParams();
		if (enabledTerms != null && enabledTerms.length > 0)
			for (int i = 0; i < enabledTerms.length; i++)
				params.getEnabledTerms().enable(enabledTerms[i]);
		if (disabledTerms != null && disabledTerms.length > 0)
			for (int i = 0; i < disabledTerms.length; i++)
				params.getEnabledTerms().disable(disabledTerms[i]);
		QueryBusinessObjects items = new QueryBusinessObjects();
		try
		{
			for (int i = 0; i < paramNames.length; i++)
				params.getParameters().put(paramNames[i], paramVals[i]);
			params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			boolean ok = context.getQueryFactory().first(queryName, params, items, rowCount, false);
			if (ok && items.size() > 0)
				return items;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	
	public static QueryBusinessObjects runQuery(IClientContext context, String queryName, String[] paramNames, Object[] paramVals,
			String[] enabledTerms, String[] disabledTerms)
	{
		if (paramNames == null || paramVals == null || paramNames.length != paramNames.length)
			return null;		
		QueryParams params = new QueryParams();
		if (enabledTerms != null && enabledTerms.length > 0)
			for (int i = 0; i < enabledTerms.length; i++)
				params.getEnabledTerms().enable(enabledTerms[i]);
		if (disabledTerms != null && disabledTerms.length > 0)
			for (int i = 0; i < disabledTerms.length; i++)
				params.getEnabledTerms().disable(disabledTerms[i]);
		QueryBusinessObjects items = new QueryBusinessObjects();
		try
		{
			for (int i = 0; i < paramNames.length; i++)
				params.getParameters().put(paramNames[i], paramVals[i]);
			params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			boolean ok = context.getQueryFactory().first(queryName, params, items, -1, false);
			if (ok && items.size() > 0)
				return items;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	public static QueryBusinessObjects runQueryForSingleRow(JLbsXUIEventBase event, String queryName, String[] paramNames, Object[] paramVals,
			String[] enabledTerms, String[] disabledTerms)
	{
		if (paramNames == null || paramVals == null || paramNames.length != paramNames.length)
			return null;
		IClientContext context = event.getContainer().getContext();
		QueryParams params = new QueryParams();
		if (enabledTerms != null && enabledTerms.length > 0)
			for (int i = 0; i < enabledTerms.length; i++)
				params.getEnabledTerms().enable(enabledTerms[i]);
		if (disabledTerms != null && disabledTerms.length > 0)
			for (int i = 0; i < disabledTerms.length; i++)
				params.getEnabledTerms().disable(disabledTerms[i]);
		QueryBusinessObjects items = new QueryBusinessObjects();
		try
		{
			for (int i = 0; i < paramNames.length; i++)
				params.getParameters().put(paramNames[i], paramVals[i]);
			params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			boolean ok = context.getQueryFactory().first(queryName, params, items, 1, false);
			if (ok && items.size() > 0)
				return items;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}	
	
	public static QueryBusinessObjects runQuery2(IClientContext context, String queryName, String[] paramNames, Object[] paramVals,
			String[] enabledTerms, String[] disabledTerms)
	{
		if (paramNames == null || paramVals == null || paramNames.length != paramNames.length)
			return null;
		QueryParams params = new QueryParams();
		if (enabledTerms != null && enabledTerms.length > 0)
			for (int i = 0; i < enabledTerms.length; i++)
				params.getEnabledTerms().enable(enabledTerms[i]);
		if (disabledTerms != null && disabledTerms.length > 0)
			for (int i = 0; i < disabledTerms.length; i++)
				params.getEnabledTerms().disable(disabledTerms[i]);
		QueryBusinessObjects items = new QueryBusinessObjects();
		try
		{
			for (int i = 0; i < paramNames.length; i++)
				params.getParameters().put(paramNames[i], paramVals[i]);
			params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
			boolean ok = context.getQueryFactory().first(queryName, params, items, -1, false);
			if (ok && items.size() > 0)
				return items;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static QueryBusinessObjects runQuery(JLbsXUIEventBase event, String queryName, String[] paramNames, Object[] paramVals)
	{
		return runQuery(event, queryName, paramNames, paramVals, null, null);
	}
	public static QueryBusinessObject runSingleLineQuery(JLbsXUIEventBase event, String queryName, String[] paramNames,
			Object[] paramVals, String[] enabledTerms, String[] disabledTerms)
	{
		QueryBusinessObjects lines = runQuery(event, queryName, paramNames, paramVals, enabledTerms, disabledTerms);
		if (lines != null && lines.size() > 0)
			return lines.elementAt(0);
		return null;
	}
	
	public static QueryBusinessObject runSingleLineQueryReal(JLbsXUIEventBase event, String queryName, String[] paramNames,
			Object[] paramVals, String[] enabledTerms, String[] disabledTerms)
	{	
		QueryBusinessObjects lines = runQueryForSingleRow(event, queryName, paramNames, paramVals, enabledTerms, disabledTerms);
		if (lines != null && lines.size() > 0)
			return lines.elementAt(0);
		return null;
	}
	
	public static QueryBusinessObject runSingleLineQuery(JLbsXUIEventBase event, String queryName, String[] paramNames,
			Object[] paramVals)
	{
		return runSingleLineQuery(event, queryName, paramNames, paramVals, null, null);
	}
	public static Object getContextVar(JLbsXUIControlEvent event, String varName)
	{
		IClientContext context = event.getClientContext();
		return context.getVariable(varName);
	}
	public static void setContextVar(JLbsXUIControlEvent event, String varName, Object value)
	{
		IClientContext context = event.getClientContext();
		context.setVariable(varName, value);
	}
	public static void warn(JLbsXUIEventBase event, String msg)
	{
		event.getClientContext().showMessage("", msg);
	}
	/*public static boolean Confirm(JLbsXUIEventBase event, String msg)
	{
		JLbsXUIPane container = event.getContainer();
		return container.confirmed(msg);
	}
	
	public static void warn(JLbsXUIEventBase event, String msg, JLbsStringList list)
	{
		JLbsXUIPane container = event.getContainer();
		container.messageDialog(msg, list);
	}*/
	public static CustomBusinessObject readObject(JLbsXUIControlEvent event, String BOName, int BORef)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		CustomBusinessObject CBO = new CustomBusinessObject(BOName);
		CBO.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			boolean ok = objFactory.read(CBO, params, BORef);
			if (ok)
				return CBO;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static CustomBusinessObject readObject(IApplicationContext context, String BOName, int BORef)
	{
		IObjectFactory objFactory = context.getObjectFactory();
		CustomBusinessObject CBO = new CustomBusinessObject(BOName);
		CBO.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			boolean ok = objFactory.read(CBO, params, BORef);
			if (ok)
				return CBO;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static CustomBusinessObject readObject(JLbsXUIEventBase event, String BOName, int BORef)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		CustomBusinessObject CBO = new CustomBusinessObject(BOName);
		CBO.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			boolean ok = objFactory.read(CBO, params, BORef);
			if (ok)
				return CBO;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}	
	
	

	public static CustomBusinessObject readObjectGrid(JLbsXUIGridEvent event, String BOName, int BORef)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		CustomBusinessObject CBO = new CustomBusinessObject(BOName);
		CBO.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		
		try
		{
			boolean ok = objFactory.read(CBO, params, BORef);
			if (ok)
				return CBO;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static int daysBetween(Calendar begDate, Calendar endDate)
	{
		Calendar c = Calendar.getInstance();

		int begY = begDate.get(Calendar.YEAR);
		int endY = endDate.get(Calendar.YEAR);
		int numDays = 0;

		for (int i = begY; i <= endY; i++)
		{
			int begDay = 0;
			int endDay = 0;

			if (i == begY)
				begDay = begDate.get(Calendar.DAY_OF_YEAR);
			else
				begDay = 0;

			if (i == endY)
				endDay = endDate.get(Calendar.DAY_OF_YEAR);
			else
			{
				c.set(i, 11, 31);
				endDay = c.get(Calendar.DAY_OF_YEAR);
			}

			numDays = numDays + (endDay - begDay);

		}

		return numDays;
	}
/*	public static void showFormParameters(JLbsXUIControlEvent event)
	{
		JLbsXUIPane container = event.getContainer();
		Object data = container.getData();
		JLbsStringList list = new JLbsStringList();
		if (data == null)
			warn(event, "Form data is null!");
		else if (data instanceof JLbsXUILookupInfo)
		{
			list.add("Form data type is JLbsXUILookupInfo");
			JLbsXUILookupInfo info = (JLbsXUILookupInfo) data;
			if (info.getParameters() != null)
			{
				list.add("");
				list.add("Parameters:");
				HashMap map = info.getParameters();
				Set s = map.keySet();
				Iterator itr = s.iterator();
				while (itr.hasNext())
				{
					String itemName = null;
					Object obj = itr.next();
					if (obj != null)
						itemName = obj.toString();
					if (itemName != null)
						list.add("  " + itemName + ", value: " + info.getParameter(itemName));
				}
			}
			if (info.getQueryParamCount() > 0)
			{
				list.add("");
				list.add("Query parameters:");
				for (int i = 0; i < info.getQueryParamCount(); i++)
					list.add("  " + info.getQueryParam(i) + ", value: " + info.getQueryParamValue(i));
			}
			if (info.getQueryParamCount() > 0)
			{
				list.add("");
				list.add("Query variables:");
				for (int i = 0; i < info.getQueryVariableCount(); i++)
					list.add("  " + info.getQueryVariable(i) + ", value: " + info.getQueryVariableValue(i));
			}
			warn(event, "Form data:", list);
		}
		else
		{
			warn(event, "Form data: " + data.toString());
		}
	}
	*/
	public static Object getBOFieldValue(Object bo, String fieldName)
	{
		//BusinessObject bObject = (BusinessObject) bo;
		if (bo != null)
		{
			try
			{
				Object retObj = UnityHelper.getPropertyValue(bo, fieldName);
				return retObj;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static Calendar getBOCalendarFieldValue(Object bo, String fieldName)
	{
		Object o = getBOFieldValue(bo, fieldName);
		return o == null
				? null
				: ((Calendar) o);
	}
	
	public static boolean getBOBooleanFieldValue(Object bo, String fieldName)
	{
		Object o = getBOFieldValue(bo, fieldName);
		return o == null
				? false
				: ((Boolean) o).booleanValue();
	}
	
	public static BigDecimal getBOBigDecimalFieldValue(Object bo, String fieldName)
	{
		Object o = getBOFieldValue(bo, fieldName);
		if ( o instanceof Double)
			return new BigDecimal(((Double)  o).doubleValue());
		else if ( o instanceof BigDecimal)
			return (BigDecimal)  o;
			
		return UnityConstants.bZero;
	}
	
	public static Integer getBOIntegerFieldValue(Object bo, String fieldName)
	{
		Object o = getBOFieldValue(bo, fieldName);
		return o != null ? ((Integer) o) : Integer.valueOf(0);
	}
	
	public static int getBOIntFieldValue(Object bo, String fieldName)
	{
		Object o = getBOFieldValue(bo, fieldName);
		return o == null
				? 0
				: ((Integer) o).intValue();
	}
	public static String getBOStringFieldValue(Object bo, String fieldName)
	{
		Object o = getBOFieldValue(bo, fieldName);
		if (o != null)
		{
			String s = (String) o;
			return s;
		}
		return "";
	}
	public static Calendar getBODateFieldValue(Object bo, String fieldName)
	{
		Object o = getBOFieldValue(bo, fieldName);
		if (o != null)
		{
			Calendar c = (Calendar) o;
			return c;
		}
		return null;
	}
	public static CustomBusinessObject createNewCBO(String CBOName)
	{
		CustomBusinessObject CBO = new CustomBusinessObject(CBOName);
		CBO.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		CBO.createLinkedObjects();
		return CBO;
	}
	public static CustomBusinessObject checkAndCreateNewCBO(BasicBusinessObject BO, String CBOName)
	{
		CustomBusinessObject editCBO = BO.getExtensions().get(CBOName);
		if (editCBO == null)
		{
			editCBO = ProjectUtilEInv.createNewCBO(CBOName);
			BO.getExtensions().add(editCBO);
		}
		return editCBO;
	}
	public static CustomBusinessObject getCBO(BasicBusinessObject BO, String CBOName)
	{
		CustomBusinessObject editCBO = BO.getExtensions().get(CBOName);
		return editCBO;
	}
	public static void checkAndCreateLinkedObjects(BusinessObject BO)
	{
		BO.getInitialValues().setEnabled(false);
		BO.createLinkedObjects();
		BO.getInitialValues().setEnabled(true);
	}
	public static void insertNewLine(JLbsXUIEventBase event, CustomBusinessObjects lines, String lineCBOName)
	{
		CustomBusinessObject newLine = ProjectUtilEInv.createNewCBO(lineCBOName);
		lines.add(newLine);
	}
	public static void insertNewLineToGrid(JLbsXUIEventBase event, JLbsObjectListGrid objGrid, String lineCBOName)
	{
		try
		{
			CustomBusinessObjects lines = (CustomBusinessObjects) objGrid.getObjects();
			System.out.println("** objGrid.getObjects():" + objGrid.getObjects().getClass());
			insertNewLine(event, lines, lineCBOName);
			objGrid.rowListChanged();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void deleteGridRow(JLbsXUIEventBase event, JLbsObjectListGrid objGrid, int lineNr)
	{
		if (objGrid != null)
		{
			try
			{
				CustomBusinessObjects lines = (CustomBusinessObjects) objGrid.getObjects();
				if (lines.size() > 0 && lineNr < lines.size() && lineNr >= 0)
				{
					lines.delete(lineNr);
					objGrid.rowListChanged();
					if (lineNr > 0)
					{
						objGrid.setSelectedRow(lineNr - 1);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	public static boolean eventGridTagIs(JLbsXUIGridEvent event, int gridTag)
	{
		JLbsXUIPane container = event.getContainer();
		JLbsObjectListGrid grid = (JLbsObjectListGrid) container.getComponentByTag(gridTag);
		JLbsObjectListGrid objGrid = (JLbsObjectListGrid) event.getGrid();
		return grid == objGrid;
	}
	public static String getCompanyNumber(IClientContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		int firmNr = userLoginInfo.getFirm().getNr();
		String firmNrStr = "" + firmNr;
		if (firmNrStr.length() == 1)
		{
			firmNrStr = "00" + firmNrStr;
		}
		if (firmNrStr.length() == 2)
		{
			firmNrStr = "0" + firmNrStr;
		}
		return firmNrStr;
	}
	public static String getCompanyNumber(IServerContext context)
	{
		LbsUserLoginInfo userLoginInfo;		
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		int firmNr = userLoginInfo.getFirm().getNr();
		String firmNrStr = "" + firmNr;
		if (firmNrStr.length() == 1)
		{
			firmNrStr = "00" + firmNrStr;
		}
		if (firmNrStr.length() == 2)
		{
			firmNrStr = "0" + firmNrStr;
		}
		return firmNrStr;
	}
	
	public static String getPeriodNumber(IServerContext context)
	{
		int periodNr = getPeriodNr(context);
		String periodNrStr = "" + periodNr;
		if (periodNrStr.length() == 1)
		{
			periodNrStr = "0" + periodNrStr;
		}
		return periodNrStr;
	}
	
	public static int getPeriodNr(IServerContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getPeriod().getNr();
	}
	
	public static String getCompanyNumber (JLbsXUIEventBase event)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) event.getClientContext().getVariable(ms_UserInfo);
		int firmNr = userLoginInfo.getFirm().getNr();
		String firmNrStr = "" + firmNr;
		if (firmNrStr.length() == 1)
		{
			firmNrStr = "00" + firmNrStr;
		}
		if (firmNrStr.length() == 2)
		{
			firmNrStr = "0" + firmNrStr;
		}
		return firmNrStr;
	}
	
	public static String getPeriodNumber(JLbsXUIEventBase event)
	{
		int periodNr = getPeriodNumber(event.getClientContext());
		String periodNrStr = "" + periodNr;
		if (periodNrStr.length() == 1)
		{
			periodNrStr = "0" + periodNrStr;
		}
		return periodNrStr;
	}
	
	public static String getPeriodNr(IClientContext context)
	{
		int periodNr = getPeriodNumber(context);
		String periodNrStr = "" + periodNr;
		if (periodNrStr.length() == 1)
		{
			periodNrStr = "0" + periodNrStr;
		}
		return periodNrStr;
	}
	
	public static int getPeriodNumber(IClientContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getPeriod().getNr();
	}
	public static int getCompanyRef(IApplicationContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getFirm().getLogicalRef();
	}
	public static int getCompanyRef(IServerContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getFirm().getLogicalRef();
	}
	public static String getCompanyName(IApplicationContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getFirm().getName();
	}
	public static int getUserNr(IClientContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getUser().getNr();
	}
	public static int getUserNr(IServerContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getUser().getNr();
	}
	public static int getUserLRef(IClientContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getUser().getLogicalRef();
	}
	public static int getUserLRef(IServerContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);
		return userLoginInfo.getUser().getLogicalRef();
	}
	public static boolean getIsSuperUser(IClientContext context)
	{
		LbsUserLoginInfo userLoginInfo;
		userLoginInfo = (LbsUserLoginInfo) context.getVariable(ms_UserInfo);		
		return userLoginInfo.isSuperUser();
	}
	public static GOBOOrgUnit getUserOrgUnit(IClientContext context)
	{
		int userNr = getUserNr(context);
		int orgUnitRef = 0;
		EMBOPerson personCrdTmp = new EMBOPerson();
		FactoryParams paramHrTmp = new FactoryParams();
		paramHrTmp.setRequestLock(false);
		paramHrTmp.setActiveFilter("USERNR = " + userNr);
		try {
			if (context.getObjectFactory().search(personCrdTmp, paramHrTmp, IFactorySearchConstants.SEARCH_GE))
			{
				orgUnitRef = personCrdTmp.getUnit_Reference();
			}
		} catch (ObjectFactoryException e) {			
			e.printStackTrace();
		} catch (SessionTimeoutException e) {
			e.printStackTrace();
		} catch (SessionReestablishedException e) {
			e.printStackTrace();
		}		
		if (orgUnitRef <= 0)
			return null;
		
		GOBOOrgUnit res = new GOBOOrgUnit();
		FactoryParams param = new FactoryParams();
		param.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		param.setRequestLock(false);		
		try {
			 context.getObjectFactory().read(res, param, orgUnitRef);
			 if(res != null)
				 return res;
			
		} catch (ObjectFactoryException e) {			
			e.printStackTrace();
		} catch (SessionTimeoutException e) {
			e.printStackTrace();
		} catch (SessionReestablishedException e) {
			e.printStackTrace();
		}		
		return null;
	}
	public static GOBOOrgUnit getUserOrgUnit(IServerContext context)
	{
		int userNr = getUserNr(context);
		int orgUnitRef = 0;
		EMBOPerson personCrdTmp = new EMBOPerson();
		FactoryParams paramHrTmp = new FactoryParams();
		paramHrTmp.setRequestLock(false);
		paramHrTmp.setActiveFilter("USERNR = " + userNr);
		try {
			if (context.getObjectFactory().search(personCrdTmp, paramHrTmp, IFactorySearchConstants.SEARCH_GE))
			{
				orgUnitRef = personCrdTmp.getUnit_Reference();
			}
		} catch (ObjectFactoryException e) {			
			e.printStackTrace();
		} catch (SessionTimeoutException e) {
			e.printStackTrace();
		} catch (SessionReestablishedException e) {
			e.printStackTrace();
		}		
		if (orgUnitRef <= 0)
			return null;
		
		GOBOOrgUnit res = new GOBOOrgUnit();
		FactoryParams param = new FactoryParams();
		param.setRequestLock(false);		
		try {
			if (context.getObjectFactory().read(res, param, orgUnitRef))
			{
				return res;
			}
			else
			{
				return null;
			}
		} catch (ObjectFactoryException e) {			
			e.printStackTrace();
		} catch (SessionTimeoutException e) {
			e.printStackTrace();
		} catch (SessionReestablishedException e) {
			e.printStackTrace();
		}		
		return null;
	}	
	public static Object callAppMethod(IClientContext context, String className, String methodName, Object[] parameters)
	{
		//* @author AyhanI
		//* @version 1.0
		Object o = null;
		try
		{
			if (applicationClasses == null)
				applicationClasses = new Hashtable();
			o = applicationClasses.get(className);
			if (o == null)
			{
				o = context.createInstance(className);
				applicationClasses.put(className, o);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (o != null)
			return MethodInvoker.invokeMethodSafe(methodName, o, parameters);
		return null;
	}
	public static void dumpEventParams(JLbsXUIControlEvent event)
	{
		System.out.println("Dumping event params");
		System.out.println("CtxData " + event.getCtxData());
		System.out.println("Tag " + event.getTag());
		System.out.println("Id " + event.getId());
		System.out.println("Index " + event.getIndex());
		System.out.println("ItemMask " + event.getItemMask());
		System.out.println("StringData " + event.getStringData());
	}
	
	public static boolean persistCBO(JLbsXUIControlEvent event, CustomBusinessObject CBO)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			objFactory.persist(CBO, params);
		}
		catch (Exception e)
		{
			// TODO: handle exception
			return false;
		}
		return true;
	}
	
	public static boolean persistCBO(IApplicationContext context, CustomBusinessObject CBO)
	{
		IObjectFactory objFactory = context.getObjectFactory();
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			objFactory.persist(CBO, params);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean persistCBOs(JLbsXUIControlEvent event, CustomBusinessObjects CBOs)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		FactoryParams params = new FactoryParams();
		
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			for (int i = 0; i < CBOs.size(); i++)
			{
				CustomBusinessObject CBO = (CustomBusinessObject)CBOs.get(i);
				objFactory.persist(CBO, params);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean deleteCBOs(JLbsXUIControlEvent event, CustomBusinessObjects CBOs)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			for (int i = 0; i < CBOs.size(); i++)
			{
				CustomBusinessObject CBO = (CustomBusinessObject)CBOs.get(i);
				objFactory.delete(CBO, params);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean deleteCBO(JLbsXUIControlEvent event, CustomBusinessObject CBO)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
				objFactory.delete(CBO, params);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean UpdateCBO(JLbsXUIControlEvent event, CustomBusinessObject CBO)
	{
		JLbsXUIPane container = event.getContainer();
		IClientContext context = container.getContext();
		IObjectFactory objFactory = context.getObjectFactory();
		FactoryParams params = new FactoryParams();
		params.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
		try
		{
			objFactory.update(CBO, params);
		}
		catch (Exception e)
		{
			// TODO: handle exception
			return false;
		}
		return true;
	}
	public static BusinessObject createBOInstance(IApplicationContext context, String BOName)
	{
		BusinessObject newBO;
		try
		{
			newBO = (BusinessObject) context.createInstance(BOName);
		}
		catch (Exception e)
		{
			newBO = null;
			e.printStackTrace();
		}
		return newBO;
	}
	public static BusinessObject createBOInstance(IServerContext context, String BOName)
	{
		BusinessObject newBO;
		try
		{
			newBO = (BusinessObject) context.createInstance(BOName);
		}
		catch (Exception e)
		{
			newBO = null;
			e.printStackTrace();
		}
		return newBO;
	}
	public static BusinessObject createBOInstance(JLbsXUIControlEvent event, String BOName)
	{
		return createBOInstance(event.getContainer().getContext(), BOName);
	}
	public static BusinessObject createBOInstance(JLbsXUIGridEvent event, String BOName)
	{
		return createBOInstance(event.getContainer().getContext(), BOName);
	}
	public static Object callAppMethod(IClientContext context, String methodName, Object[] parameters)
	{
		Object o = null;
		String className = "com.lbs.unity.UnityCustomizationHelper";
		try
		{
			if (applicationClasses == null)
				applicationClasses = new Hashtable();
			o = applicationClasses.get(className);
			if (o == null)
			{
				o = context.createInstance(className);
				applicationClasses.put(className, o);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (o != null)
			return MethodInvoker.invokeMethodSafe(methodName, o, parameters);
		return null;
	}
	public static JLbsStringListItemEx[] formListArray(String[] paramNames, Object[] items)
	{
		if (items != null && paramNames != null && items.length == paramNames.length)
		{
			JLbsStringListItemEx[] array = new JLbsStringListItemEx[items.length];
			for (int i = 0; i < items.length; i++)
				array[i] = new JLbsStringListItemEx(paramNames[i], i, items[i]);
			return array;
		}
		return null;
	}
	public static boolean AppendGridRowToObjectGrid(JLbsObjectListGrid objGrid)
	{
		boolean res = false;
		if (objGrid != null)
		{
			Class objClass = objGrid.getObjectClass();
			if (objClass != null)
			{
				try
				{
					Object newObject = objClass.newInstance();
					objGrid.getObjects().add(newObject);
					objGrid.rowListChanged();
					res = true;
				}
				catch (Exception exc)
				{
					exc.printStackTrace();
				}
			}
		}
		return res;
	}
	public static boolean InsertGridRowToObjectGrid(int rowNr, JLbsObjectListGrid objGrid)
	{
		boolean res = false;
		if (objGrid != null)
		{
			Class objClass = objGrid.getObjectClass();
			if ((objClass != null) && (rowNr >= 0) && (rowNr < objGrid.getRowCount()))
			{
				try
				{
					Object newObject = objClass.newInstance();
					objGrid.getObjects().add(rowNr, newObject);
					objGrid.rowListChanged();
					res = true;
				}
				catch (Exception exc)
				{
					exc.printStackTrace();
				}
			}
		}
		return res;
	}
	
	public static void showRecordInf(JLbsXUIEventBase event, int created_By, int modified_By, Calendar created_On, Calendar modified_On)
	{		
		UNEORecInfo bo = new UNEORecInfo();
		bo.setCreated_By(created_By);
		bo.setModified_By(modified_By);
		bo.setCreated_On(created_On);
		bo.setModified_On(modified_On);		
		event.getContainer().openChild("UNXFRecInfo.jfm", bo, true, JLbsXUITypes.XUIMODE_DEFAULT);
	}
	
	public static boolean inIntegerList(ArrayList list, int tag)
	{
		if (list != null)
			for (int i = 0; i < list.size(); i++)
			{
				int iVal = ((Integer) list.get(i)).intValue();
				if (iVal == tag)
					return true;
			}
		return false;
	}
	
	public static void showSearchRowOfGrid(JLbsXUIControlEvent event, JLbsQuerySelectionGrid grid)
	{
		if ((event == null) || (grid == null))
		{
			return;
		}
		grid.doShowSearchRow(0);
	}
	
	public static  void checkSubLink(CustomBusinessObject myExt, String LinkName, String LinkCBOName) 
	   {
	               CustomBusinessObjects LinkLines = (CustomBusinessObjects) ProjectUtilEInv.getMemberValue(myExt, LinkName);
	               if (LinkLines == null)
	               {
	                          LinkLines = new CustomBusinessObjects();
	                          ProjectUtilEInv.setMemberValue(myExt, LinkName, LinkLines);
	               }
	               if (LinkLines != null && LinkLines.size() == 0)
	               {
	                          CustomBusinessObject emptyCompLine = ProjectUtilEInv.createNewCBO(LinkCBOName);
	                          LinkLines.add(emptyCompLine);
	               }
	   }
	public static String rpad(String s, int pad, String charac) {
	       if ( s==null ) s = "";
	       StringBuffer sb = new StringBuffer( pad );
	       for (int i = 0; i < (pad-s.length()); i++) {
	         sb = sb.append(charac);
	       }
	       return s + sb.toString();
	    }

	    public static String lpad(String s, int pad, String charac) {
	       if ( s==null ) s = "";
	       StringBuffer sb = new StringBuffer( pad );
	       for (int i = 0; i < (pad-s.length()); i++) {
	         sb = sb.append(charac);
	       }
	       return sb.toString() + s;
	    }
	    public static QueryBusinessObjects executeUODSelectQuerybyServer(IClientContext context, String sqlTxt, QueryParams prms, ArrayList<String> errLst)
		{
			if ((context == null) || (StringUtil.isEmpty(sqlTxt)))
				return null;		
			if (errLst != null)
				errLst.clear();
			RemoteMethodResponse response = null;
			Object[] inParam = new Object[] { null, sqlTxt, prms};
			try
			{
				response = context.callRemoteMethod("CSRV", "executeUODSelectQuery", inParam);
			}
			catch (Exception e)
			{
				if (errLst != null)
					errLst.add(e.getMessage());
				e.printStackTrace();			
				return null;
			}			
			if (response == null)
			{
				if (errLst != null)
					errLst.add("Query response == null");
				return null;
			}
			Object[] results = (Object[]) response.Result;
			if (((Boolean) (results[0])).booleanValue())
			{
				QueryBusinessObjects items = (QueryBusinessObjects) results[1];
				if ((items != null) && (items.size() > 0))
					return items;
			}
			if ((errLst != null) && (errLst.size() <= 0))
				errLst.add("No rows returned");		
			return null;

		}
	    
	    
	    public Object executeUODSelectQueryWithJDBC(IServerContext contextx, String sqlText) 
	       {
	             QueryBusinessObjects result = new QueryBusinessObjects();
	             Object[] params = new Object[3];
	             params[0] = new Boolean(false);//result
	             params[1] = new String("");//errorDesc         
	             params[2] = new QueryBusinessObjects();
	             
	             if ((sqlText == null) || (sqlText.compareTo("") == 0)) {
	                    params[1] = new String("sqlText empty !");//errorDesc
	                    return params;
	             }
	             
	        IServerContext context = LbsServerContext.getSessionlessContext("Direct_Select_Context");
	        IServerQueryFactory serverQueryFactory = context.getServerQueryFactory();
	        QueryBusinessObjects items = new QueryBusinessObjects();
	        try
	        {
	               QueryParams qryPrms = new QueryParams();
	               qryPrms.setDomainless(true);
	               serverQueryFactory.executeSelectQuery(sqlText, qryPrms, items, -1);
	               
	               
	                       if ((items != null)&&(items.size() > 0))
	                       {
	                               params[0] = new Boolean(true);
	                               params[1] = new String("");//errorDesc
	                               params[2] = items;//(QueryBusinessObject) items.elementAt(0);
	                           }else{
	                                  params[0] = new Boolean(false);
	                                  params[1] = new String("sqlText empty !");//errorDesc
	                               params[2] = null;
	                           }
	        }
	        catch (Exception e)
	        {
	                    params[0] = new Boolean(false);
	                    params[1] = e.getLocalizedMessage();
	                 params[2] = null;             
	            e.printStackTrace();
	            //return params;
	        }
	        return params;
	       }

		public static QueryBusinessObjects executeQuerybyServer(IClientContext context, String sqlTxt, ArrayList errLst)
        {
               if ((context == null) || (StringUtil.isEmpty(sqlTxt)))
                      return null;        
               if (errLst != null)
                      errLst.clear();
               RemoteMethodResponse response = null;
               Object[] inParam = new Object[] { null, sqlTxt };
               try
               {
                      response = context.callRemoteMethod("CSRV", "executeUODSelectQueryWithJDBC", inParam);
               }
               catch (Exception e)
               {
                      if (errLst != null)
                             errLst.add(e.getMessage());
                      e.printStackTrace();                    
                      return null;
               }                   
               if (response == null)
               {
                      if (errLst != null)
                             errLst.add("Query response == null");
                      return null;
               }
               Object[] results = (Object[]) response.Result;
               if (((Boolean) (results[0])).booleanValue())
               {
                      QueryBusinessObjects items = (QueryBusinessObjects) results[2];
                      if ((items != null) && (items.size() > 0))
                             return items;
               }
               if ((errLst != null) && (errLst.size() <= 0))
                      errLst.add("No rows returned");         
               return null;
        }

		public static boolean executeUpdateQuerybyServer(IClientContext context, String sqlTxt,ArrayList errLst)
		{
			if ((context == null) || (StringUtil.isEmpty(sqlTxt)))
				return false;		
			RemoteMethodResponse response = null;
			Object[] inParam = new Object[] { null, sqlTxt };
			try
			{
				response = context.callRemoteMethod("CSRV", "executeUpdateQuery", inParam);
				return true;
			}
			catch (Exception e)
			{
				e.printStackTrace();			
				return false;
			}
			
			
		}
		
		public static QueryBusinessObjects executeQuerybyServer(IServerContext context, String sqlTxt, ArrayList errLst)
		{
			if ((context == null) || (StringUtil.isEmpty(sqlTxt)))
				return null;		
			if (errLst != null)
				errLst.clear();
			RemoteMethodResponse response = null;
			Object[] inParam = new Object[] { null, sqlTxt };
			try
			{
				response = context.callRemoteMethod("CSRV", "executeUODSelectQueryWithJDBC", inParam);
			}
			catch (Exception e)
			{
				if (errLst != null)
					errLst.add(e.getMessage());
				e.printStackTrace();			
				return null;
			}			
			if (response == null)
			{
				if (errLst != null)
					errLst.add("Query response == null");
				return null;
			}
			Object[] results = (Object[]) response.Result;
			if (((Boolean) (results[0])).booleanValue())
			{
				QueryBusinessObjects items = (QueryBusinessObjects) results[2];
				if ((items != null) && (items.size() > 0))
					return items;
			}
			if ((errLst != null) && (errLst.size() <= 0))
				errLst.add("No rows returned");		
			return null;
		}
		public static boolean rrrexecuteUpdateQuerybyServer(IServerContext context, String sqlTxt) {
			if ((context == null) || (StringUtil.isEmpty(sqlTxt)))
				return false;		
			RemoteMethodResponse response = null;
			Object[] inParam = new Object[] { null, sqlTxt };
			try
			{
				response = context.callRemoteMethod("CSRV", "executeUpdateQuery", inParam);
			}
			catch (Exception e)
			{
				e.printStackTrace();			
				return false;
			}			
			if (response == null)
			{
				return false;
			}
			Object[] results = (Object[]) response.Result;
			return (((Boolean) (results[0])).booleanValue());
			
		}
		
		public static QueryBusinessObjects UrunleriListele(IClientContext context) {
            QueryBusinessObjects objects = new QueryBusinessObjects();
            try {

                   IQueryFactory queryFactory = context.getQueryFactory();
     

                   QueryParams queryParams = new QueryParams();
                   queryParams.setCustomization(ProjectGlobalsEInv.getM_ProjectGUID());
                   
                                 
                   
                   //TODO
                   queryFactory.first("CQItems", queryParams, objects, -1, true);
                   return objects;

            } catch (Exception e) {
                   e.printStackTrace();
                   return objects;

            }

      }

	
		
		public static String dateStr(Calendar date)
		{
			Date tarih = date.getTime();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			String dateString = formatter.format(tarih);
			return dateString;
			
		}
		
		public static String dateStr2(Calendar date)
		{
			Date tarih = date.getTime();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			String dateString = formatter.format(tarih);
			return dateString;
			
		}
		
		public static String dateToString(Calendar date, String format)
		{
			Date tarih = date.getTime();
			SimpleDateFormat formatter = new SimpleDateFormat(format);
			String dateString = formatter.format(tarih);
			return dateString;
			
		}
		
		
		public static IServerContext /*IserverDataContext*/ createConnection(IServerContext context, String serverName, String dbName, String userName, String password)
		{
			DBConnectionInfo dbConnInfo = new DBConnectionInfo();
			dbConnInfo.setServerName(serverName);
			dbConnInfo.setDatabaseName(dbName);
			dbConnInfo.setUserName(userName);
			dbConnInfo.setPassword(password);
			dbConnInfo.setServerType(DBConnection.DBTYPE_SQLSERVER);
			DBConnectionManager connManager = new DBConnectionManager();
//			IServerDataContext dataContext = null;
			IServerContext dataContext = null;
			DBConnection conn = null;
			try
			{
				conn = DBConnectionManager.connect(dbConnInfo);
			}
			catch (SQLException e1)
			{
				e1.printStackTrace();
			}
			catch (DBException e1)
			{
				e1.printStackTrace();
			}
			catch (ClassNotFoundException e1)
			{
				e1.printStackTrace();
			}
			try
			{
				dataContext = context.getContext(conn);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
			return dataContext;
		}
		
	
}
