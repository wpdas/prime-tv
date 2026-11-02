# PrimeTV — Roadmap

## Fase 1 — App (em andamento)

- [x] Paleta e design das telas (ver `design/README.md`)
- [x] Prompt e especificação para implementação no Claude Code
- [ ] Implementação do app Android (TV Box, Android 11, sem assinatura de loja)
- [ ] Repositório no GitHub com releases do APK (usado pela verificação de atualização do app)

## Fase 2 — Site "Receber link" (issue futura)

**Problema:** o usuário recebe o link da lista IPTV pelo WhatsApp ou outro app no celular. Digitar ou copiar esse link na TV Box com o controle remoto é difícil.

**Solução:** um site simples onde o usuário envia o link do celular para a TV.

Fluxo:
1. No app, o usuário escolhe **Receber link pelo celular** (tela de configuração inicial e em Configurações › Lista IPTV).
2. O app gera um código curto e temporário (ex.: `K7P-4QX`, válido por ~10 min) e mostra também um QR code que abre o site com o código já preenchido.
3. No site, o usuário digita o código. Se for válido, o site libera o campo para colar o link.
4. Ao enviar, o app recebe o link e mostra a confirmação ("Link recebido · Detectado: Xtream Codes") com o botão **Usar este link**.

Entrega do link ao app (decidir na implementação):
- WebSocket, usando o serviço de WebSocket que já existe; ou
- polling simples (o app consulta a cada poucos segundos enquanto a tela está aberta).

Requisitos a considerar:
- Código de uso único, com expiração e limite de tentativas (evitar força bruta).
- O link contém usuário e senha: trafegar só por HTTPS, não registrar em log e apagar do servidor assim que o app confirmar o recebimento.
- Domínio do site: [a definir].

Critérios de aceite:
- Código expira e pode ser regenerado pela TV.
- Link enviado chega ao app em poucos segundos.
- Nenhum link fica guardado no servidor após a entrega.

## Melhorias planejadas (abrir como issues após o primeiro push)

### Agrupar variantes de resolução do mesmo item ✅ parcial (fase 8)
Feito: as listas mostram só a melhor variante de cada canal/filme (4K/UHD → FHD → HD → SD), com selo da qualidade; se o canal falhar no player, cai para a variante seguinte. Pendente: troca automática por velocidade da internet durante a reprodução (e/ou diálogo sugerindo qualidade menor).

Listas costumam repetir o mesmo canal/filme em várias qualidades (`<nome> FHD`, `<nome> HD`, `<nome> SD`, `<nome> UHD`, `<nome> 4K`). Exibir só uma entrada, agrupando as variantes pelo nome normalizado. A tag de qualidade pode vir como sufixo ou prefixo (`UHD Globo`, `Globo UHD 4K`) e `UHD` e `4K` são a mesma coisa; a ordem de preferência é 4K/UHD → FHD → HD → SD. O normalizador de títulos (`TextNormalizer`) já remove essas tags e serve de base.

Estratégia de qualidade (a investigar antes de implementar):
- Reproduzir sempre a melhor variante (FHD) por padrão.
- Opção A — automático: se o player detectar lentidão (rebuffering seguido), descer FHD → HD → SD; se a conexão melhorar, subir de volta HD → FHD.
- Opção B — assistido: ao detectar lentidão, mostrar um diálogo perguntando se o usuário quer tentar uma resolução menor (talvez mais previsível e barato em box fraca).
- Decidir entre A, B ou as duas (automático com opção de desligar) depois de testar em box real.

### Ao vivo: ocultar TV aberta em "Todos"
Configuração em Ao vivo / Configurações para definir se a categoria "Todos" inclui ou exclui os canais de TV aberta. A maioria dos usuários de TV Box já tem esses canais na TV comum e não quer vê-los misturados. Precisa de um critério para identificar TV aberta (categoria da lista, palavras-chave configuráveis como "abertos", "aberta", "TV aberta", nomes de emissoras) e de um padrão sensato (sugestão: ocultar).

### Arte para jogos de futebol sem imagem ✅ (fase 5)
Feito com escudos estilizados gerados no app (sigla + cores do clube para os mais comuns do Brasil e da Europa; cores derivadas do nome para os demais) e um "×" no meio. Imagens reais de escudos ficaram de fora por serem marcas registradas. Melhoria futura: mais clubes no mapa de cores e apelidos.

### Destaque com imagem em toda a largura ✅ (fase 5)
Com backdrop do TMDB o destaque usa a imagem em toda a largura com o texto sobre um degradê; só com o pôster vertical da lista, mantém o layout do protótipo (texto à esquerda, imagem à direita).

### Controle parental (adiado)
Previsto na especificação (PIN de 4 dígitos, ocultar categorias adultas por palavras-chave e marcação manual), mas adiado: na prática o conteúdo adulto costuma vir habilitado ou não na própria lista, por definição de quem contratou com o provedor. Reavaliar se houver pedido dos usuários. Quando entrar: PIN no DataStore (hash), filtro `isAdult`/palavras-chave nas consultas e diálogo de PIN na tela Ao vivo e nas categorias.

### Intro animada com logo e som
Ao abrir o app, exibir uma vinheta curta (2–3 s) com a animação do logo PrimeTV e um áudio de fundo, no espírito da abertura da Netflix. Depende de dois ativos que ainda não existem: a logo em formato animável (vetor/Lottie ou sequência de quadros) e uma trilha curta com licença para uso. Deve ser pulável com qualquer tecla e não atrasar o carregamento da lista.

## Ideias para depois

- Mais idiomas (arquivos JSON de tradução).
- Perfis de usuário (histórico e favoritos separados).
