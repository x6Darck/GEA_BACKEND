# 2. Backend (Spring Boot)

El backend de GEA es una aplicación **Spring Boot 3.4.3 sobre Java 21** que expone la API REST de la plataforma bajo el *context-path* `/api` (puerto 8083). Toda la lógica de negocio, la persistencia y la seguridad residen aquí; tanto el panel web (React) como la app móvil (Flutter) son únicamente clientes de esta API.

El paquete raíz del código es `com.calendario.callapp.callapp_backend`. A partir de aquí, todas las clases mencionadas se ubican en subpaquetes de esa raíz.

## 2.1 Arquitectura en capas

El backend sigue una **arquitectura en capas** clásica de tipo *Controller → Service → Repository → Entity*, con un flujo de datos basado en DTOs (objetos de transferencia) que aíslan el modelo de dominio (entidades JPA) de la API pública. Cada capa tiene una responsabilidad única y depende únicamente de la capa inmediatamente inferior:

- **Capa de presentación (`controller/`)**: clases anotadas con `@RestController`. Reciben las peticiones HTTP, validan el cuerpo entrante (`@Valid` sobre un *DTO Request*), delegan en la capa de servicio y devuelven la respuesta envuelta en `ApiResponse<T>` (ver `util/ApiResponse`). No contienen lógica de negocio. Ejemplo: `AuthController` expone `POST /auth/login` y se limita a invocar `authService.login(request)`.

- **Capa de servicio (`service/` + `service/impl/`)**: cada caso de uso se define como una **interfaz** en `service/` (p. ej. `AuthService`) y se implementa en `service/impl/` (p. ej. `AuthServiceImpl`). Aquí vive la lógica de negocio, la gestión transaccional (`@Transactional`) y la orquestación de repositorios, mappers y servicios de seguridad. Esta separación interfaz/implementación favorece la inversión de dependencias (principio D de SOLID): los controladores dependen de la abstracción, no de la implementación concreta.

- **Capa de acceso a datos (`repository/`)**: interfaces que extienden Spring Data JPA. Encapsulan el acceso a la base de datos mediante *query methods* derivados del nombre (p. ej. `findByNombreIgnoreCase`) y consultas optimizadas con `@Query` (p. ej. `getByCorreoOptimized`, que precarga rol y oficina para evitar el problema N+1 en el login).

- **Capa de dominio / persistencia (`entity/`)**: clases anotadas con `@Entity` que representan las tablas de la base de datos MySQL. Son el modelo persistente y nunca se exponen directamente al cliente.

- **Mappers (`mapper/`)**: traducen entre entidades y DTOs (entidad → *DTO Response* para la salida, *DTO Request* → entidad para la entrada), evitando filtrar campos sensibles (como el `password`) y desacoplando el contrato de la API del esquema de base de datos.

El flujo de una petición típica es: el cliente envía un **DTO Request** → el **Controller** lo valida y lo pasa al **Service** → el Service usa el **Repository** para leer/escribir **Entidades JPA** sobre **MySQL** → el Service emplea un **Mapper** para construir el **DTO Response** → el Controller devuelve ese DTO al cliente. La siguiente figura resume este recorrido.

![Figura: Capas del backend](../assets/diagrams/backend-capas.svg)
<p class="figure-caption">Figura: Arquitectura en capas del backend</p>

## 2.2 Estructura de paquetes

Bajo el paquete raíz `com.calendario.callapp.callapp_backend`, el código se organiza en los siguientes subpaquetes. El número de clases es orientativo del estado actual del repositorio.

