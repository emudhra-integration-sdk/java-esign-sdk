package esign.text.xmp.properties;

import esign.text.xmp.options.AliasOptions;

public interface XMPAliasInfo {
  String getNamespace();
  
  String getPrefix();
  
  String getPropName();
  
  AliasOptions getAliasForm();
}

