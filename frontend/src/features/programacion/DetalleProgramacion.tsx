import { ActionIcon, Alert, Button, Group, Paper, Select, Skeleton, Stack, Text, Title, Tooltip } from '@mantine/core';
import {
  IconArrowDown,
  IconArrowsExchange,
  IconArrowUp,
  IconInfoCircle,
  IconLock,
  IconPlus,
  IconTrash,
} from '@tabler/icons-react';
import { useState } from 'react';
import type { ProgramacionResumen, RutaProgramada } from '../../api/tipos';
import { colores } from '../../app/theme';
import { BadgeEnRecorrido } from '../../components/BadgeEnRecorrido';
import { BotonAccion } from '../../components/BotonAccion';
import { Chip } from '../../components/Chip';
import { ErrorPantalla } from '../../components/ErrorPantalla';
import { MENSAJE_INESPERADO, mensajeDeError, notificarErrorAccion, notificarExito } from '../../utils/errores';
import { formatearFecha } from '../../utils/fechas';
import { InsigniaRuta } from '../rutas/componentes';
import { useRutas } from '../rutas/hooks';
import { HistorialReasignaciones } from './HistorialReasignaciones';
import { useGuardarProgramacion, useProgramacion } from './hooks';
import { ReasignarModal } from './ReasignarModal';

interface Props {
  conductor: ProgramacionResumen;
  fecha: string;
  hoy: string;
  /** Cambios locales; null = se muestra lo del servidor. */
  borrador: RutaProgramada[] | null;
  onBorrador: (rutas: RutaProgramada[] | null) => void;
}

function EstadoFila({ ruta }: { ruta: RutaProgramada }) {
  switch (ruta.estado) {
    case 'PENDIENTE':
      return (
        <Text size="sm" c="dimmed">
          Pendiente
        </Text>
      );
    case 'EN_RECORRIDO':
      return (
        <Stack gap={2} align="flex-start">
          <Chip tono="info">En recorrido</Chip>
          <Group gap={4} wrap="nowrap">
            <IconLock size={14} aria-hidden color={colores.textoSecundario} />
            <Text size="xs" c="dimmed">
              No se puede retirar hasta que termine
            </Text>
          </Group>
        </Stack>
      );
    case 'RETIRO_PENDIENTE':
      return <Chip tono="aviso">Se retira al terminar el recorrido</Chip>;
  }
}

const mismosIds = (a: RutaProgramada[], b: RutaProgramada[]) =>
  a.length === b.length && a.every((r, i) => r.rutaId === b[i].rutaId);

