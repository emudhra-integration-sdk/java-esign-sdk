 package esign.text.pdf.languages;
 
 import esign.text.pdf.Glyph;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Map;
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 public class BanglaGlyphRepositioner
   extends IndicGlyphRepositioner
 {
   private static final String[] CHARCTERS_TO_BE_SHIFTED_LEFT_BY_1 = new String[] { "ি", "ে", "ৈ" };
   
   private final Map<Integer, int[]> cmap31;
   
   private final Map<String, Glyph> glyphSubstitutionMap;
   
   public BanglaGlyphRepositioner(Map<Integer, int[]> cmap31, Map<String, Glyph> glyphSubstitutionMap) {
     this.cmap31 = cmap31;
     this.glyphSubstitutionMap = glyphSubstitutionMap;
   }
 
 
   
   public void repositionGlyphs(List<Glyph> glyphList) {
     for (int i = 0; i < glyphList.size(); i++) {
       Glyph glyph = glyphList.get(i);
       
       if (glyph.chars.equals("ো")) {
         handleOKaarAndOUKaar(i, glyphList, 'ে', 'া');
       } else if (glyph.chars.equals("ৌ")) {
         handleOKaarAndOUKaar(i, glyphList, 'ে', 'ৗ');
       } 
     } 
     
     super.repositionGlyphs(glyphList);
   }
 
   
   public List<String> getCharactersToBeShiftedLeftByOnePosition() {
     return Arrays.asList(CHARCTERS_TO_BE_SHIFTED_LEFT_BY_1);
   }
 
 
 
 
 
 
   
   private void handleOKaarAndOUKaar(int currentIndex, List<Glyph> glyphList, char first, char second) {
     Glyph g1 = getGlyph(first);
     Glyph g2 = getGlyph(second);
     glyphList.set(currentIndex, g1);
     glyphList.add(currentIndex + 1, g2);
   }
 
   
   private Glyph getGlyph(char c) {
     Glyph glyph = this.glyphSubstitutionMap.get(String.valueOf(c));
     
     if (glyph != null) {
       return glyph;
     }
     
     int[] metrics = this.cmap31.get(Integer.valueOf(c));
     int glyphCode = metrics[0];
     int glyphWidth = metrics[1];
     return new Glyph(glyphCode, glyphWidth, String.valueOf(c));
   }
 }

