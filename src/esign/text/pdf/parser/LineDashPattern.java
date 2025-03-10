package esign.text.pdf.parser;

import esign.text.pdf.PdfArray;

public class LineDashPattern {

    private PdfArray dashArray;
    private float dashPhase;
    private int currentIndex;
    private int elemOrdinalNumber = 1;

    private DashArrayElem currentElem;

    public LineDashPattern(PdfArray dashArray, float dashPhase) {
        this.dashArray = new PdfArray(dashArray);
        this.dashPhase = dashPhase;
        initFirst(dashPhase);
    }

    public PdfArray getDashArray() {
        return this.dashArray;
    }

    public void setDashArray(PdfArray dashArray) {
        this.dashArray = dashArray;
    }

    public float getDashPhase() {
        return this.dashPhase;
    }

    public void setDashPhase(float dashPhase) {
        this.dashPhase = dashPhase;
    }

    public DashArrayElem next() {
        DashArrayElem ret = this.currentElem;

        if (this.dashArray.size() > 0) {
            this.currentIndex = (this.currentIndex + 1) % this.dashArray.size();
            this.currentElem = new DashArrayElem(this.dashArray.getAsNumber(this.currentIndex).floatValue(), isEven(++this.elemOrdinalNumber));
        }

        return ret;
    }

    public void reset() {
        this.currentIndex = 0;
        this.elemOrdinalNumber = 1;
        initFirst(this.dashPhase);
    }

    public boolean isSolid() {
        if (this.dashArray.size() % 2 != 0) {
            return false;
        }

        float unitsOffSum = 0.0F;

        for (int i = 1; i < this.dashArray.size(); i += 2) {
            unitsOffSum += this.dashArray.getAsNumber(i).floatValue();
        }

        return (Float.compare(unitsOffSum, 0.0F) == 0);
    }

    private void initFirst(float phase) {
        if (this.dashArray.size() > 0) {
            while (phase > 0.0F) {
                phase -= this.dashArray.getAsNumber(this.currentIndex).floatValue();
                this.currentIndex = (this.currentIndex + 1) % this.dashArray.size();
                this.elemOrdinalNumber++;
            }

            if (phase < 0.0F) {
                this.elemOrdinalNumber--;
                this.currentIndex--;
                this.currentElem = new DashArrayElem(-phase, isEven(this.elemOrdinalNumber));
            } else {
                this.currentElem = new DashArrayElem(this.dashArray.getAsNumber(this.currentIndex).floatValue(), isEven(this.elemOrdinalNumber));
            }
        }
    }

    private boolean isEven(int num) {
        return (num % 2 == 0);
    }

    public class DashArrayElem {

        private float val;
        private boolean isGap;

        public DashArrayElem(float val, boolean isGap) {
            this.val = val;
            this.isGap = isGap;
        }

        public float getVal() {
            return this.val;
        }

        public void setVal(float val) {
            this.val = val;
        }

        public boolean isGap() {
            return this.isGap;
        }

        public void setGap(boolean isGap) {
            this.isGap = isGap;
        }
    }
}
