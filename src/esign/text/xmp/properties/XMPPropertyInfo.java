package esign.text.xmp.properties;

import esign.text.xmp.options.PropertyOptions;

public interface XMPPropertyInfo extends XMPProperty {
  String getNamespace();
  
  String getPath();
  
  String getValue();
  
  PropertyOptions getOptions();
}

