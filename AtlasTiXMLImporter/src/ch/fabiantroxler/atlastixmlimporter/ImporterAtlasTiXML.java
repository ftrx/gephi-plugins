/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.fabiantroxler.atlastixmlimporter;


import java.io.Reader;
import java.util.HashMap;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ImportUtils;
import org.gephi.io.importer.api.Issue;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.importer.api.PropertiesAssociations;
import org.gephi.io.importer.api.PropertiesAssociations.EdgeProperties;
import org.gephi.io.importer.api.PropertiesAssociations.NodeProperties;
import org.gephi.io.importer.api.Report;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.NbBundle;

import ch.fabiantroxler.atlastixmlimporter.DataSource;
import java.util.ArrayList;

/**
 *
 * @author fabiantroxler
 */


public class ImporterAtlasTiXML implements FileImporter, LongTask{
   
   private ArrayList<DataSource> dataSourceList = new ArrayList<DataSource>();
   
   private static final String PRIMDOC = "primDoc";
   private static final String PRIMDOC_LOC = "loc";
   
   private static final String DATASOURCE = "dataSource";
   private static final String DATASOURCE_ID = "id";
   private static final String DATASOURCE_TYPE = "mime"; // what kind of type text/video/pictures
   private static final String DATASOURCE_LOCATION = "loc";
   private static final String DATASOURCE_TIMEFRAME = "tf"; //timeframe - unittype (ms,frames, seconds);
   private static final String DATASOURCE_FPS = "mfps"; // FPS in milliseconds
   
    
   private static final String QUOTATION = "q";
   private static final String QUOTATION_ID = "id";
   private static final String QUOTATION_LABEL = "name";
   private static final String QUOTATION_LOCATION = "loc";
   private static final String QUOTATION_CONTENT = "content";
   private static final String QUOTATION_CONTENT_P = "p";
   
   private static final String EDGE = "edge";
   private static final String EDGE_ID = "id";
   private static final String EDGE_SOURCE = "source";
   private static final String EDGE_TARGET = "target";
   private static final String EDGE_DIRECTED = "directed";
   
   private static final String LINK = "iLink";
   private static final String LINK_REFERENCE = "qRef";
   private static final String LINK_OBJECT = "obj";
   
   private static final String CODE = "code";
   private static final String CODE_MDATE = "mDate";
   private static final String CODE_CDATE = "cDate";
   private static final String CODE_AU = "au";
   private static final String CODE_NAME = "name";
   private static final String CODE_ID = "id";
   private static final String CODE_QCOUNT = "qCount";
   private static final String CODE_CCOUNT = "cCount";
   private static final String CODE_COLOR = "color";
   
   private static final String ATTRIBUTE = "key";
   private static final String ATTRIBUTE_ID = "id";
   private static final String ATTRIBUTE_TITLE = "attr.name";
   private static final String ATTRIBUTE_TYPE = "attr.type";
   private static final String ATTRIBUTE_DEFAULT = "default";
   private static final String ATTRIBUTE_FOR = "for";
   private static final String ATTVALUE = "data";
   private static final String ATTVALUE_FOR = "key"; 
    
    
   private Reader reader;
   private ContainerLoader container;
   private Report report;
   private ProgressTicket progressTicket;
   private boolean cancel = false;
   private PropertiesAssociations properties = new PropertiesAssociations();
   private XMLStreamReader xmlReader;
   
   private int edgeCounter = 0;
   
   private HashMap<String, AtlasTiCode> codeProperties = new HashMap<String, AtlasTiCode>();
   
   
   public void setReader(Reader reader) {
      this.reader = reader;
   }
 
