import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Group, Modal, Radio, Select, SimpleGrid, Stack, Text, Textarea } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { z } from 'zod';
import type { Conductor, DisponibilidadRequest } from '../../api/tipos';
import { manejarErrorFormulario, notificarExito } from '../../utils/errores';
import { OPCIONES_MOTIVO } from '../../utils/motivos';
import { useCambiarDisponibilidad } from './hooks';

const MOTIVOS = ['INCAPACIDAD', 'VACACIONES', 'PERMISO', 'OTRO'] as const;

const esquema = z
  .object({
    estado: z.enum(['DISPONIBLE', 'NO_DISPONIBLE']),
    motivo: z.enum(MOTIVOS).nullable(),
    detalle: z.string().trim().max(300, 'El detalle admite máximo 300 caracteres'),
  })
  .superRefine((v, ctx) => {
    if (v.estado !== 'NO_DISPONIBLE') return;
    if (!v.motivo) ctx.addIssue({ code: 'custom', path: ['motivo'], message: 'Seleccione el motivo' });
    else if (v.motivo === 'OTRO' && v.detalle === '')
      ctx.addIssue({ code: 'custom', path: ['detalle'], message: 'Describa el motivo' });
  });

type Entrada = z.input<typeof esquema>;
type Salida = z.output<typeof esquema>;

const CAMPOS = ['estado', 'motivo', 'detalle'] as const;

interface Props {
  conductor: Conductor | null;
  abierto: boolean;
  onCerrar: () => void;
}

export function DisponibilidadModal({ conductor, abierto, onCerrar }: Props) {
  return (
    <Modal
      opened={abierto}
      onClose={onCerrar}
      centered
      size="lg"
      title={conductor ? `Disponibilidad de ${conductor.nombre}` : 'Disponibilidad'}
      styles={{ title: { fontWeight: 600, fontSize: 18 } }}
    >
      {conductor && <Formulario key={conductor.id} conductor={conductor} onCerrar={onCerrar} />}
    </Modal>
  );
}

function Formulario({ conductor, onCerrar }: { conductor: Conductor; onCerrar: () => void }) {
  const cambiar = useCambiarDisponibilidad();
  const enRecorrido = conductor.enRecorrido !== null;

  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Salida>({
    resolver: zodResolver(esquema),
    defaultValues: {
      estado: conductor.disponible ? 'DISPONIBLE' : 'NO_DISPONIBLE',
      motivo: conductor.motivoNoDisponible,
      detalle: conductor.detalleMotivo ?? '',
    },
  });

  const estado = useWatch({ control, name: 'estado' });
  const motivo = useWatch({ control, name: 'motivo' });

  const enviar = handleSubmit(async (datos) => {
    const cuerpo: DisponibilidadRequest =
      datos.estado === 'DISPONIBLE'
        ? { disponible: true }
        : { disponible: false, motivo: datos.motivo, detalle: datos.detalle === '' ? null : datos.detalle };
    try {
      await cambiar.mutateAsync({ id: conductor.id, datos: cuerpo });
      notificarExito('Disponibilidad actualizada', conductor.nombre);
      onCerrar();
    } catch (error) {
      manejarErrorFormulario(error, setError, CAMPOS);
    }
  });

  const errorGeneral = errors.root?.servidor?.message;

  return (
    <form onSubmit={enviar} noValidate>
      <Stack gap="md">
        <Controller
          control={control}
          name="estado"
          render={({ field }) => (
            <Radio.Group label="Estado" value={field.value} onChange={field.onChange} error={errors.estado?.message}>
              <SimpleGrid cols={{ base: 1, xs: 2 }} mt="xs">
                <Radio.Card value="DISPONIBLE" p="md" radius="md">
                  <Group wrap="nowrap" align="flex-start">
                    <Radio.Indicator />
                    <div>
                      <Text fw={600}>Disponible</Text>
                      <Text size="sm" c="dimmed">
                        Puede iniciar recorridos
                      </Text>
                    </div>
                  </Group>
                </Radio.Card>
                <Radio.Card value="NO_DISPONIBLE" p="md" radius="md" disabled={enRecorrido}>
                  <Group wrap="nowrap" align="flex-start">
                    <Radio.Indicator disabled={enRecorrido} />
                    <div>
                      <Text fw={600} c={enRecorrido ? 'dimmed' : undefined}>
                        No disponible
                      </Text>
                      <Text size="sm" c="dimmed">
                        {enRecorrido
                          ? 'En recorrido: no se puede marcar como no disponible hasta que termine.'
                          : 'Incapacidad, vacaciones, permiso u otro'}
                      </Text>
                    </div>
                  </Group>
                </Radio.Card>
              </SimpleGrid>
            </Radio.Group>
          )}
        />

        {estado === 'NO_DISPONIBLE' && (
          <>
            <Controller
              control={control}
              name="motivo"
              render={({ field }) => (
                <Select
                  label="Motivo"
                  withAsterisk
                  placeholder="Seleccione el motivo"
                  data={OPCIONES_MOTIVO}
                  value={field.value}
                  onChange={(v) => field.onChange(v)}
                  onBlur={field.onBlur}
                  allowDeselect={false}
                  error={errors.motivo?.message}
                  comboboxProps={{ withinPortal: true }}
                />
              )}
            />
            <Textarea
              label="Detalle"
              withAsterisk={motivo === 'OTRO'}
              description={motivo === 'OTRO' ? 'Obligatorio con el motivo «Otro»' : 'Opcional'}
              autosize
              minRows={3}
              maxLength={300}
              error={errors.detalle?.message}
              {...register('detalle')}
            />
          </>
        )}

        <Text size="sm" c="dimmed">
          Mientras esté no disponible, no podrá iniciar recorridos.
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
            Guardar
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
