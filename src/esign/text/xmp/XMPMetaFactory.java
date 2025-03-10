package esign.text.xmp;

import esign.text.xmp.impl.XMPMetaImpl;
import esign.text.xmp.impl.XMPMetaParser;
import esign.text.xmp.impl.XMPSchemaRegistryImpl;
import esign.text.xmp.impl.XMPSerializerHelper;
import esign.text.xmp.options.ParseOptions;
import esign.text.xmp.options.SerializeOptions;
import java.io.InputStream;
import java.io.OutputStream;

public final class XMPMetaFactory {

    private static XMPSchemaRegistry schema = (XMPSchemaRegistry) new XMPSchemaRegistryImpl();

    private static XMPVersionInfo versionInfo = null;

    public static XMPSchemaRegistry getSchemaRegistry() {
        return schema;
    }

    public static XMPMeta create() {
        return (XMPMeta) new XMPMetaImpl();
    }

    public static XMPMeta parse(InputStream in) throws XMPException {
        return parse(in, null);
    }

    public static XMPMeta parse(InputStream in, ParseOptions options) throws XMPException {
        return XMPMetaParser.parse(in, options);
    }

    public static XMPMeta parseFromString(String packet) throws XMPException {
        return parseFromString(packet, null);
    }

    public static XMPMeta parseFromString(String packet, ParseOptions options) throws XMPException {
        return XMPMetaParser.parse(packet, options);
    }

    public static XMPMeta parseFromBuffer(byte[] buffer) throws XMPException {
        return parseFromBuffer(buffer, null);
    }

    public static XMPMeta parseFromBuffer(byte[] buffer, ParseOptions options) throws XMPException {
        return XMPMetaParser.parse(buffer, options);
    }

    public static void serialize(XMPMeta xmp, OutputStream out) throws XMPException {
        serialize(xmp, out, null);
    }

    public static void serialize(XMPMeta xmp, OutputStream out, SerializeOptions options) throws XMPException {
        assertImplementation(xmp);
        XMPSerializerHelper.serialize((XMPMetaImpl) xmp, out, options);
    }

    public static byte[] serializeToBuffer(XMPMeta xmp, SerializeOptions options) throws XMPException {
        assertImplementation(xmp);
        return XMPSerializerHelper.serializeToBuffer((XMPMetaImpl) xmp, options);
    }

    public static String serializeToString(XMPMeta xmp, SerializeOptions options) throws XMPException {
        assertImplementation(xmp);
        return XMPSerializerHelper.serializeToString((XMPMetaImpl) xmp, options);
    }

    private static void assertImplementation(XMPMeta xmp) {
        if (!(xmp instanceof XMPMetaImpl)) {
            throw new UnsupportedOperationException("The serializing service works onlywith the XMPMeta implementation of this library");
        }
    }

    public static void reset() {
        schema = (XMPSchemaRegistry) new XMPSchemaRegistryImpl();
    }

    public static synchronized XMPVersionInfo getVersionInfo() {
        if (versionInfo == null) {
            try {
                int major = 5;
                int minor = 1;
                int micro = 0;
                int engBuild = 3;
                boolean debug = false;

                String message = "Adobe XMP Core 5.1.0-jc003";

                versionInfo = new XMPVersionInfo() {
                    public int getMajor() {
                        return 5;
                    }

                    public int getMinor() {
                        return 1;
                    }

                    public int getMicro() {
                        return 0;
                    }

                    public boolean isDebug() {
                        return false;
                    }

                    public int getBuild() {
                        return 3;
                    }

                    public String getMessage() {
                        return "Adobe XMP Core 5.1.0-jc003";
                    }

                    public String toString() {
                        return "Adobe XMP Core 5.1.0-jc003";
                    }
                };
            } catch (Throwable e) {

                System.out.println(e);
            }
        }
        return versionInfo;
    }
}
