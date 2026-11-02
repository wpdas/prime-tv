# Changelog

Formato: uma seção por versão, com **Novidades** e **Correções**. A seção `[Não lançado]` recebe o que entra na `main`; ao criar a tag, o workflow de release copia a seção da versão para a descrição da release, que é o texto mostrado no app em "Verificar atualização".

## [Não lançado]

### Novidades
- Destaque do topo nas abas Kids e Anime passa a ser do próprio contexto: em Anime, Dragon Ball e Naruto/Boruto mais recentes que a lista tiver, One Piece, os animes populares do TMDB presentes na lista e títulos das categorias de anime sorteados por dia; em Kids, os desenhos populares do TMDB presentes na lista e títulos das categorias infantis sorteados por dia
- Menu Qualidade: a versão sem tag no nome da lista aparece como "Padrão", com a resolução real quando está tocando ("Padrão · 1080p"), em vez de "—"

### Correções
- Aviso automático de versão nova: a checagem silenciosa ao abrir o app passa de 6 h para 30 min de intervalo mínimo, então uma release publicada aparece na próxima abertura
- Com os controles do player escondidos, ← e → só mostram os controles; o avanço de 10 s fica na barra de progresso, acabando com o pulo acidental ao pegar o controle
- Trilho "Vídeos" removido da tela de Futebol: puxava filmes sem relação com esporte

## [0.0.5] - 2026-11-02

### Novidades
- Aba Futebol abre o Guia de jogos: partidas do dia com horário, escudos dos times, competição e canal, montadas a partir dos canais de evento da lista ("Time x Time - 16:45") e do guia de programação. Trilho com Jogos, Ao vivo agora, Canais e Vídeos; OK abre o canal. Reprises e anúncios de jogos futuros ficam de fora
- Competição e escudos vêm da base pública TheSportsDB (o que a lista informa, como "Série B", tem prioridade); são pré-carregados aos poucos ao abrir o app e ficam guardados por 30 dias
- Na Início, "Futebol agora" virou "Jogos de hoje": ao vivo primeiro, depois os próximos, com horário e competição, e um card "Ver guia de jogos"
- Abas Kids e Anime na barra superior quando a lista tem categorias infantis ou de anime (uma linha por categoria; em Kids, também as séries marcadas como "Kids" pela lista)
- Menu Qualidade no player (ao vivo e filmes): Auto e as resoluções do stream quando ele é adaptativo, e as versões da lista (4K/FHD/HD/SD) do mesmo canal ou filme; a escolha manual desliga a troca automática por conexão lenta
- Velocidade de download do stream aparece junto do spinner enquanto carrega
- Ao subir da lista para a barra superior, o foco cai sempre na aba ativa, sem trocar de seção sem querer
- O logo deixa de ser botão; o menu do app (atualização, configurações, trocar lista, sobre) fica no ícone ⋯ à direita
- Atualização automática de Ao vivo e do Guia passa a ser a cada 6 h por padrão (os canais de jogo mudam ao longo do dia); quem já escolheu outro intervalo mantém a escolha

## [0.0.4] - 2026-11-02

### Novidades
- Na Início, Voltar mostra "Aperte Voltar de novo para sair" e o segundo toque fecha o app de verdade
- Segurar ↑/↓/←/→ no controle anda no ritmo da tela: repetições que chegam antes de a rolagem terminar são ignoradas, e o foco não some mais da área visível em boxes mais lentas

### Correções
- Logo após atualizar o app, Início, Filmes e Séries ficavam vazias até a reimportação terminar (minutos numa internet lenta). Causa: os itens ainda sem chave de agrupamento eram tratados como um grupo só e sumiam atrás de um único título. Agora cada um deles conta como item próprio e tudo continua visível enquanto a lista é atualizada por trás
- Importação lê a lista inteira da rede antes de gravar, numa transação curta: a tela nunca vê a tabela vazia, uma queda de rede não toca no banco e favoritos/histórico não ficam presos esperando o download; o guia de programação é baixado para o cache antes de entrar no banco
- Banco sempre em modo WAL: em aparelhos marcados como "low RAM", o modo automático deixava toda leitura da interface esperando a importação terminar

## [0.0.3] - 2026-11-02

### Novidades
- Diálogo de atualização abre sozinho quando há versão nova, com o changelog rolável e os botões sempre visíveis
- Ao voltar do player para a tela Ao vivo, o foco fica no canal que estava em tela cheia
- Ao vivo reabre na última categoria e canal visitados, já com a prévia
- Abas do topo e seções das Configurações mudam ao receber o foco, sem precisar de OK
- Conexão lenta: se o vídeo travar 3 vezes em 1,5 min, o player troca sozinho para a qualidade abaixo do mesmo filme/canal (mantendo a posição) ou libera o ajuste automático em streams adaptativos; buffers Normal e Alto ficaram mais tolerantes a oscilação

### Correções
- Agrupamento de variantes ficou estrito: só junta nomes iguais tirando a tag de qualidade (4K/FHD/HD/SD) e o marcador de servidor repetido. "HBO+", "HBO Legendado", filmes "(L)" e títulos com ano deixam de ser engolidos por parecidos; a lista é reimportada em segundo plano uma vez após atualizar
- Deduplicação de qualidade corrigida em todo lugar (Ao vivo, categorias, Futebol, Filmes, busca e contagens): só colapsa dentro do escopo mostrado e só quando o grupo tem alguma tag de qualidade. Antes, canais sumiam de TELECINE/HBO e jogos repetidos do Brasileirão apareciam uma vez só
- Atualizar o app não apaga mais favoritos, histórico nem a lista; quando a estrutura do banco muda, a lista é reimportada em segundo plano
- Voltar fecha o painel de Áudio/Legendas/Episódios de uma vez
- Texto literal "$input_" ao digitar número de canal
- Depois de ir a um canal pelo número, cima/baixo continuam a partir dele
- Anel de foco não é mais cortado nas bordas das listas (Início, grade, trilho, Ao vivo)

## [0.0.2] - 2026-11-02

### Novidades
- Canais e filmes repetidos em várias qualidades aparecem uma vez só, na melhor (4K/UHD, FHD, HD, SD), com selo da qualidade
- Se a melhor qualidade de um canal falhar, o player troca sozinho para a seguinte
- Barra de tempo navegável no player: segure ◀ ▶ para avançar mais rápido (10 s → 30 s → 1 min → 2 min)
- No ao vivo: cima/baixo trocam de canal, OK pausa, números do controle vão direto ao canal, esquerda/direita abrem Áudio e Legendas
- Digitar o número do canal na tela Ao vivo foca o canal na lista
- Grade de categoria com busca dentro da categoria, ordenação (Recentes, A–Z, Ano) e filtro por gênero quando a lista informa
- Margem de segurança (overscan) em Configurações › Player para TVs que cortam as bordas
- Opção de som na prévia de canais

### Correções
- Ao voltar para a Início, o foco fica no mesmo card de onde você saiu, sem rolar ao topo
- Primeiro card da linha volta alinhado à margem
- Painel de Episódios abre com o foco no episódio atual
- "Futebol agora" abre direto o canal do jogo
- Prefixos como "VT -" e "AO VIVO:" não entram mais no nome dos times
- Player pede sempre a maior qualidade dos streams adaptativos
- Trilho de categorias do Ao vivo mais largo, com nomes em duas linhas

## [0.0.1] - 2026-11-02

Primeira versão: configuração de lista M3U/Xtream, Início com recomendações do TMDB, Ao vivo com guia, filmes, séries, busca, player, histórico, favoritos, Configurações, atualização em segundo plano e atualização do próprio app pelo GitHub Releases.
