import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { authApi } from '@/api/auth/auth.api'
import { getLoginErrorMessage } from '@/api/errors'
import { CEDULA_DIGITOS, formatCedula, onlyDigits } from '@/lib/cedula'

function Login() {
  const [params] = useSearchParams()
  const error = getLoginErrorMessage(params.get('error'))
  const [mock, setMock] = useState(false)
  const [cedula, setCedula] = useState('')

  useEffect(() => {
    authApi
      .getConfig()
      .then((config) => setMock(config.mock))
      .catch(() => setMock(false))
  }, [])

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6">
      <Link to="/" className="text-lg font-semibold">
        Carga UY
      </Link>
      <div className="w-full max-w-sm space-y-4">
        {error && (
          <Alert variant="destructive">
            <AlertCircle />
            <AlertTitle>No se pudo ingresar</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <Card>
          <CardHeader className="text-center">
            <CardTitle className="text-xl">Bienvenido</CardTitle>
            <CardDescription>Ingresá con tu usuario gub.uy para continuar.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {mock && (
              <Input
                placeholder="Ingrese cédula"
                inputMode="numeric"
                value={formatCedula(cedula)}
                onChange={(e) => setCedula(onlyDigits(e.target.value))}
              />
            )}
            {mock && cedula.length < CEDULA_DIGITOS ? (
              <Button size="lg" className="w-full" disabled>
                Ingresar con gub.uy
              </Button>
            ) : (
              <Button asChild size="lg" className="w-full">
                <a href={mock ? authApi.mockLoginUrl(cedula) : authApi.loginUrl}>Ingresar con gub.uy</a>
              </Button>
            )}
          </CardContent>
        </Card>
        <p className="text-center text-xs text-muted-foreground">Ministerio de Transporte y Obras Públicas</p>
      </div>
    </div>
  )
}

export default Login
