package esign.text;

import esign.text.factories.RomanNumberFactory;

public class RomanList
        extends List {

    public RomanList() {
        super(true);
    }

    public RomanList(int symbolIndent) {
        super(true, symbolIndent);
    }

    public RomanList(boolean lowercase, int symbolIndent) {
        super(true, symbolIndent);
        this.lowercase = lowercase;
    }

    public boolean add(Element o) {
        if (o instanceof ListItem) {
            ListItem item = (ListItem) o;

            Chunk chunk = new Chunk(this.preSymbol, this.symbol.getFont());
            chunk.setAttributes(this.symbol.getAttributes());
            chunk.append(RomanNumberFactory.getString(this.first + this.list.size(), this.lowercase));
            chunk.append(this.postSymbol);
            item.setListSymbol(chunk);
            item.setIndentationLeft(this.symbolIndent, this.autoindent);
            item.setIndentationRight(0.0F);
            this.list.add(item);
        } else if (o instanceof List) {
            List nested = (List) o;
            nested.setIndentationLeft(nested.getIndentationLeft() + this.symbolIndent);
            this.first--;
            return this.list.add(nested);
        }
        return false;
    }

    public List cloneShallow() {
        RomanList clone = new RomanList();
        populateProperties(clone);
        return clone;
    }
}
