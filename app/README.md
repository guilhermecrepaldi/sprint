# Android App Scaffold

This is the first Jetpack Compose scaffold for the Strava da Matemática Android app.

## Current State

- Gradle/Android module structure is present.
- Compose theme follows `APP_LAYOUT_SPEC.md` colors, typography, and spacing.
- Data models mirror the backend API contracts.
- Retrofit interface is defined for:
  - `GET /api/health`
  - `POST /api/session/start`
  - `POST /api/session/{sessionId}/submit`
- A demo `SessionViewModel` drives the visual flow without requiring the backend:
  - config
  - folha
  - page result
  - session summary
- Ink UI is currently visual-only. Real stylus capture/crop generation is the next major step.

## Validate When Android Tooling Is Available

From the repository root:

```powershell
.\gradlew.bat :app:assembleDebug
```

This workspace currently does not include a Gradle wrapper and `gradle` was not available in PATH during scaffold creation, so local Android compilation was not executed yet.

## Next Steps

1. Open the project in Android Studio.
2. Let Android Studio create/download the Gradle wrapper if needed.
3. Run `:app:assembleDebug`.
4. Fix any Android dependency/version issues from the first real compile.
5. Replace demo ViewModel actions with `ApiClient`.
6. Implement real stylus capture in `InkCanvas`.
7. Implement per-field bitmap crop export for submit payloads.
