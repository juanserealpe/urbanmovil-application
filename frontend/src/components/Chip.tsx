import { Badge } from '@mantine/core';
import type { ReactNode } from 'react';
import { colores } from '../app/theme';

export type TonoChip = 'exito' | 'aviso' | 'info' | 'neutro';

const TONOS: Record<TonoChip, { bg: string; c: string }> = {
  exito: { bg: colores.exitoFondo, c: colores.exitoTexto },
  aviso: { bg: colores.avisoFondo, c: colores.avisoTexto },
  info: { bg: colores.activoMenu, c: colores.primario },
  neutro: { bg: colores.neutroFondo, c: colores.neutroTexto },
};

interface Props {
  tono: TonoChip;
  children: ReactNode;
  icono?: ReactNode;
}

/** Etiqueta de estado. Siempre lleva texto: el estado no depende solo del color. */
export function Chip({ tono, children, icono }: Props) {
  const { bg, c } = TONOS[tono];
  return (
    <Badge variant="filled" bg={bg} c={c} radius="xl" size="lg" tt="none" fw={600} leftSection={icono}>
      {children}
    </Badge>
  );
}
