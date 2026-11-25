import { Button, Group, Paper, Stack, Table, Text } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconPencil, IconPlayerPlay, IconPlayerStop, IconPlus, IconTrash } from '@tabler/icons-react';
import { useState } from 'react';
import type { Vehiculo } from '../../api/tipos';
import { BadgeEnRecorrido } from '../../components/BadgeEnRecorrido';
import { BotonAccion } from '../../components/BotonAccion';
import { BuscadorConFiltros } from '../../components/BuscadorConFiltros';
import { CargadorPantalla } from '../../components/CargadorPantalla';
import { Chip } from '../../components/Chip';
import { confirmar } from '../../components/ConfirmarEliminar';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { ErrorPantalla } from '../../components/ErrorPantalla';
import { EstadoVacio } from '../../components/EstadoVacio';
import { MENSAJE_INESPERADO, mensajeDeError, notificarErrorAccion, notificarExito } from '../../utils/errores';
import { formatearFechaHora } from '../../utils/fechas';
import { DesactivarModal } from './DesactivarModal';
import { useCambiarEstadoVehiculo, useEliminarVehiculo, useVehiculos } from './hooks';
import { VehiculoForm } from './VehiculoForm';

type Filtro = 'todos' | 'activos' | 'inactivos';

const OPCIONES_FILTRO: { value: Filtro; label: string }[] = [
  { value: 'todos', label: 'Todos' },
  { value: 'activos', label: 'Activos' },
  { value: 'inactivos', label: 'Inactivos' },
];

const ACTIVO_POR_FILTRO: Record<Filtro, boolean | undefined> = {
  todos: undefined,
  activos: true,
  inactivos: false,
};

const MOTIVO_RECORRIDO = 'Tiene un recorrido activo';

function EstadoVehiculo({ vehiculo }: { vehiculo: Vehiculo }) {
  if (vehiculo.activo) return <Chip tono="exito">Activo</Chip>;
  return (
    <Stack gap={2} align="flex-start">
      <Chip tono="neutro">Inactivo</Chip>
      {vehiculo.motivoInactivacion && (
        <Text size="xs" c="dimmed" maw={260}>
          {vehiculo.motivoInactivacion}
        </Text>
      )}
      {vehiculo.fechaInactivacion && (
        <Text size="xs" c="dimmed">
          desde {formatearFechaHora(vehiculo.fechaInactivacion)}
        </Text>
      )}
    </Stack>
  );
}

