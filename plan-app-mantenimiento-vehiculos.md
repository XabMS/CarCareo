# Plan de acción — App Android de mantenimiento de vehículos

> Documento de especificación e implementación. Destinado a ser ejecutado por un agente de desarrollo.
> Léelo entero antes de escribir código. Implementa por fases, en orden, sin adelantar funcionalidad.

---

## 1. Objetivo

App Android para gestionar el mantenimiento de varios vehículos personales. El usuario define sus
vehículos, asigna a cada uno un plan de mantenimiento (a partir de plantillas o a medida), y registra
las intervenciones realizadas. La app calcula y muestra qué mantenimiento toca a continuación.

**Criterio de éxito:** el usuario sigue usándola a los dos años. Esto depende enteramente de que
mantener los datos al día cueste poco esfuerzo. Cualquier decisión de diseño que aumente la fricción
de registrar una intervención o de actualizar el kilometraje está mal, aunque mejore otra cosa.

---

## 2. Decisiones ya cerradas

No las cuestiones ni las cambies sin consultar.

| Aspecto | Decisión |
|---|---|
| Plataforma | Android nativo. No web, no iOS, no responsive. |
| Lenguaje / UI | Kotlin + Jetpack Compose (Material 3) |
| Persistencia | Room (SQLite local). Sin backend, sin cuentas, sin red. |
| Arquitectura | MVVM: Compose → ViewModel → Repository → DAO |
| Sincronización | Ninguna. Portabilidad vía export/import de fichero JSON. |
| minSdk / targetSdk | minSdk 26, targetSdk el más reciente estable |
| Idioma UI | Bilingüe español/inglés según el idioma del dispositivo. Ver apartado 3.0. |
| Unidades | Kilómetros y meses. Sin millas. |
| Categorías v1 | Moto térmica, coche térmico, coche eléctrico. Con plantilla cada una. |

**Permisos:** ninguno en tiempo de ejecución salvo notificaciones (`POST_NOTIFICATIONS`, API 33+) si
se implementa la fase opcional. El export/import usa Storage Access Framework, que no requiere permiso.

---

## 2.1 Localización

La app es bilingüe desde F0. No es una tarea de pulido final: reconvertir literales a recursos al
final del proyecto es tedioso y siempre se escapa alguno.

**Regla:** dispositivo en español → app en español. Dispositivo en cualquier otro idioma → app en
inglés. Esto es exactamente el comportamiento por defecto de Android, así que no escribas lógica de
selección de idioma: basta con la estructura de recursos correcta.

```
res/values/strings.xml      → inglés  (default / fallback)
res/values-es/strings.xml   → español
```

El inglés va en `values/` sin sufijo, no en `values-en/`. Es lo que hace que cualquier idioma no
contemplado caiga en inglés en lugar de fallar.

**Requisitos:**

- Cero literales de texto en el código Kotlin ni en los `@Composable`. Todo vía `stringResource()`.
  Activa el lint `HardcodedText` como error de compilación para que no se cuele ninguno.
- Ambos ficheros se actualizan **en la misma fase** en que se crea cada pantalla. Nunca dejes
  `values-es/` a medias "para luego".
- Plurales con `<plurals>`, no con concatenación (`"1 km restante"` vs `"1.200 km restantes"`).
- Fechas y números formateados con la locale del sistema, no con patrones fijos. Usa
  `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` y `NumberFormat`, no `"dd/MM/yyyy"`.
- Las **plantillas de mantenimiento** (apartado 6) también son bilingües: cada tarea de plantilla
  lleva su nombre en ambos idiomas y se instancia en el idioma activo al aplicarla. Una vez creada,
  el nombre de la tarea es un dato del usuario y **no** se traduce ni se retraduce después.
- Los nombres que introduce el usuario (vehículos, tareas propias, notas, talleres) nunca se tocan.
- El fichero de export/import (apartado 7) es **independiente del idioma**: los enums viajan como
  constantes (`MOTO_TERMICA`), las fechas en ISO-8601 y los importes con punto decimal. Un export
  hecho en un móvil en español debe importarse sin problema en uno en inglés.

