package esign.text.xmp.impl.xpath;

import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.impl.Utils;
import esign.text.xmp.properties.XMPAliasInfo;

public final class XMPPathParser {

    public static XMPPath expandXPath(String schemaNS, String path) throws XMPException {
        if (schemaNS == null || path == null) {
            throw new XMPException("Parameter must not be null", 4);
        }

        XMPPath expandedXPath = new XMPPath();
        PathPosition pos = new PathPosition();
        pos.path = path;

        parseRootNode(schemaNS, pos, expandedXPath);

        while (pos.stepEnd < path.length()) {
            XMPPathSegment segment;
            pos.stepBegin = pos.stepEnd;

            skipPathDelimiter(path, pos);

            pos.stepEnd = pos.stepBegin;

            if (path.charAt(pos.stepBegin) != '[') {

                segment = parseStructSegment(pos);

            } else {

                segment = parseIndexSegment(pos);
            }

            if (segment.getKind() == 1) {

                if (segment.getName().charAt(0) == '@') {

                    segment.setName("?" + segment.getName().substring(1));
                    if (!"?xml:lang".equals(segment.getName())) {
                        throw new XMPException("Only xml:lang allowed with '@'", 102);
                    }
                }

                if (segment.getName().charAt(0) == '?') {

                    pos.nameStart++;
                    segment.setKind(2);
                }

                verifyQualName(pos.path.substring(pos.nameStart, pos.nameEnd));
            } else if (segment.getKind() == 6) {

                if (segment.getName().charAt(1) == '@') {

                    segment.setName("[?" + segment.getName().substring(2));
                    if (!segment.getName().startsWith("[?xml:lang=")) {
                        throw new XMPException("Only xml:lang allowed with '@'", 102);
                    }
                }

                if (segment.getName().charAt(1) == '?') {

                    pos.nameStart++;
                    segment.setKind(5);
                    verifyQualName(pos.path.substring(pos.nameStart, pos.nameEnd));
                }
            }

            expandedXPath.add(segment);
        }
        return expandedXPath;
    }

    private static void skipPathDelimiter(String path, PathPosition pos) throws XMPException {
        if (path.charAt(pos.stepBegin) == '/') {

            pos.stepBegin++;

            if (pos.stepBegin >= path.length()) {
                throw new XMPException("Empty XMPPath segment", 102);
            }
        }

        if (path.charAt(pos.stepBegin) == '*') {

            pos.stepBegin++;
            if (pos.stepBegin >= path.length() || path.charAt(pos.stepBegin) != '[') {
                throw new XMPException("Missing '[' after '*'", 102);
            }
        }
    }

    private static XMPPathSegment parseStructSegment(PathPosition pos) throws XMPException {
        pos.nameStart = pos.stepBegin;
        while (pos.stepEnd < pos.path.length() && "/[*".indexOf(pos.path.charAt(pos.stepEnd)) < 0) {
            pos.stepEnd++;
        }
        pos.nameEnd = pos.stepEnd;

        if (pos.stepEnd == pos.stepBegin) {
            throw new XMPException("Empty XMPPath segment", 102);
        }

        XMPPathSegment segment = new XMPPathSegment(pos.path.substring(pos.stepBegin, pos.stepEnd), 1);

        return segment;
    }

    private static XMPPathSegment parseIndexSegment(PathPosition pos) throws XMPException {
        XMPPathSegment segment;
        pos.stepEnd++;

        if ('0' <= pos.path.charAt(pos.stepEnd) && pos.path.charAt(pos.stepEnd) <= '9') {

            while (pos.stepEnd < pos.path.length() && '0' <= pos.path.charAt(pos.stepEnd) && pos.path
                    .charAt(pos.stepEnd) <= '9') {
                pos.stepEnd++;
            }

            segment = new XMPPathSegment(null, 3);

        } else {

            while (pos.stepEnd < pos.path.length() && pos.path.charAt(pos.stepEnd) != ']' && pos.path
                    .charAt(pos.stepEnd) != '=') {
                pos.stepEnd++;
            }

            if (pos.stepEnd >= pos.path.length()) {
                throw new XMPException("Missing ']' or '=' for array index", 102);
            }

            if (pos.path.charAt(pos.stepEnd) == ']') {

                if (!"[last()".equals(pos.path.substring(pos.stepBegin, pos.stepEnd))) {
                    throw new XMPException("Invalid non-numeric array index", 102);
                }

                segment = new XMPPathSegment(null, 4);
            } else {

                pos.nameStart = pos.stepBegin + 1;
                pos.nameEnd = pos.stepEnd;
                pos.stepEnd++;
                char quote = pos.path.charAt(pos.stepEnd);
                if (quote != '\'' && quote != '"') {
                    throw new XMPException("Invalid quote in array selector", 102);
                }

                pos.stepEnd++;
                while (pos.stepEnd < pos.path.length()) {

                    if (pos.path.charAt(pos.stepEnd) == quote) {

                        if (pos.stepEnd + 1 >= pos.path.length() || pos.path
                                .charAt(pos.stepEnd + 1) != quote) {
                            break;
                        }

                        pos.stepEnd++;
                    }
                    pos.stepEnd++;
                }

                if (pos.stepEnd >= pos.path.length()) {
                    throw new XMPException("No terminating quote for array selector", 102);
                }

                pos.stepEnd++;

                segment = new XMPPathSegment(null, 6);
            }
        }

        if (pos.stepEnd >= pos.path.length() || pos.path.charAt(pos.stepEnd) != ']') {
            throw new XMPException("Missing ']' for array index", 102);
        }
        pos.stepEnd++;
        segment.setName(pos.path.substring(pos.stepBegin, pos.stepEnd));

        return segment;
    }

