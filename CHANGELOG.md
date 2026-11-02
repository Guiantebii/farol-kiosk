# Changelog

## [1.2.0] — 2026-11-01

### Melhorias

- Tela de espera (splash) com fade até o painel carregar
- Recuperação automática após boot sem rede (watchdog + callback de rede)
- Preferências criptografadas (URL, hash do PIN e dica) com migração automática
- Validação de URL (`http`/`https`) e normalização com `https://` quando omitido
- `FLAG_SECURE` nas telas de PIN e configurações
- Release com R8, shrink de resources e template de assinatura (`keystore.properties.example`)
- Regras de backup/extração de dados e `network_security_config` documentado

## [1.0.0] — 2026-10-31

Primeira versão pública (**Farol Kiosk**).

### Incluído

- Launcher HOME com WebView em tela cheia (modo quiosque)
- Tela de configurações (URL, PIN opcional, atalhos do sistema)
- PIN flexível com alerta ao iniciar sem proteção
- Gatilhos de administrador (swipe, 7× Voltar/OK/Home)
- Otimizações de WebView para TV Box e logs `KioskWebView`
- Identidade visual Farol Kiosk (ícone e logo nas configurações)
