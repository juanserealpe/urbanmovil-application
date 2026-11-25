import { Stack, Text } from '@mantine/core';
import type { Conductor } from '../api/tipos';
import { textoMotivo } from '../utils/motivos';
import { Chip } from './Chip';

type Props = Pick<Conductor, 'disponible' | 'motivoNoDisponible' | 'detalleMotivo'>;

export function BadgeDisponibilidad({ disponible, motivoNoDisponible, detalleMotivo }: Props) {
  if (disponible) return <Chip tono="exito">Disponible</Chip>;
  const motivo = textoMotivo(motivoNoDisponible, detalleMotivo);
  return (
    <Stack gap={2} align="flex-start">
      <Chip tono="aviso">No disponible</Chip>
      {motivo && (
        <Text size="xs" c="dimmed" maw={220}>
          {motivo}
        </Text>
      )}
    </Stack>
  );
}
