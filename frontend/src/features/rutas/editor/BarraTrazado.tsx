import { Button, Group, Paper, ScrollArea, SegmentedControl } from '@mantine/core';
import { IconArrowBackUp, IconEraser } from '@tabler/icons-react';
import { PRINCIPAL, type Objetivo, type TrazadoEditable } from './trazado';

interface Props {
  activo: Objetivo;
  alternos: TrazadoEditable[];
  puntosActivos: number;
  onActivar: (objetivo: Objetivo) => void;
  onDeshacer: () => void;
  onBorrar: () => void;
}

/** Barra flotante sobre el mapa: qué se traza y las acciones sobre ese trazado. */
export function BarraTrazado({ activo, alternos, puntosActivos, onActivar, onDeshacer, onBorrar }: Props) {
  const opciones = [
    { value: PRINCIPAL, label: 'Recorrido principal' },
    ...alternos.map((a, i) => ({ value: a.clave, label: `Alterno ${i + 1}` })),
  ];
  const sinPuntos = puntosActivos === 0;

  return (
    <Paper shadow="md" p={6} radius="md" withBorder>
      <Group gap="xs" wrap="wrap">
        <ScrollArea type="auto" scrollbarSize={6} offsetScrollbars="x" maw="100%">
          <SegmentedControl
            aria-label="Trazado que se está editando"
            size="xs"
            value={activo}
            onChange={onActivar}
            data={opciones}
          />
        </ScrollArea>
        <Group gap={6} wrap="nowrap">
          <Button
            size="xs"
            variant="default"
            leftSection={<IconArrowBackUp size={16} aria-hidden />}
            onClick={onDeshacer}
            disabled={sinPuntos}
          >
            Deshacer punto
          </Button>
          <Button
            size="xs"
            variant="default"
            c={sinPuntos ? undefined : 'red.7'}
            leftSection={<IconEraser size={16} aria-hidden />}
            onClick={onBorrar}
            disabled={sinPuntos}
          >
            Borrar trazado
          </Button>
        </Group>
      </Group>
    </Paper>
  );
}