---

## 3. Modelo de datos

Cuatro entidades. El estado de una tarea **nunca se almacena**: siempre se calcula.

### 3.1 `Vehicle`

| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long (PK, autogen) | |
| `name` | String | Apodo. Obligatorio. Ej. "Hornet" |
| `category` | Enum | `MOTO_TERMICA`, `COCHE_TERMICO`, `COCHE_ELECTRICO` |
| `make` | String? | Marca |
| `model` | String? | Modelo |
| `year` | Int? | |
| `plate` | String? | Matrícula |
| `lastConfirmedKm` | Int | Último km **confirmado** por el usuario |
| `lastConfirmedKmDate` | LocalDate | Fecha de esa confirmación |
| `annualKmEstimate` | Int | Para estimar el km actual entre confirmaciones. Default 12000 |
| `purchaseDate` | LocalDate? | |
| `notes` | String? | |
| `archived` | Boolean | Vehículo vendido: se oculta pero no se borra. Default false |

### 3.2 `MaintenanceTask` — una línea del plan de un vehículo

| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long (PK) | |
| `vehicleId` | Long (FK → Vehicle, ON DELETE CASCADE) | |
| `name` | String | Ej. "Cambio de aceite y filtro" |
| `intervalKm` | Int? | Nullable |
| `intervalMonths` | Int? | Nullable |
| `warnKmBefore` | Int | Margen para el estado "Próximo". Default 1000 |
| `warnDaysBefore` | Int | Default 30 |
| `notes` | String? | Ej. "5W-30, 3,8 L" |
| `active` | Boolean | Permite desactivar sin borrar el histórico. Default true |
| `sortOrder` | Int | Orden manual en el editor |

**Regla de validación:** al menos uno de `intervalKm` / `intervalMonths` debe estar informado.
Si ambos lo están, vence **lo que ocurra primero**.

### 3.3 `MaintenanceRecord` — una intervención realizada

| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long (PK) | |
| `vehicleId` | Long (FK, CASCADE) | |
| `date` | LocalDate | Obligatorio |
| `odometerKm` | Int | Obligatorio. **Confirma el km del vehículo** |
| `workshop` | String? | |
| `cost` | BigDecimal? | |
| `notes` | String? | |
| `attachmentUri` | String? | URI persistida de una foto/PDF de factura |

### 3.4 `RecordTaskCrossRef` — qué tareas cubre cada registro

| Campo | Tipo |
|---|---|
| `recordId` | Long (FK, CASCADE) |
| `taskId` | Long (FK, CASCADE) |

Clave primaria compuesta. **Relación N:N y esto es esencial**: una revisión de taller cubre ocho
tareas en un solo registro. Un registro puede además no cubrir ninguna tarea (intervención no
planificada: pinchazo, avería, retrovisor roto). Debe seguir siendo válido y aparecer en el histórico.

---

## 4. Lógica de cálculo — el núcleo de la app

Impleméntala en una clase pura, sin dependencias de Android ni de Room, y **cúbrela con tests
unitarios antes de conectarla a la UI**. Es el sitio donde un error hace que la app mienta al usuario.

### 4.1 Kilometraje estimado

```
kmEstimado(vehículo, hoy) =
    lastConfirmedKm + round(annualKmEstimate * díasDesde(lastConfirmedKmDate) / 365)
```

- Nunca decrece.
- La UI debe distinguir visualmente **estimado** de **confirmado** (icono `~` o texto secundario
  "estimado hace 3 meses"). No presentes una estimación como si fuera un dato real.
- Se confirma automáticamente al crear un `MaintenanceRecord`, o manualmente desde el detalle.
- Si el usuario introduce un km confirmado inferior al último confirmado, avisa pero permítelo
  (corrección de errata). Si es inferior en más del 10%, pide confirmación explícita.

### 4.2 Última ejecución de una tarea

Del conjunto de registros ligados a esa tarea, el de **fecha más reciente**. Guarda su `date` y su
`odometerKm`.