| Subpaquete | Contenido | Clases (aprox.) |
|------------|-----------|-----------------|
| `config/` | Configuración transversal de la aplicación: pools asíncronos, caché, codificador de contraseñas, auditoría JPA, propiedades de seguridad y *seeders* de datos iniciales. | 8 + `package-info` |
| `controller/` | Controladores REST (`@RestController`). Punto de entrada HTTP de la API. | 13 |
| `dto/request/` | Objetos de entrada (cuerpo de las peticiones). Incluyen validaciones Bean Validation (`@NotBlank`, `@Email`, etc.). | 18 |
| `dto/response/` | Objetos de salida que se serializan a JSON. Construidos normalmente con el patrón *builder* de Lombok. | 18 |
| `entity/` | Entidades JPA (`@Entity`) mapeadas a las tablas MySQL, más enums de dominio (estados, roles, proveedores de autenticación). | 22 |
| `exception/` | Manejo centralizado de errores: `GlobalExceptionHandler` (`@RestControllerAdvice`) que normaliza las respuestas de error. | 1 |
| `mapper/` | Conversores entidad ↔ DTO. | 5 |
| `repository/` | Interfaces Spring Data JPA para el acceso a datos. | 15 |
| `security/` | Configuración y componentes de seguridad: cadena de filtros, JWT, *rate limiting*, *entry points* de error. | 7 + `package-info` |
| `service/` | Interfaces de los casos de uso (contratos de la capa de negocio). | 5 |
| `service/impl/` | Implementaciones concretas de los servicios. | 18 |
| `util/` | Utilidades transversales: `ApiResponse<T>` (envoltorio uniforme de respuestas) y `AuditRevisionListener` (auditoría con Hibernate Envers). | 2 |

> En la raíz del paquete se encuentra además la clase principal anotada con `@SpringBootApplication`, punto de arranque de la aplicación.

## 2.3 Capa de configuración

El paquete `config/` agrupa la configuración programática (clases `@Configuration`) y los inicializadores de datos. La siguiente tabla resume la responsabilidad real de cada clase; las más relevantes se detallan en subsecciones posteriores.

| Clase | Anotaciones clave | Responsabilidad |
|-------|-------------------|-----------------|
| `AsyncConfig` | `@Configuration`, `@EnableAsync`, `AsyncConfigurer` | Define el pool de hilos para tareas `@Async` (notificaciones) y el manejador global de excepciones asíncronas. |
| `CacheConfig` | `@Configuration`, `@EnableCaching` | Configura el `CacheManager` con Caffeine: caché `userDetails` con TTL de 45 s. |
| `PasswordConfig` | `@Configuration` | Expone el bean `BCryptPasswordEncoder` para el hash de contraseñas. |
| `JpaAuditingConfig` | `@Configuration` | Proveedor `AuditorAware<String>` que identifica al usuario autor de cada cambio para la auditoría JPA. |
| `SecurityProperties` | `@Configuration`, `@ConfigurationProperties("app.security")`, `@Data` | Mapea propiedades de seguridad (CORS y *rate limit*) desde `application.properties`. |
| `AppSecurityProperties` | `@Configuration`, `@ConfigurationProperties("app.security")` | Variante de propiedades de seguridad (orígenes CORS y *rate limit*) con getters/setters explícitos. |
| `DataInitializer` | `@Component`, `CommandLineRunner`, `@Transactional` | Siembra los datos maestros y de prueba en cada arranque. |
| `OficinaDataSeeder` | `@Configuration`, `CommandLineRunner` | Inserta oficinas adicionales (`Comunicaciones`, `Integridad`) si no existen. |

> **Nota sobre `SecurityProperties` / `AppSecurityProperties`**: ambas clases declaran el prefijo `app.security` y modelan los mismos conceptos (orígenes CORS permitidos y límite de peticiones por minuto, con `enabled=true` y `requestsPerMinute=10` por defecto). Coexisten como puntos de configuración tipados; los valores reales se inyectan en los filtros y en `SecurityConfig` vía `@Value`.

### 2.3.1 AsyncConfig — pool de hilos asíncrono

`AsyncConfig` habilita el procesamiento asíncrono (`@EnableAsync`) e implementa `AsyncConfigurer` para personalizar tanto el executor por defecto como el tratamiento de errores. El bean `notificacionesExecutor` es un `ThreadPoolTaskExecutor` parametrizado por propiedades (con valores por defecto entre paréntesis):

- `core-pool-size` (**3**): hilos mínimos siempre activos.
- `max-pool-size` (**6**): hilos máximos cuando la cola se llena.
- `queue-capacity` (**30**): tareas en espera antes de crear hilos adicionales.
- Prefijo de nombre de hilo `notif-async-` (facilita el rastreo en logs).
- Política de rechazo `CallerRunsPolicy`: si el pool y la cola están saturados, la tarea se ejecuta en el hilo que la invocó en lugar de descartarse (contrapresión natural, sin pérdida de notificaciones).

