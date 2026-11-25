import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Drawer, Group, PasswordInput, Stack, TextInput } from '@mantine/core';
import { IconAlertCircle, IconInfoCircle } from '@tabler/icons-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import type { Conductor } from '../../api/tipos';
import { generarContrasena } from '../../utils/contrasena';
import { manejarErrorFormulario, notificarExito } from '../../utils/errores';
import { useCrearConductor, useModificarConductor } from './hooks';

const MIN_CONTRASENA = 'La contraseña debe tener al menos 8 caracteres';

function crearEsquema(esEdicion: boolean) {
  return z.object({
    nombre: z
      .string()
      .trim()
      .min(1, 'Escriba el nombre')
      .max(120, 'El nombre admite máximo 120 caracteres'),
    identificacion: z
      .string()
      .trim()
      .min(1, 'Escriba el número de identificación')
      .max(20, 'La identificación admite máximo 20 caracteres'),
    correo: z
      .string()
      .trim()
      .min(1, 'Escriba el correo')
      .max(120, 'El correo admite máximo 120 caracteres')
      .pipe(z.email('Escriba un correo válido')),
    telefono: z
      .string()
      .trim()
      .min(1, 'Escriba el teléfono')
      .max(20, 'El teléfono admite máximo 20 caracteres'),
    contrasena: esEdicion
      ? z.string().refine((v) => v.length === 0 || v.length >= 8, MIN_CONTRASENA)
      : z.string().min(1, 'Escriba la contraseña inicial').min(8, MIN_CONTRASENA),
  });
}

type Esquema = ReturnType<typeof crearEsquema>;
type Entrada = z.input<Esquema>;
type Salida = z.output<Esquema>;

const CAMPOS = ['nombre', 'identificacion', 'correo', 'telefono', 'contrasena'] as const;

interface Props {
  abierto: boolean;
  /** null = registrar uno nuevo. */
  conductor: Conductor | null;
  onCerrar: () => void;
}

export function ConductorForm({ abierto, conductor, onCerrar }: Props) {
  return (
    <Drawer
      opened={abierto}
      onClose={onCerrar}
      position="right"
      size={460}
      title={conductor ? 'Editar conductor' : 'Registrar conductor'}
      styles={{ title: { fontWeight: 600, fontSize: 20 } }}
    >
      {/* El `key` reinicia el formulario cada vez que cambia el conductor que se edita. */}
      <Formulario key={conductor?.id ?? 'nuevo'} conductor={conductor} onCerrar={onCerrar} />
    </Drawer>
  );
}

function Formulario({ conductor, onCerrar }: Omit<Props, 'abierto'>) {
  const esEdicion = conductor !== null;
  const crear = useCrearConductor();
  const modificar = useModificarConductor();
  const [verContrasena, setVerContrasena] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Salida>({
    resolver: zodResolver(crearEsquema(esEdicion)),
    defaultValues: {
      nombre: conductor?.nombre ?? '',
      identificacion: conductor?.identificacion ?? '',
      correo: conductor?.correo ?? '',
      telefono: conductor?.telefono ?? '',
      contrasena: '',
    },
  });

  const enviar = handleSubmit(async (datos) => {
    try {
      if (conductor) {
        await modificar.mutateAsync({
          id: conductor.id,
          datos: { ...datos, contrasena: datos.contrasena === '' ? null : datos.contrasena },
        });
        notificarExito('Cambios guardados');
      } else {
        await crear.mutateAsync(datos);
        notificarExito('Conductor registrado');
      }
      onCerrar();
    } catch (error) {
      manejarErrorFormulario(error, setError, CAMPOS);
    }
  });

  const generar = () => {
    setValue('contrasena', generarContrasena(12), { shouldValidate: true, shouldDirty: true });
    setVerContrasena(true);
  };

  const errorGeneral = errors.root?.servidor?.message;

  return (
    <form onSubmit={enviar} noValidate>
      <Stack gap="md">
        {!esEdicion && (
          <Alert color="urbano" variant="light" icon={<IconInfoCircle size={18} />}>
            El conductor queda disponible por defecto. Todos los campos son obligatorios.
          </Alert>
        )}

        <TextInput label="Nombre completo" withAsterisk error={errors.nombre?.message} {...register('nombre')} />
        <TextInput
          label="Número de identificación"
          withAsterisk
          error={errors.identificacion?.message}
          {...register('identificacion')}
        />
        <TextInput
          label="Correo"
          type="email"
          withAsterisk
          description="Será su usuario en la app del conductor"
          error={errors.correo?.message}
          {...register('correo')}
        />
        <TextInput
          label="Teléfono"
          type="tel"
          withAsterisk
          error={errors.telefono?.message}
          {...register('telefono')}
        />

        <Group align="flex-start" gap="xs" wrap="nowrap">
          <PasswordInput
            style={{ flex: 1 }}
            label={esEdicion ? 'Nueva contraseña' : 'Contraseña inicial'}
            withAsterisk={!esEdicion}
            autoComplete="new-password"
            description={esEdicion ? 'Déjela vacía para no cambiarla' : 'Mínimo 8 caracteres'}
            visible={verContrasena}
            onVisibilityChange={setVerContrasena}
            visibilityToggleButtonProps={{ 'aria-label': 'Mostrar u ocultar la contraseña' }}
            error={errors.contrasena?.message}
            {...register('contrasena')}
          />
          {!esEdicion && (
            <Button variant="default" onClick={generar} mt={45}>
              Generar
            </Button>
          )}
        </Group>

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