**Si nunca se ha ejecutado:** se toma como base `purchaseDate` y el km de compra si existe; si no,
`lastConfirmedKmDate` y `lastConfirmedKm` iniciales. Marca internamente la tarea como
`sinHistórico = true` y muéstrala en la UI como "Sin registro previo" en vez de dar una fecha de
vencimiento falsa con aire de certeza.

### 4.3 Estado

Para cada dimensión informada se calcula el margen restante:

```
margenKm    = (últimoKm    + intervalKm)     - kmEstimadoActual
margenDías  = (últimaFecha + intervalMonths) - hoy
```

Estado de la tarea:

- `VENCIDO` — cualquiera de los márgenes informados es ≤ 0
- `PROXIMO` — `margenKm <= warnKmBefore` o `margenDías <= warnDaysBefore`
- `VIGENTE` — en el resto de casos

**Orden de presentación:** por urgencia real, no alfabético ni por categoría. Ordena por
`min(margenKm / intervalKm, margenDías / intervalMonths)` normalizado, ascendente. Lo más urgente
arriba, siempre.

### 4.4 Estado del vehículo

El peor estado entre sus tareas activas. Semáforo en la lista: rojo `VENCIDO`, ámbar `PROXIMO`,
verde `VIGENTE`.

---

## 5. Pantallas

### P1 — Garaje (home)

Lista de vehículos no archivados. Cada tarjeta: nombre, categoría (icono), km actual, indicador de
color y **una sola línea** con lo más urgente: *"Próximo: aceite en 1.200 km / marzo"*.

Debe responder en dos segundos a la pregunta "¿tengo algo pendiente?". Nada más.

FAB: añadir vehículo. Overflow: importar/exportar datos, ver archivados.

### P2 — Detalle de vehículo

- Cabecera: nombre, datos, km actual grande con botón **"Actualizar km"** bien visible.
- Lista de tareas ordenada por urgencia. Cada línea: nombre, margen en **ambas unidades**
  ("faltan 1.200 km · 4 meses"), color de estado.
- Acciones: **Registrar mantenimiento** (destacada), Editar plan, Ver histórico, Editar vehículo.

### P3 — Registrar mantenimiento

**La pantalla más importante. Debe cerrarse en menos de 30 segundos.**

- Fecha: prerrellenada a hoy.
- Km: prerrellenado con el km estimado, editable, con foco automático.
- **Lista de tareas con checkboxes, selección múltiple.** Las tareas vencidas y próximas aparecen
  arriba y **premarcadas**. Botón "Seleccionar todas las vencidas".
- Taller, coste, notas, adjunto: opcionales y colapsados por defecto.
- Guardar → confirma el km del vehículo y recalcula todos los estados.

No obligues a seleccionar ninguna tarea: el registro libre debe ser válido.

### P4 — Editor de plan

Punto de abandono clásico. Mitiga la fricción así:

- Al crear un vehículo, se ofrece **aplicar la plantilla de su categoría** (opción por defecto).
- Alternativa: **duplicar el plan de otro vehículo existente**.
- Alternativa: empezar vacío.
- Todas las tareas de la plantilla son editables y borrables tras aplicarla.
- La app debe ser usable con tres tareas definidas. **No exijas un plan completo para nada.**

Edición de tarea: nombre, intervalo km, intervalo meses, márgenes de aviso, notas, activo.

### P5 — Histórico

Timeline descendente de los registros del vehículo. Cada entrada: fecha, km, tareas cubiertas,
taller, coste. Cabecera con **coste acumulado** y coste de los últimos 12 meses.

Filtro por tarea. Editar y borrar registros (borrar recalcula estados).

### P6 — Ajustes / Datos

Exportar, importar, ver vehículos archivados, versión de la app.

---

## 6. Plantillas de plan

Van embebidas en la app como recurso estático (JSON en `assets/` o constantes Kotlin). Son un
**punto de partida genérico y editable**, no una verdad del fabricante. Muestra este aviso al
aplicarlas: *"Valores orientativos. Consulta el manual de tu vehículo y ajústalos."*

### 6.1 Moto térmica

