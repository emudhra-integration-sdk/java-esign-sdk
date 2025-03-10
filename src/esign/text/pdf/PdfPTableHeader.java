package esign.text.pdf;

public class PdfPTableHeader
        extends PdfPTableBody {

    protected PdfName role = PdfName.THEAD;

    public PdfName getRole() {
        return this.role;
    }

    public void setRole(PdfName role) {
        this.role = role;
    }
}
