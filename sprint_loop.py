"""
sprint_loop.py — Loop horário autônomo do SPRINT.

Executa a cada 1h via Windows Task Scheduler:
  1. Lê .sprint/session.md (estado atual)
  2. Claude (Opus) avalia o que foi feito e o que fazer agora
  3. Gera o próximo bloco de tarefas e atualiza session.md
  4. Envia resumo via WhatsApp (CallMeBot) ou Telegram

Configuração inicial (uma única vez):
  1. Copie .env.example → .env e preencha as variáveis
  2. Rode: python sprint_loop.py --setup   para testar o envio
"""

import os
import sys
import json
import argparse
from pathlib import Path
from datetime import datetime

# ── Dependências ──────────────────────────────────────────────────────────────
try:
    import anthropic
    import requests
    from dotenv import load_dotenv
except ImportError:
    print("Instalando dependências...")
    import subprocess
    subprocess.run([sys.executable, "-m", "pip", "install",
                    "anthropic", "requests", "python-dotenv", "-q"], check=True)
    import anthropic
    import requests
    from dotenv import load_dotenv

# ── Configuração ──────────────────────────────────────────────────────────────

PROJETO_DIR   = Path(__file__).parent
SESSION_FILE  = PROJETO_DIR / ".sprint" / "session.md"
LOG_FILE      = PROJETO_DIR / ".sprint" / "loop.log"
ENV_FILE      = PROJETO_DIR / ".env"

load_dotenv(ENV_FILE)

ANTHROPIC_KEY = os.getenv("ANTHROPIC_API_KEY", "")
WA_PHONE      = os.getenv("WA_PHONE", "")          # Ex: 5511999999999
WA_APIKEY     = os.getenv("WA_APIKEY", "")          # Chave do CallMeBot
TG_TOKEN      = os.getenv("TG_BOT_TOKEN", "")       # Opcional: Telegram
TG_CHAT_ID    = os.getenv("TG_CHAT_ID", "")         # Opcional: Telegram

# ── Log ───────────────────────────────────────────────────────────────────────

def log(msg: str):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    linha = f"[{timestamp}] {msg}"
    print(linha)
    LOG_FILE.parent.mkdir(parents=True, exist_ok=True)
    with LOG_FILE.open("a", encoding="utf-8") as f:
        f.write(linha + "\n")

# ── Notificações ──────────────────────────────────────────────────────────────

def enviar_whatsapp(mensagem: str) -> bool:
    """Envia via CallMeBot (gratuito). Limite: ~15 msg/hora."""
    if not WA_PHONE or not WA_APIKEY:
        log("⚠️  WA_PHONE ou WA_APIKEY não configurado — pulando WhatsApp")
        return False
    try:
        r = requests.get(
            "https://api.callmebot.com/whatsapp.php",
            params={"phone": WA_PHONE, "text": mensagem, "apikey": WA_APIKEY},
            timeout=15,
        )
        ok = r.status_code == 200
        log(f"WhatsApp {'✅' if ok else '❌'} status={r.status_code}")
        return ok
    except Exception as e:
        log(f"❌ WhatsApp erro: {e}")
        return False


def enviar_telegram(mensagem: str) -> bool:
    """Envia via Telegram Bot (alternativa gratuita e ilimitada)."""
    if not TG_TOKEN or not TG_CHAT_ID:
        return False
    try:
        r = requests.post(
            f"https://api.telegram.org/bot{TG_TOKEN}/sendMessage",
            json={"chat_id": TG_CHAT_ID, "text": mensagem, "parse_mode": "Markdown"},
            timeout=15,
        )
        ok = r.status_code == 200
        log(f"Telegram {'✅' if ok else '❌'} status={r.status_code}")
        return ok
    except Exception as e:
        log(f"❌ Telegram erro: {e}")
        return False


def notificar(mensagem: str):
    """Tenta WhatsApp; fallback para Telegram."""
    if not enviar_whatsapp(mensagem):
        enviar_telegram(mensagem)

# ── Estado ────────────────────────────────────────────────────────────────────

def ler_estado() -> str:
    if SESSION_FILE.exists():
        return SESSION_FILE.read_text(encoding="utf-8")
    return "Nenhuma sessão anterior encontrada."


def salvar_estado(conteudo: str):
    SESSION_FILE.parent.mkdir(parents=True, exist_ok=True)
    SESSION_FILE.write_text(conteudo, encoding="utf-8")

# ── Avaliação Claude ──────────────────────────────────────────────────────────

