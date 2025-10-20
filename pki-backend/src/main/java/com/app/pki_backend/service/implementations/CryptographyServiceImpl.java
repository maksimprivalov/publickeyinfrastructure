package com.app.pki_backend.service.implementations;

import com.app.pki_backend.service.interfaces.CryptographyService;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;

@Service
public class CryptographyServiceImpl implements CryptographyService {

    @Override
    public KeyPair generateKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            // Use RSAKeyGenParameterSpec for secure key generation
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4);
            keyPairGenerator.initialize(spec, new SecureRandom());

            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }

    @Override
    public BigInteger generateSerialNumber() {
        try {
            byte[] serialBytes = new byte[20];
            new SecureRandom().nextBytes(serialBytes);

            // must be positive
            serialBytes[0] &= 0x7F;

            return new BigInteger(serialBytes);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate serial number", e);
        }
    }

    @Override
    public SubjectKeyIdentifier createSubjectKeyIdentifier(SubjectPublicKeyInfo publicKeyInfo) {
        try {
            return new BcX509ExtensionUtils().createSubjectKeyIdentifier(publicKeyInfo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Subject Key Identifier", e);
        }
    }

    @Override
    public SecretKey generateAESKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    @Override
    public String encryptPrivateKey(PrivateKey privateKey, SecretKey encryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);

            byte[] privateKeyBytes = privateKey.getEncoded();
            byte[] encryptedData = cipher.doFinal(privateKeyBytes);
            byte[] iv = cipher.getIV();

            // Combine IV + encrypted data
            byte[] result = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt private key", e);
        }
    }

    @Override
    public PrivateKey decryptPrivateKey(String encryptedData, SecretKey encryptionKey) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedData);

            byte[] iv = new byte[12];
            System.arraycopy(data, 0, iv, 0, 12);

            byte[] encrypted = new byte[data.length - 12];
            System.arraycopy(data, 12, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);

            byte[] decryptedData = cipher.doFinal(encrypted);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedData);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt private key", e);
        }
    }
}
