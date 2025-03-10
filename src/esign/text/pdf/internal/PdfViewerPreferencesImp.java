package esign.text.pdf.internal;

import esign.text.pdf.PdfBoolean;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.interfaces.PdfViewerPreferences;

public class PdfViewerPreferencesImp
        implements PdfViewerPreferences {

    public static final PdfName[] VIEWER_PREFERENCES = new PdfName[]{PdfName.HIDETOOLBAR, PdfName.HIDEMENUBAR, PdfName.HIDEWINDOWUI, PdfName.FITWINDOW, PdfName.CENTERWINDOW, PdfName.DISPLAYDOCTITLE, PdfName.NONFULLSCREENPAGEMODE, PdfName.DIRECTION, PdfName.VIEWAREA, PdfName.VIEWCLIP, PdfName.PRINTAREA, PdfName.PRINTCLIP, PdfName.PRINTSCALING, PdfName.DUPLEX, PdfName.PICKTRAYBYPDFSIZE, PdfName.PRINTPAGERANGE, PdfName.NUMCOPIES};

    public static final PdfName[] NONFULLSCREENPAGEMODE_PREFERENCES = new PdfName[]{PdfName.USENONE, PdfName.USEOUTLINES, PdfName.USETHUMBS, PdfName.USEOC};

    public static final PdfName[] DIRECTION_PREFERENCES = new PdfName[]{PdfName.L2R, PdfName.R2L};

    public static final PdfName[] PAGE_BOUNDARIES = new PdfName[]{PdfName.MEDIABOX, PdfName.CROPBOX, PdfName.BLEEDBOX, PdfName.TRIMBOX, PdfName.ARTBOX};

    public static final PdfName[] PRINTSCALING_PREFERENCES = new PdfName[]{PdfName.APPDEFAULT, PdfName.NONE};

    public static final PdfName[] DUPLEX_PREFERENCES = new PdfName[]{PdfName.SIMPLEX, PdfName.DUPLEXFLIPSHORTEDGE, PdfName.DUPLEXFLIPLONGEDGE};

    private int pageLayoutAndMode = 0;

    private PdfDictionary viewerPreferences = new PdfDictionary();

    private static final int viewerPreferencesMask = 16773120;

    public int getPageLayoutAndMode() {
        return this.pageLayoutAndMode;
    }

    public PdfDictionary getViewerPreferences() {
        return this.viewerPreferences;
    }

    public void setViewerPreferences(int preferences) {
        this.pageLayoutAndMode |= preferences;

        if ((preferences & 0xFFF000) != 0) {
            this.pageLayoutAndMode = 0xFF000FFF & this.pageLayoutAndMode;
            if ((preferences & 0x1000) != 0) {
                this.viewerPreferences.put(PdfName.HIDETOOLBAR, (PdfObject) PdfBoolean.PDFTRUE);
            }
            if ((preferences & 0x2000) != 0) {
                this.viewerPreferences.put(PdfName.HIDEMENUBAR, (PdfObject) PdfBoolean.PDFTRUE);
            }
            if ((preferences & 0x4000) != 0) {
                this.viewerPreferences.put(PdfName.HIDEWINDOWUI, (PdfObject) PdfBoolean.PDFTRUE);
            }
            if ((preferences & 0x8000) != 0) {
                this.viewerPreferences.put(PdfName.FITWINDOW, (PdfObject) PdfBoolean.PDFTRUE);
            }
            if ((preferences & 0x10000) != 0) {
                this.viewerPreferences.put(PdfName.CENTERWINDOW, (PdfObject) PdfBoolean.PDFTRUE);
            }
            if ((preferences & 0x20000) != 0) {
                this.viewerPreferences.put(PdfName.DISPLAYDOCTITLE, (PdfObject) PdfBoolean.PDFTRUE);
            }
            if ((preferences & 0x40000) != 0) {
                this.viewerPreferences.put(PdfName.NONFULLSCREENPAGEMODE, (PdfObject) PdfName.USENONE);
            } else if ((preferences & 0x80000) != 0) {
                this.viewerPreferences.put(PdfName.NONFULLSCREENPAGEMODE, (PdfObject) PdfName.USEOUTLINES);
            } else if ((preferences & 0x100000) != 0) {
                this.viewerPreferences.put(PdfName.NONFULLSCREENPAGEMODE, (PdfObject) PdfName.USETHUMBS);
            } else if ((preferences & 0x200000) != 0) {
                this.viewerPreferences.put(PdfName.NONFULLSCREENPAGEMODE, (PdfObject) PdfName.USEOC);
            }
            if ((preferences & 0x400000) != 0) {
                this.viewerPreferences.put(PdfName.DIRECTION, (PdfObject) PdfName.L2R);
            } else if ((preferences & 0x800000) != 0) {
                this.viewerPreferences.put(PdfName.DIRECTION, (PdfObject) PdfName.R2L);
            }
            if ((preferences & 0x1000000) != 0) {
                this.viewerPreferences.put(PdfName.PRINTSCALING, (PdfObject) PdfName.NONE);
            }
        }
    }

    private int getIndex(PdfName key) {
        for (int i = 0; i < VIEWER_PREFERENCES.length; i++) {
            if (VIEWER_PREFERENCES[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPossibleValue(PdfName value, PdfName[] accepted) {
        for (int i = 0; i < accepted.length; i++) {
            if (accepted[i].equals(value)) {
                return true;
            }
        }
        return false;
    }

    public void addViewerPreference(PdfName key, PdfObject value) {
        switch (getIndex(key)) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 14:
                if (value instanceof PdfBoolean) {
                    this.viewerPreferences.put(key, value);
                }
                break;
            case 6:
                if (value instanceof PdfName
                        && isPossibleValue((PdfName) value, NONFULLSCREENPAGEMODE_PREFERENCES)) {
                    this.viewerPreferences.put(key, value);
                }
                break;
            case 7:
                if (value instanceof PdfName
                        && isPossibleValue((PdfName) value, DIRECTION_PREFERENCES)) {
                    this.viewerPreferences.put(key, value);
                }
                break;
            case 8:
            case 9:
            case 10:
            case 11:
                if (value instanceof PdfName
                        && isPossibleValue((PdfName) value, PAGE_BOUNDARIES)) {
                    this.viewerPreferences.put(key, value);
                }
                break;
            case 12:
                if (value instanceof PdfName
                        && isPossibleValue((PdfName) value, PRINTSCALING_PREFERENCES)) {
                    this.viewerPreferences.put(key, value);
                }
                break;
            case 13:
                if (value instanceof PdfName
                        && isPossibleValue((PdfName) value, DUPLEX_PREFERENCES)) {
                    this.viewerPreferences.put(key, value);
                }
                break;
            case 15:
                if (value instanceof esign.text.pdf.PdfArray) {
                    this.viewerPreferences.put(key, value);
                }
                break;
            case 16:
                if (value instanceof esign.text.pdf.PdfNumber) {
                    this.viewerPreferences.put(key, value);
                }
                break;
        }
    }

    public void addToCatalog(PdfDictionary catalog) {
        catalog.remove(PdfName.PAGELAYOUT);
        if ((this.pageLayoutAndMode & 0x1) != 0) {
            catalog.put(PdfName.PAGELAYOUT, (PdfObject) PdfName.SINGLEPAGE);
        } else if ((this.pageLayoutAndMode & 0x2) != 0) {
            catalog.put(PdfName.PAGELAYOUT, (PdfObject) PdfName.ONECOLUMN);
        } else if ((this.pageLayoutAndMode & 0x4) != 0) {
            catalog.put(PdfName.PAGELAYOUT, (PdfObject) PdfName.TWOCOLUMNLEFT);
        } else if ((this.pageLayoutAndMode & 0x8) != 0) {
            catalog.put(PdfName.PAGELAYOUT, (PdfObject) PdfName.TWOCOLUMNRIGHT);
        } else if ((this.pageLayoutAndMode & 0x10) != 0) {
            catalog.put(PdfName.PAGELAYOUT, (PdfObject) PdfName.TWOPAGELEFT);
        } else if ((this.pageLayoutAndMode & 0x20) != 0) {
            catalog.put(PdfName.PAGELAYOUT, (PdfObject) PdfName.TWOPAGERIGHT);
        }

        catalog.remove(PdfName.PAGEMODE);
        if ((this.pageLayoutAndMode & 0x40) != 0) {
            catalog.put(PdfName.PAGEMODE, (PdfObject) PdfName.USENONE);
        } else if ((this.pageLayoutAndMode & 0x80) != 0) {
            catalog.put(PdfName.PAGEMODE, (PdfObject) PdfName.USEOUTLINES);
        } else if ((this.pageLayoutAndMode & 0x100) != 0) {
            catalog.put(PdfName.PAGEMODE, (PdfObject) PdfName.USETHUMBS);
        } else if ((this.pageLayoutAndMode & 0x200) != 0) {
            catalog.put(PdfName.PAGEMODE, (PdfObject) PdfName.FULLSCREEN);
        } else if ((this.pageLayoutAndMode & 0x400) != 0) {
            catalog.put(PdfName.PAGEMODE, (PdfObject) PdfName.USEOC);
        } else if ((this.pageLayoutAndMode & 0x800) != 0) {
            catalog.put(PdfName.PAGEMODE, (PdfObject) PdfName.USEATTACHMENTS);
        }

        catalog.remove(PdfName.VIEWERPREFERENCES);
        if (this.viewerPreferences.size() > 0) {
            catalog.put(PdfName.VIEWERPREFERENCES, (PdfObject) this.viewerPreferences);
        }
    }

    public static PdfViewerPreferencesImp getViewerPreferences(PdfDictionary catalog) {
        PdfViewerPreferencesImp preferences = new PdfViewerPreferencesImp();
        int prefs = 0;
        PdfName name = null;

        PdfObject obj = PdfReader.getPdfObjectRelease(catalog.get(PdfName.PAGELAYOUT));
        if (obj != null && obj.isName()) {
            name = (PdfName) obj;
            if (name.equals(PdfName.SINGLEPAGE)) {
                prefs |= 0x1;
            } else if (name.equals(PdfName.ONECOLUMN)) {
                prefs |= 0x2;
            } else if (name.equals(PdfName.TWOCOLUMNLEFT)) {
                prefs |= 0x4;
            } else if (name.equals(PdfName.TWOCOLUMNRIGHT)) {
                prefs |= 0x8;
            } else if (name.equals(PdfName.TWOPAGELEFT)) {
                prefs |= 0x10;
            } else if (name.equals(PdfName.TWOPAGERIGHT)) {
                prefs |= 0x20;
            }
        }
        obj = PdfReader.getPdfObjectRelease(catalog.get(PdfName.PAGEMODE));
        if (obj != null && obj.isName()) {
            name = (PdfName) obj;
            if (name.equals(PdfName.USENONE)) {
                prefs |= 0x40;
            } else if (name.equals(PdfName.USEOUTLINES)) {
                prefs |= 0x80;
            } else if (name.equals(PdfName.USETHUMBS)) {
                prefs |= 0x100;
            } else if (name.equals(PdfName.FULLSCREEN)) {
                prefs |= 0x200;
            } else if (name.equals(PdfName.USEOC)) {
                prefs |= 0x400;
            } else if (name.equals(PdfName.USEATTACHMENTS)) {
                prefs |= 0x800;
            }
        }
        preferences.setViewerPreferences(prefs);

        obj = PdfReader.getPdfObjectRelease(catalog
                .get(PdfName.VIEWERPREFERENCES));
        if (obj != null && obj.isDictionary()) {
            PdfDictionary vp = (PdfDictionary) obj;
            for (int i = 0; i < VIEWER_PREFERENCES.length; i++) {
                obj = PdfReader.getPdfObjectRelease(vp.get(VIEWER_PREFERENCES[i]));
                preferences.addViewerPreference(VIEWER_PREFERENCES[i], obj);
            }
        }
        return preferences;
    }
}