export function DetalleProgramacion({ conductor, fecha, hoy, borrador, onBorrador }: Props) {
  const consulta = useProgramacion(conductor.conductorId, fecha);
  const catalogo = useRutas('');
  const guardar = useGuardarProgramacion();
  const [rutaAAgregar, setRutaAAgregar] = useState<string | null>(null);
  const [reasignarAbierto, setReasignarAbierto] = useState(false);

  const soloLectura = fecha < hoy;
  const esHoy = fecha === hoy;
  const servidor = consulta.data?.rutas ?? [];
  const visibles = borrador ?? servidor;
  const hayCambios = borrador !== null && !mismosIds(borrador, servidor);
  const fechaLarga = formatearFecha(fecha);
  // Reasignar cambia el servidor: con cambios locales pendientes, el borrador quedaría desfasado.
  const reasignarBloqueado = hayCambios || !consulta.data;

  const editar = (rutas: RutaProgramada[]) =>
    onBorrador(rutas.map((r, i) => ({ ...r, orden: i + 1 })));

  const mover = (indice: number, delta: -1 | 1) => {
    const destino = indice + delta;
    if (destino < 0 || destino >= visibles.length) return;
    const copia = [...visibles];
    [copia[indice], copia[destino]] = [copia[destino], copia[indice]];
    editar(copia);
  };

  const retirar = (indice: number) => editar(visibles.filter((_, i) => i !== indice));

  const asignadas = new Set(visibles.map((r) => r.rutaId));
  const opciones = (catalogo.data ?? [])
    .filter((r) => !asignadas.has(r.id))
    .map((r) => ({ value: String(r.id), label: `${r.codigo} · ${r.nombre}` }));

  const agregar = () => {
    const ruta = catalogo.data?.find((r) => String(r.id) === rutaAAgregar);
    if (!ruta) return;
    editar([
      ...visibles,
      { rutaId: ruta.id, codigo: ruta.codigo, nombre: ruta.nombre, color: ruta.color, orden: 0, estado: 'PENDIENTE' },
    ]);
    setRutaAAgregar(null);
  };

  const guardarCambios = () =>
    guardar.mutate(
      { conductorId: conductor.conductorId, fecha, rutas: visibles.map((r) => r.rutaId) },
      {
        onSuccess: () => {
          onBorrador(null);
          notificarExito('Programación guardada', `${conductor.nombre} · ${fechaLarga}`);
        },
        onError: notificarErrorAccion,
      },
    );

  let lista;
  if (consulta.isPending) {
    lista = (
      <Stack gap="xs" role="status" aria-label="Cargando programación">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} h={56} radius="md" />
        ))}
      </Stack>
    );
  } else if (consulta.isError) {
    lista = (
      <ErrorPantalla
        mensaje={mensajeDeError(consulta.error) ?? MENSAJE_INESPERADO}
        onReintentar={() => void consulta.refetch()}
      />
    );
  } else if (visibles.length === 0) {
    lista = (
      <Text c="dimmed" py="md">
        Sin rutas programadas para este día.
      </Text>
    );
  } else {
    lista = (
      <Stack component="ol" gap={6} p={0} m={0} style={{ listStyle: 'none' }} aria-label="Rutas en orden">
        {visibles.map((r, i) => {
          const bloqueada = r.estado === 'RETIRO_PENDIENTE';
          const enRecorrido = r.estado === 'EN_RECORRIDO';
          return (
            <Paper component="li" key={r.rutaId} withBorder p="sm" radius="md">
              <Group justify="space-between" wrap="nowrap" gap="sm">
                <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
                  <Text fw={700} w={20} ta="right" aria-label={`Orden ${i + 1}`}>
                    {i + 1}
                  </Text>
                  <InsigniaRuta codigo={r.codigo} color={r.color} tamano={36} />
                  <Stack gap={4} style={{ minWidth: 0 }}>
                    <Text fw={600} truncate>
                      {r.nombre}
                    </Text>
                    <EstadoFila ruta={r} />
                  </Stack>
                </Group>
                {!soloLectura && !bloqueada && (
                  <Group gap={2} wrap="nowrap">
                    <Tooltip label="Subir" withArrow>
                      <ActionIcon
                        variant="subtle"
                        color="gray"
                        size="lg"
                        aria-label={`Subir ${r.codigo}`}
                        disabled={i === 0}
                        onClick={() => mover(i, -1)}
                      >
                        <IconArrowUp size={18} />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label="Bajar" withArrow>
                      <ActionIcon
                        variant="subtle"
                        color="gray"
                        size="lg"
                        aria-label={`Bajar ${r.codigo}`}
                        disabled={i === visibles.length - 1}
                        onClick={() => mover(i, 1)}
                      >
                        <IconArrowDown size={18} />
                      </ActionIcon>
                    </Tooltip>
                    {!enRecorrido && (
                      <BotonAccion
                        etiqueta={`Retirar ${r.codigo}`}
                        color="red"
                        icono={<IconTrash size={18} />}
                        onClick={() => retirar(i)}
                      />
                    )}
                  </Group>
                )}
              </Group>
            </Paper>
          );
        })}
      </Stack>
    );
  }

  return (
    <Stack gap="lg">
      <Paper withBorder p={{ base: 'sm', sm: 'lg' }}>
        <Stack gap="md">
          <Group justify="space-between" align="flex-start" gap="sm">
            <div>
              <Title order={2} fz="xl">
                {conductor.nombre}
              </Title>
              <Text c="dimmed" size="sm">
                Programación del {fechaLarga} · se hace en este orden
              </Text>
              <Group gap={6} mt={8}>
                {conductor.disponible ? (
                  <Chip tono="exito">Disponible</Chip>
                ) : (
                  <Chip tono="aviso">No disponible</Chip>
                )}
                {conductor.enRecorrido && <BadgeEnRecorrido enRecorrido={conductor.enRecorrido} />}
              </Group>
            </div>
            {esHoy && (
              <Tooltip label="Guarde o descarte los cambios primero" disabled={!hayCambios} withArrow>
                <Button
                  variant="light"
                  leftSection={<IconArrowsExchange size={18} aria-hidden />}
                  // `data-disabled` en vez de `disabled` para que el tooltip siga funcionando.
                  data-disabled={reasignarBloqueado || undefined}
                  aria-disabled={reasignarBloqueado || undefined}
                  onClick={() => {
                    if (!reasignarBloqueado) setReasignarAbierto(true);
                  }}
                >
                  Reasignar por eventualidad
                </Button>
              </Tooltip>
            )}
          </Group>

          {soloLectura && (
            <Alert color="gray" variant="light" icon={<IconInfoCircle size={18} />}>
              Las programaciones de días pasados no se pueden modificar.
            </Alert>
          )}

          {lista}

          {!soloLectura && consulta.data && (
            <Stack gap={6}>
              <Group align="flex-end" gap="sm" wrap="wrap">
                <Select
                  label="Agregar ruta"
                  placeholder={opciones.length === 0 ? 'No hay más rutas para agregar' : 'Elija una ruta'}
                  data={opciones}
                  value={rutaAAgregar}
                  onChange={setRutaAAgregar}
                  searchable
                  nothingFoundMessage="Ninguna ruta coincide"
                  disabled={opciones.length === 0}
                  style={{ flex: 1, minWidth: 220 }}
                />
                <Button
                  variant="default"
                  leftSection={<IconPlus size={16} aria-hidden />}
                  onClick={agregar}
                  disabled={!rutaAAgregar}
                >
                  Agregar
                </Button>
              </Group>
              <Text size="xs" c="dimmed">
                Las rutas ya asignadas no aparecen en la lista.
              </Text>
            </Stack>
          )}

          {hayCambios && (
            <Alert color="orange" variant="light" icon={<IconInfoCircle size={18} />} role="status">
              <Group justify="space-between" gap="sm">
                <Text size="sm" fw={600} c={colores.avisoTexto}>
                  Tiene cambios sin guardar
                </Text>
                <Button size="xs" variant="default" onClick={() => onBorrador(null)} disabled={guardar.isPending}>
                  Descartar
                </Button>
              </Group>
            </Alert>
          )}

          {!soloLectura && (
            <Group justify="flex-end">
              <Button onClick={guardarCambios} loading={guardar.isPending} disabled={!hayCambios}>
                Guardar programación
              </Button>
            </Group>
          )}
        </Stack>
      </Paper>

      <HistorialReasignaciones conductorId={conductor.conductorId} fecha={fecha} catalogo={catalogo.data ?? []} />

      {esHoy && consulta.data && (
        <ReasignarModal
          abierto={reasignarAbierto}
          onCerrar={() => setReasignarAbierto(false)}
          conductor={conductor}
          programacion={consulta.data.rutas}
          catalogo={catalogo.data ?? []}
        />
      )}
    </Stack>
  );
}
