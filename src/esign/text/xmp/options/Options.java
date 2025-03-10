package esign.text.xmp.options;

import esign.text.xmp.XMPException;
import java.util.HashMap;
import java.util.Map;

public abstract class Options {

    private int options = 0;

    private Map optionNames = null;

    public Options() {
    }

    public Options(int options) throws XMPException {
        assertOptionsValid(options);
        setOptions(options);
    }

    public void clear() {
        this.options = 0;
    }

    public boolean isExactly(int optionBits) {
        return (getOptions() == optionBits);
    }

    public boolean containsAllOptions(int optionBits) {
        return ((getOptions() & optionBits) == optionBits);
    }

    public boolean containsOneOf(int optionBits) {
        return ((getOptions() & optionBits) != 0);
    }

    protected boolean getOption(int optionBit) {
        return ((this.options & optionBit) != 0);
    }

    public void setOption(int optionBits, boolean value) {
        this.options = value ? (this.options | optionBits) : (this.options & (optionBits ^ 0xFFFFFFFF));
    }

    public int getOptions() {
        return this.options;
    }

    public void setOptions(int options) throws XMPException {
        assertOptionsValid(options);
        this.options = options;
    }

    public boolean equals(Object obj) {
        return (getOptions() == ((Options) obj).getOptions());
    }

    public int hashCode() {
        return getOptions();
    }

    public String getOptionsString() {
        if (this.options != 0) {

            StringBuffer sb = new StringBuffer();
            int theBits = this.options;
            while (theBits != 0) {

                int oneLessBit = theBits & theBits - 1;
                int singleBit = theBits ^ oneLessBit;
                String bitName = getOptionName(singleBit);
                sb.append(bitName);
                if (oneLessBit != 0) {
                    sb.append(" | ");
                }
                theBits = oneLessBit;
            }
            return sb.toString();
        }

        return "<none>";
    }

    public String toString() {
        return "0x" + Integer.toHexString(this.options);
    }

    protected abstract int getValidOptions();

    protected abstract String defineOptionName(int paramInt);

    protected void assertConsistency(int options) throws XMPException {
    }

    private void assertOptionsValid(int options) throws XMPException {
        int invalidOptions = options & (getValidOptions() ^ 0xFFFFFFFF);
        if (invalidOptions == 0) {

            assertConsistency(options);
        } else {

            throw new XMPException("The option bit(s) 0x" + Integer.toHexString(invalidOptions) + " are invalid!", 103);
        }
    }

    private String getOptionName(int option) {
        Map<Integer, String> optionsNames = procureOptionNames();

        Integer key = new Integer(option);
        String result = (String) optionsNames.get(key);
        if (result == null) {

            result = defineOptionName(option);
            if (result != null) {

                optionsNames.put(key, result);
            } else {

                result = "<option name not defined>";
            }
        }

        return result;
    }

    private Map procureOptionNames() {
        if (this.optionNames == null) {
            this.optionNames = new HashMap<Object, Object>();
        }
        return this.optionNames;
    }
}
