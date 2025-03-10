package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.util.ArrayList;

public class PdfContentParser {

    public static final int COMMAND_TYPE = 200;
    private PRTokeniser tokeniser;

    public PdfContentParser(PRTokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }

    public ArrayList<PdfObject> parse(ArrayList<PdfObject> ls) throws IOException {
        if (ls == null) {
            ls = new ArrayList<PdfObject>();
        } else {
            ls.clear();
        }
        PdfObject ob = null;
        while ((ob = readPRObject()) != null) {
            ls.add(ob);
            if (ob.type() == 200) {
                break;
            }
        }
        return ls;
    }

    public PRTokeniser getTokeniser() {
        return this.tokeniser;
    }

    public void setTokeniser(PRTokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }

    public PdfDictionary readDictionary() throws IOException {
        PdfDictionary dic = new PdfDictionary();
        while (true) {
            if (!nextValidToken()) {
                throw new IOException(MessageLocalization.getComposedMessage("unexpected.end.of.file", new Object[0]));
            }
            if (this.tokeniser.getTokenType() == PRTokeniser.TokenType.END_DIC) {
                break;
            }
            if (this.tokeniser.getTokenType() == PRTokeniser.TokenType.OTHER && "def".equals(this.tokeniser.getStringValue())) {
                continue;
            }
            if (this.tokeniser.getTokenType() != PRTokeniser.TokenType.NAME) {
                throw new IOException(MessageLocalization.getComposedMessage("dictionary.key.1.is.not.a.name", new Object[]{this.tokeniser.getStringValue()}));
            }
            PdfName name = new PdfName(this.tokeniser.getStringValue(), false);
            PdfObject obj = readPRObject();
            int type = obj.type();
            if (-type == PRTokeniser.TokenType.END_DIC.ordinal()) {
                throw new IOException(MessageLocalization.getComposedMessage("unexpected.gt.gt", new Object[0]));
            }
            if (-type == PRTokeniser.TokenType.END_ARRAY.ordinal()) {
                throw new IOException(MessageLocalization.getComposedMessage("unexpected.close.bracket", new Object[0]));
            }
            dic.put(name, obj);
        }
        return dic;
    }

    public PdfArray readArray() throws IOException {
        PdfArray array = new PdfArray();
        while (true) {
            PdfObject obj = readPRObject();
            int type = obj.type();
            if (-type == PRTokeniser.TokenType.END_ARRAY.ordinal()) {
                break;
            }
            if (-type == PRTokeniser.TokenType.END_DIC.ordinal()) {
                throw new IOException(MessageLocalization.getComposedMessage("unexpected.gt.gt", new Object[0]));
            }
            array.add(obj);
        }
        return array;
    }

    public PdfObject readPRObject() throws IOException {
        PdfDictionary dic;
        PdfString str;
        if (!nextValidToken()) {
            return null;
        }
        PRTokeniser.TokenType type = this.tokeniser.getTokenType();
        switch (type) {
            case START_DIC:
                dic = readDictionary();
                return dic;

            case START_ARRAY:
                return readArray();
            case STRING:
                str = (new PdfString(this.tokeniser.getStringValue(), null)).setHexWriting(this.tokeniser.isHexString());
                return str;
            case NAME:
                return new PdfName(this.tokeniser.getStringValue(), false);
            case NUMBER:
                return new PdfNumber(this.tokeniser.getStringValue());
            case OTHER:
                return new PdfLiteral(200, this.tokeniser.getStringValue());
        }
        return new PdfLiteral(-type.ordinal(), this.tokeniser.getStringValue());
    }

    public boolean nextValidToken() throws IOException {
        while (this.tokeniser.nextToken()) {
            if (this.tokeniser.getTokenType() == PRTokeniser.TokenType.COMMENT) {
                continue;
            }
            return true;
        }
        return false;
    }
}
