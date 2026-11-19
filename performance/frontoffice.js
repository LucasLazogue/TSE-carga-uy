// Usuarios simulados del frontoffice para la prueba de performance (seccion 4.3 de la letra): consultas de
// responsables de empresa, funcionarios y publico mientras el generador de tracking carga la ingesta.
// El objetivo es p95 < 500 ms en las consultas del frontoffice (AC09). Ver "Usuarios simulados del frontoffice"
// en docs/performance.md.
//
// Los tokens no pasan por gub.uy: se firman aca con el mismo secreto que usa el central (CARGAUY_JWT_SECRET), con
// las mismas claims que firma UsuarioEJB. Los usuarios salen de performance/usuarios.sh, que los lee de la base.
//
//   k6 run -e JWT_SECRETO="$CARGAUY_JWT_SECRET" -e TASA=20 -e DURACION=10m performance/frontoffice.js
import http from 'k6/http';
import { check, group } from 'k6';
import exec from 'k6/execution';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const BASE = __ENV.BASE_URL || 'https://carga-uy-kevin-martinez-herna-dev.apps.rm2.thpm.p1.openshiftapps.com/carga-uy';
const SECRETO = __ENV.JWT_SECRETO;
// pedidos por segundo de cada tipo de usuario (cada iteracion hace varias consultas, ver abajo)
const TASA = Number(__ENV.TASA || 10);
const DURACION = __ENV.DURACION || '5m';
// choferes que terminan su viaje por segundo desde la app movil (0 = sin escenario movil). Con 18.000 vehiculos y
// viajes de unas 5 horas, termina un viaje por segundo. Ver "Escenario movil" en docs/performance.md
const TASA_MOVIL = Number(__ENV.TASA_MOVIL || 0);
// primer vehiculo SIM que usa el escenario movil: cada iteracion toma el siguiente. En una segunda corrida sobre la
// misma carga de datos, arrancar despues de los que ya se usaron
const MOVIL_DESDE = Number(__ENV.MOVIL_DESDE || 1);
const VEHICULOS = Number(__ENV.VEHICULOS || 18000);
const USUARIOS = JSON.parse(open(__ENV.USUARIOS || './usuarios.json'));

if (!SECRETO) {
  throw new Error('falta JWT_SECRETO (el CARGAUY_JWT_SECRET del central)');
}

const escenario = (funcion) => ({
  executor: 'constant-arrival-rate',
  exec: funcion,
  rate: TASA,
  timeUnit: '1s',
  duration: DURACION,
  preAllocatedVUs: Math.max(10, TASA * 2),
  maxVUs: TASA * 20,
});

const escenarios = {
  responsable: escenario('responsable'),
  funcionario: escenario('funcionario'),
  publico: escenario('publico'),
};
if (TASA_MOVIL > 0) {
  escenarios.movil = { ...escenario('movil'), rate: TASA_MOVIL };
}

