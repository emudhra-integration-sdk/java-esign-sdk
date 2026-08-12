package com.emudhra.esign;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Content and layout settings for the Aadhaar (eSign) signature appearance.
 *
 * <p>Pass an instance to
 * {@link eSign#getSigedDocument(String, String, AadhaarSignatureAppearance)} to
 * control what the visible signature block shows. Every field is optional — an
 * all-default instance produces:
 *
 * <pre>
 *   Digitally Signed by
 *   Name : &lt;CN from signer certificate&gt;
 *   Aadhaar No : **** **** 1234
 *   Reason : &lt;PDF /Reason, when present&gt;
 *   Date : 09-Aug-2026 19:38:42
 * </pre>
 *
 * <p>That is the pre-existing block with two deliberate corrections: the reason
 * label is {@code "Reason : "} rather than {@code "Reason: "}, matching the other
 * labels, and the date is rendered in IST rather than the JVM default zone.
 * Callers that pass no appearance keep the old output exactly.
 *
 * <p><b>Name resolution</b> follows {@code signerName} &rarr; {@code aadhaarName}
 * &rarr; certificate CN: set {@code signerName} and it wins; leave it unset and
 * the Aadhaar name is shown instead.
 *
 * <p><b>Note on fonts:</b> the appearance is drawn with non-embedded Helvetica
 * in WinAnsi encoding, so only Latin-1 characters render. An Aadhaar name in a
 * regional script needs an embedded Unicode font, which this class does not yet
 * support.
 */
public class AadhaarSignatureAppearance {

    /** Default label/format values — also the pre-existing hardcoded appearance. */
    public static final String DEFAULT_HEADER_TEXT = "Digitally Signed by";
    public static final String DEFAULT_NAME_LABEL = "Name : ";
    public static final String DEFAULT_AADHAAR_LABEL = "Aadhaar No : ";
    public static final String DEFAULT_REASON_LABEL = "Reason : ";
    public static final String DEFAULT_LOCATION_LABEL = "Location : ";
    public static final String DEFAULT_DATE_LABEL = "Date : ";
    public static final String DEFAULT_DATE_FORMAT = "dd-MMM-yyyy HH:mm:ss";
    public static final String DEFAULT_TIME_ZONE = "IST";
    /** Mask shown before the four visible Aadhaar digits. */
    public static final String AADHAAR_MASK = "**** **** ";

    // ── content ──────────────────────────────────────────────────────────────

    /** First line, e.g. "Digitally Signed by" or "eSigned by". Empty string omits it. */
    private String headerText = DEFAULT_HEADER_TEXT;

    /** Signer name supplied by the ASP. When set it is shown in place of the certificate name. */
    private String signerName;

    /**
     * Custom content as lines containing placeholders — when set, this replaces the
     * whole label/order/toggle machinery and is used verbatim. See
     * {@link #setContentLines(List)} for the placeholder list.
     */
    private List<String> contentLines;

    private boolean showName = true;
    private boolean showAadhaar = true;
    private boolean showReason = true;
    private boolean showLocation = false;
    private boolean showDate = true;

    private String nameLabel = DEFAULT_NAME_LABEL;
    private String aadhaarLabel = DEFAULT_AADHAAR_LABEL;
    private String reasonLabel = DEFAULT_REASON_LABEL;
    private String locationLabel = DEFAULT_LOCATION_LABEL;
    private String dateLabel = DEFAULT_DATE_LABEL;

    private String dateFormat = DEFAULT_DATE_FORMAT;
    private String timeZone = DEFAULT_TIME_ZONE;

    /** Extra lines appended verbatim after the standard block. */
    private List<String> additionalLines;

    /** A line of the appearance block, for {@link #setLineOrder(List)}. */
    public enum Field {
        HEADER, NAME, AADHAAR, REASON, LOCATION, DATE
    }

    /** Default top-to-bottom order of the block. */
    private static final List<Field> DEFAULT_LINE_ORDER = java.util.Collections.unmodifiableList(
            java.util.Arrays.asList(Field.HEADER, Field.NAME, Field.AADHAAR,
                    Field.REASON, Field.LOCATION, Field.DATE));

    /** Line order; null uses {@link #DEFAULT_LINE_ORDER}. Omitted fields are not drawn. */
    private List<Field> lineOrder;

    // ── layout ───────────────────────────────────────────────────────────────

    /** Where the text block is anchored inside the signature rectangle. */
    private eSign.Coordinates contentPosition = eSign.Coordinates.TopLeft;
    /** Font size in points; 0 auto-fits the block to the rectangle. */
    private float fontSize;
    /** Line spacing in points; 0 uses the font size. */
    private float leading;
    /** Text colour as "RRGGBB" or "#RRGGBB"; null is black. */
    private String fontColorHex;
    /** Draw the block in italic (Helvetica-Oblique). */
    private boolean italic;
    /** Draw the block in bold (Helvetica-Bold). */
    private boolean bold;

    private float marginLeft = 4f;
    private float marginRight = 4f;
    private float marginTop = 3f;
    private float marginBottom = 3f;

    // ── line building ────────────────────────────────────────────────────────

    /**
     * Builds the appearance text, resolving overrides against the certificate
     * and the values embedded in the PDF signature dictionary.
     *
     * @param certCommonName CN from the signer certificate (may be null)
     * @param certAadhaar    Aadhaar value from the certificate Title OID (may be null)
     * @param reason         PDF /Reason (may be null)
     * @param location       PDF /Location (may be null)
     * @param signedOn       signing time from PDF /M, or now when unavailable
     * @return 
     */
    protected List<String> buildLines(String certCommonName, String certAadhaar,
            String reason, String location, Date signedOn) {

        List<String> lines = new ArrayList<>();

        if (contentLines != null && !contentLines.isEmpty()) {
            lines.addAll(resolveTemplate(certCommonName, certAadhaar, reason, location, signedOn));
            appendAdditionalLines(lines);
            return lines;
        }

        for (Field field : (lineOrder != null && !lineOrder.isEmpty()) ? lineOrder : DEFAULT_LINE_ORDER) {
            if (field == null) {
                continue;
            }
            switch (field) {
                case HEADER:
                    if (!isBlank(headerText)) {
                        lines.add(headerText);
                    }
                    break;
                case NAME:
                    if (showName) {
                        lines.add(nvl(nameLabel) + resolveName(certCommonName));
                    }
                    break;
                case AADHAAR:
                    if (showAadhaar && !isBlank(certAadhaar)) {
                        lines.add(nvl(aadhaarLabel) + AADHAAR_MASK + lastFourOf(certAadhaar));
                    }
                    break;
                case REASON:
                    if (showReason && !isBlank(reason)) {
                        lines.add(nvl(reasonLabel) + reason.trim());
                    }
                    break;
                case LOCATION:
                    if (showLocation && !isBlank(location)) {
                        lines.add(nvl(locationLabel) + location.trim());
                    }
                    break;
                case DATE:
                    if (showDate) {
                        lines.add(nvl(dateLabel) + formatDate(signedOn != null ? signedOn : new Date()));
                    }
                    break;
            }
        }
        appendAdditionalLines(lines);
        return lines;
    }

    private void appendAdditionalLines(List<String> lines) {
        if (additionalLines != null) {
            for (String extra : additionalLines) {
                if (!isBlank(extra)) {
                    lines.add(extra);
                }
            }
        }
    }

    /** Name shown to the signer: the ASP-supplied name if any, else the certificate CN. */
    private String resolveName(String certCommonName) {
        return firstNonBlank(signerName, certCommonName, "Unknown");
    }

    /**
     * Substitutes placeholders in {@link #contentLines}.
     *
     * <p>A line whose placeholders <em>all</em> resolve to empty is dropped, so
     * {@code "Reason: {reason}"} disappears when the PDF carries no reason, and
     * {@code "Aadhaar No : {aadhaar}"} disappears when {@code showAadhaar} is false.
     * A line with no placeholders at all is always kept.
     */
    private List<String> resolveTemplate(String certCommonName, String certAadhaar,
            String reason, String location, Date signedOn) {

        String maskedAadhaar = "";
        String aadhaarDigits = "";
        if (showAadhaar && !isBlank(certAadhaar)) {
            aadhaarDigits = lastFourOf(certAadhaar);
            maskedAadhaar = AADHAAR_MASK + aadhaarDigits;
        }

        // Insertion-ordered so substitution is deterministic.
        java.util.LinkedHashMap<String, String> tokens = new java.util.LinkedHashMap<>();
        tokens.put("{name}", resolveName(certCommonName));
        tokens.put("{certName}", nvl(certCommonName).trim());
        tokens.put("{aadhaar}", maskedAadhaar);
        tokens.put("{aadhaarDigits}", aadhaarDigits);
        tokens.put("{reason}", isBlank(reason) ? "" : reason.trim());
        tokens.put("{location}", isBlank(location) ? "" : location.trim());
        tokens.put("{date}", formatDate(signedOn != null ? signedOn : new Date()));

        List<String> resolved = new ArrayList<>();
        for (String template : contentLines) {
            if (template == null) {
                continue;
            }
            String line = template;
            int placeholders = 0;
            int filled = 0;
            for (java.util.Map.Entry<String, String> token : tokens.entrySet()) {
                if (line.contains(token.getKey())) {
                    placeholders++;
                    if (!token.getValue().isEmpty()) {
                        filled++;
                    }
                    line = line.replace(token.getKey(), token.getValue());
                }
            }
            if (placeholders > 0 && filled == 0) {
                continue;   // every placeholder was empty — drop the line
            }
            if (!isBlank(line)) {
                resolved.add(line);
            }
        }
        return resolved;
    }

    private String formatDate(Date when) {
        SimpleDateFormat sdf = new SimpleDateFormat(
                isBlank(dateFormat) ? DEFAULT_DATE_FORMAT : dateFormat);
        if (!isBlank(timeZone)) {
            sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        return sdf.format(when);
    }

    /**
     * The certificate stores only the last four Aadhaar digits, but an ASP-supplied
     * override may carry the full number — never render more than the last four.
     */
    private static String lastFourOf(String aadhaar) {
        String trimmed = aadhaar.trim();
        return trimmed.length() > 4 ? trimmed.substring(trimmed.length() - 4) : trimmed;
    }

    /** Parsed {@link #fontColorHex} as {r,g,b} in 0..1, or black.
     * @return  */
    protected float[] resolveFontColor() {
        float[] black = {0f, 0f, 0f};
        if (isBlank(fontColorHex)) {
            return black;
        }
        try {
            String hex = fontColorHex.trim();
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() != 6) {
                return black;
            }
            return new float[]{
                Integer.parseInt(hex.substring(0, 2), 16) / 255f,
                Integer.parseInt(hex.substring(2, 4), 16) / 255f,
                Integer.parseInt(hex.substring(4, 6), 16) / 255f
            };
        } catch (NumberFormatException e) {
            return black;
        }
    }

    /**
     * The base-14 font name for the requested style. Standard PDF fonts, so
     * nothing is embedded and the file size is unchanged.
     */
    protected String resolveBaseFontName() {
        if (bold && italic) {
            return "Helvetica-BoldOblique";
        }
        if (bold) {
            return "Helvetica-Bold";
        }
        if (italic) {
            return "Helvetica-Oblique";
        }
        return "Helvetica";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (!isBlank(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    // ── getters / setters ────────────────────────────────────────────────────

    public String getHeaderText() { return headerText; }
    public void setHeaderText(String headerText) { this.headerText = headerText; }

    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }

    public List<String> getContentLines() { return contentLines; }

    /**
     * Sets custom content as a list of lines. When set, these lines <em>are</em> the
     * appearance — {@code headerText}, the {@code *Label} values, the {@code show*}
     * flags except {@code showAadhaar}, and {@code lineOrder} are all ignored.
     *
     * <p>Supported placeholders:
     * <table>
     *   <tr><td>{@code {name}}</td><td>signerName if supplied, else the certificate CN</td></tr>
     *   <tr><td>{@code {certName}}</td><td>the certificate CN, always</td></tr>
     *   <tr><td>{@code {aadhaar}}</td><td>masked number, e.g. {@code **** **** 1234}; empty when
     *       {@code showAadhaar} is false</td></tr>
     *   <tr><td>{@code {aadhaarDigits}}</td><td>the four digits alone; same toggle</td></tr>
     *   <tr><td>{@code {reason}}</td><td>PDF /Reason</td></tr>
     *   <tr><td>{@code {location}}</td><td>PDF /Location</td></tr>
     *   <tr><td>{@code {date}}</td><td>signing time, per dateFormat and timeZone</td></tr>
     * </table>
     *
     * <p>Lines whose placeholders all resolve to empty are dropped, so optional
     * values do not leave dangling labels behind.
     *
     * <pre>
     * ap.setContentLines(Arrays.asList(
     *         "E-Signed By: {name}",
     *         "Date: {date}",
     *         "Reason: {reason}",
     *         "Location: {location}"));
     * </pre>
     */
    public void setContentLines(List<String> contentLines) { this.contentLines = contentLines; }

    public boolean isShowName() { return showName; }
    public void setShowName(boolean showName) { this.showName = showName; }

    public boolean isShowAadhaar() { return showAadhaar; }
    public void setShowAadhaar(boolean showAadhaar) { this.showAadhaar = showAadhaar; }

    public boolean isShowReason() { return showReason; }
    public void setShowReason(boolean showReason) { this.showReason = showReason; }

    public boolean isShowLocation() { return showLocation; }
    public void setShowLocation(boolean showLocation) { this.showLocation = showLocation; }

    public boolean isShowDate() { return showDate; }
    public void setShowDate(boolean showDate) { this.showDate = showDate; }

    public String getNameLabel() { return nameLabel; }
    public void setNameLabel(String nameLabel) { this.nameLabel = nameLabel; }

    public String getAadhaarLabel() { return aadhaarLabel; }
    public void setAadhaarLabel(String aadhaarLabel) { this.aadhaarLabel = aadhaarLabel; }

    public String getReasonLabel() { return reasonLabel; }
    public void setReasonLabel(String reasonLabel) { this.reasonLabel = reasonLabel; }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public List<String> getAdditionalLines() { return additionalLines; }
    public void setAdditionalLines(List<String> additionalLines) { this.additionalLines = additionalLines; }

    public List<Field> getLineOrder() { return lineOrder; }

    /**
     * Sets the top-to-bottom order of the block. Fields left out of the list are
     * not drawn, so this can replace the {@code show*} flags entirely. Null or an
     * empty list restores the default order.
     */
    public void setLineOrder(List<Field> lineOrder) { this.lineOrder = lineOrder; }

    public eSign.Coordinates getContentPosition() { return contentPosition; }
    public void setContentPosition(eSign.Coordinates contentPosition) { this.contentPosition = contentPosition; }

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }

    public float getLeading() { return leading; }
    public void setLeading(float leading) { this.leading = leading; }

    public String getFontColorHex() { return fontColorHex; }
    public void setFontColorHex(String fontColorHex) { this.fontColorHex = fontColorHex; }

    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }

    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }

    public float getMarginLeft() { return marginLeft; }
    public void setMarginLeft(float marginLeft) { this.marginLeft = marginLeft; }

    public float getMarginRight() { return marginRight; }
    public void setMarginRight(float marginRight) { this.marginRight = marginRight; }

    public float getMarginTop() { return marginTop; }
    public void setMarginTop(float marginTop) { this.marginTop = marginTop; }

    public float getMarginBottom() { return marginBottom; }
    public void setMarginBottom(float marginBottom) { this.marginBottom = marginBottom; }
}
