# escenario PDI que no responde (compose local con GUBUY_MOCK=true): 1 ingreso nuevo por segundo, que consulta la PDI,
# y una consulta publica por segundo para ver que el resto del central no se afecta. La PDI se pausa (docker pause):
# la conexion se acepta pero nunca llega respuesta
import time, threading, urllib.request, urllib.parse, http.cookiejar, subprocess, sys
B = 'http://localhost:8080/carga-uy'; SAL = sys.argv[1]; CONT = 'pdiprueba-pdi-suplantador-1'
FASES = [('normal', 60), ('caida', 180), ('restaurada', 90)]
filas = []; lock = threading.Lock(); fase = ['normal']; n = [41000000]
class SinRedireccion(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k): return None
def ingreso():
    with lock: n[0] += 1; ced = str(n[0])
    jar = http.cookiejar.CookieJar(); op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar), SinRedireccion)
    f = fase[0]; t0 = time.time(); code = 0
    try:
        try: op.open(f'{B}/api/auth/login?cedula={ced}', timeout=60)
        except urllib.error.HTTPError as e: url = e.headers.get('Location')
        t1 = time.time()
        try: r = op.open(url, timeout=60); code = r.status
        except urllib.error.HTTPError as e: code = e.code
        ms = (time.time() - t1) * 1000
    except Exception as e: ms = (time.time() - t0) * 1000; code = 'error'
    with lock: filas.append((time.strftime('%H:%M:%S', time.localtime(t0)), f, 'ingreso', code, round(ms)))
def publica():
    f = fase[0]; t0 = time.time()
    try:
        with urllib.request.urlopen(f'{B}/api/publico/rubros', timeout=60) as r: code = r.status
    except urllib.error.HTTPError as e: code = e.code
    except Exception: code = 'error'
    with lock: filas.append((time.strftime('%H:%M:%S', time.localtime(t0)), f, 'publica', code, round((time.time() - t0) * 1000)))
try:
    for nombre, dur in FASES:
        fase[0] = nombre
        if nombre == 'caida': subprocess.run(['docker', 'pause', CONT], check=True); print('pdi pausada', flush=True)
        if nombre == 'restaurada': subprocess.run(['docker', 'unpause', CONT], check=True); print('pdi restaurada', flush=True)
        fin = time.time() + dur
        while time.time() < fin:
            threading.Thread(target=ingreso, daemon=True).start(); threading.Thread(target=publica, daemon=True).start(); time.sleep(1)
finally:
    subprocess.run(['docker', 'unpause', CONT])
time.sleep(30)
with open(SAL, 'w') as f:
    f.write('hora,fase,tipo,codigo,ms\n')
    for x in sorted(filas): f.write(','.join(map(str, x)) + '\n')
print('filas', len(filas))
