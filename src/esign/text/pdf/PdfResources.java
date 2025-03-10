package esign.text.pdf;

class PdfResources
        extends PdfDictionary {

    void add(PdfName key, PdfDictionary resource) {
        if (resource.size() == 0) {
            return;
        }
        PdfDictionary dic = getAsDict(key);
        if (dic == null) {
            put(key, resource);
        } else {
            dic.putAll(resource);
        }
    }
}
