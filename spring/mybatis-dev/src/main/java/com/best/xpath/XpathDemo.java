package com.best.xpath;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * @author dngzs
 * @date 2019-08-23 15:15
 */
public class XpathDemo {

    public static void main(String[] args) throws Exception{
        Document document = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        try {
            InputStream inputStream = XpathDemo.class.getResource("stu.xml").openStream();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            document = documentBuilder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        Node root = (Node)xPath.evaluate("/students", document, XPathConstants.NODE);
        System.out.println(root);
        if(root.hasChildNodes()){
            NodeList childNodes = root.getChildNodes();
            for (int i = 0;i<childNodes.getLength();i++){
                Node item = childNodes.item(i);
                System.out.println(item.getTextContent());
            }
        }
    }
}
