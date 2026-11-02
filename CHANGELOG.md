# Changelog

Formato: uma seção por versão, com **Novidades** e **Correções**. A seção `[Não lançado]` recebe o que entra na `main`; ao criar a tag, o workflow de release copia a seção da versão para a descrição da release, que é o texto mostrado no app em "Verificar atualização".

## [Não lançado]

### Novidades
- Diálogo de atualização abre sozinho quando há versão nova, com o changelog rolável e os botões sempre visíveis
- Ao voltar do player para a tela Ao vivo, o foco fica no canal que estava em tela cheia
- Ao vivo reabre na última categoria e canal visitados, já com a prévia
- Abas do topo e seções das Configurações mudam ao receber o foco, sem precisar de OK
- Conexão lenta: se o vídeo travar 3 vezes em 1,5 min, o player troca sozinho para a qualidade abaixo do mesmo filme/canal (mantendo a posição) ou libera o ajuste automático em streams adaptativos; buffers Normal e Alto ficaram mais tolerantes a oscilação
- Configurações › Player: margem de segurança (overscan) e som na prévia

### Correções
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
