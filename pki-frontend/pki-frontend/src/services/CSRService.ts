import * as asn1js from "asn1js";
import * as pkijs from "pkijs";
import { fromBER } from "asn1js";
import { toBase64, toPem } from "../utils/pemUtils"

// Генерация CSR (SHA256WithRSA)
export async function generateCSR({ commonName, organization, email }) {
  // 1️⃣ Генерируем RSA ключи (2048 бит)
  const algorithm = {
    name: "RSASSA-PKCS1-v1_5",
    modulusLength: 2048,
    publicExponent: new Uint8Array([1, 0, 1]),
    hash: "SHA-256"
  };

  const keyPair = await crypto.subtle.generateKey(algorithm, true, ["sign", "verify"]);

  // 2️⃣ Формируем subject: CN, O, C=RS, E
  const attributes = [
    new pkijs.AttributeTypeAndValue({
      type: "2.5.4.3", // CN
      value: new asn1js.PrintableString({ value: commonName })
    }),
    new pkijs.AttributeTypeAndValue({
      type: "2.5.4.10", // O
      value: new asn1js.PrintableString({ value: organization })
    }),
    new pkijs.AttributeTypeAndValue({
      type: "2.5.4.6", // C
      value: new asn1js.PrintableString({ value: "RS" })
    }),
    new pkijs.AttributeTypeAndValue({
      type: "1.2.840.113549.1.9.1", // emailAddress
      value: new asn1js.IA5String({ value: email })
    })
  ];

  // 3️⃣ Создаём объект CSR
  const csr = new pkijs.CertificationRequest();

  csr.subject.typesAndValues.push(...attributes);

  await csr.subjectPublicKeyInfo.importKey(keyPair.publicKey);

  // 4️⃣ Подписываем CSR
  await csr.sign(keyPair.privateKey, "SHA-256");

  // 5️⃣ Конвертируем в PEM
  const ber = csr.toSchema().toBER(false);
  const base64 = toBase64(ber);
  const pem = toPem(base64, "CERTIFICATE REQUEST");

  return { pem, keyPair };
}
