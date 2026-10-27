import fs from 'node:fs'
import { fileURLToPath } from 'node:url'

const carpeta = fileURLToPath(new URL('..', import.meta.url))

const nodos = [
  {
    nombre: 'balanzas-sur',
    url: 'http://localhost:8082/nodo-balanzas',
    claveAdmin: process.env.BALANZAS_SUR_CLAVE_ADMIN ?? 'clave-admin-balanzas-sur',
    inventario: 'datos/balanzas-sur.json',
    claves: 'claves/balanzas-sur.json',
  },
  {
    nombre: 'balanzas-norte',
    url: 'http://localhost:8083/nodo-balanzas',
    claveAdmin: process.env.BALANZAS_NORTE_CLAVE_ADMIN ?? 'clave-admin-balanzas-norte',
    inventario: 'datos/balanzas-norte.json',
    claves: 'claves/balanzas-norte.json',
  },
]

fs.mkdirSync(carpeta + 'claves', { recursive: true })

for (const nodo of nodos) {
  const peajes = JSON.parse(fs.readFileSync(carpeta + nodo.inventario, 'utf8'))
  let claves = {}
  if (fs.existsSync(carpeta + nodo.claves)) {
    claves = JSON.parse(fs.readFileSync(carpeta + nodo.claves, 'utf8'))
  }

  for (const peaje of peajes) {
    const { balanzas, ...datosPeaje } = peaje
    await enviar(nodo, '/api/peajes', datosPeaje)

    for (const balanza of balanzas) {
      const creada = await enviar(nodo, `/api/peajes/${peaje.codigo}/balanzas`, balanza)

      if (creada) {
        claves[creada.codigo] = creada.clave
        fs.writeFileSync(carpeta + nodo.claves, JSON.stringify(claves, null, 2))
        console.log(`${nodo.nombre}: alta de ${creada.codigo} en ${peaje.codigo}`)
      } else if (claves[balanza.codigo]) {
        console.log(`${nodo.nombre}: ${balanza.codigo} ya existia`)
      } else {
        console.log(`${nodo.nombre}: ${balanza.codigo} ya existia y no esta su clave en ${nodo.claves}`)
      }
    }
  }
}

async function enviar(nodo, camino, cuerpo) {
  let respuesta
  try {
    respuesta = await fetch(nodo.url + camino, {
      method: 'POST',
      headers: { Authorization: `Bearer ${nodo.claveAdmin}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(cuerpo),
    })
  } catch {
    console.log(`${nodo.nombre} no responde en ${nodo.url}, esta levantado?`)
    process.exit(1)
  }

  if (respuesta.status === 409) {
    return null
  }
  if (!respuesta.ok) {
    console.log(`${nodo.nombre} rechazo ${camino} (${respuesta.status}): ${await respuesta.text()}`)
    process.exit(1)
  }

  const texto = await respuesta.text()
  return texto ? JSON.parse(texto) : {}
}
