package com.app.pki_backend.service.interfaces;

import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.security.KeyPair;
import java.math.BigInteger;

/**
 * Service interface for low-level cryptographic operations.
 * Handles only pure cryptographic functions, not business logic.
 */
public interface CryptographyService {

    /**
     * Generate a new key pair with the specified key size.
     * @param keySize Key size in bits.
     * @return Key pair containing public and private keys.
     */
    KeyPair generateKeyPair(int keySize);

    /**
     * Generate a unique serial number for certificates.
     * @return A unique serial number as a BigInteger.
     */
    BigInteger generateSerialNumber();

    /**
     * Creation Subject Key Identifier for sertificate.
     * @param publicKeyInfo Public key info.
     * @return Subject Key Identifier.
     */
    SubjectKeyIdentifier createSubjectKeyIdentifier(SubjectPublicKeyInfo publicKeyInfo);

}