export function VehiculosPage() {
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState<Filtro>('todos');
  const [q] = useDebouncedValue(busqueda.trim(), 300);

  const consulta = useVehiculos({ q, activo: ACTIVO_POR_FILTRO[filtro] });
  const eliminar = useEliminarVehiculo();
  const cambiarEstado = useCambiarEstadoVehiculo();

  const [formAbierto, setFormAbierto] = useState(false);
  const [enEdicion, setEnEdicion] = useState<Vehiculo | null>(null);
  const [desactivarAbierto, setDesactivarAbierto] = useState(false);
  const [enDesactivacion, setEnDesactivacion] = useState<Vehiculo | null>(null);

  const abrirRegistro = () => {
    setEnEdicion(null);
    setFormAbierto(true);
  };
  const abrirEdicion = (v: Vehiculo) => {
    setEnEdicion(v);
    setFormAbierto(true);
  };
  const abrirDesactivar = (v: Vehiculo) => {
    setEnDesactivacion(v);
    setDesactivarAbierto(true);
  };

  const pedirReactivar = (v: Vehiculo) =>
    confirmar({
      titulo: `¿Reactivar ${v.numeroInterno}?`,
      texto: 'Volverá a estar disponible para recorridos.',
      etiquetaConfirmar: 'Reactivar',
      colorConfirmar: 'urbano',
      onConfirmar: () =>
        cambiarEstado.mutate(
          { id: v.id, datos: { activo: true } },
          {
            onSuccess: () => notificarExito('Vehículo reactivado', v.numeroInterno),
            onError: notificarErrorAccion,
          },
        ),
    });

  const pedirEliminar = (v: Vehiculo) =>
    confirmar({
      titulo: `¿Eliminar ${v.numeroInterno}?`,
      texto: `Se eliminará el vehículo con placa ${v.placa}. No se puede deshacer.`,
      onConfirmar: () =>
        eliminar.mutate(v.id, {
          onSuccess: () => notificarExito('Vehículo eliminado', v.numeroInterno),
          onError: notificarErrorAccion,
        }),
    });

  const vehiculos = consulta.data ?? [];
  const hayFiltros = q !== '' || filtro !== 'todos';
  const total = vehiculos.length;

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
      <EstadoVacio titulo={hayFiltros ? 'Ningún vehículo coincide con la búsqueda' : 'No hay vehículos registrados'} />
    );
  } else {
    contenido = (
      <Table.ScrollContainer minWidth={760}>
        <Table verticalSpacing="sm" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Número interno</Table.Th>
              <Table.Th>Placa</Table.Th>
              <Table.Th>Estado</Table.Th>
              <Table.Th>En recorrido</Table.Th>
              <Table.Th ta="right">Acciones</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {vehiculos.map((v) => {
              const bloqueo = v.enRecorrido ? MOTIVO_RECORRIDO : null;
              return (
                <Table.Tr key={v.id}>
                  <Table.Td>
                    <Text fw={600}>{v.numeroInterno}</Text>
                  </Table.Td>
                  <Table.Td>{v.placa}</Table.Td>
                  <Table.Td>
                    <EstadoVehiculo vehiculo={v} />
                  </Table.Td>
                  <Table.Td>{v.enRecorrido ? <BadgeEnRecorrido enRecorrido={v.enRecorrido} /> : '—'}</Table.Td>
                  <Table.Td>
                    <Group gap={4} justify="flex-end" wrap="nowrap">
                      <BotonAccion etiqueta="Editar" icono={<IconPencil size={18} />} onClick={() => abrirEdicion(v)} />
                      {v.activo ? (
                        <BotonAccion
                          etiqueta="Desactivar"
                          color="orange"
                          icono={<IconPlayerStop size={18} />}
                          motivoDeshabilitado={bloqueo}
                          onClick={() => abrirDesactivar(v)}
                        />
                      ) : (
                        <BotonAccion
                          etiqueta="Reactivar"
                          color="teal"
                          icono={<IconPlayerPlay size={18} />}
                          onClick={() => pedirReactivar(v)}
                        />
                      )}
                      <BotonAccion
                        etiqueta="Eliminar"
                        color="red"
                        icono={<IconTrash size={18} />}
                        motivoDeshabilitado={bloqueo}
                        onClick={() => pedirEliminar(v)}
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
        titulo="Vehículos"
        subtitulo="Buses de la empresa habilitados para operar"
        acciones={
          <Button leftSection={<IconPlus size={18} />} onClick={abrirRegistro}>
            Registrar vehículo
          </Button>
        }
      />

      <Paper withBorder p={{ base: 'sm', sm: 'lg' }}>
        <BuscadorConFiltros
          etiquetaBusqueda="Buscar"
          placeholder="Placa o número interno"
          busqueda={busqueda}
          onBusqueda={setBusqueda}
          etiquetaFiltro="Estado"
          filtro={filtro}
          onFiltro={setFiltro}
          opciones={OPCIONES_FILTRO}
          contador={consulta.data ? `${total} ${total === 1 ? 'vehículo' : 'vehículos'}` : undefined}
        />
        {contenido}
      </Paper>

      <Text size="sm" c="dimmed" mt="md">
        Un vehículo en recorrido no se puede eliminar ni desactivar hasta que termine.
      </Text>

      <VehiculoForm abierto={formAbierto} vehiculo={enEdicion} onCerrar={() => setFormAbierto(false)} />
      <DesactivarModal
        abierto={desactivarAbierto}
        vehiculo={enDesactivacion}
        onCerrar={() => setDesactivarAbierto(false)}
      />
    </>
  );
}
