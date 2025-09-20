package com.app.pki_backend.service.implementations;

import com.app.pki_backend.service.interfaces.CryptographyService;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.RSAKeyGenParameterSpec;

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


}
