export const CEDULA_DIGITOS = 8

export function onlyDigits(value: string) {
  return value.replace(/\D/g, '').slice(0, CEDULA_DIGITOS)
}

export function formatCedula(digits: string) {
  const parts = [digits.slice(0, 1), digits.slice(1, 4), digits.slice(4, 7)].filter(Boolean).join('.')
  return digits.length > 7 ? `${parts}-${digits.slice(7)}` : parts
}
