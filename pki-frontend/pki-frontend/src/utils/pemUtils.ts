export function toBase64(buffer: any) {
  let binary = '';
  const bytes = new Uint8Array(buffer);
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const chunk = bytes.subarray(i, i + chunkSize);
    binary += String.fromCharCode(...chunk);
  }
  return btoa(binary);
}

export function toPem(base64: any, label: any) {
  let formatted = base64.match(/.{1,64}/g).join('\n');
  return `-----BEGIN ${label}-----\n${formatted}\n-----END ${label}-----`;
}
