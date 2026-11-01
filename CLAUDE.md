# PrimeTV — guia para o Claude Code

App Android de IPTV com visual estilo Netflix para **TV Box com Android 11 comum** (não é Android TV), usada com controle remoto (D-pad). APK instalado por sideload, sem Play Store.

**Especificação completa:** `docs/prompt-claude-code.md` (fonte da verdade do que construir e em que fase).
**Design:** `design/README.md` (tokens) e `design/prototipos/*.dc.html` (layout e textos de cada tela).
**Roadmap:** `docs/roadmap.md`.

## Comandos

```bash
make build      # ./gradlew assembleDebug
make test       # ./gradlew testDebugUnitTest
make check      # testes + lint
make install    # instala o APK debug no aparelho/emulador (SERIAL=xxx para escolher)
make run        # instala e abre
make help       # todos os alvos
```

Ao terminar cada fase: build passando, testes rodados, commit e resumo curto antes de seguir.

## Stack (versões em `gradle/libs.versions.toml`)

Kotlin 2.4 (built-in do AGP) · AGP 9.5 · Gradle 9.6 · Jetpack Compose (BOM) + `androidx.tv:tv-material` · Navigation Compose · Hilt 2.60 (via KSP) · Coroutines/Flow · Media3 · Room (FTS4) · DataStore · Tink · WorkManager · OkHttp + Retrofit + kotlinx.serialization · Coil 3 · ZXing.

`minSdk 24`, `targetSdk 34`, `compileSdk 37`. ABIs `armeabi-v7a` + `arm64-v8a` num APK universal. Nada de Play Services, Firebase ou WebView.

## Estrutura

```
prime-tv/
├── app/
│   ├── build.gradle.kts            # único módulo; versão vem da tag git (vX.Y.Z)
│   ├── proguard-rules.pro
│   ├── schemas/                    # schemas do Room exportados pelo KSP
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── fonts/          # Inter 400/500/600/700
│       │   │   └── i18n/           # pt-BR.json, en.json... (textos da UI)
│       │   ├── java/com/wpdas/primetv/
│       │   │   ├── PrimeTvApp.kt   # @HiltAndroidApp
│       │   │   ├── MainActivity.kt # single activity
│       │   │   ├── core/           # transversal: i18n, di, util, dispatchers, log seguro
│       │   │   ├── data/           # implementação: local (Room, DataStore), remote (Xtream, M3U, TMDB, GitHub), repository, worker
│       │   │   ├── domain/         # modelos, interfaces de repositório, casos de uso (puro Kotlin)
│       │   │   └── ui/             # theme, components, navigation, screens/<tela>/ (Screen + ViewModel + UiState)
│       │   └── res/                # só o indispensável: tema da janela, ícone, banner, xml de rede
│       ├── test/                   # unitários (JUnit4, MockK, Turbine, MockWebServer)
│       │   ├── kotlin/com/wpdas/primetv/
│       │   └── resources/          # fixtures M3U e JSON Xtream (dados inventados)
│       └── androidTest/
├── design/                         # protótipos e tokens (não editar sem alinhar)
├── docs/                           # prompt/especificação e roadmap
├── gradle/libs.versions.toml       # version catalog
├── Makefile                        # atalhos de dev
├── CLAUDE.md · README.md · .gitignore
├── local.properties                # TMDB_API_KEY (ignorado)
└── keystore.properties             # assinatura release (ignorado)
```

Telas (pacotes em `ui/screens/`): `setup`, `receivelink`, `home`, `details`, `live`, `search`, `player`, `appmenu`, `settings`.

## Convenções

**Arquitetura**
- MVVM: `Screen` (Compose, sem lógica) ← `ViewModel` (Hilt, expõe um `StateFlow<UiState>`) ← casos de uso/repositórios do `domain` ← implementações em `data`.
- `domain` não depende de Android nem de `data`. `ui` não acessa `data` diretamente.
- Tudo assíncrono com Coroutines/Flow; `Dispatchers` injetados para testar.
- Nada pesado na main thread: parse de lista em streaming, inserções no Room em lotes dentro de transação, listas e grades sempre lazy.

**UI / TV**
- Design base **1280×720**. O `PrimeTheme` instala um `Density` com `density = larguraPx / 1280f` e `fontScale = 1f`; use as medidas dos protótipos direto em `dp`.
- Tokens só do `PrimeTheme` (cores, tipografia Inter, formas). Nunca cor literal em tela.
- Tudo focável pelo D-pad, ordem previsível, foco lembrado ao voltar para uma linha. Foco visível: anel 3px `#8B5CF6` (ou `#F2F2F2` sobre violeta) + escala ~1.05. Voltar sempre volta um nível. Suportar também ponteiro/air mouse e toque.
- Componentes reutilizáveis ficam em `ui/components` (`PrimeButton`, `PosterCard`, `WideCard`, `ChannelRow`, `CategoryRail`, `Chip`, `LoadingSpinner`, `Toast`).
- Ao focar um card que muda o destaque, debounce de ~400 ms.

**Textos / i18n**
- **Não usar `strings.xml`** para textos da UI. Textos vivem em `assets/i18n/<idioma>.json` com chaves planas (`"home.continue_watching"`), lidos por `I18n` com fallback para pt-BR, expostos via `CompositionLocal` e `t("chave")`.
- Toda mensagem de erro em português, clara, sempre com uma ação ("Tentar de novo", "Trocar lista").

**Segurança**
- Nunca logar URL da lista, usuário, senha ou chave do TMDB. Interceptor de log, se houver, só em debug e com redação.
- URL da lista persistida criptografada (Tink). Segredos só em `local.properties` / `keystore.properties` / secrets do CI.
- Fixtures de teste com dados inventados.

**Código**
- Kotlin idiomático, `val` por padrão, sem `!!`. Nomes em inglês no código; textos ao usuário, comentários e commits em português.
- Dependências só pelo version catalog. Sem `strings.xml`, `WebView`, Play Services ou Firebase.
- Testes unitários obrigatórios para: parser M3U, cliente Xtream, classificação de futebol, matching TMDB ↔ lista, comparação de versões, i18n.
- Commits pequenos e descritivos (`feat:`, `fix:`, `chore:`, `docs:`, `test:`), em português.

## Fases

0 Plano/estrutura · 1 Projeto base (tema, escala, i18n, navegação, componentes, Início fake) · 2 Dados (M3U/Xtream, Room, Setup) · 3 Telas · 4 Player/histórico/favoritos · 5 TMDB · 6 Configurações/atualização em 2º plano/parental/Receber link · 7 GitHub Releases/assinatura/CI · 8 Polimento.

Se algo da especificação for ambíguo ou difícil de desfazer, pergunte antes.
