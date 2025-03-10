package esign.text.pdf.codec;

import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.ImgRaw;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class GifImage {

    protected DataInputStream in;
    protected int width;
    protected int height;
    protected boolean gctFlag;
    protected int bgIndex;
    protected int bgColor;
    protected int pixelAspect;
    protected boolean lctFlag;
    protected boolean interlace;
    protected int lctSize;
    protected int ix;
    protected int iy;
    protected int iw;
    protected int ih;
    protected byte[] block = new byte[256];
    protected int blockSize = 0;

    protected int dispose = 0;
    protected boolean transparency = false;
    protected int delay = 0;

    protected int transIndex;

    protected static final int MaxStackSize = 4096;

    protected short[] prefix;

    protected byte[] suffix;

    protected byte[] pixelStack;

    protected byte[] pixels;
    protected byte[] m_out;
    protected int m_bpc;
    protected int m_gbpc;
    protected byte[] m_global_table;
    protected byte[] m_local_table;
    protected byte[] m_curr_table;
    protected int m_line_stride;
    protected byte[] fromData;
    protected URL fromUrl;
    protected ArrayList<GifFrame> frames = new ArrayList<GifFrame>();

    public GifImage(URL url) throws IOException {
        this.fromUrl = url;
        InputStream is = null;
        try {
            is = url.openStream();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = is.read(bytes)) != -1) {
                baos.write(bytes, 0, read);
            }
            is.close();

            is = new ByteArrayInputStream(baos.toByteArray());
            baos.flush();
            baos.close();

            process(is);
        } finally {

            if (is != null) {
                is.close();
            }
        }
    }

    public GifImage(String file) throws IOException {
        this(Utilities.toURL(file));
    }

    public GifImage(byte[] data) throws IOException {
        this.fromData = data;
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(data);
            process(is);
        } finally {

            if (is != null) {
                is.close();
            }
        }
    }

    public GifImage(InputStream is) throws IOException {
        process(is);
    }

    public int getFrameCount() {
        return this.frames.size();
    }

    public Image getImage(int frame) {
        GifFrame gf = this.frames.get(frame - 1);
        return gf.image;
    }

    public int[] getFramePosition(int frame) {
        GifFrame gf = this.frames.get(frame - 1);
        return new int[]{gf.ix, gf.iy};
    }

    public int[] getLogicalScreen() {
        return new int[]{this.width, this.height};
    }

    void process(InputStream is) throws IOException {
        this.in = new DataInputStream(new BufferedInputStream(is));
        readHeader();
        readContents();
        if (this.frames.isEmpty()) {
            throw new IOException(MessageLocalization.getComposedMessage("the.file.does.not.contain.any.valid.image", new Object[0]));
        }
    }

    protected void readHeader() throws IOException {
        StringBuilder id = new StringBuilder("");
        for (int i = 0; i < 6; i++) {
            id.append((char) this.in.read());
        }
        if (!id.toString().startsWith("GIF8")) {
            throw new IOException(MessageLocalization.getComposedMessage("gif.signature.nor.found", new Object[0]));
        }

        readLSD();
        if (this.gctFlag) {
            this.m_global_table = readColorTable(this.m_gbpc);
        }
    }

    protected void readLSD() throws IOException {
        this.width = readShort();
        this.height = readShort();

        int packed = this.in.read();
        this.gctFlag = ((packed & 0x80) != 0);
        this.m_gbpc = (packed & 0x7) + 1;
        this.bgIndex = this.in.read();
        this.pixelAspect = this.in.read();
    }

    protected int readShort() throws IOException {
        return this.in.read() | this.in.read() << 8;
    }

    protected int readBlock() throws IOException {
        this.blockSize = this.in.read();
        if (this.blockSize <= 0) {
            return this.blockSize = 0;
        }
        this.blockSize = this.in.read(this.block, 0, this.blockSize);

        return this.blockSize;
    }

    protected byte[] readColorTable(int bpc) throws IOException {
        int ncolors = 1 << bpc;
        int nbytes = 3 * ncolors;
        bpc = newBpc(bpc);
        byte[] table = new byte[(1 << bpc) * 3];
        this.in.readFully(table, 0, nbytes);
        return table;
    }

    protected static int newBpc(int bpc) {
        switch (bpc) {

            case 1:
            case 2:
            case 4:
                return bpc;
            case 3:
                return 4;
        }
        return 8;
    }

    protected void readContents() throws IOException {
        boolean done = false;
        while (!done) {
            int code = this.in.read();
            switch (code) {

                case 44:
                    readImage();
                    continue;

                case 33:
                    code = this.in.read();
                    switch (code) {

                        case 249:
                            readGraphicControlExt();
                            continue;

                        case 255:
                            readBlock();
                            skip();
                            continue;
                    }

                    skip();
                    continue;
            }

            done = true;
        }
    }

    protected void readImage() throws IOException {
        ImgRaw imgRaw;
        this.ix = readShort();
        this.iy = readShort();
        this.iw = readShort();
        this.ih = readShort();

        int packed = this.in.read();
        this.lctFlag = ((packed & 0x80) != 0);
        this.interlace = ((packed & 0x40) != 0);

        this.lctSize = 2 << (packed & 0x7);
        this.m_bpc = newBpc(this.m_gbpc);
        if (this.lctFlag) {
            this.m_curr_table = readColorTable((packed & 0x7) + 1);
            this.m_bpc = newBpc((packed & 0x7) + 1);
        } else {

            this.m_curr_table = this.m_global_table;
        }
        if (this.transparency && this.transIndex >= this.m_curr_table.length / 3) {
            this.transparency = false;
        }
        if (this.transparency && this.m_bpc == 1) {
            byte[] tp = new byte[12];
            System.arraycopy(this.m_curr_table, 0, tp, 0, 6);
            this.m_curr_table = tp;
            this.m_bpc = 2;
        }
        boolean skipZero = decodeImageData();
        if (!skipZero) {
            skip();
        }
        Image img = null;
        try {
            imgRaw = new ImgRaw(this.iw, this.ih, 1, this.m_bpc, this.m_out);
            PdfArray colorspace = new PdfArray();
            colorspace.add((PdfObject) PdfName.INDEXED);
            colorspace.add((PdfObject) PdfName.DEVICERGB);
            int len = this.m_curr_table.length;
            colorspace.add((PdfObject) new PdfNumber(len / 3 - 1));
            colorspace.add((PdfObject) new PdfString(this.m_curr_table));
            PdfDictionary ad = new PdfDictionary();
            ad.put(PdfName.COLORSPACE, (PdfObject) colorspace);
            imgRaw.setAdditional(ad);
            if (this.transparency) {
                imgRaw.setTransparency(new int[]{this.transIndex, this.transIndex});
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        imgRaw.setOriginalType(3);
        imgRaw.setOriginalData(this.fromData);
        imgRaw.setUrl(this.fromUrl);
        GifFrame gf = new GifFrame();
        gf.image = (Image) imgRaw;
        gf.ix = this.ix;
        gf.iy = this.iy;
        this.frames.add(gf);
    }

    protected boolean decodeImageData() throws IOException {
        int NullCode = -1;
        int npix = this.iw * this.ih;

        boolean skipZero = false;

        if (this.prefix == null) {
            this.prefix = new short[4096];
        }
        if (this.suffix == null) {
            this.suffix = new byte[4096];
        }
        if (this.pixelStack == null) {
            this.pixelStack = new byte[4097];
        }
        this.m_line_stride = (this.iw * this.m_bpc + 7) / 8;
        this.m_out = new byte[this.m_line_stride * this.ih];
        int pass = 1;
        int inc = this.interlace ? 8 : 1;
        int line = 0;
        int xpos = 0;

        int data_size = this.in.read();
        int clear = 1 << data_size;
        int end_of_information = clear + 1;
        int available = clear + 2;
        int old_code = NullCode;
        int code_size = data_size + 1;
        int code_mask = (1 << code_size) - 1;
        int code;
        for (code = 0; code < clear; code++) {
            this.prefix[code] = 0;
            this.suffix[code] = (byte) code;
        }

        int bi = 0, top = bi, first = top, count = first, bits = count, datum = bits;
        int i;
        label75:
        for (i = 0; i < npix;) {
            if (top == 0) {
                if (bits < code_size) {

                    if (count == 0) {

                        count = readBlock();
                        if (count <= 0) {
                            skipZero = true;
                            break;
                        }
                        bi = 0;
                    }
                    datum += (this.block[bi] & 0xFF) << bits;
                    bits += 8;
                    bi++;
                    count--;

                    continue;
                }

                code = datum & code_mask;
                datum >>= code_size;
                bits -= code_size;

                if (code > available || code == end_of_information) {
                    break;
                }
                if (code == clear) {

                    code_size = data_size + 1;
                    code_mask = (1 << code_size) - 1;
                    available = clear + 2;
                    old_code = NullCode;
                    continue;
                }
                if (old_code == NullCode) {
                    this.pixelStack[top++] = this.suffix[code];
                    old_code = code;
                    first = code;
                    continue;
                }
                int in_code = code;
                if (code == available) {
                    this.pixelStack[top++] = (byte) first;
                    code = old_code;
                }
                while (code > clear) {
                    this.pixelStack[top++] = this.suffix[code];
                    code = this.prefix[code];
                }
                first = this.suffix[code] & 0xFF;

                if (available >= 4096) {
                    break;
                }
                this.pixelStack[top++] = (byte) first;
                this.prefix[available] = (short) old_code;
                this.suffix[available] = (byte) first;
                available++;
                if ((available & code_mask) == 0 && available < 4096) {
                    code_size++;
                    code_mask += available;
                }
                old_code = in_code;
            }

            top--;
            i++;

            setPixel(xpos, line, this.pixelStack[top]);
            xpos++;
            if (xpos >= this.iw) {
                xpos = 0;
                line += inc;
                if (line >= this.ih) {
                    if (this.interlace) {
                        while (true) {
                            pass++;
                            switch (pass) {
                                case 2:
                                    line = 4;
                                    break;
                                case 3:
                                    line = 2;
                                    inc = 4;
                                    break;
                                case 4:
                                    line = 1;
                                    inc = 2;
                                    break;
                                default:
                                    line = this.ih - 1;
                                    inc = 0;
                                    break;
                            }
                            if (line < this.ih) {
                                continue label75;
                            }
                        }
                    }
                    line = this.ih - 1;
                    inc = 0;
                }
            }
        }

        return skipZero;
    }

    protected void setPixel(int x, int y, int v) {
        if (this.m_bpc == 8) {
            int pos = x + this.iw * y;
            this.m_out[pos] = (byte) v;
        } else {

            int pos = this.m_line_stride * y + x / 8 / this.m_bpc;
            int vout = v << 8 - this.m_bpc * x % 8 / this.m_bpc - this.m_bpc;
            this.m_out[pos] = (byte) (this.m_out[pos] | vout);
        }
    }

    protected void resetFrame() {
    }

    protected void readGraphicControlExt() throws IOException {
        this.in.read();
        int packed = this.in.read();
        this.dispose = (packed & 0x1C) >> 2;
        if (this.dispose == 0) {
            this.dispose = 1;
        }
        this.transparency = ((packed & 0x1) != 0);
        this.delay = readShort() * 10;
        this.transIndex = this.in.read();
        this.in.read();
    }

    protected void skip() throws IOException {
        do {
            readBlock();
        } while (this.blockSize > 0);
    }

    static class GifFrame {

        Image image;
        int ix;
        int iy;
    }
}
