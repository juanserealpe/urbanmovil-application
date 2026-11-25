import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Checkbox, Group, Modal, SimpleGrid, Stack, Text, Textarea } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import type { ProgramacionResumen, RutaProgramada, RutaResumen } from '../../api/tipos';
import { colores } from '../../app/theme';
import { manejarErrorFormulario, notificarExito } from '../../utils/errores';
import { InsigniaRuta } from '../rutas/componentes';
import { useReasignar } from './hooks';

const MSJ_SIN_RUTAS = 'Indique al menos una ruta para asignar o quitar';

const esquema = z
  .object({
    motivo: z.string().trim().min(1, 'Escriba qué pasó').max(300, 'Escriba máximo 300 caracteres'),
    rutasAsignar: z.array(z.string()),
    rutasQuitar: z.array(z.string()),
  })
  .refine((d) => d.rutasAsignar.length + d.rutasQuitar.length > 0, {
    message: MSJ_SIN_RUTAS,
    path: ['rutasAsignar'],
    // Por defecto zod no corre el refine si `motivo` ya falló: así se ven los dos errores a la vez.
    when: () => true,
  });

type Entrada = z.input<typeof esquema>;
type Salida = z.output<typeof esquema>;
const CAMPOS = ['motivo', 'rutasAsignar', 'rutasQuitar'] as const;

interface Props {
  abierto: boolean;
  onCerrar: () => void;
  conductor: ProgramacionResumen;
  /** Programación de HOY, tal como está en el servidor. */
  programacion: RutaProgramada[];
  catalogo: RutaResumen[];
}

export function ReasignarModal({ abierto, onCerrar, ...resto }: Props) {
  return (
    <Modal
      opened={abierto}
      onClose={onCerrar}
      size="xl"
      centered
      title={`Reasignar rutas de ${resto.conductor.nombre}`}
      styles={{ title: { fontWeight: 600, fontSize: 20 } }}
    >
      {/* Se monta al abrir: el formulario siempre arranca vacío y con las opciones vigentes. */}
      {abierto && <Formulario onCerrar={onCerrar} {...resto} />}
    </Modal>
  );
}

function EtiquetaRuta({ codigo, nombre, color }: { codigo: string; nombre: string; color: string }) {
  return (
    <Group gap={8} wrap="nowrap">
      <InsigniaRuta codigo={codigo} color={color} tamano={26} />
      <Text size="sm">{nombre}</Text>
    </Group>
  );
}

function Formulario({ onCerrar, conductor, programacion, catalogo }: Omit<Props, 'abierto'>) {
  const reasignar = useReasignar();
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Salida>({
    resolver: zodResolver(esquema),
    defaultValues: { motivo: '', rutasAsignar: [], rutasQuitar: [] },
  });

  const enProgramacion = new Set(programacion.map((r) => r.rutaId));
  const paraAsignar = catalogo.filter((r) => !enProgramacion.has(r.id));
  // Las que ya se retiran al terminar no se pueden volver a quitar.
  const paraQuitar = programacion.filter((r) => r.estado !== 'RETIRO_PENDIENTE');

  const enviar = handleSubmit(async (datos) => {
    try {
      await reasignar.mutateAsync({
        conductorId: conductor.conductorId,
        datos: {
          motivo: datos.motivo,
          rutasAsignar: datos.rutasAsignar.map(Number),
          rutasQuitar: datos.rutasQuitar.map(Number),
        },
      });
      notificarExito('Reasignación guardada', conductor.nombre);
      onCerrar();
    } catch (error) {
      manejarErrorFormulario(error, setError, CAMPOS);
    }
  });

  const errorRutas = errors.rutasAsignar?.message ?? errors.rutasQuitar?.message;
  const errorGeneral = errors.root?.servidor?.message;

  return (
    <form onSubmit={enviar} noValidate>
      <Stack gap="md">
        <Textarea
          label="¿Qué pasó?"
          withAsterisk
          placeholder="Por ejemplo: Diana reportó falla mecánica"
          autosize
          minRows={2}
          maxRows={5}
          error={errors.motivo?.message}
          data-autofocus
          {...register('motivo')}
        />

        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="lg">
          <Controller
            control={control}
            name="rutasAsignar"
            render={({ field }) => (
              <Checkbox.Group
                label="Rutas que se le asignan"
                description="Se agregan al final de su programación."
                value={field.value}
                onChange={field.onChange}
              >
                <Stack gap="sm" mt="xs">
                  {paraAsignar.length === 0 && (
                    <Text size="sm" c="dimmed">
                      Ya tiene todas las rutas.
                    </Text>
                  )}
                  {paraAsignar.map((r) => (
                    <Checkbox
                      key={r.id}
                      value={String(r.id)}
                      label={<EtiquetaRuta codigo={r.codigo} nombre={r.nombre} color={r.color} />}
                    />
                  ))}
                </Stack>
              </Checkbox.Group>
            )}
          />
          <Controller
            control={control}
            name="rutasQuitar"
            render={({ field }) => (
              <Checkbox.Group label="Rutas que se le quitan" value={field.value} onChange={field.onChange}>
                <Stack gap="sm" mt="xs">
                  {paraQuitar.length === 0 && (
                    <Text size="sm" c="dimmed">
                      No tiene rutas que se puedan quitar hoy.
                    </Text>
                  )}
                  {paraQuitar.map((r) => (
                    <Checkbox
                      key={r.rutaId}
                      value={String(r.rutaId)}
                      label={<EtiquetaRuta codigo={r.codigo} nombre={r.nombre} color={r.color} />}
                      description={
                        r.estado === 'EN_RECORRIDO' ? (
                          <Text component="span" size="xs" c={colores.avisoTexto} fw={500}>
                            Está en recorrido: se quitará al terminarlo
                          </Text>
                        ) : undefined
                      }
                    />
                  ))}
                </Stack>
              </Checkbox.Group>
            )}
          />
        </SimpleGrid>

        {errorRutas && (
          <Text size="sm" c="red.8" role="alert">
            {errorRutas}
          </Text>
        )}

        <Text size="sm" c="dimmed">
          Debe marcar al menos una ruta. Se guardan la fecha, la hora y el administrador. La programación de los
          demás conductores no cambia.
        </Text>

        {errorGeneral && (
          <Alert color="red" variant="light" icon={<IconAlertCircle size={18} />} role="alert">
            {errorGeneral}
          </Alert>
        )}

        <Group justify="flex-end">
          <Button variant="default" onClick={onCerrar} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" loading={isSubmitting}>
            Guardar reasignación
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
