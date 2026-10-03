# Backend Access Alignment — Context

Decisões capturadas na conversa (substituem discuss interativo para este feature).

| Área | Decisão | Fonte |
| ---- | ------- | ----- |
| Ativação de conta | Link no e-mail; conta inativa até verificar; **depois** login | Usuário — opção 1 |
| Papéis | ADMIN = config/governança; OPERATOR = merchant, fee rules, fluxo operacional completo | Usuário |
| Auto-grant | Sim, na criação de merchant | Usuário |
| Owner por merchant | Adiar | Usuário |
| Frontend | Fora deste feature; CORS incluído como preparação (P2) | Conversa |
| E-mail | **Resend** para confirmação e demais e-mails transacionais | Usuário 2026-10-02 |
| Código | Não alterar até aprovar spec/tasks | Usuário |

**Agent discretion (defaults locked para spec):**

- Renomear enum `FINANCIAL_ANALYST` → `OPERATOR` (migration de dados + seeds + testes).
- ADMIN mantém bypass em `MerchantAccessGuard`; OPERATOR exige grant (incl. auto-grant).
- Endpoints de contexto: `GET /api/me`, `GET /api/me/merchants` (lista enxuta para UI).
- Dev: Mailhog ou log-only mail sender; prod: SMTP via variáveis de ambiente.
- Remover ou deprecar `PATCH .../activation` para fluxo de auto-registro; manter criação de usuário ativo por ADMIN via `POST /api/users`.
