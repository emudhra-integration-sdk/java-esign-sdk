package esign.text.xmp.impl;

import esign.text.xmp.XMPDateTime;
import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMeta;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.XMPUtils;
import esign.text.xmp.impl.xpath.XMPPath;
import esign.text.xmp.impl.xpath.XMPPathParser;
import esign.text.xmp.options.ParseOptions;
import esign.text.xmp.options.PropertyOptions;
import esign.text.xmp.properties.XMPAliasInfo;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XMPNormalizer {

    private static Map dcArrayForms;

    static {
        initDCArrays();
    }

    static XMPMeta process(XMPMetaImpl xmp, ParseOptions options) throws XMPException {
        XMPNode tree = xmp.getRoot();

        touchUpDataModel(xmp);
        moveExplicitAliases(tree, options);

        tweakOldXMP(tree);

        deleteEmptySchemas(tree);

        return xmp;
    }

    private static void tweakOldXMP(XMPNode tree) throws XMPException {
        if (tree.getName() != null && tree.getName().length() >= 36) {

            String nameStr = tree.getName().toLowerCase();
            if (nameStr.startsWith("uuid:")) {
                nameStr = nameStr.substring(5);
            }

            if (Utils.checkUUIDFormat(nameStr)) {

                XMPPath path = XMPPathParser.expandXPath("http://ns.adobe.com/xap/1.0/mm/", "InstanceID");
                XMPNode idNode = XMPNodeUtils.findNode(tree, path, true, null);
                if (idNode != null) {

                    idNode.setOptions(null);
                    idNode.setValue("uuid:" + nameStr);
                    idNode.removeChildren();
                    idNode.removeQualifiers();
                    tree.setName(null);
                } else {

                    throw new XMPException("Failure creating xmpMM:InstanceID", 9);
                }
            }
        }
    }

    private static void touchUpDataModel(XMPMetaImpl xmp) throws XMPException {
        XMPNodeUtils.findSchemaNode(xmp.getRoot(), "http://purl.org/dc/elements/1.1/", true);

        for (Iterator<XMPNode> it = xmp.getRoot().iterateChildren(); it.hasNext();) {

            XMPNode currSchema = it.next();
            if ("http://purl.org/dc/elements/1.1/".equals(currSchema.getName())) {

                normalizeDCArrays(currSchema);
                continue;
            }
            if ("http://ns.adobe.com/exif/1.0/".equals(currSchema.getName())) {

                fixGPSTimeStamp(currSchema);
                XMPNode arrayNode = XMPNodeUtils.findChildNode(currSchema, "exif:UserComment", false);

                if (arrayNode != null) {
                    repairAltText(arrayNode);
                }
                continue;
            }
            if ("http://ns.adobe.com/xmp/1.0/DynamicMedia/".equals(currSchema.getName())) {

                XMPNode dmCopyright = XMPNodeUtils.findChildNode(currSchema, "xmpDM:copyright", false);

                if (dmCopyright != null) {
                    migrateAudioCopyright(xmp, dmCopyright);
                }
                continue;
            }
            if ("http://ns.adobe.com/xap/1.0/rights/".equals(currSchema.getName())) {

                XMPNode arrayNode = XMPNodeUtils.findChildNode(currSchema, "xmpRights:UsageTerms", false);

                if (arrayNode != null) {
                    repairAltText(arrayNode);
                }
            }
        }
    }

    private static void normalizeDCArrays(XMPNode dcSchema) throws XMPException {
        for (int i = 1; i <= dcSchema.getChildrenLength(); i++) {

            XMPNode currProp = dcSchema.getChild(i);

            PropertyOptions arrayForm = (PropertyOptions) dcArrayForms.get(currProp.getName());
            if (arrayForm != null) {

                if (currProp.getOptions().isSimple()) {

                    XMPNode newArray = new XMPNode(currProp.getName(), arrayForm);
                    currProp.setName("[]");
                    newArray.addChild(currProp);
                    dcSchema.replaceChild(i, newArray);

                    if (arrayForm.isArrayAltText() && !currProp.getOptions().getHasLanguage()) {
                        XMPNode newLang = new XMPNode("xml:lang", "x-default", null);
                        currProp.addQualifier(newLang);
                    }

                } else {

                    currProp.getOptions().setOption(7680, false);

                    currProp.getOptions().mergeWith(arrayForm);

                    if (arrayForm.isArrayAltText()) {

                        repairAltText(currProp);
                    }
                }
            }
        }
    }

    private static void repairAltText(XMPNode arrayNode) throws XMPException {
        if (arrayNode == null
                || !arrayNode.getOptions().isArray()) {
            return;
        }

        arrayNode.getOptions().setArrayOrdered(true).setArrayAlternate(true).setArrayAltText(true);

        for (Iterator<XMPNode> it = arrayNode.iterateChildren(); it.hasNext();) {

            XMPNode currChild = it.next();
            if (currChild.getOptions().isCompositeProperty()) {

                it.remove();
                continue;
            }
            if (!currChild.getOptions().getHasLanguage()) {

                String childValue = currChild.getValue();
                if (childValue == null || childValue.length() == 0) {

                    it.remove();

                    continue;
                }

                XMPNode repairLang = new XMPNode("xml:lang", "x-repair", null);
                currChild.addQualifier(repairLang);
            }
        }
    }

    private static void moveExplicitAliases(XMPNode tree, ParseOptions options) throws XMPException {
        if (!tree.getHasAliases()) {
            return;
        }

        tree.setHasAliases(false);

        boolean strictAliasing = options.getStrictAliasing();

        for (Iterator<XMPNode> schemaIt = tree.getUnmodifiableChildren().iterator(); schemaIt.hasNext();) {

            XMPNode currSchema = schemaIt.next();
            if (!currSchema.getHasAliases()) {
                continue;
            }

            for (Iterator<XMPNode> propertyIt = currSchema.iterateChildren(); propertyIt.hasNext();) {

                XMPNode currProp = propertyIt.next();

                if (!currProp.isAlias()) {
                    continue;
                }

                currProp.setAlias(false);

                XMPAliasInfo info = XMPMetaFactory.getSchemaRegistry().findAlias(currProp.getName());
                if (info != null) {

                    XMPNode baseSchema = XMPNodeUtils.findSchemaNode(tree, info
                            .getNamespace(), null, true);
                    baseSchema.setImplicit(false);

                    XMPNode baseNode = XMPNodeUtils.findChildNode(baseSchema, info
                            .getPrefix() + info.getPropName(), false);
                    if (baseNode == null) {

                        if (info.getAliasForm().isSimple()) {

                            String qname = info.getPrefix() + info.getPropName();
                            currProp.setName(qname);
                            baseSchema.addChild(currProp);

                            propertyIt.remove();

                            continue;
                        }

                        baseNode = new XMPNode(info.getPrefix() + info.getPropName(), info.getAliasForm().toPropertyOptions());
                        baseSchema.addChild(baseNode);
                        transplantArrayItemAlias(propertyIt, currProp, baseNode);

                        continue;
                    }
                    if (info.getAliasForm().isSimple()) {

                        if (strictAliasing) {
                            compareAliasedSubtrees(currProp, baseNode, true);
                        }

                        propertyIt.remove();

                        continue;
                    }

                    XMPNode itemNode = null;
                    if (info.getAliasForm().isArrayAltText()) {

                        int xdIndex = XMPNodeUtils.lookupLanguageItem(baseNode, "x-default");

                        if (xdIndex != -1) {
                            itemNode = baseNode.getChild(xdIndex);
                        }
                    } else if (baseNode.hasChildren()) {

                        itemNode = baseNode.getChild(1);
                    }

                    if (itemNode == null) {

                        transplantArrayItemAlias(propertyIt, currProp, baseNode);

                        continue;
                    }
                    if (strictAliasing) {
                        compareAliasedSubtrees(currProp, itemNode, true);
                    }

                    propertyIt.remove();
                }
            }

            currSchema.setHasAliases(false);
        }
    }

    private static void transplantArrayItemAlias(Iterator propertyIt, XMPNode childNode, XMPNode baseArray) throws XMPException {
        if (baseArray.getOptions().isArrayAltText()) {

            if (childNode.getOptions().getHasLanguage()) {
                throw new XMPException("Alias to x-default already has a language qualifier", 203);
            }

            XMPNode langQual = new XMPNode("xml:lang", "x-default", null);
            childNode.addQualifier(langQual);
        }

        propertyIt.remove();
        childNode.setName("[]");
        baseArray.addChild(childNode);
    }

    private static void fixGPSTimeStamp(XMPNode exifSchema) throws XMPException {
        XMPNode gpsDateTime = XMPNodeUtils.findChildNode(exifSchema, "exif:GPSTimeStamp", false);
        if (gpsDateTime == null) {
            return;
        }

        try {
            XMPDateTime binGPSStamp = XMPUtils.convertToDate(gpsDateTime.getValue());
            if (binGPSStamp.getYear() != 0 || binGPSStamp
                    .getMonth() != 0 || binGPSStamp
                            .getDay() != 0) {
                return;
            }

            XMPNode otherDate = XMPNodeUtils.findChildNode(exifSchema, "exif:DateTimeOriginal", false);

            if (otherDate == null) {
                otherDate = XMPNodeUtils.findChildNode(exifSchema, "exif:DateTimeDigitized", false);
            }

            XMPDateTime binOtherDate = XMPUtils.convertToDate(otherDate.getValue());
            Calendar cal = binGPSStamp.getCalendar();
            cal.set(1, binOtherDate.getYear());
            cal.set(2, binOtherDate.getMonth());
            cal.set(5, binOtherDate.getDay());
            binGPSStamp = new XMPDateTimeImpl(cal);
            gpsDateTime.setValue(XMPUtils.convertFromDate(binGPSStamp));
        } catch (XMPException e) {
            return;
        }
    }

    private static void deleteEmptySchemas(XMPNode tree) {
        for (Iterator<XMPNode> it = tree.iterateChildren(); it.hasNext();) {

            XMPNode schema = it.next();
            if (!schema.hasChildren()) {
                it.remove();
            }
        }
    }

    private static void compareAliasedSubtrees(XMPNode aliasNode, XMPNode baseNode, boolean outerCall) throws XMPException {
        if (!aliasNode.getValue().equals(baseNode.getValue()) || aliasNode
                .getChildrenLength() != baseNode.getChildrenLength()) {
            throw new XMPException("Mismatch between alias and base nodes", 203);
        }

        if (!outerCall && (!aliasNode.getName().equals(baseNode.getName())
                || !aliasNode.getOptions().equals(baseNode.getOptions()) || aliasNode
                .getQualifierLength() != baseNode.getQualifierLength())) {

            throw new XMPException("Mismatch between alias and base nodes", 203);
        }

        Iterator<XMPNode> iterator1 = aliasNode.iterateChildren();
        Iterator<XMPNode> iterator2 = baseNode.iterateChildren();
        while (iterator1.hasNext() && iterator2.hasNext()) {

            XMPNode aliasChild = iterator1.next();
            XMPNode baseChild = iterator2.next();
            compareAliasedSubtrees(aliasChild, baseChild, false);
        }

        Iterator<XMPNode> an = aliasNode.iterateQualifier();
        Iterator<XMPNode> bn = baseNode.iterateQualifier();
        while (an.hasNext() && bn.hasNext()) {

            XMPNode aliasQual = an.next();
            XMPNode baseQual = bn.next();
            compareAliasedSubtrees(aliasQual, baseQual, false);
        }
    }

    private static void migrateAudioCopyright(XMPMeta xmp, XMPNode dmCopyright) {
        try {
            XMPNode dcSchema = XMPNodeUtils.findSchemaNode(((XMPMetaImpl) xmp)
                    .getRoot(), "http://purl.org/dc/elements/1.1/", true);

            String dmValue = dmCopyright.getValue();
            String doubleLF = "\n\n";

            XMPNode dcRightsArray = XMPNodeUtils.findChildNode(dcSchema, "dc:rights", false);

            if (dcRightsArray == null || !dcRightsArray.hasChildren()) {

                dmValue = doubleLF + dmValue;
                xmp.setLocalizedText("http://purl.org/dc/elements/1.1/", "rights", "", "x-default", dmValue, null);

            } else {

                int xdIndex = XMPNodeUtils.lookupLanguageItem(dcRightsArray, "x-default");

                if (xdIndex < 0) {

                    String firstValue = dcRightsArray.getChild(1).getValue();
                    xmp.setLocalizedText("http://purl.org/dc/elements/1.1/", "rights", "", "x-default", firstValue, null);

                    xdIndex = XMPNodeUtils.lookupLanguageItem(dcRightsArray, "x-default");
                }

                XMPNode defaultNode = dcRightsArray.getChild(xdIndex);
                String defaultValue = defaultNode.getValue();
                int lfPos = defaultValue.indexOf(doubleLF);

                if (lfPos < 0) {

                    if (!dmValue.equals(defaultValue)) {

                        defaultNode.setValue(defaultValue + doubleLF + dmValue);

                    }

                } else if (!defaultValue.substring(lfPos + 2).equals(dmValue)) {

                    defaultNode.setValue(defaultValue.substring(0, lfPos + 2) + dmValue);
                }
            }

            dmCopyright.getParent().removeChild(dmCopyright);
        } catch (XMPException xMPException) {
        }
    }

    private static void initDCArrays() {
        dcArrayForms = new HashMap<Object, Object>();

        PropertyOptions bagForm = new PropertyOptions();
        bagForm.setArray(true);
        dcArrayForms.put("dc:contributor", bagForm);
        dcArrayForms.put("dc:language", bagForm);
        dcArrayForms.put("dc:publisher", bagForm);
        dcArrayForms.put("dc:relation", bagForm);
        dcArrayForms.put("dc:subject", bagForm);
        dcArrayForms.put("dc:type", bagForm);

        PropertyOptions seqForm = new PropertyOptions();
        seqForm.setArray(true);
        seqForm.setArrayOrdered(true);
        dcArrayForms.put("dc:creator", seqForm);
        dcArrayForms.put("dc:date", seqForm);

        PropertyOptions altTextForm = new PropertyOptions();
        altTextForm.setArray(true);
        altTextForm.setArrayOrdered(true);
        altTextForm.setArrayAlternate(true);
        altTextForm.setArrayAltText(true);
        dcArrayForms.put("dc:description", altTextForm);
        dcArrayForms.put("dc:rights", altTextForm);
        dcArrayForms.put("dc:title", altTextForm);
    }
}
