import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Drawer, Group, Stack, TextInput } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import type { Vehiculo } from '../../api/tipos';
import { manejarErrorFormulario, notificarExito } from '../../utils/errores';
import { useCrearVehiculo, useModificarVehiculo } from './hooks';

const esquema = z.object({
  numeroInterno: z
    .string()
    .trim()
    .min(1, 'Escriba el número interno')
    .max(20, 'El número interno admite máximo 20 caracteres'),
  placa: z.string().trim().min(1, 'Escriba la placa').max(10, 'La placa admite máximo 10 caracteres'),
});

type Entrada = z.input<typeof esquema>;
type Salida = z.output<typeof esquema>;

const CAMPOS = ['numeroInterno', 'placa'] as const;

interface Props {
  abierto: boolean;
  /** null = registrar uno nuevo. */
  vehiculo: Vehiculo | null;
  onCerrar: () => void;
}

export function VehiculoForm({ abierto, vehiculo, onCerrar }: Props) {
  return (
    <Drawer
      opened={abierto}
      onClose={onCerrar}
      position="right"
      size={460}
      title={vehiculo ? 'Editar vehículo' : 'Registrar vehículo'}
      styles={{ title: { fontWeight: 600, fontSize: 20 } }}
    >
      <Formulario key={vehiculo?.id ?? 'nuevo'} vehiculo={vehiculo} onCerrar={onCerrar} />
    </Drawer>
  );
}

function Formulario({ vehiculo, onCerrar }: Omit<Props, 'abierto'>) {
  const esEdicion = vehiculo !== null;
  const crear = useCrearVehiculo();
  const modificar = useModificarVehiculo();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Salida>({
    resolver: zodResolver(esquema),
    defaultValues: {
      numeroInterno: vehiculo?.numeroInterno ?? '',
      placa: vehiculo?.placa ?? '',
    },
  });

  const enviar = handleSubmit(async (datos) => {
    try {
      // Se muestran los valores que devuelve el backend, ya normalizados.
      const guardado = vehiculo
        ? await modificar.mutateAsync({ id: vehiculo.id, datos })
        : await crear.mutateAsync(datos);
      notificarExito(
        esEdicion ? 'Cambios guardados' : 'Vehículo registrado',
        `${guardado.numeroInterno} · placa ${guardado.placa}`,
      );
      onCerrar();
    } catch (error) {
      manejarErrorFormulario(error, setError, CAMPOS);
    }
  });

  const errorGeneral = errors.root?.servidor?.message;

  return (
    <form onSubmit={enviar} noValidate>
      <Stack gap="md">
        <TextInput
          label="Número interno"
          withAsterisk
          description="Por ejemplo UM-104"
          error={errors.numeroInterno?.message}
          {...register('numeroInterno')}
        />
        <TextInput
          label="Placa"
          withAsterisk
          description="Se guarda en mayúsculas, sin espacios ni guiones"
          error={errors.placa?.message}
          {...register('placa')}
        />

        {errorGeneral && (
          <Alert color="red" variant="light" icon={<IconAlertCircle size={18} />} role="alert">
            {errorGeneral}
          </Alert>
        )}

        <Group justify="flex-end" mt="sm">
          <Button variant="default" onClick={onCerrar} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" loading={isSubmitting}>
            {esEdicion ? 'Guardar cambios' : 'Registrar'}
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
