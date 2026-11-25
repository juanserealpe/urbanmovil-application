import { AppShell, Badge, Box, Burger, Button, Divider, Group, NavLink, ScrollArea, Stack, Text } from '@mantine/core';
import { useDisclosure, useDocumentTitle, useMediaQuery } from '@mantine/hooks';
import {
  IconBus,
  IconCalendarEvent,
  IconFlask,
  IconLogout,
  IconMapPin,
  IconRoute,
  IconUsers,
} from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { Link, Outlet, useLocation } from 'react-router';
import { useCerrarSesion, useSesion } from '../auth/useSesion';
import { Logo } from '../components/Logo';
import { useModoDemo } from '../features/demo/hooks';
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

const ITEM_DEMO: ItemMenu = { ruta: '/demo', etiqueta: 'Recorridos de demostración', icono: <IconFlask size={20} /> };

const SUFIJO_TITULO = ' · UrbanMovil Admin';

/** Título de la pestaña según la pantalla: «Conductores · UrbanMovil Admin». */
function tituloDePestana(pathname: string): string {
  let nombre: string | undefined;
  if (pathname === '/rutas/nueva') nombre = 'Nueva ruta';
  else if (/^\/rutas\/[^/]+$/.test(pathname)) nombre = 'Editar ruta';
  else nombre = [...MENU, ITEM_DEMO].find((i) => pathname === i.ruta || pathname.startsWith(`${i.ruta}/`))?.etiqueta;
  return `${nombre ?? 'Administración'}${SUFIJO_TITULO}`;
}

function ItemNavegacion({ item, pathname, onClick, extra }: { item: ItemMenu; pathname: string; onClick: () => void; extra?: ReactNode }) {
  const activo = pathname === item.ruta || pathname.startsWith(`${item.ruta}/`);
  return (
    <NavLink
      component={Link}
      to={item.ruta}
      label={item.etiqueta}
      leftSection={item.icono}
      rightSection={extra}
      active={activo}
      aria-current={activo ? 'page' : undefined}
      className={clases.link}
      onClick={onClick}
    />
  );
}

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
  const { modo: modoDemo } = useModoDemo();
  useDocumentTitle(tituloDePestana(pathname));

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
            {MENU.map((item) => (
              <ItemNavegacion key={item.ruta} item={item} pathname={pathname} onClick={close} />
            ))}
            {/* Solo si el backend corre con el perfil demo (GET /demo/recorridos no da 404). */}
            {modoDemo === 'activo' && (
              <>
                <Divider my="xs" />
                <ItemNavegacion
                  item={ITEM_DEMO}
                  pathname={pathname}
                  onClick={close}
                  extra={
                    <Badge color="gray" variant="light" size="sm" tt="none">
                      Demo
                    </Badge>
                  }
                />
              </>
            )}
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