| Tarea | km | meses |
|---|---|---|
| Aceite motor y filtro | 8.000 | 12 |
| Filtro de aire | 12.000 | 24 |
| Bujías | 16.000 | — |
| Líquido de frenos | — | 24 |
| Líquido refrigerante | 30.000 | 24 |
| Limpieza y tensado de cadena | 1.000 | — |
| Sustitución de kit de transmisión | 28.000 | — |
| Inspección de pastillas de freno | 6.000 | 12 |
| Neumáticos | 15.000 | 60 |
| Revisión general en taller | 12.000 | 12 |

### 6.2 Coche térmico

| Tarea | km | meses |
|---|---|---|
| Aceite motor y filtro | 15.000 | 12 |
| Filtro de aire | 30.000 | 24 |
| Filtro de habitáculo | 15.000 | 12 |
| Filtro de combustible | 40.000 | 48 |
| Bujías (gasolina) | 60.000 | — |
| Líquido de frenos | — | 24 |
| Líquido refrigerante | 60.000 | 48 |
| Correa de distribución | 120.000 | 84 |
| Pastillas de freno | 40.000 | — |
| Discos de freno | 80.000 | — |
| Neumáticos | 40.000 | 72 |
| Batería 12 V | — | 60 |
| Escobillas limpiaparabrisas | — | 12 |
| ITV | — | 12 |

### 6.3 Coche eléctrico

Sin aceite motor, sin filtros de combustible, sin bujías. Frenos con menos desgaste por la
regeneración, pero neumáticos con **más** desgaste por peso y par.

| Tarea | km | meses |
|---|---|---|
| Filtro de habitáculo | 15.000 | 12 |
| Líquido de frenos | — | 24 |
| Refrigerante de batería / circuito térmico | 100.000 | 60 |
| Aceite de reductora | 100.000 | — |
| Inspección y engrase de frenos | 20.000 | 12 |
| Pastillas de freno | 60.000 | — |
| Neumáticos | 30.000 | 60 |
| Rotación de neumáticos | 10.000 | — |
| Batería 12 V / auxiliar | — | 48 |
| Revisión de cable y equipo de carga | — | 12 |
| Escobillas limpiaparabrisas | — | 12 |
| ITV | — | 12 |

---

## 7. Export / Import

**Formato:** JSON UTF-8, un único fichero, legible por humanos.
**Nombre sugerido:** `mantenimiento-vehiculos-AAAAMMDD.json`
**Mecanismo:** Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`).

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-09-06T21:00:00Z",
  "vehicles": [
    {
      "name": "...", "category": "MOTO_TERMICA", "...": "...",
      "tasks": [ { "name": "...", "intervalKm": 8000, "intervalMonths": 12, "...": "..." } ],
      "records": [
        { "date": "2026-03-14", "odometerKm": 24500, "cost": 189.50,
          "taskNames": ["Aceite motor y filtro", "Filtro de aire"], "...": "..." }
      ]
    }
  ]
}
```

**Requisitos:**

- Incluye siempre `schemaVersion` y valídalo al importar.
- Los vínculos registro↔tarea se serializan **por nombre de tarea dentro del vehículo**, no por id,
  para que el fichero sobreviva a una reimportación.
- Los adjuntos **no** se incluyen (son URIs externas). Advierte al usuario en la exportación.
- Importar ofrece dos modos: **reemplazar todo** o **añadir** (los vehículos entrantes se crean
  nuevos, nunca se fusionan). Confirma con diálogo explícito antes de reemplazar.
- Un fichero corrupto o de versión superior debe fallar limpiamente sin tocar la base de datos:
  parsea entero en memoria y valida antes de abrir la transacción de escritura.

---

## 8. Fases de implementación

Cada fase termina con una app que compila, arranca y es verificable a mano. No empieces una fase
sin haber cerrado la anterior.

### F0 — Esqueleto
Proyecto Compose + Material 3. Room configurado con las cuatro entidades y sus DAO. Navigation
Compose con las rutas de las seis pantallas, todas vacías. Tema claro/oscuro. Estructura de recursos
bilingüe (`values/` + `values-es/`) y lint `HardcodedText` como error. Compila y arranca.

