package esign.text.pdf.codec;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class JBIG2SegmentReader {

    public static final int SYMBOL_DICTIONARY = 0;
    public static final int INTERMEDIATE_TEXT_REGION = 4;
    public static final int IMMEDIATE_TEXT_REGION = 6;
    public static final int IMMEDIATE_LOSSLESS_TEXT_REGION = 7;
    public static final int PATTERN_DICTIONARY = 16;
    public static final int INTERMEDIATE_HALFTONE_REGION = 20;
    public static final int IMMEDIATE_HALFTONE_REGION = 22;
    public static final int IMMEDIATE_LOSSLESS_HALFTONE_REGION = 23;
    public static final int INTERMEDIATE_GENERIC_REGION = 36;
    public static final int IMMEDIATE_GENERIC_REGION = 38;
    public static final int IMMEDIATE_LOSSLESS_GENERIC_REGION = 39;
    public static final int INTERMEDIATE_GENERIC_REFINEMENT_REGION = 40;
    public static final int IMMEDIATE_GENERIC_REFINEMENT_REGION = 42;
    public static final int IMMEDIATE_LOSSLESS_GENERIC_REFINEMENT_REGION = 43;
    public static final int PAGE_INFORMATION = 48;
    public static final int END_OF_PAGE = 49;
    public static final int END_OF_STRIPE = 50;
    public static final int END_OF_FILE = 51;
    public static final int PROFILES = 52;
    public static final int TABLES = 53;
    public static final int EXTENSION = 62;
    private final SortedMap<Integer, JBIG2Segment> segments = new TreeMap<Integer, JBIG2Segment>();
    private final SortedMap<Integer, JBIG2Page> pages = new TreeMap<Integer, JBIG2Page>();
    private final SortedSet<JBIG2Segment> globals = new TreeSet<JBIG2Segment>();
    private RandomAccessFileOrArray ra;
    private boolean sequential;
    private boolean number_of_pages_known;
    private int number_of_pages = -1;

    private boolean read = false;

    public static class JBIG2Segment
            implements Comparable<JBIG2Segment> {

        public final int segmentNumber;

        public long dataLength = -1L;
        public int page = -1;
        public int[] referredToSegmentNumbers = null;
        public boolean[] segmentRetentionFlags = null;
        public int type = -1;
        public boolean deferredNonRetain = false;
        public int countOfReferredToSegments = -1;
        public byte[] data = null;
        public byte[] headerData = null;
        public boolean page_association_size = false;
        public int page_association_offset = -1;

        public JBIG2Segment(int segment_number) {
            this.segmentNumber = segment_number;
        }

        public int compareTo(JBIG2Segment s) {
            return this.segmentNumber - s.segmentNumber;
        }
    }

    public static class JBIG2Page {

        public final int page;

        private final JBIG2SegmentReader sr;

        private final SortedMap<Integer, JBIG2SegmentReader.JBIG2Segment> segs = new TreeMap<Integer, JBIG2SegmentReader.JBIG2Segment>();
        public int pageBitmapWidth = -1;
        public int pageBitmapHeight = -1;

        public JBIG2Page(int page, JBIG2SegmentReader sr) {
            this.page = page;
            this.sr = sr;
        }

        public byte[] getData(boolean for_embedding) throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (Integer sn : this.segs.keySet()) {
                JBIG2SegmentReader.JBIG2Segment s = this.segs.get(sn);

                if (for_embedding && (s.type == 51 || s.type == 49)) {
                    continue;
                }

                if (for_embedding) {

                    byte[] headerData_emb = JBIG2SegmentReader.copyByteArray(s.headerData);
                    if (s.page_association_size) {
                        headerData_emb[s.page_association_offset] = 0;
                        headerData_emb[s.page_association_offset + 1] = 0;
                        headerData_emb[s.page_association_offset + 2] = 0;
                        headerData_emb[s.page_association_offset + 3] = 1;
                    } else {
                        headerData_emb[s.page_association_offset] = 1;
                    }
                    os.write(headerData_emb);
                } else {
                    os.write(s.headerData);
                }
                os.write(s.data);
            }
            os.close();
            return os.toByteArray();
        }

        public void addSegment(JBIG2SegmentReader.JBIG2Segment s) {
            this.segs.put(Integer.valueOf(s.segmentNumber), s);
        }
    }

    public JBIG2SegmentReader(RandomAccessFileOrArray ra) throws IOException {
        this.ra = ra;
    }

    public static byte[] copyByteArray(byte[] b) {
        byte[] bc = new byte[b.length];
        System.arraycopy(b, 0, bc, 0, b.length);
        return bc;
    }

    public void read() throws IOException {
        if (this.read) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("already.attempted.a.read.on.this.jbig2.file", new Object[0]));
        }
        this.read = true;

        readFileHeader();

        if (this.sequential) {

            do {
                JBIG2Segment tmp = readHeader();
                readSegment(tmp);
                this.segments.put(Integer.valueOf(tmp.segmentNumber), tmp);
            } while (this.ra.getFilePointer() < this.ra.length());
        } else {

            while (true) {

                JBIG2Segment tmp = readHeader();
                this.segments.put(Integer.valueOf(tmp.segmentNumber), tmp);
                if (tmp.type == 51) {
                    Iterator<Integer> segs = this.segments.keySet().iterator();
                    while (segs.hasNext()) {
                        readSegment(this.segments.get(segs.next()));
                    }
                    return;
                }
            }
        }
    }

    void readSegment(JBIG2Segment s) throws IOException {
        int ptr = (int) this.ra.getFilePointer();

        if (s.dataLength == 4294967295L) {
            return;
        }

        byte[] data = new byte[(int) s.dataLength];
        this.ra.read(data);
        s.data = data;

        if (s.type == 48) {
            int last = (int) this.ra.getFilePointer();
            this.ra.seek(ptr);
            int page_bitmap_width = this.ra.readInt();
            int page_bitmap_height = this.ra.readInt();
            this.ra.seek(last);
            JBIG2Page p = this.pages.get(Integer.valueOf(s.page));
            if (p == null) {
                throw new IllegalStateException(MessageLocalization.getComposedMessage("referring.to.widht.height.of.page.we.havent.seen.yet.1", s.page));
            }

            p.pageBitmapWidth = page_bitmap_width;
            p.pageBitmapHeight = page_bitmap_height;
        }
    }

    JBIG2Segment readHeader() throws IOException {
        int segment_page_association, ptr = (int) this.ra.getFilePointer();

        int segment_number = this.ra.readInt();
        JBIG2Segment s = new JBIG2Segment(segment_number);

        int segment_header_flags = this.ra.read();
        boolean deferred_non_retain = ((segment_header_flags & 0x80) == 128);
        s.deferredNonRetain = deferred_non_retain;
        boolean page_association_size = ((segment_header_flags & 0x40) == 64);
        int segment_type = segment_header_flags & 0x3F;
        s.type = segment_type;

        int referred_to_byte0 = this.ra.read();
        int count_of_referred_to_segments = (referred_to_byte0 & 0xE0) >> 5;
        int[] referred_to_segment_numbers = null;
        boolean[] segment_retention_flags = null;

        if (count_of_referred_to_segments == 7) {

            this.ra.seek(this.ra.getFilePointer() - 1L);
            count_of_referred_to_segments = this.ra.readInt() & 0x1FFFFFFF;
            segment_retention_flags = new boolean[count_of_referred_to_segments + 1];
            int j = 0;
            int referred_to_current_byte = 0;
            do {
                int k = j % 8;
                if (k == 0) {
                    referred_to_current_byte = this.ra.read();
                }
                segment_retention_flags[j] = ((1 << k & referred_to_current_byte) >> k == 1);
                ++j;
            } while (j <= count_of_referred_to_segments);
        } else if (count_of_referred_to_segments <= 4) {

            segment_retention_flags = new boolean[count_of_referred_to_segments + 1];
            referred_to_byte0 &= 0x1F;
            for (int j = 0; j <= count_of_referred_to_segments; j++) {
                segment_retention_flags[j] = ((1 << j & referred_to_byte0) >> j == 1);
            }
        } else if (count_of_referred_to_segments == 5 || count_of_referred_to_segments == 6) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("count.of.referred.to.segments.had.bad.value.in.header.for.segment.1.starting.at.2", new Object[]{String.valueOf(segment_number), String.valueOf(ptr)}));
        }
        s.segmentRetentionFlags = segment_retention_flags;
        s.countOfReferredToSegments = count_of_referred_to_segments;

        referred_to_segment_numbers = new int[count_of_referred_to_segments + 1];
        for (int i = 1; i <= count_of_referred_to_segments; i++) {
            if (segment_number <= 256) {
                referred_to_segment_numbers[i] = this.ra.read();
            } else if (segment_number <= 65536) {
                referred_to_segment_numbers[i] = this.ra.readUnsignedShort();
            } else {
                referred_to_segment_numbers[i] = (int) this.ra.readUnsignedInt();
            }
        }
        s.referredToSegmentNumbers = referred_to_segment_numbers;

        int page_association_offset = (int) this.ra.getFilePointer() - ptr;
        if (page_association_size) {
            segment_page_association = this.ra.readInt();
        } else {
            segment_page_association = this.ra.read();
        }
        if (segment_page_association < 0) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("page.1.invalid.for.segment.2.starting.at.3", new Object[]{String.valueOf(segment_page_association), String.valueOf(segment_number), String.valueOf(ptr)}));
        }
        s.page = segment_page_association;

        s.page_association_size = page_association_size;
        s.page_association_offset = page_association_offset;

        if (segment_page_association > 0 && !this.pages.containsKey(Integer.valueOf(segment_page_association))) {
            this.pages.put(Integer.valueOf(segment_page_association), new JBIG2Page(segment_page_association, this));
        }
        if (segment_page_association > 0) {
            ((JBIG2Page) this.pages.get(Integer.valueOf(segment_page_association))).addSegment(s);
        } else {
            this.globals.add(s);
        }

        long segment_data_length = this.ra.readUnsignedInt();

        s.dataLength = segment_data_length;

        int end_ptr = (int) this.ra.getFilePointer();
        this.ra.seek(ptr);
        byte[] header_data = new byte[end_ptr - ptr];
        this.ra.read(header_data);
        s.headerData = header_data;

        return s;
    }

    void readFileHeader() throws IOException {
        this.ra.seek(0L);
        byte[] idstring = new byte[8];
        this.ra.read(idstring);

        byte[] refidstring = {-105, 74, 66, 50, 13, 10, 26, 10};

        for (int i = 0; i < idstring.length; i++) {
            if (idstring[i] != refidstring[i]) {
                throw new IllegalStateException(MessageLocalization.getComposedMessage("file.header.idstring.not.good.at.byte.1", i));
            }
        }

        int fileheaderflags = this.ra.read();

        this.sequential = ((fileheaderflags & 0x1) == 1);
        this.number_of_pages_known = ((fileheaderflags & 0x2) == 0);

        if ((fileheaderflags & 0xFC) != 0) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("file.header.flags.bits.2.7.not.0", new Object[0]));
        }

        if (this.number_of_pages_known) {
            this.number_of_pages = this.ra.readInt();
        }
    }

    public int numberOfPages() {
        return this.pages.size();
    }

    public int getPageHeight(int i) {
        return ((JBIG2Page) this.pages.get(Integer.valueOf(i))).pageBitmapHeight;
    }

    public int getPageWidth(int i) {
        return ((JBIG2Page) this.pages.get(Integer.valueOf(i))).pageBitmapWidth;
    }

    public JBIG2Page getPage(int page) {
        return this.pages.get(Integer.valueOf(page));
    }

    public byte[] getGlobal(boolean for_embedding) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            for (JBIG2Segment element : this.globals) {
                JBIG2Segment s = element;
                if (for_embedding && (s.type == 51 || s.type == 49)) {
                    continue;
                }

                os.write(s.headerData);
                os.write(s.data);
            }
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (os.size() <= 0) {
            return null;
        }
        return os.toByteArray();
    }

    public String toString() {
        if (this.read) {
            return "Jbig2SegmentReader: number of pages: " + numberOfPages();
        }
        return "Jbig2SegmentReader in indeterminate state.";
    }
}
