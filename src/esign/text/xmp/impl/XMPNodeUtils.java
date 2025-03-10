package esign.text.xmp.impl;

import esign.text.xmp.XMPConst;
import esign.text.xmp.XMPDateTime;
import esign.text.xmp.XMPDateTimeFactory;
import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.XMPUtils;
import esign.text.xmp.impl.xpath.XMPPath;
import esign.text.xmp.impl.xpath.XMPPathSegment;
import esign.text.xmp.options.PropertyOptions;
import java.util.GregorianCalendar;
import java.util.Iterator;

public class XMPNodeUtils
        implements XMPConst {

    static final int CLT_NO_VALUES = 0;
    static final int CLT_SPECIFIC_MATCH = 1;
    static final int CLT_SINGLE_GENERIC = 2;
    static final int CLT_MULTIPLE_GENERIC = 3;
    static final int CLT_XDEFAULT = 4;
    static final int CLT_FIRST_ITEM = 5;

    static XMPNode findSchemaNode(XMPNode tree, String namespaceURI, boolean createNodes) throws XMPException {
        return findSchemaNode(tree, namespaceURI, null, createNodes);
    }

    static XMPNode findSchemaNode(XMPNode tree, String namespaceURI, String suggestedPrefix, boolean createNodes) throws XMPException {
        assert tree.getParent() == null;
        XMPNode schemaNode = tree.findChildByName(namespaceURI);

        if (schemaNode == null && createNodes) {

            schemaNode = new XMPNode(namespaceURI, (new PropertyOptions()).setSchemaNode(true));
            schemaNode.setImplicit(true);

            String prefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(namespaceURI);
            if (prefix == null) {
                if (suggestedPrefix != null && suggestedPrefix.length() != 0) {

                    prefix = XMPMetaFactory.getSchemaRegistry().registerNamespace(namespaceURI, suggestedPrefix);

                } else {

                    throw new XMPException("Unregistered schema namespace URI", 101);
                }
            }

            schemaNode.setValue(prefix);

            tree.addChild(schemaNode);
        }

        return schemaNode;
    }

    static XMPNode findChildNode(XMPNode parent, String childName, boolean createNodes) throws XMPException {
        if (!parent.getOptions().isSchemaNode() && !parent.getOptions().isStruct()) {

            if (!parent.isImplicit()) {
                throw new XMPException("Named children only allowed for schemas and structs", 102);
            }

            if (parent.getOptions().isArray()) {
                throw new XMPException("Named children not allowed for arrays", 102);
            }

            if (createNodes) {
                parent.getOptions().setStruct(true);
            }
        }

        XMPNode childNode = parent.findChildByName(childName);

        if (childNode == null && createNodes) {

            PropertyOptions options = new PropertyOptions();
            childNode = new XMPNode(childName, options);
            childNode.setImplicit(true);
            parent.addChild(childNode);
        }

        assert childNode != null || !createNodes;

        return childNode;
    }

    static XMPNode findNode(XMPNode xmpTree, XMPPath xpath, boolean createNodes, PropertyOptions leafOptions) throws XMPException {
        if (xpath == null || xpath.size() == 0) {
            throw new XMPException("Empty XMPPath", 102);
        }

        XMPNode rootImplicitNode = null;
        XMPNode currNode = null;

        currNode = findSchemaNode(xmpTree, xpath
                .getSegment(0).getName(), createNodes);
        if (currNode == null) {
            return null;
        }
        if (currNode.isImplicit()) {

            currNode.setImplicit(false);
            rootImplicitNode = currNode;
        }

        try {
            for (int i = 1; i < xpath.size(); i++) {

                currNode = followXPathStep(currNode, xpath.getSegment(i), createNodes);
                if (currNode == null) {

                    if (createNodes) {

                        deleteNode(rootImplicitNode);
                    }
                    return null;
                }
                if (currNode.isImplicit()) {

                    currNode.setImplicit(false);

                    if (i == 1 && xpath
                            .getSegment(i).isAlias() && xpath
                            .getSegment(i).getAliasForm() != 0) {

                        currNode.getOptions().setOption(xpath.getSegment(i).getAliasForm(), true);

                    } else if (i < xpath.size() - 1 && xpath
                            .getSegment(i).getKind() == 1
                            && !currNode.getOptions().isCompositeProperty()) {

                        currNode.getOptions().setStruct(true);
                    }

                    if (rootImplicitNode == null) {
                        rootImplicitNode = currNode;
                    }
                }

            }
        } catch (XMPException e) {

            if (rootImplicitNode != null) {
                deleteNode(rootImplicitNode);
            }
            throw e;
        }

        if (rootImplicitNode != null) {

            currNode.getOptions().mergeWith(leafOptions);
            currNode.setOptions(currNode.getOptions());
        }

        return currNode;
    }

    static void deleteNode(XMPNode node) {
        XMPNode parent = node.getParent();

        if (node.getOptions().isQualifier()) {

            parent.removeQualifier(node);

        } else {

            parent.removeChild(node);
        }

        if (!parent.hasChildren() && parent.getOptions().isSchemaNode()) {
            parent.getParent().removeChild(parent);
        }
    }

    static void setNodeValue(XMPNode node, Object value) {
        String strValue = serializeNodeValue(value);
        if (!node.getOptions().isQualifier() || !"xml:lang".equals(node.getName())) {

            node.setValue(strValue);
        } else {

            node.setValue(Utils.normalizeLangValue(strValue));
        }
    }

    static PropertyOptions verifySetOptions(PropertyOptions options, Object itemValue) throws XMPException {
        if (options == null) {

            options = new PropertyOptions();
        }

        if (options.isArrayAltText()) {
            options.setArrayAlternate(true);
        }

        if (options.isArrayAlternate()) {
            options.setArrayOrdered(true);
        }

        if (options.isArrayOrdered()) {
            options.setArray(true);
        }

        if (options.isCompositeProperty() && itemValue != null && itemValue.toString().length() > 0) {
            throw new XMPException("Structs and arrays can't have values", 103);
        }

        options.assertConsistency(options.getOptions());

        return options;
    }

    static String serializeNodeValue(Object value) {
        String strValue;
        if (value == null) {

            strValue = null;
        } else if (value instanceof Boolean) {

            strValue = XMPUtils.convertFromBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Integer) {

            strValue = XMPUtils.convertFromInteger(((Integer) value).intValue());
        } else if (value instanceof Long) {

            strValue = XMPUtils.convertFromLong(((Long) value).longValue());
        } else if (value instanceof Double) {

            strValue = XMPUtils.convertFromDouble(((Double) value).doubleValue());
        } else if (value instanceof XMPDateTime) {

            strValue = XMPUtils.convertFromDate((XMPDateTime) value);
        } else if (value instanceof GregorianCalendar) {

            XMPDateTime dt = XMPDateTimeFactory.createFromCalendar((GregorianCalendar) value);
            strValue = XMPUtils.convertFromDate(dt);
        } else if (value instanceof byte[]) {

            strValue = XMPUtils.encodeBase64((byte[]) value);
        } else {

            strValue = value.toString();
        }

        return (strValue != null) ? Utils.removeControlChars(strValue) : null;
    }

    private static XMPNode followXPathStep(XMPNode parentNode, XMPPathSegment nextStep, boolean createNodes) throws XMPException {
        XMPNode nextNode = null;
        int index = 0;
        int stepKind = nextStep.getKind();

        if (stepKind == 1) {

            nextNode = findChildNode(parentNode, nextStep.getName(), createNodes);
        } else if (stepKind == 2) {

            nextNode = findQualifierNode(parentNode, nextStep
                    .getName().substring(1), createNodes);

        } else {

            if (!parentNode.getOptions().isArray()) {
                throw new XMPException("Indexing applied to non-array", 102);
            }

            if (stepKind == 3) {

                index = findIndexedItem(parentNode, nextStep.getName(), createNodes);
            } else if (stepKind == 4) {

                index = parentNode.getChildrenLength();
            } else if (stepKind == 6) {

                String[] result = Utils.splitNameAndValue(nextStep.getName());
                String fieldName = result[0];
                String fieldValue = result[1];
                index = lookupFieldSelector(parentNode, fieldName, fieldValue);
            } else if (stepKind == 5) {

                String[] result = Utils.splitNameAndValue(nextStep.getName());
                String qualName = result[0];
                String qualValue = result[1];
                index = lookupQualSelector(parentNode, qualName, qualValue, nextStep
                        .getAliasForm());
            } else {

                throw new XMPException("Unknown array indexing step in FollowXPathStep", 9);
            }

            if (1 <= index && index <= parentNode.getChildrenLength()) {
                nextNode = parentNode.getChild(index);
            }
        }

        return nextNode;
    }

    private static XMPNode findQualifierNode(XMPNode parent, String qualName, boolean createNodes) throws XMPException {
        assert !qualName.startsWith("?");

        XMPNode qualNode = parent.findQualifierByName(qualName);

        if (qualNode == null && createNodes) {

            qualNode = new XMPNode(qualName, null);
            qualNode.setImplicit(true);

            parent.addQualifier(qualNode);
        }

        return qualNode;
    }

    private static int findIndexedItem(XMPNode arrayNode, String segment, boolean createNodes) throws XMPException {
        int index = 0;

        try {
            segment = segment.substring(1, segment.length() - 1);
            index = Integer.parseInt(segment);
            if (index < 1) {
                throw new XMPException("Array index must be larger than zero", 102);

            }
        } catch (NumberFormatException e) {

            throw new XMPException("Array index not digits.", 102);
        }

        if (createNodes && index == arrayNode.getChildrenLength() + 1) {

            XMPNode newItem = new XMPNode("[]", null);
            newItem.setImplicit(true);
            arrayNode.addChild(newItem);
        }

        return index;
    }

    private static int lookupFieldSelector(XMPNode arrayNode, String fieldName, String fieldValue) throws XMPException {
        int result = -1;

        for (int index = 1; index <= arrayNode.getChildrenLength() && result < 0; index++) {

            XMPNode currItem = arrayNode.getChild(index);

            if (!currItem.getOptions().isStruct()) {
                throw new XMPException("Field selector must be used on array of struct", 102);
            }

            for (int f = 1; f <= currItem.getChildrenLength(); f++) {

                XMPNode currField = currItem.getChild(f);
                if (fieldName.equals(currField.getName())) {

                    if (fieldValue.equals(currField.getValue())) {

                        result = index;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static int lookupQualSelector(XMPNode arrayNode, String qualName, String qualValue, int aliasForm) throws XMPException {
        if ("xml:lang".equals(qualName)) {

            qualValue = Utils.normalizeLangValue(qualValue);
            int i = lookupLanguageItem(arrayNode, qualValue);
            if (i < 0 && (aliasForm & 0x1000) > 0) {

                XMPNode langNode = new XMPNode("[]", null);
                XMPNode xdefault = new XMPNode("xml:lang", "x-default", null);
                langNode.addQualifier(xdefault);
                arrayNode.addChild(1, langNode);
                return 1;
            }

            return i;
        }

        for (int index = 1; index < arrayNode.getChildrenLength(); index++) {

            XMPNode currItem = arrayNode.getChild(index);

            for (Iterator<XMPNode> it = currItem.iterateQualifier(); it.hasNext();) {

                XMPNode qualifier = it.next();
                if (qualName.equals(qualifier.getName()) && qualValue
                        .equals(qualifier.getValue())) {
                    return index;
                }
            }
        }
        return -1;
    }

    static void normalizeLangArray(XMPNode arrayNode) {
        if (!arrayNode.getOptions().isArrayAltText()) {
            return;
        }

        for (int i = 2; i <= arrayNode.getChildrenLength(); i++) {

            XMPNode child = arrayNode.getChild(i);
            if (child.hasQualifier() && "x-default".equals(child.getQualifier(1).getValue())) {

                try {

                    arrayNode.removeChild(i);
                    arrayNode.addChild(1, child);
                } catch (XMPException e) {
                    assert false;
                }

                if (i == 2) {
                    arrayNode.getChild(2).setValue(child.getValue());
                }
                break;
            }
        }
    }

    static void detectAltText(XMPNode arrayNode) {
        if (arrayNode.getOptions().isArrayAlternate() && arrayNode.hasChildren()) {

            boolean isAltText = false;
            for (Iterator<XMPNode> it = arrayNode.iterateChildren(); it.hasNext();) {

                XMPNode child = it.next();
                if (child.getOptions().getHasLanguage()) {

                    isAltText = true;

                    break;
                }
            }
            if (isAltText) {

                arrayNode.getOptions().setArrayAltText(true);
                normalizeLangArray(arrayNode);
            }
        }
    }

    static void appendLangItem(XMPNode arrayNode, String itemLang, String itemValue) throws XMPException {
        XMPNode newItem = new XMPNode("[]", itemValue, null);
        XMPNode langQual = new XMPNode("xml:lang", itemLang, null);
        newItem.addQualifier(langQual);

        if (!"x-default".equals(langQual.getValue())) {

            arrayNode.addChild(newItem);
        } else {

            arrayNode.addChild(1, newItem);
        }
    }

    static Object[] chooseLocalizedText(XMPNode arrayNode, String genericLang, String specificLang) throws XMPException {
        if (!arrayNode.getOptions().isArrayAltText()) {
            throw new XMPException("Localized text array is not alt-text", 102);
        }
        if (!arrayNode.hasChildren()) {
            return new Object[]{new Integer(0), null};
        }

        int foundGenericMatches = 0;
        XMPNode resultNode = null;
        XMPNode xDefault = null;

        for (Iterator<XMPNode> it = arrayNode.iterateChildren(); it.hasNext();) {

            XMPNode currItem = it.next();

            if (currItem.getOptions().isCompositeProperty()) {
                throw new XMPException("Alt-text array item is not simple", 102);
            }
            if (!currItem.hasQualifier()
                    || !"xml:lang".equals(currItem.getQualifier(1).getName())) {
                throw new XMPException("Alt-text array item has no language qualifier", 102);
            }

            String currLang = currItem.getQualifier(1).getValue();

            if (specificLang.equals(currLang)) {
                return new Object[]{new Integer(1), currItem};
            }
            if (genericLang != null && currLang.startsWith(genericLang)) {

                if (resultNode == null) {
                    resultNode = currItem;
                }

                foundGenericMatches++;
                continue;
            }
            if ("x-default".equals(currLang)) {
                xDefault = currItem;
            }
        }

        if (foundGenericMatches == 1) {
            return new Object[]{new Integer(2), resultNode};
        }
        if (foundGenericMatches > 1) {
            return new Object[]{new Integer(3), resultNode};
        }
        if (xDefault != null) {
            return new Object[]{new Integer(4), xDefault};
        }

        return new Object[]{new Integer(5), arrayNode.getChild(1)};
    }

    static int lookupLanguageItem(XMPNode arrayNode, String language) throws XMPException {
        if (!arrayNode.getOptions().isArray()) {
            throw new XMPException("Language item must be used on array", 102);
        }

        for (int index = 1; index <= arrayNode.getChildrenLength(); index++) {

            XMPNode child = arrayNode.getChild(index);
            if (child.hasQualifier() && "xml:lang".equals(child.getQualifier(1).getName())) {

                if (language.equals(child.getQualifier(1).getValue())) {
                    return index;
                }
            }
        }
        return -1;
    }
}
