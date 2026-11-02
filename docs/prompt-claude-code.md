# Prompt para o Claude Code — PrimeTV

> Cole o bloco abaixo no Claude Code, aberto na raiz da pasta `prime-tv`.

---

Você vai implementar o **PrimeTV**, um app Android de IPTV com interface no estilo Netflix, feito para rodar em **TV Box com Android 11 comum (não é Android TV)**, controlada por controle remoto (D-pad). O APK é instalado direto no aparelho (sideload), sem Play Store.

Antes de escrever código, leia:
- `design/README.md` — tokens de cor, fonte, medidas e lista de telas.
- `design/prototipos/*.dc.html` — layout, textos e hierarquia de cada tela (é HTML de referência; reproduza o visual em Compose, não use WebView). Canvas visual: https://claude.ai/artifact/SjyEjfYaogqEymziiLPnmu
- `docs/roadmap.md` — o que é desta fase e o que fica para depois.

Trabalhe em fases (lista no final). Ao terminar cada fase: build passando, rode os testes, faça um commit e me mostre um resumo curto antes de seguir. Se algo da especificação for ambíguo, pergunte antes de decidir algo difícil de desfazer.

## 1. Stack

- Kotlin, Gradle Kotlin DSL, version catalog (`libs.versions.toml`).
- Jetpack Compose + `androidx.tv:tv-material` (componentes focáveis para D-pad). Single-activity, Navigation Compose.
- Arquitetura MVVM com camadas `data / domain / ui`, Hilt para DI, Coroutines + Flow.
- Media3 (ExoPlayer) para reprodução: HLS, MPEG-TS e MP4/MKV.
- Room (com FTS4 para busca) para catálogo, histórico, favoritos e EPG.
- DataStore para preferências; a URL da lista guardada criptografada (Tink ou EncryptedSharedPreferences).
- WorkManager para atualização da lista em segundo plano.
- OkHttp + Retrofit + kotlinx.serialization para APIs; Coil para imagens (cache em disco limitado).
- `minSdk 24`, `targetSdk 34`, `compileSdk` atual. ABIs: `armeabi-v7a` e `arm64-v8a` (gerar APK universal).
- Nada de Google Play Services, Firebase ou WebView.

## 2. Requisitos de plataforma (TV Box Android comum)

- No `AndroidManifest`, registrar a activity com `CATEGORY_LAUNCHER` **e** `CATEGORY_LEANBACK_LAUNCHER`, ter `android:banner` além do ícone, e declarar `uses-feature` de `touchscreen` e `leanback` com `required="false"`.
- Sempre na horizontal: `android:screenOrientation="sensorLandscape"`.
- **Escala da UI:** boxes comuns reportam densidades inconsistentes (160/240/320 dpi). O design foi feito numa base de **1280×720**. Crie um `Density` customizado no tema que mapeie a largura da tela para 1280 unidades (`density = larguraPx / 1280f`, `fontScale = 1f`), para a interface ficar igual em qualquer box. Use as medidas do design diretamente em `dp` nessa base.
- **Navegação:** tudo focável pelo D-pad, com ordem previsível (cima/baixo entre linhas, esquerda/direita dentro da linha, foco lembrado ao voltar para uma linha). Foco visível sempre: anel 3px `#8B5CF6` + escala ~1.05. Tecla Voltar sempre volta um nível. Suportar também clique de air mouse/ponteiro e toque.
- Muitas listas IPTV usam `http`: permitir cleartext via `network_security_config`.
- Hardware fraco: listas e grades sempre lazy, imagens redimensionadas, parse da lista em streaming, nada pesado na main thread.
- Nunca registrar em log a URL da lista, usuário ou senha.

## 3. Identidade visual

Use exatamente os tokens de `design/README.md` num `PrimeTheme` (cores, tipografia Inter empacotada nos assets, formas). Fundo escuro `#0B0B0F`, acento violeta `#8B5CF6`, texto branco em botão violeta. Logo: quadrado arredondado violeta com "P" branco + wordmark "Prime**TV**". Componentes reutilizáveis: `PrimeButton` (primário/secundário), `PosterCard`, `WideCard` com barra de progresso, `ChannelRow`, `CategoryRail`, `Chip`, `LoadingSpinner`, `Toast` de status.