El manejador `getAsyncUncaughtExceptionHandler()` registra en el log cualquier excepción no capturada en métodos `@Async` con valor de retorno `void`, evitando que un fallo asíncrono pase desapercibido.

```java
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Value("${app.async.notifications.core-pool-size:3}")
    private int corePoolSize;

    @Value("${app.async.notifications.max-pool-size:6}")
    private int maxPoolSize;

    @Value("${app.async.notifications.queue-capacity:30}")
    private int queueCapacity;

    @Bean(name = "notificacionesExecutor")
    public ThreadPoolTaskExecutor notificacionesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("notif-async-");
        // Si el pool y la cola se saturan, la tarea corre en el hilo llamador
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return notificacionesExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("[ASYNC-ERROR] Excepción no capturada en método '{}': {}",
                      method.getName(), ex.getMessage(), ex);
    }
}
```

### 2.3.2 CacheConfig — caché Caffeine

`CacheConfig` activa la abstracción de caché de Spring (`@EnableCaching`) y registra un `CaffeineCacheManager` con una única caché llamada **`userDetails`**. Esta caché almacena los `UserDetails` cargados durante la autenticación para evitar una consulta a base de datos en cada petición:

- **TTL**: `expireAfterWrite(45, SECONDS)`. El comentario del código lo justifica: *"un usuario baneado deja de autenticarse en máx. 45 s"*. Es decir, el TTL acota la ventana durante la cual un cambio de estado/rol del usuario aún no se ha propagado.
- **Tamaño máximo**: `maximumSize(500)` entradas (límite de memoria).

Esta caché es consumida por `CustomUserDetailsService` (ver 2.4.4) mediante `@Cacheable`.

### 2.3.3 PasswordConfig — codificador BCrypt

Expone un único bean `BCryptPasswordEncoder` (tipo `PasswordEncoder`). Es el algoritmo usado para *hashear* las contraseñas al crear usuarios (`DataInitializer.crearUsuario` invoca `passwordEncoder.encode(...)`) y para verificarlas en el login (`AuthServiceImpl` usa `passwordEncoder.matches(...)`). BCrypt incorpora *salt* automático y un factor de coste configurable, resistente a ataques de fuerza bruta.

### 2.3.4 JpaAuditingConfig — auditoría JPA

Provee el bean `AuditorAware<String> auditorProvider`, que Spring Data JPA usa para rellenar los campos de auditoría (autor de creación/modificación) de las entidades. La lógica:

- Lee el `Authentication` del `SecurityContextHolder`.
- Si no hay autenticación, no está autenticado, o el principal es `anonymousUser`, devuelve **`"SISTEMA"`** (cambios automáticos como los *seeders*).
- En caso contrario devuelve `authentication.getName()` (el correo del usuario autenticado).

De este modo, cada registro queda atribuido a un autor identificable, ya sea un usuario real o el propio sistema.

### 2.3.5 DataInitializer — siembra de datos maestros y de prueba

`DataInitializer` implementa `CommandLineRunner`, por lo que su método `run` se ejecuta una vez completado el arranque, dentro de una transacción (`@Transactional`). Garantiza un estado funcional mínimo del sistema. Las operaciones, en orden:

1. **Roles base** (sólo si la tabla está vacía): `SuperAdmin`, `Comunicaciones`, `Oficina`, `Usuario Autenticado`.
2. **Oficinas** (sincronización idempotente): inserta/actualiza 11 oficinas oficiales (Facultad Derecho, Facultad CEAC, Facultad Ingeniería, Rectoría, Biblioteca, Consultorio Jurídico, Bienestar Universitario, Proyección Social, Marketing y Comunicaciones, Sistemas, Egresados) y **desactiva** las que no figuran en la lista oficial.
3. **Tipos de evento** (catálogo con color): Académico, Cultural, Deportivo, Institucional, Investigación, Bienestar, cada uno con su `colorHex`.
4. **Lugares físicos** (sincronización idempotente): 13 espacios con capacidad (Aula Máxima, Sala de Audiencias, Teatro, Biblioteca, Plaza de Banderas, etc.); también desactiva los obsoletos.
5. **Usuarios del sistema** (sólo si la tabla está vacía): tres cuentas base con contraseña `1234` (codificada con BCrypt) y proveedor `LOCAL`:
   - `Cesar@gmail.com` → rol **SuperAdmin** (oficina Sistemas).
   - `Comunicaciones@gmail.com` → rol **Comunicaciones** (oficina Marketing y Comunicaciones).
   - `oficina@gmail.com` → rol **Oficina** (oficina Rectoría).
   - Además vincula un avatar por defecto a los usuarios sin foto, copiándolo desde `classpath:static/assets/img/default-avatar.png` al directorio `uploads/` y registrándolo como `ArchivoAdjunto` público.
