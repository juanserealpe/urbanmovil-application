import { Group, Paper, Skeleton, Stack, Table, Text, Title } from '@mantine/core';
import type { RutaResumen } from '../../api/tipos';
import { ErrorPantalla } from '../../components/ErrorPantalla';
import { MENSAJE_INESPERADO, mensajeDeError } from '../../utils/errores';
import { formatearFechaHora } from '../../utils/fechas';
import { InsigniaRuta } from '../rutas/componentes';
import { useReasignaciones } from './hooks';

const COLOR_SIN_RUTA = '#6B7280';

function Insignias({ codigos, colorDe }: { codigos: string[]; colorDe: (codigo: string) => string }) {
  if (codigos.length === 0) return <Text c="dimmed">—</Text>;
  return (
    <Group gap={4} aria-label={codigos.join(', ')}>
      {codigos.map((c) => (
        <InsigniaRuta key={c} codigo={c} color={colorDe(c)} tamano={28} />
      ))}
    </Group>
  );
}

interface Props {
  conductorId: number;
  fecha: string;
  catalogo: RutaResumen[];
}

export function HistorialReasignaciones({ conductorId, fecha, catalogo }: Props) {
  const consulta = useReasignaciones({ conductorId, fecha });
  // El historial trae solo códigos; el color sale del catálogo (gris si la ruta ya no existe).
  const colorDe = (codigo: string) => catalogo.find((r) => r.codigo === codigo)?.color ?? COLOR_SIN_RUTA;
  const filas = consulta.data ?? [];

  let contenido;
  if (consulta.isPending) {
    contenido = (
      <Stack gap="xs" role="status" aria-label="Cargando historial">
        <Skeleton h={36} />
        <Skeleton h={36} />
      </Stack>
    );
  } else if (consulta.isError) {
    contenido = (
      <ErrorPantalla
        mensaje={mensajeDeError(consulta.error) ?? MENSAJE_INESPERADO}
        onReintentar={() => void consulta.refetch()}
      />
    );
  } else if (filas.length === 0) {
    contenido = <Text c="dimmed">Sin reasignaciones en esta fecha.</Text>;
  } else {
    contenido = (
      <Table.ScrollContainer minWidth={720}>
        <Table verticalSpacing="sm">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Fecha y hora</Table.Th>
              <Table.Th>Administrador</Table.Th>
              <Table.Th>Qué pasó</Table.Th>
              <Table.Th>Asignadas</Table.Th>
              <Table.Th>Quitadas</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {/* El backend ya las devuelve de la más reciente a la más antigua. */}
            {filas.map((r) => (
              <Table.Tr key={r.id}>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatearFechaHora(r.fechaHora)}</Table.Td>
                <Table.Td>{r.administrador.nombre}</Table.Td>
                <Table.Td maw={320}>{r.motivo}</Table.Td>
                <Table.Td>
                  <Insignias codigos={r.rutasAsignadas} colorDe={colorDe} />
                </Table.Td>
                <Table.Td>
                  <Insignias codigos={r.rutasQuitadas} colorDe={colorDe} />
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    );
  }

  return (
    <Paper withBorder p={{ base: 'sm', sm: 'lg' }}>
      <Title order={2} fz="lg" mb="sm">
        Historial de reasignaciones
      </Title>
      {contenido}
    </Paper>
  );
}
