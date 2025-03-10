package esign.text.xml.simpleparser;

import java.util.HashMap;
import java.util.Map;

public class EntitiesToUnicode {

    private static final Map<String, Character> MAP = new HashMap<String, Character>();

    static {
        MAP.put("nbsp", Character.valueOf(' '));
        MAP.put("iexcl", Character.valueOf('¡'));
        MAP.put("cent", Character.valueOf('¢'));
        MAP.put("pound", Character.valueOf('£'));
        MAP.put("curren", Character.valueOf('¤'));
        MAP.put("yen", Character.valueOf('¥'));
        MAP.put("brvbar", Character.valueOf('¦'));
        MAP.put("sect", Character.valueOf('§'));
        MAP.put("uml", Character.valueOf('¨'));
        MAP.put("copy", Character.valueOf('©'));
        MAP.put("ordf", Character.valueOf('ª'));
        MAP.put("laquo", Character.valueOf('«'));
        MAP.put("not", Character.valueOf('¬'));
        MAP.put("shy", Character.valueOf('­'));
        MAP.put("reg", Character.valueOf('®'));
        MAP.put("macr", Character.valueOf('¯'));
        MAP.put("deg", Character.valueOf('°'));
        MAP.put("plusmn", Character.valueOf('±'));
        MAP.put("sup2", Character.valueOf('²'));
        MAP.put("sup3", Character.valueOf('³'));
        MAP.put("acute", Character.valueOf('´'));
        MAP.put("micro", Character.valueOf('µ'));
        MAP.put("para", Character.valueOf('¶'));
        MAP.put("middot", Character.valueOf('·'));
        MAP.put("cedil", Character.valueOf('¸'));
        MAP.put("sup1", Character.valueOf('¹'));
        MAP.put("ordm", Character.valueOf('º'));
        MAP.put("raquo", Character.valueOf('»'));
        MAP.put("frac14", Character.valueOf('¼'));
        MAP.put("frac12", Character.valueOf('½'));
        MAP.put("frac34", Character.valueOf('¾'));
        MAP.put("iquest", Character.valueOf('¿'));
        MAP.put("Agrave", Character.valueOf('À'));
        MAP.put("Aacute", Character.valueOf('Á'));
        MAP.put("Acirc", Character.valueOf('Â'));
        MAP.put("Atilde", Character.valueOf('Ã'));
        MAP.put("Auml", Character.valueOf('Ä'));
        MAP.put("Aring", Character.valueOf('Å'));
        MAP.put("AElig", Character.valueOf('Æ'));
        MAP.put("Ccedil", Character.valueOf('Ç'));
        MAP.put("Egrave", Character.valueOf('È'));
        MAP.put("Eacute", Character.valueOf('É'));
        MAP.put("Ecirc", Character.valueOf('Ê'));
        MAP.put("Euml", Character.valueOf('Ë'));
        MAP.put("Igrave", Character.valueOf('Ì'));
        MAP.put("Iacute", Character.valueOf('Í'));
        MAP.put("Icirc", Character.valueOf('Î'));
        MAP.put("Iuml", Character.valueOf('Ï'));
        MAP.put("ETH", Character.valueOf('Ð'));
        MAP.put("Ntilde", Character.valueOf('Ñ'));
        MAP.put("Ograve", Character.valueOf('Ò'));
        MAP.put("Oacute", Character.valueOf('Ó'));
        MAP.put("Ocirc", Character.valueOf('Ô'));
        MAP.put("Otilde", Character.valueOf('Õ'));
        MAP.put("Ouml", Character.valueOf('Ö'));
        MAP.put("times", Character.valueOf('×'));
        MAP.put("Oslash", Character.valueOf('Ø'));
        MAP.put("Ugrave", Character.valueOf('Ù'));
        MAP.put("Uacute", Character.valueOf('Ú'));
        MAP.put("Ucirc", Character.valueOf('Û'));
        MAP.put("Uuml", Character.valueOf('Ü'));
        MAP.put("Yacute", Character.valueOf('Ý'));
        MAP.put("THORN", Character.valueOf('Þ'));
        MAP.put("szlig", Character.valueOf('ß'));
        MAP.put("agrave", Character.valueOf('à'));
        MAP.put("aacute", Character.valueOf('á'));
        MAP.put("acirc", Character.valueOf('â'));
        MAP.put("atilde", Character.valueOf('ã'));
        MAP.put("auml", Character.valueOf('ä'));
        MAP.put("aring", Character.valueOf('å'));
        MAP.put("aelig", Character.valueOf('æ'));
        MAP.put("ccedil", Character.valueOf('ç'));
        MAP.put("egrave", Character.valueOf('è'));
        MAP.put("eacute", Character.valueOf('é'));
        MAP.put("ecirc", Character.valueOf('ê'));
        MAP.put("euml", Character.valueOf('ë'));
        MAP.put("igrave", Character.valueOf('ì'));
        MAP.put("iacute", Character.valueOf('í'));
        MAP.put("icirc", Character.valueOf('î'));
        MAP.put("iuml", Character.valueOf('ï'));
        MAP.put("eth", Character.valueOf('ð'));
        MAP.put("ntilde", Character.valueOf('ñ'));
        MAP.put("ograve", Character.valueOf('ò'));
        MAP.put("oacute", Character.valueOf('ó'));
        MAP.put("ocirc", Character.valueOf('ô'));
        MAP.put("otilde", Character.valueOf('õ'));
        MAP.put("ouml", Character.valueOf('ö'));
        MAP.put("divide", Character.valueOf('÷'));
        MAP.put("oslash", Character.valueOf('ø'));
        MAP.put("ugrave", Character.valueOf('ù'));
        MAP.put("uacute", Character.valueOf('ú'));
        MAP.put("ucirc", Character.valueOf('û'));
        MAP.put("uuml", Character.valueOf('ü'));
        MAP.put("yacute", Character.valueOf('ý'));
        MAP.put("thorn", Character.valueOf('þ'));
        MAP.put("yuml", Character.valueOf('ÿ'));

        MAP.put("fnof", Character.valueOf('ƒ'));

        MAP.put("Alpha", Character.valueOf('Α'));
        MAP.put("Beta", Character.valueOf('Β'));
        MAP.put("Gamma", Character.valueOf('Γ'));
        MAP.put("Delta", Character.valueOf('Δ'));
        MAP.put("Epsilon", Character.valueOf('Ε'));
        MAP.put("Zeta", Character.valueOf('Ζ'));
        MAP.put("Eta", Character.valueOf('Η'));
        MAP.put("Theta", Character.valueOf('Θ'));
        MAP.put("Iota", Character.valueOf('Ι'));
        MAP.put("Kappa", Character.valueOf('Κ'));
        MAP.put("Lambda", Character.valueOf('Λ'));
        MAP.put("Mu", Character.valueOf('Μ'));
        MAP.put("Nu", Character.valueOf('Ν'));
        MAP.put("Xi", Character.valueOf('Ξ'));
        MAP.put("Omicron", Character.valueOf('Ο'));
        MAP.put("Pi", Character.valueOf('Π'));
        MAP.put("Rho", Character.valueOf('Ρ'));

        MAP.put("Sigma", Character.valueOf('Σ'));
        MAP.put("Tau", Character.valueOf('Τ'));
        MAP.put("Upsilon", Character.valueOf('Υ'));
        MAP.put("Phi", Character.valueOf('Φ'));
        MAP.put("Chi", Character.valueOf('Χ'));
        MAP.put("Psi", Character.valueOf('Ψ'));
        MAP.put("Omega", Character.valueOf('Ω'));
        MAP.put("alpha", Character.valueOf('α'));
        MAP.put("beta", Character.valueOf('β'));
        MAP.put("gamma", Character.valueOf('γ'));
        MAP.put("delta", Character.valueOf('δ'));
        MAP.put("epsilon", Character.valueOf('ε'));
        MAP.put("zeta", Character.valueOf('ζ'));
        MAP.put("eta", Character.valueOf('η'));
        MAP.put("theta", Character.valueOf('θ'));
        MAP.put("iota", Character.valueOf('ι'));
        MAP.put("kappa", Character.valueOf('κ'));
        MAP.put("lambda", Character.valueOf('λ'));
        MAP.put("mu", Character.valueOf('μ'));
        MAP.put("nu", Character.valueOf('ν'));
        MAP.put("xi", Character.valueOf('ξ'));
        MAP.put("omicron", Character.valueOf('ο'));
        MAP.put("pi", Character.valueOf('π'));
        MAP.put("rho", Character.valueOf('ρ'));
        MAP.put("sigmaf", Character.valueOf('ς'));
        MAP.put("sigma", Character.valueOf('σ'));
        MAP.put("tau", Character.valueOf('τ'));
        MAP.put("upsilon", Character.valueOf('υ'));
        MAP.put("phi", Character.valueOf('φ'));
        MAP.put("chi", Character.valueOf('χ'));
        MAP.put("psi", Character.valueOf('ψ'));
        MAP.put("omega", Character.valueOf('ω'));
        MAP.put("thetasym", Character.valueOf('ϑ'));
        MAP.put("upsih", Character.valueOf('ϒ'));
        MAP.put("piv", Character.valueOf('ϖ'));

        MAP.put("bull", Character.valueOf('•'));

        MAP.put("hellip", Character.valueOf('…'));
        MAP.put("prime", Character.valueOf('′'));
        MAP.put("Prime", Character.valueOf('″'));
        MAP.put("oline", Character.valueOf('‾'));
        MAP.put("frasl", Character.valueOf('⁄'));

        MAP.put("weierp", Character.valueOf('℘'));
        MAP.put("image", Character.valueOf('ℑ'));
        MAP.put("real", Character.valueOf('ℜ'));
        MAP.put("trade", Character.valueOf('™'));
        MAP.put("alefsym", Character.valueOf('ℵ'));

        MAP.put("larr", Character.valueOf('←'));
        MAP.put("uarr", Character.valueOf('↑'));
        MAP.put("rarr", Character.valueOf('→'));
        MAP.put("darr", Character.valueOf('↓'));
        MAP.put("harr", Character.valueOf('↔'));
        MAP.put("crarr", Character.valueOf('↵'));
        MAP.put("lArr", Character.valueOf('⇐'));

        MAP.put("uArr", Character.valueOf('⇑'));
        MAP.put("rArr", Character.valueOf('⇒'));

        MAP.put("dArr", Character.valueOf('⇓'));
        MAP.put("hArr", Character.valueOf('⇔'));

        MAP.put("forall", Character.valueOf('∀'));
        MAP.put("part", Character.valueOf('∂'));
        MAP.put("exist", Character.valueOf('∃'));
        MAP.put("empty", Character.valueOf('∅'));
        MAP.put("nabla", Character.valueOf('∇'));
        MAP.put("isin", Character.valueOf('∈'));
        MAP.put("notin", Character.valueOf('∉'));
        MAP.put("ni", Character.valueOf('∋'));

        MAP.put("prod", Character.valueOf('∏'));

        MAP.put("sum", Character.valueOf('∑'));

        MAP.put("minus", Character.valueOf('−'));
        MAP.put("lowast", Character.valueOf('∗'));
        MAP.put("radic", Character.valueOf('√'));
        MAP.put("prop", Character.valueOf('∝'));
        MAP.put("infin", Character.valueOf('∞'));
        MAP.put("ang", Character.valueOf('∠'));
        MAP.put("and", Character.valueOf('∧'));
        MAP.put("or", Character.valueOf('∨'));
        MAP.put("cap", Character.valueOf('∩'));
        MAP.put("cup", Character.valueOf('∪'));
        MAP.put("int", Character.valueOf('∫'));
        MAP.put("there4", Character.valueOf('∴'));
        MAP.put("sim", Character.valueOf('∼'));

        MAP.put("cong", Character.valueOf('≅'));
        MAP.put("asymp", Character.valueOf('≈'));
        MAP.put("ne", Character.valueOf('≠'));
        MAP.put("equiv", Character.valueOf('≡'));
        MAP.put("le", Character.valueOf('≤'));
        MAP.put("ge", Character.valueOf('≥'));
        MAP.put("sub", Character.valueOf('⊂'));
        MAP.put("sup", Character.valueOf('⊃'));

        MAP.put("nsub", Character.valueOf('⊄'));
        MAP.put("sube", Character.valueOf('⊆'));
        MAP.put("supe", Character.valueOf('⊇'));
        MAP.put("oplus", Character.valueOf('⊕'));
        MAP.put("otimes", Character.valueOf('⊗'));
        MAP.put("perp", Character.valueOf('⊥'));
        MAP.put("sdot", Character.valueOf('⋅'));

        MAP.put("lceil", Character.valueOf('⌈'));
        MAP.put("rceil", Character.valueOf('⌉'));
        MAP.put("lfloor", Character.valueOf('⌊'));
        MAP.put("rfloor", Character.valueOf('⌋'));
        MAP.put("lang", Character.valueOf('〈'));

        MAP.put("rang", Character.valueOf('〉'));

        MAP.put("loz", Character.valueOf('◊'));

        MAP.put("spades", Character.valueOf('♠'));

        MAP.put("clubs", Character.valueOf('♣'));
        MAP.put("hearts", Character.valueOf('♥'));
        MAP.put("diams", Character.valueOf('♦'));

        MAP.put("quot", Character.valueOf('"'));
        MAP.put("amp", Character.valueOf('&'));
        MAP.put("apos", Character.valueOf('\''));
        MAP.put("lt", Character.valueOf('<'));
        MAP.put("gt", Character.valueOf('>'));

        MAP.put("OElig", Character.valueOf('Œ'));
        MAP.put("oelig", Character.valueOf('œ'));

        MAP.put("Scaron", Character.valueOf('Š'));
        MAP.put("scaron", Character.valueOf('š'));
        MAP.put("Yuml", Character.valueOf('Ÿ'));

        MAP.put("circ", Character.valueOf('ˆ'));
        MAP.put("tilde", Character.valueOf('˜'));

        MAP.put("ensp", Character.valueOf(' '));
        MAP.put("emsp", Character.valueOf(' '));
        MAP.put("thinsp", Character.valueOf(' '));
        MAP.put("zwnj", Character.valueOf('‌'));
        MAP.put("zwj", Character.valueOf('‍'));
        MAP.put("lrm", Character.valueOf('‎'));
        MAP.put("rlm", Character.valueOf('‏'));
        MAP.put("ndash", Character.valueOf('–'));
        MAP.put("mdash", Character.valueOf('—'));
        MAP.put("lsquo", Character.valueOf('‘'));
        MAP.put("rsquo", Character.valueOf('’'));
        MAP.put("sbquo", Character.valueOf('‚'));
        MAP.put("ldquo", Character.valueOf('“'));
        MAP.put("rdquo", Character.valueOf('”'));
        MAP.put("bdquo", Character.valueOf('„'));
        MAP.put("dagger", Character.valueOf('†'));
        MAP.put("Dagger", Character.valueOf('‡'));
        MAP.put("permil", Character.valueOf('‰'));
        MAP.put("lsaquo", Character.valueOf('‹'));

        MAP.put("rsaquo", Character.valueOf('›'));

        MAP.put("euro", Character.valueOf('€'));
    }

