import { IconBus } from '@tabler/icons-react';
import type { EnRecorrido } from '../api/tipos';
import { Chip } from './Chip';

export function BadgeEnRecorrido({ enRecorrido }: { enRecorrido: EnRecorrido }) {
  return (
    <Chip tono="info" icono={<IconBus size={14} aria-hidden />}>
      En recorrido · {enRecorrido.rutaCodigo}
    </Chip>
  );
}
