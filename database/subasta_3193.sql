/* =====================================================================
   Plataforma de Subastas de Vehículos (Caso Copart) — 2do. Parcial Web
   Axel Herrera — Carné 1890-23-3193
   BD compartida: db_WebDevUMG (SQL Server)

   Convenciones:
   - Todas las tablas, vistas, SP y CONSTRAINTS llevan sufijo _3193
     (los nombres de constraints son globales en el esquema; así no
      chocan con los de tus compañeros).
   - Todas las fechas se guardan en UTC (DATETIME2). El frontend las
     muestra en hora de Guatemala.
   - El script es re-ejecutable (no falla si los objetos ya existen).
   ===================================================================== */
SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   (OPCIONAL) Reinicio total — descomentar solo si quieres borrar todo
   ---------------------------------------------------------------------
DROP VIEW  IF EXISTS dbo.vw_Inventario_3193;
DROP PROCEDURE IF EXISTS dbo.sp_RegistrarPuja_3193;
DROP PROCEDURE IF EXISTS dbo.sp_CerrarSubastasVencidas_3193;
DROP TABLE IF EXISTS dbo.Pujas_3193;
DROP TABLE IF EXISTS dbo.FotosVehiculo_3193;
DROP TABLE IF EXISTS dbo.Vehiculos_3193;
DROP TABLE IF EXISTS dbo.Cat_Modelo_3193;
DROP TABLE IF EXISTS dbo.Cat_Marca_3193;
DROP TABLE IF EXISTS dbo.Cat_TipoArticulo_3193;
DROP TABLE IF EXISTS dbo.Cat_Transmision_3193;
DROP TABLE IF EXISTS dbo.Cat_Combustible_3193;
DROP TABLE IF EXISTS dbo.Cat_TrenManejo_3193;
DROP TABLE IF EXISTS dbo.Cat_NivelDanio_3193;
DROP TABLE IF EXISTS dbo.Usuarios_3193;
GO
--------------------------------------------------------------------- */

/* =====================================================================
   1. USUARIOS
   ===================================================================== */
IF OBJECT_ID('dbo.Usuarios_3193', 'U') IS NULL
CREATE TABLE dbo.Usuarios_3193 (
    Id             INT IDENTITY(1,1) CONSTRAINT PK_Usuarios_3193 PRIMARY KEY,
    Nombre         NVARCHAR(80)  NOT NULL,
    Apellido       NVARCHAR(80)  NOT NULL,
    Correo         NVARCHAR(150) NOT NULL CONSTRAINT UQ_Usuarios_3193_Correo UNIQUE,
    Telefono       VARCHAR(20)   NOT NULL,
    PasswordHash   VARCHAR(100)  NOT NULL,           -- BCrypt (lo genera Spring)
    Activo         BIT           NOT NULL CONSTRAINT DF_Usuarios_3193_Activo DEFAULT 1,
    FechaRegistro  DATETIME2(0)  NOT NULL CONSTRAINT DF_Usuarios_3193_Fecha  DEFAULT SYSUTCDATETIME()
);
GO

/* =====================================================================
   2. CATÁLOGOS
   ===================================================================== */
IF OBJECT_ID('dbo.Cat_TipoArticulo_3193', 'U') IS NULL
CREATE TABLE dbo.Cat_TipoArticulo_3193 (
    Id     INT IDENTITY(1,1) CONSTRAINT PK_Cat_TipoArticulo_3193 PRIMARY KEY,
    Nombre NVARCHAR(60) NOT NULL CONSTRAINT UQ_Cat_TipoArticulo_3193 UNIQUE,
    Activo BIT NOT NULL CONSTRAINT DF_Cat_TipoArticulo_3193_Activo DEFAULT 1
);

IF OBJECT_ID('dbo.Cat_Marca_3193', 'U') IS NULL
CREATE TABLE dbo.Cat_Marca_3193 (
    Id     INT IDENTITY(1,1) CONSTRAINT PK_Cat_Marca_3193 PRIMARY KEY,
    Nombre NVARCHAR(60) NOT NULL CONSTRAINT UQ_Cat_Marca_3193 UNIQUE,
    Activo BIT NOT NULL CONSTRAINT DF_Cat_Marca_3193_Activo DEFAULT 1
);

