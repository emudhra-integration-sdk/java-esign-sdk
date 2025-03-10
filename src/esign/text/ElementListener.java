package esign.text;

import java.util.EventListener;

public interface ElementListener extends EventListener {

    boolean add(Element paramElement) throws DocumentException;
}
