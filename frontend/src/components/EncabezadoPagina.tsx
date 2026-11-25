import { Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

interface Props {
  titulo: string;
  subtitulo?: string;
  acciones?: ReactNode;
}

export function EncabezadoPagina({ titulo, subtitulo, acciones }: Props) {
  return (
    <Group justify="space-between" align="flex-start" mb="lg" gap="md">
      <Stack gap={4}>
        <Title order={1} fz={28} lh={1.2}>
          {titulo}
        </Title>
        {subtitulo && <Text c="dimmed">{subtitulo}</Text>}
      </Stack>
      {acciones && <Group gap="sm">{acciones}</Group>}
    </Group>
  );
}