IF OBJECT_ID('dbo.Cat_Modelo_3193', 'U') IS NULL
CREATE TABLE dbo.Cat_Modelo_3193 (
    Id      INT IDENTITY(1,1) CONSTRAINT PK_Cat_Modelo_3193 PRIMARY KEY,
    MarcaId INT NOT NULL CONSTRAINT FK_Cat_Modelo_3193_Marca REFERENCES dbo.Cat_Marca_3193(Id),
    Nombre  NVARCHAR(60) NOT NULL,
    Activo  BIT NOT NULL CONSTRAINT DF_Cat_Modelo_3193_Activo DEFAULT 1,
    CONSTRAINT UQ_Cat_Modelo_3193 UNIQUE (MarcaId, Nombre)
);

IF OBJECT_ID('dbo.Cat_Transmision_3193', 'U') IS NULL
CREATE TABLE dbo.Cat_Transmision_3193 (
    Id     INT IDENTITY(1,1) CONSTRAINT PK_Cat_Transmision_3193 PRIMARY KEY,
    Nombre NVARCHAR(60) NOT NULL CONSTRAINT UQ_Cat_Transmision_3193 UNIQUE,
    Activo BIT NOT NULL CONSTRAINT DF_Cat_Transmision_3193_Activo DEFAULT 1
);

IF OBJECT_ID('dbo.Cat_Combustible_3193', 'U') IS NULL
CREATE TABLE dbo.Cat_Combustible_3193 (
    Id     INT IDENTITY(1,1) CONSTRAINT PK_Cat_Combustible_3193 PRIMARY KEY,
    Nombre NVARCHAR(60) NOT NULL CONSTRAINT UQ_Cat_Combustible_3193 UNIQUE,
    Activo BIT NOT NULL CONSTRAINT DF_Cat_Combustible_3193_Activo DEFAULT 1
);

IF OBJECT_ID('dbo.Cat_TrenManejo_3193', 'U') IS NULL
CREATE TABLE dbo.Cat_TrenManejo_3193 (
    Id     INT IDENTITY(1,1) CONSTRAINT PK_Cat_TrenManejo_3193 PRIMARY KEY,
    Codigo VARCHAR(5)   NOT NULL CONSTRAINT UQ_Cat_TrenManejo_3193 UNIQUE,  -- AWD, FWD, RWD, 4WD
    Nombre NVARCHAR(60) NOT NULL,
    Activo BIT NOT NULL CONSTRAINT DF_Cat_TrenManejo_3193_Activo DEFAULT 1
);

IF OBJECT_ID('dbo.Cat_NivelDanio_3193', 'U') IS NULL
CREATE TABLE dbo.Cat_NivelDanio_3193 (
    Id          INT IDENTITY(1,1) CONSTRAINT PK_Cat_NivelDanio_3193 PRIMARY KEY,
    Codigo      VARCHAR(10)   NOT NULL CONSTRAINT UQ_Cat_NivelDanio_3193 UNIQUE, -- VERDE, AMARILLO, ROJO
    Nombre      NVARCHAR(60)  NOT NULL,
    Descripcion NVARCHAR(120) NOT NULL,
    ColorHex    CHAR(7)       NOT NULL,
    Orden       TINYINT       NOT NULL
);
GO

/* =====================================================================
   3. VEHÍCULOS (publicación + parámetros de subasta)
   ===================================================================== */
