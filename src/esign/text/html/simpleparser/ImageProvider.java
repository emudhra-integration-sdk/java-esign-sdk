package esign.text.html.simpleparser;

import esign.text.DocListener;
import esign.text.Image;
import java.util.Map;

@Deprecated
public interface ImageProvider {
  Image getImage(String paramString, Map<String, String> paramMap, ChainedProperties paramChainedProperties, DocListener paramDocListener);
}

