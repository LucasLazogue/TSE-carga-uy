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
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const BASE = __ENV.BASE_URL || 'https://carga-uy-kevin-martinez-herna-dev.apps.rm2.thpm.p1.openshiftapps.com/carga-uy';
const SECRETO = __ENV.JWT_SECRETO;
// pedidos por segundo de cada tipo de usuario (cada iteracion hace varias consultas, ver abajo)
const TASA = Number(__ENV.TASA || 10);
const DURACION = __ENV.DURACION || '5m';
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

export const options = {
  scenarios: {
    responsable: escenario('responsable'),
    funcionario: escenario('funcionario'),
    publico: escenario('publico'),
  },
  thresholds: {
    // el SLO de la letra, por tipo de usuario
    'http_req_duration{scenario:responsable}': ['p(95)<500'],
    'http_req_duration{scenario:funcionario}': ['p(95)<500'],
    'http_req_duration{scenario:publico}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

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
