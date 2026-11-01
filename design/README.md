# PrimeTV — Design

Protótipos das telas do app PrimeTV (Android 11, TV Box, sempre na horizontal).

Canvas interativo (fonte da verdade do design):
https://claude.ai/artifact/SjyEjfYaogqEymziiLPnmu

A pasta `prototipos/` guarda o código-fonte de cada tela (`.dc.html`) e o índice do canvas (`canvas.json`). Esses arquivos dependem do runtime do canvas para renderizar, então servem como referência de layout, textos, medidas e cores para a implementação. Para visualizar, use o link acima.

## Telas

| # | Arquivo | Tela |
|---|---------|------|
| 01 | `Main.dc.html` | Início: destaque (recomendação TMDB), Continuar assistindo, Futebol agora, aviso de atualização em segundo plano |
| 02 | `Detalhes.dc.html` | Detalhes da série: capa, metadados, temporadas, episódios com progresso |
| 03 | `AoVivo.dc.html` | Ao vivo: categorias, canais com programa atual (EPG), prévia com Agora / A seguir |
| 04 | `Busca.dc.html` | Busca: teclado para controle remoto + autocomplete do conteúdo local |
| 05 | `Player.dc.html` | Player: progresso, ±10 s, áudio, legendas, episódios, próximo episódio |
| 06 | `MenuApp.dc.html` | Menu do logo "P": verificar atualização via GitHub Releases |
| 07 | `Configuracoes.dc.html` | Configurações: intervalo de atualização por seção, palavras-chave de futebol, controle parental, idioma |
| 08 | `Setup.dc.html` | Configuração inicial: URL da lista (M3U ou Xtream), carregamento por seção |
| 09 | `ReceberLink.dc.html` | Receber link pelo celular: código + QR, aguardando / recebido |

## Tokens

| Token | Valor |
|-------|-------|
| Fundo | `#0B0B0F` |
| Superfície | `#16161C` |
| Superfície 2 | `#22222A` |
| Botão secundário | `#2A2A33` |
| Texto | `#F2F2F2` |
| Texto secundário | `#A1A1AD` |
| Acento (violeta) | `#8B5CF6` |
| Acento hover/pressed | `#7340E8` |
| Acento claro (texto em chip) | `#DDD6FE` sobre `#2E2350` |
| Selecionado (linha/card) | `#1E1A2E` |
| Ao vivo (badge) | `#C2263B` |

- Fonte: Inter (400/500/600/700).
- Tela base 1280×720 dp-equivalente; escalar para 1920×1080.
- Margem lateral 48, raio 8 (cards) / 12–16 (painéis).
- Foco do D-pad: anel 3px `#8B5CF6` (ou `#F2F2F2` sobre botão violeta) + escala ~1.04–1.06.
- Texto em botão violeta: branco, 18px semibold.
