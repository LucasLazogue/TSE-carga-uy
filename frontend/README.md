# Frontend de carga.uy

Interfaz web en React y TypeScript, con Vite, Tailwind CSS y Oxlint.

## Ejecutar en desarrollo

Requiere Node.js 24, npm y el [central levantado](../README.md#ejecución-local).
Desde la raíz del repositorio, en PowerShell:

```powershell
cd frontend
Copy-Item .env.example .env
npm ci
npm run dev
```

En Bash, usar `cp .env.example .env` para la copia. La aplicación queda en http://localhost:5173/.

## Configurar el backend

El archivo [`.env.example`](.env.example) contiene la configuración local:

```dotenv
VITE_API_TARGET=http://localhost:8080/carga-uy
```

Vite reenvía los pedidos a `/api` hacia esa dirección. Para apuntar a un central remoto,
crear `.env.local` con su URL y reiniciar `npm run dev`:

```dotenv
VITE_API_TARGET=https://URL_DEL_CENTRAL/carga-uy
```

Esta variable configura el proxy de desarrollo. El login también depende del callback y de
`CARGAUY_FRONTEND_URL` en el central; ver la [documentación de seguridad](../docs/seguridad.md).

## Verificar cambios

Desde `frontend`:

```sh
npm run lint
npm run build
```

El build comprueba los tipos con TypeScript y genera la aplicación en `dist/`.
El [CI](../.github/workflows/ci.yml) ejecuta estas mismas verificaciones.
