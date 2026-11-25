-- UrbanMovil · Esquema inicial completo (fases 1, 2 y 3)
-- ON DELETE CASCADE implementa la decisión provisional D-17: al eliminar una ruta,
-- un conductor o un vehículo se borra también su historial asociado.

-- =====================================================================
-- Administrador
-- =====================================================================
CREATE TABLE administrador (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    correo          VARCHAR(120) NOT NULL,
    hash_contrasena VARCHAR(100) NOT NULL,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_administrador_correo ON administrador (lower(correo));

-- =====================================================================
-- Conductor
-- =====================================================================
CREATE TABLE conductor (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre               VARCHAR(120) NOT NULL,
    identificacion       VARCHAR(20)  NOT NULL,
    correo               VARCHAR(120) NOT NULL,
    telefono             VARCHAR(20)  NOT NULL,
    hash_contrasena      VARCHAR(100) NOT NULL,
    disponible           BOOLEAN      NOT NULL DEFAULT TRUE,
    motivo_no_disponible VARCHAR(20),
    detalle_motivo       VARCHAR(300),
    creado_en            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_conductor_motivo
        CHECK (motivo_no_disponible IN ('INCAPACIDAD', 'VACACIONES', 'PERMISO', 'OTRO')),
    CONSTRAINT ck_conductor_disponible_o_motivo
        CHECK (disponible OR motivo_no_disponible IS NOT NULL)
);

CREATE UNIQUE INDEX ux_conductor_identificacion ON conductor (lower(identificacion));
CREATE UNIQUE INDEX ux_conductor_correo ON conductor (lower(correo));

-- =====================================================================
-- Vehículo
-- =====================================================================
CREATE TABLE vehiculo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    placa               VARCHAR(10)  NOT NULL,
    numero_interno      VARCHAR(20)  NOT NULL,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    motivo_inactivacion VARCHAR(300),
    fecha_inactivacion  TIMESTAMPTZ,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_vehiculo_activo_o_motivo
        CHECK (activo OR motivo_inactivacion IS NOT NULL)
);

CREATE UNIQUE INDEX ux_vehiculo_placa ON vehiculo (lower(placa));
CREATE UNIQUE INDEX ux_vehiculo_numero_interno ON vehiculo (lower(numero_interno));

-- =====================================================================
-- Ruta y caminos alternos (Fase 2)
-- =====================================================================
CREATE TABLE ruta (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo              VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(120) NOT NULL,
    color               VARCHAR(20)  NOT NULL,
    recorrido_principal JSONB        NOT NULL DEFAULT '[]'::jsonb,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_ruta_codigo ON ruta (lower(codigo));
CREATE UNIQUE INDEX ux_ruta_nombre ON ruta (lower(nombre));

CREATE TABLE camino_alterno (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruta_id BIGINT       NOT NULL REFERENCES ruta (id) ON DELETE CASCADE,
    nombre  VARCHAR(120) NOT NULL,
    puntos  JSONB        NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX ix_camino_alterno_ruta ON camino_alterno (ruta_id);

-- =====================================================================
-- Programación del día (Fase 3)
-- =====================================================================
CREATE TABLE programacion (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conductor_id BIGINT NOT NULL REFERENCES conductor (id) ON DELETE CASCADE,
    fecha        DATE   NOT NULL,
    CONSTRAINT ux_programacion_conductor_fecha UNIQUE (conductor_id, fecha)
);

CREATE TABLE programacion_ruta (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programacion_id  BIGINT  NOT NULL REFERENCES programacion (id) ON DELETE CASCADE,
    ruta_id          BIGINT  NOT NULL REFERENCES ruta (id) ON DELETE CASCADE,
    orden            INTEGER NOT NULL,
    retiro_pendiente BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ux_programacion_ruta UNIQUE (programacion_id, ruta_id)
);

CREATE INDEX ix_programacion_ruta_ruta ON programacion_ruta (ruta_id);

-- =====================================================================
-- Reasignaciones (Fase 3)
-- =====================================================================
CREATE TABLE reasignacion (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conductor_id     BIGINT       NOT NULL REFERENCES conductor (id) ON DELETE CASCADE,
    administrador_id BIGINT       NOT NULL REFERENCES administrador (id),
    fecha_hora       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    motivo           VARCHAR(300) NOT NULL
);

CREATE INDEX ix_reasignacion_conductor ON reasignacion (conductor_id);

CREATE TABLE reasignacion_ruta (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reasignacion_id BIGINT      NOT NULL REFERENCES reasignacion (id) ON DELETE CASCADE,
    ruta_id         BIGINT      NOT NULL REFERENCES ruta (id) ON DELETE CASCADE,
    tipo            VARCHAR(20) NOT NULL,
    CONSTRAINT ck_reasignacion_ruta_tipo CHECK (tipo IN ('ASIGNADA', 'QUITADA'))
);

-- =====================================================================
-- Recorridos (se consultan desde Fase 1; se crean en Fase 3)
-- =====================================================================
CREATE TABLE recorrido (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conductor_id BIGINT      NOT NULL REFERENCES conductor (id) ON DELETE CASCADE,
    ruta_id      BIGINT      NOT NULL REFERENCES ruta (id) ON DELETE CASCADE,
    vehiculo_id  BIGINT      NOT NULL REFERENCES vehiculo (id) ON DELETE CASCADE,
    estado       VARCHAR(20) NOT NULL,
    inicio       TIMESTAMPTZ NOT NULL DEFAULT now(),
    fin          TIMESTAMPTZ,
    CONSTRAINT ck_recorrido_estado CHECK (estado IN ('ACTIVO', 'FINALIZADO'))
);

-- Un solo recorrido ACTIVO por conductor y uno solo por vehículo
CREATE UNIQUE INDEX ux_recorrido_activo_conductor ON recorrido (conductor_id) WHERE estado = 'ACTIVO';
CREATE UNIQUE INDEX ux_recorrido_activo_vehiculo ON recorrido (vehiculo_id) WHERE estado = 'ACTIVO';
CREATE INDEX ix_recorrido_ruta ON recorrido (ruta_id);
