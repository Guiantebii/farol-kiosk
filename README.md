# Farol Kiosk

**Farol Kiosk** é um launcher Android (Kotlin) para **TV Box** em modo quiosque: substitui a tela inicial, exibe um painel web em tela cheia e protege o acesso às configurações com **PIN opcional**. Projeto legado no contexto acadêmico da **FATEC**.

| | |
|---|---|
| **Versão** | 1.0 (`versionCode` 1) |
| **Package** | `com.kiosk.launcher` |
| **Min. Android** | 7.0 (API 24) |
| **Orientação** | Paisagem (landscape) |

---

## Índice

1. [Para quem é este projeto](#para-quem-é-este-projeto)
2. [Funcionalidades](#funcionalidades)
3. [Requisitos](#requisitos)
4. [Guia rápido (usuário final)](#guia-rápido-usuário-final)
5. [Desenvolvimento (Android Studio)](#desenvolvimento-android-studio)
6. [Instalar na TV Box (ADB)](#instalar-na-tv-box-adb)
7. [Configuração inicial](#configuração-inicial)
8. [Definir launcher padrão](#definir-launcher-padrão)
9. [Segurança e PIN](#segurança-e-pin)
10. [Depuração (Logcat)](#depuração-logcat)
11. [Painel web (Netlify / FATEC)](#painel-web-netlify--fatec)
12. [Build e release](#build-e-release)
13. [Publicar no GitHub](#publicar-no-github)
14. [Estrutura do código](#estrutura-do-código)
15. [Solução de problemas](#solução-de-problemas)

---

## Para quem é este projeto

- **Operador / instituição**: instalar APK, configurar URL do painel e deixar a TV Box fixa no conteúdo.
- **Desenvolvedor**: clonar, compilar, depurar WebView e evoluir o app.
- **Equipe acadêmica (FATEC)**: base versionada para quiosques e painéis digitais.

---

## Funcionalidades

- Launcher **HOME** + entrada **LAUNCHER** (útil em desenvolvimento)
- **WebView** fullscreen, modo imersivo, links abertos dentro do app
- **SharedPreferences**: `kiosk_url`, `kiosk_pin`
- **Settings**: URL, PIN, *Salvar e Iniciar Kiosk*, *Abrir Configurações do Android*, *Mudar Launcher Padrão*
- **PIN opcional** com diálogo de aviso se iniciar sem PIN
- **Gatilhos** para abrir configurações (com PIN, se configurado)
- **Performance TV Box**: `largeHeap`, GPU, logs de rede/JS (`KioskWebView`)

---

## Requisitos

### Hardware

- Android TV Box (ou emulador) com Android 7.0+

### Software (desenvolvimento)

- [Android Studio](https://developer.android.com/studio) (Ladybug ou mais recente recomendado)
- JDK 17 (incluso no Android Studio)
- [Platform Tools](https://developer.android.com/tools/releases/platform-tools) (`adb`) para instalar na box

### Rede

- Internet na TV Box para carregar o painel (permissão `INTERNET` já declarada)
- URLs **HTTP** permitidas via `usesCleartextTraffic` (ajuste para produção se usar só HTTPS)

---

## Guia rápido (usuário final)

1. Instale o APK na TV Box (veja [Instalar na TV Box](#instalar-na-tv-box-adb)).
2. Abra **Farol Kiosk**.
3. Informe a **URL** do painel e, se quiser, um **PIN** (mín. 4 dígitos).
4. Toque em **Salvar e Iniciar Kiosk**.
5. Quando o Android perguntar, escolha **Farol Kiosk** como **Tela inicial** → **Sempre**  
   (ou use **Mudar Launcher Padrão** nas configurações do app).

---

## Desenvolvimento (Android Studio)

### 1. Clonar

```bash
git clone https://github.com/SEU_USUARIO/farol-kiosk.git
cd farol-kiosk
```

Substitua a URL pelo repositório real após criar no GitHub.

### 2. Abrir o projeto

1. Android Studio → **File → Open**
2. Selecione a pasta raiz do projeto (onde está `settings.gradle.kts`)
3. Aguarde **Gradle Sync** (primeira vez pode baixar dependências)

> Não commite `local.properties` (caminho do SDK); ele já está no `.gitignore`.

### 3. Executar

- Conecte TV Box via USB (depuração USB) ou use emulador **TV / Tablet landscape**
- **Run** ▶ ou:

```bash
./gradlew assembleDebug
```

APK gerado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 4. Limpar build (opcional)

```bash
./gradlew clean assembleDebug
```

---

## Instalar na TV Box (ADB)

### USB

1. Ative **Opções do desenvolvedor** e **Depuração USB** na box.
2. Conecte ao PC e confira:

```bash
adb devices
```

3. Instale ou atualize:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Wi-Fi (sem cabo)

Na box (com USB uma vez, ou se já tiver depuração Wi-Fi):

```bash
adb tcpip 5555
adb connect IP_DA_BOX:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuração inicial

| Campo | Descrição |
|--------|-----------|
| **URL do quiosque** | Endereço completo (`https://...`) do painel web |
| **PIN** | Opcional; vazio = aviso de segurança antes de iniciar |

Valores gravados localmente (`kiosk_prefs`). Exemplo de URL institucional no helper da tela: `https://www.fatecsp.br/`.

### Sem URL na primeira execução

Se não houver URL salva, o app abre **Configurações** automaticamente.

### Alterar URL ou PIN depois

Use os [gatilhos de administrador](#segurança-e-pin) → digite o PIN (se houver) → **Configurações**.

---

## Definir launcher padrão

### Pelo app

**Configurações** → **Mudar Launcher Padrão** (abre `ACTION_HOME_SETTINGS`; fallback para configurações gerais em boxes customizadas).

### Pelo ADB

```bash
adb shell cmd package set-home-activity com.kiosk.launcher/.MainActivity
```

### Voltar ao launcher original

Use **Mudar Launcher Padrão** ou **Configurações → Apps → Launcher padrão** e escolha outro app.

---

## Segurança e PIN

### Com PIN (recomendado em produção)

Mínimo **4 dígitos**. Para acessar **Configurações** a partir do kiosk:

| Gatilho | Ação |
|--------|------|
| **Swipe** | Arraste rápido da **borda esquerda** para a **direita** no WebView |
| **OK / Enter** | **7** pressões em **2 segundos** |
| **Home** | **7** reaberturas do launcher em **2 segundos** (`onNewIntent`) |
| **Voltar** | **7** pressões em **2 segundos** |

Digite o PIN correto no diálogo → abre **Configurações**.

### Sem PIN

Os gatilhos abrem **Configurações** diretamente. Use só em ambiente controlado.

Ao salvar com PIN vazio, o app exibe um **AlertDialog** de confirmação antes de iniciar o kiosk.

### Limitações do Android

Tecla **Home** do sistema não é interceptável como tecla normal; o app usa reabertura do launcher como contagem. Bloqueio total (impossível sair) exige **Device Owner / Lock Task Mode** ou MDM — fora do escopo desta v1.0.

---

## Depuração (Logcat)

1. Box conectada: `adb devices`
2. Android Studio → **Logcat**
3. Filtro:

```text
tag:KioskWebView
```

| Log | Significado |
|-----|-------------|
| `Erro de carregamento:` | Falha de rede (ex.: OOM, conexão abortada) |
| `Erro HTTP:` | Status HTTP (404, 503, …) |
| Console JS | Erros do painel web |

Se **não** houver erros e imagens/conteúdo pararem na TV Box, otimize o **frontend** (tamanho de imagens, lazy load).

---

## Painel web (Netlify / FATEC)

Recomendações para boxes com pouca RAM:

- Imagens até **1920×1080**, preferir **WebP**
- Evitar carregar muitas imagens pesadas de uma vez; usar **preload sequencial**
- Testar no Logcat ao reproduzir o problema

---

## Build e release

### Debug (testes)

```bash
./gradlew assembleDebug
```

### Release (distribuição)

1. Crie um keystore (uma vez):

```bash
keytool -genkey -v -keystore farol-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias farol
```

2. Configure assinatura em `app/build.gradle.kts` (não commite o `.jks` — já ignorado no `.gitignore`).
3. Gere:

```bash
./gradlew assembleRelease
```

Para a **v1.0** acadêmica, o APK **debug** costuma ser suficiente; para produção institucional, prefira **release** assinado.

---

## Publicar no GitHub

### Checklist antes do push

- [ ] `./gradlew assembleDebug` sem erros
- [ ] Sem `local.properties`, `.idea/` ou `build/` no commit
- [ ] README com URL do repositório atualizada
- [ ] (Opcional) Screenshots em `docs/screenshots/` para enriquecer o README

### Primeiro upload

```bash
cd farol-kiosk
git init
git add .
git status
git commit -m "Release inicial: Farol Kiosk v1.0"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/farol-kiosk.git
git push -u origin main
```

### Release v1.0.0 (recomendado)

No GitHub: **Releases → Draft a new release**

- **Tag:** `v1.0.0`
- **Title:** Farol Kiosk 1.0.0 — primeira versão
- **Description:** copie de [CHANGELOG.md](CHANGELOG.md)
- **Anexo:** `app-debug.apk` (ou APK release assinado)

### Deixar o repositório “apresentável”

- **About** (lado direito): descrição curta + topics: `android`, `kotlin`, `kiosk`, `android-tv`, `webview`, `fatec`
- **Website:** URL do painel demo, se houver
- Adicione 2–3 screenshots (configurações + kiosk) na pasta `docs/screenshots/` e referencie no README:

```markdown
![Tela de configurações](docs/screenshots/settings.png)
```

---

## Estrutura do código

```text
app/src/main/java/com/kiosk/launcher/
├── MainActivity.kt          # Kiosk, WebView, gatilhos, logs
├── SettingsActivity.kt      # Configurações e alertas
├── KioskPreferences.kt      # URL e PIN
├── PinUnlockDialog.kt       # Modal de PIN
└── RapidPressCounter.kt     # Contagem 7× / 2s

app/src/main/res/
├── layout/                  # activity_main, activity_settings, dialog_pin
├── drawable-nodpi/          # brand_logo, ícones adaptativos
└── mipmap-*/                # ic_launcher (WebP)
```

Histórico de versões: [CHANGELOG.md](CHANGELOG.md).

---

## Solução de problemas

| Problema | O que fazer |
|----------|-------------|
| App não vira launcher | **Mudar Launcher Padrão** ou ADB `set-home-activity` |
| Tela branca / sem rede | Confirme URL, Wi-Fi da box, Logcat `KioskWebView` |
| Só parte das imagens carrega | Logcat + otimizar imagens/JS no site |
| Esqueci o PIN | Limpar dados do app: **Configurações Android → Apps → Farol Kiosk → Armazenamento → Limpar dados** (reconfigura URL/PIN) |
| Gradle sync falha | JDK 17, Android SDK 35, internet para dependências |

---

**FATEC** — Farol Kiosk, apoio a painéis digitais e experiências em TV Box.

Uso educacional e operacional em quiosques; avalie a política de segurança da sua unidade antes de implantar **sem PIN** em área pública.