## 4. Fontes de conteúdo

### 4.1 Lista do usuário (M3U e Xtream Codes)
- A tela de configuração inicial recebe uma URL que **já contém usuário e senha**.
- **Detecção:** se a URL tiver `username` e `password` (ex.: `.../get.php?username=U&password=P&type=m3u_plus`), testar `player_api.php?username=U&password=P`. Se responder, usar o modo **Xtream**; senão, tratar como **M3U**.
- **Xtream:** `get_live_categories`, `get_live_streams`, `get_vod_categories`, `get_vod_streams`, `get_vod_info`, `get_series_categories`, `get_series`, `get_series_info`. URLs de stream no formato `/live/U/P/{id}.ts` (ou `.m3u8`), `/movie/U/P/{id}.{ext}` e `/series/U/P/{id}.{ext}`. EPG em `xmltv.php?username=U&password=P`.
- **M3U:** parser em streaming de `#EXTINF` (atributos `tvg-id`, `tvg-name`, `tvg-logo`, `group-title` e nome após a vírgula; `url-tvg`/`x-tvg-url` no cabeçalho para o EPG). Classificar em Ao vivo, Filmes e Séries pelo caminho da URL (`/movie/`, `/series/`), pela extensão e por palavras no `group-title`. Agrupar episódios de série por regex (`S01E02`, `T1E2`, `1x02`).
- Inserção no Room em lotes, com transação, sem travar a UI. Listas podem ter centenas de milhares de itens.

### 4.2 Recomendações (TMDB)
- Usar a API do TMDB (`language=pt-BR`): trending da semana, populares e em cartaz. A chave fica em `local.properties` → `BuildConfig.TMDB_API_KEY` (no CI, vem de um secret). Se não houver chave, o destaque usa itens recentes da própria lista.
- **Mostrar só o que existe na lista do usuário:** cruzar por título normalizado (sem acentos, minúsculo, sem ano/qualidade como "4K", "FHD", "[L]") + ano quando houver; usar `tmdb_id` do Xtream quando disponível.
- Do TMDB vêm backdrop, sinopse, gêneros, nota e número de temporadas/episódios. A capa da lista (`tvg-logo`/`stream_icon`) é a primeira opção para pôsteres; o TMDB é o fallback.
- Cache local: guardar no Room as respostas do TMDB (detalhes por `tmdb_id` por ~7 dias; listas de trending/populares por ~12 h) e as imagens no cache do Coil, para fazer poucas chamadas e funcionar offline com o que já foi visto.
- Atribuição exigida pelo TMDB: na tela Sobre, exibir o logo do TMDB e o aviso "Este produto usa a API do TMDB, mas não é endossado ou certificado pelo TMDB".
- Trailer: se houver trailer no YouTube e o app do YouTube estiver instalado, abrir por intent; senão, esconder o botão.

### 4.3 Futebol
- Aba própria "Futebol" que reúne canais, eventos e VOD cujo nome ou categoria bate com palavras-chave configuráveis (padrão: futebol, premiere, brasileirão, libertadores, copa do brasil, champions, premier league, laliga, série a, série b, estaduais), mais o padrão "time x time" / "time vs time".
- Comparação normalizada (sem acentos, minúsculas). A classificação roda no fim de cada atualização e é refeita quando as palavras-chave mudam.

## 5. Telas (ver protótipos em `design/prototipos/`)

