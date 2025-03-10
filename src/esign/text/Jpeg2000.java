package esign.text;

import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class Jpeg2000
        extends Image {

    public static final int JP2_JP = 1783636000;
    public static final int JP2_IHDR = 1768449138;
    public static final int JPIP_JPIP = 1785751920;
    public static final int JP2_FTYP = 1718909296;
    public static final int JP2_JP2H = 1785737832;
    public static final int JP2_COLR = 1668246642;
    public static final int JP2_JP2C = 1785737827;
    public static final int JP2_URL = 1970433056;
    public static final int JP2_DBTL = 1685348972;
    public static final int JP2_BPCC = 1651532643;
    public static final int JP2_JP2 = 1785737760;
    InputStream inp;
    int boxLength;
    int boxType;
    int numOfComps;
    ArrayList<ColorSpecBox> colorSpecBoxes = null;

    boolean isJp2 = false;

    byte[] bpcBoxData;

    Jpeg2000(Image image) {
        super(image);
        if (image instanceof Jpeg2000) {
            Jpeg2000 jpeg2000 = (Jpeg2000) image;
            this.numOfComps = jpeg2000.numOfComps;
            if (this.colorSpecBoxes != null) {
                this.colorSpecBoxes = (ArrayList<ColorSpecBox>) jpeg2000.colorSpecBoxes.clone();
            }
            this.isJp2 = jpeg2000.isJp2;
            if (this.bpcBoxData != null) {
                this.bpcBoxData = (byte[]) jpeg2000.bpcBoxData.clone();
            }
        }
    }

    public Jpeg2000(URL url) throws BadElementException, IOException {
        super(url);
        processParameters();
    }

    public Jpeg2000(byte[] img) throws BadElementException, IOException {
        super((URL) null);
        this.rawData = img;
        this.originalData = img;
        processParameters();
    }

    public Jpeg2000(byte[] img, float width, float height) throws BadElementException, IOException {
        this(img);
        this.scaledWidth = width;
        this.scaledHeight = height;
    }

    private int cio_read(int n) throws IOException {
        int v = 0;
        for (int i = n - 1; i >= 0; i--) {
            v += this.inp.read() << i << 3;
        }
        return v;
    }

    public void jp2_read_boxhdr() throws IOException {
        this.boxLength = cio_read(4);
        this.boxType = cio_read(4);
        if (this.boxLength == 1) {
            if (cio_read(4) != 0) {
                throw new IOException(MessageLocalization.getComposedMessage("cannot.handle.box.sizes.higher.than.2.32", new Object[0]));
            }
            this.boxLength = cio_read(4);
            if (this.boxLength == 0) {
                throw new IOException(MessageLocalization.getComposedMessage("unsupported.box.size.eq.eq.0", new Object[0]));
            }
        } else if (this.boxLength == 0) {
            throw new ZeroBoxSizeException(MessageLocalization.getComposedMessage("unsupported.box.size.eq.eq.0", new Object[0]));
        }
    }

    private void processParameters() throws IOException {
        // Byte code:
        //   0: aload_0
        //   1: bipush #33
        //   3: putfield type : I
        //   6: aload_0
        //   7: bipush #8
        //   9: putfield originalType : I
        //   12: aload_0
        //   13: aconst_null
        //   14: putfield inp : Ljava/io/InputStream;
        //   17: aload_0
        //   18: getfield rawData : [B
        //   21: ifnonnull -> 38
        //   24: aload_0
        //   25: aload_0
        //   26: getfield url : Ljava/net/URL;
        //   29: invokevirtual openStream : ()Ljava/io/InputStream;
        //   32: putfield inp : Ljava/io/InputStream;
        //   35: goto -> 53
        //   38: aload_0
        //   39: new java/io/ByteArrayInputStream
        //   42: dup
        //   43: aload_0
        //   44: getfield rawData : [B
        //   47: invokespecial <init> : ([B)V
        //   50: putfield inp : Ljava/io/InputStream;
        //   53: aload_0
        //   54: aload_0
        //   55: iconst_4
        //   56: invokespecial cio_read : (I)I
        //   59: putfield boxLength : I
        //   62: aload_0
        //   63: getfield boxLength : I
        //   66: bipush #12
        //   68: if_icmpne -> 453
        //   71: aload_0
        //   72: iconst_1
        //   73: putfield isJp2 : Z
        //   76: aload_0
        //   77: aload_0
        //   78: iconst_4
        //   79: invokespecial cio_read : (I)I
        //   82: putfield boxType : I
        //   85: ldc 1783636000
        //   87: aload_0
        //   88: getfield boxType : I
        //   91: if_icmpeq -> 111
        //   94: new java/io/IOException
        //   97: dup
        //   98: ldc 'expected.jp.marker'
        //   100: iconst_0
        //   101: anewarray java/lang/Object
        //   104: invokestatic getComposedMessage : (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
        //   107: invokespecial <init> : (Ljava/lang/String;)V
        //   110: athrow
        //   111: ldc 218793738
        //   113: aload_0
        //   114: iconst_4
        //   115: invokespecial cio_read : (I)I
        //   118: if_icmpeq -> 138
        //   121: new java/io/IOException
        //   124: dup
        //   125: ldc 'error.with.jp.marker'
        //   127: iconst_0
        //   128: anewarray java/lang/Object
        //   131: invokestatic getComposedMessage : (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
        //   134: invokespecial <init> : (Ljava/lang/String;)V
        //   137: athrow
        //   138: aload_0
        //   139: invokevirtual jp2_read_boxhdr : ()V
        //   142: ldc 1718909296
        //   144: aload_0
        //   145: getfield boxType : I
        //   148: if_icmpeq -> 168
        //   151: new java/io/IOException
        //   154: dup
        //   155: ldc 'expected.ftyp.marker'
        //   157: iconst_0
        //   158: anewarray java/lang/Object
        //   161: invokestatic getComposedMessage : (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
        //   164: invokespecial <init> : (Ljava/lang/String;)V
        //   167: athrow
        //   168: aload_0
        //   169: getfield inp : Ljava/io/InputStream;
        //   172: aload_0
        //   173: getfield boxLength : I
        //   176: bipush #8
        //   178: isub
        //   179: invokestatic skip : (Ljava/io/InputStream;I)V
        //   182: aload_0
        //   183: invokevirtual jp2_read_boxhdr : ()V
        //   186: ldc 1785737832
        //   188: aload_0
        //   189: getfield boxType : I
        //   192: if_icmpeq -> 239
        //   195: aload_0
        //   196: getfield boxType : I
        //   199: ldc 1785737827
        //   201: if_icmpne -> 221
        //   204: new java/io/IOException
        //   207: dup
        //   208: ldc 'expected.jp2h.marker'
        //   210: iconst_0
        //   211: anewarray java/lang/Object
        //   214: invokestatic getComposedMessage : (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
        //   217: invokespecial <init> : (Ljava/lang/String;)V
        //   220: athrow
        //   221: aload_0
        //   222: getfield inp : Ljava/io/InputStream;
        //   225: aload_0
        //   226: getfield boxLength : I
        //   229: bipush #8
        //   231: isub
        //   232: invokestatic skip : (Ljava/io/InputStream;I)V
        //   235: aload_0
        //   236: invokevirtual jp2_read_boxhdr : ()V
        //   239: ldc 1785737832
        //   241: aload_0
        //   242: getfield boxType : I
        //   245: if_icmpne -> 186
        //   248: aload_0
        //   249: invokevirtual jp2_read_boxhdr : ()V
        //   252: ldc 1768449138
        //   254: aload_0
        //   255: getfield boxType : I
        //   258: if_icmpeq -> 278
        //   261: new java/io/IOException
        //   264: dup
        //   265: ldc 'expected.ihdr.marker'
        //   267: iconst_0
        //   268: anewarray java/lang/Object
        //   271: invokestatic getComposedMessage : (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
        //   274: invokespecial <init> : (Ljava/lang/String;)V
        //   277: athrow
        //   278: aload_0
        //   279: aload_0
        //   280: iconst_4
        //   281: invokespecial cio_read : (I)I
        //   284: i2f
        //   285: putfield scaledHeight : F
        //   288: aload_0
        //   289: aload_0
        //   290: getfield scaledHeight : F
        //   293: invokevirtual setTop : (F)V
        //   296: aload_0
        //   297: aload_0
        //   298: iconst_4
        //   299: invokespecial cio_read : (I)I
        //   302: i2f
        //   303: putfield scaledWidth : F
        //   306: aload_0
        //   307: aload_0
        //   308: getfield scaledWidth : F
        //   311: invokevirtual setRight : (F)V
        //   314: aload_0
        //   315: aload_0
        //   316: iconst_2
        //   317: invokespecial cio_read : (I)I
        //   320: putfield numOfComps : I
        //   323: aload_0
        //   324: iconst_m1
        //   325: putfield bpc : I
        //   328: aload_0
        //   329: aload_0
        //   330: iconst_1
        //   331: invokespecial cio_read : (I)I
        //   334: putfield bpc : I
        //   337: aload_0
        //   338: getfield inp : Ljava/io/InputStream;
        //   341: iconst_3
        //   342: invokestatic skip : (Ljava/io/InputStream;I)V
        //   345: aload_0
        //   346: invokevirtual jp2_read_boxhdr : ()V
        //   349: aload_0
        //   350: getfield boxType : I
        //   353: ldc 1651532643
        //   355: if_icmpne -> 394
        //   358: aload_0
        //   359: aload_0
        //   360: getfield boxLength : I
        //   363: bipush #8
        //   365: isub
        //   366: newarray byte
        //   368: putfield bpcBoxData : [B
        //   371: aload_0
        //   372: getfield inp : Ljava/io/InputStream;
        //   375: aload_0
        //   376: getfield bpcBoxData : [B
        //   379: iconst_0
        //   380: aload_0
        //   381: getfield boxLength : I
        //   384: bipush #8
        //   386: isub
        //   387: invokevirtual read : ([BII)I
        //   390: pop
        //   391: goto -> 572
        //   394: aload_0
        //   395: getfield boxType : I
        //   398: ldc 1668246642
        //   400: if_icmpne -> 572
        //   403: aload_0
        //   404: getfield colorSpecBoxes : Ljava/util/ArrayList;
        //   407: ifnonnull -> 421
        //   410: aload_0
        //   411: new java/util/ArrayList
        //   414: dup
        //   415: invokespecial <init> : ()V
        //   418: putfield colorSpecBoxes : Ljava/util/ArrayList;
        //   421: aload_0
        //   422: getfield colorSpecBoxes : Ljava/util/ArrayList;
        //   425: aload_0
        //   426: invokespecial jp2_read_colr : ()Lcom/itextpdf/text/Jpeg2000$ColorSpecBox;
        //   429: invokevirtual add : (Ljava/lang/Object;)Z
        //   432: pop
        //   433: aload_0
        //   434: invokevirtual jp2_read_boxhdr : ()V
        //   437: goto -> 441
        //   440: astore_1
        //   441: ldc 1668246642
        //   443: aload_0
        //   444: getfield boxType : I
        //   447: if_icmpeq -> 403
        //   450: goto -> 572
        //   453: aload_0
        //   454: getfield boxLength : I
        //   457: ldc -11534511
        //   459: if_icmpne -> 555
        //   462: aload_0
        //   463: getfield inp : Ljava/io/InputStream;
        //   466: iconst_4
        //   467: invokestatic skip : (Ljava/io/InputStream;I)V
        //   470: aload_0
        //   471: iconst_4
        //   472: invokespecial cio_read : (I)I
        //   475: istore_1
        //   476: aload_0
        //   477: iconst_4
        //   478: invokespecial cio_read : (I)I
        //   481: istore_2
        //   482: aload_0
        //   483: iconst_4
        //   484: invokespecial cio_read : (I)I
        //   487: istore_3
        //   488: aload_0
        //   489: iconst_4
        //   490: invokespecial cio_read : (I)I
        //   493: istore #4
        //   495: aload_0
        //   496: getfield inp : Ljava/io/InputStream;
        //   499: bipush #16
        //   501: invokestatic skip : (Ljava/io/InputStream;I)V
        //   504: aload_0
        //   505: aload_0
        //   506: iconst_2
        //   507: invokespecial cio_read : (I)I
        //   510: putfield colorspace : I
        //   513: aload_0
        //   514: bipush #8
        //   516: putfield bpc : I
        //   519: aload_0
        //   520: iload_2
        //   521: iload #4
        //   523: isub
        //   524: i2f
        //   525: putfield scaledHeight : F
        //   528: aload_0
        //   529: aload_0
        //   530: getfield scaledHeight : F
        //   533: invokevirtual setTop : (F)V
        //   536: aload_0
        //   537: iload_1
        //   538: iload_3
        //   539: isub
        //   540: i2f
        //   541: putfield scaledWidth : F
        //   544: aload_0
        //   545: aload_0
        //   546: getfield scaledWidth : F
        //   549: invokevirtual setRight : (F)V
        //   552: goto -> 572
        //   555: new java/io/IOException
        //   558: dup
        //   559: ldc 'not.a.valid.jpeg2000.file'
        //   561: iconst_0
        //   562: anewarray java/lang/Object
        //   565: invokestatic getComposedMessage : (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
        //   568: invokespecial <init> : (Ljava/lang/String;)V
        //   571: athrow
        //   572: aload_0
        //   573: getfield inp : Ljava/io/InputStream;
        //   576: ifnull -> 627
        //   579: aload_0
        //   580: getfield inp : Ljava/io/InputStream;
        //   583: invokevirtual close : ()V
        //   586: goto -> 590
        //   589: astore_1
        //   590: aload_0
        //   591: aconst_null
        //   592: putfield inp : Ljava/io/InputStream;
        //   595: goto -> 627
        //   598: astore #5
        //   600: aload_0
        //   601: getfield inp : Ljava/io/InputStream;
        //   604: ifnull -> 624
        //   607: aload_0
        //   608: getfield inp : Ljava/io/InputStream;
        //   611: invokevirtual close : ()V
        //   614: goto -> 619
        //   617: astore #6
        //   619: aload_0
        //   620: aconst_null
        //   621: putfield inp : Ljava/io/InputStream;
        //   624: aload #5
        //   626: athrow
        //   627: aload_0
        //   628: aload_0
        //   629: invokevirtual getWidth : ()F
        //   632: putfield plainWidth : F
        //   635: aload_0
        //   636: aload_0
        //   637: invokevirtual getHeight : ()F
        //   640: putfield plainHeight : F
        //   643: return
        // Line number table:
        //   Java source line number -> byte code offset
        //   #174	-> 0
        //   #175	-> 6
        //   #176	-> 12
        //   #178	-> 17
        //   #179	-> 24
        //   #182	-> 38
        //   #184	-> 53
        //   #185	-> 62
        //   #186	-> 71
        //   #187	-> 76
        //   #188	-> 85
        //   #189	-> 94
        //   #191	-> 111
        //   #192	-> 121
        //   #195	-> 138
        //   #196	-> 142
        //   #197	-> 151
        //   #199	-> 168
        //   #200	-> 182
        //   #202	-> 186
        //   #203	-> 195
        //   #204	-> 204
        //   #206	-> 221
        //   #207	-> 235
        //   #209	-> 239
        //   #210	-> 248
        //   #211	-> 252
        //   #212	-> 261
        //   #214	-> 278
        //   #215	-> 288
        //   #216	-> 296
        //   #217	-> 306
        //   #218	-> 314
        //   #219	-> 323
        //   #220	-> 328
        //   #222	-> 337
        //   #224	-> 345
        //   #225	-> 349
        //   #226	-> 358
        //   #227	-> 371
        //   #228	-> 394
        //   #230	-> 403
        //   #231	-> 410
        //   #232	-> 421
        //   #234	-> 433
        //   #237	-> 437
        //   #235	-> 440
        //   #238	-> 441
        //   #241	-> 453
        //   #242	-> 462
        //   #243	-> 470
        //   #244	-> 476
        //   #245	-> 482
        //   #246	-> 488
        //   #247	-> 495
        //   #248	-> 504
        //   #249	-> 513
        //   #250	-> 519
        //   #251	-> 528
        //   #252	-> 536
        //   #253	-> 544
        //   #254	-> 552
        //   #256	-> 555
        //   #260	-> 572
        //   #261	-> 579
        //   #262	-> 590
        //   #260	-> 598
        //   #261	-> 607
        //   #262	-> 619
        //   #265	-> 627
        //   #266	-> 635
        //   #267	-> 643
        // Local variable table:
        //   start	length	slot	name	descriptor
        //   476	76	1	x1	I
        //   482	70	2	y1	I
        //   488	64	3	x0	I
        //   495	57	4	y0	I
        //   0	644	0	this	Lcom/itextpdf/text/Jpeg2000;
        // Exception table:
        //   from	to	target	type
        //   17	572	598	finally
        //   433	437	440	com/itextpdf/text/Jpeg2000$ZeroBoxSizeException
        //   579	586	589	java/lang/Exception
        //   598	600	598	finally
        //   607	614	617	java/lang/Exception
    }

    private ColorSpecBox jp2_read_colr() throws IOException {
        int readBytes = 8;
        ColorSpecBox colr = new ColorSpecBox();
        for (int i = 0; i < 3; i++) {
            colr.add(Integer.valueOf(cio_read(1)));
            readBytes++;
        }
        if (colr.getMeth() == 1) {
            colr.add(Integer.valueOf(cio_read(4)));
            readBytes += 4;
        } else {
            colr.add(Integer.valueOf(0));
        }

        if (this.boxLength - readBytes > 0) {
            byte[] colorProfile = new byte[this.boxLength - readBytes];
            this.inp.read(colorProfile, 0, this.boxLength - readBytes);
            colr.setColorProfile(colorProfile);
        }
        return colr;
    }

    public int getNumOfComps() {
        return this.numOfComps;
    }

    public byte[] getBpcBoxData() {
        return this.bpcBoxData;
    }

    public ArrayList<ColorSpecBox> getColorSpecBoxes() {
        return this.colorSpecBoxes;
    }

    public boolean isJp2() {
        return this.isJp2;
    }

    public static class ColorSpecBox extends ArrayList<Integer> {

        private byte[] colorProfile;

        public int getMeth() {
            return get(0).intValue();
        }

        public int getPrec() {
            return get(1).intValue();
        }

        public int getApprox() {
            return get(2).intValue();
        }

        public int getEnumCs() {
            return get(3).intValue();
        }

        public byte[] getColorProfile() {
            return this.colorProfile;
        }

        void setColorProfile(byte[] colorProfile) {
            this.colorProfile = colorProfile;
        }
    }

    private class ZeroBoxSizeException
            extends IOException {

        public ZeroBoxSizeException() {
        }

        public ZeroBoxSizeException(String s) {
            super(s);
        }
    }
}