6. **Datos de prueba** (sólo si no hay solicitudes de evento): crea una `SolicitudEvento` de ejemplo (estado `APROBADA`) y una `SolicitudAnuncio` de ejemplo (estado `PUBLICADA`), con sus piezas gráficas.

> Las credenciales y datos de prueba anteriores son **valores de desarrollo**. En un despliegue productivo deben cambiarse las contraseñas por defecto.

### 2.3.6 OficinaDataSeeder — oficinas complementarias

Componente `CommandLineRunner` independiente que inserta dos oficinas adicionales si aún no existen (`findByNombreIgnoreCase` vacío): **`Comunicaciones`** y **`Integridad`**. Es idempotente y complementa el catálogo de oficinas sembrado por `DataInitializer`.

## 2.4 Capa de seguridad

El paquete `security/` concentra toda la configuración de Spring Security. El modelo es **stateless basado en JWT**: no hay sesiones de servidor; cada petición se autentica por sí misma mediante un *Bearer token*. La cadena de filtros se complementa con un limitador de peticiones por IP y con manejadores de error que devuelven JSON uniforme.

### 2.4.1 SecurityConfig

`SecurityConfig` (`@EnableWebSecurity`, `@EnableMethodSecurity`) define el bean `SecurityFilterChain` con las siguientes características:

- **CORS**: habilitado mediante `corsConfigurationSource()`. Los orígenes permitidos se inyectan con `@Value("${app.security.cors.allowed-origins:*}")` y se aplican como *origin patterns*. Métodos permitidos: `GET, POST, PUT, DELETE, OPTIONS, PATCH`. Cabeceras permitidas: `Authorization, Cache-Control, Content-Type, Bypass-Tunnel-Reminder`. `allowCredentials = true`.
- **CSRF**: **deshabilitado** (`csrf.disable()`), apropiado para una API stateless consumida por clientes que envían el token explícitamente.
- **Gestión de sesión**: `SessionCreationPolicy.STATELESS` (no se crea ni usa `HttpSession`).
- **Cabeceras de seguridad**:
  - `frameOptions().deny()` — previene *clickjacking*.
  - `X-XSS-Protection: 1; mode=block`.
  - **Content-Security-Policy**: `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;`.
- **Manejo de excepciones**: `authenticationEntryPoint` → `JwtAuthenticationEntryPoint` (401) y `accessDeniedHandler` → `JwtAccessDeniedHandler` (403).
- **Beans adicionales**: `AuthenticationManager` (obtenido de `AuthenticationConfiguration`) y `CorsConfigurationSource`.

**Mapa de autorización ruta → roles** (extraído literalmente de `authorizeHttpRequests`). El orden importa: Spring evalúa los *matchers* de arriba abajo y aplica el primero que coincide.

| # | Método | Patrón de ruta | Regla de acceso |
|---|--------|----------------|-----------------|
| 1 | (todos) | `/auth/**`, `/api/auth/**` | `permitAll()` — público (login, auth Microsoft) |
| 2 | `GET` | `/app/eventos/publicados/**`, `/app/anuncios/publicados/**`, `/archivos/public/**`, `/app/eventos/agenda/export/pdf` | `permitAll()` — contenido público de la app móvil |
| 3 | (todos) | `/lugares-fisicos/**` | `authenticated()` — cualquier usuario autenticado |
| 4 | (todos) | `/admin/**` | `hasAnyRole("SUPER_ADMIN", "ADMIN")` |
| 5 | (todos) | `/comunicaciones/archivos/upload` | `hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "USUARIO_AUTENTICADO_APP")` |
| 6 | (todos) | `/comunicaciones/**` | `hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES")` |
| 7 | (todos) | `/oficina/**` | `hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA")` |
| 8 | (todos) | `/app/solicitudes-anuncio/**` | `hasAnyRole("USUARIO_AUTENTICADO_APP", "SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "USUARIO_APP")` |
| 9 | (todos) | `/reportes/**` | `authenticated()` |
| 10 | (todos) | `/usuario/**` | `hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "USUARIO_APP", "USUARIO_AUTENTICADO_APP")` |
| 11 | (todos) | `anyRequest()` | `authenticated()` — por defecto, todo lo demás requiere autenticación |