IF OBJECT_ID('dbo.Vehiculos_3193', 'U') IS NULL
CREATE TABLE dbo.Vehiculos_3193 (
    Id                INT IDENTITY(1,1) CONSTRAINT PK_Vehiculos_3193 PRIMARY KEY,
    UsuarioId         INT NOT NULL CONSTRAINT FK_Vehiculos_3193_Usuario     REFERENCES dbo.Usuarios_3193(Id),

    -- Ficha técnica
    Anio              SMALLINT NOT NULL CONSTRAINT CK_Vehiculos_3193_Anio CHECK (Anio BETWEEN 1900 AND 2100),
    TipoArticuloId    INT NOT NULL CONSTRAINT FK_Vehiculos_3193_TipoArt     REFERENCES dbo.Cat_TipoArticulo_3193(Id),
    MarcaId           INT NOT NULL CONSTRAINT FK_Vehiculos_3193_Marca       REFERENCES dbo.Cat_Marca_3193(Id),
    ModeloId          INT NOT NULL CONSTRAINT FK_Vehiculos_3193_Modelo      REFERENCES dbo.Cat_Modelo_3193(Id),
    Motor             NVARCHAR(60) NOT NULL,                     -- ej. "2.5L I4", "3.5L V6 Turbo"
    TransmisionId     INT NOT NULL CONSTRAINT FK_Vehiculos_3193_Transm      REFERENCES dbo.Cat_Transmision_3193(Id),
    CombustibleId     INT NOT NULL CONSTRAINT FK_Vehiculos_3193_Combust     REFERENCES dbo.Cat_Combustible_3193(Id),
    TrenManejoId      INT NOT NULL CONSTRAINT FK_Vehiculos_3193_Tren        REFERENCES dbo.Cat_TrenManejo_3193(Id),
    Cilindros         TINYINT NOT NULL CONSTRAINT CK_Vehiculos_3193_Cil CHECK (Cilindros BETWEEN 0 AND 16), -- 0 = eléctrico
    NivelDanioId      INT NOT NULL CONSTRAINT FK_Vehiculos_3193_Danio       REFERENCES dbo.Cat_NivelDanio_3193(Id),
    Descripcion       NVARCHAR(1000) NULL,

    -- Parámetros de subasta
    MontoBase         DECIMAL(12,2) NOT NULL CONSTRAINT CK_Vehiculos_3193_Base CHECK (MontoBase > 0),
    FechaInicio       DATETIME2(0)  NOT NULL,
    FechaCierre       DATETIME2(0)  NOT NULL,

    -- Estado desnormalizado de la subasta (se actualiza en sp_RegistrarPuja_3193)
    OfertaActual      DECIMAL(12,2) NULL,
    LiderUsuarioId    INT NULL CONSTRAINT FK_Vehiculos_3193_Lider REFERENCES dbo.Usuarios_3193(Id),
    TotalPujas        INT NOT NULL CONSTRAINT DF_Vehiculos_3193_TotalPujas DEFAULT 0,
    Estado            VARCHAR(10) NOT NULL CONSTRAINT DF_Vehiculos_3193_Estado DEFAULT 'ABIERTA',

    FechaCreacion     DATETIME2(0) NOT NULL CONSTRAINT DF_Vehiculos_3193_FCrea DEFAULT SYSUTCDATETIME(),
    FechaModificacion DATETIME2(0) NULL,

    CONSTRAINT CK_Vehiculos_3193_Fechas CHECK (FechaCierre > FechaInicio),
    CONSTRAINT CK_Vehiculos_3193_Estado CHECK (Estado IN ('ABIERTA', 'VENDIDA', 'DESIERTA'))
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Vehiculos_3193_Usuario')
    CREATE INDEX IX_Vehiculos_3193_Usuario ON dbo.Vehiculos_3193 (UsuarioId);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Vehiculos_3193_Cierre')
    CREATE INDEX IX_Vehiculos_3193_Cierre  ON dbo.Vehiculos_3193 (Estado, FechaCierre);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Vehiculos_3193_Filtros')
    CREATE INDEX IX_Vehiculos_3193_Filtros ON dbo.Vehiculos_3193 (MarcaId, ModeloId, NivelDanioId, Anio);
GO

/* =====================================================================
   4. GALERÍA DE FOTOS (mínimo 5 por vehículo — validado en el backend)
   ===================================================================== */
IF OBJECT_ID('dbo.FotosVehiculo_3193', 'U') IS NULL
CREATE TABLE dbo.FotosVehiculo_3193 (
    Id         INT IDENTITY(1,1) CONSTRAINT PK_FotosVehiculo_3193 PRIMARY KEY,
    VehiculoId INT NOT NULL CONSTRAINT FK_FotosVehiculo_3193_Vehiculo
               REFERENCES dbo.Vehiculos_3193(Id) ON DELETE CASCADE,
    Url        NVARCHAR(500) NOT NULL,
    PublicId   NVARCHAR(200) NULL,      -- id en el servicio de imágenes (para borrar)
    Orden      TINYINT NOT NULL,
    CONSTRAINT UQ_FotosVehiculo_3193_Orden UNIQUE (VehiculoId, Orden)
);
GO

/* =====================================================================
   5. PUJAS (historial / trazabilidad — la identidad NUNCA se expone)
   ===================================================================== */
IF OBJECT_ID('dbo.Pujas_3193', 'U') IS NULL
CREATE TABLE dbo.Pujas_3193 (
    Id         BIGINT IDENTITY(1,1) CONSTRAINT PK_Pujas_3193 PRIMARY KEY,
    VehiculoId INT NOT NULL CONSTRAINT FK_Pujas_3193_Vehiculo REFERENCES dbo.Vehiculos_3193(Id),
    UsuarioId  INT NOT NULL CONSTRAINT FK_Pujas_3193_Usuario  REFERENCES dbo.Usuarios_3193(Id),
    Monto      DECIMAL(12,2) NOT NULL CONSTRAINT CK_Pujas_3193_Monto CHECK (Monto > 0),
    FechaPuja  DATETIME2(3)  NOT NULL CONSTRAINT DF_Pujas_3193_Fecha DEFAULT SYSUTCDATETIME()
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Pujas_3193_Vehiculo')
    CREATE INDEX IX_Pujas_3193_Vehiculo ON dbo.Pujas_3193 (VehiculoId, Monto DESC);
GO

/* =====================================================================
   6. DATOS DE CATÁLOGOS (idempotente)
   ===================================================================== */
INSERT INTO dbo.Cat_TipoArticulo_3193 (Nombre)
SELECT v.Nombre FROM (VALUES
    (N'Automóvil'), (N'SUV'), (N'Pickup'), (N'Van / Microbús'), (N'Motocicleta'), (N'Camión')
) v(Nombre)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Cat_TipoArticulo_3193 c WHERE c.Nombre = v.Nombre);

INSERT INTO dbo.Cat_Transmision_3193 (Nombre)
SELECT v.Nombre FROM (VALUES
    (N'Automática'), (N'Manual'), (N'CVT'), (N'Doble embrague (DCT)')
) v(Nombre)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Cat_Transmision_3193 c WHERE c.Nombre = v.Nombre);

