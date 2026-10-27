# Coinly versión final

Proyecto Android con Firebase Authentication y Cloud Firestore. El paquete Android
se mantiene como `com.example.coinly` y usa el `google-services.json` proporcionado.

## Funciones corregidas

- Registro e inicio de sesión reales por correo y contraseña.
- Recuperación por correo y cambio de contraseña con reautenticación.
- Cambio de correo con enlace de verificación de Firebase.
- Recordar sesión opcional. Sin marcarlo, el siguiente inicio de proceso requiere
  volver a entrar. Cerrar sesión limpia las instantáneas y el historial de chat.
- Movimientos y presupuestos bajo `usuarios/{uid}`; perfil en ese documento.
- Búsqueda por descripción, categoría y etiquetas, con o sin `#`.
- Presupuesto mensual: `period` usa `YYYY-MM`; lo gastado se calcula con los gastos
  de la misma categoría y mes. No se guarda un segundo saldo que pueda desajustarse.
- Usuarios nuevos sin movimientos ni presupuestos de ejemplo.
- Guardado confirmado por el servidor, carga, errores, reintento y aviso sin conexión.
- Contenido con ancho limitado para tablet; formularios desplazables; texto escalable;
  navegación con etiquetas de dos líneas; rotación sin perder formularios ni hojas.
- Textos de interfaz en `app/src/main/res/values/strings.xml`.
- Backup de datos deshabilitado, permiso de Internet e ícono Coinly real.

## Configuración en Firebase

1. Habilita **Authentication > Proveedores > Correo electrónico/contraseña**.
2. Crea o usa una base **Cloud Firestore Standard**, con ID `(default)`.
3. Publica `firestore.rules`. No dejes reglas abiertas de prueba.

Si se publicó desde esta sesión, el resultado se registra en `VERIFICACION.md`.
Para volver a desplegar desde la raíz del proyecto, con Node.js 22 o superior:

```powershell
npm install
npm run firebase:login
npm run deploy:rules
```

El proyecto de destino ya se identifica en `.firebaserc`. La configuración Android
no habilita automáticamente los proveedores de acceso ni crea la base de datos.
Las cuentas locales del prototipo no eran cuentas Firebase: deben registrarse.
No se importan automáticamente ejemplos antiguos ni datos locales a un usuario nuevo.

## Compilar

Abre esta carpeta en Android Studio. Usa el JDK incluido en Android Studio
(JDK 21; el mínimo del plugin es JDK 17), Gradle 9.3.1, AGP 9.1.1 y SDK 36.1.
Android Studio puede regenerar `local.properties` si cambia la ubicación del SDK.
Authentication usa 24.2.0, compatible con Kotlin incorporado en este AGP. Se retiraron
las dependencias de Google ID que no utilizaba la interfaz y exigían Kotlin 2.4.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease bundleRelease lintRelease
```

El APK release y el AAB se generan en `app/build/outputs`. La variante release
siempre conecta al Firebase real, nunca al emulador, y no es depurable.

## Firma de release

`signing/coinly-release.p12` y `signing.properties` permiten recompilar y firmar
futuras versiones con la misma identidad. **Conserva ambos de forma privada y haz
una copia de seguridad. No los publiques ni los subas a Git.** Están excluidos por
`.gitignore`. La contraseña no se incluye dentro del APK.

Se creó una firma nueva para esta entrega. Android no permite actualizar una app
firmada con otra clave, como un APK debug anterior, instalando encima este release.
Antes de desinstalar una versión antigua, conserva cualquier dato local que necesites.
Los datos del prototipo local no se migran automáticamente a Firestore.

Publicar en Google Play requiere completar la ficha, políticas, pruebas y formularios
de la cuenta de desarrollador; generar el AAB no publica la aplicación.

## Pruebas

Pruebas de reglas contra Firebase Emulator Suite, sin tocar datos reales:

```powershell
npm install
npm test
```

Para ejecutar el recorrido Android de integración, inicia antes los emuladores:

```powershell
npm run emulators
```

En otra terminal, con un emulador Android abierto:

```powershell
.\gradlew.bat connectedDebugAndroidTest -PfirebaseEmulator=true "-Pandroid.testInstrumentationRunnerArguments.class=com.example.coinly.FirebaseFlowTest"
```

Los puertos locales son Auth 9199 y Firestore 8180. Android accede por `10.0.2.2`.
El test se niega a ejecutar si la compilación no tiene `FIREBASE_EMULATOR=true`.
Las pruebas crean cuentas ficticias y borran únicamente documentos del emulador.
El APK de pruebas no debe distribuirse: usa el APK release entregado.

## Alcance y decisiones

- Firestore conserva instantáneas en memoria durante la sesión. Sin conexión se
  informa que los datos pueden estar incompletos; no se afirma un guardado remoto
  antes de que Firestore lo confirme. Las escrituras requieren conexión de red.
- Las consultas se filtran en la instantánea de movimientos del usuario. No se
  leen movimientos de otras cuentas. Para volúmenes muy grandes convendrá paginar.
- La foto seleccionada y las preferencias de apariencia se guardan localmente,
  separadas por UID. Los campos del perfil sí se sincronizan con Firestore.
- Se conserva el asistente financiero local. No se distribuye una clave privada
  de Gemini incrustada en el APK; integrar Gemini en producción requiere un backend
  o un servicio de IA configurado y protegido.
- No se recibió un documento previo con capturas. El documento final reúne las
  capturas reales generadas al probar esta versión, identificadas como pruebas.

## Referencias técnicas

- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/docs/auth/android/manage-users
- https://firebase.google.com/docs/firestore/security/rules-conditions
- https://developer.android.com/build/releases/agp-9-1-0-release-notes
