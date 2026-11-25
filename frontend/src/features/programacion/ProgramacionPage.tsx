import { Group, Paper, Skeleton, Stack, Text, Title } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconCalendar } from '@tabler/icons-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import type { ProgramacionResumen, RutaProgramada } from '../../api/tipos';
import { BadgeEnRecorrido } from '../../components/BadgeEnRecorrido';
import { Chip } from '../../components/Chip';
import { confirmar } from '../../components/ConfirmarEliminar';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { ErrorPantalla } from '../../components/ErrorPantalla';
import { EstadoVacio } from '../../components/EstadoVacio';
import { MENSAJE_INESPERADO, mensajeDeError } from '../../utils/errores';
import { esFechaValida, hoyBogota } from '../../utils/fechas';
import { DetalleProgramacion } from './DetalleProgramacion';
import { useProgramaciones } from './hooks';
import clases from './ProgramacionPage.module.css';

function TarjetaConductor({
  conductor,
  seleccionada,
  onElegir,
}: {
  conductor: ProgramacionResumen;
  seleccionada: boolean;
  onElegir: () => void;
}) {
  const n = conductor.totalRutas;
  return (
    <button
      type="button"
      className={clases.tarjetaConductor}
      data-seleccionada={seleccionada || undefined}
      aria-pressed={seleccionada}
      onClick={onElegir}
    >
      <Text fw={600}>{conductor.nombre}</Text>
      <Text size="sm" c="dimmed">
        {n === 1 ? '1 ruta programada' : `${n} rutas programadas`}
      </Text>
      {(conductor.enRecorrido || !conductor.disponible) && (
        <Group gap={6} mt={6}>
          {conductor.enRecorrido && <BadgeEnRecorrido enRecorrido={conductor.enRecorrido} />}
          {!conductor.disponible && <Chip tono="aviso">No disponible</Chip>}
        </Group>
      )}
    </button>
  );
}

export function ProgramacionPage() {
  const [params, setParams] = useSearchParams();
  const hoy = hoyBogota();
  const paramFecha = params.get('fecha');
  const fecha = esFechaValida(paramFecha) ? paramFecha : hoy;
  const paramConductor = Number(params.get('conductor'));

  const consulta = useProgramaciones(fecha);
  const conductores = consulta.data ?? [];
  const seleccionado =
    conductores.find((c) => c.conductorId === paramConductor) ?? conductores[0] ?? null;

  // Cambios locales sin guardar, atados a su conductor y fecha: si la selección cambia
  // por otra vía (p. ej. el conductor desaparece de la lista), no se le pegan a otro.
  const claveDetalle = seleccionado ? `${seleccionado.conductorId}-${fecha}` : '';
  const [cambios, setCambios] = useState<{ clave: string; rutas: RutaProgramada[] } | null>(null);
  const borrador = cambios?.clave === claveDetalle ? cambios.rutas : null;
  const setBorrador = (rutas: RutaProgramada[] | null) =>
    setCambios(rutas ? { clave: claveDetalle, rutas } : null);

  const siNoHayCambios = (accion: () => void) => {
    if (!borrador) return accion();
    confirmar({
      titulo: '¿Descartar los cambios?',
      texto: 'La programación tiene cambios sin guardar. Si continúa, se perderán.',
      etiquetaConfirmar: 'Descartar cambios',
      onConfirmar: () => {
        setBorrador(null);
        accion();
      },
    });
  };

  const actualizarUrl = (cambios: Record<string, string>) =>
    setParams(
      (prev) => {
        const siguiente = new URLSearchParams(prev);
        for (const [k, v] of Object.entries(cambios)) siguiente.set(k, v);
        return siguiente;
      },
      { replace: true },
    );

  const elegirConductor = (id: number) => {
    if (id === seleccionado?.conductorId) return;
    siNoHayCambios(() => actualizarUrl({ fecha, conductor: String(id) }));
  };

  const cambiarFecha = (valor: string | null) => {
    if (!esFechaValida(valor) || valor === fecha) return;
    siNoHayCambios(() =>
      actualizarUrl({ fecha: valor, ...(seleccionado ? { conductor: String(seleccionado.conductorId) } : {}) }),
    );
  };

  let lista;
  if (consulta.isPending) {
    lista = (
      <Stack gap="xs" aria-label="Cargando conductores" role="status">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} h={64} radius="md" />
        ))}
      </Stack>
    );
  } else if (consulta.isError && !consulta.data) {
    lista = (
      <ErrorPantalla
        mensaje={mensajeDeError(consulta.error) ?? MENSAJE_INESPERADO}
        onReintentar={() => void consulta.refetch()}
      />
    );
  } else if (conductores.length === 0) {
    lista = <EstadoVacio titulo="No hay conductores registrados" />;
  } else {
    lista = (
      <Stack gap={6}>
        {conductores.map((c) => (
          <TarjetaConductor
            key={c.conductorId}
            conductor={c}
            seleccionada={c.conductorId === seleccionado?.conductorId}
            onElegir={() => elegirConductor(c.conductorId)}
          />
        ))}
      </Stack>
    );
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Programación del día"
        subtitulo="Rutas que cada conductor hará y en qué orden"
        acciones={
          <DatePickerInput
            label="Fecha"
            value={fecha}
            onChange={cambiarFecha}
            valueFormat="D [de] MMMM YYYY"
            leftSection={<IconCalendar size={16} aria-hidden />}
            w={220}
          />
        }
      />

      <div className={clases.disposicion}>
        <Paper withBorder p="md" className={clases.columnaConductores}>
          <Title order={2} fz="md" mb="sm">
            Conductores
          </Title>
          {lista}
        </Paper>

        <div className={clases.detalle}>
          {seleccionado && (
            <DetalleProgramacion
              // Otro conductor u otra fecha: estado del detalle desde cero.
              key={claveDetalle}
              conductor={seleccionado}
              fecha={fecha}
              hoy={hoy}
              borrador={borrador}
              onBorrador={setBorrador}
            />
          )}
        </div>
      </div>
    </>
  );
}
