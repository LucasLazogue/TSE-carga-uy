import json, hmac, hashlib, base64, time, os, sys, urllib.request
B = os.environ.get('CENTRAL', 'https://carga-uy-kevin-martinez-herna-dev.apps.rm2.thpm.p1.openshiftapps.com/carga-uy')
S=os.environ['JWT_SECRETO'].encode(); U=json.load(open(sys.argv[1])); r=U['responsables'][0]
def b64(x): return base64.urlsafe_b64encode(x).rstrip(b'=').decode()
now=int(time.time()); h=b64(json.dumps({'alg':'HS256','typ':'JWT'}).encode())
c=b64(json.dumps({'iss':'carga-uy','sub':str(r['id']),'typ':'sesion','iat':now,'exp':now+600,'cedula':r['cedula'],'roles':['CIUDADANO','RESPONSABLE']}).encode())
t=f'{h}.{c}.'+b64(hmac.new(S,f'{h}.{c}'.encode(),hashlib.sha256).digest())
def get(p):
    req=urllib.request.Request(B+p,headers={'Authorization':'Bearer '+t}); t0=time.time()
    try:
        with urllib.request.urlopen(req,timeout=60) as x: return x.status, x.read().decode()[:400], round((time.time()-t0)*1000)
    except urllib.error.HTTPError as e: return e.code, e.read().decode()[:300], round((time.time()-t0)*1000)
v=json.loads(get(f"/api/empresas/{r['empresa']}/viajes?pagina=0&tamanio=1")[1])['items'][0]
print(v.get('matricula'), get(f"/api/empresas/{r['empresa']}/viajes/{v['id']}/pesajes"))
