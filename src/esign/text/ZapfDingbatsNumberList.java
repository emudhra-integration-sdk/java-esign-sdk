package esign.text;

public class ZapfDingbatsNumberList
        extends List {

    protected int type;

    public ZapfDingbatsNumberList(int type) {
        super(true);
        this.type = type;
        float fontsize = this.symbol.getFont().getSize();
        this.symbol.setFont(FontFactory.getFont("ZapfDingbats", fontsize, 0));
        this.postSymbol = " ";
    }

    public ZapfDingbatsNumberList(int type, int symbolIndent) {
        super(true, symbolIndent);
        this.type = type;
        float fontsize = this.symbol.getFont().getSize();
        this.symbol.setFont(FontFactory.getFont("ZapfDingbats", fontsize, 0));
        this.postSymbol = " ";
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public boolean add(Element o) {
        if (o instanceof ListItem) {
            ListItem item = (ListItem) o;
            Chunk chunk = new Chunk(this.preSymbol, this.symbol.getFont());
            chunk.setAttributes(this.symbol.getAttributes());
            switch (this.type) {
                case 0:
                    chunk.append(String.valueOf((char) (this.first + this.list.size() + 171)));
                    break;
                case 1:
                    chunk.append(String.valueOf((char) (this.first + this.list.size() + 181)));
                    break;
                case 2:
                    chunk.append(String.valueOf((char) (this.first + this.list.size() + 191)));
                    break;
                default:
                    chunk.append(String.valueOf((char) (this.first + this.list.size() + 201)));
                    break;
            }
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
        ZapfDingbatsNumberList clone = new ZapfDingbatsNumberList(this.type);
        populateProperties(clone);
        return clone;
    }
}
