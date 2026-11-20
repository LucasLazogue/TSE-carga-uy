# escenario balanza caida: pide los pesajes de viajes al ritmo TASA/s y registra latencia y nodos sin responder
import json, hmac, hashlib, base64, time, os, sys, random, threading, urllib.request, subprocess
B = os.environ.get('CENTRAL', 'https://carga-uy-kevin-martinez-herna-dev.apps.rm2.thpm.p1.openshiftapps.com/carga-uy')
S = os.environ['JWT_SECRETO'].encode(); U = json.load(open(sys.argv[1])); SAL = sys.argv[2]
TASA = float(os.environ.get('TASA', '2'))
FASES = [('normal', 60), ('caida', 240), ('restaurada', 120)]
NORTE_OK = os.environ['NORTE_URL']; NORTE_CAIDA = 'https://10.255.255.1/nodo-balanzas'
def b64(x): return base64.urlsafe_b64encode(x).rstrip(b'=').decode()
def token(u):
    now = int(time.time()); h = b64(json.dumps({'alg': 'HS256', 'typ': 'JWT'}).encode())
    c = b64(json.dumps({'iss': 'carga-uy', 'sub': str(u['id']), 'typ': 'sesion', 'iat': now, 'exp': now + 7200, 'cedula': u['cedula'], 'roles': ['CIUDADANO', 'RESPONSABLE']}).encode())
    return f'{h}.{c}.' + b64(hmac.new(S, f'{h}.{c}'.encode(), hashlib.sha256).digest())
def get(path, tok):
    req = urllib.request.Request(B + path, headers={'Authorization': 'Bearer ' + tok}); t = time.time()
    try:
        with urllib.request.urlopen(req, timeout=90) as r: body = r.read(); code = r.status
    except urllib.error.HTTPError as e: body = b''; code = e.code
    except Exception as e: body = b''; code = 0
    return code, (time.time() - t) * 1000, body
def psql(sql):
    p = subprocess.run(['sh', '-c', 'P=$(oc get pods -l deploymentconfig=postgres -o name | head -1); oc exec -i $P -- sh -c \'psql -U "$POSTGRESQL_USER" -d "$POSTGRESQL_DATABASE" -At\''], input=sql, text=True, capture_output=True)
    return p.stdout.strip()
def apuntar(url): print('norte ->', 'CAIDA' if url == NORTE_CAIDA else 'ok', psql(f"UPDATE nodoperiferico SET puntoacceso='{url}' WHERE identificador='balanzas-norte';"), flush=True)
# viajes de algunas empresas
objetivos = []
for r in random.sample(U['responsables'], 10):
    tok = token(r)
    for estado in ('EN_CURSO', 'FINALIZADO'):
        code, ms, body = get(f"/api/empresas/{r['empresa']}/viajes?estado={estado}&pagina=0&tamanio=10", tok)
        if code == 200: objetivos += [(r['empresa'], v['id'], tok) for v in json.loads(body)['items']]
if not objetivos: sys.exit('sin viajes en curso ni terminados')
print('viajes', len(objetivos), flush=True)
fase = ['normal']; filas = []; lock = threading.Lock()
def pedir():
    e, v, tok = random.choice(objetivos); t0 = time.time(); f = fase[0]
    code, ms, body = get(f'/api/empresas/{e}/viajes/{v}/pesajes', tok)
    sin = ';'.join(json.loads(body).get('nodosSinResponder', [])) if code == 200 else ''
    with lock: filas.append((time.strftime('%H:%M:%S', time.localtime(t0)), f, code, round(ms), sin))
try:
    for nombre, dur in FASES:
        fase[0] = nombre
        if nombre == 'caida': apuntar(NORTE_CAIDA)
        if nombre == 'restaurada': apuntar(NORTE_OK)
        fin = time.time() + dur
        while time.time() < fin:
            threading.Thread(target=pedir, daemon=True).start(); time.sleep(1 / TASA)
finally:
    apuntar(NORTE_OK)
time.sleep(30)
with open(SAL, 'w') as f:
    f.write('hora,fase,codigo,ms,sin_responder\n')
    for x in sorted(filas): f.write(','.join(map(str, x)) + '\n')
print('filas', len(filas))
