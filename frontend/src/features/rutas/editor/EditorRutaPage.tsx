import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, ColorInput, Group, Paper, Stack, TextInput, Title } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useCallback, useMemo, useReducer, useState, type FormEvent } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router';
import { ApiError } from '../../../api/client';
import type { Punto, RutaDetalle } from '../../../api/tipos';
import { colores } from '../../../app/theme';
import { CargadorPantalla } from '../../../components/CargadorPantalla';
import { confirmar } from '../../../components/ConfirmarEliminar';
import { EncabezadoPagina } from '../../../components/EncabezadoPagina';
import { ErrorPantalla } from '../../../components/ErrorPantalla';
import { MapaBase } from '../../../mapa/MapaBase';
import type { LineaMapa, MarcadorMapa } from '../../../mapa/tipos';
import { MENSAJE_INESPERADO, manejarErrorFormulario, mensajeDeError, notificarExito } from '../../../utils/errores';
import { COLOR_ALTERNO, esColorHex, PALETA_RUTAS } from '../constantes';
import { marcadoresInicioFinal } from '../dibujoRuta';
import { useGuardarRuta, useRuta } from '../hooks';
import type { EstadoNavegacionRutas } from '../RutasPage';
import { BarraTrazado } from './BarraTrazado';
import clases from './EditorRuta.module.css';
import { TarjetaAlternos, TarjetaPrincipal } from './TarjetasTrazado';
import { estadoInicial, PRINCIPAL, puntosDe, reducirTrazado, type Objetivo, type TrazadoEditable } from './trazado';
import {
  CAMPOS_DATOS,
  construirCuerpo,
  erroresTrazadoDeApi,
  esquemaDatos,
  SIN_ERRORES,
  validarTrazado,
  type EntradaDatos,
  type ErroresTrazado,
  type SalidaDatos,
} from './validacion';

const AYUDA =
  'Haga clic en el mapa para agregar puntos en el sentido de circulación. ' +
  'El primer punto es el inicio y el último el final. Arrastre un punto para moverlo.';

/** `/rutas/nueva` y `/rutas/:id`. Carga la ruta (si aplica) y monta el editor con una `key` propia. */
export function EditorRutaPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const esNueva = id === undefined;
  const rutaId = esNueva ? null : Number(id);
  const idValido = rutaId !== null && Number.isInteger(rutaId) && rutaId > 0;
  const consulta = useRuta(idValido ? rutaId : null, { siempreFresca: true });

  if (esNueva) return <EditorRuta key="nueva" ruta={null} />;

  const volver = (
    <Group justify="center">
      <Button variant="default" onClick={() => navigate('/rutas')}>
        Volver a rutas
      </Button>
    </Group>
  );

  if (!idValido) {
    return (
      <>
        <ErrorPantalla mensaje="La ruta no existe" />
        {volver}
      </>
    );
  }

  // El último resultado fue un error (p. ej. 404): no se edita una copia vieja de la caché.
  if (consulta.isError && consulta.errorUpdatedAt >= consulta.dataUpdatedAt) {
    const es404 = consulta.error instanceof ApiError && consulta.error.status === 404;
    return (
      <>
        <ErrorPantalla
          mensaje={mensajeDeError(consulta.error) ?? MENSAJE_INESPERADO}
          onReintentar={es404 ? undefined : () => void consulta.refetch()}
        />
        {volver}
      </>
    );
  }

  if (!consulta.data || !consulta.isFetchedAfterMount) return <CargadorPantalla />;

  return <EditorRuta key={rutaId} ruta={consulta.data} />;
}

// ---------------------------------------------------------------------------

