# Farol Kiosk

**Farol Kiosk** é um launcher Android para **TV Boxes** e dispositivos
de sinalização digital em modo quiosque. O aplicativo substitui a tela
inicial do dispositivo, exibe um painel web em tela cheia e permite
proteger o acesso às configurações com um **PIN opcional**.

  -------------------- --------------
  **Versão**           1.2.0
  **Android mínimo**   7.0 (API 24)
  **Orientação**       Paisagem
  -------------------- --------------

------------------------------------------------------------------------

## Índice

1.  [Sobre o Farol Kiosk](#sobre-o-farol-kiosk)
2.  [Funcionalidades](#funcionalidades)
3.  [Requisitos](#requisitos)
4.  [Instalação](#instalação)
5.  [Configuração inicial](#configuração-inicial)
6.  [Iniciar o modo quiosque](#iniciar-o-modo-quiosque)
7.  [Acessar as configurações](#acessar-as-configurações)
8.  [Segurança e PIN](#segurança-e-pin)
9.  [Solução de problemas](#solução-de-problemas)

------------------------------------------------------------------------

## Sobre o Farol Kiosk

O Farol Kiosk foi desenvolvido para transformar uma TV Box ou outro
dispositivo Android compatível em uma tela dedicada à exibição de
conteúdo web, como painéis informativos, comunicados e materiais de
sinalização digital.

O aplicativo é destinado a quem instala, configura e utiliza o
dispositivo para visualizar esse conteúdo.

------------------------------------------------------------------------

## Funcionalidades

-   **Tela inicial dedicada:** pode ser definido como launcher padrão do
    Android.
-   **Painel em tela cheia:** exibe o conteúdo web sem elementos
    desnecessários da interface.
-   **Navegação integrada:** mantém os links do painel dentro do próprio
    aplicativo.
-   **Configuração de endereço:** permite informar ou alterar a URL do
    painel.
-   **Proteção por PIN:** opção de restringir o acesso às configurações.
-   **Acesso administrativo:** oferece gestos e atalhos para retornar às
    configurações.
-   **Uso em TV Box:** interface e orientação adequadas à exibição em
    telas paisagem.
-   **Tela de espera:** logo e indicador de carregamento enquanto o painel
    ou a rede ficam prontos (útil após ligar a TV Box na tomada).
-   **Recuperação automática:** novas tentativas de carga se a internet
    ainda não estiver disponível no boot.

------------------------------------------------------------------------

## Requisitos

### Dispositivo

-   TV Box ou dispositivo Android compatível.
-   Android 7.0 (API 24) ou superior.
-   Tela configurada em orientação paisagem.

### Conexão

-   Acesso à internet ou à rede onde o painel está hospedado.
-   URL válida e acessível para o painel web.

------------------------------------------------------------------------

## Instalação

1.  Obtenha o arquivo APK do Farol Kiosk fornecido pelo responsável pelo
    dispositivo.
2.  Abra o arquivo APK na TV Box.
3.  Caso o Android solicite, autorize a instalação de aplicativos dessa
    fonte.
4.  Aguarde a conclusão da instalação.
5.  Abra o aplicativo **Farol Kiosk**.

> **Importante:** instale somente arquivos APK disponibilizados por uma
> fonte confiável.

------------------------------------------------------------------------

## Configuração inicial

Na primeira abertura, caso ainda não exista uma URL salva, o aplicativo
exibirá automaticamente a tela de configurações.

### URL do quiosque

Informe o endereço completo do painel que deverá ser exibido.

Exemplo:

``` text
https://exemplo.com/painel
```

Verifique se o endereço está correto e se o dispositivo possui conexão
com a rede.

### PIN de acesso (opcional)

O PIN protege o acesso à tela de configurações do aplicativo.

-   Para ativar a proteção, informe um PIN numérico de pelo menos **4
    dígitos**.
-   Para utilizar o aplicativo sem senha, deixe o campo em branco.
-   **Dica do PIN (opcional):** texto de ajuda exibido na tela de
    desbloqueio (não substitui o PIN).
-   Guarde o PIN em local seguro para evitar a perda de acesso às
    configurações.

### Salvar as configurações

Após preencher os campos:

1.  Confira a URL informada.
2.  Defina um PIN, caso deseje utilizar proteção por senha.
3.  Toque em **Salvar e Iniciar Kiosk**.

------------------------------------------------------------------------

## Iniciar o modo quiosque

Ao iniciar o modo quiosque pela primeira vez, o Android poderá perguntar
qual aplicativo deverá ser utilizado como tela inicial.

1.  Selecione **Farol Kiosk** na lista apresentada.
2.  Escolha a opção **Sempre**, quando disponível.

O Farol Kiosk passará a ser utilizado como tela inicial padrão, abrindo
o painel configurado quando o dispositivo retornar à tela inicial.

### Alterar o launcher padrão

Caso queira selecionar outro aplicativo de tela inicial:

1.  Acesse as configurações do Android.
2.  Entre na seção de aplicativos padrão ou de tela inicial.
3.  Selecione o launcher desejado.

O nome e a localização dessas opções podem variar conforme o fabricante
da TV Box.

------------------------------------------------------------------------

## Acessar as configurações

É possível retornar à tela de configurações mesmo enquanto o painel
estiver sendo exibido.

Utilize um dos gatilhos abaixo:

  -----------------------------------------------------------------------
  Gatilho                             Como utilizar
  ----------------------------------- -----------------------------------
  **Gesto na tela**                   Deslize rapidamente da borda
                                      esquerda para a direita sobre o
                                      WebView.

  **OK / Enter**                      Pressione a tecla OK 7 vezes em até
                                      2 segundos.

  **Voltar**                          Pressione a tecla Voltar 7 vezes em
                                      até 2 segundos.

  **Home**                            Reabra o launcher 7 vezes em até 2
                                      segundos.
  -----------------------------------------------------------------------

Se houver um PIN configurado, será exibida uma tela solicitando a senha.
Após informar o PIN correto, será possível acessar as configurações.

Na tela de configurações, você poderá alterar a URL do painel e o PIN e
iniciar novamente o modo quiosque.

------------------------------------------------------------------------

## Segurança e PIN

### Com PIN configurado

O acesso às configurações exige a informação do PIN correto após a
utilização de um dos gatilhos administrativos. O PIN é armazenado apenas
como **hash** (SHA-256), em preferências **criptografadas** no
dispositivo.

Recomenda-se manter um PIN configurado quando o dispositivo estiver
instalado em ambientes públicos ou acessíveis a outras pessoas.

### Sem PIN

Se o campo de PIN estiver vazio, os gatilhos administrativos abrem
diretamente a tela de configurações.

Ao tentar iniciar o modo quiosque sem PIN, o aplicativo apresenta um
alerta para confirmar que o dispositivo ficará sem essa proteção.

### Limitações do Android

O PIN protege as configurações do Farol Kiosk, mas não bloqueia
completamente os recursos nativos do Android.

A tecla **Home**, por exemplo, não pode ser interceptada diretamente por
aplicativos comuns. O bloqueio integral do dispositivo depende de
recursos específicos do Android, como **Device Owner** e **Lock Task
Mode**, que exigem configuração adicional e compatibilidade do
equipamento.

------------------------------------------------------------------------

## Solução de problemas

  -----------------------------------------------------------------------
  Problema                            Possível solução
  ----------------------------------- -----------------------------------
  O painel não abre                   Verifique se a URL está correta e
                                      se a TV Box possui conexão com a
                                      rede. Aguarde a tela de espera; o
                                      app tenta de novo automaticamente.

  A tela fica preta ou piscando       Normal logo após ligar na tomada
                                      enquanto a Wi‑Fi conecta. Se
                                      persistir, confira a URL e veja
                                      Logcat com a tag `KioskWebView`.

  A tela fica branca                  Teste o endereço em outro
                                      dispositivo e confirme se o painel
                                      está disponível.

  Imagens não carregam                Verifique a conexão e se os
                                      arquivos de imagem estão acessíveis
                                      pelo endereço do painel.

  O Farol Kiosk não inicia como tela  Defina o aplicativo como launcher
  inicial                             padrão nas configurações do
                                      Android.

  Não consigo acessar as              Utilize um dos gatilhos
  configurações                       administrativos descritos neste
                                      manual.

  Esqueci o PIN                       Nas configurações do Android,
                                      acesse Aplicativos → Farol Kiosk →
                                      Armazenamento → Limpar dados. Isso
                                      apaga o PIN, a URL e as demais
                                      preferências salvas.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## Desenvolvimento

Requisitos: **JDK 17**, Android SDK **35**.

```bash
./gradlew assembleDebug          # APK de teste
./gradlew assembleRelease        # release (R8); assine se necessário
```

Assinatura release (opcional):

1. Copie `keystore.properties.example` para `keystore.properties`.
2. Gere ou aponte para um `.jks` e preencha senhas e alias.
3. Rode `./gradlew assembleRelease` — o APK assinado fica em
   `app/build/outputs/apk/release/`.

Instalação via ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Logs do WebView no dispositivo:

```bash
adb logcat -s KioskWebView
```

------------------------------------------------------------------------

## GitHub e releases

Repositório: **https://github.com/Guiantebii/farol-kiosk**

### Como o projeto está organizado

| Peça | Significado |
|------|-------------|
| **`main`** | Branch principal — é o que deve estar no GitHub e refletir a versão “oficial”. |
| **Branches `feature/...`** | Trabalho em andamento; quando terminar, você **mescla** em `main`. |
| **Tag `v1.x.x`** | Marcador de versão no Git (ex.: `v1.0.0` já existe no remoto). |
| **GitHub Release** | Página no GitHub com notas + **APK anexo** para baixar/instalar. |
| **APK debug** | `./gradlew assembleDebug` — testes na TV Box (ADB). |
| **APK release** | `./gradlew assembleRelease` — menor, otimizado (R8); ideal para publicar. |

Versão atual no código: **1.2.0** (`versionCode` 4) em `app/build.gradle.kts`.

### Fluxo para “atualizar tudo” (checklist)

1. **Salvar o código no Git** (commit na branch em que você está).
2. **Mesclar em `main`** se ainda estiver numa `feature/...`.
3. **`git push origin main`** — GitHub fica igual ao seu PC.
4. **Gerar o APK** (`assembleDebug` ou `assembleRelease`).
5. **Criar tag** `v1.2.0` e enviar: `git push origin v1.2.0`.
6. **No GitHub:** *Releases* → *Draft a new release* → escolher a tag → colar trecho do `CHANGELOG.md` → anexar o `.apk`.

### Comandos (copiar e colar)

Na pasta do projeto:

```bash
# Ver o que falta commitar
git status

# Commitar tudo da versão atual (ajuste a mensagem se quiser)
git add -A
git commit -m "$(cat <<'EOF'
release: Farol Kiosk 1.2.0

Splash de carregamento, recuperação de rede no boot, preferências criptografadas,
validação de URL e build release otimizado.
EOF
)"

# Trazer main para cá e mesclar (se você estava em feature/pin-recovery-and-launcher)
git checkout main
git merge feature/pin-recovery-and-launcher

# Enviar para o GitHub
git push origin main

# Marcar versão no Git
git tag -a v1.2.0 -m "Farol Kiosk 1.2.0"
git push origin v1.2.0

# APK de teste (não precisa de keystore)
./gradlew assembleDebug
# Saída: app/build/outputs/apk/debug/app-debug.apk

# APK release (opcional: keystore.properties para assinar)
./gradlew assembleRelease
# Saída: app/build/outputs/apk/release/
```

### Assinatura do APK release (recomendado para distribuir)

Sem `keystore.properties`, o Gradle gera release **não assinado** (útil só em parte dos fluxos).
Para instalar em TV Box de clientes:

```bash
keytool -genkey -v -keystore release/farol-kiosk.jks -keyalg RSA -keysize 2048 -validity 10000 -alias farol
cp keystore.properties.example keystore.properties
# Edite keystore.properties com caminhos e senhas
./gradlew assembleRelease
```

Guarde o `.jks` e as senhas em local seguro — **sem eles você não atualiza o app** com a mesma chave.

### Criar a Release no site do GitHub

1. Abra **Releases** no repositório.
2. **Draft a new release** → tag **v1.2.0** (criada no passo acima).
3. Título: `Farol Kiosk 1.2.0`.
4. Descrição: copie a seção `[1.2.0]` do `CHANGELOG.md`.
5. Anexe `app-debug.apk` (testes) ou `app-release.apk` (assinado).
6. **Publish release**.

### Situação típica se você “se perdeu”

- Código novo só no PC, **sem commit** → passo 1 do checklist.
- Commits numa **feature**, `main` no GitHub **atrás** → merge + push.
- Tag **só v1.0.0** no GitHub → criar **v1.2.0** depois do push de `main`.
- **Release** no GitHub ≠ tag: release é tag + notas + APK.

Branches antigas (`feature/security-and-resilience`, etc.) podem ser apagadas no GitHub depois que tudo estiver em `main`.

------------------------------------------------------------------------

**Farol Kiosk** --- solução para exibição de painéis web em dispositivos
Android.
