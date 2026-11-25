import type { MotivoNoDisponible } from '../api/tipos';

export const ETIQUETA_MOTIVO: Record<MotivoNoDisponible, string> = {
  INCAPACIDAD: 'Incapacidad',
  VACACIONES: 'Vacaciones',
  PERMISO: 'Permiso',
  OTRO: 'Otro',
};

export const OPCIONES_MOTIVO = (Object.keys(ETIQUETA_MOTIVO) as MotivoNoDisponible[]).map((value) => ({
  value,
  label: ETIQUETA_MOTIVO[value],
}));

/** «Vacaciones», «Otro: Cita médica»… */
export function textoMotivo(motivo: MotivoNoDisponible | null, detalle: string | null): string | null {
  if (!motivo) return null;
  if (motivo === 'OTRO') return detalle ? `Otro: ${detalle}` : 'Otro';
  return ETIQUETA_MOTIVO[motivo];
}
