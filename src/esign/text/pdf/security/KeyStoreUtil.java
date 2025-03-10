package esign.text.pdf.security;

import esign.text.ExceptionConverter;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class KeyStoreUtil {

    public static KeyStore loadCacertsKeyStore(String provider) {
        File file = new File(System.getProperty("java.home"), "lib");
        file = new File(file, "security");
        file = new File(file, "cacerts");
        FileInputStream fin = null;
        try {
            KeyStore k;
            fin = new FileInputStream(file);

            if (provider == null) {
                k = KeyStore.getInstance("JKS");
            } else {
                k = KeyStore.getInstance("JKS", provider);
            }
            k.load(fin, null);
            return k;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        } finally {

            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (Exception exception) {
            }
        }
    }

    public static KeyStore loadCacertsKeyStore() {
        return loadCacertsKeyStore(null);
    }
}