> Spring Security antepone automáticamente el prefijo `ROLE_` al comparar con `hasAnyRole(...)`. Por eso `JwtAuthenticationFilter` y `CustomUserDetailsService` construyen autoridades con el prefijo explícito `ROLE_` (ver 2.4.3 y 2.4.4). El nombre concreto del rol proviene de `usuario.getRol().getSecurityRole()`.

**Orden de los filtros** (ambos registrados *antes* de `UsernamePasswordAuthenticationFilter`):

1. `RateLimiterFilter` — limita peticiones por IP antes de cualquier procesamiento de autenticación.
2. `JwtAuthenticationFilter` — valida el JWT y establece el `SecurityContext`.

### 2.4.2 JwtService

`JwtService` encapsula la emisión y validación de tokens JWT con la librería **jjwt** (firma HMAC-SHA). Lee dos propiedades: `jwt.secret` y `jwt.expiration`.

**Fail-fast del secret (`@PostConstruct validarConfiguracion`)**: en el arranque valida la configuración y aborta si es insegura, lanzando `IllegalStateException`:

- Si `jwt.secret` es nulo o vacío.
- Si tiene menos de **32 caracteres** (requisito de longitud de clave HMAC).
- Si empieza por `dev_only_` **y** el entorno es de producción (detectado por los perfiles activos de Spring: contiene `prod`). Esto impide arrancar en producción con el secreto de desarrollo por defecto.

**`generarToken(Usuario)`** construye un JWT con:

- **subject** = correo del usuario.
- **claim `id`** = identificador del usuario.
- **claim `rol`** = `usuario.getRol().getSecurityRole()` (el rol de seguridad).
- **issuedAt** = ahora; **expiration** = ahora + `tokenExpiration`.
- Firmado con `getSigningKey()` (clave HMAC derivada del secreto en UTF-8).

```java
public String generarToken(Usuario usuario) {
    return Jwts.builder()
            .setSubject(usuario.getCorreo())                  // subject = correo
            .claim("id", usuario.getId())                     // claim id
            .claim("rol", usuario.getRol().getSecurityRole()) // claim rol
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
            .signWith(getSigningKey())                         // firma HMAC
            .compact();
}
```

Métodos de lectura: `extractUsername` (subject), `extractRol` (claim `rol`) e `isTokenValid` (comprueba que la expiración sea futura). La extracción de claims (`extractClaim`) está envuelta en *try/catch* y devuelve `null` ante cualquier token inválido o manipulado, en lugar de propagar excepciones.

El siguiente diagrama muestra la secuencia completa de un login exitoso, desde la petición del cliente hasta la emisión del token por `JwtService`.

![Figura: Secuencia de autenticación (login)](../assets/diagrams/seq-login.svg)
<p class="figure-caption">Figura: Flujo de autenticación con JWT</p>

> En el código real, `AuthServiceImpl.login` además valida que el usuario exista y que su estado sea `ACTIVO` antes de comprobar la contraseña, y `AuthController` envuelve el `AuthResponse` resultante en `ApiResponse.success(...)` con el mensaje *"Sesión iniciada correctamente"*.

### 2.4.3 JwtAuthenticationFilter

Filtro `OncePerRequestFilter` que se ejecuta una vez por petición. Su lógica:

1. Lee la cabecera `Authorization`. Si falta o no empieza por `"Bearer "`, deja pasar la petición sin autenticar (la autorización posterior decidirá si es pública o no).
2. Extrae el token (subcadena tras `"Bearer "`) y obtiene `correo` (`extractUsername`) y `rol` (`extractRol`).
3. Si `correo` y `rol` no son nulos, el token es válido (`isTokenValid`) y aún no hay autenticación en el contexto:
   - Carga los `UserDetails` con `userDetailsService.loadUserByUsername(correo)` (consulta cacheada — ver 2.4.4), lo que **revalida el estado del usuario contra la base de datos**.
   - Si el usuario está habilitado (`isEnabled()`), crea un `UsernamePasswordAuthenticationToken` con la autoridad `ROLE_<rol>` y lo fija en el `SecurityContextHolder`.
