package org.tc.cluster.watcher.util;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

public class XPathReader {

  private String   uri;
  private Document xmlDocument;
  private XPath    xPath;

  public XPathReader(String uri) {
    this.uri = uri;
  }

  private void init() throws Exception {
    xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(uri);
    xPath = XPathFactory.newInstance().newXPath();
  }

  public Object read(String expression, QName returnType) throws Exception {
    if (xPath == null) {
      init();
    }
    XPathExpression xPathExpression = xPath.compile(expression);
    return xPathExpression.evaluate(xmlDocument, returnType);
  }

}
