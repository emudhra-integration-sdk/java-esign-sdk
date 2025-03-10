package esign.text.xmp.impl;

import esign.text.xmp.XMPConst;
import esign.text.xmp.XMPError;
import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.XMPSchemaRegistry;
import esign.text.xmp.options.PropertyOptions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ParseRDF
        implements XMPError, XMPConst {

    public static final int RDFTERM_OTHER = 0;
    public static final int RDFTERM_RDF = 1;
    public static final int RDFTERM_ID = 2;
    public static final int RDFTERM_ABOUT = 3;
    public static final int RDFTERM_PARSE_TYPE = 4;
    public static final int RDFTERM_RESOURCE = 5;
    public static final int RDFTERM_NODE_ID = 6;
    public static final int RDFTERM_DATATYPE = 7;
    public static final int RDFTERM_DESCRIPTION = 8;
    public static final int RDFTERM_LI = 9;
    public static final int RDFTERM_ABOUT_EACH = 10;
    public static final int RDFTERM_ABOUT_EACH_PREFIX = 11;
    public static final int RDFTERM_BAG_ID = 12;
    public static final int RDFTERM_FIRST_CORE = 1;
    public static final int RDFTERM_LAST_CORE = 7;
    public static final int RDFTERM_FIRST_SYNTAX = 1;
    public static final int RDFTERM_LAST_SYNTAX = 9;
    public static final int RDFTERM_FIRST_OLD = 10;
    public static final int RDFTERM_LAST_OLD = 12;
    public static final String DEFAULT_PREFIX = "_dflt";

    static XMPMetaImpl parse(Node xmlRoot) throws XMPException {
        XMPMetaImpl xmp = new XMPMetaImpl();
        rdf_RDF(xmp, xmlRoot);
        return xmp;
    }

    static void rdf_RDF(XMPMetaImpl xmp, Node rdfRdfNode) throws XMPException {
        if (rdfRdfNode.hasAttributes()) {

            rdf_NodeElementList(xmp, xmp.getRoot(), rdfRdfNode);
        } else {

            throw new XMPException("Invalid attributes of rdf:RDF element", 202);
        }
    }

    private static void rdf_NodeElementList(XMPMetaImpl xmp, XMPNode xmpParent, Node rdfRdfNode) throws XMPException {
        for (int i = 0; i < rdfRdfNode.getChildNodes().getLength(); i++) {

            Node child = rdfRdfNode.getChildNodes().item(i);

            if (!isWhitespaceNode(child)) {
                rdf_NodeElement(xmp, xmpParent, child, true);
            }
        }
    }

    private static void rdf_NodeElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        int nodeTerm = getRDFTermKind(xmlNode);
        if (nodeTerm != 8 && nodeTerm != 0) {
            throw new XMPException("Node element must be rdf:Description or typed node", 202);
        }

        if (isTopLevel && nodeTerm == 0) {
            throw new XMPException("Top level typed node not allowed", 203);
        }

        rdf_NodeElementAttrs(xmp, xmpParent, xmlNode, isTopLevel);
        rdf_PropertyElementList(xmp, xmpParent, xmlNode, isTopLevel);
    }

    private static void rdf_NodeElementAttrs(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        int exclusiveAttrs = 0;

        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {

            Node attribute = xmlNode.getAttributes().item(i);

            if (!"xmlns".equals(attribute.getPrefix()) && (attribute
                    .getPrefix() != null || !"xmlns".equals(attribute.getNodeName()))) {

                int attrTerm = getRDFTermKind(attribute);

                switch (attrTerm) {

                    case 2:
                    case 3:
                    case 6:
                        if (exclusiveAttrs > 0) {
                            throw new XMPException("Mutally exclusive about, ID, nodeID attributes", 202);
                        }

                        exclusiveAttrs++;

                        if (isTopLevel && attrTerm == 3) {

                            if (xmpParent.getName() != null && xmpParent.getName().length() > 0) {

                                if (!xmpParent.getName().equals(attribute.getNodeValue())) {
                                    throw new XMPException("Mismatched top level rdf:about values", 203);
                                }

                                break;
                            }

                            xmpParent.setName(attribute.getNodeValue());
                        }
                        break;

                    case 0:
                        addChildNode(xmp, xmpParent, attribute, attribute.getNodeValue(), isTopLevel);
                        break;

                    default:
                        throw new XMPException("Invalid nodeElement attribute", 202);
                }
            }
        }
    }

    private static void rdf_PropertyElementList(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlParent, boolean isTopLevel) throws XMPException {
        for (int i = 0; i < xmlParent.getChildNodes().getLength(); i++) {

            Node currChild = xmlParent.getChildNodes().item(i);
            if (!isWhitespaceNode(currChild)) {

                if (currChild.getNodeType() != 1) {
                    throw new XMPException("Expected property element node not found", 202);
                }

                rdf_PropertyElement(xmp, xmpParent, currChild, isTopLevel);
            }
        }
    }

    private static void rdf_PropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        int nodeTerm = getRDFTermKind(xmlNode);
        if (!isPropertyElementName(nodeTerm)) {
            throw new XMPException("Invalid property element name", 202);
        }

        NamedNodeMap attributes = xmlNode.getAttributes();
        List<String> nsAttrs = null;
        int i;
        for (i = 0; i < attributes.getLength(); i++) {

            Node attribute = attributes.item(i);
            if ("xmlns".equals(attribute.getPrefix()) || (attribute
                    .getPrefix() == null && "xmlns".equals(attribute.getNodeName()))) {

                if (nsAttrs == null) {
                    nsAttrs = new ArrayList();
                }
                nsAttrs.add(attribute.getNodeName());
            }
        }
        if (nsAttrs != null) {
            for (Iterator<String> it = nsAttrs.iterator(); it.hasNext();) {

                String ns = it.next();
                attributes.removeNamedItem(ns);
            }
        }

        if (attributes.getLength() > 3) {

            rdf_EmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);

        } else {

            for (i = 0; i < attributes.getLength(); i++) {

                Node attribute = attributes.item(i);
                String attrLocal = attribute.getLocalName();
                String attrNS = attribute.getNamespaceURI();
                String attrValue = attribute.getNodeValue();
                if (!"xml:lang".equals(attribute.getNodeName()) || ("ID"
                        .equals(attrLocal) && "http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(attrNS))) {

                    if ("datatype".equals(attrLocal) && "http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(attrNS)) {

                        rdf_LiteralPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                    } else if (!"parseType".equals(attrLocal) || !"http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(attrNS)) {

                        rdf_EmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                    } else if ("Literal".equals(attrValue)) {

                        rdf_ParseTypeLiteralPropertyElement();
                    } else if ("Resource".equals(attrValue)) {

                        rdf_ParseTypeResourcePropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                    } else if ("Collection".equals(attrValue)) {

                        rdf_ParseTypeCollectionPropertyElement();
                    } else {

                        rdf_ParseTypeOtherPropertyElement();
                    }

                    return;
                }
            }

            if (xmlNode.hasChildNodes()) {

                for (i = 0; i < xmlNode.getChildNodes().getLength(); i++) {

                    Node currChild = xmlNode.getChildNodes().item(i);
                    if (currChild.getNodeType() != 3) {

                        rdf_ResourcePropertyElement(xmp, xmpParent, xmlNode, isTopLevel);

                        return;
                    }
                }
                rdf_LiteralPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
            } else {

                rdf_EmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
            }
        }
    }

    private static void rdf_ResourcePropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        if (isTopLevel && "iX:changes".equals(xmlNode.getNodeName())) {
            return;
        }

        XMPNode newCompound = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel);

        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {

            Node attribute = xmlNode.getAttributes().item(i);
            if (!"xmlns".equals(attribute.getPrefix()) && (attribute
                    .getPrefix() != null || !"xmlns".equals(attribute.getNodeName()))) {

                String attrLocal = attribute.getLocalName();
                String attrNS = attribute.getNamespaceURI();
                if ("xml:lang".equals(attribute.getNodeName())) {

                    addQualifierNode(newCompound, "xml:lang", attribute.getNodeValue());
                } else if (!"ID".equals(attrLocal) || !"http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(attrNS)) {

                    throw new XMPException("Invalid attribute for resource property element", 202);
                }
            }
        }

        Node currChild = null;
        boolean found = false;

        for (int j = 0; j < xmlNode.getChildNodes().getLength(); j++) {

            currChild = xmlNode.getChildNodes().item(j);
            if (!isWhitespaceNode(currChild)) {
                if (currChild.getNodeType() == 1 && !found) {

                    boolean isRDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(currChild.getNamespaceURI());
                    String childLocal = currChild.getLocalName();

                    if (isRDF && "Bag".equals(childLocal)) {

                        newCompound.getOptions().setArray(true);
                    } else if (isRDF && "Seq".equals(childLocal)) {

                        newCompound.getOptions().setArray(true).setArrayOrdered(true);
                    } else if (isRDF && "Alt".equals(childLocal)) {

                        newCompound.getOptions().setArray(true).setArrayOrdered(true)
                                .setArrayAlternate(true);
                    } else {

                        newCompound.getOptions().setStruct(true);
                        if (!isRDF && !"Description".equals(childLocal)) {

                            String typeName = currChild.getNamespaceURI();
                            if (typeName == null) {
                                throw new XMPException("All XML elements must be in a namespace", 203);
                            }

                            typeName = typeName + ':' + childLocal;
                            addQualifierNode(newCompound, "rdf:type", typeName);
                        }
                    }

                    rdf_NodeElement(xmp, newCompound, currChild, false);

                    if (newCompound.getHasValueChild()) {

                        fixupQualifiedNode(newCompound);
                    } else if (newCompound.getOptions().isArrayAlternate()) {

                        XMPNodeUtils.detectAltText(newCompound);
                    }

                    found = true;
                } else {
                    if (found) {

                        throw new XMPException("Invalid child of resource property element", 202);
                    }

                    throw new XMPException("Children of resource property element must be XML elements", 202);
                }
            }
        }

        if (!found) {

            throw new XMPException("Missing child of resource property element", 202);
        }
    }

    private static void rdf_LiteralPropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        XMPNode newChild = addChildNode(xmp, xmpParent, xmlNode, null, isTopLevel);

        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {

            Node attribute = xmlNode.getAttributes().item(i);
            if (!"xmlns".equals(attribute.getPrefix()) && (attribute
                    .getPrefix() != null || !"xmlns".equals(attribute.getNodeName()))) {

                String attrNS = attribute.getNamespaceURI();
                String attrLocal = attribute.getLocalName();
                if ("xml:lang".equals(attribute.getNodeName())) {

                    addQualifierNode(newChild, "xml:lang", attribute.getNodeValue());
                } else if (!"http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(attrNS) || (!"ID".equals(attrLocal) && !"datatype".equals(attrLocal))) {

                    throw new XMPException("Invalid attribute for literal property element", 202);
                }
            }
        }
        String textValue = "";
        for (int j = 0; j < xmlNode.getChildNodes().getLength(); j++) {

            Node child = xmlNode.getChildNodes().item(j);
            if (child.getNodeType() == 3) {

                textValue = textValue + child.getNodeValue();
            } else {

                throw new XMPException("Invalid child of literal property element", 202);
            }
        }
        newChild.setValue(textValue);
    }

    private static void rdf_ParseTypeLiteralPropertyElement() throws XMPException {
        throw new XMPException("ParseTypeLiteral property element not allowed", 203);
    }

    private static void rdf_ParseTypeResourcePropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        XMPNode newStruct = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel);

        newStruct.getOptions().setStruct(true);

        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {

            Node attribute = xmlNode.getAttributes().item(i);
            if (!"xmlns".equals(attribute.getPrefix()) && (attribute
                    .getPrefix() != null || !"xmlns".equals(attribute.getNodeName()))) {

                String attrLocal = attribute.getLocalName();
                String attrNS = attribute.getNamespaceURI();
                if ("xml:lang".equals(attribute.getNodeName())) {

                    addQualifierNode(newStruct, "xml:lang", attribute.getNodeValue());
                } else if (!"http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(attrNS) || (!"ID".equals(attrLocal) && !"parseType".equals(attrLocal))) {

                    throw new XMPException("Invalid attribute for ParseTypeResource property element", 202);
                }
            }
        }

        rdf_PropertyElementList(xmp, newStruct, xmlNode, false);

        if (newStruct.getHasValueChild()) {
            fixupQualifiedNode(newStruct);
        }
    }

    private static void rdf_ParseTypeCollectionPropertyElement() throws XMPException {
        throw new XMPException("ParseTypeCollection property element not allowed", 203);
    }

    private static void rdf_ParseTypeOtherPropertyElement() throws XMPException {
        throw new XMPException("ParseTypeOther property element not allowed", 203);
    }

    private static void rdf_EmptyPropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        boolean hasPropertyAttrs = false;
        boolean hasResourceAttr = false;
        boolean hasNodeIDAttr = false;
        boolean hasValueAttr = false;

        Node valueNode = null;

        if (xmlNode.hasChildNodes()) {
            throw new XMPException("Nested content not allowed with rdf:resource or property attributes", 202);
        }

        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {

            Node attribute = xmlNode.getAttributes().item(i);
            if (!"xmlns".equals(attribute.getPrefix()) && (attribute
                    .getPrefix() != null || !"xmlns".equals(attribute.getNodeName()))) {

                int attrTerm = getRDFTermKind(attribute);

                switch (attrTerm) {
                    case 2:
                        break;

                    case 5:
                        if (hasNodeIDAttr) {
                            throw new XMPException("Empty property element can't have both rdf:resource and rdf:nodeID", 202);
                        }

                        if (hasValueAttr) {
                            throw new XMPException("Empty property element can't have both rdf:value and rdf:resource", 203);
                        }

                        hasResourceAttr = true;
                        if (!hasValueAttr) {
                            valueNode = attribute;
                        }
                        break;

                    case 6:
                        if (hasResourceAttr) {
                            throw new XMPException("Empty property element can't have both rdf:resource and rdf:nodeID", 202);
                        }

                        hasNodeIDAttr = true;
                        break;

                    case 0:
                        if ("value".equals(attribute.getLocalName()) && "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                .equals(attribute.getNamespaceURI())) {

                            if (hasResourceAttr) {
                                throw new XMPException("Empty property element can't have both rdf:value and rdf:resource", 203);
                            }

                            hasValueAttr = true;
                            valueNode = attribute;
                            break;
                        }
                        if (!"xml:lang".equals(attribute.getNodeName())) {
                            hasPropertyAttrs = true;
                        }
                        break;

                    default:
                        throw new XMPException("Unrecognized attribute of empty property element", 202);
                }

            }
        }
        XMPNode childNode = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel);
        boolean childIsStruct = false;

        if (hasValueAttr || hasResourceAttr) {

            childNode.setValue((valueNode != null) ? valueNode.getNodeValue() : "");
            if (!hasValueAttr) {

                childNode.getOptions().setURI(true);
            }
        } else if (hasPropertyAttrs) {

            childNode.getOptions().setStruct(true);
            childIsStruct = true;
        }

        for (int j = 0; j < xmlNode.getAttributes().getLength(); j++) {

            Node attribute = xmlNode.getAttributes().item(j);
            if (attribute != valueNode
                    && !"xmlns".equals(attribute.getPrefix()) && (attribute
                    .getPrefix() != null || !"xmlns".equals(attribute.getNodeName()))) {

                int attrTerm = getRDFTermKind(attribute);

                switch (attrTerm) {
                    case 2:
                    case 6:
                        break;

                    case 5:
                        addQualifierNode(childNode, "rdf:resource", attribute.getNodeValue());
                        break;

                    case 0:
                        if (!childIsStruct) {

                            addQualifierNode(childNode, attribute
                                    .getNodeName(), attribute.getNodeValue());
                            break;
                        }
                        if ("xml:lang".equals(attribute.getNodeName())) {

                            addQualifierNode(childNode, "xml:lang", attribute.getNodeValue());

                            break;
                        }
                        addChildNode(xmp, childNode, attribute, attribute.getNodeValue(), false);
                        break;

                    default:
                        throw new XMPException("Unrecognized attribute of empty property element", 202);
                }
            }
        }
    }

    private static XMPNode addChildNode(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, String value, boolean isTopLevel) throws XMPException {
        String childName;
        XMPSchemaRegistry registry = XMPMetaFactory.getSchemaRegistry();
        String namespace = xmlNode.getNamespaceURI();

        if (namespace != null) {

            if ("http://purl.org/dc/1.1/".equals(namespace)) {

                namespace = "http://purl.org/dc/elements/1.1/";
            }

            String prefix = registry.getNamespacePrefix(namespace);
            if (prefix == null) {

                prefix = (xmlNode.getPrefix() != null) ? xmlNode.getPrefix() : "_dflt";
                prefix = registry.registerNamespace(namespace, prefix);
            }
            childName = prefix + xmlNode.getLocalName();
        } else {

            throw new XMPException("XML namespace required for all elements and attributes", 202);
        }

        PropertyOptions childOptions = new PropertyOptions();
        boolean isAlias = false;
        if (isTopLevel) {

            XMPNode schemaNode = XMPNodeUtils.findSchemaNode(xmp.getRoot(), namespace, "_dflt", true);

            schemaNode.setImplicit(false);

            xmpParent = schemaNode;

            if (registry.findAlias(childName) != null) {

                isAlias = true;
                xmp.getRoot().setHasAliases(true);
                schemaNode.setHasAliases(true);
            }
        }

        boolean isArrayItem = "rdf:li".equals(childName);
        boolean isValueNode = "rdf:value".equals(childName);

        XMPNode newChild = new XMPNode(childName, value, childOptions);

        newChild.setAlias(isAlias);

        if (!isValueNode) {

            xmpParent.addChild(newChild);
        } else {

            xmpParent.addChild(1, newChild);
        }

        if (isValueNode) {

            if (isTopLevel || !xmpParent.getOptions().isStruct()) {
                throw new XMPException("Misplaced rdf:value element", 202);
            }
            xmpParent.setHasValueChild(true);
        }

        if (isArrayItem) {

            if (!xmpParent.getOptions().isArray()) {
                throw new XMPException("Misplaced rdf:li element", 202);
            }
            newChild.setName("[]");
        }

        return newChild;
    }

    private static XMPNode addQualifierNode(XMPNode xmpParent, String name, String value) throws XMPException {
        boolean isLang = "xml:lang".equals(name);

        XMPNode newQual = null;

        newQual = new XMPNode(name, isLang ? Utils.normalizeLangValue(value) : value, null);
        xmpParent.addQualifier(newQual);

        return newQual;
    }

    private static void fixupQualifiedNode(XMPNode xmpParent) throws XMPException {
        assert xmpParent.getOptions().isStruct() && xmpParent.hasChildren();

        XMPNode valueNode = xmpParent.getChild(1);
        assert "rdf:value".equals(valueNode.getName());

        if (valueNode.getOptions().getHasLanguage()) {

            if (xmpParent.getOptions().getHasLanguage()) {
                throw new XMPException("Redundant xml:lang for rdf:value element", 203);
            }

            XMPNode langQual = valueNode.getQualifier(1);
            valueNode.removeQualifier(langQual);
            xmpParent.addQualifier(langQual);
        }

        int i;
        for (i = 1; i <= valueNode.getQualifierLength(); i++) {

            XMPNode qualifier = valueNode.getQualifier(i);
            xmpParent.addQualifier(qualifier);
        }

        for (i = 2; i <= xmpParent.getChildrenLength(); i++) {

            XMPNode qualifier = xmpParent.getChild(i);
            xmpParent.addQualifier(qualifier);
        }

        assert xmpParent.getOptions().isStruct() || xmpParent.getHasValueChild();

        xmpParent.setHasValueChild(false);
        xmpParent.getOptions().setStruct(false);
        xmpParent.getOptions().mergeWith(valueNode.getOptions());
        xmpParent.setValue(valueNode.getValue());

        xmpParent.removeChildren();
        for (Iterator<XMPNode> it = valueNode.iterateChildren(); it.hasNext();) {

            XMPNode child = it.next();
            xmpParent.addChild(child);
        }
    }

    private static boolean isWhitespaceNode(Node node) {
        if (node.getNodeType() != 3) {
            return false;
        }

        String value = node.getNodeValue();
        for (int i = 0; i < value.length(); i++) {

            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isPropertyElementName(int term) {
        if (term == 8 || isOldTerm(term)) {
            return false;
        }

        return !isCoreSyntaxTerm(term);
    }

    private static boolean isOldTerm(int term) {
        return (10 <= term && term <= 12);
    }

    private static boolean isCoreSyntaxTerm(int term) {
        return (1 <= term && term <= 7);
    }

    private static int getRDFTermKind(Node node) {
        String localName = node.getLocalName();
        String namespace = node.getNamespaceURI();

        if (namespace == null && ("about"
                .equals(localName) || "ID".equals(localName)) && node instanceof Attr && "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                .equals(((Attr) node).getOwnerElement().getNamespaceURI())) {

            namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        }

        if ("http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(namespace)) {

            if ("li".equals(localName)) {
                return 9;
            }
            if ("parseType".equals(localName)) {
                return 4;
            }
            if ("Description".equals(localName)) {
                return 8;
            }
            if ("about".equals(localName)) {
                return 3;
            }
            if ("resource".equals(localName)) {
                return 5;
            }
            if ("RDF".equals(localName)) {
                return 1;
            }
            if ("ID".equals(localName)) {
                return 2;
            }
            if ("nodeID".equals(localName)) {
                return 6;
            }
            if ("datatype".equals(localName)) {
                return 7;
            }
            if ("aboutEach".equals(localName)) {
                return 10;
            }
            if ("aboutEachPrefix".equals(localName)) {
                return 11;
            }
            if ("bagID".equals(localName)) {
                return 12;
            }
        }

        return 0;
    }
}