1. **Configuração inicial** (`Setup`): campo da URL com a dica "O link já deve vir com usuário e senha embutidos…"; chip com o formato detectado; botão "Conectar"; botão "Receber link pelo celular". Depois de conectar, painel de progresso por seção (Ao vivo, Filmes, Séries, Guia). O botão "Começar a assistir" libera assim que Ao vivo termina; o resto continua em segundo plano.
2. **Receber link pelo celular** (`ReceberLink`): só a UI nesta fase. Mostra um código de 6 caracteres, um QR code (gerar com ZXing) apontando para `[SITE]/link?code=XXX`, o status "Aguardando o link…" e o tempo até expirar, com estados *aguardando* e *recebido*. Criar a interface `LinkReceiver` (Flow que emite o link recebido) com uma implementação fake para teste. O backend real é a fase 2 do roadmap (WebSocket ou polling).
3. **Início** (`Main`): barra superior com logo (abre o menu do app), Buscar, Início, Ao vivo, Filmes, Séries, Futebol, Minha lista, relógio e Configurações. Destaque rotativo de recomendações (backdrop, selo, título, tipo/gêneros, temporadas/episódios, nota, sinopse, "Assistir", "Mais informações", "+ Minha lista"). Abaixo, linhas: "Continuar assistindo" (com progresso), "Futebol agora" e as categorias da lista conforme a aba escolhida.
   - Ao focar um card, mostrar a arte dele (pôster da lista ou TMDB) e atualizar o destaque com debounce de ~400 ms.
4. **Detalhes** (`Detalhes`): capa, metadados, sinopse, "Assistir"/"Continuar T1:E4", "Minha lista", trailer, seletor de temporadas e episódios com miniatura, duração, progresso e "Assistido".
5. **Ao vivo** (`AoVivo`): trilho de categorias (Favoritos, Futebol primeiro), lista de canais com número, logo, nome, programa atual e barra de progresso do EPG. À direita, prévia do canal em foco (player pequeno sem áudio após ~1 s de foco) com "Agora / A seguir". OK abre em tela cheia; segurar OK favorita. Canal anterior/próximo no player com cima/baixo.
6. **Busca** (`Busca`): teclado na tela pensado para o D-pad (a–z, 0–9, espaço, apagar, limpar) e entrada por voz/teclado do sistema quando houver. Autocomplete do conteúdo local via FTS, destacando o trecho digitado, com o tipo (Filme/Série/Canal); grade de resultados. Resposta em menos de 100 ms em listas grandes.
7. **Player** (`Player`): título e episódio no topo, barra de progresso, voltar/avançar 10 s, play/pause, Áudio, Legendas, Episódios e "Próximo episódio" (card com contagem regressiva de 10 s nos últimos segundos). Controles somem após 4 s. Retomar de onde parou.
8. **Menu do app** (`MenuApp`): ao selecionar o logo, abrir o menu com a versão atual e as opções Verificar atualização, Configurações, Trocar lista IPTV e Sobre. O diálogo de atualização tem os estados verificando, nova versão (com changelog da release), baixando (com progresso) e já atualizado.
9. **Configurações** (`Configuracoes`): Lista IPTV (trocar URL, receber pelo celular), Atualização da lista (intervalo por seção — Ao vivo, Filmes, Séries, EPG — com opções 6 h, 12 h, 1 dia, 3 dias, 7 dias e manual (padrão: Ao vivo e EPG 6 h, Filmes e Séries 1 dia); mostrar a última atualização; "Atualizar tudo agora"; "Só em Wi-Fi/cabo"), Futebol (editar palavras-chave), Controle parental, Player (decodificador hardware/software, buffer), Idioma e Sobre.

## 6. Comportamentos

