package com.jvault.jvault.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String aes = "AES";

    private final Key key;
    private final Cipher cipher;

    public EncryptionConverter(
            @Value("${application.security.encryption.key}") String secret
    ) throws Exception {
        this.key = new SecretKeySpec(secret.getBytes(), aes);
        this.cipher = Cipher.getInstance(aes);
    }

    @Override
    public String convertToDatabaseColumn(String s) {
        if(s == null)
            return null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(s.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e){
            throw new RuntimeException("Eroare la criptarea datelor");
        }
    }

    @Override
    public String convertToEntityAttribute(String s) {
        if(s == null)
            return null;
        try{
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(s);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e){
            throw new RuntimeException("Eroare la decriptarea datelor");
        }
    }
}
