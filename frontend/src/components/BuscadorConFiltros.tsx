import { Group, SegmentedControl, Text, TextInput } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';

interface Opcion<V extends string> {
  value: V;
  label: string;
}

interface Props<V extends string> {
  busqueda: string;
  onBusqueda: (valor: string) => void;
  placeholder: string;
  etiquetaBusqueda: string;
  filtro: V;
  onFiltro: (valor: V) => void;
  opciones: Opcion<V>[];
  etiquetaFiltro: string;
  contador?: string;
}

export function BuscadorConFiltros<V extends string>({
  busqueda,
  onBusqueda,
  placeholder,
  etiquetaBusqueda,
  filtro,
  onFiltro,
  opciones,
  etiquetaFiltro,
  contador,
}: Props<V>) {
  return (
    <Group justify="space-between" align="flex-end" gap="md" mb="md" wrap="wrap">
      <Group gap="md" align="flex-end" wrap="wrap" style={{ flex: 1 }}>
        <TextInput
          label={etiquetaBusqueda}
          placeholder={placeholder}
          leftSection={<IconSearch size={16} aria-hidden />}
          value={busqueda}
          onChange={(e) => onBusqueda(e.currentTarget.value)}
          w={{ base: '100%', sm: 320 }}
        />
        <div>
          <Text component="span" size="sm" fw={500} display="block" mb={4} id={`filtro-${etiquetaFiltro}`}>
            {etiquetaFiltro}
          </Text>
          <SegmentedControl
            aria-labelledby={`filtro-${etiquetaFiltro}`}
            value={filtro}
            onChange={(v) => onFiltro(v as V)}
            data={opciones}
          />
        </div>
      </Group>
      {contador && (
        <Text c="dimmed" size="sm">
          {contador}
        </Text>
      )}
    </Group>
  );
}
