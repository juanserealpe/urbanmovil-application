import { IconClockHour4 } from '@tabler/icons-react';
import { EncabezadoPagina } from './EncabezadoPagina';
import { EstadoVacio } from './EstadoVacio';

export function PaginaProvisional({ titulo }: { titulo: string }) {
  return (
    <>
      <EncabezadoPagina titulo={titulo} />
      <EstadoVacio icono={<IconClockHour4 size={28} />} titulo="Disponible en la siguiente fase" />
    </>
  );
}
