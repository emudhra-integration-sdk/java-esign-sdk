package esign.text.pdf;

import esign.text.pdf.security.XpathConstructor;

public class XfaXpathConstructor
        implements XpathConstructor {

    private String CONFIG = "config";
    private String CONNECTIONSET = "connectionSet";
    private String DATASETS = "datasets";
    private String LOCALESET = "localeSet";
    private String PDF = "pdf";
    private String SOURCESET = "sourceSet";
    private String STYLESHEET = "stylesheet";
    private String TEMPLATE = "template";
    private String XDC = "xdc";
    private String XFDF = "xfdf";
    private String XMPMETA = "xmpmeta";
    private String xpathExpression;

    public enum XdpPackage {
        Config,
        ConnectionSet,
        Datasets,
        LocaleSet,
        Pdf,
        SourceSet,
        Stylesheet,
        Template,
        Xdc,
        Xfdf,
        Xmpmeta;
    }

    public XfaXpathConstructor() {
        this.CONFIG = "config";
        this.CONNECTIONSET = "connectionSet";
        this.DATASETS = "datasets";
        this.LOCALESET = "localeSet";
        this.PDF = "pdf";
        this.SOURCESET = "sourceSet";
        this.STYLESHEET = "stylesheet";
        this.TEMPLATE = "template";
        this.XDC = "xdc";
        this.XFDF = "xfdf";
        this.XMPMETA = "xmpmeta";

        this.xpathExpression = "";
    }

    public XfaXpathConstructor(XdpPackage xdpPackage) {
        String strPackage;
        this.CONFIG = "config";
        this.CONNECTIONSET = "connectionSet";
        this.DATASETS = "datasets";
        this.LOCALESET = "localeSet";
        this.PDF = "pdf";
        this.SOURCESET = "sourceSet";
        this.STYLESHEET = "stylesheet";
        this.TEMPLATE = "template";
        this.XDC = "xdc";
        this.XFDF = "xfdf";
        this.XMPMETA = "xmpmeta";
        switch (xdpPackage) {
            case Config:
                strPackage = "config";
                break;
            case ConnectionSet:
                strPackage = "connectionSet";
                break;
            case Datasets:
                strPackage = "datasets";
                break;
            case LocaleSet:
                strPackage = "localeSet";
                break;
            case Pdf:
                strPackage = "pdf";
                break;
            case SourceSet:
                strPackage = "sourceSet";
                break;
            case Stylesheet:
                strPackage = "stylesheet";
                break;
            case Template:
                strPackage = "template";
                break;
            case Xdc:
                strPackage = "xdc";
                break;
            case Xfdf:
                strPackage = "xfdf";
                break;
            case Xmpmeta:
                strPackage = "xmpmeta";
                break;
            default:
                this.xpathExpression = "";
                return;
        }

        StringBuilder builder = new StringBuilder("/xdp:xdp/*[local-name()='");
        builder.append(strPackage);
        builder.append("']");
        this.xpathExpression = builder.toString();
    }

    public String getXpathExpression() {
        return this.xpathExpression;
    }
}
