package esign.text.xmp;

import esign.text.xmp.options.IteratorOptions;
import esign.text.xmp.options.ParseOptions;
import esign.text.xmp.options.PropertyOptions;
import esign.text.xmp.properties.XMPProperty;
import java.util.Calendar;

public interface XMPMeta extends Cloneable {
  XMPProperty getProperty(String paramString1, String paramString2) throws XMPException;
  
  XMPProperty getArrayItem(String paramString1, String paramString2, int paramInt) throws XMPException;
  
  int countArrayItems(String paramString1, String paramString2) throws XMPException;
  
  XMPProperty getStructField(String paramString1, String paramString2, String paramString3, String paramString4) throws XMPException;
  
  XMPProperty getQualifier(String paramString1, String paramString2, String paramString3, String paramString4) throws XMPException;
  
  void setProperty(String paramString1, String paramString2, Object paramObject, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setProperty(String paramString1, String paramString2, Object paramObject) throws XMPException;
  
  void setArrayItem(String paramString1, String paramString2, int paramInt, String paramString3, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setArrayItem(String paramString1, String paramString2, int paramInt, String paramString3) throws XMPException;
  
  void insertArrayItem(String paramString1, String paramString2, int paramInt, String paramString3, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void insertArrayItem(String paramString1, String paramString2, int paramInt, String paramString3) throws XMPException;
  
  void appendArrayItem(String paramString1, String paramString2, PropertyOptions paramPropertyOptions1, String paramString3, PropertyOptions paramPropertyOptions2) throws XMPException;
  
  void appendArrayItem(String paramString1, String paramString2, String paramString3) throws XMPException;
  
  void setStructField(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setStructField(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5) throws XMPException;
  
  void setQualifier(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setQualifier(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5) throws XMPException;
  
  void deleteProperty(String paramString1, String paramString2);
  
  void deleteArrayItem(String paramString1, String paramString2, int paramInt);
  
  void deleteStructField(String paramString1, String paramString2, String paramString3, String paramString4);
  
  void deleteQualifier(String paramString1, String paramString2, String paramString3, String paramString4);
  
  boolean doesPropertyExist(String paramString1, String paramString2);
  
  boolean doesArrayItemExist(String paramString1, String paramString2, int paramInt);
  
  boolean doesStructFieldExist(String paramString1, String paramString2, String paramString3, String paramString4);
  
  boolean doesQualifierExist(String paramString1, String paramString2, String paramString3, String paramString4);
  
  XMPProperty getLocalizedText(String paramString1, String paramString2, String paramString3, String paramString4) throws XMPException;
  
  void setLocalizedText(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setLocalizedText(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5) throws XMPException;
  
  Boolean getPropertyBoolean(String paramString1, String paramString2) throws XMPException;
  
  Integer getPropertyInteger(String paramString1, String paramString2) throws XMPException;
  
  Long getPropertyLong(String paramString1, String paramString2) throws XMPException;
  
  Double getPropertyDouble(String paramString1, String paramString2) throws XMPException;
  
  XMPDateTime getPropertyDate(String paramString1, String paramString2) throws XMPException;
  
  Calendar getPropertyCalendar(String paramString1, String paramString2) throws XMPException;
  
  byte[] getPropertyBase64(String paramString1, String paramString2) throws XMPException;
  
  String getPropertyString(String paramString1, String paramString2) throws XMPException;
  
  void setPropertyBoolean(String paramString1, String paramString2, boolean paramBoolean, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setPropertyBoolean(String paramString1, String paramString2, boolean paramBoolean) throws XMPException;
  
  void setPropertyInteger(String paramString1, String paramString2, int paramInt, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setPropertyInteger(String paramString1, String paramString2, int paramInt) throws XMPException;
  
  void setPropertyLong(String paramString1, String paramString2, long paramLong, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setPropertyLong(String paramString1, String paramString2, long paramLong) throws XMPException;
  
  void setPropertyDouble(String paramString1, String paramString2, double paramDouble, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setPropertyDouble(String paramString1, String paramString2, double paramDouble) throws XMPException;
  
  void setPropertyDate(String paramString1, String paramString2, XMPDateTime paramXMPDateTime, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setPropertyDate(String paramString1, String paramString2, XMPDateTime paramXMPDateTime) throws XMPException;
  
  void setPropertyCalendar(String paramString1, String paramString2, Calendar paramCalendar, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setPropertyCalendar(String paramString1, String paramString2, Calendar paramCalendar) throws XMPException;
  
  void setPropertyBase64(String paramString1, String paramString2, byte[] paramArrayOfbyte, PropertyOptions paramPropertyOptions) throws XMPException;
  
  void setPropertyBase64(String paramString1, String paramString2, byte[] paramArrayOfbyte) throws XMPException;
  
  XMPIterator iterator() throws XMPException;
  
  XMPIterator iterator(IteratorOptions paramIteratorOptions) throws XMPException;
  
  XMPIterator iterator(String paramString1, String paramString2, IteratorOptions paramIteratorOptions) throws XMPException;
  
  String getObjectName();
  
  void setObjectName(String paramString);
  
  String getPacketHeader();
  
  Object clone();
  
  void sort();
  
  void normalize(ParseOptions paramParseOptions) throws XMPException;
  
  String dumpObject();
}


/* Location:              D:\test\tp\itextpdf-5.5.10.jar!\com\itextpdf\xmp\XMPMeta.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */
