package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.error_messages.MessageLocalization;
import java.util.Arrays;
import java.util.Locale;

public class PdfDeviceNColor
        implements ICachedColorSpace, IPdfSpecialColorSpace {

    PdfSpotColor[] spotColors;
    ColorDetails[] colorantsDetails;

    public PdfDeviceNColor(PdfSpotColor[] spotColors) {
        this.spotColors = spotColors;
    }

    public int getNumberOfColorants() {
        return this.spotColors.length;
    }

    public PdfSpotColor[] getSpotColors() {
        return this.spotColors;
    }

    public ColorDetails[] getColorantDetails(PdfWriter writer) {
        if (this.colorantsDetails == null) {
            this.colorantsDetails = new ColorDetails[this.spotColors.length];
            int i = 0;
            for (PdfSpotColor spotColorant : this.spotColors) {
                this.colorantsDetails[i] = writer.addSimple(spotColorant);
                i++;
            }
        }
        return this.colorantsDetails;
    }

    public PdfObject getPdfObject(PdfWriter writer) {
        PdfArray array = new PdfArray(PdfName.DEVICEN);

        PdfArray colorants = new PdfArray();
        float[] colorantsRanges = new float[this.spotColors.length * 2];
        PdfDictionary colorantsDict = new PdfDictionary();
        String psFunFooter = "";

        int numberOfColorants = this.spotColors.length;
        float[][] CMYK = new float[4][numberOfColorants];
        int i = 0;
        for (; i < numberOfColorants; i++) {
            PdfSpotColor spotColorant = this.spotColors[i];
            colorantsRanges[2 * i] = 0.0F;
            colorantsRanges[2 * i + 1] = 1.0F;
            colorants.add(spotColorant.getName());
            if (colorantsDict.get(spotColorant.getName()) != null) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("devicen.component.names.shall.be.different", new Object[0]));
            }
            if (this.colorantsDetails != null) {
                colorantsDict.put(spotColorant.getName(), this.colorantsDetails[i].getIndirectReference());
            } else {
                colorantsDict.put(spotColorant.getName(), spotColorant.getPdfObject(writer));
            }
            BaseColor color = spotColorant.getAlternativeCS();
            if (color instanceof ExtendedColor) {
                CMYKColor cmyk;
                int type = ((ExtendedColor) color).type;
                switch (type) {
                    case 1:
                        CMYK[0][i] = 0.0F;
                        CMYK[1][i] = 0.0F;
                        CMYK[2][i] = 0.0F;
                        CMYK[3][i] = 1.0F - ((GrayColor) color).getGray();
                        break;
                    case 2:
                        CMYK[0][i] = ((CMYKColor) color).getCyan();
                        CMYK[1][i] = ((CMYKColor) color).getMagenta();
                        CMYK[2][i] = ((CMYKColor) color).getYellow();
                        CMYK[3][i] = ((CMYKColor) color).getBlack();
                        break;
                    case 7:
                        cmyk = ((LabColor) color).toCmyk();
                        CMYK[0][i] = cmyk.getCyan();
                        CMYK[1][i] = cmyk.getMagenta();
                        CMYK[2][i] = cmyk.getYellow();
                        CMYK[3][i] = cmyk.getBlack();
                        break;
                    default:
                        throw new RuntimeException(MessageLocalization.getComposedMessage("only.rgb.gray.and.cmyk.are.supported.as.alternative.color.spaces", new Object[0]));
                }
            } else {
                float r = color.getRed();
                float g = color.getGreen();
                float b = color.getBlue();
                float computedC = 0.0F, computedM = 0.0F, computedY = 0.0F, computedK = 0.0F;

                if (r == 0.0F && g == 0.0F && b == 0.0F) {
                    computedK = 1.0F;
                } else {
                    computedC = 1.0F - r / 255.0F;
                    computedM = 1.0F - g / 255.0F;
                    computedY = 1.0F - b / 255.0F;

                    float minCMY = Math.min(computedC,
                            Math.min(computedM, computedY));
                    computedC = (computedC - minCMY) / (1.0F - minCMY);
                    computedM = (computedM - minCMY) / (1.0F - minCMY);
                    computedY = (computedY - minCMY) / (1.0F - minCMY);
                    computedK = minCMY;
                }
                CMYK[0][i] = computedC;
                CMYK[1][i] = computedM;
                CMYK[2][i] = computedY;
                CMYK[3][i] = computedK;
            }
            psFunFooter = psFunFooter + "pop ";
        }
        array.add(colorants);

        String psFunHeader = String.format(Locale.US, "1.000000 %d 1 roll ", new Object[]{Integer.valueOf(numberOfColorants + 1)});
        array.add(PdfName.DEVICECMYK);
        psFunHeader = psFunHeader + psFunHeader + psFunHeader + psFunHeader;
        String psFun = "";
        i = numberOfColorants + 4;
        for (; i > numberOfColorants; i--) {
            psFun = psFun + String.format(Locale.US, "%d -1 roll ", new Object[]{Integer.valueOf(i)});
            for (int j = numberOfColorants; j > 0; j--) {
                psFun = psFun + String.format(Locale.US, "%d index %f mul 1.000000 cvr exch sub mul ", new Object[]{Integer.valueOf(j), Float.valueOf(CMYK[numberOfColorants + 4 - i][numberOfColorants - j])});
            }
            psFun = psFun + String.format(Locale.US, "1.000000 cvr exch sub %d 1 roll ", new Object[]{Integer.valueOf(i)});
        }

        PdfFunction func = PdfFunction.type4(writer, colorantsRanges, new float[]{0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F}, "{ " + psFunHeader + psFun + psFunFooter + "}");
        array.add(func.getReference());

        PdfDictionary attr = new PdfDictionary();
        attr.put(PdfName.SUBTYPE, PdfName.NCHANNEL);
        attr.put(PdfName.COLORANTS, colorantsDict);
        array.add(attr);

        return array;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PdfDeviceNColor)) {
            return false;
        }

        PdfDeviceNColor that = (PdfDeviceNColor) o;

        if (!Arrays.equals((Object[]) this.spotColors, (Object[]) that.spotColors)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return Arrays.hashCode((Object[]) this.spotColors);
    }
}
