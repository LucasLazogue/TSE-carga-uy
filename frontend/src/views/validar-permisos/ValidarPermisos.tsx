import { useState, type FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { getErrorMessage } from '@/api/errors'
import { PublicoApi } from '@/api/publico/publico.api'
import type { HabilitacionEmpresa } from '@/api/publico/publico.types'
import { fecha } from '@/lib/fechas'

type Criterio = 'nro' | 'razonSocial'

function ValidarPermisos() {
  const [criterio, setCriterio] = useState<Criterio>('nro')
  const [valor, setValor] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [resultado, setResultado] = useState<HabilitacionEmpresa | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cambiarCriterio = (nuevo: Criterio) => {
    setCriterio(nuevo)
    setValor('')
    setResultado(null)
    setError(null)
  }

  const buscar = async (e: FormEvent) => {
    e.preventDefault()
    setBuscando(true)
    setResultado(null)
    setError(null)
    try {
      setResultado(
        await PublicoApi.validarEmpresa(criterio === 'nro' ? { nro: Number(valor) } : { razonSocial: valor.trim() }),
      )
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBuscando(false)
    }
  }

  return (
    <div className="max-w-xl space-y-4">
      <PageHeader
        title="Validar permisos"
        description="Verificá si una empresa transportista tiene sus permisos vigentes."
      />
      <Card>
        <CardContent>
          <form onSubmit={buscar} className="space-y-4">
            <RadioGroup
              value={criterio}
              onValueChange={(v) => cambiarCriterio(v as Criterio)}
              className="flex gap-6"
            >
              <div className="flex items-center gap-2">
                <RadioGroupItem value="nro" id="criterio-nro" />
                <Label htmlFor="criterio-nro">Nro. de empresa</Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="razonSocial" id="criterio-razon-social" />
                <Label htmlFor="criterio-razon-social">Razón social</Label>
              </div>
            </RadioGroup>
            <div className="flex gap-2">
              <Input
                aria-label={criterio === 'nro' ? 'Nro. de empresa' : 'Razón social'}
                placeholder={criterio === 'nro' ? 'Ej. 1001' : 'Razón social completa'}
                inputMode={criterio === 'nro' ? 'numeric' : 'text'}
                value={valor}
                onChange={(e) => setValor(criterio === 'nro' ? e.target.value.replace(/\D/g, '') : e.target.value)}
              />
              <Button type="submit" disabled={buscando || !valor.trim()}>
                Validar
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      {error && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {resultado && (
        <Card>
          <CardHeader>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <CardTitle>{resultado.nombrePublico}</CardTitle>
              <Badge variant={resultado.habilitada ? 'default' : 'destructive'}>
                {resultado.habilitada ? 'Habilitada' : 'No habilitada'}
              </Badge>
            </div>
            <CardDescription>{resultado.razonSocial}</CardDescription>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            {resultado.habilitada
              ? `Al ${fecha(resultado.fecha)} tiene vehículos con Permiso Nacional de Circulación e ITV vigentes.`
              : `Al ${fecha(resultado.fecha)} no tiene vehículos con Permiso Nacional de Circulación e ITV vigentes.`}
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default ValidarPermisos
