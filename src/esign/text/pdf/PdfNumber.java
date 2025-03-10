package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;

public class PdfNumber
        extends PdfObject {

    private double value;

    public PdfNumber(String content) {
        super(2);
        try {
            this.value = Double.parseDouble(content.trim());
            setContent(content);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("1.is.not.a.valid.number.2", new Object[]{content, nfe.toString()}));
        }
    }

    public PdfNumber(int value) {
        super(2);
        this.value = value;
        setContent(String.valueOf(value));
    }

    public PdfNumber(long value) {
        super(2);
        this.value = value;
        setContent(String.valueOf(value));
    }

    public PdfNumber(double value) {
        super(2);
        this.value = value;
        setContent(ByteBuffer.formatDouble(value));
    }

    public PdfNumber(float value) {
        this((double)value);
    }

    public int intValue() {
        return (int) this.value;
    }

    public long longValue() {
        return (long) this.value;
    }

    public double doubleValue() {
        return this.value;
    }

    public float floatValue() {
        return (float) this.value;
    }

    public void increment() {
        this.value++;
        setContent(ByteBuffer.formatDouble(this.value));
    }
}

