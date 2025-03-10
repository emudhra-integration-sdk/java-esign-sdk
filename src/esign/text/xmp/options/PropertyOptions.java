package esign.text.xmp.options;

import esign.text.xmp.XMPException;

public final class PropertyOptions
        extends Options {

    public static final int NO_OPTIONS = 0;
    public static final int URI = 2;
    public static final int HAS_QUALIFIERS = 16;
    public static final int QUALIFIER = 32;
    public static final int HAS_LANGUAGE = 64;
    public static final int HAS_TYPE = 128;
    public static final int STRUCT = 256;
    public static final int ARRAY = 512;
    public static final int ARRAY_ORDERED = 1024;
    public static final int ARRAY_ALTERNATE = 2048;
    public static final int ARRAY_ALT_TEXT = 4096;
    public static final int SCHEMA_NODE = -2147483648;
    public static final int DELETE_EXISTING = 536870912;
    public static final int SEPARATE_NODE = 1073741824;

    public PropertyOptions() {
    }

    public PropertyOptions(int options) throws XMPException {
        super(options);
    }

    public boolean isURI() {
        return getOption(2);
    }

    public PropertyOptions setURI(boolean value) {
        setOption(2, value);
        return this;
    }

    public boolean getHasQualifiers() {
        return getOption(16);
    }

    public PropertyOptions setHasQualifiers(boolean value) {
        setOption(16, value);
        return this;
    }

    public boolean isQualifier() {
        return getOption(32);
    }

    public PropertyOptions setQualifier(boolean value) {
        setOption(32, value);
        return this;
    }

    public boolean getHasLanguage() {
        return getOption(64);
    }

    public PropertyOptions setHasLanguage(boolean value) {
        setOption(64, value);
        return this;
    }

    public boolean getHasType() {
        return getOption(128);
    }

    public PropertyOptions setHasType(boolean value) {
        setOption(128, value);
        return this;
    }

    public boolean isStruct() {
        return getOption(256);
    }

    public PropertyOptions setStruct(boolean value) {
        setOption(256, value);
        return this;
    }

    public boolean isArray() {
        return getOption(512);
    }

    public PropertyOptions setArray(boolean value) {
        setOption(512, value);
        return this;
    }

    public boolean isArrayOrdered() {
        return getOption(1024);
    }

    public PropertyOptions setArrayOrdered(boolean value) {
        setOption(1024, value);
        return this;
    }

    public boolean isArrayAlternate() {
        return getOption(2048);
    }

    public PropertyOptions setArrayAlternate(boolean value) {
        setOption(2048, value);
        return this;
    }

    public boolean isArrayAltText() {
        return getOption(4096);
    }

    public PropertyOptions setArrayAltText(boolean value) {
        setOption(4096, value);
        return this;
    }

    public boolean isSchemaNode() {
        return getOption(-2147483648);
    }

    public PropertyOptions setSchemaNode(boolean value) {
        setOption(-2147483648, value);
        return this;
    }

    public boolean isCompositeProperty() {
        return ((getOptions() & 0x300) > 0);
    }

    public boolean isSimple() {
        return ((getOptions() & 0x300) == 0);
    }

    public boolean equalArrayTypes(PropertyOptions options) {
        return (isArray() == options.isArray()
                && isArrayOrdered() == options.isArrayOrdered()
                && isArrayAlternate() == options.isArrayAlternate()
                && isArrayAltText() == options.isArrayAltText());
    }

    public void mergeWith(PropertyOptions options) throws XMPException {
        if (options != null) {
            setOptions(getOptions() | options.getOptions());
        }
    }

    public boolean isOnlyArrayOptions() {
        return ((getOptions() & 0xFFFFE1FF) == 0);
    }

    protected int getValidOptions() {
        return -1073733646;
    }

    protected String defineOptionName(int option) {
        switch (option) {
            case 2:
                return "URI";
            case 16:
                return "HAS_QUALIFIER";
            case 32:
                return "QUALIFIER";
            case 64:
                return "HAS_LANGUAGE";
            case 128:
                return "HAS_TYPE";
            case 256:
                return "STRUCT";
            case 512:
                return "ARRAY";
            case 1024:
                return "ARRAY_ORDERED";
            case 2048:
                return "ARRAY_ALTERNATE";
            case 4096:
                return "ARRAY_ALT_TEXT";
            case -2147483648:
                return "SCHEMA_NODE";
        }
        return null;
    }

    public void assertConsistency(int options) throws XMPException {
        if ((options & 0x100) > 0 && (options & 0x200) > 0) {
            throw new XMPException("IsStruct and IsArray options are mutually exclusive", 103);
        }

        if ((options & 0x2) > 0 && (options & 0x300) > 0) {
            throw new XMPException("Structs and arrays can't have \"value\" options", 103);
        }
    }
}
