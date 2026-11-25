import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Box, Button, Center, Paper, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core';
import { useDocumentTitle } from '@mantine/hooks';
import { IconAlertCircle } from '@tabler/icons-react';
import { useForm } from 'react-hook-form';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import { CargadorPantalla } from '../components/CargadorPantalla';
import { Logo } from '../components/Logo';
import { manejarErrorFormulario } from '../utils/errores';
import { leerDesde, useLogin, useSesion } from './useSesion';

const esquema = z.object({
  correo: z.string().trim().min(1, 'Escriba el correo'),
  contrasena: z.string().min(1, 'Escriba la contraseña'),
});

type Entrada = z.input<typeof esquema>;
type Salida = z.output<typeof esquema>;

const CAMPOS = ['correo', 'contrasena'] as const;

export function LoginPage() {
  useDocumentTitle('Iniciar sesión · UrbanMovil Admin');
  const { administrador, cargando } = useSesion();
  const location = useLocation();
  const navigate = useNavigate();
  const loginMutation = useLogin();
  const destino = leerDesde(location.state);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Salida>({
    resolver: zodResolver(esquema),
    defaultValues: { correo: '', contrasena: '' },
  });

  if (administrador && !loginMutation.isPending) return <Navigate to={destino} replace />;
  if (cargando) return <CargadorPantalla />;

  const enviar = handleSubmit(async (datos) => {
    try {
      await loginMutation.mutateAsync(datos);
      navigate(destino, { replace: true });
    } catch (error) {
      manejarErrorFormulario(error, setError, CAMPOS);
    }
  });

  const errorGeneral = errors.root?.servidor?.message;

  return (
    <Center mih="100vh" p="md">
      <Paper withBorder shadow="sm" p="xl" w="100%" maw={420}>
        <form onSubmit={enviar} noValidate>
          <Stack gap="lg">
            <Stack gap={6} align="center">
              <Logo tamano={48} />
              <Title order={2} fz={24}>
                UrbanMovil
              </Title>
              <Text c="dimmed" size="sm">
                Administración de la empresa
              </Text>
            </Stack>

            <Title order={3} fz={20}>
              Iniciar sesión
            </Title>

            <TextInput
              label="Correo"
              type="email"
              autoComplete="username"
              placeholder="admin@urbanmovil.co"
              error={errors.correo?.message}
              {...register('correo')}
            />
            <PasswordInput
              label="Contraseña"
              autoComplete="current-password"
              error={errors.contrasena?.message}
              visibilityToggleButtonProps={{ 'aria-label': 'Mostrar u ocultar la contraseña' }}
              {...register('contrasena')}
            />

            {errorGeneral && (
              <Alert color="red" variant="light" icon={<IconAlertCircle size={18} />} role="alert">
                {errorGeneral}
              </Alert>
            )}

            <Box>
              <Button type="submit" fullWidth loading={isSubmitting} disabled={isSubmitting}>
                Ingresar
              </Button>
              <Text c="dimmed" size="xs" ta="center" mt="sm">
                Solo para administradores
              </Text>
            </Box>
          </Stack>
        </form>
      </Paper>
    </Center>
  );
}
