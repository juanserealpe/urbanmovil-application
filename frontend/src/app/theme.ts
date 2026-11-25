import { createTheme, type CSSVariablesResolver, type MantineColorsTuple } from '@mantine/core';

/** Tokens de color del panel (FRONTEND-FASE-1 §3.2). */
export const colores = {
  primario: '#1F4FD8',
  texto: '#12161C',
  textoSecundario: '#4B5563',
  fondo: '#F6F7F9',
  borde: '#E3E6EB',
  exitoTexto: '#0E5A2F',
  exitoFondo: '#DDF3E5',
  avisoTexto: '#7A3E00',
  avisoFondo: '#FFF4E5',
  error: '#B42318',
  activoMenu: '#E4EBFC',
  neutroTexto: '#374151',
  neutroFondo: '#EEF0F3',
} as const;

// Escala de 10 tonos con #1F4FD8 como tono 6.
const urbano: MantineColorsTuple = [
  '#EBF0FD',
  '#D6E0FA',
  '#ADC1F5',
  '#84A2F0',
  '#5B83EB',
  '#3566E2',
  '#1F4FD8',
  '#1940B0',
  '#133189',
  '#0D2261',
];

const fuenteSistema =
  "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";

export const theme = createTheme({
  primaryColor: 'urbano',
  primaryShade: 6,
  colors: { urbano },
  fontFamily: `Inter, ${fuenteSistema}`,
  headings: { fontFamily: `'Space Grotesk', ${fuenteSistema}`, fontWeight: '600' },
  radius: { md: '10px', lg: '14px' },
  defaultRadius: 'md',
  components: {
    Paper: { defaultProps: { radius: 'lg' } },
    Card: { defaultProps: { radius: 'lg' } },
    Notification: { defaultProps: { radius: 'lg' } },
  },
});

export const cssVariablesResolver: CSSVariablesResolver = () => ({
  variables: {},
  light: {
    '--mantine-color-text': colores.texto,
    '--mantine-color-body': colores.fondo,
    '--mantine-color-dimmed': colores.textoSecundario,
    '--mantine-color-default-border': colores.borde,
    '--mantine-color-error': colores.error,
  },
  dark: {},
});
