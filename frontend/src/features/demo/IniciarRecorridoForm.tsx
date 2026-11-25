import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Anchor, Button, Group, Paper, Select, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { IconAlertCircle, IconPlayerPlay } from '@tabler/icons-react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { Link } from 'react-router';
import { z } from 'zod';
import { manejarErrorFormulario, notificarExito } from '../../utils/errores';
import { hoyBogota } from '../../utils/fechas';
import { useConductores } from '../conductores/hooks';
import { useProgramacion } from '../programacion/hooks';
import { useVehiculos } from '../vehiculos/hooks';
import { useIniciarRecorrido } from './hooks';

const esquema = z.object({
  conductorId: z.string('Indique el conductor').min(1, 'Indique el conductor'),
  rutaId: z.string('Indique la ruta').min(1, 'Indique la ruta'),
  vehiculoId: z.string('Indique el vehículo').min(1, 'Indique el vehículo'),
});

type Entrada = z.input<typeof esquema>;
type Salida = z.output<typeof esquema>;
const CAMPOS = ['conductorId', 'rutaId', 'vehiculoId'] as const;
const VACIO: Entrada = { conductorId: '', rutaId: '', vehiculoId: '' };

export function IniciarRecorridoForm() {
  const iniciar = useIniciarRecorrido();
  const hoy = hoyBogota();

  const {
    control,
    handleSubmit,
    setError,
    setValue,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Salida>({ resolver: zodResolver(esquema), defaultValues: VACIO });

  const conductorElegido = useWatch({ control, name: 'conductorId' });
  const conductorId = conductorElegido ? Number(conductorElegido) : null;

  const conductores = useConductores({ disponible: true });
  const vehiculos = useVehiculos({ activo: true });
  const programacion = useProgramacion(conductorId, hoy);

  const opcionesConductor = (conductores.data ?? [])
    .filter((c) => c.enRecorrido === null)
    .map((c) => ({ value: String(c.id), label: c.nombre }));
  const opcionesVehiculo = (vehiculos.data ?? [])
    .filter((v) => v.enRecorrido === null)
    .map((v) => ({ value: String(v.id), label: `${v.numeroInterno} · ${v.placa}` }));
  const opcionesRuta = (programacion.data?.rutas ?? [])
    .filter((r) => r.estado === 'PENDIENTE')
    .map((r) => ({ value: String(r.rutaId), label: `${r.codigo} · ${r.nombre}` }));

  const sinRutasPendientes = conductorId !== null && programacion.isSuccess && opcionesRuta.length === 0;

  const enviar = handleSubmit(async (datos) => {
    try {
      const r = await iniciar.mutateAsync({
        conductorId: Number(datos.conductorId),
        rutaId: Number(datos.rutaId),
        vehiculoId: Number(datos.vehiculoId),
      });
      notificarExito(
        `Recorrido iniciado: ${r.conductor.nombre} en ${r.ruta.codigo} con ${r.vehiculo.numeroInterno}`,
      );
      reset(VACIO);
    } catch (error) {
      // Los 409 (REGLA_NEGOCIO) salen en notificación con su `mensaje`.
      manejarErrorFormulario(error, setError, CAMPOS);
    }
  });

  const errorGeneral = errors.root?.servidor?.message;

  return (
    <Paper withBorder p={{ base: 'sm', sm: 'lg' }}>
      <form onSubmit={enviar} noValidate>
        <Stack gap="md">
          <Title order={2} fz="lg">
            Iniciar recorrido
          </Title>
          <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
            <Controller
              control={control}
              name="conductorId"
              render={({ field }) => (
                <Select
                  label="Conductor"
                  withAsterisk
                  placeholder={conductores.isPending ? 'Cargando…' : 'Elija un conductor'}
                  description="Disponibles y sin recorrido activo"
                  data={opcionesConductor}
                  value={field.value || null}
                  onChange={(v) => {
                    field.onChange(v ?? '');
                    // La ruta depende del conductor.
                    setValue('rutaId', '');
                  }}
                  searchable
                  nothingFoundMessage="Ningún conductor coincide"
                  error={errors.conductorId?.message}
                />
              )}
            />
            <Controller
              control={control}
              name="rutaId"
              render={({ field }) => (
                <Select
                  label="Ruta"
                  withAsterisk
                  placeholder={conductorId === null ? 'Elija primero el conductor' : 'Elija una ruta'}
                  description="Pendientes en su programación de hoy"
                  data={opcionesRuta}
                  value={field.value || null}
                  onChange={(v) => field.onChange(v ?? '')}
                  disabled={conductorId === null || sinRutasPendientes}
                  error={errors.rutaId?.message}
                />
              )}
            />
            <Controller
              control={control}
              name="vehiculoId"
              render={({ field }) => (
                <Select
                  label="Vehículo"
                  withAsterisk
                  placeholder={vehiculos.isPending ? 'Cargando…' : 'Elija un vehículo'}
                  description="Activos y sin recorrido"
                  data={opcionesVehiculo}
                  value={field.value || null}
                  onChange={(v) => field.onChange(v ?? '')}
                  searchable
                  nothingFoundMessage="Ningún vehículo coincide"
                  error={errors.vehiculoId?.message}
                />
              )}
            />
          </SimpleGrid>

          {sinRutasPendientes && (
            <Text size="sm" c="dimmed">
              Este conductor no tiene rutas pendientes hoy. Asígnelas en{' '}
              <Anchor component={Link} to={`/programacion?fecha=${hoy}&conductor=${conductorId}`}>
                Programación del día
              </Anchor>
              .
            </Text>
          )}

          {errorGeneral && (
            <Alert color="red" variant="light" icon={<IconAlertCircle size={18} />} role="alert">
              {errorGeneral}
            </Alert>
          )}

          <Group justify="flex-end">
            <Button type="submit" loading={isSubmitting} leftSection={<IconPlayerPlay size={18} aria-hidden />}>
              Iniciar recorrido
            </Button>
          </Group>
        </Stack>
      </form>
    </Paper>
  );
}
