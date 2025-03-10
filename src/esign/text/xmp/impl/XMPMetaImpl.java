package esign.text.xmp.impl;

import esign.text.xmp.XMPConst;
import esign.text.xmp.XMPDateTime;
import esign.text.xmp.XMPException;
import esign.text.xmp.XMPIterator;
import esign.text.xmp.XMPMeta;
import esign.text.xmp.XMPPathFactory;
import esign.text.xmp.XMPUtils;
import esign.text.xmp.impl.xpath.XMPPath;
import esign.text.xmp.impl.xpath.XMPPathParser;
import esign.text.xmp.options.IteratorOptions;
import esign.text.xmp.options.ParseOptions;
import esign.text.xmp.options.PropertyOptions;
import esign.text.xmp.properties.XMPProperty;
import java.util.Calendar;
import java.util.Iterator;

public class XMPMetaImpl
        implements XMPMeta, XMPConst {

    private static final int VALUE_STRING = 0;
    private static final int VALUE_BOOLEAN = 1;
    private static final int VALUE_INTEGER = 2;
    private static final int VALUE_LONG = 3;
    private static final int VALUE_DOUBLE = 4;
    private static final int VALUE_DATE = 5;
    private static final int VALUE_CALENDAR = 6;
    private static final int VALUE_BASE64 = 7;
    private XMPNode tree;
    private String packetHeader = null;

    public XMPMetaImpl() {
        this.tree = new XMPNode(null, null, null);
    }

    public XMPMetaImpl(XMPNode tree) {
        this.tree = tree;
    }

    public void appendArrayItem(String schemaNS, String arrayName, PropertyOptions arrayOptions, String itemValue, PropertyOptions itemOptions) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);

        if (arrayOptions == null) {
            arrayOptions = new PropertyOptions();
        }
        if (!arrayOptions.isOnlyArrayOptions()) {
            throw new XMPException("Only array form flags allowed for arrayOptions", 103);
        }

        arrayOptions = XMPNodeUtils.verifySetOptions(arrayOptions, null);

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);

        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);

        if (arrayNode != null) {

            if (!arrayNode.getOptions().isArray()) {
                throw new XMPException("The named property is not an array", 102);

            }

        } else if (arrayOptions.isArray()) {

            arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, true, arrayOptions);
            if (arrayNode == null) {
                throw new XMPException("Failure creating array node", 102);

            }
        } else {

            throw new XMPException("Explicit arrayOptions required to create new array", 103);
        }

        doSetArrayItem(arrayNode, -1, itemValue, itemOptions, true);
    }

    public void appendArrayItem(String schemaNS, String arrayName, String itemValue) throws XMPException {
        appendArrayItem(schemaNS, arrayName, null, itemValue, null);
    }

    public int countArrayItems(String schemaNS, String arrayName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);

        if (arrayNode == null) {
            return 0;
        }

        if (arrayNode.getOptions().isArray()) {
            return arrayNode.getChildrenLength();
        }

        throw new XMPException("The named property is not an array", 102);
    }

    public void deleteArrayItem(String schemaNS, String arrayName, int itemIndex) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertArrayName(arrayName);

            String itemPath = XMPPathFactory.composeArrayItemPath(arrayName, itemIndex);
            deleteProperty(schemaNS, itemPath);
        } catch (XMPException xMPException) {
        }
    }

    public void deleteProperty(String schemaNS, String propName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);

            XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);

            XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);
            if (propNode != null) {
                XMPNodeUtils.deleteNode(propNode);
            }
        } catch (XMPException xMPException) {
        }
    }

    public void deleteQualifier(String schemaNS, String propName, String qualNS, String qualName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);

            String qualPath = propName + XMPPathFactory.composeQualifierPath(qualNS, qualName);
            deleteProperty(schemaNS, qualPath);
        } catch (XMPException xMPException) {
        }
    }

    public void deleteStructField(String schemaNS, String structName, String fieldNS, String fieldName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertStructName(structName);

            String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
            deleteProperty(schemaNS, fieldPath);
        } catch (XMPException xMPException) {
        }
    }

    public boolean doesPropertyExist(String schemaNS, String propName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);

            XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
            XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);
            return (propNode != null);
        } catch (XMPException e) {

            return false;
        }
    }

    public boolean doesArrayItemExist(String schemaNS, String arrayName, int itemIndex) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertArrayName(arrayName);

            String path = XMPPathFactory.composeArrayItemPath(arrayName, itemIndex);
            return doesPropertyExist(schemaNS, path);
        } catch (XMPException e) {

            return false;
        }
    }

    public boolean doesStructFieldExist(String schemaNS, String structName, String fieldNS, String fieldName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertStructName(structName);

            String path = XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
            return doesPropertyExist(schemaNS, structName + path);
        } catch (XMPException e) {

            return false;
        }
    }

    public boolean doesQualifierExist(String schemaNS, String propName, String qualNS, String qualName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);

            String path = XMPPathFactory.composeQualifierPath(qualNS, qualName);
            return doesPropertyExist(schemaNS, propName + path);
        } catch (XMPException e) {

            return false;
        }
    }

    public XMPProperty getArrayItem(String schemaNS, String arrayName, int itemIndex) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);

        String itemPath = XMPPathFactory.composeArrayItemPath(arrayName, itemIndex);
        return getProperty(schemaNS, itemPath);
    }

    public XMPProperty getLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(altTextName);
        ParameterAsserts.assertSpecificLang(specificLang);

        genericLang = (genericLang != null) ? Utils.normalizeLangValue(genericLang) : null;
        specificLang = Utils.normalizeLangValue(specificLang);

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, altTextName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);
        if (arrayNode == null) {
            return null;
        }

        Object[] result = XMPNodeUtils.chooseLocalizedText(arrayNode, genericLang, specificLang);
        int match = ((Integer) result[0]).intValue();
        final XMPNode itemNode = (XMPNode) result[1];

        if (match != 0) {
            return new XMPProperty() {
                public String getValue() {
                    return itemNode.getValue();
                }

                public PropertyOptions getOptions() {
                    return itemNode.getOptions();
                }

                public String getLanguage() {
                    return itemNode.getQualifier(1).getValue();
                }

                public String toString() {
                    return itemNode.getValue().toString();
                }
            };
        }

        return null;
    }

    public void setLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang, String itemValue, PropertyOptions options) throws XMPException {
        Iterator<XMPNode> iterator1;
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(altTextName);
        ParameterAsserts.assertSpecificLang(specificLang);

        genericLang = (genericLang != null) ? Utils.normalizeLangValue(genericLang) : null;
        specificLang = Utils.normalizeLangValue(specificLang);

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, altTextName);

        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, true, new PropertyOptions(7680));

        if (arrayNode == null) {
            throw new XMPException("Failed to find or create array node", 102);
        }
        if (!arrayNode.getOptions().isArrayAltText()) {
            if (!arrayNode.hasChildren() && arrayNode.getOptions().isArrayAlternate()) {

                arrayNode.getOptions().setArrayAltText(true);
            } else {

                throw new XMPException("Specified property is no alt-text array", 102);
            }
        }

        boolean haveXDefault = false;
        XMPNode xdItem = null;

        for (Iterator<XMPNode> it = arrayNode.iterateChildren(); it.hasNext();) {

            XMPNode currItem = it.next();
            if (!currItem.hasQualifier()
                    || !"xml:lang".equals(currItem.getQualifier(1).getName())) {
                throw new XMPException("Language qualifier must be first", 102);
            }
            if ("x-default".equals(currItem.getQualifier(1).getValue())) {

                xdItem = currItem;
                haveXDefault = true;

                break;
            }
        }

        if (xdItem != null && arrayNode.getChildrenLength() > 1) {

            arrayNode.removeChild(xdItem);
            arrayNode.addChild(1, xdItem);
        }

        Object[] result = XMPNodeUtils.chooseLocalizedText(arrayNode, genericLang, specificLang);
        int match = ((Integer) result[0]).intValue();
        XMPNode itemNode = (XMPNode) result[1];

        boolean specificXDefault = "x-default".equals(specificLang);

        switch (match) {

            case 0:
                XMPNodeUtils.appendLangItem(arrayNode, "x-default", itemValue);
                haveXDefault = true;
                if (!specificXDefault) {
                    XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                }
                break;

            case 1:
                if (!specificXDefault) {

                    if (haveXDefault && xdItem != itemNode && xdItem != null && xdItem
                            .getValue().equals(itemNode.getValue())) {
                        xdItem.setValue(itemValue);
                    }

                    itemNode.setValue(itemValue);

                    break;
                }

                assert haveXDefault && xdItem == itemNode;
                for (iterator1 = arrayNode.iterateChildren(); iterator1.hasNext();) {

                    XMPNode currItem = iterator1.next();
                    if (currItem == xdItem
                            || !currItem.getValue().equals((xdItem != null) ? xdItem
                                    .getValue() : null)) {
                        continue;
                    }

                    currItem.setValue(itemValue);
                }

                if (xdItem != null) {
                    xdItem.setValue(itemValue);
                }
                break;

            case 2:
                if (haveXDefault && xdItem != itemNode && xdItem != null && xdItem
                        .getValue().equals(itemNode.getValue())) {
                    xdItem.setValue(itemValue);
                }
                itemNode.setValue(itemValue);
                break;

            case 3:
                XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                if (specificXDefault) {
                    haveXDefault = true;
                }
                break;

            case 4:
                if (xdItem != null && arrayNode.getChildrenLength() == 1) {
                    xdItem.setValue(itemValue);
                }
                XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                break;

            case 5:
                XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                if (specificXDefault) {
                    haveXDefault = true;
                }
                break;

            default:
                throw new XMPException("Unexpected result from ChooseLocalizedText", 9);
        }

        if (!haveXDefault && arrayNode.getChildrenLength() == 1) {
            XMPNodeUtils.appendLangItem(arrayNode, "x-default", itemValue);
        }
    }

    public void setLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang, String itemValue) throws XMPException {
        setLocalizedText(schemaNS, altTextName, genericLang, specificLang, itemValue, null);
    }

    public XMPProperty getProperty(String schemaNS, String propName) throws XMPException {
        return getProperty(schemaNS, propName, 0);
    }

    protected XMPProperty getProperty(String schemaNS, String propName, int valueType) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);

        XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
        final XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);

        if (propNode != null) {

            if (valueType != 0 && propNode.getOptions().isCompositeProperty()) {
                throw new XMPException("Property must be simple when a value type is requested", 102);
            }

            final Object value = evaluateNodeValue(valueType, propNode);

            return new XMPProperty() {
                public String getValue() {
                    return (value != null) ? value.toString() : null;
                }

                public PropertyOptions getOptions() {
                    return propNode.getOptions();
                }

                public String getLanguage() {
                    return null;
                }

                public String toString() {
                    return value.toString();
                }
            };
        }

        return null;
    }

    protected Object getPropertyObject(String schemaNS, String propName, int valueType) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);

        XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
        XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, false, null);

        if (propNode != null) {

            if (valueType != 0 && propNode.getOptions().isCompositeProperty()) {
                throw new XMPException("Property must be simple when a value type is requested", 102);
            }

            return evaluateNodeValue(valueType, propNode);
        }

        return null;
    }

    public Boolean getPropertyBoolean(String schemaNS, String propName) throws XMPException {
        return (Boolean) getPropertyObject(schemaNS, propName, 1);
    }

    public void setPropertyBoolean(String schemaNS, String propName, boolean propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue ? "True" : "False", options);
    }

    public void setPropertyBoolean(String schemaNS, String propName, boolean propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue ? "True" : "False", null);
    }

    public Integer getPropertyInteger(String schemaNS, String propName) throws XMPException {
        return (Integer) getPropertyObject(schemaNS, propName, 2);
    }

    public void setPropertyInteger(String schemaNS, String propName, int propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Integer(propValue), options);
    }

    public void setPropertyInteger(String schemaNS, String propName, int propValue) throws XMPException {
        setProperty(schemaNS, propName, new Integer(propValue), null);
    }

    public Long getPropertyLong(String schemaNS, String propName) throws XMPException {
        return (Long) getPropertyObject(schemaNS, propName, 3);
    }

    public void setPropertyLong(String schemaNS, String propName, long propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Long(propValue), options);
    }

    public void setPropertyLong(String schemaNS, String propName, long propValue) throws XMPException {
        setProperty(schemaNS, propName, new Long(propValue), null);
    }

    public Double getPropertyDouble(String schemaNS, String propName) throws XMPException {
        return (Double) getPropertyObject(schemaNS, propName, 4);
    }

    public void setPropertyDouble(String schemaNS, String propName, double propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Double(propValue), options);
    }

    public void setPropertyDouble(String schemaNS, String propName, double propValue) throws XMPException {
        setProperty(schemaNS, propName, new Double(propValue), null);
    }

    public XMPDateTime getPropertyDate(String schemaNS, String propName) throws XMPException {
        return (XMPDateTime) getPropertyObject(schemaNS, propName, 5);
    }

    public void setPropertyDate(String schemaNS, String propName, XMPDateTime propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    public void setPropertyDate(String schemaNS, String propName, XMPDateTime propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public Calendar getPropertyCalendar(String schemaNS, String propName) throws XMPException {
        return (Calendar) getPropertyObject(schemaNS, propName, 6);
    }

    public void setPropertyCalendar(String schemaNS, String propName, Calendar propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    public void setPropertyCalendar(String schemaNS, String propName, Calendar propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public byte[] getPropertyBase64(String schemaNS, String propName) throws XMPException {
        return (byte[]) getPropertyObject(schemaNS, propName, 7);
    }

    public String getPropertyString(String schemaNS, String propName) throws XMPException {
        return (String) getPropertyObject(schemaNS, propName, 0);
    }

    public void setPropertyBase64(String schemaNS, String propName, byte[] propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    public void setPropertyBase64(String schemaNS, String propName, byte[] propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public XMPProperty getQualifier(String schemaNS, String propName, String qualNS, String qualName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);

        String qualPath = propName + XMPPathFactory.composeQualifierPath(qualNS, qualName);
        return getProperty(schemaNS, qualPath);
    }

    public XMPProperty getStructField(String schemaNS, String structName, String fieldNS, String fieldName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertStructName(structName);

        String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
        return getProperty(schemaNS, fieldPath);
    }

    public XMPIterator iterator() throws XMPException {
        return iterator(null, null, null);
    }

    public XMPIterator iterator(IteratorOptions options) throws XMPException {
        return iterator(null, null, options);
    }

    public XMPIterator iterator(String schemaNS, String propName, IteratorOptions options) throws XMPException {
        return new XMPIteratorImpl(this, schemaNS, propName, options);
    }

    public void setArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);

        if (arrayNode != null) {

            doSetArrayItem(arrayNode, itemIndex, itemValue, options, false);
        } else {

            throw new XMPException("Specified array does not exist", 102);
        }
    }

    public void setArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue) throws XMPException {
        setArrayItem(schemaNS, arrayName, itemIndex, itemValue, null);
    }

    public void insertArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);

        if (arrayNode != null) {

            doSetArrayItem(arrayNode, itemIndex, itemValue, options, true);
        } else {

            throw new XMPException("Specified array does not exist", 102);
        }
    }

    public void insertArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue) throws XMPException {
        insertArrayItem(schemaNS, arrayName, itemIndex, itemValue, null);
    }

    public void setProperty(String schemaNS, String propName, Object propValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);

        options = XMPNodeUtils.verifySetOptions(options, propValue);

        XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);

        XMPNode propNode = XMPNodeUtils.findNode(this.tree, expPath, true, options);
        if (propNode != null) {

            setNode(propNode, propValue, options, false);
        } else {

            throw new XMPException("Specified property does not exist", 102);
        }
    }

    public void setProperty(String schemaNS, String propName, Object propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public void setQualifier(String schemaNS, String propName, String qualNS, String qualName, String qualValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);

        if (!doesPropertyExist(schemaNS, propName)) {
            throw new XMPException("Specified property does not exist!", 102);
        }

        String qualPath = propName + XMPPathFactory.composeQualifierPath(qualNS, qualName);
        setProperty(schemaNS, qualPath, qualValue, options);
    }

    public void setQualifier(String schemaNS, String propName, String qualNS, String qualName, String qualValue) throws XMPException {
        setQualifier(schemaNS, propName, qualNS, qualName, qualValue, null);
    }

    public void setStructField(String schemaNS, String structName, String fieldNS, String fieldName, String fieldValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertStructName(structName);

        String fieldPath = structName + XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
        setProperty(schemaNS, fieldPath, fieldValue, options);
    }

    public void setStructField(String schemaNS, String structName, String fieldNS, String fieldName, String fieldValue) throws XMPException {
        setStructField(schemaNS, structName, fieldNS, fieldName, fieldValue, null);
    }

    public String getObjectName() {
        return (this.tree.getName() != null) ? this.tree.getName() : "";
    }

    public void setObjectName(String name) {
        this.tree.setName(name);
    }

    public String getPacketHeader() {
        return this.packetHeader;
    }

    public void setPacketHeader(String packetHeader) {
        this.packetHeader = packetHeader;
    }

    public Object clone() {
        XMPNode clonedTree = (XMPNode) this.tree.clone();
        return new XMPMetaImpl(clonedTree);
    }

    public String dumpObject() {
        return getRoot().dumpNode(true);
    }

    public void sort() {
        this.tree.sort();
    }

    public void normalize(ParseOptions options) throws XMPException {
        if (options == null) {
            options = new ParseOptions();
        }
        XMPNormalizer.process(this, options);
    }

    public XMPNode getRoot() {
        return this.tree;
    }

    private void doSetArrayItem(XMPNode arrayNode, int itemIndex, String itemValue, PropertyOptions itemOptions, boolean insert) throws XMPException {
        XMPNode itemNode = new XMPNode("[]", null);
        itemOptions = XMPNodeUtils.verifySetOptions(itemOptions, itemValue);

        int maxIndex = insert ? (arrayNode.getChildrenLength() + 1) : arrayNode.getChildrenLength();
        if (itemIndex == -1) {
            itemIndex = maxIndex;
        }

        if (1 <= itemIndex && itemIndex <= maxIndex) {

            if (!insert) {
                arrayNode.removeChild(itemIndex);
            }
            arrayNode.addChild(itemIndex, itemNode);
            setNode(itemNode, itemValue, itemOptions, false);
        } else {

            throw new XMPException("Array index out of bounds", 104);
        }
    }

    void setNode(XMPNode node, Object value, PropertyOptions newOptions, boolean deleteExisting) throws XMPException {
        if (deleteExisting) {
            node.clear();
        }

        node.getOptions().mergeWith(newOptions);

        if (!node.getOptions().isCompositeProperty()) {

            XMPNodeUtils.setNodeValue(node, value);
        } else {

            if (value != null && value.toString().length() > 0) {
                throw new XMPException("Composite nodes can't have values", 102);
            }

            node.removeChildren();
        }
    }

    private Object evaluateNodeValue(int valueType, XMPNode propNode) throws XMPException {
        XMPDateTime dt;
        String rawValue = propNode.getValue();
        Object value = (rawValue != null || propNode.getOptions().isCompositeProperty()) ? rawValue : "";
        switch (valueType) {
            case 1:
                value = new Boolean(XMPUtils.convertToBoolean(rawValue));

                return value;
            case 2:
                value = new Integer(XMPUtils.convertToInteger(rawValue));
                return value;
            case 3:
                value = new Long(XMPUtils.convertToLong(rawValue));
                return value;
            case 4:
                value = new Double(XMPUtils.convertToDouble(rawValue));
                return value;
            case 5:
                value = XMPUtils.convertToDate(rawValue);
                return value;
            case 6:
                dt = XMPUtils.convertToDate(rawValue);
                value = dt.getCalendar();
                return value;
            case 7:
                value = XMPUtils.decodeBase64(rawValue);
                return value;
        }
        
        return value;
    }
}

