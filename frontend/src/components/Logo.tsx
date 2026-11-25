import { ThemeIcon } from '@mantine/core';
import { IconBus } from '@tabler/icons-react';

export function Logo({ tamano = 36 }: { tamano?: number }) {
  return (
    <ThemeIcon size={tamano} radius="md" color="urbano" aria-hidden>
      <IconBus size={Math.round(tamano * 0.6)} stroke={1.8} />
    </ThemeIcon>
  );
}
