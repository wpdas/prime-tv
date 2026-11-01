# PrimeTV

App Android de IPTV com interface no estilo Netflix, feito para **TV Box com Android 11 comum** (não precisa ser Android TV) e uso com controle remoto. Suporta listas **M3U** e **Xtream Codes**, EPG, busca, recomendações (TMDB), aba de futebol e atualização automática pelo GitHub Releases.

> Em desenvolvimento. Veja `docs/roadmap.md` e `docs/prompt-claude-code.md`.

## O que já funciona

- Configuração inicial com detecção automática de lista M3U ou Xtream Codes e importação por seção (Ao vivo libera primeiro; Filmes, Séries e Guia seguem em segundo plano)
- Início com destaque rotativo, linhas por categoria, "Futebol agora" e abas Filmes, Séries e Futebol
- Ao vivo com categorias, guia de programação (XMLTV) com programa atual, progresso e "A seguir"
- Detalhes de filmes e séries, com temporadas e episódios carregados sob demanda
- Busca com teclado para controle remoto, voz (quando houver) e sugestões instantâneas
- Player (HLS, MPEG-TS, MP4/MKV) com ±10 s, áudio e legendas, próximo episódio automático, troca de canal com cima/baixo e prévia sem áudio na tela Ao vivo
- Histórico com "Continuar assistindo" e favoritos (Minha lista, categoria Favoritos em Ao vivo, segurar OK no canal)
- Recomendações do TMDB (em alta, populares, em cartaz) só com o que existe na sua lista, com backdrop, sinopse em português, gêneros, nota e trailer no YouTube; cache local para funcionar offline
- Arte gerada para jogos de futebol sem imagem (escudos estilizados "A × B")
- Tudo navegável só com o D-pad, foco visível e interface escalada para qualquer resolução

Ainda por vir: configurações, atualização automática em segundo plano, controle parental, "Receber link pelo celular" e releases assinadas.

## Telas

| Configuração inicial | Início |
|---|---|
| ![Configuração inicial](docs/screenshots/01-configuracao.jpg) | ![Início](docs/screenshots/02-inicio.jpg) |

| Filmes | Séries |
|---|---|
| ![Filmes](docs/screenshots/03-filmes.jpg) | ![Séries](docs/screenshots/04-series.jpg) |

| Detalhes de série | Detalhes de filme |
|---|---|
| ![Detalhes de série](docs/screenshots/05-detalhes-serie.jpg) | ![Detalhes de filme](docs/screenshots/09-detalhes-filme.jpg) |

| Ao vivo com guia | Futebol |
|---|---|
| ![Ao vivo](docs/screenshots/07-ao-vivo.jpg) | ![Futebol](docs/screenshots/06-futebol.jpg) |

| Busca | Categoria completa |
|---|---|
| ![Busca](docs/screenshots/08-busca.jpg) | ![Categoria](docs/screenshots/10-categoria.jpg) |

| Player ao vivo | Player com episódios |
|---|---|
| ![Player ao vivo](docs/screenshots/11-player-ao-vivo.jpg) | ![Player com episódios](docs/screenshots/12-player-episodios.jpg) |

| Sobre (atribuição TMDB) | |
|---|---|
| ![Sobre](docs/screenshots/13-sobre.jpg) | |

## Requisitos para compilar

- JDK 17
- Android SDK com `platforms;android-37` e `build-tools;36.0.0` (o Android Studio instala)
- `local.properties` com `sdk.dir` e, opcionalmente, `TMDB_API_KEY=...` (sem a chave, o destaque usa itens da própria lista)

```bash
make build          # APK debug em app/build/outputs/apk/debug/
make test           # testes unitários
make install        # instala no aparelho/emulador conectado via adb
make help           # todos os atalhos
```

## Instalar na TV Box

O APK universal (`armeabi-v7a` + `arm64-v8a`) é publicado em cada release do GitHub.

- **Pendrive:** copie o `.apk` para um pendrive, abra o gerenciador de arquivos da box e toque no arquivo. Se pedir, autorize "Instalar apps desconhecidos" para o gerenciador.
- **Downloader / navegador:** baixe o `.apk` pelo link da release e abra o arquivo baixado.
- **adb:** com depuração USB/rede ativa na box:
  ```bash
  adb connect <ip-da-box>:5555
  adb install -r primetv-vX.Y.Z.apk
  ```

Depois da primeira instalação, o próprio app avisa quando há versão nova e faz a atualização.

## Assinatura (release)

O APK release é assinado com um keystore próprio, fora do repositório. **Todas as versões precisam da mesma chave**, senão o Android recusa a atualização por cima da anterior.

Gerar o keystore uma única vez:

```bash
keytool -genkeypair -v -keystore primetv.jks -alias primetv \
  -keyalg RSA -keysize 2048 -validity 10000
```

Criar `keystore.properties` na raiz (já ignorado pelo git):

```properties
storeFile=../primetv.jks
storePassword=...
keyAlias=primetv
keyPassword=...
```

No CI, o keystore entra em base64 por secret (detalhes em `.github/workflows/release.yml`, fase 7).

## Versionamento

`versionName` e `versionCode` são derivados da tag git `vX.Y.Z`. Criar a tag e dar push dispara a release com o APK.

## Créditos

Este produto usa a API do TMDB, mas não é endossado ou certificado pelo TMDB.
