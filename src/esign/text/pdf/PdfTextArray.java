package esign.text.pdf;

import java.util.ArrayList;

public class PdfTextArray {

    ArrayList<Object> arrayList = new ArrayList();

    private String lastStr;

    private Float lastNum;

    public PdfTextArray(String str) {
        add(str);
    }

    public PdfTextArray() {
    }

    public void add(PdfNumber number) {
        add((float) number.doubleValue());
    }

    public void add(float number) {
        if (number != 0.0F) {
            if (this.lastNum != null) {
                this.lastNum = new Float(number + this.lastNum.floatValue());
                if (this.lastNum.floatValue() != 0.0F) {
                    replaceLast(this.lastNum);
                } else {
                    this.arrayList.remove(this.arrayList.size() - 1);
                }
            } else {
                this.lastNum = new Float(number);
                this.arrayList.add(this.lastNum);
            }

            this.lastStr = null;
        }
    }

    public void add(String str) {
        if (str.length() > 0) {
            if (this.lastStr != null) {
                this.lastStr += str;
                replaceLast(this.lastStr);
            } else {
                this.lastStr = str;
                this.arrayList.add(this.lastStr);
            }
            this.lastNum = null;
        }
    }

    ArrayList<Object> getArrayList() {
        return this.arrayList;
    }

    private void replaceLast(Object obj) {
        this.arrayList.set(this.arrayList.size() - 1, obj);
    }
}
