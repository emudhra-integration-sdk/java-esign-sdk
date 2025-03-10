package esign.text.xmp;

public interface XMPVersionInfo {
  int getMajor();
  
  int getMinor();
  
  int getMicro();
  
  int getBuild();
  
  boolean isDebug();
  
  String getMessage();
}

