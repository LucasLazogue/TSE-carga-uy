import json, hmac, hashlib, base64, time, os, sys, urllib.request
B = os.environ.get('CENTRAL', 'https://carga-uy-kevin-martinez-herna-dev.apps.rm2.thpm.p1.openshiftapps.com/carga-uy')
S = os.environ['JWT_SECRETO'].encode()
U = json.load(open(sys.argv[1]))
def b64(x): return base64.urlsafe_b64encode(x).rstrip(b'=').decode()
def token(sub, cedula, roles):
    now = int(time.time())
    h = b64(json.dumps({'alg': 'HS256', 'typ': 'JWT'}).encode())
    c = b64(json.dumps({'iss': 'carga-uy', 'sub': str(sub), 'typ': 'sesion', 'iat': now, 'exp': now + 3600, 'cedula': cedula, 'roles': roles}).encode())
    return f'{h}.{c}.' + b64(hmac.new(S, f'{h}.{c}'.encode(), hashlib.sha256).digest())
r = U['responsables'][0]; f = U['funcionarios'][0]
tr = token(r['id'], r['cedula'], ['CIUDADANO', 'RESPONSABLE']); tf = token(f['id'], f['cedula'], ['FUNCIONARIO'])
def get(path, tok=None):
    req = urllib.request.Request(B + path, headers={'Authorization': 'Bearer ' + tok} if tok else {})
    t = time.time()
    try:
        with urllib.request.urlopen(req, timeout=60) as resp: body = resp.read(); code = resp.status
    except urllib.error.HTTPError as e: body = b''; code = e.code
    return code, (time.time() - t) * 1000, body
e = r['empresa']
code, ms, body = get(f'/api/empresas/{e}/viajes?pagina=0&tamanio=20', tr)
vid = json.loads(body)['items'][0]['id'] if code == 200 else 0
casos = [
  ('responsable viajes', f'/api/empresas/{e}/viajes?pagina=0&tamanio=20', tr),
  ('responsable viaje', f'/api/empresas/{e}/viajes/{vid}', tr),
  ('responsable eventos', f'/api/empresas/{e}/viajes/{vid}/eventos?pagina=0&tamanio=20', tr),
  ('responsable vehiculos', f'/api/vehiculos?idEmpresa={e}&pagina=0&tamanio=20', tr),
  ('responsable infracciones', f'/api/infracciones?empresa={e}&pagina=0&tamanio=20', tr),
  ('funcionario empresas', '/api/empresas?pagina=0&tamanio=20', tf),
  ('funcionario vehiculos', '/api/vehiculos?pagina=0&tamanio=20', tf),
  ('funcionario casos', '/api/casos?pagina=0&tamanio=20', tf),
  ('publico habilitacion', f"/api/publico/empresas/habilitacion?nro={U['nrosEmpresa'][0]}", None),
  ('publico indicadores', '/api/publico/indicadores?desde=2026-01-01&hasta=2026-12-31', None),
]
for nombre, path, tok in casos:
    ts = []
    for _ in range(3):
        code, ms, _b = get(path, tok); ts.append(ms)
    print(f'{nombre:26} {code} ' + ' '.join(f'{x:7.0f}' for x in ts) + ' ms')
