# Strava da Matematica AI Studio Reference

This folder is an external AI delivery kept as a reference artifact.

## Use As Reference

- React/Vite interaction model for session setup, folha flow, pause overlay, result screen and visual rhythm.
- `src/components/InkCanvas.tsx` for canvas behavior ideas:
  - pointer-based strokes;
  - pressure/tilt capture when available;
  - undo/redo stacks;
  - clear command;
  - base64 export on a solid background.
- `src/components/ThermometerView.tsx` and `src/components/PauseSheet.tsx` for UI copy and density cues.

## Do Not Treat As Canonical

- `server.ts` is an alternate Express/Gemini mock backend and should not replace the canonical FastAPI/Postgres backend.
- API payloads diverge from the current backend contract.
- Config enum names diverge:
  - external: `free`, `time`, `linear`;
  - canonical: `unlimited`, `timed`, `arithmetic`.
- Field indexing diverges:
  - external UI uses 1-based field numbers;
  - canonical API uses 0-based `field_index`.

## Canonical Sources

- `SUPER_SPEC.md`
- `HANDOFF_CODEX.md`
- `backend/app/api/routes.py`
- `app/src/main/java/com/strava_matematica/model/ApiModels.kt`
