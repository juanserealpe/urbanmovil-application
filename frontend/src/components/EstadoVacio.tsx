import { Stack, Text, ThemeIcon } from '@mantine/core';
import { IconInbox } from '@tabler/icons-react';
import type { ReactNode } from 'react';

interface Props {
  titulo: string;
  descripcion?: string;
  icono?: ReactNode;
}

export function EstadoVacio({ titulo, descripcion, icono }: Props) {
  return (
    <Stack align="center" gap="xs" py={48} px="md" ta="center">
      <ThemeIcon size={56} radius="xl" variant="light" color="gray" aria-hidden>
        {icono ?? <IconInbox size={28} />}
      </ThemeIcon>
      <Text fw={600}>{titulo}</Text>
      {descripcion && (
        <Text c="dimmed" size="sm">
          {descripcion}
        </Text>
      )}
    </Stack>
  );
}