    public static char decodeEntity(String name) {
        if (name.startsWith("#x")) {
            try {
                return (char) Integer.parseInt(name.substring(2), 16);
            } catch (NumberFormatException nfe) {
                return Character.MIN_VALUE;
            }
        }
        if (name.startsWith("#")) {
            try {
                return (char) Integer.parseInt(name.substring(1));
            } catch (NumberFormatException nfe) {
                return Character.MIN_VALUE;
            }
        }
        Character c = MAP.get(name);
        if (c == null) {
            return Character.MIN_VALUE;
        }
        return c.charValue();
    }

    public static String decodeString(String s) {
        int pos_amp = s.indexOf('&');
        if (pos_amp == -1) {
            return s;
        }

        StringBuffer buf = new StringBuffer(s.substring(0, pos_amp));

        while (true) {
            int pos_sc = s.indexOf(';', pos_amp);
            if (pos_sc == -1) {
                buf.append(s.substring(pos_amp));
                return buf.toString();
            }
            int pos_a = s.indexOf('&', pos_amp + 1);
            while (pos_a != -1 && pos_a < pos_sc) {
                buf.append(s.substring(pos_amp, pos_a));
                pos_amp = pos_a;
                pos_a = s.indexOf('&', pos_amp + 1);
            }
            char replace = decodeEntity(s.substring(pos_amp + 1, pos_sc));
            if (s.length() < pos_sc + 1) {
                return buf.toString();
            }
            if (replace == '\000') {
                buf.append(s.substring(pos_amp, pos_sc + 1));
            } else {

                buf.append(replace);
            }
            pos_amp = s.indexOf('&', pos_sc);
            if (pos_amp == -1) {
                buf.append(s.substring(pos_sc + 1));
                return buf.toString();
            }

            buf.append(s.substring(pos_sc + 1, pos_amp));
        }
    }
}
