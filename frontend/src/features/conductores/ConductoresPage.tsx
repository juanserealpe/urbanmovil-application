import { Button, Group, Paper, Stack, Table, Text } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconPencil, IconPlus, IconTrash, IconUserCheck } from '@tabler/icons-react';
import { useState } from 'react';
import type { Conductor } from '../../api/tipos';
import { BadgeDisponibilidad } from '../../components/BadgeDisponibilidad';
import { BadgeEnRecorrido } from '../../components/BadgeEnRecorrido';
import { BotonAccion } from '../../components/BotonAccion';
import { BuscadorConFiltros } from '../../components/BuscadorConFiltros';
import { CargadorPantalla } from '../../components/CargadorPantalla';
import { confirmar } from '../../components/ConfirmarEliminar';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { ErrorPantalla } from '../../components/ErrorPantalla';
import { EstadoVacio } from '../../components/EstadoVacio';
import { MENSAJE_INESPERADO, mensajeDeError, notificarErrorAccion, notificarExito } from '../../utils/errores';
import { ConductorForm } from './ConductorForm';
import { DisponibilidadModal } from './DisponibilidadModal';
import { useConductores, useEliminarConductor } from './hooks';

type Filtro = 'todos' | 'disponibles' | 'no_disponibles';

const OPCIONES_FILTRO: { value: Filtro; label: string }[] = [
  { value: 'todos', label: 'Todos' },
  { value: 'disponibles', label: 'Disponibles' },
  { value: 'no_disponibles', label: 'No disponibles' },
];

const DISPONIBLE_POR_FILTRO: Record<Filtro, boolean | undefined> = {
  todos: undefined,
  disponibles: true,
  no_disponibles: false,
};

const MOTIVO_RECORRIDO = 'Tiene un recorrido activo';

export function ConductoresPage() {
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState<Filtro>('todos');
  const [q] = useDebouncedValue(busqueda.trim(), 300);

  const consulta = useConductores({ q, disponible: DISPONIBLE_POR_FILTRO[filtro] });
  const eliminar = useEliminarConductor();

  // El drawer y el modal guardan el último conductor para no vaciarse durante la animación de cierre.
  const [formAbierto, setFormAbierto] = useState(false);
  const [enEdicion, setEnEdicion] = useState<Conductor | null>(null);
  const [dispAbierto, setDispAbierto] = useState(false);
  const [enDisponibilidad, setEnDisponibilidad] = useState<Conductor | null>(null);

  const abrirRegistro = () => {
    setEnEdicion(null);
    setFormAbierto(true);
  };
  const abrirEdicion = (c: Conductor) => {
    setEnEdicion(c);
    setFormAbierto(true);
  };
  const abrirDisponibilidad = (c: Conductor) => {
    setEnDisponibilidad(c);
    setDispAbierto(true);
  };

  const pedirEliminar = (c: Conductor) =>
    confirmar({
      titulo: `¿Eliminar a ${c.nombre}?`,
      texto: 'Se borrarán también su programación, reasignaciones y recorridos finalizados. No se puede deshacer.',
      onConfirmar: () =>
        eliminar.mutate(c.id, {
          onSuccess: () => notificarExito('Conductor eliminado', c.nombre),
          onError: notificarErrorAccion,
        }),
    });

  const conductores = consulta.data ?? [];
  const hayFiltros = q !== '' || filtro !== 'todos';
  const total = conductores.length;

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
      <EstadoVacio titulo={hayFiltros ? 'Ningún conductor coincide con la búsqueda' : 'No hay conductores registrados'} />
    );
  } else {
    contenido = (
      <Table.ScrollContainer minWidth={900}>
        <Table verticalSpacing="sm" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Conductor</Table.Th>
              <Table.Th>Correo</Table.Th>
              <Table.Th>Teléfono</Table.Th>
              <Table.Th>Disponibilidad</Table.Th>
              <Table.Th>Rutas de hoy</Table.Th>
              <Table.Th ta="right">Acciones</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {conductores.map((c) => (
              <Table.Tr key={c.id}>
                <Table.Td>
                  <Text fw={600}>{c.nombre}</Text>
                  <Text size="sm" c="dimmed">
                    CC {c.identificacion}
                  </Text>
                </Table.Td>
                <Table.Td>{c.correo}</Table.Td>
                <Table.Td>{c.telefono}</Table.Td>
                <Table.Td>
                  <Stack gap={6} align="flex-start">
                    <BadgeDisponibilidad
                      disponible={c.disponible}
                      motivoNoDisponible={c.motivoNoDisponible}
                      detalleMotivo={c.detalleMotivo}
                    />
                    {c.enRecorrido && <BadgeEnRecorrido enRecorrido={c.enRecorrido} />}
                  </Stack>
                </Table.Td>
                <Table.Td>{c.rutasHoy.length > 0 ? c.rutasHoy.join(', ') : '—'}</Table.Td>
                <Table.Td>
                  <Group gap={4} justify="flex-end" wrap="nowrap">
                    <BotonAccion etiqueta="Editar" icono={<IconPencil size={18} />} onClick={() => abrirEdicion(c)} />
                    <BotonAccion
                      etiqueta="Cambiar disponibilidad"
                      icono={<IconUserCheck size={18} />}
                      onClick={() => abrirDisponibilidad(c)}
                    />
                    <BotonAccion
                      etiqueta="Eliminar"
                      color="red"
                      icono={<IconTrash size={18} />}
                      motivoDeshabilitado={c.enRecorrido ? MOTIVO_RECORRIDO : null}
                      onClick={() => pedirEliminar(c)}
                    />
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    );
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Conductores"
        subtitulo="Personal habilitado para usar la app del conductor"
        acciones={
          <Button leftSection={<IconPlus size={18} />} onClick={abrirRegistro}>
            Registrar conductor
          </Button>
        }
      />

      <Paper withBorder p={{ base: 'sm', sm: 'lg' }}>
        <BuscadorConFiltros
          etiquetaBusqueda="Buscar"
          placeholder="Nombre, identificación o correo"
          busqueda={busqueda}
          onBusqueda={setBusqueda}
          etiquetaFiltro="Disponibilidad"
          filtro={filtro}
          onFiltro={setFiltro}
          opciones={OPCIONES_FILTRO}
          contador={consulta.data ? `${total} ${total === 1 ? 'conductor' : 'conductores'}` : undefined}
        />
        {contenido}
      </Paper>

      <Text size="sm" c="dimmed" mt="md">
        Un conductor en recorrido no se puede eliminar ni marcar como no disponible hasta que termine.
      </Text>

      <ConductorForm abierto={formAbierto} conductor={enEdicion} onCerrar={() => setFormAbierto(false)} />
      <DisponibilidadModal
        abierto={dispAbierto}
        conductor={enDisponibilidad}
        onCerrar={() => setDispAbierto(false)}
      />
    </>
  );
}
