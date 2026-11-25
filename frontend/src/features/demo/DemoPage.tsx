import { Alert, Anchor, Group, Paper, SegmentedControl, Stack, Table, Text, Title } from '@mantine/core';
import { IconFlask, IconMapPin, IconPlayerStop } from '@tabler/icons-react';
import { useState } from 'react';
import { Link, Navigate } from 'react-router';
import type { EstadoRecorrido, RecorridoDemo } from '../../api/tipos';
import { colores } from '../../app/theme';
import { BotonAccion } from '../../components/BotonAccion';
import { CargadorPantalla } from '../../components/CargadorPantalla';
import { confirmar } from '../../components/ConfirmarEliminar';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { ErrorPantalla } from '../../components/ErrorPantalla';
import { EstadoVacio } from '../../components/EstadoVacio';
import { MENSAJE_INESPERADO, mensajeDeError, notificarErrorAccion, notificarExito } from '../../utils/errores';
import { formatearFechaHora } from '../../utils/fechas';
import { useFinalizarRecorrido, useModoDemo, useRecorridosDemo } from './hooks';
import { IniciarRecorridoForm } from './IniciarRecorridoForm';

const OPCIONES: { value: EstadoRecorrido; label: string }[] = [
  { value: 'ACTIVO', label: 'Activos' },
  { value: 'FINALIZADO', label: 'Finalizados' },
];

function TablaRecorridos() {
  const [estado, setEstado] = useState<EstadoRecorrido>('ACTIVO');
  const consulta = useRecorridosDemo(estado);
  const finalizar = useFinalizarRecorrido();
  const recorridos = consulta.data ?? [];
  const activos = estado === 'ACTIVO';

  const pedirFinalizar = (r: RecorridoDemo) =>
    confirmar({
      titulo: `¿Finalizar el recorrido de ${r.conductor.nombre} en ${r.ruta.codigo}?`,
      texto: 'El bus dejará de verse en el monitoreo y el conductor y el vehículo quedarán libres.',
      etiquetaConfirmar: 'Finalizar',
      onConfirmar: () =>
        finalizar.mutate(r.id, {
          onSuccess: () => notificarExito('Recorrido finalizado', `${r.conductor.nombre} · ${r.ruta.codigo}`),
          onError: notificarErrorAccion,
        }),
    });

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
  } else if (recorridos.length === 0) {
    contenido = <EstadoVacio titulo={activos ? 'No hay recorridos activos' : 'No hay recorridos finalizados'} />;
  } else {
    contenido = (
      <Table.ScrollContainer minWidth={760}>
        <Table verticalSpacing="sm" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Conductor</Table.Th>
              <Table.Th>Ruta</Table.Th>
              <Table.Th>Vehículo</Table.Th>
              <Table.Th>Inicio</Table.Th>
              {!activos && <Table.Th>Fin</Table.Th>}
              {activos && <Table.Th ta="right">Acciones</Table.Th>}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {recorridos.map((r) => (
              <Table.Tr key={r.id}>
                <Table.Td fw={600}>{r.conductor.nombre}</Table.Td>
                <Table.Td>
                  {r.ruta.codigo} · {r.ruta.nombre}
                </Table.Td>
                <Table.Td>
                  {r.vehiculo.numeroInterno} · {r.vehiculo.placa}
                </Table.Td>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatearFechaHora(r.inicio)}</Table.Td>
                {!activos && (
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>{r.fin ? formatearFechaHora(r.fin) : '—'}</Table.Td>
                )}
                {activos && (
                  <Table.Td>
                    <Group justify="flex-end">
                      <BotonAccion
                        etiqueta={`Finalizar recorrido de ${r.conductor.nombre}`}
                        color="red"
                        icono={<IconPlayerStop size={18} />}
                        onClick={() => pedirFinalizar(r)}
                      />
                    </Group>
                  </Table.Td>
                )}
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    );
  }

  return (
    <Paper withBorder p={{ base: 'sm', sm: 'lg' }}>
      <Group justify="space-between" mb="md" gap="sm">
        <Title order={2} fz="lg">
          Recorridos
        </Title>
        <Group gap="md">
          <Anchor component={Link} to="/monitoreo" size="sm">
            <Group gap={4} component="span">
              <IconMapPin size={16} aria-hidden />
              Ver en el monitoreo
            </Group>
          </Anchor>
          <SegmentedControl
            aria-label="Estado de los recorridos"
            value={estado}
            onChange={(v) => setEstado(v as EstadoRecorrido)}
            data={OPCIONES}
          />
        </Group>
      </Group>
      {contenido}
    </Paper>
  );
}

export function DemoPage() {
  const { modo, error, reintentar } = useModoDemo();
  if (modo === 'cargando') return <CargadorPantalla />;
  // Sin perfil demo (404), esta pantalla no existe.
  if (modo === 'inactivo') return <Navigate to="/" replace />;
  if (modo === 'desconocido') {
    return <ErrorPantalla mensaje={mensajeDeError(error) ?? MENSAJE_INESPERADO} onReintentar={reintentar} />;
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Recorridos de demostración"
        subtitulo="Inicie y finalice recorridos para probar las reglas que dependen de ellos"
      />
      <Alert
        variant="filled"
        bg={colores.avisoFondo}
        icon={<IconFlask size={18} color={colores.avisoTexto} aria-hidden />}
        mb="md"
      >
        <Text size="sm" c={colores.avisoTexto}>
          Herramienta de demostración. Reemplaza a la app del conductor, que todavía no existe.
        </Text>
      </Alert>
      <Stack gap="lg">
        <IniciarRecorridoForm />
        <TablaRecorridos />
      </Stack>
    </>
  );
}
