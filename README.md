# Farol Kiosk

**Farol Kiosk** é um launcher Android nativo em Kotlin para **TV Box** e dispositivos de sinalização digital em modo quiosque: substitui a tela inicial, exibe um painel web em tela cheia e protege o acesso às configurações com **PIN opcional**.

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
11. [Painel web (Netlify)](#painel-web-netlify)
12. [Build e release](#build-e-release)
13. [Publicar no GitHub](#publicar-no-github)
14. [Estrutura do código](#estrutura-do-código)
15. [Solução de problemas](#solução-de-problemas)

---

## Para quem é este projeto

- **Operador / administrador**: instalar APK, configurar URL do painel e fixar a TV Box no conteúdo de exibição.
- **Desenvolvedor**: clonar, compilar, depurar WebView e evoluir o app.
- **Equipe técnica**: base versionada e limpa para implantação de quiosques e painéis digitais.

---

## Funcionalidades

- Launcher **HOME** + entrada padrão de aplicativo (útil em desenvolvimento)
- **WebView** fullscreen, modo imersivo, links internos mantidos no app
- **SharedPreferences**: persistência local de `kiosk_url` e `kiosk_pin`
- **Settings**: painel de configuração com identidade visual, gerenciamento de URL/PIN, botão de atalho para configurações nativas do Android e seletor de launcher padrão
- **PIN flexível com Alerta de Segurança**: opcional (vazio por padrão), exibindo um diálogo de confirmação caso o técnico tente iniciar o modo kiosk sem proteção por senha
- **Gatilhos de administrador**: múltiplos métodos para retornar às configurações protegidas por PIN
- **Performance para TV Box**: suporte a `largeHeap`, aceleração de hardware e logs dedicados de rede/JS (`KioskWebView`)

---

## Requisitos

### Hardware

- Android TV Box (ou emulador) com Android 7.0+

### Software (desenvolvimento)

- [Android Studio](https://developer.android.com/studio) (Ladybug ou mais recente recomendado)
- JDK 17 (incluso no Android Studio)
- [Platform Tools](https://developer.android.com/tools/releases/platform-tools) (`adb`) para instalar na box

### Rede

- Conexão de rede ativa na TV Box para carregar o painel web (permissão `INTERNET` declarada)
- Suporte a tráfego HTTP via `usesCleartextTraffic` (ajuste para HTTPS em produção)

---

## Guia rápido (usuário final)

1. Instale o APK na TV Box (veja [Instalar na TV Box](#instalar-na-tv-box-adb)).
2. Abra o aplicativo **Farol Kiosk**.
3. Insira a **URL** do painel web e, caso deseje proteger o acesso, defina um **PIN** (mín. 4 dígitos). Deixe em branco se preferir sem senha.
4. Toque em **Salvar e Iniciar Kiosk**.
5. Quando o sistema Android perguntar, selecione **Farol Kiosk** como **Tela inicial** → **Sempre** (ou utilize o botão de **Mudar Launcher Padrão** nas configurações do app).

---

## Desenvolvimento (Android Studio)

### 1. Clonar

```bash
git clone https://github.com/SEU_USUARIO/farol-kiosk.git
cd farol-kiosk
```

Substitua a URL pelo link real do seu repositório no GitHub.

### 2. Abrir o projeto

1. Android Studio → **File → Open**
2. Selecione a pasta raiz do projeto (onde está o arquivo `settings.gradle.kts`)
3. Aguarde a sincronização do **Gradle**

> *Nota:* O arquivo `local.properties` (que contém caminhos locais do SDK) é ignorado automaticamente pelo `.gitignore`.

### 3. Executar

- Conecte a TV Box via ADB ou utilize um emulador configurado em orientação paisagem (Landscape).
- Execute o projeto pelo ícone **Run** ▶ ou via terminal:

```bash
./gradlew assembleDebug
```

O APK de debug será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 4. Limpar build (opcional)

```bash
./gradlew clean assembleDebug
```

---

## Instalar na TV Box (ADB)

### Via USB

1. Ative as **Opções do desenvolvedor** e a **Depuração USB** nas configurações da TV Box.
2. Conecte o dispositivo ao computador e verifique a conexão:

```bash
adb devices
```

3. Instale ou atualize o pacote:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Via Wi-Fi (sem cabos)

Estando com o dispositivo conectado via USB ao menos uma vez (ou com ADB por rede ativado):

```bash
adb tcpip 5555
adb connect IP_DA_BOX:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuração inicial

| Campo | Descrição |
|--------|-----------|
| **URL do quiosque** | Endereço web completo (`https://...`) do painel de exibição |
| **PIN** | Opcional; se mantido em branco, exige confirmação via alerta |

Valores gravados localmente em `kiosk_prefs`.

### Sem URL na primeira execução

Caso nenhuma URL esteja salva ao abrir o app pela primeira vez, o painel de configurações é aberto automaticamente.

### Alterar URL ou PIN posteriormente

Utilize um dos [gatilhos de administrador](#segurança-e-pin) para acessar as configurações.

---

## Definir launcher padrão

### Pelo aplicativo

Acesse **Configurações** → **Mudar Launcher Padrão** (dispara `ACTION_HOME_SETTINGS` para o seletor nativo do sistema).

### Pelo terminal (ADB)

```bash
adb shell cmd package set-home-activity com.kiosk.launcher/.MainActivity
```

### Reverter para o launcher original

Utilize o seletor nativo do sistema em **Configurações → Aplicativos → Início (Home)** e altere para o launcher original da box.

---

## Segurança e PIN

### Com PIN configurado (Recomendado)

Requer mínimo de **4 dígitos**. Para desbloquear o acesso às configurações a partir do modo kiosk:

| Gatilho | Ação |
|--------|------|
| **Swipe** | Deslize rápido da **borda esquerda** para a **direita** sobre o WebView |
| **OK / Enter** | Pressionar a tecla **OK** 7 vezes em até 2 segundos |
| **Home** | Reabrir o launcher 7 vezes em até 2 segundos (`onNewIntent`) |
| **Voltar** | Pressionar a tecla **Voltar** 7 vezes em até 2 segundos |

Após o gatilho, insira o PIN correto no modal para abrir as configurações.

### Sem PIN (Modo Flexível)

Se o campo de PIN estiver vazio, os gatilhos abrem a tela de configurações diretamente. 

Ao tentar salvar sem PIN, o app exibe um **AlertDialog** de confirmação alertando que a segurança do quiosque ficará comprometida.

### Limitações do Android

A tecla nativa **Home** não pode ser interceptada diretamente por aplicações comuns de launcher. O bloqueio total absoluto exigiria permissões de *Device Owner* / *Lock Task Mode* via MDM, mantendo-se fora do escopo desta versão para preservar a compatibilidade universal com boxes genéricas.

---

## Depuração (Logcat)

1. Com a box conectada, filtre os logs no Android Studio:

```text
tag:KioskWebView
```

| Log exibido | Significado |
|-------------|-------------|
| `Erro de carregamento:` | Falha de rede, abortamento ou estouro de memória (OOM) |
| `Erro HTTP:` | Códigos de status de erro do servidor (404, 503, etc.) |
| Console JS | Erros internos reportados pelos scripts da página web |

---

## Painel web (Netlify)

Recomendações essenciais para desempenho otimizado em hardware limitado de TV Boxes:

- Utilizar dimensões máximas de **1920×1080 pixels**
- Dar preferência absoluta ao formato **WebP** com arquivos leves (<300 KB)
- Evitar carregamento simultâneo massivo de mídias pesadas (recomenda-se script de pré-carregamento sequencial)

---

## Build e release

### Versão Debug (Testes)

```bash
./gradlew assembleDebug
```

### Versão Release (Produção)

1. Gerar chave de assinatura (*Keystore*):

```bash
keytool -genkey -v -keystore farol-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias farol
```

2. Configurar os parâmetros de assinatura em `app/build.gradle.kts` (o arquivo `.jks` é ignorado no `.gitignore`).
3. Compilar:

```bash
./gradlew assembleRelease
```

---

## Publicar no GitHub

### Primeiro upload via Git

```bash
cd farol-kiosk
git init
git add .
git status
git commit -m "feat: initial release of Farol Kiosk app"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/farol-kiosk.git
git push -u origin main
```

### Publicar Release v1.0.0 no GitHub

Na aba **Releases** do seu repositório:
- **Tag:** `v1.0.0`
- **Title:** Farol Kiosk 1.0.0 — primeira versão
- **Anexo:** O arquivo binário `app-debug.apk` direto (sem compactar em ZIP).

---

## Estrutura do código

```text
app/src/main/java/com/kiosk/launcher/
├── MainActivity.kt          # Lógica do WebView, modo kiosk e gatilhos
├── SettingsActivity.kt      # Tela de configurações e validações
├── KioskPreferences.kt      # Gerenciador de SharedPreferences (URL/PIN)
└── PinUnlockDialog.kt       # Modal de desbloqueio por PIN
```

---

## Solução de problemas

| Problema | Solução |
|----------|---------|
| O app não assume como launcher | Utilizar o botão **Mudar Launcher Padrão** nas configurações ou o comando ADB `set-home-activity` |
| Tela branca / sem carregamento | Validar conectividade de rede da box e filtrar o Logcat com `tag:KioskWebView` |
| Imagens não carregam | Reduzir o peso e resolução dos arquivos WebP no painel web hospedado |
| PIN esquecido / bloqueado | Limpar dados do app via Configurações do Android → Aplicativos → Farol Kiosk → Armazenamento → Limpar dados |

---

**Farol Kiosk** — Solução leve e segura para sinalização digital e exibições dedicadas em Android TV Box.