export const options = {
  scenarios: escenarios,
  thresholds: {
    // el SLO de la letra, por tipo de usuario
    'http_req_duration{scenario:responsable}': ['p(95)<500'],
    'http_req_duration{scenario:funcionario}': ['p(95)<500'],
    'http_req_duration{scenario:publico}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};
if (TASA_MOVIL > 0) {
  // el fin y el inicio se tienen que aplicar: un rechazo indica un problema de datos o de concurrencia
  options.thresholds['checks{scenario:movil}'] = ['rate>0.99'];
}

const b64 = (texto) => encoding.b64encode(texto, 'rawurl');

// HS256 con iss, sub, typ, iat y exp, igual que tse.cargauy.seguridad.Jwt
function token(usuario, roles) {
  const ahora = Math.floor(Date.now() / 1000);
  const cabecera = b64(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const cuerpo = b64(JSON.stringify({
    iss: 'carga-uy', sub: String(usuario.id), typ: 'sesion', iat: ahora, exp: ahora + 6 * 3600,
    cedula: usuario.cedula, roles,
  }));
  const firma = crypto.hmac('sha256', SECRETO, `${cabecera}.${cuerpo}`, 'base64rawurl');
  return `${cabecera}.${cuerpo}.${firma}`;
}

export function setup() {
  if (!USUARIOS.responsables.length || !USUARIOS.funcionarios.length) {
    throw new Error('usuarios.json sin responsables o sin funcionarios: correr performance/usuarios.sh');
  }
  return {
    responsables: USUARIOS.responsables.map((u) => ({ ...u, token: token(u, ['CIUDADANO', 'RESPONSABLE']) })),
    funcionarios: USUARIOS.funcionarios.map((u) => ({ ...u, token: token(u, ['FUNCIONARIO']) })),
    empresas: USUARIOS.nrosEmpresa,
  };
}

const alAzar = (lista) => lista[Math.floor(Math.random() * lista.length)];
const conToken = (t, nombre) => ({ headers: { Authorization: `Bearer ${t}` }, tags: { name: nombre } });
const ok = (r) => check(r, { 'respuesta 200': (x) => x.status === 200 });

// lo que hace un responsable al entrar: sus viajes, el detalle de uno con sus eventos, sus vehiculos e infracciones
export function responsable(datos) {
  const u = alAzar(datos.responsables);
  group('responsable', () => {
    const viajes = http.get(`${BASE}/api/empresas/${u.empresa}/viajes?pagina=0&tamanio=20`, conToken(u.token, 'viajes'));
    ok(viajes);
    const lista = viajes.status === 200 ? (viajes.json('items') || []) : [];
    if (lista.length) {
      const id = alAzar(lista).id;
      ok(http.get(`${BASE}/api/empresas/${u.empresa}/viajes/${id}`, conToken(u.token, 'viaje')));
      ok(http.get(`${BASE}/api/empresas/${u.empresa}/viajes/${id}/eventos?pagina=0&tamanio=20`, conToken(u.token, 'eventos')));
    }
    ok(http.get(`${BASE}/api/vehiculos?idEmpresa=${u.empresa}&pagina=0&tamanio=20`, conToken(u.token, 'vehiculos')));
    ok(http.get(`${BASE}/api/infracciones?empresa=${u.empresa}&pagina=0&tamanio=20`, conToken(u.token, 'infracciones')));
  });
}

// el funcionario recorre empresas, vehiculos y casos de fiscalizacion
export function funcionario(datos) {
  const u = alAzar(datos.funcionarios);
  group('funcionario', () => {
    ok(http.get(`${BASE}/api/empresas?pagina=0&tamanio=20`, conToken(u.token, 'empresas')));
    ok(http.get(`${BASE}/api/vehiculos?pagina=0&tamanio=20`, conToken(u.token, 'vehiculos')));
    ok(http.get(`${BASE}/api/casos?pagina=0&tamanio=20`, conToken(u.token, 'casos')));
  });
}

// consultas publicas, sin sesion: validar una empresa y los indicadores del año
export function publico(datos) {
  group('publico', () => {
    const anio = new Date().getFullYear();
    ok(http.get(`${BASE}/api/publico/empresas/habilitacion?nro=${alAzar(datos.empresas)}`, { tags: { name: 'habilitacion' } }));
    ok(http.get(`${BASE}/api/publico/indicadores?desde=${anio}-01-01&hasta=${anio}-12-31`, { tags: { name: 'indicadores' } }));
  });
}

// uuid v4 como el que genera la app para cada evento
function uuid() {
  const b = new Uint8Array(crypto.randomBytes(16));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  const h = Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

// el chofer de un vehiculo SIM termina el viaje en curso e inicia el siguiente, como la app: pide sus viajes y manda
// los dos eventos en un mismo envio. Al terminar el viaje, el central lo evalua en segundo plano y le pide los pesajes
// a los nodos de balanzas. Los ids salen de performance/datos.sql: el chofer del vehiculo n es el usuario 11000000 + n
export function movil() {
  const n = MOVIL_DESDE + exec.scenario.iterationInTest;
  if (n > VEHICULOS) {
    return;
  }
  const chofer = { id: 11000000 + n, cedula: String(71000000 + n) };
  const t = token(chofer, ['CHOFER']);
  group('movil', () => {
    const viajes = http.get(`${BASE}/api/movil/viajes`, conToken(t, 'movil viajes'));
    ok(viajes);
    // cada elemento es { viaje, guia }
    const lista = viajes.status === 200 ? viajes.json().map((x) => x.viaje) : [];
    const enCurso = lista.find((v) => v.estado === 'EN_CURSO');
    const siguiente = lista.find((v) => v.estado === 'ASIGNADO');
    const ahora = Date.now();
    const eventos = [];
    if (enCurso) {
      eventos.push({ idEvento: uuid(), idViaje: enCurso.id, tipo: 'FIN', timestampGeneracion: new Date(ahora).toISOString() });
    }
    if (siguiente) {
      eventos.push({ idEvento: uuid(), idViaje: siguiente.id, tipo: 'INICIO', timestampGeneracion: new Date(ahora + 1000).toISOString() });
    }
    if (!eventos.length) {
      return;
    }
    const sync = http.post(`${BASE}/api/movil/sync`, JSON.stringify(eventos),
      { headers: { Authorization: `Bearer ${t}`, 'Content-Type': 'application/json' }, tags: { name: 'movil sync' } });
    check(sync, {
      'sync 200': (x) => x.status === 200,
      'eventos aplicados': (x) => x.status === 200
        && (x.json('resultados') || []).every((r) => r.resultado === 'APLICADO'),
    });
  });
}

