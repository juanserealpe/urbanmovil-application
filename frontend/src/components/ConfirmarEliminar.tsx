import { Text } from '@mantine/core';
import { modals } from '@mantine/modals';

interface Opciones {
  titulo: string;
  texto: string;
  etiquetaConfirmar?: string;
  colorConfirmar?: string;
  onConfirmar: () => void;
}

/** Diálogo de confirmación estándar (eliminar, reactivar…). */
export function confirmar({
  titulo,
  texto,
  etiquetaConfirmar = 'Eliminar',
  colorConfirmar = 'red',
  onConfirmar,
}: Opciones) {
  modals.openConfirmModal({
    title: <Text fw={600}>{titulo}</Text>,
    centered: true,
    children: <Text size="sm">{texto}</Text>,
    labels: { confirm: etiquetaConfirmar, cancel: 'Cancelar' },
    confirmProps: { color: colorConfirmar },
    onConfirm: onConfirmar,
  });
}