SYSTEM_AVALIADOR = """\
Você é o Orquestrador do projeto SPRINT (app Android de treino matemático).

Sua função neste ciclo horário:
1. Ler o estado atual do projeto em .sprint/session.md
2. Avaliar o que foi concluído desde o último ciclo
3. Identificar as próximas 2-3 tarefas prioritárias
4. Atualizar o session.md com progresso e próximas tarefas
5. Produzir um resumo curto (máx 300 chars) para WhatsApp

Responda estritamente com JSON:
{
  "session_md_atualizado": "conteúdo completo do session.md atualizado",
  "resumo_whatsapp": "mensagem curta para o usuário — max 300 chars, sem markdown"
}
"""


def avaliar_com_claude(estado_atual: str) -> tuple[str, str]:
    """Retorna (session_md_novo, resumo_whatsapp)."""
    if not ANTHROPIC_KEY:
        raise EnvironmentError("ANTHROPIC_API_KEY não definida no .env")

    cliente = anthropic.Anthropic(api_key=ANTHROPIC_KEY)
    hora = datetime.now().strftime("%H:%M")

    resposta = cliente.messages.create(
        model="claude-opus-4-5",
        max_tokens=4096,
        system=SYSTEM_AVALIADOR,
        messages=[{
            "role": "user",
            "content": (
                f"Ciclo das {hora}. Estado atual do projeto:\n\n"
                f"```\n{estado_atual}\n```\n\n"
                "Avalie, atualize o session.md e produza o resumo para WhatsApp."
            ),
        }],
    )

    raw = resposta.content[0].text
    try:
        inicio = raw.find("{")
        fim = raw.rfind("}") + 1
        dados = json.loads(raw[inicio:fim])
        return dados["session_md_atualizado"], dados["resumo_whatsapp"]
    except Exception:
        # Fallback: usa resposta bruta como resumo
        return estado_atual, raw[:300]

# ── Setup interativo ──────────────────────────────────────────────────────────

def setup():
    """Guia de configuração inicial."""
    print("\n🔧 SPRINT Loop — Configuração inicial\n")

    if not ENV_FILE.exists():
        ENV_FILE.write_text(
            "ANTHROPIC_API_KEY=sk-ant-...\n"
            "WA_PHONE=5511999999999\n"
            "WA_APIKEY=\n"
            "TG_BOT_TOKEN=\n"
            "TG_CHAT_ID=\n",
            encoding="utf-8",
        )
        print(f"✅ Arquivo .env criado em: {ENV_FILE}")
        print("   Preencha as variáveis e rode novamente.\n")

    print("📱 Para ativar WhatsApp (CallMeBot — gratuito):")
    print("   1. Adicione o número +34 644 54 42 09 nos seus contatos")
    print("   2. Mande: 'I allow callmebot to send me messages'")
    print("   3. Você receberá a sua apikey por WhatsApp")
    print("   4. Cole essa apikey em WA_APIKEY no .env\n")

    print("📱 Alternativa mais simples: Telegram Bot")
    print("   1. Fale com @BotFather no Telegram → /newbot")
    print("   2. Copie o token → TG_BOT_TOKEN no .env")
    print("   3. Abra o bot, mande qualquer mensagem")
    print("   4. Acesse: https://api.telegram.org/bot<TOKEN>/getUpdates")
    print("   5. Copie o chat.id → TG_CHAT_ID no .env\n")

    print("▶️  Para testar o envio:")
    print("   python sprint_loop.py --test-notify\n")

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="SPRINT Loop Horário")
    parser.add_argument("--setup",        action="store_true", help="Guia de configuração")
    parser.add_argument("--test-notify",  action="store_true", help="Testa envio de mensagem")
    args = parser.parse_args()

    if args.setup:
        setup()
        return

    if args.test_notify:
        hora = datetime.now().strftime("%H:%M")
        notificar(f"[SPRINT] Teste de notificação — {hora} ✅\nTudo certo! O loop horário está funcionando.")
        return

    # ── Ciclo normal ─────────────────────────────────────────────────────────
    log("=" * 50)
    log("🔄 Iniciando ciclo horário SPRINT")

    try:
        estado = ler_estado()
        log(f"Estado lido ({len(estado)} chars)")

        novo_estado, resumo = avaliar_com_claude(estado)
        log("Claude avaliou o estado")

        salvar_estado(novo_estado)
        log("session.md atualizado")

        hora = datetime.now().strftime("%H:%M")
        mensagem = f"[SPRINT {hora}]\n{resumo}"
        notificar(mensagem)

    except Exception as e:
        log(f"❌ Erro no ciclo: {e}")
        notificar(f"[SPRINT ⚠️] Erro no ciclo das {datetime.now().strftime('%H:%M')}: {str(e)[:150]}")

    log("✅ Ciclo concluído")


if __name__ == "__main__":
    main()
