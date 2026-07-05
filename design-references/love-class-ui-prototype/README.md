# Love Class UI Prototype Reference

Entrega externa recebida em 2026-05-24.

Este pacote é um protótipo web/React estático para orientar o visual do app Android. Ele não é código de produção Android.

## Conteúdo

- `prototype.html`: protótipo navegável em browser.
- `tokens.css`: tokens visuais para white/dark focus.
- `components/folha-safe.jsx`: variação conservadora da FolhaScreen.
- `components/folha-bold.jsx`: variação editorial/mais expressiva da FolhaScreen.
- `components/result-safe.jsx`: variação conservadora da PageResultScreen.
- `components/result-bold.jsx`: variação editorial/mais expressiva da PageResultScreen.
- `screenshots/`: capturas de referência.

## Decisão de Uso

Portar primeiro a linha `safe`, porque ela é mais adequada ao MVP Android:

- Menos arriscada para tablet/stylus.
- Mais próxima do `APP_LAYOUT_SPEC.md`.
- Melhor para preservar área de escrita e legibilidade.

A linha `bold` deve ser tratada como refinamento futuro ou modo visual alternativo.

## Status de Port

Já portado parcialmente para Compose:

- Top training bar mais densa.
- Header do campo com número, skill label e enunciado.
- Área de tinta separada.
- Linha de resposta final.
- Result rows mais próximos da variação safe.

Ainda falta:

- Captura real de stylus.
- Export de crop por campo.
- QA visual em Android Studio.
- Build Android real com Gradle wrapper.
