/** Paleta con la que el backend asigna color si no llega uno (FRONTEND-API.md §5.4). */
export const PALETA_RUTAS = ['#0057B8', '#D62828', '#2A9D8F', '#F4A261', '#6A4C93', '#E76F51'];

export const COLOR_ALTERNO = '#C25E00';
export const MAX_ALTERNOS = 10;
export const MAX_PUNTOS_TRAZADO = 2000;

const HEX = /^#[0-9A-Fa-f]{6}$/;
export const esColorHex = (valor: string | null | undefined): valor is string => HEX.test(valor ?? '');

/** Texto blanco o oscuro según el fondo, para que el código de la ruta se lea sobre cualquier color. */
export function colorTextoSobre(fondo: string): string {
  if (!esColorHex(fondo)) return '#FFFFFF';
  const canal = (i: number) => {
    const c = parseInt(fondo.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  };
  const luminancia = 0.2126 * canal(1) + 0.7152 * canal(3) + 0.0722 * canal(5);
  // Contraste contra blanco vs. contra #12161C.
  const contraBlanco = 1.05 / (luminancia + 0.05);
  const contraOscuro = (luminancia + 0.05) / 0.0186;
  return contraBlanco >= contraOscuro ? '#FFFFFF' : '#12161C';
}
