import { AppShell, Box, Burger, Button, Group, NavLink, ScrollArea, Stack, Text } from '@mantine/core';
import { useDisclosure, useMediaQuery } from '@mantine/hooks';
import {
  IconBus,
  IconCalendarEvent,
  IconLogout,
  IconMapPin,
  IconRoute,
  IconUsers,
} from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { Link, Outlet, useLocation } from 'react-router';
import { useCerrarSesion, useSesion } from '../auth/useSesion';
import { Logo } from '../components/Logo';
import clases from './AppLayout.module.css';

interface ItemMenu {
  ruta: string;
  etiqueta: string;
  icono: ReactNode;
}

const MENU: ItemMenu[] = [
  { ruta: '/rutas', etiqueta: 'Rutas', icono: <IconRoute size={20} /> },
  { ruta: '/conductores', etiqueta: 'Conductores', icono: <IconUsers size={20} /> },
  { ruta: '/vehiculos', etiqueta: 'Vehículos', icono: <IconBus size={20} /> },
  { ruta: '/programacion', etiqueta: 'Programación del día', icono: <IconCalendarEvent size={20} /> },
  { ruta: '/monitoreo', etiqueta: 'Monitoreo', icono: <IconMapPin size={20} /> },
];

function Marca() {
  return (
    <Group gap="sm" wrap="nowrap">
      <Logo />
      <Box>
        <Text fw={700} ff="heading" lh={1.1}>
          UrbanMovil
        </Text>
        <Text size="xs" c="dimmed">
          Administración
        </Text>
      </Box>
    </Group>
  );
}

export function AppLayout() {
  const [abierto, { toggle, close }] = useDisclosure(false);
  // Por debajo de 768 px (breakpoint `sm` de Mantine) el menú pasa a hamburguesa.
  const esMovil = useMediaQuery('(max-width: 47.99em)') ?? false;
  const { pathname } = useLocation();
  const { administrador } = useSesion();
  const cerrarSesion = useCerrarSesion();

  return (
    <AppShell
      header={{ height: 56, collapsed: !esMovil }}
      navbar={{ width: 240, breakpoint: 'sm', collapsed: { mobile: !abierto } }}
      padding={{ base: 'md', sm: 'xl' }}
    >
      <AppShell.Header px="md">
        <Group h="100%" gap="sm">
          <Burger opened={abierto} onClick={toggle} size="sm" aria-label={abierto ? 'Cerrar menú' : 'Abrir menú'} />
          <Marca />
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md" className={clases.navbar} aria-label="Menú principal">
        {!esMovil && (
          <Box mb="lg">
            <Marca />
          </Box>
        )}

        <AppShell.Section grow component={ScrollArea}>
          <Stack gap={4} component="nav">
            {MENU.map((item) => {
              const activo = pathname === item.ruta || pathname.startsWith(`${item.ruta}/`);
              return (
                <NavLink
                  key={item.ruta}
                  component={Link}
                  to={item.ruta}
                  label={item.etiqueta}
                  leftSection={item.icono}
                  active={activo}
                  aria-current={activo ? 'page' : undefined}
                  className={clases.link}
                  onClick={close}
                />
              );
            })}
          </Stack>
        </AppShell.Section>

        <AppShell.Section pt="md" style={{ borderTop: '1px solid var(--mantine-color-default-border)' }}>
          {administrador && (
            <Box mb="sm">
              <Text fw={600} size="sm" truncate>
                {administrador.nombre}
              </Text>
              <Text c="dimmed" size="xs" truncate>
                {administrador.correo}
              </Text>
            </Box>
          )}
          <Button
            variant="default"
            fullWidth
            leftSection={<IconLogout size={16} />}
            loading={cerrarSesion.isPending}
            onClick={() => cerrarSesion.mutate()}
          >
            Cerrar sesión
          </Button>
        </AppShell.Section>
      </AppShell.Navbar>

      <AppShell.Main className={clases.main}>
        <Box maw={1280} mx="auto">
          <Outlet />
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}
