package esign.text.xmp;

import esign.text.xmp.properties.XMPAliasInfo;
import java.util.Map;

public interface XMPSchemaRegistry {
  String registerNamespace(String paramString1, String paramString2) throws XMPException;
  
  String getNamespacePrefix(String paramString);
  
  String getNamespaceURI(String paramString);
  
  Map getNamespaces();
  
  Map getPrefixes();
  
  void deleteNamespace(String paramString);
  
  XMPAliasInfo resolveAlias(String paramString1, String paramString2);
  
  XMPAliasInfo[] findAliases(String paramString);
  
  XMPAliasInfo findAlias(String paramString);
  
  Map getAliases();
}