- **Histórico / Continuar assistindo:** salvar a posição a cada 10 s e ao sair; marcar como assistido acima de 90%; remover da linha ao terminar; série aponta para o próximo episódio. Para canais ao vivo, guardar "últimos canais".
- **Minha lista / Favoritos:** filmes, séries e canais, persistidos localmente.
- **Atualização em segundo plano:** um `PeriodicWorkRequest` por seção com o intervalo configurado (padrão 1 dia), restrição de rede e, opcionalmente, rede não medida. Também verificar ao abrir o app se alguma seção venceu. Sem bloquear a UI: mostrar o toast "Atualizando Filmes em segundo plano". Se o usuário estiver dentro da seção atualizada, marcar `newDataAvailable`; na próxima interação (tecla), mostrar a animação de carregamento e recarregar mantendo o foco no item equivalente.
- **Controle parental:** PIN de 4 dígitos; esconder categorias adultas por palavras-chave (adulto, adult, xxx, +18) e categorias marcadas manualmente; pedir o PIN para mostrar.
- **i18n por JSON:** não usar `strings.xml` para os textos da UI. Criar `assets/i18n/pt-BR.json` (chaves planas, ex.: `"home.continue_watching": "Continuar assistindo"`), um `I18n` que carrega o idioma escolhido com fallback para pt-BR e `CompositionLocal` + função `t("chave")`. Novos idiomas devem funcionar só adicionando `en.json`, `es.json` etc. Deixar a estrutura pronta para baixar JSONs no futuro.
- **Erros:** mensagens claras e em português (lista inválida, servidor fora do ar, credenciais expiradas, sem internet), sempre com uma ação ("Tentar de novo", "Trocar lista").

## 7. Atualização do app via GitHub Releases

- Repositório: `wpdas/prime-tv` (https://github.com/wpdas/prime-tv.git), configurável em `BuildConfig`.
- Consultar `https://api.github.com/repos/wpdas/prime-tv/releases/latest`, comparar `tag_name` (semver `vX.Y.Z`) com `versionName` e pegar o asset `.apk`.
- Baixar para o cache com progresso e instalar via `FileProvider` + `ACTION_VIEW`/`PackageInstaller`. Declarar `REQUEST_INSTALL_PACKAGES`; se `canRequestPackageInstalls()` for falso, levar o usuário à tela de permissão explicando o motivo.
- Checar automaticamente uma vez por dia (silencioso; só avisa se houver versão nova) e manualmente pelo menu do logo.

## 8. Build, assinatura e CI

- APK assinado com **keystore próprio** (fora do repositório; `keystore.properties` no `.gitignore`). Documentar no README como gerar o keystore com `keytool` e avisar que **todas as versões precisam da mesma chave**.
- `versionCode` e `versionName` derivados da tag do git.
- GitHub Actions `release.yml`: ao criar uma tag `v*`, decodificar o keystore de um secret em base64, rodar `./gradlew assembleRelease` e publicar a release com o APK (`primetv-vX.Y.Z.apk`) e o changelog. `ci.yml`: build + testes em cada PR.
- R8/minify ligado no release, com as regras necessárias para serialization, Room e Media3.
- README com instalação por pendrive, Downloader e `adb install`.

## 9. Testes

- Unitários: parser M3U (incluindo atributos faltando e listas grandes), cliente Xtream (respostas mockadas), classificação de futebol, matching TMDB ↔ lista, comparação de versões, i18n com fallback.
- Fixtures de M3U e de JSON Xtream em `src/test/resources` (dados inventados, sem credenciais reais).

## 10. Fases

0. **Plano:** a pasta ainda não é um repositório git: rode `git init`, adicione o remote `origin` = https://github.com/wpdas/prime-tv.git e mantenha o `.gitignore` existente (ele já bloqueia `local.properties`, onde está a `TMDB_API_KEY`). Estrutura do projeto, `CLAUDE.md` com as convenções, `.gitignore`, README inicial. Me mostre a estrutura antes de seguir.
1. Projeto base: tema, escala 1280, i18n, navegação, componentes, manifest para launcher comum e Android TV. Tela de Início com dados fake navegável por D-pad.
2. Dados: detecção M3U/Xtream, parsers, Room + FTS, tela de Configuração inicial com progresso.
3. Telas: Início com dados reais, Filmes, Séries, Detalhes, Ao vivo com EPG, Futebol, Busca.
4. Player (Media3), histórico, Continuar assistindo e favoritos.
5. TMDB e recomendações.
6. Configurações, atualização em segundo plano, controle parental, tela Receber link (UI + fake).
7. Atualização via GitHub, assinatura, workflows de CI/release.
8. Polimento: desempenho em box fraca, estados de erro e vazio, revisão de foco em todas as telas.
