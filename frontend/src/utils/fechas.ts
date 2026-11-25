import dayjs from 'dayjs';
import 'dayjs/locale/es';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(customParseFormat);
dayjs.locale('es');

export const ZONA_NEGOCIO = 'America/Bogota';

const MESES_CORTOS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/** «24 nov 2026, 10:41 a. m.» en America/Bogota. */
export function formatearFechaHora(iso: string): string {
  const d = dayjs(iso).tz(ZONA_NEGOCIO);
  if (!d.isValid()) return iso;
  const meridiano = d.hour() < 12 ? 'a. m.' : 'p. m.';
  return `${d.date()} ${MESES_CORTOS[d.month()]} ${d.year()}, ${d.format('h:mm')} ${meridiano}`;
}

/** «10:41:05 a. m.» en America/Bogota. */
export function formatearHora(iso: string): string {
  const d = dayjs(iso).tz(ZONA_NEGOCIO);
  if (!d.isValid()) return iso;
  return `${d.format('h:mm:ss')} ${d.hour() < 12 ? 'a. m.' : 'p. m.'}`;
}

/** true si `valor` es una fecha `YYYY-MM-DD` válida. */
export function esFechaValida(valor: string | null | undefined): valor is string {
  return Boolean(valor) && dayjs(valor, 'YYYY-MM-DD', true).isValid();
}

/** «martes 24 de noviembre» a partir de `YYYY-MM-DD`. */
export function formatearFecha(yyyyMmDd: string): string {
  const d = dayjs(yyyyMmDd, 'YYYY-MM-DD', true);
  if (!d.isValid()) return yyyyMmDd;
  return d.locale('es').format('dddd D [de] MMMM');
}

/** `YYYY-MM-DD` de hoy en America/Bogota. */
export function hoyBogota(): string {
  return dayjs().tz(ZONA_NEGOCIO).format('YYYY-MM-DD');
}