INSERT INTO dbo.Cat_Combustible_3193 (Nombre)
SELECT v.Nombre FROM (VALUES
    (N'Gasolina'), (N'Diésel'), (N'Híbrido'), (N'Híbrido enchufable'), (N'Eléctrico'), (N'Gas (GLP)')
) v(Nombre)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Cat_Combustible_3193 c WHERE c.Nombre = v.Nombre);

INSERT INTO dbo.Cat_TrenManejo_3193 (Codigo, Nombre)
SELECT v.Codigo, v.Nombre FROM (VALUES
    ('AWD', N'Tracción integral (AWD)'),
    ('FWD', N'Tracción delantera (FWD)'),
    ('RWD', N'Tracción trasera (RWD)'),
    ('4WD', N'Doble tracción (4WD)')
) v(Codigo, Nombre)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Cat_TrenManejo_3193 c WHERE c.Codigo = v.Codigo);

INSERT INTO dbo.Cat_NivelDanio_3193 (Codigo, Nombre, Descripcion, ColorHex, Orden)
SELECT v.Codigo, v.Nombre, v.Descripcion, v.ColorHex, v.Orden FROM (VALUES
    ('VERDE',    N'Verde',    N'Daño menor / Limpio',       '#5E9E5A', 1),
    ('AMARILLO', N'Amarillo', N'Daño medio / Reparable',    '#E2B44B', 2),
    ('ROJO',     N'Rojo',     N'Daño severo / Salvamento',  '#C8584E', 3)
) v(Codigo, Nombre, Descripcion, ColorHex, Orden)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Cat_NivelDanio_3193 c WHERE c.Codigo = v.Codigo);

INSERT INTO dbo.Cat_Marca_3193 (Nombre)
SELECT v.Nombre FROM (VALUES
    (N'Toyota'), (N'Honda'), (N'Nissan'), (N'Ford'), (N'Chevrolet'), (N'Hyundai'),
    (N'Kia'), (N'Mazda'), (N'Jeep'), (N'Tesla'), (N'Volkswagen'), (N'BMW')
) v(Nombre)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Cat_Marca_3193 c WHERE c.Nombre = v.Nombre);

