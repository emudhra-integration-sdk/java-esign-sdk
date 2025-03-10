package esign.text.pdf.events;

import esign.text.Chunk;
import esign.text.Document;
import esign.text.Rectangle;
import esign.text.pdf.PdfPageEventHelper;
import esign.text.pdf.PdfWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class IndexEvents
        extends PdfPageEventHelper {

    private Map<String, Integer> indextag = new TreeMap<String, Integer>();

    public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
        this.indextag.put(text, Integer.valueOf(writer.getPageNumber()));
    }

    private long indexcounter = 0L;

    private List<Entry> indexentry = new ArrayList<Entry>();

    public Chunk create(String text, String in1, String in2, String in3) {
        Chunk chunk = new Chunk(text);
        String tag = "idx_" + this.indexcounter++;
        chunk.setGenericTag(tag);
        chunk.setLocalDestination(tag);
        Entry entry = new Entry(in1, in2, in3, tag);
        this.indexentry.add(entry);
        return chunk;
    }

    public Chunk create(String text, String in1) {
        return create(text, in1, "", "");
    }

    public Chunk create(String text, String in1, String in2) {
        return create(text, in1, in2, "");
    }

    public void create(Chunk text, String in1, String in2, String in3) {
        String tag = "idx_" + this.indexcounter++;
        text.setGenericTag(tag);
        text.setLocalDestination(tag);
        Entry entry = new Entry(in1, in2, in3, tag);
        this.indexentry.add(entry);
    }

    public void create(Chunk text, String in1) {
        create(text, in1, "", "");
    }

    public void create(Chunk text, String in1, String in2) {
        create(text, in1, in2, "");
    }

    private Comparator<Entry> comparator = new Comparator<Entry>() {
        public int compare(IndexEvents.Entry en1, IndexEvents.Entry en2) {
            int rt = 0;
            if (en1.getIn1() != null && en2.getIn1() != null && (rt = en1.getIn1().compareToIgnoreCase(en2.getIn1())) == 0) {
                if (en1.getIn2() != null && en2.getIn2() != null && (rt = en1.getIn2().compareToIgnoreCase(en2.getIn2())) == 0) {
                    if (en1.getIn3() != null && en2.getIn3() != null) {
                        rt = en1.getIn3().compareToIgnoreCase(en2
                                .getIn3());
                    }
                }
            }

            return rt;
        }
    };

    public void setComparator(Comparator<Entry> aComparator) {
        this.comparator = aComparator;
    }

    public List<Entry> getSortedEntries() {
        Map<String, Entry> grouped = new HashMap<String, Entry>();

        for (int i = 0; i < this.indexentry.size(); i++) {
            Entry e = this.indexentry.get(i);
            String key = e.getKey();

            Entry master = grouped.get(key);
            if (master != null) {
                master.addPageNumberAndTag(e.getPageNumber(), e.getTag());
            } else {
                e.addPageNumberAndTag(e.getPageNumber(), e.getTag());
                grouped.put(key, e);
            }
        }

        List<Entry> sorted = new ArrayList<Entry>(grouped.values());
        Collections.sort(sorted, this.comparator);
        return sorted;
    }

    public class Entry {

        private String in1;

        private String in2;

        private String in3;

        private String tag;

        private List<Integer> pagenumbers = new ArrayList<Integer>();

        private List<String> tags = new ArrayList<String>();

        public Entry(String aIn1, String aIn2, String aIn3, String aTag) {
            this.in1 = aIn1;
            this.in2 = aIn2;
            this.in3 = aIn3;
            this.tag = aTag;
        }

        public String getIn1() {
            return this.in1;
        }

        public String getIn2() {
            return this.in2;
        }

        public String getIn3() {
            return this.in3;
        }

        public String getTag() {
            return this.tag;
        }

        public int getPageNumber() {
            int rt = -1;
            Integer i = (Integer) IndexEvents.this.indextag.get(this.tag);
            if (i != null) {
                rt = i.intValue();
            }
            return rt;
        }

        public void addPageNumberAndTag(int number, String tag) {
            this.pagenumbers.add(Integer.valueOf(number));
            this.tags.add(tag);
        }

        public String getKey() {
            return this.in1 + "!" + this.in2 + "!" + this.in3;
        }

        public List<Integer> getPagenumbers() {
            return this.pagenumbers;
        }

        public List<String> getTags() {
            return this.tags;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(this.in1).append(' ');
            buf.append(this.in2).append(' ');
            buf.append(this.in3).append(' ');
            for (int i = 0; i < this.pagenumbers.size(); i++) {
                buf.append(this.pagenumbers.get(i)).append(' ');
            }
            return buf.toString();
        }
    }
}
