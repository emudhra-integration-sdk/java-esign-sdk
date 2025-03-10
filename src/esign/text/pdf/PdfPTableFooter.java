package esign.text.pdf;

public class PdfPTableFooter
        extends PdfPTableBody {

    protected PdfName role = PdfName.TFOOT;

    public PdfName getRole() {
        return this.role;
    }

    public void setRole(PdfName role) {
        this.role = role;
    }
}
