package esign.text;

import java.net.URL;
import java.security.MessageDigest;

public class ImgJBIG2
        extends Image {

    private byte[] global;
    private byte[] globalHash;

    ImgJBIG2(Image image) {
        super(image);
    }

    public ImgJBIG2() {
        super((Image) null);
    }

    public ImgJBIG2(int width, int height, byte[] data, byte[] globals) {
        super((URL) null);
        this.type = 36;
        this.originalType = 9;
        this.scaledHeight = height;
        setTop(this.scaledHeight);
        this.scaledWidth = width;
        setRight(this.scaledWidth);
        this.bpc = 1;
        this.colorspace = 1;
        this.rawData = data;
        this.plainWidth = getWidth();
        this.plainHeight = getHeight();
        if (globals != null) {
            this.global = globals;

            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(this.global);
                this.globalHash = md.digest();
            } catch (Exception exception) {
            }
        }
    }

    public byte[] getGlobalBytes() {
        return this.global;
    }

    public byte[] getGlobalHash() {
        return this.globalHash;
    }
}