4. Cualquier excepción (usuario inexistente o deshabilitado) se registra como *warn* y limpia el contexto, dejando la petición como no autenticada.

Este doble control (firma del token + estado actual del usuario) permite que un usuario desactivado deje de autenticarse aunque su JWT siga vigente, dentro de la ventana del TTL de la caché (45 s).

### 2.4.4 CustomUserDetailsService

Implementa `UserDetailsService`. Su método `loadUserByUsername` está anotado con **`@Cacheable(value = "userDetails", key = "#correo")`**, de modo que el primer acceso consulta la base de datos (`getByCorreoOptimized`, que precarga rol y oficina) y los siguientes reutilizan el resultado cacheado durante 45 s. Construye un `UserDetails` con:

- `username` = correo; `password` = hash almacenado.
- **`disabled(!"ACTIVO".equalsIgnoreCase(estado))`**: el usuario queda habilitado únicamente si su estado es `ACTIVO`; cualquier otro estado lo marca como deshabilitado (`enabled = false`).
- Autoridad `ROLE_<securityRole>` derivada de `usuario.getRol().getSecurityRole()`.

El método **`invalidarCacheUsuario(String correo)`** está anotado con **`@CacheEvict(value = "userDetails", key = "#correo")`**. Debe invocarse cuando cambian el estado o el rol de un usuario para **invalidar inmediatamente** su entrada cacheada, sin esperar al TTL.

### 2.4.5 RateLimiterFilter

Filtro `OncePerRequestFilter` que limita la tasa de peticiones **por IP de cliente**, mitigando fuerza bruta y abuso. Características:

- Se activa según `app.security.rate-limit.enabled` (por defecto `true`); si está deshabilitado, deja pasar todo.
- **Exclusiones**: las rutas `/archivos/public/`, `/swagger` y `/v3/api-docs` se omiten por rendimiento.
- **Ventana temporal**: resolución por **minuto** (`System.currentTimeMillis() / 60000`). El contador por IP se reinicia al cambiar de minuto.
- **Almacenamiento**: `ConcurrentHashMap<String, UserRequests>` actualizado atómicamente con `compute` y un `AtomicInteger` por entrada (seguro para concurrencia).
- **Límites diferenciados**: para rutas de autenticación (`/auth/` o `/api/auth/`) el máximo es `requests-per-minute` (por defecto **10/min**); para el resto, **5×** ese valor (50/min).
- Al superar el límite responde **HTTP 429** (*Too Many Requests*) con un JSON `{"success":false, "message":"Has superado el limite de peticiones..."}`.
- **Identificación de IP** (`getClientIp`): da prioridad a la cabecera `X-Forwarded-For` (primer valor de la lista), validándola con una expresión regular de caracteres IPv4/IPv6 y longitud ≤ 45 para evitar *spoofing* por cabeceras malformadas; si no es válida, usa `request.getRemoteAddr()`.
- **Prevención de fugas de memoria**: el método `cleanupOldEntries`, anotado con `@Scheduled(fixedDelay = 600000)` (cada 10 min), elimina las entradas de minutos anteriores para que el mapa no crezca indefinidamente.

### 2.4.6 Entry points de error (401 / 403)

Dos componentes producen respuestas de error de seguridad uniformes en JSON (con `timestamp`, `status`, `error`, `message` y `path`), serializadas con `ObjectMapper`:

- **`JwtAuthenticationEntryPoint`** (`AuthenticationEntryPoint`): se invoca cuando un recurso protegido se solicita **sin autenticación válida**. Devuelve **HTTP 401 UNAUTHORIZED** con el mensaje *"Debes autenticarte para acceder a este recurso"*.
- **`JwtAccessDeniedHandler`** (`AccessDeniedHandler`): se invoca cuando un usuario **autenticado** intenta acceder a un recurso para el que **no tiene permisos**. Devuelve **HTTP 403 FORBIDDEN** con el mensaje *"No tienes permisos para acceder a este recurso"*.

Ambos se registran en `SecurityConfig` dentro de `exceptionHandling`, garantizando que los errores de autenticación y autorización tengan el mismo formato que el resto de la API.