   public boolean execute(ContainerLoader loader) {
      this.container = loader;
      this.report = new Report();
      //Import
      xmlReader = ImportUtils.getXMLReader(reader);
      try {
        importData(xmlReader);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      Progress.finish(progressTicket);
      return !cancel;
   }
 
   public ContainerLoader getContainer() {
      return container;
   }
 
   public Report getReport() {
      return report;
   }
 
   public boolean cancel() {
      cancel = true;
      return true;
   }
 
   public void setProgressTicket(ProgressTicket progressTicket) {
      this.progressTicket = progressTicket;
   }

    private void importData(XMLStreamReader xmlReader) throws Exception {
        
        String primeDoc = "";
        
        while (xmlReader.hasNext()) {
                Integer eventType = xmlReader.next();
                if (eventType.equals(XMLEvent.START_ELEMENT)) {
                    String name = xmlReader.getLocalName();
                    if (PRIMDOC.equalsIgnoreCase(name)) {
                        primeDoc = readPrimDocLocation(xmlReader);
                    } else if (QUOTATION.equalsIgnoreCase(name)) {
                        readQuotation(xmlReader, null, primeDoc);
                    } else if (CODE.equalsIgnoreCase(name)) {
                        readCode(xmlReader);
                    } else if (LINK.equalsIgnoreCase(name)) {
                        readLink(xmlReader);
                    } else if (DATASOURCE.equalsIgnoreCase(name)){
                        readDatasource(xmlReader, null);
                    }
                } else if (eventType.equals(XMLStreamReader.END_ELEMENT)) {
                    String name = xmlReader.getLocalName();
                    if (QUOTATION.equalsIgnoreCase(name)) {
                    }
                }
            }
        xmlReader.close();
        
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String readPrimDocLocation(XMLStreamReader reader) throws Exception {
       String loc = "";
       for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (PRIMDOC_LOC.equalsIgnoreCase(attName)) {
                loc = reader.getAttributeValue(i);
            }
        }

       return loc;
    }

    private void readQuotation(XMLStreamReader reader,NodeDraft parent,String primDoc) throws Exception {
        String id = "";
        String label = "";
        //String type = "";
        String[] loc = null;
        String startDate = "";
        String endDate = "";
        String pid = "";
        boolean startOpen = false;
        boolean endOpen = false;

        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (QUOTATION_ID.equalsIgnoreCase(attName)) {
                id = reader.getAttributeValue(i);
            } else if (QUOTATION_LABEL.equalsIgnoreCase(attName)) {
                label = reader.getAttributeValue(i);
            } else if (QUOTATION_LOCATION.equalsIgnoreCase(attName)) {
                String location = reader.getAttributeValue(i);
                String[] str;
                str = location.split(",");
                
                loc = new String[str.length];
                for (int j = 0; j< str.length; j++){
                    loc[j] = str[j].split("@")[0];
                    loc[j] = loc[j].split("!")[0];
                    loc[j] = loc[j].trim();
                }
            }
        }

        if (id.isEmpty()) {
            report.logIssue(new Issue("ID is empty", Issue.Level.SEVERE));
            return;
        }

        NodeDraft node = null;
        if (container.nodeExists(id)) {
            node = container.getNode(id);
        } else {
            node = container.factory().newNodeDraft();
        }
        node.setId(id);
        node.setLabel(label);
        
        if(loc.length > 0){
            node = addStringAtributeToNodeTable(container,node,"locationStart","locationStart",loc[0], "int");
            node = addStringAtributeToNodeTable(container,node,"locationEnd","locationEnd",loc[1], "int");
        }
        node = addStringAtributeToNodeTable(container,node,"primDoc","primDoc",primDoc, "string");

        //Parent
        if (parent != null) {
            node.setParent(parent);
        } else if (!pid.isEmpty()) {
            NodeDraft parentNode = container.getNode(pid);
            if (parentNode == null) {
                report.logIssue(new Issue("parentNode == NULL", Issue.Level.SEVERE));
            } else {
                node.setParent(parentNode);
            }
        }

        if (!container.nodeExists(id)) {
            container.addNode(node);
        }

        boolean end = false;
        boolean slices = false;
        while (reader.hasNext() && !end) {
            int type = reader.next();

            switch (type) {
                case XMLStreamReader.START_ELEMENT:
                    String name = xmlReader.getLocalName();
                    if (QUOTATION_CONTENT.equalsIgnoreCase(name)) {
                        readQuotationContent(reader, node);
                    } 
                    break;

                case XMLStreamReader.END_ELEMENT:
                    if (QUOTATION.equalsIgnoreCase(xmlReader.getLocalName())) {
                        end = true;
                    }
                    break;
            }
        }


        //Dynamic
        if (!slices && (!startDate.isEmpty() || !endDate.isEmpty())) {
            try {
                node.addTimeInterval(startDate, endDate, startOpen, endOpen);
            } catch (IllegalArgumentException e) {
                report.logIssue(new Issue("could not add TimeIntervall", Issue.Level.SEVERE));
            }
        }
    }