    private static void parseRootNode(String schemaNS, PathPosition pos, XMPPath expandedXPath) throws XMPException {
        while (pos.stepEnd < pos.path.length() && "/[*".indexOf(pos.path.charAt(pos.stepEnd)) < 0) {
            pos.stepEnd++;
        }

        if (pos.stepEnd == pos.stepBegin) {
            throw new XMPException("Empty initial XMPPath step", 102);
        }

        String rootProp = verifyXPathRoot(schemaNS, pos.path.substring(pos.stepBegin, pos.stepEnd));
        XMPAliasInfo aliasInfo = XMPMetaFactory.getSchemaRegistry().findAlias(rootProp);
        if (aliasInfo == null) {

            expandedXPath.add(new XMPPathSegment(schemaNS, -2147483648));
            XMPPathSegment rootStep = new XMPPathSegment(rootProp, 1);
            expandedXPath.add(rootStep);

        } else {

            expandedXPath.add(new XMPPathSegment(aliasInfo.getNamespace(), -2147483648));
            XMPPathSegment rootStep = new XMPPathSegment(verifyXPathRoot(aliasInfo.getNamespace(), aliasInfo
                    .getPropName()), 1);

            rootStep.setAlias(true);
            rootStep.setAliasForm(aliasInfo.getAliasForm().getOptions());
            expandedXPath.add(rootStep);

            if (aliasInfo.getAliasForm().isArrayAltText()) {

                XMPPathSegment qualSelectorStep = new XMPPathSegment("[?xml:lang='x-default']", 5);

                qualSelectorStep.setAlias(true);
                qualSelectorStep.setAliasForm(aliasInfo.getAliasForm().getOptions());
                expandedXPath.add(qualSelectorStep);
            } else if (aliasInfo.getAliasForm().isArray()) {

                XMPPathSegment indexStep = new XMPPathSegment("[1]", 3);

                indexStep.setAlias(true);
                indexStep.setAliasForm(aliasInfo.getAliasForm().getOptions());
                expandedXPath.add(indexStep);
            }
        }
    }

    private static void verifyQualName(String qualName) throws XMPException {
        int colonPos = qualName.indexOf(':');
        if (colonPos > 0) {

            String prefix = qualName.substring(0, colonPos);
            if (Utils.isXMLNameNS(prefix)) {

                String regURI = XMPMetaFactory.getSchemaRegistry().getNamespaceURI(prefix);

                if (regURI != null) {
                    return;
                }

                throw new XMPException("Unknown namespace prefix for qualified name", 102);
            }
        }

        throw new XMPException("Ill-formed qualified name", 102);
    }

    private static void verifySimpleXMLName(String name) throws XMPException {
        if (!Utils.isXMLName(name)) {
            throw new XMPException("Bad XML name", 102);
        }
    }

    private static String verifyXPathRoot(String schemaNS, String rootProp) throws XMPException {
        if (schemaNS == null || schemaNS.length() == 0) {
            throw new XMPException("Schema namespace URI is required", 101);
        }

        if (rootProp.charAt(0) == '?' || rootProp.charAt(0) == '@') {
            throw new XMPException("Top level name must not be a qualifier", 102);
        }

        if (rootProp.indexOf('/') >= 0 || rootProp.indexOf('[') >= 0) {
            throw new XMPException("Top level name must be simple", 102);
        }

        String prefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(schemaNS);
        if (prefix == null) {
            throw new XMPException("Unregistered schema namespace URI", 101);
        }

        int colonPos = rootProp.indexOf(':');
        if (colonPos < 0) {

            verifySimpleXMLName(rootProp);
            return prefix + rootProp;
        }

        verifySimpleXMLName(rootProp.substring(0, colonPos));
        verifySimpleXMLName(rootProp.substring(colonPos));

        prefix = rootProp.substring(0, colonPos + 1);

        String regPrefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(schemaNS);
        if (regPrefix == null) {
            throw new XMPException("Unknown schema namespace prefix", 101);
        }
        if (!prefix.equals(regPrefix)) {
            throw new XMPException("Schema namespace URI and prefix mismatch", 101);
        }

        return rootProp;
    }
}
