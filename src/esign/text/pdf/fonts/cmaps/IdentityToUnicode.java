package esign.text.pdf.fonts.cmaps;

import java.io.IOException;

public class IdentityToUnicode {

    private static CMapToUnicode identityCNS;
    private static CMapToUnicode identityJapan;
    private static CMapToUnicode identityKorea;
    private static CMapToUnicode identityGB;
    private static CMapToUnicode identityH;

    public static CMapToUnicode GetMapFromOrdering(String ordering) throws IOException {
        if (ordering.equals("CNS1")) {
            if (identityCNS == null) {
                CMapUniCid uni = CMapCache.getCachedCMapUniCid("UniCNS-UTF16-H");
                if (uni == null) {
                    return null;
                }
                identityCNS = uni.exportToUnicode();
            }
            return identityCNS;
        }
        if (ordering.equals("Japan1")) {
            if (identityJapan == null) {
                CMapUniCid uni = CMapCache.getCachedCMapUniCid("UniJIS-UTF16-H");
                if (uni == null) {
                    return null;
                }
                identityJapan = uni.exportToUnicode();
            }
            return identityJapan;
        }
        if (ordering.equals("Korea1")) {
            if (identityKorea == null) {
                CMapUniCid uni = CMapCache.getCachedCMapUniCid("UniKS-UTF16-H");
                if (uni == null) {
                    return null;
                }
                identityKorea = uni.exportToUnicode();
            }
            return identityKorea;
        }
        if (ordering.equals("GB1")) {
            if (identityGB == null) {
                CMapUniCid uni = CMapCache.getCachedCMapUniCid("UniGB-UTF16-H");
                if (uni == null) {
                    return null;
                }
                identityGB = uni.exportToUnicode();
            }
            return identityGB;
        }
        if (ordering.equals("Identity")) {
            if (identityH == null) {
                identityH = CMapToUnicode.getIdentity();
            }
            return identityH;
        }
        return null;
    }
}
