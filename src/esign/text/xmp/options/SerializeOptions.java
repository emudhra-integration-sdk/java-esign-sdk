package esign.text.xmp.options;

import esign.text.xmp.XMPException;

public final class SerializeOptions
        extends Options {

    public static final int OMIT_PACKET_WRAPPER = 16;
    public static final int READONLY_PACKET = 32;
    public static final int USE_COMPACT_FORMAT = 64;
    public static final int USE_CANONICAL_FORMAT = 128;
    public static final int INCLUDE_THUMBNAIL_PAD = 256;
    public static final int EXACT_PACKET_LENGTH = 512;
    public static final int OMIT_XMPMETA_ELEMENT = 4096;
    public static final int SORT = 8192;
    private static final int LITTLEENDIAN_BIT = 1;
    private static final int UTF16_BIT = 2;
    public static final int ENCODE_UTF8 = 0;
    public static final int ENCODE_UTF16BE = 2;
    public static final int ENCODE_UTF16LE = 3;
    private static final int ENCODING_MASK = 3;
    private int padding = 2048;

    private String newline = "\n";

    private String indent = "  ";

    private int baseIndent = 0;

    private boolean omitVersionAttribute = false;

    public SerializeOptions() {
    }

    public SerializeOptions(int options) throws XMPException {
        super(options);
    }

    public boolean getOmitPacketWrapper() {
        return getOption(16);
    }

    public SerializeOptions setOmitPacketWrapper(boolean value) {
        setOption(16, value);
        return this;
    }

    public boolean getOmitXmpMetaElement() {
        return getOption(4096);
    }

    public SerializeOptions setOmitXmpMetaElement(boolean value) {
        setOption(4096, value);
        return this;
    }

    public boolean getReadOnlyPacket() {
        return getOption(32);
    }

    public SerializeOptions setReadOnlyPacket(boolean value) {
        setOption(32, value);
        return this;
    }

    public boolean getUseCompactFormat() {
        return getOption(64);
    }

    public SerializeOptions setUseCompactFormat(boolean value) {
        setOption(64, value);
        return this;
    }

    public boolean getUseCanonicalFormat() {
        return getOption(128);
    }

    public SerializeOptions setUseCanonicalFormat(boolean value) {
        setOption(128, value);
        return this;
    }

    public boolean getIncludeThumbnailPad() {
        return getOption(256);
    }

    public SerializeOptions setIncludeThumbnailPad(boolean value) {
        setOption(256, value);
        return this;
    }

    public boolean getExactPacketLength() {
        return getOption(512);
    }

    public SerializeOptions setExactPacketLength(boolean value) {
        setOption(512, value);
        return this;
    }

    public boolean getSort() {
        return getOption(8192);
    }

    public SerializeOptions setSort(boolean value) {
        setOption(8192, value);
        return this;
    }

    public boolean getEncodeUTF16BE() {
        return ((getOptions() & 0x3) == 2);
    }

    public SerializeOptions setEncodeUTF16BE(boolean value) {
        setOption(3, false);
        setOption(2, value);
        return this;
    }

    public boolean getEncodeUTF16LE() {
        return ((getOptions() & 0x3) == 3);
    }

    public SerializeOptions setEncodeUTF16LE(boolean value) {
        setOption(3, false);
        setOption(3, value);
        return this;
    }

    public int getBaseIndent() {
        return this.baseIndent;
    }

    public SerializeOptions setBaseIndent(int baseIndent) {
        this.baseIndent = baseIndent;
        return this;
    }

    public String getIndent() {
        return this.indent;
    }

    public SerializeOptions setIndent(String indent) {
        this.indent = indent;
        return this;
    }

    public String getNewline() {
        return this.newline;
    }

    public SerializeOptions setNewline(String newline) {
        this.newline = newline;
        return this;
    }

    public int getPadding() {
        return this.padding;
    }

    public SerializeOptions setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public boolean getOmitVersionAttribute() {
        return this.omitVersionAttribute;
    }

    public String getEncoding() {
        if (getEncodeUTF16BE()) {
            return "UTF-16BE";
        }
        if (getEncodeUTF16LE()) {
            return "UTF-16LE";
        }

        return "UTF-8";
    }

    public Object clone() throws CloneNotSupportedException {
        try {
            SerializeOptions clone = new SerializeOptions(getOptions());
            clone.setBaseIndent(this.baseIndent);
            clone.setIndent(this.indent);
            clone.setNewline(this.newline);
            clone.setPadding(this.padding);
            return clone;
        } catch (XMPException e) {

            return null;
        }
    }

    protected String defineOptionName(int option) {
        switch (option) {
            case 16:
                return "OMIT_PACKET_WRAPPER";
            case 32:
                return "READONLY_PACKET";
            case 64:
                return "USE_COMPACT_FORMAT";
            case 256:
                return "INCLUDE_THUMBNAIL_PAD";
            case 512:
                return "EXACT_PACKET_LENGTH";
            case 4096:
                return "OMIT_XMPMETA_ELEMENT";
            case 8192:
                return "NORMALIZED";
        }
        return null;
    }

    protected int getValidOptions() {
        return 13168;
    }
}
