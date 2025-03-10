package esign.text.xmp.properties;

import esign.text.xmp.options.PropertyOptions;

public interface XMPProperty {
  String getValue();
  
  PropertyOptions getOptions();
  
  String getLanguage();
}

