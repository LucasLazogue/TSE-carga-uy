export function fecha(iso: string) {
  const [anio, mes, dia] = iso.slice(0, 10).split('-')
  return `${dia}/${mes}/${anio}`
}

export function fechaHora(iso: string) {
  return `${fecha(iso)} ${iso.slice(11, 16)}`
}
