import fs from 'node:fs'
import { fileURLToPath } from 'node:url'

const carpeta = fileURLToPath(new URL('..', import.meta.url))

const nodos = [
  {
    nombre: 'balanzas-sur',
    url: 'http://localhost:8082/nodo-balanzas',
    pasadas: 'datos/pasadas-sur.json',
    claves: 'claves/balanzas-sur.json',
  },
  {
    nombre: 'balanzas-norte',
    url: 'http://localhost:8083/nodo-balanzas',
    pasadas: 'datos/pasadas-norte.json',
    claves: 'claves/balanzas-norte.json',
  },
]

for (const nodo of nodos) {
  if (!fs.existsSync(carpeta + nodo.claves)) {
    console.log(`no esta ${nodo.claves}, primero hay que correr alta.mjs`)
    process.exit(1)
  }

  const pasadas = JSON.parse(fs.readFileSync(carpeta + nodo.pasadas, 'utf8'))
  const claves = JSON.parse(fs.readFileSync(carpeta + nodo.claves, 'utf8'))

  for (const pasada of pasadas) {
    const clave = claves[pasada.balanza]
    if (!clave) {
      console.log(`no hay clave para ${pasada.balanza} en ${nodo.claves}`)
      process.exit(1)
    }

    for (const lectura of pasada.lecturas) {
      const fecha = lectura.fecha ?? fechaDeHace(lectura.dias)
      const pesaje = {
        idRegistro: lectura.idRegistro,
        matricula: lectura.matricula,
        fechaHora: `${fecha}T${lectura.hora}:00`,
        pesoRegistrado: lectura.pesoRegistrado,
      }

      let respuesta
      try {
        respuesta = await fetch(nodo.url + '/api/pesajes', {
          method: 'POST',
          headers: { Authorization: `Bearer ${clave}`, 'Content-Type': 'application/json' },
          body: JSON.stringify([pesaje]),
        })
      } catch {
        console.log(`${nodo.nombre} no responde en ${nodo.url}, esta levantado?`)
        process.exit(1)
      }

      if (respuesta.status !== 204) {
        console.log(`${pasada.balanza} no pudo enviar ${lectura.idRegistro} (${respuesta.status}): ${await respuesta.text()}`)
        process.exit(1)
      }

      console.log(`${pasada.balanza} envio ${pesaje.matricula} ${pesaje.fechaHora} ${pesaje.pesoRegistrado} kg`)
    }
  }
}

function fechaDeHace(dias) {
  const dia = new Date()
  dia.setDate(dia.getDate() + dias)
  const mes = String(dia.getMonth() + 1).padStart(2, '0')
  const numero = String(dia.getDate()).padStart(2, '0')
  return `${dia.getFullYear()}-${mes}-${numero}`
}
