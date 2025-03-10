package esign.text.xml.xmp;

import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMeta;
import esign.text.xmp.XMPUtils;
import esign.text.xmp.options.PropertyOptions;

public class XmpBasicProperties {

    public static final String ADVISORY = "Advisory";
    public static final String BASEURL = "BaseURL";
    public static final String CREATEDATE = "CreateDate";
    public static final String CREATORTOOL = "CreatorTool";
    public static final String IDENTIFIER = "Identifier";
    public static final String METADATADATE = "MetadataDate";
    public static final String MODIFYDATE = "ModifyDate";
    public static final String NICKNAME = "Nickname";
    public static final String THUMBNAILS = "Thumbnails";

    public static void setCreatorTool(XMPMeta xmpMeta, String creator) throws XMPException {
        xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "CreatorTool", creator);
    }

    public static void setCreateDate(XMPMeta xmpMeta, String date) throws XMPException {
        xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "CreateDate", date);
    }

    public static void setModDate(XMPMeta xmpMeta, String date) throws XMPException {
        xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "ModifyDate", date);
    }

    public static void setMetaDataDate(XMPMeta xmpMeta, String date) throws XMPException {
        xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "MetadataDate", date);
    }

    public static void setIdentifiers(XMPMeta xmpMeta, String[] id) throws XMPException {
        XMPUtils.removeProperties(xmpMeta, "http://purl.org/dc/elements/1.1/", "Identifier", true, true);
        for (int i = 0; i < id.length; i++) {
            xmpMeta.appendArrayItem("http://purl.org/dc/elements/1.1/", "Identifier", new PropertyOptions(512), id[i], null);
        }
    }

    public static void setNickname(XMPMeta xmpMeta, String name) throws XMPException {
        xmpMeta.setProperty("http://ns.adobe.com/xap/1.0/", "Nickname", name);
    }
}