### F1 — Vehículos
CRUD completo de vehículos. Pantalla Garaje con la lista (sin semáforo todavía). Detalle con los
datos y el km. Actualización manual del km con la validación de retroceso. Cálculo de km estimado
con sus tests unitarios. Archivar/desarchivar.

### F2 — Plan y plantillas
CRUD de tareas. Editor de plan. Las tres plantillas embebidas. Aplicar plantilla al crear vehículo.
Duplicar plan desde otro vehículo. Validación de "al menos un intervalo".

### F3 — Registros y estados
**Fase crítica.** Primero el motor de cálculo del apartado 4 como clase pura, con su batería de
tests unitarios (ver apartado 9). Después la pantalla de registro con selección múltiple. Después
el estado en el detalle y el semáforo en el garaje. En este orden.

### F4 — Histórico y costes
Timeline, filtro por tarea, edición y borrado de registros, coste acumulado y a 12 meses,
adjuntos con URI persistida (`takePersistableUriPermission`).

### F5 — Export / Import
Según el apartado 7, con los tests de round-trip.

### F6 — Notificaciones *(opcional, solo si F0–F5 están sólidas)*
`WorkManager` con un chequeo semanal. Notificación cuando una tarea entra en `PROXIMO` o `VENCIDO`
(una por vehículo, agrupada, no una por tarea). Recordatorio mensual de "actualiza el kilometraje".
Todo desactivable en Ajustes.

### F7 — Pulido
Estados vacíos con texto útil, confirmaciones de borrado, accesibilidad (`contentDescription`),
rotación de pantalla. Recorrido completo de la app con el dispositivo en español y con el
dispositivo en inglés, comprobando que no queda ningún texto sin traducir ni ningún literal
desbordando su contenedor.

---

## 9. Tests obligatorios

Unitarios sobre el motor de cálculo, como mínimo estos casos:

1. Tarea solo con `intervalKm`, sin superar → `VIGENTE`
2. Tarea solo con `intervalMonths`, superada → `VENCIDO`
3. Tarea con ambos intervalos: vence por km antes que por tiempo → `VENCIDO`
4. Tarea con ambos intervalos: vence por tiempo antes que por km → `VENCIDO`
5. Dentro del margen de aviso por km → `PROXIMO`
6. Dentro del margen de aviso por días → `PROXIMO`
7. Tarea sin ningún registro previo → `sinHistórico`, sin fecha de vencimiento inventada
8. Km estimado con 0 días transcurridos → igual al confirmado
9. Km estimado con 365 días → confirmado + `annualKmEstimate`
10. Un registro que cubre varias tareas resetea todas ellas
11. Borrar el último registro de una tarea la devuelve al estado anterior
12. Registro sin tareas asociadas: válido, no altera ningún estado, aparece en histórico
13. Ordenación por urgencia con tareas de intervalos muy distintos

Y de integración: round-trip export → import produce datos equivalentes, incluidos los vínculos
registro↔tarea.

---

## 10. Fuera de alcance

No implementes nada de esto aunque parezca fácil o útil:

- Multiusuario, cuentas, login
- Cualquier sincronización o backend
- Lectura OBD / Bluetooth
- Integración con talleres o concesionarios
- Base de datos de vehículos por marca/modelo
- Gestión de seguros, impuestos o repostajes
- Consumos, autonomía, estadísticas de uso
- Millas o unidades imperiales
- iOS, web, tablet layout

---

## 11. Notas para el implementador

- El usuario es ingeniero de sistemas, no desarrollador Android. Comenta las decisiones no obvias
  y evita magia de framework sin explicar.
- Ante una ambigüedad no resuelta en este documento: elige la opción más simple, déjala anotada y
  sigue. No pares a preguntar por detalles menores.
- Los textos de UI van en `strings.xml` (inglés) y `values-es/strings.xml` (español) desde F0.
  Nada de literales en el código, en ninguna fase.
- Fechas con `java.time` (`LocalDate`), con desugaring si hace falta para minSdk 26.
