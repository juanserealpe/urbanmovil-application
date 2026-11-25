import { Button, Group, Paper, Stack, Table, Text, TextInput } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconBus, IconPencil, IconPlus, IconRoute, IconSearch, IconTrash } from '@tabler/icons-react';
import { useState, type KeyboardEvent } from 'react';
import { useLocation, useNavigate } from 'react-router';
import type { RutaResumen } from '../../api/tipos';
import { BotonAccion } from '../../components/BotonAccion';
import { CargadorPantalla } from '../../components/CargadorPantalla';
import { Chip } from '../../components/Chip';
import { confirmar } from '../../components/ConfirmarEliminar';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { ErrorPantalla } from '../../components/ErrorPantalla';
import { EstadoVacio } from '../../components/EstadoVacio';
import { colores } from '../../app/theme';
import { MENSAJE_INESPERADO, mensajeDeError, notificarErrorAccion, notificarExito } from '../../utils/errores';
import { formatearFechaHora } from '../../utils/fechas';
import { InsigniaRuta } from './componentes';
import { useEliminarRuta, useRutas } from './hooks';
import { VistaPreviaRuta } from './VistaPreviaRuta';

/** Estado de navegación con el que el editor vuelve a la lista. */
export interface EstadoNavegacionRutas {
  seleccionada?: number;
}

const MOTIVO_RECORRIDOS = 'Tiene recorridos activos';

function EstadoRuta({ ruta }: { ruta: RutaResumen }) {
  const n = ruta.recorridosActivos;
  if (n > 0) {
    return (
      <Chip tono="info" icono={<IconBus size={14} aria-hidden />}>
        {n === 1 ? '1 recorrido activo' : `${n} recorridos activos`}
      </Chip>
    );
  }
  return <Chip tono="neutro">Sin recorridos activos</Chip>;
}