    private void readQuotationContent(XMLStreamReader reader, NodeDraft node) throws Exception {

        boolean end = false;
        boolean defaultFlag = false;
        
        String id = "";
        String title = "";
        String type = "";
        String defaultStr = "";
        String forStr = "";
        
        while (reader.hasNext() && !end) {
            int xmltype = reader.next();

            switch (xmltype) {
                case XMLStreamReader.START_ELEMENT:
                    if (QUOTATION_CONTENT_P.equalsIgnoreCase(xmlReader.getLocalName())) {
                        defaultFlag = true;
                    }
                    break;
                case XMLStreamReader.CHARACTERS:
                    if (defaultFlag && !xmlReader.isWhiteSpace()) {
                        defaultStr = xmlReader.getText();
                        title = "content text";
                        type = "string";
                        forStr = "content";
                        id = "content";
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (QUOTATION_CONTENT_P.equalsIgnoreCase(xmlReader.getLocalName())) {
                        end = true;
                    }
                    break;
            }
        }
        //Type
        AttributeType attributeType = AttributeType.STRING;
        if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("bool")) {
            attributeType = AttributeType.BOOLEAN;
        } else if (type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("int")) {
            attributeType = AttributeType.INT;
        } else if (type.equalsIgnoreCase("long")) {
            attributeType = AttributeType.LONG;
        } else if (type.equalsIgnoreCase("float")) {
            attributeType = AttributeType.FLOAT;
        } else if (type.equalsIgnoreCase("double")) {
            attributeType = AttributeType.DOUBLE;
        } else if (type.equalsIgnoreCase("string")) {
            attributeType = AttributeType.STRING;
        } else if (type.equalsIgnoreCase("bigdecimal")) {
            attributeType = AttributeType.BIGDECIMAL;
        } else if (type.equalsIgnoreCase("biginteger")) {
            attributeType = AttributeType.BIGINTEGER;
        } else if (type.equalsIgnoreCase("byte")) {
            attributeType = AttributeType.BYTE;
        } else if (type.equalsIgnoreCase("char")) {
            attributeType = AttributeType.CHAR;
        } else if (type.equalsIgnoreCase("short")) {
            attributeType = AttributeType.SHORT;
        } else if (type.equalsIgnoreCase("listboolean")) {
            attributeType = AttributeType.LIST_BOOLEAN;
        } else if (type.equalsIgnoreCase("listint")) {
            attributeType = AttributeType.LIST_INTEGER;
        } else if (type.equalsIgnoreCase("listlong")) {
            attributeType = AttributeType.LIST_LONG;
        } else if (type.equalsIgnoreCase("listfloat")) {
            attributeType = AttributeType.LIST_FLOAT;
        } else if (type.equalsIgnoreCase("listdouble")) {
            attributeType = AttributeType.LIST_DOUBLE;
        } else if (type.equalsIgnoreCase("liststring")) {
            attributeType = AttributeType.LIST_STRING;
        } else if (type.equalsIgnoreCase("listbigdecimal")) {
            attributeType = AttributeType.LIST_BIGDECIMAL;
        } else if (type.equalsIgnoreCase("listbiginteger")) {
            attributeType = AttributeType.LIST_BIGINTEGER;
        } else if (type.equalsIgnoreCase("listbyte")) {
            attributeType = AttributeType.LIST_BYTE;
        } else if (type.equalsIgnoreCase("listchar")) {
            attributeType = AttributeType.LIST_CHARACTER;
        } else if (type.equalsIgnoreCase("listshort")) {
            attributeType = AttributeType.LIST_SHORT;
        } else {
            report.logIssue(new Issue("no type", Issue.Level.SEVERE));
            return;
        }

        //Default Object
        Object defaultValue = null;
        if (!defaultStr.isEmpty()) {
            try {
                defaultValue = attributeType.parse(defaultStr);
                //report.log(NbBundle.getMessage(ImporterAtlasTiXML.class, "importerGraphML_log_default", defaultStr, title));
            } catch (Exception e) {
                report.logIssue(new Issue("could not parse attributeType", Issue.Level.SEVERE));
            }
        }

        //Add to model
        if ("content".equalsIgnoreCase(forStr)) {
            if (container.getAttributeModel().getNodeTable().hasColumn(id) || container.getAttributeModel().getNodeTable().hasColumn(title)) {
                //report.log(NbBundle.getMessage(ImporterAtlasTiXML.class, "importerGraphML_error_attributecolumn_exist", id));
                AttributeColumn column = container.getAttributeModel().getNodeTable().getColumn(forStr);
                node.addAttributeValue(column, defaultStr);
                return;
            }
            container.getAttributeModel().getNodeTable().addColumn(id, title, attributeType, AttributeOrigin.DATA, defaultValue);
            //report.log(NbBundle.getMessage(ImporterAtlasTiXML.class, "importerGraphML_log_nodeattribute", title, attributeType.getTypeString()));
        }
    }
    