function EditorRuta({ ruta }: { ruta: RutaDetalle | null }) {
  const navigate = useNavigate();
  const esEdicion = ruta !== null;
  const guardarRuta = useGuardarRuta();

  const [trazado, despachar] = useReducer(reducirTrazado, ruta, estadoInicial);
  const [errores, setErrores] = useState<ErroresTrazado>(SIN_ERRORES);
  // Solo al cargar: no se re-encuadra con cada clic.
  const [encuadre] = useState<Punto[] | undefined>(() =>
    ruta && ruta.recorridoPrincipal.length > 0 ? ruta.recorridoPrincipal : undefined,
  );

  const {
    register,
    control,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<EntradaDatos, unknown, SalidaDatos>({
    resolver: zodResolver(esquemaDatos),
    defaultValues: { nombre: ruta?.nombre ?? '', codigo: ruta?.codigo ?? '', color: ruta?.color ?? '' },
  });

  const colorElegido = useWatch({ control, name: 'color' });
  // Mientras no haya color válido: el actual (edición) o el primario (lo asignará el backend).
  const colorRuta = esColorHex(colorElegido) ? colorElegido : (ruta?.color ?? colores.primario);

  const { activo, principal, alternos } = trazado;
  const principalActivo = activo === PRINCIPAL;
  const puntosActivos = puntosDe(trazado, activo);
  const nombreActivo = principalActivo
    ? 'Recorrido principal'
    : (alternos.find((a) => a.clave === activo)?.nombre.trim() || 'el camino alterno');

  // ---------- Errores ----------
  const limpiarErroresDe = useCallback((objetivo: Objetivo) => {
    setErrores((e) => {
      if (objetivo === PRINCIPAL) return e.principal ? { ...e, principal: undefined } : e;
      if (!e.alternos[objetivo] && !e.listaAlternos) return e;
      const resto = { ...e.alternos };
      delete resto[objetivo];
      return { ...e, alternos: resto, listaAlternos: undefined };
    });
  }, []);

  // ---------- Mapa ----------
  const lineas = useMemo<LineaMapa[]>(() => {
    const inactivos = alternos
      .filter((a) => a.clave !== activo)
      .map((a) => ({ id: a.clave, puntos: a.puntos, color: COLOR_ALTERNO, ancho: 5, punteada: true, opacidad: 0.5 }));
    const lineaPrincipal: LineaMapa = {
      id: PRINCIPAL,
      puntos: principal,
      color: colorRuta,
      opacidad: principalActivo ? 1 : 0.5,
    };
    const alternoActivo = alternos.find((a) => a.clave === activo);
    return [
      ...inactivos,
      lineaPrincipal,
      ...(alternoActivo
        ? [{ id: alternoActivo.clave, puntos: alternoActivo.puntos, color: COLOR_ALTERNO, punteada: true }]
        : []),
    ];
  }, [alternos, activo, principal, principalActivo, colorRuta]);

  const marcadores = useMemo<MarcadorMapa[]>(() => {
    const colorActivo = principalActivo ? colorRuta : COLOR_ALTERNO;
    const ultimo = puntosActivos.length - 1;
    const vertices: MarcadorMapa[] = [];
    puntosActivos.forEach((p, i) => {
      // En el principal, el primer y el último punto los representan los pines I y F (también arrastrables).
      if (principalActivo && (i === 0 || (i === ultimo && ultimo >= 1))) return;
      vertices.push({
        id: `v-${i}`,
        punto: p,
        tipo: 'vertice',
        color: colorActivo,
        etiqueta: `Punto ${i + 1} de ${nombreActivo}`,
        arrastrable: true,
      });
    });
    const pines = marcadoresInicioFinal(principal).map((m) => {
      if (!principalActivo) return m;
      const indice = m.tipo === 'inicio' ? 0 : principal.length - 1;
      return { ...m, id: `v-${indice}`, arrastrable: true };
    });
    return [...vertices, ...pines];
  }, [puntosActivos, principal, principalActivo, colorRuta, nombreActivo]);

  const alClicMapa = useCallback(
    (punto: Punto) => {
      despachar({ tipo: 'agregarPunto', punto });
      limpiarErroresDe(activo);
    },
    [activo, limpiarErroresDe],
  );

  const alMoverMarcador = useCallback((id: string, punto: Punto) => {
    if (!id.startsWith('v-')) return;
    despachar({ tipo: 'moverPunto', indice: Number(id.slice(2)), punto });
  }, []);

  // ---------- Acciones del trazado ----------
  const deshacer = () => despachar({ tipo: 'deshacer' });

  const borrar = () => {
    const ejecutar = () => despachar({ tipo: 'borrar' });
    if (puntosActivos.length <= 2) return ejecutar();
    confirmar({
      titulo: '¿Borrar el trazado?',
      texto: `Se quitarán los ${puntosActivos.length} puntos de ${nombreActivo}.`,
      etiquetaConfirmar: 'Borrar trazado',
      onConfirmar: ejecutar,
    });
  };

  const eliminarAlterno = (a: TrazadoEditable) => {
    const ejecutar = () => {
      despachar({ tipo: 'eliminarAlterno', clave: a.clave });
      limpiarErroresDe(a.clave);
    };
    if (a.puntos.length <= 2) return ejecutar();
    confirmar({
      titulo: `¿Eliminar el camino alterno «${a.nombre.trim() || 'sin nombre'}»?`,
      texto: `Se quitarán sus ${a.puntos.length} puntos. El cambio se aplica al guardar la ruta.`,
      etiquetaConfirmar: 'Eliminar camino',
      onConfirmar: ejecutar,
    });
  };

  // ---------- Guardar / cancelar ----------
  const guardar = async (e: FormEvent) => {
    e.preventDefault();
    clearErrors('root');
    const erroresCliente = validarTrazado(trazado);
    setErrores(erroresCliente ?? SIN_ERRORES);

    await handleSubmit(async (datos) => {
      if (erroresCliente) return; // sin llamar a la API
      // Índice del backend (`alternos[i]`) → clave local, según el orden enviado.
      const claves = trazado.alternos.map((a) => a.clave);
      try {
        const guardada = await guardarRuta.mutateAsync({ id: ruta?.id ?? null, datos: construirCuerpo(datos, trazado) });
        notificarExito(esEdicion ? 'Cambios guardados' : 'Ruta creada', `${guardada.codigo} · ${guardada.nombre}`);
        const estado: EstadoNavegacionRutas = { seleccionada: guardada.id };
        navigate('/rutas', { state: estado });
      } catch (error) {
        // Datos bajo cada input; lo demás (trazados) va a la alerta general…
        manejarErrorFormulario(error, setError, CAMPOS_DATOS);
        // …y además en la tarjeta del trazado correspondiente.
        const deApi = erroresTrazadoDeApi(error, claves);
        if (deApi) setErrores(deApi);
      }
    })();
  };

  const cancelar = () => {
    if (!isDirty && !trazado.modificado) return navigate('/rutas');
    confirmar({
      titulo: '¿Descartar los cambios?',
      texto: 'Los cambios sin guardar de esta ruta se perderán.',
      etiquetaConfirmar: 'Descartar cambios',
      onConfirmar: () => navigate('/rutas'),
    });
  };

  const alertaGeneral = errors.root?.servidor?.message ?? errores.resumen;

  return (
    <>
      <EncabezadoPagina
        titulo={esEdicion ? `Editar ruta ${ruta.codigo}` : 'Nueva ruta'}
        subtitulo="Trace el recorrido principal y los caminos alternos"
      />

      <form onSubmit={guardar} noValidate className={clases.disposicion}>
        <div className={clases.columnaMapa}>
          <MapaBase
            alto="100%"
            lineas={lineas}
            marcadores={marcadores}
            encuadrar={encuadre}
            onClickMapa={alClicMapa}
            onMoverMarcador={alMoverMarcador}
            ariaLabel={`Mapa para trazar la ruta. Trazando: ${nombreActivo}`}
          />
          <div className={clases.barra}>
            <BarraTrazado
              activo={activo}
              alternos={alternos}
              puntosActivos={puntosActivos.length}
              onActivar={(objetivo) => despachar({ tipo: 'activar', objetivo })}
              onDeshacer={deshacer}
              onBorrar={borrar}
            />
          </div>
          <div className={clases.ayuda}>{AYUDA}</div>
        </div>

        <Stack className={clases.panel} gap="md">
          <Paper withBorder p="md">
            <Stack gap="sm">
              <Title order={2} fz="md">
                Datos
              </Title>
              <TextInput
                label="Nombre de la ruta"
                withAsterisk
                description="Debe ser único"
                error={errors.nombre?.message}
                {...register('nombre')}
              />
              <TextInput
                label="Código"
                withAsterisk
                description="Por ejemplo R1"
                styles={{ input: { textTransform: 'uppercase' } }}
                error={errors.codigo?.message}
                {...register('codigo')}
              />
              <Controller
                control={control}
                name="color"
                render={({ field }) => (
                  <ColorInput
                    label="Color"
                    format="hex"
                    swatches={PALETA_RUTAS}
                    swatchesPerRow={6}
                    placeholder={esEdicion ? 'Se conserva el actual' : 'Automático'}
                    description={esEdicion ? undefined : 'Si lo deja vacío, se asigna uno automáticamente'}
                    value={field.value}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    ref={field.ref}
                    error={errors.color?.message}
                  />
                )}
              />
            </Stack>
          </Paper>

          <TarjetaPrincipal
            puntos={principal.length}
            activo={principalActivo}
            error={errores.principal}
            onTrazar={() => despachar({ tipo: 'activar', objetivo: PRINCIPAL })}
          />

          <TarjetaAlternos
            alternos={alternos}
            activo={activo}
            errores={errores}
            onTrazar={(clave) => despachar({ tipo: 'activar', objetivo: clave })}
            onRenombrar={(clave, nombre) => {
              despachar({ tipo: 'renombrarAlterno', clave, nombre });
              limpiarErroresDe(clave);
            }}
            onEliminar={eliminarAlterno}
            onAgregar={() => despachar({ tipo: 'agregarAlterno' })}
          />

          {alertaGeneral && (
            <Alert color="red" variant="light" icon={<IconAlertCircle size={18} />} role="alert">
              {alertaGeneral}
            </Alert>
          )}

          <Group justify="flex-end">
            <Button variant="default" onClick={cancelar} disabled={isSubmitting}>
              Cancelar
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Guardar ruta
            </Button>
          </Group>
        </Stack>
      </form>
    </>
  );
}