INSERT INTO dbo.Cat_Modelo_3193 (MarcaId, Nombre)
SELECT m.Id, v.Modelo
FROM (VALUES
    (N'Toyota', N'Corolla'), (N'Toyota', N'Camry'), (N'Toyota', N'RAV4'), (N'Toyota', N'Tacoma'), (N'Toyota', N'Hilux'),
    (N'Honda', N'Civic'), (N'Honda', N'Accord'), (N'Honda', N'CR-V'), (N'Honda', N'Pilot'),
    (N'Nissan', N'Sentra'), (N'Nissan', N'Altima'), (N'Nissan', N'Rogue'), (N'Nissan', N'Frontier'),
    (N'Ford', N'F-150'), (N'Ford', N'Explorer'), (N'Ford', N'Escape'), (N'Ford', N'Mustang'), (N'Ford', N'Ranger'),
    (N'Chevrolet', N'Silverado'), (N'Chevrolet', N'Malibu'), (N'Chevrolet', N'Equinox'), (N'Chevrolet', N'Tahoe'),
    (N'Hyundai', N'Elantra'), (N'Hyundai', N'Tucson'), (N'Hyundai', N'Santa Fe'),
    (N'Kia', N'Rio'), (N'Kia', N'Sportage'), (N'Kia', N'Sorento'), (N'Kia', N'Soul'),
    (N'Mazda', N'Mazda3'), (N'Mazda', N'CX-5'), (N'Mazda', N'CX-30'),
    (N'Jeep', N'Wrangler'), (N'Jeep', N'Grand Cherokee'), (N'Jeep', N'Compass'),
    (N'Tesla', N'Model 3'), (N'Tesla', N'Model Y'),
    (N'Volkswagen', N'Jetta'), (N'Volkswagen', N'Tiguan'),
    (N'BMW', N'Serie 3'), (N'BMW', N'X5')
) v(Marca, Modelo)
JOIN dbo.Cat_Marca_3193 m ON m.Nombre = v.Marca
WHERE NOT EXISTS (SELECT 1 FROM dbo.Cat_Modelo_3193 c WHERE c.MarcaId = m.Id AND c.Nombre = v.Modelo);
GO

/* =====================================================================
   7. VISTA DE INVENTARIO (para Home + filtros; NO expone al líder)
   ===================================================================== */
CREATE OR ALTER VIEW dbo.vw_Inventario_3193
AS
SELECT
    v.Id, v.UsuarioId, v.Anio,
    v.TipoArticuloId, ta.Nombre AS TipoArticulo,
    v.MarcaId,        ma.Nombre AS Marca,
    v.ModeloId,       mo.Nombre AS Modelo,
    v.Motor,
    v.TransmisionId,  tr.Nombre AS Transmision,
    v.CombustibleId,  co.Nombre AS Combustible,
    v.TrenManejoId,   tm.Codigo AS TrenManejo,
    v.Cilindros,
    v.NivelDanioId,   nd.Codigo AS NivelDanio, nd.Descripcion AS NivelDanioDesc, nd.ColorHex AS NivelDanioColor,
    v.Descripcion,
    v.MontoBase, v.OfertaActual, v.TotalPujas,
    v.FechaInicio, v.FechaCierre, v.Estado,
    (SELECT TOP 1 f.Url FROM dbo.FotosVehiculo_3193 f
      WHERE f.VehiculoId = v.Id ORDER BY f.Orden) AS FotoPortada
FROM dbo.Vehiculos_3193 v
JOIN dbo.Cat_TipoArticulo_3193 ta ON ta.Id = v.TipoArticuloId
JOIN dbo.Cat_Marca_3193        ma ON ma.Id = v.MarcaId
JOIN dbo.Cat_Modelo_3193       mo ON mo.Id = v.ModeloId
JOIN dbo.Cat_Transmision_3193  tr ON tr.Id = v.TransmisionId
JOIN dbo.Cat_Combustible_3193  co ON co.Id = v.CombustibleId
JOIN dbo.Cat_TrenManejo_3193   tm ON tm.Id = v.TrenManejoId
JOIN dbo.Cat_NivelDanio_3193   nd ON nd.Id = v.NivelDanioId;
GO

