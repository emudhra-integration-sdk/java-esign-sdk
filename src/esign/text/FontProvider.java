package esign.text;

public interface FontProvider {

    boolean isRegistered(String paramString);

    Font getFont(String paramString1, String paramString2, boolean paramBoolean, float paramFloat, int paramInt, BaseColor paramBaseColor);
}
