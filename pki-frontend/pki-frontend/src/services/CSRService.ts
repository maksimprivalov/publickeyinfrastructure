import * as asn1js from 'asn1js'
import * as pkijs from 'pkijs'
import { toBase64, toPem } from '../utils/pemUtils'

/**
 * Генерация CSR (SHA256WithRSA)
 * ИТОГОВАЯ ПРАВИЛЬНАЯ ВЕРСИЯ
 */
export async function generateCSR({
	commonName,
	organization,
	email,
}: {
	commonName: string
	organization: string
	email?: string | null
}) {
	console.log('\n🔍 ========== CSR GENERATION START ==========')
	console.log('📝 Input parameters:')
	console.log('   commonName:', commonName)
	console.log('   organization:', organization)
	console.log('   email:', email || '(not provided)')

	// ✅ Нормализация входа
	const safeCommonName = (commonName || '').trim()
	const safeOrganization = (organization || '').trim()
	const safeEmail = (email || '').trim()

	if (!safeCommonName)
		throw new Error('Common Name (CN) обязателен для заполнения')
	if (!safeOrganization)
		throw new Error('Organization (O) обязательна для заполнения')

	try {
		// 1) Генерация ключей
		console.log('\n🔑 Step 1: Generating RSA key pair (2048 bit)...')
		const algorithm: RsaHashedKeyGenParams = {
			name: 'RSASSA-PKCS1-v1_5',
			modulusLength: 2048,
			publicExponent: new Uint8Array([1, 0, 1]), // 65537
			hash: { name: 'SHA-256' },
		}

		const keyPair = await crypto.subtle.generateKey(algorithm, true, [
			'sign',
			'verify',
		])
		console.log('✅ Key pair generated successfully')

		// 2) Объект CSR
		console.log('\n📦 Step 2: Creating CSR object...')
		const csr = new pkijs.CertificationRequest()

		// 3) Формируем subject КОРРЕКТНО (каждый атрибут — отдельный RDN)
		console.log('\n📋 Step 3: Building subject attributes...')
		const subject = new pkijs.RelativeDistinguishedNames({
			typesAndValues: [
				new pkijs.AttributeTypeAndValue({
					type: '2.5.4.3', // CN
					value: new asn1js.Utf8String({ value: safeCommonName }),
				}),
				new pkijs.AttributeTypeAndValue({
					type: '2.5.4.10', // O
					value: new asn1js.Utf8String({ value: safeOrganization }),
				}),
				new pkijs.AttributeTypeAndValue({
					type: '2.5.4.6', // C
					value: new asn1js.PrintableString({ value: 'RS' }),
				}),
				...(safeEmail !== ''
					? [
							new pkijs.AttributeTypeAndValue({
								type: '1.2.840.113549.1.9.1', // emailAddress
								value: new asn1js.IA5String({ value: safeEmail }),
							}),
					  ]
					: []),
			],
		})

		// ВАЖНО: присваиваем весь RDNSequence целиком
		csr.subject = subject

		console.log(
			'✅ Subject built with',
			csr.subject.typesAndValues.length,
			'attributes (separate RDNs)'
		)

		// 4) Публичный ключ
		console.log('\n🔑 Step 4: Importing public key...')
		await csr.subjectPublicKeyInfo.importKey(keyPair.publicKey)
		console.log('✅ Public key imported')

		// 5) Подпись CSR
		console.log(
			'\n✍️ Step 5: Signing CSR with private key (SHA-256 / RSASSA-PKCS1-v1_5)...'
		)
		await csr.sign(keyPair.privateKey, 'SHA-256')
		console.log('✅ CSR signed successfully')

		// (опц.) локальная проверка, чтобы отловить несовпадение байтов до отправки
		const localVerify = await csr.verify()
		if (!localVerify) {
			throw new Error(
				'Локальная проверка подписи CSR не прошла (csr.verify() === false). Проверьте сборку subject/DER.'
			)
		}
		console.log('🔎 Local verify OK')

		// 6) Конвертируем в PEM (строго DER)
		console.log('\n📄 Step 6: Converting CSR to PEM format...')
		const csrBER = csr.toSchema().toBER(false) // DER
		const csrBase64 = toBase64(csrBER)
		const csrPEM = toPem(csrBase64, 'CERTIFICATE REQUEST')

		console.log('✅ CSR PEM generated')
		console.log('📏 CSR PEM length:', csrPEM.length, 'characters')
		console.log('📝 CSR preview (first 100 chars):')
		console.log('   ', csrPEM.substring(0, 100) + '...')

		// 7) Экспорт приватного ключа (PKCS#8)
		console.log('\n🔐 Step 7: Exporting private key...')
		const privateKeyArrayBuffer = await crypto.subtle.exportKey(
			'pkcs8',
			keyPair.privateKey
		)
		const privateKeyBase64 = toBase64(privateKeyArrayBuffer)
		const privateKeyPEM = toPem(privateKeyBase64, 'PRIVATE KEY')

		console.log('✅ Private key PEM generated')
		console.log(
			'📏 Private key PEM length:',
			privateKeyPEM.length,
			'characters'
		)
		console.log('\n========== CSR GENERATION SUCCESS ==========\n')

		return {
			pem: csrPEM, // для обратной совместимости
			csr: csrPEM, // явное имя
			privateKey: privateKeyPEM,
			keyPair,
		}
	} catch (error: any) {
		console.error('\n❌ ========== CSR GENERATION FAILED ==========')
		console.error('Error type:', error?.constructor?.name)
		console.error('Error message:', error?.message)
		console.error('Error stack:', error?.stack)
		console.error('==============================================\n')
		throw error
	}
}
