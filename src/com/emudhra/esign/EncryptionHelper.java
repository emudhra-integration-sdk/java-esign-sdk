package com.emudhra.esign;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.emcastle.util.encoders.UrlBase64;

public class EncryptionHelper {

    private EncryptionHelper() {

    }

    private static final String PBKDF2WITHHMACSHA1 = "PBKDF2WithHmacSHA1";
    private static final String AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";

    public static String getEncryptedData(String data, String keyValue) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return encrypt(data, keyValue);
    }

    public static String getEncryptedData(String data) throws Exception {
        if (eSignUtility.isNullOrEmpty(data)) {
            throw new Exception("Invalid path data.");
        }
        return encrypt(data, eSignSettings.getEncryptionKey());
    }

    private static String encrypt(String data, String keyValue) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2WITHHMACSHA1);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(keyValue.toCharArray(), new byte[]{0x49, 0x76, 0x61, 0x6e, 0x20, 0x4d, 0x65, 0x64, 0x76, 0x65, 0x64, 0x65, 0x76}, 1000, 384);
            Key secretKey = factory.generateSecret(pbeKeySpec);

            byte[] key1 = new byte[32];
            byte[] iv = new byte[16];

            System.arraycopy(secretKey.getEncoded(), 0, key1, 0, 32);
            System.arraycopy(secretKey.getEncoded(), 32, iv, 0, 16);

            SecretKeySpec skeySpec = new SecretKeySpec(key1, AES);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String encrypteddata = new String(UrlBase64.encode(encrypted));
            return encrypteddata;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            return null;
        }
    }

    public static String getDencryptedData(String encrypted, String key) {
        return decrypt(encrypted, key);
    }

    public static String getDencryptedData(String encrypted) throws Exception {
        if (eSignUtility.isNullOrEmpty(encrypted)) {
            throw new Exception("Encrypted data cannot be empty.");
        }
        return decrypt(encrypted, eSignSettings.getEncryptionKey());
    }

    private static String decrypt(String encrypted, String key) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2WITHHMACSHA1);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(key.toCharArray(), new byte[]{0x49, 0x76, 0x61, 0x6e, 0x20, 0x4d, 0x65, 0x64, 0x76, 0x65, 0x64, 0x65, 0x76}, 1000, 384);

            Key secretKey = factory.generateSecret(pbeKeySpec);
            byte[] key1 = new byte[32];
            byte[] iv = new byte[16];
            System.arraycopy(secretKey.getEncoded(), 0, key1, 0, 32);
            System.arraycopy(secretKey.getEncoded(), 32, iv, 0, 16);

            SecretKeySpec skeySpec = new SecretKeySpec(key1, AES);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            byte[] data = UrlBase64.decode(encrypted);
            byte[] original = cipher.doFinal(data);

            return new String(original, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            return null;
        }
    }
}
