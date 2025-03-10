package esign.text;

import esign.text.error_messages.MessageLocalization;
import java.lang.reflect.Field;

public class PageSize {

    public static final Rectangle LETTER = new RectangleReadOnly(612.0F, 792.0F);

    public static final Rectangle NOTE = new RectangleReadOnly(540.0F, 720.0F);

    public static final Rectangle LEGAL = new RectangleReadOnly(612.0F, 1008.0F);

    public static final Rectangle TABLOID = new RectangleReadOnly(792.0F, 1224.0F);

    public static final Rectangle EXECUTIVE = new RectangleReadOnly(522.0F, 756.0F);

    public static final Rectangle POSTCARD = new RectangleReadOnly(283.0F, 416.0F);

    public static final Rectangle A0 = new RectangleReadOnly(2384.0F, 3370.0F);

    public static final Rectangle A1 = new RectangleReadOnly(1684.0F, 2384.0F);

    public static final Rectangle A2 = new RectangleReadOnly(1191.0F, 1684.0F);

    public static final Rectangle A3 = new RectangleReadOnly(842.0F, 1191.0F);

    public static final Rectangle A4 = new RectangleReadOnly(595.0F, 842.0F);

    public static final Rectangle A5 = new RectangleReadOnly(420.0F, 595.0F);

    public static final Rectangle A6 = new RectangleReadOnly(297.0F, 420.0F);

    public static final Rectangle A7 = new RectangleReadOnly(210.0F, 297.0F);

    public static final Rectangle A8 = new RectangleReadOnly(148.0F, 210.0F);

    public static final Rectangle A9 = new RectangleReadOnly(105.0F, 148.0F);

    public static final Rectangle A10 = new RectangleReadOnly(73.0F, 105.0F);

    public static final Rectangle B0 = new RectangleReadOnly(2834.0F, 4008.0F);

    public static final Rectangle B1 = new RectangleReadOnly(2004.0F, 2834.0F);

    public static final Rectangle B2 = new RectangleReadOnly(1417.0F, 2004.0F);

    public static final Rectangle B3 = new RectangleReadOnly(1000.0F, 1417.0F);

    public static final Rectangle B4 = new RectangleReadOnly(708.0F, 1000.0F);

    public static final Rectangle B5 = new RectangleReadOnly(498.0F, 708.0F);

    public static final Rectangle B6 = new RectangleReadOnly(354.0F, 498.0F);

    public static final Rectangle B7 = new RectangleReadOnly(249.0F, 354.0F);

    public static final Rectangle B8 = new RectangleReadOnly(175.0F, 249.0F);

    public static final Rectangle B9 = new RectangleReadOnly(124.0F, 175.0F);

    public static final Rectangle B10 = new RectangleReadOnly(87.0F, 124.0F);

    public static final Rectangle ARCH_E = new RectangleReadOnly(2592.0F, 3456.0F);

    public static final Rectangle ARCH_D = new RectangleReadOnly(1728.0F, 2592.0F);

    public static final Rectangle ARCH_C = new RectangleReadOnly(1296.0F, 1728.0F);

    public static final Rectangle ARCH_B = new RectangleReadOnly(864.0F, 1296.0F);

    public static final Rectangle ARCH_A = new RectangleReadOnly(648.0F, 864.0F);

    public static final Rectangle FLSA = new RectangleReadOnly(612.0F, 936.0F);

    public static final Rectangle FLSE = new RectangleReadOnly(648.0F, 936.0F);

    public static final Rectangle HALFLETTER = new RectangleReadOnly(396.0F, 612.0F);

    public static final Rectangle _11X17 = new RectangleReadOnly(792.0F, 1224.0F);

    public static final Rectangle ID_1 = new RectangleReadOnly(242.65F, 153.0F);

    public static final Rectangle ID_2 = new RectangleReadOnly(297.0F, 210.0F);

    public static final Rectangle ID_3 = new RectangleReadOnly(354.0F, 249.0F);

    public static final Rectangle LEDGER = new RectangleReadOnly(1224.0F, 792.0F);

    public static final Rectangle CROWN_QUARTO = new RectangleReadOnly(535.0F, 697.0F);

    public static final Rectangle LARGE_CROWN_QUARTO = new RectangleReadOnly(569.0F, 731.0F);

    public static final Rectangle DEMY_QUARTO = new RectangleReadOnly(620.0F, 782.0F);

    public static final Rectangle ROYAL_QUARTO = new RectangleReadOnly(671.0F, 884.0F);

    public static final Rectangle CROWN_OCTAVO = new RectangleReadOnly(348.0F, 527.0F);

    public static final Rectangle LARGE_CROWN_OCTAVO = new RectangleReadOnly(365.0F, 561.0F);

    public static final Rectangle DEMY_OCTAVO = new RectangleReadOnly(391.0F, 612.0F);

    public static final Rectangle ROYAL_OCTAVO = new RectangleReadOnly(442.0F, 663.0F);

    public static final Rectangle SMALL_PAPERBACK = new RectangleReadOnly(314.0F, 504.0F);

    public static final Rectangle PENGUIN_SMALL_PAPERBACK = new RectangleReadOnly(314.0F, 513.0F);

    public static final Rectangle PENGUIN_LARGE_PAPERBACK = new RectangleReadOnly(365.0F, 561.0F);

    public static final Rectangle LETTER_LANDSCAPE = new RectangleReadOnly(612.0F, 792.0F, 90);

    public static final Rectangle LEGAL_LANDSCAPE = new RectangleReadOnly(612.0F, 1008.0F, 90);

    public static final Rectangle A4_LANDSCAPE = new RectangleReadOnly(595.0F, 842.0F, 90);

    public static Rectangle getRectangle(String name) {
        name = name.trim().toUpperCase();
        int pos = name.indexOf(' ');
        if (pos == -1) {
            try {
                Field field = PageSize.class.getDeclaredField(name.toUpperCase());
                return (Rectangle) field.get(null);
            } catch (Exception e) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("can.t.find.page.size.1", new Object[]{name}));
            }
        }

        try {
            String width = name.substring(0, pos);
            String height = name.substring(pos + 1);
            return new Rectangle(Float.parseFloat(width), Float.parseFloat(height));
        } catch (Exception e) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("1.is.not.a.valid.page.size.format.2", new Object[]{name, e.getMessage()}));
        }
    }
}
