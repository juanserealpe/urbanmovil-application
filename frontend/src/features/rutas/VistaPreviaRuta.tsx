import { Alert, Group, Loader, Paper, Stack, Text } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useMemo } from 'react';
import { MapaBase } from '../../mapa/MapaBase';
import { MENSAJE_INESPERADO, mensajeDeError } from '../../utils/errores';
import { LeyendaRuta } from './componentes';
import { dibujarRuta, puntosDeRuta } from './dibujoRuta';
import { useRuta } from './hooks';

export function VistaPreviaRuta({ id }: { id: number }) {
  const consulta = useRuta(id);
  const ruta = consulta.data;

  const dibujo = useMemo(() => (ruta ? dibujarRuta(ruta) : { lineas: [], marcadores: [] }), [ruta]);
  const encuadre = useMemo(() => (ruta ? puntosDeRuta(ruta) : undefined), [ruta]);

  return (
    <Paper withBorder p={{ base: 'sm', sm: 'lg' }} mt="lg">
      <Stack gap="sm">
        <Group justify="space-between" gap="sm">
          <Group gap="xs">
            <Text fw={600} component="h2" fz="lg" m={0}>
              Vista previa{ruta ? ` · ${ruta.codigo} ${ruta.nombre}` : ''}
            </Text>
            {consulta.isFetching && <Loader size="xs" aria-label="Cargando vista previa" />}
          </Group>
          <LeyendaRuta colorPrincipal={ruta?.color ?? '#1F4FD8'} />
        </Group>

        {consulta.isError && !ruta ? (
          <Alert color="red" variant="light" icon={<IconAlertCircle size={18} />} role="alert">
            {mensajeDeError(consulta.error) ?? MENSAJE_INESPERADO}
          </Alert>
        ) : (
          <Paper withBorder radius="md" style={{ overflow: 'hidden' }}>
            <MapaBase
              alto={300}
              lineas={dibujo.lineas}
              marcadores={dibujo.marcadores}
              encuadrar={encuadre}
              ariaLabel={ruta ? `Mapa de la ruta ${ruta.codigo} ${ruta.nombre}` : 'Mapa de la ruta'}
            />
          </Paper>
        )}

        {ruta && ruta.recorridoPrincipal.length === 0 && (
          <Text size="sm" c="dimmed">
            Esta ruta todavía no tiene recorrido trazado.
          </Text>
        )}
      </Stack>
    </Paper>
  );
}
