import { useSearchParams } from 'react-router-dom'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { authApi } from '@/api/auth/auth.api'
import { getLoginErrorMessage } from '@/api/errors'

function Login() {
  const [params] = useSearchParams()
  const error = getLoginErrorMessage(params.get('error'))

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6">
      <div className="text-lg font-semibold">Carga UY</div>
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
          <CardContent>
            <Button asChild size="lg" className="w-full">
              <a href={authApi.loginUrl}>Ingresar con gub.uy</a>
            </Button>
          </CardContent>
        </Card>
        <p className="text-center text-xs text-muted-foreground">Ministerio de Transporte y Obras Públicas</p>
      </div>
    </div>
  )
}

export default Login
