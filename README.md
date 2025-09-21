# HA Notifier (Android)

APP Android em **tema escuro azul**, para **notificações e alertas** integrados com **Home Assistant** sem MQTT. Suporta:

- Notificações **normais** e **persistentes**
- **Popup (full-screen intent)** para alertas críticos (mesmo com ecrã bloqueado)
- Funcionamento **LAN** (WebSocket/eventos) e **Externo** (REST via Nabu Casa/reverse proxy)
- Configuração total na **página de definições** (ícone ⚙️)
- Distribuição **sem Play Store** (APK sideload)

## Instalação (APK sideload)
1. Compila: `gradle assembleRelease`
2. Assina com o teu `keystore.jks` (ver `gradle.properties`).
3. Instala no dispositivo: `adb install -r app/build/outputs/apk/release/app-release.apk`.
4. Na primeira execução, aceita a permissão de **Notificações**.

> ℹ️ **Nota:** O repositório não inclui o Gradle Wrapper por restrições da plataforma. Instala o Gradle 8.9+ localmente (ou gera o wrapper na tua máquina) antes de correr os comandos acima.

## Configuração na APP
1. ⚙️ Abre **Definições**.
2. Preenche:
   - **URL LAN**: `http://ha.local:8123` (opcional)
   - **URL Externa**: URL Nabu Casa ou reverse proxy com HTTPS (opcional)
   - **Token** (Long-Lived Access Token) se quiseres executar ações no HA ou usar WebSocket.
3. Em **Notificações**, escolhe se queres **popup** para crítico e se deve ser **persistente**.
4. Usa a página **Início** para testar uma notificação e um alerta popup.

## Integração com Home Assistant

### Opção A: REST (externo)
1. No HA, adiciona `rest_command` do ficheiro `exemplos-homeassistant/rest_command.yaml`.
2. Usa as automações de exemplo para enviar payloads de notificação para o endpoint da tua app/relay.

### Opção B: Eventos (LAN/WebSocket)
1. A app liga-se ao `wss://<HA>/api/websocket` com o **Long-Lived Token**.
2. Dispara um evento no HA com `event_type: app_notify` contendo o JSON do payload (ver `Payload`).

### Payload suportado
```json
{
  "title":"Porta da Garagem",
  "body":"Aberta há 10 minutos.",
  "priority":"info|warning|critical",
  "persistent":true,
  "popup":true,
  "requireAck":true
}
```

## Segurança
- Guarda o token com **DataStore** (podes migrar para `EncryptedSharedPreferences`).
- Recomenda‑se HTTPS para URLs externas (Nabu Casa/Let's Encrypt).

## Roadmap
- Botões de **ações** na notificação/popup (executar serviços HA via REST).
- Histórico visual e filtros.
- Suporte opcional a FCM (push) como terceiro modo.

## Licença
MIT


## Subscrição WebSocket automática
- Ativa em ⚙️ **Definições → WebSocket (LAN/Externo)**.
- A app conecta ao `.../api/websocket` do URL configurado (preferindo LAN se selecionado), autentica com o teu Long‑Lived Token e subscreve `event_type: app_notify`.
- Quando o HA dispara `app_notify`, a app converte o `event.data` em `Payload` e mostra a notificação/alerta automaticamente.


## Templates (per-notificação)
- Define modelos em **Templates** (ícone ➕ no topo).
- Em automações, envia `templateId` **ou** `templateName` no payload para aplicar as preferências desse modelo (prioridade, persistente, popup, requireAck).
- Campos enviados no payload **sobrepõem** o que estiver no template; se o campo vier omisso, herda do template.

Exemplo de payload usando `templateName`:
```json
{
  "title": "Luz da sala ligada",
  "body": "Há mais de 2 horas.",
  "templateName": "Avisos de Energia"
}
```