export function RutasPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const estado = location.state as EstadoNavegacionRutas | null;

  const [busqueda, setBusqueda] = useState('');
  const [q] = useDebouncedValue(busqueda.trim(), 300);
  const consulta = useRutas(q);
  const eliminar = useEliminarRuta();

  const [elegida, setElegida] = useState<number | null>(estado?.seleccionada ?? null);

  const rutas = consulta.data ?? [];
  const total = rutas.length;
  // Si la elegida no está en la lista (filtro, eliminación), se muestra la primera.
  const seleccionada = rutas.find((r) => r.id === elegida)?.id ?? rutas[0]?.id ?? null;

  const pedirEliminar = (r: RutaResumen) =>
    confirmar({
      titulo: `¿Eliminar la ruta ${r.codigo}?`,
      texto:
        'Dejará de verse para pasajeros y conductores y se retirará de las programaciones. No se puede deshacer.',
      etiquetaConfirmar: 'Eliminar ruta',
      onConfirmar: () =>
        eliminar.mutate(r.id, {
          onSuccess: () => notificarExito('Ruta eliminada', `${r.codigo} · ${r.nombre}`),
          onError: notificarErrorAccion,
        }),
    });

  const alTeclaFila = (e: KeyboardEvent, id: number) => {
    if (e.target !== e.currentTarget) return;
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setElegida(id);
    }
  };

  let contenido;
  if (consulta.isPending) {
    contenido = <CargadorPantalla />;
  } else if (consulta.isError && !consulta.data) {
    contenido = (
      <ErrorPantalla
        mensaje={mensajeDeError(consulta.error) ?? MENSAJE_INESPERADO}
        onReintentar={() => void consulta.refetch()}
      />
    );
  } else if (total === 0) {
    contenido = (
      <EstadoVacio
        icono={<IconRoute size={28} />}
        titulo={q ? 'Ninguna ruta coincide con la búsqueda' : 'Todavía no hay rutas. Cree la primera con Nueva ruta.'}
      />
    );
  } else {
    contenido = (
      <Table.ScrollContainer minWidth={900}>
        <Table verticalSpacing="sm" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Ruta</Table.Th>
              <Table.Th>Recorrido principal</Table.Th>
              <Table.Th>Alternos</Table.Th>
              <Table.Th>Asignada hoy a</Table.Th>
              <Table.Th>Estado</Table.Th>
              <Table.Th ta="right">Acciones</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rutas.map((r) => {
              const esSeleccionada = r.id === seleccionada;
              return (
                <Table.Tr
                  key={r.id}
                  onClick={() => setElegida(r.id)}
                  onKeyDown={(e) => alTeclaFila(e, r.id)}
                  tabIndex={0}
                  aria-label={`${r.codigo} ${r.nombre}. ${esSeleccionada ? 'En vista previa' : 'Ver vista previa'}`}
                  style={{
                    cursor: 'pointer',
                    background: esSeleccionada ? colores.activoMenu : undefined,
                  }}
                >
                  <Table.Td>
                    <Group gap="sm" wrap="nowrap">
                      <InsigniaRuta codigo={r.codigo} color={r.color} />
                      <div>
                        <Text fw={600}>{r.nombre}</Text>
                        <Text size="xs" c="dimmed">
                          Editada {formatearFechaHora(r.actualizadoEn)}
                        </Text>
                      </div>
                    </Group>
                  </Table.Td>
                  <Table.Td>{r.puntosRecorrido === 1 ? '1 punto' : `${r.puntosRecorrido} puntos`}</Table.Td>
                  <Table.Td>{r.numAlternos}</Table.Td>
                  <Table.Td>
                    {r.conductoresHoy.length > 0 ? (
                      r.conductoresHoy.join(', ')
                    ) : (
                      <Text c="dimmed" size="sm" component="span">
                        Sin asignar
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td>
                    <EstadoRuta ruta={r} />
                  </Table.Td>
                  {/* Los botones no deben cambiar la selección de la fila. */}
                  <Table.Td onClick={(e) => e.stopPropagation()} onKeyDown={(e) => e.stopPropagation()}>
                    <Group gap={4} justify="flex-end" wrap="nowrap">
                      <BotonAccion
                        etiqueta={`Editar ${r.codigo}`}
                        icono={<IconPencil size={18} />}
                        onClick={() => navigate(`/rutas/${r.id}`)}
                      />
                      <BotonAccion
                        etiqueta={`Eliminar ${r.codigo}`}
                        color="red"
                        icono={<IconTrash size={18} />}
                        motivoDeshabilitado={r.recorridosActivos > 0 ? MOTIVO_RECORRIDOS : null}
                        onClick={() => pedirEliminar(r)}
                      />
                    </Group>
                  </Table.Td>
                </Table.Tr>
              );
            })}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    );
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Rutas"
        subtitulo="Rutas de la empresa que ven pasajeros y conductores"
        acciones={
          <Button leftSection={<IconPlus size={18} />} onClick={() => navigate('/rutas/nueva')}>
            Nueva ruta
          </Button>
        }
      />

      <Paper withBorder p={{ base: 'sm', sm: 'lg' }}>
        <Group justify="space-between" align="flex-end" gap="md" mb="md" wrap="wrap">
          <TextInput
            label="Buscar"
            placeholder="Buscar por código o nombre"
            leftSection={<IconSearch size={16} aria-hidden />}
            value={busqueda}
            onChange={(e) => setBusqueda(e.currentTarget.value)}
            w={{ base: '100%', sm: 320 }}
          />
          {consulta.data && (
            <Text c="dimmed" size="sm" aria-live="polite">
              {total === 1 ? '1 ruta' : `${total} rutas`}
            </Text>
          )}
        </Group>
        <Stack gap={0}>{contenido}</Stack>
      </Paper>

      {seleccionada !== null && <VistaPreviaRuta id={seleccionada} />}
    </>
  );
}
