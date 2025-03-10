package esign.text.xmp.impl;

import esign.text.xmp.XMPConst;
import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMeta;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.impl.xpath.XMPPath;
import esign.text.xmp.impl.xpath.XMPPathParser;
import esign.text.xmp.options.PropertyOptions;
import esign.text.xmp.properties.XMPAliasInfo;
import java.util.Iterator;

public class XMPUtilsImpl
        implements XMPConst {

    private static final int UCK_NORMAL = 0;
    private static final int UCK_SPACE = 1;
    private static final int UCK_COMMA = 2;
    private static final int UCK_SEMICOLON = 3;
    private static final int UCK_QUOTE = 4;
    private static final int UCK_CONTROL = 5;
    private static final String SPACES = " 　〿";
    private static final String COMMAS = ",，､﹐﹑、،՝";
    private static final String SEMICOLA = ";；﹔؛;";
    private static final String QUOTES = "\"«»〝〞〟―‹›";
    private static final String CONTROLS = "  ";

    public static String catenateArrayItems(XMPMeta xmp, String schemaNS, String arrayName, String separator, String quotes, boolean allowCommas) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        ParameterAsserts.assertImplementation(xmp);
        if (separator == null || separator.length() == 0) {
            separator = "; ";
        }
        if (quotes == null || quotes.length() == 0) {
            quotes = "\"";
        }

        XMPMetaImpl xmpImpl = (XMPMetaImpl) xmp;
        XMPNode arrayNode = null;
        XMPNode currItem = null;

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        arrayNode = XMPNodeUtils.findNode(xmpImpl.getRoot(), arrayPath, false, null);
        if (arrayNode == null) {
            return "";
        }
        if (!arrayNode.getOptions().isArray() || arrayNode.getOptions().isArrayAlternate()) {
            throw new XMPException("Named property must be non-alternate array", 4);
        }

        checkSeparator(separator);

        char openQuote = quotes.charAt(0);
        char closeQuote = checkQuotes(quotes, openQuote);

        StringBuffer catinatedString = new StringBuffer();

        for (Iterator<XMPNode> it = arrayNode.iterateChildren(); it.hasNext();) {

            currItem = it.next();
            if (currItem.getOptions().isCompositeProperty()) {
                throw new XMPException("Array items must be simple", 4);
            }
            String str = applyQuotes(currItem.getValue(), openQuote, closeQuote, allowCommas);

            catinatedString.append(str);
            if (it.hasNext()) {
                catinatedString.append(separator);
            }
        }

        return catinatedString.toString();
    }

    public static void separateArrayItems(XMPMeta xmp, String schemaNS, String arrayName, String catedStr, PropertyOptions arrayOptions, boolean preserveCommas) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        if (catedStr == null) {
            throw new XMPException("Parameter must not be null", 4);
        }
        ParameterAsserts.assertImplementation(xmp);
        XMPMetaImpl xmpImpl = (XMPMetaImpl) xmp;

        XMPNode arrayNode = separateFindCreateArray(schemaNS, arrayName, arrayOptions, xmpImpl);

        int nextKind = 0, charKind = 0;
        char ch = Character.MIN_VALUE, nextChar = Character.MIN_VALUE;

        int itemEnd = 0;
        int endPos = catedStr.length();
        while (itemEnd < endPos) {
            String itemValue;

            int itemStart;
            for (itemStart = itemEnd; itemStart < endPos; itemStart++) {

                ch = catedStr.charAt(itemStart);
                charKind = classifyCharacter(ch);
                if (charKind == 0 || charKind == 4) {
                    break;
                }
            }

            if (itemStart >= endPos) {
                break;
            }

            if (charKind != 4) {

                for (itemEnd = itemStart; itemEnd < endPos; itemEnd++) {

                    ch = catedStr.charAt(itemEnd);
                    charKind = classifyCharacter(ch);

                    if (charKind == 0 || charKind == 4 || (charKind == 2 && preserveCommas)) {
                        continue;
                    }

                    if (charKind != 1) {
                        break;
                    }

                    if (itemEnd + 1 < endPos) {

                        ch = catedStr.charAt(itemEnd + 1);
                        nextKind = classifyCharacter(ch);
                        if (nextKind == 0 || nextKind == 4 || (nextKind == 2 && preserveCommas)) {
                            continue;
                        }
                    }
                }

                itemValue = catedStr.substring(itemStart, itemEnd);

            } else {

                char openQuote = ch;
                char closeQuote = getClosingQuote(openQuote);

                itemStart++;
                itemValue = "";

                for (itemEnd = itemStart; itemEnd < endPos; itemEnd++) {

                    ch = catedStr.charAt(itemEnd);
                    charKind = classifyCharacter(ch);

                    if (charKind != 4 || !isSurroundingQuote(ch, openQuote, closeQuote)) {

                        itemValue = itemValue + ch;

                    } else {

                        if (itemEnd + 1 < endPos) {

                            nextChar = catedStr.charAt(itemEnd + 1);
                            nextKind = classifyCharacter(nextChar);
                        } else {

                            nextKind = 3;
                            nextChar = ';';
                        }

                        if (ch == nextChar) {

                            itemValue = itemValue + ch;

                            itemEnd++;
                        } else if (!isClosingingQuote(ch, openQuote, closeQuote)) {

                            itemValue = itemValue + ch;

                        } else {

                            itemEnd++;

                            break;
                        }
                    }
                }
            }

            int foundIndex = -1;
            for (int oldChild = 1; oldChild <= arrayNode.getChildrenLength(); oldChild++) {

                if (itemValue.equals(arrayNode.getChild(oldChild).getValue())) {

                    foundIndex = oldChild;

                    break;
                }
            }
            XMPNode newItem = null;
            if (foundIndex < 0) {

                newItem = new XMPNode("[]", itemValue, null);
                arrayNode.addChild(newItem);
            }
        }
    }

    private static XMPNode separateFindCreateArray(String schemaNS, String arrayName, PropertyOptions arrayOptions, XMPMetaImpl xmp) throws XMPException {
        arrayOptions = XMPNodeUtils.verifySetOptions(arrayOptions, null);
        if (!arrayOptions.isOnlyArrayOptions()) {
            throw new XMPException("Options can only provide array form", 103);
        }

        XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(xmp.getRoot(), arrayPath, false, null);
        if (arrayNode != null) {

            PropertyOptions arrayForm = arrayNode.getOptions();
            if (!arrayForm.isArray() || arrayForm.isArrayAlternate()) {
                throw new XMPException("Named property must be non-alternate array", 102);
            }

            if (arrayOptions.equalArrayTypes(arrayForm)) {
                throw new XMPException("Mismatch of specified and existing array form", 102);

            }

        } else {

            arrayNode = XMPNodeUtils.findNode(xmp.getRoot(), arrayPath, true, arrayOptions
                    .setArray(true));
            if (arrayNode == null) {
                throw new XMPException("Failed to create named array", 102);
            }
        }
        return arrayNode;
    }

    public static void removeProperties(XMPMeta xmp, String schemaNS, String propName, boolean doAllProperties, boolean includeAliases) throws XMPException {
        ParameterAsserts.assertImplementation(xmp);
        XMPMetaImpl xmpImpl = (XMPMetaImpl) xmp;

        if (propName != null && propName.length() > 0) {

            if (schemaNS == null || schemaNS.length() == 0) {
                throw new XMPException("Property name requires schema namespace", 4);
            }

            XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);

            XMPNode propNode = XMPNodeUtils.findNode(xmpImpl.getRoot(), expPath, false, null);
            if (propNode != null) {
                if (doAllProperties
                        || !Utils.isInternalProperty(expPath.getSegment(0)
                                .getName(), expPath.getSegment(1).getName())) {
                    XMPNode parent = propNode.getParent();
                    parent.removeChild(propNode);
                    if (parent.getOptions().isSchemaNode() && !parent.hasChildren()) {

                        parent.getParent().removeChild(parent);
                    }
                }

            }
        } else if (schemaNS != null && schemaNS.length() > 0) {

            XMPNode schemaNode = XMPNodeUtils.findSchemaNode(xmpImpl.getRoot(), schemaNS, false);
            if (schemaNode != null) {
                if (removeSchemaChildren(schemaNode, doAllProperties)) {
                    xmpImpl.getRoot().removeChild(schemaNode);
                }
            }

            if (includeAliases) {

                XMPAliasInfo[] aliases = XMPMetaFactory.getSchemaRegistry().findAliases(schemaNS);
                for (int i = 0; i < aliases.length; i++) {
                    XMPAliasInfo info = aliases[i];
                    XMPPath path = XMPPathParser.expandXPath(info.getNamespace(), info
                            .getPropName());

                    XMPNode actualProp = XMPNodeUtils.findNode(xmpImpl.getRoot(), path, false, null);
                    if (actualProp != null) {
                        XMPNode parent = actualProp.getParent();
                        parent.removeChild(actualProp);

                    }

                }

            }

        } else {

            for (Iterator<XMPNode> it = xmpImpl.getRoot().iterateChildren(); it.hasNext();) {

                XMPNode schema = it.next();
                if (removeSchemaChildren(schema, doAllProperties)) {
                    it.remove();
                }
            }
        }
    }

    public static void appendProperties(XMPMeta source, XMPMeta destination, boolean doAllProperties, boolean replaceOldValues, boolean deleteEmptyValues) throws XMPException {
        ParameterAsserts.assertImplementation(source);
        ParameterAsserts.assertImplementation(destination);

        XMPMetaImpl src = (XMPMetaImpl) source;
        XMPMetaImpl dest = (XMPMetaImpl) destination;

        for (Iterator<XMPNode> it = src.getRoot().iterateChildren(); it.hasNext();) {

            XMPNode sourceSchema = it.next();

            XMPNode destSchema = XMPNodeUtils.findSchemaNode(dest.getRoot(), sourceSchema
                    .getName(), false);
            boolean createdSchema = false;
            if (destSchema == null) {

                destSchema = new XMPNode(sourceSchema.getName(), sourceSchema.getValue(), (new PropertyOptions()).setSchemaNode(true));
                dest.getRoot().addChild(destSchema);
                createdSchema = true;
            }

            for (Iterator<XMPNode> ic = sourceSchema.iterateChildren(); ic.hasNext();) {

                XMPNode sourceProp = ic.next();
                if (doAllProperties
                        || !Utils.isInternalProperty(sourceSchema.getName(), sourceProp.getName())) {
                    appendSubtree(dest, sourceProp, destSchema, replaceOldValues, deleteEmptyValues);
                }
            }

            if (!destSchema.hasChildren() && (createdSchema || deleteEmptyValues)) {

                dest.getRoot().removeChild(destSchema);
            }
        }
    }

    private static boolean removeSchemaChildren(XMPNode schemaNode, boolean doAllProperties) {
        for (Iterator<XMPNode> it = schemaNode.iterateChildren(); it.hasNext();) {

            XMPNode currProp = it.next();
            if (doAllProperties
                    || !Utils.isInternalProperty(schemaNode.getName(), currProp.getName())) {
                it.remove();
            }
        }

        return !schemaNode.hasChildren();
    }

    private static void appendSubtree(XMPMetaImpl destXMP, XMPNode sourceNode, XMPNode destParent, boolean replaceOldValues, boolean deleteEmptyValues) throws XMPException {
        XMPNode destNode = XMPNodeUtils.findChildNode(destParent, sourceNode.getName(), false);

        boolean valueIsEmpty = false;
        if (deleteEmptyValues) {

            valueIsEmpty = sourceNode.getOptions().isSimple() ? ((sourceNode.getValue() == null || sourceNode.getValue().length() == 0)) : (!sourceNode.hasChildren());
        }

        if (deleteEmptyValues && valueIsEmpty) {

            if (destNode != null) {
                destParent.removeChild(destNode);
            }
        } else if (destNode == null) {

            destParent.addChild((XMPNode) sourceNode.clone());
        } else if (replaceOldValues) {

            destXMP.setNode(destNode, sourceNode.getValue(), sourceNode.getOptions(), true);
            destParent.removeChild(destNode);
            destNode = (XMPNode) sourceNode.clone();
            destParent.addChild(destNode);

        } else {

            PropertyOptions sourceForm = sourceNode.getOptions();
            PropertyOptions destForm = destNode.getOptions();
            if (sourceForm != destForm) {
                return;
            }

            if (sourceForm.isStruct()) {

                for (Iterator<XMPNode> it = sourceNode.iterateChildren(); it.hasNext();) {
                    XMPNode sourceField = it.next();
                    appendSubtree(destXMP, sourceField, destNode, replaceOldValues, deleteEmptyValues);

                    if (deleteEmptyValues && !destNode.hasChildren()) {
                        destParent.removeChild(destNode);
                    }
                }

            } else if (sourceForm.isArrayAltText()) {

                for (Iterator<XMPNode> it = sourceNode.iterateChildren(); it.hasNext();) {
                    XMPNode sourceItem = it.next();
                    if (!sourceItem.hasQualifier()
                            || !"xml:lang".equals(sourceItem.getQualifier(1).getName())) {
                        continue;
                    }

                    int destIndex = XMPNodeUtils.lookupLanguageItem(destNode, sourceItem
                            .getQualifier(1).getValue());
                    if (deleteEmptyValues && (sourceItem
                            .getValue() == null || sourceItem
                                    .getValue().length() == 0)) {

                        if (destIndex != -1) {

                            destNode.removeChild(destIndex);
                            if (!destNode.hasChildren()) {
                                destParent.removeChild(destNode);
                            }
                        }
                        continue;
                    }
                    if (destIndex == -1) {

                        if (!"x-default".equals(sourceItem.getQualifier(1).getValue())
                                || !destNode.hasChildren()) {

                            sourceItem.cloneSubtree(destNode);

                            continue;
                        }

                        XMPNode destItem = new XMPNode(sourceItem.getName(), sourceItem.getValue(), sourceItem.getOptions());
                        sourceItem.cloneSubtree(destItem);
                        destNode.addChild(1, destItem);
                    }

                }

            } else if (sourceForm.isArray()) {

                for (Iterator<XMPNode> is = sourceNode.iterateChildren(); is.hasNext();) {

                    XMPNode sourceItem = is.next();

                    boolean match = false;
                    for (Iterator<XMPNode> id = destNode.iterateChildren(); id.hasNext();) {

                        XMPNode destItem = id.next();
                        if (itemValuesMatch(sourceItem, destItem)) {
                            match = true;
                        }
                    }
                    if (!match) {

                        destNode = (XMPNode) sourceItem.clone();
                        destParent.addChild(destNode);
                    }
                }
            }
        }
    }

    private static boolean itemValuesMatch(XMPNode leftNode, XMPNode rightNode) throws XMPException {
        PropertyOptions leftForm = leftNode.getOptions();
        PropertyOptions rightForm = rightNode.getOptions();

        if (leftForm.equals(rightForm)) {
            return false;
        }

        if (leftForm.getOptions() == 0) {

            if (!leftNode.getValue().equals(rightNode.getValue())) {
                return false;
            }
            if (leftNode.getOptions().getHasLanguage() != rightNode.getOptions().getHasLanguage()) {
                return false;
            }
            if (leftNode.getOptions().getHasLanguage()
                    && !leftNode.getQualifier(1).getValue().equals(rightNode
                            .getQualifier(1).getValue())) {
                return false;
            }
        } else if (leftForm.isStruct()) {

            if (leftNode.getChildrenLength() != rightNode.getChildrenLength()) {
                return false;
            }

            for (Iterator<XMPNode> it = leftNode.iterateChildren(); it.hasNext();) {
                XMPNode leftField = it.next();
                XMPNode rightField = XMPNodeUtils.findChildNode(rightNode, leftField.getName(), false);

                if (rightField == null || !itemValuesMatch(leftField, rightField)) {
                    return false;

                }

            }

        } else {

            assert leftForm.isArray();

            for (Iterator<XMPNode> il = leftNode.iterateChildren(); il.hasNext();) {

                XMPNode leftItem = il.next();

                boolean match = false;
                for (Iterator<XMPNode> ir = rightNode.iterateChildren(); ir.hasNext();) {

                    XMPNode rightItem = ir.next();
                    if (itemValuesMatch(leftItem, rightItem)) {

                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void checkSeparator(String separator) throws XMPException {
        boolean haveSemicolon = false;
        for (int i = 0; i < separator.length(); i++) {

            int charKind = classifyCharacter(separator.charAt(i));
            if (charKind == 3) {

                if (haveSemicolon) {
                    throw new XMPException("Separator can have only one semicolon", 4);
                }

                haveSemicolon = true;
            } else if (charKind != 1) {

                throw new XMPException("Separator can have only spaces and one semicolon", 4);
            }
        }

        if (!haveSemicolon) {
            throw new XMPException("Separator must have one semicolon", 4);
        }
    }

    private static char checkQuotes(String quotes, char openQuote) throws XMPException {
        char closeQuote;
        int charKind = classifyCharacter(openQuote);
        if (charKind != 4) {
            throw new XMPException("Invalid quoting character", 4);
        }

        if (quotes.length() == 1) {

            closeQuote = openQuote;
        } else {

            closeQuote = quotes.charAt(1);
            charKind = classifyCharacter(closeQuote);
            if (charKind != 4) {
                throw new XMPException("Invalid quoting character", 4);
            }
        }

        if (closeQuote != getClosingQuote(openQuote)) {
            throw new XMPException("Mismatched quote pair", 4);
        }
        return closeQuote;
    }

    private static int classifyCharacter(char ch) {
        if (" 　〿".indexOf(ch) >= 0 || (' ' <= ch && ch <= '​')) {
            return 1;
        }
        if (",，､﹐﹑、،՝".indexOf(ch) >= 0) {
            return 2;
        }
        if (";；﹔؛;".indexOf(ch) >= 0) {
            return 3;
        }
        if ("\"«»〝〞〟―‹›".indexOf(ch) >= 0 || ('〈' <= ch && ch <= '』') || ('‘' <= ch && ch <= '‟')) {

            return 4;
        }
        if (ch < ' ' || "  ".indexOf(ch) >= 0) {
            return 5;
        }

        return 0;
    }

    private static char getClosingQuote(char openQuote) {
        switch (openQuote) {

            case '"':
                return '"';

            case '«':
                return '»';
            case '»':
                return '«';
            case '―':
                return '―';
            case '‘':
                return '’';
            case '‚':
                return '‛';
            case '“':
                return '”';
            case '„':
                return '‟';
            case '‹':
                return '›';
            case '›':
                return '‹';
            case '〈':
                return '〉';
            case '《':
                return '》';
            case '「':
                return '」';
            case '『':
                return '』';
            case '〝':
                return '〟';
        }
        return Character.MIN_VALUE;
    }

    private static String applyQuotes(String item, char openQuote, char closeQuote, boolean allowCommas) {
        if (item == null) {
            item = "";
        }

        boolean prevSpace = false;

        int i;

        for (i = 0; i < item.length(); i++) {

            char ch = item.charAt(i);
            int charKind = classifyCharacter(ch);
            if (i == 0 && charKind == 4) {
                break;
            }

            if (charKind == 1) {

                if (prevSpace) {
                    break;
                }

                prevSpace = true;
            } else {

                prevSpace = false;
                if (charKind == 3 || charKind == 5 || (charKind == 2 && !allowCommas)) {
                    break;
                }
            }
        }

        if (i < item.length()) {

            StringBuffer newItem = new StringBuffer(item.length() + 2);
            int splitPoint;
            for (splitPoint = 0; splitPoint <= i; splitPoint++) {

                if (classifyCharacter(item.charAt(i)) == 4) {
                    break;
                }
            }

            newItem.append(openQuote).append(item.substring(0, splitPoint));

            for (int charOffset = splitPoint; charOffset < item.length(); charOffset++) {

                newItem.append(item.charAt(charOffset));
                if (classifyCharacter(item.charAt(charOffset)) == 4
                        && isSurroundingQuote(item.charAt(charOffset), openQuote, closeQuote)) {
                    newItem.append(item.charAt(charOffset));
                }
            }

            newItem.append(closeQuote);

            item = newItem.toString();
        }

        return item;
    }

    private static boolean isSurroundingQuote(char ch, char openQuote, char closeQuote) {
        return (ch == openQuote || isClosingingQuote(ch, openQuote, closeQuote));
    }

    private static boolean isClosingingQuote(char ch, char openQuote, char closeQuote) {
        return (ch == closeQuote || (openQuote == '〝' && ch == '〞') || ch == '〟');
    }
}


