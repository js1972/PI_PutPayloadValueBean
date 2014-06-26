package custom.sap.module;

import java.io.ByteArrayOutputStream;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.tc.logging.Location;


/**
 * PI Adapter Module bean.
 * This module works in tandem with SAP's standard GetPayloadValueBean module.
 * GetPayloadValueBean must be configured into the module chain prior to the 
 * adapter call. This bean must then be configured *after* the adapter call.
 *
 * Typically this will be used in scenario's where the async/sync bridge
 * modules are used. Ensure that this bean is placed *before*  the 
 * ResponseOnewayBean.
 * 
 * The process() method is called by PI. Our bean gets the XML payload,
 * adds an additional field as per the module parameters, then sets the
 * XML back as the messages principle payload.
 * 
 * To see a list of the module context in the developer trace add the
 * following:
 * 
 * location.errorT(inputModuleData.contentToString());
 * 
 */
public class ZPutPayloadValueBean implements SessionBean, Module {
	private static final String GET_PAYLOAD_VALUE_BEAN_PREFIX = "ValueBean.";
	private static final long serialVersionUID = 1L;
	
	private ModuleContext moduleContext = null;
	private Location location = null;
	private String fieldName = "";
	private String getPayloadValueBeanValue = "";
	private String fieldVar = "";
	private MessageKey messageKey = null;
	private AuditAccess audit = null;
	
	
	/**
	 * Entry point - called by PI
	 */
	public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {
		this.moduleContext = moduleContext;
		location = Location.getLocation(this.getClass().getName());
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		Message m = null;
		
		try {
			m = (Message) inputModuleData.getPrincipalData();			
			messageKey = new MessageKey(m.getMessageId(), m.getMessageDirection());
			audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
			
			getModuleContextVars(inputModuleData);
			
			XMLPayload xml = m.getDocument();
			
			DocumentBuilder builder = docFactory.newDocumentBuilder();
			Document document = builder.parse(xml.getInputStream());
			
			Element newElement = document.createElement(fieldName);
			newElement.appendChild(document.createTextNode(getPayloadValueBeanValue));
			document.getDocumentElement().appendChild(newElement);
			
			DOMSource source = new DOMSource(document);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			StreamResult streamResult = new StreamResult(ba);
			transformer.transform(source, streamResult);
			
			xml.setContent(ba.toByteArray());
			inputModuleData.setPrincipalData(m);
			
		} catch (ModuleContextVarsException mcve) {
			throw new ModuleException("Error in Context vars", mcve);
		} catch (Exception e) {
			throw new ModuleException("Error in XML handling - trying to add new element", e);
		}
		
		String successMsg = "*** ZputPayloadValueBean inserted '" 
							+ getPayloadValueBeanValue
							+ "' from GetPayloadValueBean variable '"
							+ fieldVar
							+ "'";
		audit(AuditLogStatus.SUCCESS, successMsg);
		
		
		return inputModuleData;
	}
	
	
	/**
	 * Write to the Module audit log
	 */
	private void audit(AuditLogStatus status, String msg) {
		audit.addAuditLogEntry(messageKey, status, msg);
	}
	
	
	/**
	 * Read the module context variables to determine the fieldname and value to insert
	 * into the message.
	 * Note that the GetPayloadValueBean prefixes its variables with a constant.
	 * 
	 * @param inputModuleData
	 */
	private void getModuleContextVars(ModuleData inputModuleData) {
		fieldName = moduleContext.getContextData("fieldName");
		location.errorT("Context param fieldName: " + fieldName);
		
		fieldVar = moduleContext.getContextData("fieldVar");
		location.errorT("Context param fieldName: " + fieldVar);
		
		getPayloadValueBeanValue = (String) inputModuleData.getSupplementalData(GET_PAYLOAD_VALUE_BEAN_PREFIX + fieldVar);
		location.errorT("Context param value from GetPayloadValueBean: " + getPayloadValueBeanValue);
		
		if (fieldName == null || fieldName.equals("")) {
			throw new ModuleContextVarsException("Must provide fieldName parameter to specify new message element");
		}
		if (fieldVar == null || fieldVar.equals("")) {
			throw new ModuleContextVarsException("Must provide fieldVar parameter to specify which value to use from the GetPayloadValueBean");
		}
		if (getPayloadValueBeanValue == null) {
			getPayloadValueBeanValue = "";
		}
	}
	
	/*
	 * The methods below are required as part of the EJB lifecycle
	 */
	
	public void ejbRemove() { }

	public void ejbActivate() { }

	public void ejbPassivate() { }

	public void setSessionContext(SessionContext context) {	}

	public void ejbCreate() throws CreateException { }
}