    private void readCode(XMLStreamReader reader) throws Exception {
        
        String mDate = "";
        String cDate = "";
        String au = "";
        String name= "";
        String id = "";
        String color= "";
        AtlasTiCode atlasTiCode = new AtlasTiCode();
        
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (CODE_MDATE.equalsIgnoreCase(attName)) {
                mDate = reader.getAttributeValue(i);
            } else if (CODE_CDATE.equalsIgnoreCase(attName)) {
                cDate = reader.getAttributeValue(i);
            } else if (CODE_AU.equalsIgnoreCase(attName)) {
                au = reader.getAttributeValue(i);
            } else if (CODE_NAME.equalsIgnoreCase(attName)) {
                name = reader.getAttributeValue(i);
            } else if (CODE_ID.equalsIgnoreCase(attName)) {
                id = reader.getAttributeValue(i);
            } else if (CODE_COLOR.equalsIgnoreCase(attName)) {
                color = reader.getAttributeValue(i);
            }
        }
        
        atlasTiCode.mDate = mDate;
        atlasTiCode.cDate = cDate;
        atlasTiCode.au = au;
        atlasTiCode.name = name;
        atlasTiCode.id = id;
        atlasTiCode.color = color;
        
        codeProperties.put(id, atlasTiCode);
        
        NodeDraft node = container.factory().newNodeDraft();
        node.setId(id);
        node.setLabel(name);
        
