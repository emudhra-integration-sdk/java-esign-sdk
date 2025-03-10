package esign.text.xmp;

import java.util.Iterator;

public interface XMPIterator extends Iterator {
  void skipSubtree();
  
  void skipSiblings();
}

