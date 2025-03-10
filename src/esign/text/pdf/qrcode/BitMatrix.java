package esign.text.pdf.qrcode;

public final class BitMatrix {

    public final int width;
    public final int height;
    public final int rowSize;
    public final int[] bits;

    public BitMatrix(int dimension) {
        this(dimension, dimension);
    }

    public BitMatrix(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Both dimensions must be greater than 0");
        }
        this.width = width;
        this.height = height;
        int rowSize = width >> 5;
        if ((width & 0x1F) != 0) {
            rowSize++;
        }
        this.rowSize = rowSize;
        this.bits = new int[rowSize * height];
    }

    public boolean get(int x, int y) {
        int offset = y * this.rowSize + (x >> 5);
        return ((this.bits[offset] >>> (x & 0x1F) & 0x1) != 0);
    }

    public void set(int x, int y) {
        int offset = y * this.rowSize + (x >> 5);
        this.bits[offset] = this.bits[offset] | 1 << (x & 0x1F);
    }

    public void flip(int x, int y) {
        int offset = y * this.rowSize + (x >> 5);
        this.bits[offset] = this.bits[offset] ^ 1 << (x & 0x1F);
    }

    public void clear() {
        int max = this.bits.length;
        for (int i = 0; i < max; i++) {
            this.bits[i] = 0;
        }
    }

    public void setRegion(int left, int top, int width, int height) {
        if (top < 0 || left < 0) {
            throw new IllegalArgumentException("Left and top must be nonnegative");
        }
        if (height < 1 || width < 1) {
            throw new IllegalArgumentException("Height and width must be at least 1");
        }
        int right = left + width;
        int bottom = top + height;
        if (bottom > this.height || right > this.width) {
            throw new IllegalArgumentException("The region must fit inside the matrix");
        }
        for (int y = top; y < bottom; y++) {
            int offset = y * this.rowSize;
            for (int x = left; x < right; x++) {
                this.bits[offset + (x >> 5)] = this.bits[offset + (x >> 5)] | 1 << (x & 0x1F);
            }
        }
    }

    public BitArray getRow(int y, BitArray row) {
        if (row == null || row.getSize() < this.width) {
            row = new BitArray(this.width);
        }
        int offset = y * this.rowSize;
        for (int x = 0; x < this.rowSize; x++) {
            row.setBulk(x << 5, this.bits[offset + x]);
        }
        return row;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getDimension() {
        if (this.width != this.height) {
            throw new RuntimeException("Can't call getDimension() on a non-square matrix");
        }
        return this.width;
    }

    public String toString() {
        StringBuffer result = new StringBuffer(this.height * (this.width + 1));
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                result.append(get(x, y) ? "X " : "  ");
            }
            result.append('\n');
        }
        return result.toString();
    }
}
