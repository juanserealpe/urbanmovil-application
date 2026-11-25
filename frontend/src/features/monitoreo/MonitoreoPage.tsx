import { Alert, Box, Group, Paper, Skeleton, Stack, Text, Title } from '@mantine/core';
import { IconAlertTriangle, IconBus, IconInfoCircle } from '@tabler/icons-react';
import { useMemo, useRef, useState } from 'react';
import type { Punto } from '../../api/tipos';
import { colores } from '../../app/theme';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { MapaBase } from '../../mapa/MapaBase';
import type { LineaMapa, MapaBaseHandle, MarcadorMapa } from '../../mapa/tipos';
import { formatearHora } from '../../utils/fechas';
import { useMonitoreo, useTrazadosRutas } from './hooks';
import clases from './MonitoreoPage.module.css';

const idMarcador = (recorridoId: number) => `bus-${recorridoId}`;

export function MonitoreoPage() {
  const consulta = useMonitoreo();
  const buses = useMemo(() => consulta.data?.buses ?? [], [consulta.data]);
  const mapaRef = useRef<MapaBaseHandle>(null);

  const idsRutas = useMemo(() => [...new Set(buses.map((b) => b.ruta.id))].sort((a, b) => a - b), [buses]);
  const { rutas, todosCargados } = useTrazadosRutas(idsRutas);

  const lineas = useMemo<LineaMapa[]>(
    () => rutas.map((r) => ({ id: `ruta-${r.id}`, puntos: r.recorridoPrincipal, color: r.color, opacidad: 0.6 })),
    [rutas],
  );

  const marcadores = useMemo<MarcadorMapa[]>(
    () =>
      buses.map((b) => ({
        id: idMarcador(b.recorridoId),
        punto: b.posicion,
        tipo: 'bus',
        color: b.ruta.color,
        // Estable entre consultas: si cambiara, el marcador se recrearía en vez de moverse.
        etiqueta: `Bus ${b.vehiculo.numeroInterno} · ruta ${b.ruta.codigo}`,
        popup: {
          titulo: b.vehiculo.numeroInterno,
          lineas: [
            `Placa ${b.vehiculo.placa}`,
            `Ruta ${b.ruta.codigo} · ${b.ruta.nombre}`,
            `Conductor: ${b.conductor.nombre}`,
            `Posición de las ${formatearHora(b.horaPosicion)}`,
          ],
        },
      })),
    [buses],
  );

  // Encuadre a todos los trazados la primera vez que están todos cargados; después, el usuario manda.
  const [encuadre, setEncuadre] = useState<Punto[] | undefined>(undefined);
  if (!encuadre && todosCargados) {
    const puntos = rutas.flatMap((r) => r.recorridoPrincipal);
    if (puntos.length > 0) setEncuadre(puntos);
  }

  const sinActualizar = consulta.isError;

  let lista;
  if (consulta.isPending) {
    lista = (
      <Stack gap="xs" role="status" aria-label="Cargando buses">
        <Skeleton h={44} />
        <Skeleton h={44} />
      </Stack>
    );
  } else if (buses.length === 0) {
    lista = (
      <Stack align="center" gap={6} py="lg" ta="center">
        <IconBus size={28} color={colores.textoSecundario} aria-hidden />
        <Text fw={600}>{consulta.data ? 'No hay buses en recorrido' : 'Sin datos todavía'}</Text>
      </Stack>
    );
  } else {
    lista = (
      <Stack gap={2} component="ul" p={0} m={0} style={{ listStyle: 'none' }}>
        {buses.map((b) => (
          <li key={b.recorridoId}>
            <button
              type="button"
              className={clases.filaBus}
              onClick={() => mapaRef.current?.enfocarMarcador(idMarcador(b.recorridoId))}
              aria-label={`Ver ${b.vehiculo.numeroInterno}, ruta ${b.ruta.codigo}, en el mapa`}
            >
              <Group gap={8} wrap="nowrap">
                <Box
                  w={10}
                  h={10}
                  bg={b.ruta.color}
                  style={{ borderRadius: '50%', flexShrink: 0 }}
                  aria-hidden
                />
                <Text fw={600} size="sm">
                  {b.vehiculo.numeroInterno} · {b.ruta.codigo}
                </Text>
              </Group>
              <Text size="xs" c="dimmed" pl={18}>
                {b.conductor.nombre} · {formatearHora(b.horaPosicion)}
              </Text>
            </button>
          </li>
        ))}
      </Stack>
    );
  }

  return (
    <>
      <EncabezadoPagina titulo="Monitoreo" subtitulo="Vista de solo lectura de los buses en recorrido" />

      <Alert
        variant="filled"
        bg={colores.avisoFondo}
        c={colores.avisoTexto}
        icon={<IconInfoCircle size={18} color={colores.avisoTexto} aria-hidden />}
        mb="md"
        styles={{ message: { color: colores.avisoTexto } }}
      >
        <Text span fw={700} inherit>
          Posición simulada.
        </Text>{' '}
        Los buses se mueven según la hora sobre el recorrido principal de su ruta. No hay GPS real.
      </Alert>

      {sinActualizar && (
        <Alert color="red" variant="light" icon={<IconAlertTriangle size={18} />} mb="md" role="status">
          No se pudo actualizar. Reintentando…
        </Alert>
      )}

      <div className={clases.disposicion}>
        <div className={clases.columnaMapa}>
          <MapaBase
            ref={mapaRef}
            alto={620}
            lineas={lineas}
            marcadores={marcadores}
            encuadrar={encuadre}
            ariaLabel="Mapa de buses en recorrido"
          />
        </div>

        <Paper withBorder p="md" className={clases.panel}>
          <Stack gap="sm">
            <Title order={2} fz="md" aria-live="polite">
              Buses en recorrido ({buses.length})
            </Title>
            {lista}
            {consulta.data && (
              <Text size="xs" c="dimmed" pt="xs" style={{ borderTop: `1px solid ${colores.borde}` }}>
                Actualizado a las {formatearHora(consulta.data.generadoEn)}
              </Text>
            )}
          </Stack>
        </Paper>
      </div>
    </>
  );
}
