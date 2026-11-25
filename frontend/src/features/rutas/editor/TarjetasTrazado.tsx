import { Box, Button, Group, Paper, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconCheck, IconPlus, IconTrash } from '@tabler/icons-react';
import { colores } from '../../../app/theme';
import { BotonAccion } from '../../../components/BotonAccion';
import { Chip } from '../../../components/Chip';
import { MuestraLinea } from '../componentes';
import { COLOR_ALTERNO, MAX_ALTERNOS } from '../constantes';
import type { ErroresTrazado } from './validacion';
import type { TrazadoEditable } from './trazado';

const textoPuntos = (n: number) => (n === 1 ? '1 punto' : `${n} puntos`);

function MensajeError({ children }: { children?: string }) {
  if (!children) return null;
  return (
    <Text size="sm" c="red.8" role="alert">
      {children}
    </Text>
  );
}

// ---------- Recorrido principal ----------

interface PropsPrincipal {
  puntos: number;
  activo: boolean;
  error?: string;
  onTrazar: () => void;
}

export function TarjetaPrincipal({ puntos, activo, error, onTrazar }: PropsPrincipal) {
  const completo = puntos >= 2;
  return (
    <Paper withBorder p="md" style={{ borderColor: error ? colores.error : activo ? colores.primario : undefined }}>
      <Stack gap="sm">
        <Group justify="space-between" gap="xs">
          <Title order={2} fz="md">
            Recorrido principal
          </Title>
          <Button size="compact-sm" variant={activo ? 'filled' : 'light'} onClick={onTrazar} aria-pressed={activo}>
            {activo ? 'Trazando' : 'Trazar'}
          </Button>
        </Group>
        <Group gap={6} aria-live="polite">
          <Chip tono={completo ? 'exito' : 'neutro'}>{textoPuntos(puntos)}</Chip>
          {completo ? (
            <>
              <Chip tono="exito" icono={<IconCheck size={14} aria-hidden />}>
                Inicio marcado
              </Chip>
              <Chip tono="exito" icono={<IconCheck size={14} aria-hidden />}>
                Final marcado
              </Chip>
            </>
          ) : (
            <Chip tono="aviso">Falta trazar</Chip>
          )}
        </Group>
        <MensajeError>{error}</MensajeError>
        <Text size="sm" c="dimmed">
          Se usa para tiempos de llegada y búsqueda. Conserva el sentido en que se trazó.
        </Text>
      </Stack>
    </Paper>
  );
}

// ---------- Caminos alternos ----------

interface PropsAlternos {
  alternos: TrazadoEditable[];
  activo: string;
  errores: ErroresTrazado;
  onTrazar: (clave: string) => void;
  onRenombrar: (clave: string, nombre: string) => void;
  onEliminar: (alterno: TrazadoEditable) => void;
  onAgregar: () => void;
}

export function TarjetaAlternos({ alternos, activo, errores, onTrazar, onRenombrar, onEliminar, onAgregar }: PropsAlternos) {
  const lleno = alternos.length >= MAX_ALTERNOS;
  return (
    <Paper withBorder p="md">
      <Stack gap="sm">
        <Title order={2} fz="md">
          Caminos alternos
        </Title>

        {alternos.length === 0 && (
          <Text size="sm" c="dimmed">
            Esta ruta no tiene caminos alternos.
          </Text>
        )}

        {alternos.map((a, i) => {
          const esActivo = a.clave === activo;
          const err = errores.alternos[a.clave];
          const conError = Boolean(err?.puntos || err?.otro || err?.nombre);
          return (
            <Paper
              key={a.clave}
              withBorder
              p="sm"
              radius="md"
              style={{ borderColor: conError ? colores.error : esActivo ? COLOR_ALTERNO : undefined }}
            >
              <Stack gap={6}>
                <Group gap="xs" wrap="nowrap" align="flex-start">
                  <Box pt={14}>
                    <MuestraLinea color={COLOR_ALTERNO} punteada ancho={24} />
                  </Box>
                  <TextInput
                    style={{ flex: 1 }}
                    size="sm"
                    aria-label={`Nombre del camino alterno ${i + 1}`}
                    value={a.nombre}
                    onChange={(e) => onRenombrar(a.clave, e.currentTarget.value)}
                    error={err?.nombre}
                  />
                </Group>
                <Group justify="space-between" gap="xs" wrap="nowrap">
                  <Text size="sm" c="dimmed">
                    {textoPuntos(a.puntos.length)}
                  </Text>
                  <Group gap={4} wrap="nowrap">
                    <Button
                      size="compact-sm"
                      color="orange.9"
                      variant={esActivo ? 'filled' : 'light'}
                      onClick={() => onTrazar(a.clave)}
                      aria-pressed={esActivo}
                      aria-label={`${esActivo ? 'Trazando' : 'Trazar'} ${a.nombre || `alterno ${i + 1}`}`}
                    >
                      {esActivo ? 'Trazando' : 'Trazar'}
                    </Button>
                    <BotonAccion
                      etiqueta={`Eliminar ${a.nombre || `alterno ${i + 1}`}`}
                      color="red"
                      icono={<IconTrash size={18} />}
                      onClick={() => onEliminar(a)}
                    />
                  </Group>
                </Group>
                <MensajeError>{err?.puntos}</MensajeError>
                <MensajeError>{err?.otro}</MensajeError>
              </Stack>
            </Paper>
          );
        })}

        <MensajeError>{errores.listaAlternos}</MensajeError>

        <Button
          variant="default"
          leftSection={<IconPlus size={16} aria-hidden />}
          onClick={onAgregar}
          disabled={lleno}
        >
          Agregar camino alterno
        </Button>
        {lleno && (
          <Text size="xs" c="dimmed">
            Una ruta admite máximo {MAX_ALTERNOS} caminos alternos.
          </Text>
        )}
        <Text size="sm" c="dimmed">
          Solo informativos para el pasajero.
        </Text>
      </Stack>
    </Paper>
  );
}