/* =====================================================================
   8. SP: REGISTRAR PUJA — validación atómica en servidor
      Reglas:
        - La subasta debe existir, estar ABIERTA y dentro de [inicio, cierre)
        - El dueño no puede pujar por su propio vehículo
        - El líder actual no puede pujarse a sí mismo
        - 1ra puja  >= MontoBase
        - Siguientes >= OfertaActual * 1.10  (incremento mínimo 10%)
      UPDLOCK+HOLDLOCK serializa pujas simultáneas sobre el mismo vehículo.
      Devuelve: Resultado, MontoMinimo (siguiente puja válida), AnteriorLiderId
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_RegistrarPuja_3193
    @VehiculoId INT,
    @UsuarioId  INT,
    @Monto      DECIMAL(12,2)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @ahora  DATETIME2(3) = SYSUTCDATETIME();
    DECLARE @base DECIMAL(12,2), @actual DECIMAL(12,2), @minimo DECIMAL(12,2),
            @ini DATETIME2(0), @fin DATETIME2(0),
            @duenio INT, @lider INT, @estado VARCHAR(10);

    BEGIN TRAN;

    SELECT @base = MontoBase, @actual = OfertaActual,
           @ini = FechaInicio, @fin = FechaCierre,
           @duenio = UsuarioId, @lider = LiderUsuarioId, @estado = Estado
    FROM dbo.Vehiculos_3193 WITH (UPDLOCK, HOLDLOCK)
    WHERE Id = @VehiculoId;

    IF @base IS NULL
    BEGIN ROLLBACK; SELECT 'NO_EXISTE' AS Resultado, CAST(NULL AS DECIMAL(12,2)) AS MontoMinimo, CAST(NULL AS INT) AS AnteriorLiderId; RETURN; END

    SET @minimo = CASE WHEN @actual IS NULL THEN @base
                       ELSE CEILING(@actual * 1.10 * 100) / 100.0 END;

    IF @estado <> 'ABIERTA' OR @ahora >= @fin
    BEGIN ROLLBACK; SELECT 'CERRADA' AS Resultado, @minimo AS MontoMinimo, @lider AS AnteriorLiderId; RETURN; END

    IF @ahora < @ini
    BEGIN ROLLBACK; SELECT 'NO_INICIADA' AS Resultado, @minimo AS MontoMinimo, @lider AS AnteriorLiderId; RETURN; END

    IF @duenio = @UsuarioId
    BEGIN ROLLBACK; SELECT 'PROPIETARIO' AS Resultado, @minimo AS MontoMinimo, @lider AS AnteriorLiderId; RETURN; END

    IF @lider = @UsuarioId
    BEGIN ROLLBACK; SELECT 'YA_ES_LIDER' AS Resultado, @minimo AS MontoMinimo, @lider AS AnteriorLiderId; RETURN; END

    IF @Monto < @minimo
    BEGIN ROLLBACK; SELECT 'MONTO_INSUFICIENTE' AS Resultado, @minimo AS MontoMinimo, @lider AS AnteriorLiderId; RETURN; END

    INSERT INTO dbo.Pujas_3193 (VehiculoId, UsuarioId, Monto, FechaPuja)
    VALUES (@VehiculoId, @UsuarioId, @Monto, @ahora);

    UPDATE dbo.Vehiculos_3193
       SET OfertaActual   = @Monto,
           LiderUsuarioId = @UsuarioId,
           TotalPujas     = TotalPujas + 1
     WHERE Id = @VehiculoId;

    COMMIT;

    SELECT 'OK' AS Resultado,
           CAST(CEILING(@Monto * 1.10 * 100) / 100.0 AS DECIMAL(12,2)) AS MontoMinimo,
           @lider AS AnteriorLiderId,   -- para notificar "Tu oferta ha sido superada"
           (SELECT TotalPujas FROM dbo.Vehiculos_3193 WHERE Id = @VehiculoId) AS TotalPujas
END
GO

/* =====================================================================
   9. SP: CERRAR SUBASTAS VENCIDAS — lo llama un @Scheduled del backend
      Con pujas  -> VENDIDA ; sin pujas (no alcanzó la base) -> DESIERTA
      Devuelve las subastas cerradas para notificarlas por WebSocket.
   ===================================================================== */
CREATE OR ALTER PROCEDURE dbo.sp_CerrarSubastasVencidas_3193
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.Vehiculos_3193
       SET Estado = CASE WHEN OfertaActual IS NOT NULL AND OfertaActual >= MontoBase
                         THEN 'VENDIDA' ELSE 'DESIERTA' END
    OUTPUT inserted.Id, inserted.Estado, inserted.OfertaActual, inserted.LiderUsuarioId
     WHERE Estado = 'ABIERTA'
       AND FechaCierre <= SYSUTCDATETIME();
END
GO

/* Verificación rápida */
SELECT name, type_desc FROM sys.objects
WHERE name LIKE '%[_]3193' AND type IN ('U', 'V', 'P')
ORDER BY type_desc, name;
GO
