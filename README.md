# Farol Kiosk

**Farol Kiosk** é um launcher Android de alta performance e resiliência desenvolvido para **TV Boxes** e dispositivos de sinalização digital (Digital Signage) em modo quiosque. O aplicativo substitui a tela inicial (Launcher native) do sistema, exibe um painel web em tela cheia (WebView com aceleração por hardware) e protege o acesso administrativo através de criptografia e **PIN opcional**.

| Atributo | Especificação |
| --- | --- |
| **Versão** | 1.2.0 |
| **Android mínimo** | 7.0 (API 24) |
| **Android alvo** | 15 (API 35) |
| **Orientação** | Paisagem (`landscape`) |
| **Arquitetura** | Kotlin, ViewBinding, Coroutines, Material Design 3 |

---

## Índice

1. [Sobre o Farol Kiosk](https://www.google.com/search?q=%23sobre-o-farol-kiosk)
2. [Funcionalidades e Arquitetura](https://www.google.com/search?q=%23funcionalidades-e-arquitetura)
3. [Destaques de Segurança](https://www.google.com/search?q=%23destaques-de-seguran%C3%A7a)
4. [Requisitos](https://www.google.com/search?q=%23requisitos)
5. [Instalação](https://www.google.com/search?q=%23instala%C3%A7%C3%A3o)
6. [Configuração Inicial](https://www.google.com/search?q=%23configura%C3%A7%C3%A3o-inicial)
7. [Iniciar o Modo Quiosque](https://www.google.com/search?q=%23iniciar-o-modo-quiosque)
8. [Gatilhos de Acesso Administrativo](https://www.google.com/search?q=%23gatilhos-de-acesso-administrativo)
9. [Solução de Problemas](https://www.google.com/search?q=%23solu%C3%A7%C3%A3o-de-problemas)
10. [Desenvolvimento e Build](https://www.google.com/search?q=%23desenvolvimento-e-build)

---

## Sobre o Farol Kiosk

O Farol Kiosk transforma TV Boxes e dispositivos Android em totens de sinalização digital ininterruptos. Projetado para operação 24/7 em ambientes institucionais e comerciais (como campi acadêmicos, recepções e murais digitais), o aplicativo elimina a dependência de intervenção humana em caso de quedas de energia ou oscilações de rede.

---

## Funcionalidades e Arquitetura

* **Launcher Nativo (`android.intent.category.HOME`):** Assume o controle da tela inicial da TV Box. Caso o aparelho seja desligado da tomada ou reiniciado, o Farol Kiosk é iniciado automaticamente.
* **Renderização de Alta Performance:** Configurado com aceleração gráfica por hardware (`hardwareAccelerated="true"`) e alocação expandida de memória RAM (`largeHeap="true"`), prevenindo estouros de memória (*OutOfMemoryError*) na WebView.
* **Recuperação Automática e Resiliência de Rede:**
* **Escuta Ativa de Conexão (`ConnectivityManager`):** Detecta a restauração do Wi-Fi/Ethernet e recarrega o painel imediatamente sem recarregar a tela do zero.
* **Watchdog de Erro:** Em caso de falha de carregamento (HTTP 4xx/5xx ou perda de sinal), ativa retentativas automáticas a cada 30 segundos mantendo uma tela de espera amigável (*Splash Screen*).


* **Tratamento de Teclas do Controle Remoto:** Intercepta e bloqueia teclas do sistema (`Home`, `App Switch`, `Search`, `Settings`, `Canais`, etc.) e previne o fechamento acidental da aplicação pelo botão `Voltar`.
* **Prevenção de Burn-in e Sleep Mode:** Mantém a tela ativada continuamente (`FLAG_KEEP_SCREEN_ON`) e aplica Modo Imersivo Avançado (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`) ocultando as barras de status e navegação do Android.
* **Sanitização Automática de URLs:** Normaliza endereços informados (acrescenta `https://` automaticamente caso omitido) e bloqueia esquemas inseguros (`file://`, `javascript:`).

---

## Destaques de Segurança

* **Armazenamento Criptografado:** Utiliza `EncryptedSharedPreferences` com chave mestre em hardware (`AES256_GCM` e `AES256_SIV`). Dados sensíveis não são gravados em texto plano no dispositivo.
* **Hash de PIN (SHA-256):** O PIN de administração não é salvo no sistema. A verificação é realizada exclusivamente por comparação de Hash SHA-256 unidirecional.
* **Proteção Contra Captura de Tela (`FLAG_SECURE`):** Aplicada tanto na tela de configurações (`SettingsActivity`) quanto no modal de desbloqueio (`PinUnlockDialog`), impedindo a gravação de vídeo, capturas de tela ou espelhamento não autorizado durante a digitação da senha.
* **Blindagem do Aplicativo (`exported="false"` e `allowBackup="false"`):** A tela de configurações não pode ser invocada por outros aplicativos instalados e os dados não podem ser extraídos via backup ADB por cabo USB.

---

## Requisitos

### Dispositivo

* TV Box, Smart TV ou dispositivo Android compatível.
* Android 7.0 (API 24) ou superior.
* Orientação fixada em paisagem (*landscape*).

### Conexão

* Rede Wi-Fi ou Ethernet com acesso ao painel web.
* URL acessível contendo o painel/carrossel informativo.

---

## Instalação

1. Obtenha o arquivo APK de release do **Farol Kiosk**.
2. Transfira o APK para a TV Box (via pendrive ou rede local).
3. Abra o gerenciador de arquivos do dispositivo e execute a instalação (autorize a instalação de fontes desconhecidas caso solicitado).
4. Abra o aplicativo **Farol Kiosk**.

---

## Configuração Inicial

Na primeira inicialização, o aplicativo detecta a ausência de URL configurada e abre automaticamente o painel de configurações.

### 1. URL do Quiosque

Informe o endereço web do painel. Exemplo:

```text
https://painel.fatec.br/bloco-a

```

*Se você digitar apenas `painel.fatec.br`, o validador converterá automaticamente para `[https://painel.fatec.br](https://painel.fatec.br)`.*

### 2. PIN de Acesso (Opcional, mas Recomendado)

* **Definir PIN:** Digite uma senha numérica com no mínimo **4 dígitos**.
* **Dica do PIN:** Digite um lembrete para ser exibido na tela de validação (ex: *"Ramal do TI"*).
* **Sem PIN:** Deixe o campo em branco (o app exibirá um alerta confirmando a ausência de proteção por senha).

### 3. Salvar e Iniciar

Clique em **Salvar e Iniciar Kiosk**. O painel será carregado imediatamente.

---

## Iniciar o Modo Quiosque

Ao clicar em ir para a tela inicial da TV Box (botão Home do controle) ou abrir o app pela primeira vez:

1. O Android exibirá a caixa de diálogo *"Selecione um aplicativo para a tela inicial"*.
2. Selecione **Farol Kiosk**.
3. Escolha a opção **Sempre**.

---

## Gatilhos de Acesso Administrativo

Como a interface oculta todos os menus do Android, o acesso às configurações é feito acionando um dos atalhos ocultos monitorados por um algoritmo de janela deslizante ($O(1)$) de 2 segundos:

| Gatilho | Ação de Ativação |
| --- | --- |
| **Pressionar OK / Enter** | Pressione o botão **OK** do controle remoto 7 vezes em até 2 segundos. |
| **Pressionar Voltar** | Pressione o botão **Voltar** do controle remoto 7 vezes em até 2 segundos. |
| **Reabertura Home** | Pressione o botão **Home** do controle remoto 7 vezes em até 2 segundos. |
| **Gesto Touch / Mouse** | Arraste rapidamente da **borda esquerda para a direita** na tela. |

Após o gatilho, se um PIN estiver configurado, a janela segura de autenticação será exibida com foco e teclado virtual acionados automaticamente.

---

## Solução de Problemas

| Sintoma | Causa Provável | Solução |
| --- | --- | --- |
| **Tela de carregamento em loop** | TV Box sem internet ou URL inacessível. | Verifique o cabo de rede/Wi-Fi. O app fará retentativas automáticas via *Watchdog* a cada 30s assim que o sinal retornar. |
| **Imagens do painel não atualizam** | Cache de navegação retendo imagens antigas. | Adicione *cache-busting* na URL das imagens do site (ex: `imagem.jpg?v=TIMESTAMP`) ou configure `LOAD_NO_CACHE` na WebView. |
| **O aplicativo não abre ao ligar a TV Box** | O launcher padrão não foi definido. | Acesse a `SettingsActivity` via gatilho, clique em **Definir Launcher Padrão** e escolha a opção *Sempre* para o Farol Kiosk. |
| **Esqueci o PIN de Administração** | Perda do PIN hash salvo no dispositivo. | Acesse as configurações nativas do Android (`Configurações → Apps → Farol Kiosk → Armazenamento`) e selecione **Limpar Dados**. Isso restaurará o app ao estado de fábrica. |

---

## Desenvolvimento e Build

### Requisitos de Ambiente

* **JDK 17**
* **Android SDK 35**
* **Gradle 8.x+**

### Comandos de Compilação

```bash
# Compilar versão de desenvolvimento (Debug)
./gradlew assembleDebug

# Compilar versão de produção assinada (Release)
./gradlew assembleRelease

```

> **Assinatura de Release:** Antes de executar `assembleRelease`, crie o arquivo `keystore.properties` na raiz do projeto com base em `keystore.properties.example` preenchendo as chaves do seu certificado digital.

### Debug Remoto da WebView (Chrome DevTools)

Em compilações Debug (`BuildConfig.DEBUG = true`), o suporte a depuração remota via USB/Rede vem ativado por padrão.

1. Conecte a TV Box ao computador via ADB (`adb connect IP_DA_TVBOX`).
2. Abra o Google Chrome no computador e acesse:
```text
chrome://inspect

```


3. Inspecione o elemento `KioskWebView` para analisar logs de JavaScript, rede e visualização DOM em tempo real.

### Logs do Sistema

Para filtrar logs específicos da WebView e da infraestrutura do Kiosk via terminal:

```bash
adb logcat -s KioskWebView KioskPreferences

```
