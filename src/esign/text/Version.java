package esign.text;

import java.lang.reflect.Method;

public final class Version {

    public static String AGPL = " (AGPL-version)";

    private static Version version = null;

    private String iText = "iText®";

    private String release = "5.5.10";

    private String iTextVersion = this.iText + " " + this.release + " ©2000-2015 iText Group NV";

    private String key = null;

    public static Version getInstance() {
        if (version == null) {
            version = new Version();
            synchronized (version) {
                try {
                    Class<?> klass = Class.forName("com.itextpdf.license.LicenseKey");
                    Method m = klass.getMethod("getLicenseeInfo", new Class[0]);
                    String[] info = (String[]) m.invoke(klass.newInstance(), new Object[0]);
                    if (info[3] != null && info[3].trim().length() > 0) {
                        version.key = info[3];
                    } else {
                        version.key = "Trial version ";
                        if (info[5] == null) {
                            version.key += "unauthorised";
                        } else {
                            version.key += info[5];
                        }
                    }

                    if (info[4] != null && info[4].trim().length() > 0) {
                        version.iTextVersion = info[4];
                    } else if (info[2] != null && info[2].trim().length() > 0) {
                        version.iTextVersion += " (" + info[2];
                        if (!version.key.toLowerCase().startsWith("trial")) {
                            version.iTextVersion += "; licensed version)";
                        } else {
                            version.iTextVersion += "; " + version.key + ")";
                        }
                    } else if (info[0] != null && info[0].trim().length() > 0) {

                        version.iTextVersion += " (" + info[0];
                        if (!version.key.toLowerCase().startsWith("trial")) {

                            version.iTextVersion += "; licensed version)";
                        } else {
                            version.iTextVersion += "; " + version.key + ")";
                        }
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    version.iTextVersion += AGPL;
                }
            }
        }
        return version;
    }

    public String getProduct() {
        return this.iText;
    }

    public String getRelease() {
        return this.release;
    }

    public String getVersion() {
        return this.iTextVersion;
    }

    public String getKey() {
        return this.key;
    }

    public static boolean isAGPLVersion() {
        return (getInstance().getVersion().indexOf(AGPL) > 0);
    }
}
