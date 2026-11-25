import { ActionIcon, Tooltip } from '@mantine/core';
import type { ReactNode } from 'react';

interface Props {
  etiqueta: string;
  icono: ReactNode;
  onClick: () => void;
  color?: string;
  /** Si viene, el botón queda deshabilitado y el tooltip explica por qué. */
  motivoDeshabilitado?: string | null;
}

/** Botón de solo ícono con `aria-label` y tooltip. */
export function BotonAccion({ etiqueta, icono, onClick, color = 'gray', motivoDeshabilitado }: Props) {
  const deshabilitado = Boolean(motivoDeshabilitado);
  return (
    <Tooltip label={motivoDeshabilitado ?? etiqueta} withArrow>
      {/* `data-disabled` en lugar de `disabled` para que el tooltip siga funcionando. */}
      <ActionIcon
        variant="subtle"
        color={color}
        size="lg"
        aria-label={deshabilitado ? `${etiqueta} (${motivoDeshabilitado})` : etiqueta}
        aria-disabled={deshabilitado || undefined}
        data-disabled={deshabilitado || undefined}
        onClick={(e) => {
          if (deshabilitado) {
            e.preventDefault();
            return;
          }
          onClick();
        }}
      >
        {icono}
      </ActionIcon>
    </Tooltip>
  );
}
