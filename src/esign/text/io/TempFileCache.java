package esign.text.io;

import esign.text.pdf.PdfObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;

public class TempFileCache {

    private String filename;
    private RandomAccessFile cache;
    private ByteArrayOutputStream baos;
    private byte[] buf;

    public class ObjectPosition {

        long offset;
        int length;

        ObjectPosition(long offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }

    public TempFileCache(String filename) throws IOException {
        this.filename = filename;
        File f = new File(filename);
        File parent = f.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        this.cache = new RandomAccessFile(filename, "rw");

        this.baos = new ByteArrayOutputStream();
    }

    public ObjectPosition put(PdfObject obj) throws IOException {
        this.baos.reset();
        ObjectOutputStream oos = new ObjectOutputStream(this.baos);

        long offset = this.cache.length();

        oos.writeObject(obj);
        this.cache.seek(offset);
        this.cache.write(this.baos.toByteArray());

        long size = this.cache.length() - offset;

        return new ObjectPosition(offset, (int) size);
    }

    public PdfObject get(ObjectPosition pos) throws IOException, ClassNotFoundException {
        PdfObject obj = null;
        if (pos != null) {
            this.cache.seek(pos.offset);
            this.cache.read(getBuffer(pos.length), 0, pos.length);

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(getBuffer(pos.length)));
            try {
                obj = (PdfObject) ois.readObject();
            } finally {
                ois.close();
            }
        }

        return obj;
    }

    private byte[] getBuffer(int size) {
        if (this.buf == null || this.buf.length < size) {
            this.buf = new byte[size];
        }

        return this.buf;
    }

    public void close() throws IOException {
        this.cache.close();
        this.cache = null;

        (new File(this.filename)).delete();
    }
}