        if (!container.nodeExists(id)) {
            container.addNode(node);
        }
        
         
        
    }
    
    private void readLink(XMLStreamReader reader) throws Exception {
        
        String id = "";
        String name= "";
        String obj= "";
        String qRef = "";
        
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (LINK_REFERENCE.equalsIgnoreCase(attName)) {
                qRef = reader.getAttributeValue(i);
            } else if (LINK_OBJECT.equalsIgnoreCase(attName)) {
                obj = reader.getAttributeValue(i);
            }
        }
        AtlasTiCode atlasTiCode = codeProperties.get(obj);
        
        if(atlasTiCode != null) {
            EdgeDraft edge = container.factory().newEdgeDraft();
            
            NodeDraft nodeSource = container.getNode(qRef);
            NodeDraft nodeTarget = container.getNode(obj);
            
            
            
            edge.setSource(nodeSource);
            edge.setTarget(nodeTarget);
            
            edge.setLabel(atlasTiCode.name);
            edge.setId(Integer.toString(edgeCounter));
            //edge.setColor(atlasTiCode.color);
            edgeCounter++;
            container.addEdge(edge);
        }
        else {
            report.logIssue(new Issue("no code with this object-id", Issue.Level.SEVERE));
        }
        
        
        
    }

    private void readDatasource(XMLStreamReader reader,NodeDraft parent) throws Exception {
        String startDate = "";
        String endDate = "";
        String pid = "";
        boolean startOpen = false;
        boolean endOpen = false;
        
        DataSource dataSource = new DataSource();

        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (DATASOURCE_ID.equalsIgnoreCase(attName)) {
                dataSource.id = reader.getAttributeValue(i);
            } else if (DATASOURCE_LOCATION.equalsIgnoreCase(attName)) {
                dataSource.loc = reader.getAttributeValue(i);
            } else if (DATASOURCE_TYPE.equalsIgnoreCase(attName)) {
                dataSource.mime = reader.getAttributeValue(i);
            } else if (DATASOURCE_TIMEFRAME.equalsIgnoreCase(attName)) {
                dataSource.tf = reader.getAttributeValue(i);
            } else if (DATASOURCE_FPS.equalsIgnoreCase(attName)) {
                dataSource.mfps = Integer.parseInt(reader.getAttributeValue(i));
            }
        }

        if (dataSource.id.isEmpty()) {
            report.logIssue(new Issue("ID is empty", Issue.Level.SEVERE));
            return;
        }
        
        dataSourceList.add(dataSource);
        
    }
    
    private NodeDraft addStringAtributeToNodeTable(ContainerLoader container,NodeDraft node, String column_id, String column_title, String value, String type){
        
        AttributeType attributeType = AttributeType.STRING;
        if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("bool")) {
            attributeType = AttributeType.BOOLEAN;
        } else if (type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("int")) {
            attributeType = AttributeType.INT;
        } else if (type.equalsIgnoreCase("long")) {
            attributeType = AttributeType.LONG;
        } else if (type.equalsIgnoreCase("float")) {
            attributeType = AttributeType.FLOAT;
        } else if (type.equalsIgnoreCase("double")) {
            attributeType = AttributeType.DOUBLE;
        } else if (type.equalsIgnoreCase("string")) {
            attributeType = AttributeType.STRING;
        } else if (type.equalsIgnoreCase("bigdecimal")) {
            attributeType = AttributeType.BIGDECIMAL;
        } else if (type.equalsIgnoreCase("biginteger")) {
            attributeType = AttributeType.BIGINTEGER;
        } else if (type.equalsIgnoreCase("byte")) {
            attributeType = AttributeType.BYTE;
        } else if (type.equalsIgnoreCase("char")) {
            attributeType = AttributeType.CHAR;
        } else if (type.equalsIgnoreCase("short")) {
            attributeType = AttributeType.SHORT;
        } else if (type.equalsIgnoreCase("listboolean")) {
            attributeType = AttributeType.LIST_BOOLEAN;
        } else if (type.equalsIgnoreCase("listint")) {
            attributeType = AttributeType.LIST_INTEGER;
        } else if (type.equalsIgnoreCase("listlong")) {
            attributeType = AttributeType.LIST_LONG;
        } else if (type.equalsIgnoreCase("listfloat")) {
            attributeType = AttributeType.LIST_FLOAT;
        } else if (type.equalsIgnoreCase("listdouble")) {
            attributeType = AttributeType.LIST_DOUBLE;
        } else if (type.equalsIgnoreCase("liststring")) {
            attributeType = AttributeType.LIST_STRING;
        } else if (type.equalsIgnoreCase("listbigdecimal")) {
            attributeType = AttributeType.LIST_BIGDECIMAL;
        } else if (type.equalsIgnoreCase("listbiginteger")) {
            attributeType = AttributeType.LIST_BIGINTEGER;
        } else if (type.equalsIgnoreCase("listbyte")) {
            attributeType = AttributeType.LIST_BYTE;
        } else if (type.equalsIgnoreCase("listchar")) {
            attributeType = AttributeType.LIST_CHARACTER;
        } else if (type.equalsIgnoreCase("listshort")) {
            attributeType = AttributeType.LIST_SHORT;
        } else {
            report.logIssue(new Issue("no type", Issue.Level.SEVERE));
        }
        
        
        if (container.getAttributeModel().getNodeTable().hasColumn(column_id) || container.getAttributeModel().getNodeTable().hasColumn(column_title)) {
                //report.log(NbBundle.getMessage(ImporterAtlasTiXML.class, "importerGraphML_error_attributecolumn_exist", id));
                AttributeColumn column = container.getAttributeModel().getNodeTable().getColumn(column_id);
                node.addAttributeValue(column, value);
                return node;
            }
        container.getAttributeModel().getNodeTable().addColumn(column_id, column_title, attributeType, AttributeOrigin.DATA, "");
        AttributeColumn column = container.getAttributeModel().getNodeTable().getColumn(column_id);
        node.addAttributeValue(column, value);
        return node;
    }
}
