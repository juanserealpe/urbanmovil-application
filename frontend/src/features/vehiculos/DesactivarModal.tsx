import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Group, Modal, Stack, Text, Textarea } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import type { Vehiculo } from '../../api/tipos';
import { manejarErrorFormulario, notificarExito } from '../../utils/errores';
import { useCambiarEstadoVehiculo } from './hooks';

const esquema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'Escriba el motivo de la desactivación')
    .max(300, 'El motivo admite máximo 300 caracteres'),
});

type Entrada = z.input<typeof esquema>;
type Salida = z.output<typeof esquema>;

const CAMPOS = ['motivo'] as const;

interface Props {
  vehiculo: Vehiculo | null;
  abierto: boolean;
  onCerrar: () => void;
}

export function DesactivarModal({ vehiculo, abierto, onCerrar }: Props) {
  return (
    <Modal
      opened={abierto}
      onClose={onCerrar}
      centered
      title={vehiculo ? `Desactivar ${vehiculo.numeroInterno}` : 'Desactivar vehículo'}
      styles={{ title: { fontWeight: 600, fontSize: 18 } }}
    >
      {vehiculo && <Formulario key={vehiculo.id} vehiculo={vehiculo} onCerrar={onCerrar} />}
    </Modal>
  );
}

function Formulario({ vehiculo, onCerrar }: { vehiculo: Vehiculo; onCerrar: () => void }) {
  const cambiarEstado = useCambiarEstadoVehiculo();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Salida>({
    resolver: zodResolver(esquema),
    defaultValues: { motivo: '' },
  });

  const enviar = handleSubmit(async ({ motivo }) => {
    try {
      await cambiarEstado.mutateAsync({ id: vehiculo.id, datos: { activo: false, motivo } });
      notificarExito('Vehículo desactivado', vehiculo.numeroInterno);
      onCerrar();
    } catch (error) {
      manejarErrorFormulario(error, setError, CAMPOS);
    }
  });

  const errorGeneral = errors.root?.servidor?.message;

  return (
    <form onSubmit={enviar} noValidate>
      <Stack gap="md">
        <Textarea
          label="Motivo"
          withAsterisk
          placeholder="Mantenimiento de frenos"
          description="Por ejemplo: Mantenimiento de frenos"
          autosize
          minRows={3}
          maxLength={300}
          data-autofocus
          error={errors.motivo?.message}
          {...register('motivo')}
        />
        <Text size="sm" c="dimmed">
          Un vehículo inactivo no puede usarse en recorridos.
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
          <Button type="submit" color="red" loading={isSubmitting}>
            Desactivar
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
