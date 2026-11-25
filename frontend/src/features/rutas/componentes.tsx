import { Box, Group, Text } from '@mantine/core';
import { COLOR_ALTERNO, colorTextoSobre } from './constantes';

/** Insignia cuadrada con el código sobre el color de la ruta. */
export function InsigniaRuta({ codigo, color, tamano = 40 }: { codigo: string; color: string; tamano?: number }) {
  return (
    <Box
      w={tamano}
      h={tamano}
      bg={color}
      c={colorTextoSobre(color)}
      fw={700}
      fz={codigo.length > 3 ? 11 : 14}
      style={{
        borderRadius: 8,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        overflow: 'hidden',
      }}
      aria-hidden
    >
      {codigo}
    </Box>
  );
}

/** Muestra de línea para leyendas y listas: sólida o punteada. */
export function MuestraLinea({ color, punteada = false, ancho = 28 }: { color: string; punteada?: boolean; ancho?: number }) {
  return (
    <svg width={ancho} height={8} viewBox={`0 0 ${ancho} 8`} aria-hidden style={{ flexShrink: 0 }}>
      <line
        x1={2}
        y1={4}
        x2={ancho - 2}
        y2={4}
        stroke={color}
        strokeWidth={4}
        strokeLinecap={punteada ? 'butt' : 'round'}
        strokeDasharray={punteada ? '6 4' : undefined}
      />
    </svg>
  );
}

export function LeyendaRuta({ colorPrincipal }: { colorPrincipal: string }) {
  return (
    <Group gap="lg" aria-label="Leyenda del mapa" role="list">
      <Group gap={8} role="listitem">
        <MuestraLinea color={colorPrincipal} />
        <Text size="sm">Recorrido principal</Text>
      </Group>
      <Group gap={8} role="listitem">
        <MuestraLinea color={COLOR_ALTERNO} punteada />
        <Text size="sm">Camino alterno</Text>
      </Group>
    </Group>
  );
}
