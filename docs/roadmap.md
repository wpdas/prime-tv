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

### Agrupar variantes de resolução do mesmo item
Listas costumam repetir o mesmo canal/filme em várias qualidades (`<nome> FHD`, `<nome> HD`, `<nome> SD`). Exibir só uma entrada, agrupando as variantes pelo nome normalizado (sem o sufixo de qualidade).

Estratégia de qualidade (a investigar antes de implementar):
- Reproduzir sempre a melhor variante (FHD) por padrão.
- Opção A — automático: se o player detectar lentidão (rebuffering seguido), descer FHD → HD → SD; se a conexão melhorar, subir de volta HD → FHD.
- Opção B — assistido: ao detectar lentidão, mostrar um diálogo perguntando se o usuário quer tentar uma resolução menor (talvez mais previsível e barato em box fraca).
- Decidir entre A, B ou as duas (automático com opção de desligar) depois de testar em box real.

### Ao vivo: ocultar TV aberta em "Todos"
Configuração em Ao vivo / Configurações para definir se a categoria "Todos" inclui ou exclui os canais de TV aberta. A maioria dos usuários de TV Box já tem esses canais na TV comum e não quer vê-los misturados. Precisa de um critério para identificar TV aberta (categoria da lista, palavras-chave configuráveis como "abertos", "aberta", "TV aberta", nomes de emissoras) e de um padrão sensato (sugestão: ocultar).

### Arte para jogos de futebol sem imagem
Canais e eventos "Time A x Time B" quase nunca têm capa. Montar a arte em tempo de execução: escudo do time A, um "×" e o escudo do time B, sobre o fundo do app. Pacote de escudos embutido para os clubes mais comuns (Brasil e Europa) com apelidos ("Fla", "Atlético-MG", "São Paulo"); sem escudo conhecido, usar as iniciais em um círculo com cor derivada do nome. Entra junto com as imagens do TMDB (fase 5).

### Destaque com imagem em toda a largura
Hoje o destaque da Início segue o protótipo: texto à esquerda, imagem à direita. Com os backdrops horizontais do TMDB (fase 5), avaliar a imagem ocupando toda a largura com o texto sobre um degradê escuro, estilo Netflix. Com os pôsteres verticais da lista isso ficaria cortado, por isso a decisão fica para a fase 5.

### Intro animada com logo e som
Ao abrir o app, exibir uma vinheta curta (2–3 s) com a animação do logo PrimeTV e um áudio de fundo, no espírito da abertura da Netflix. Depende de dois ativos que ainda não existem: a logo em formato animável (vetor/Lottie ou sequência de quadros) e uma trilha curta com licença para uso. Deve ser pulável com qualquer tecla e não atrasar o carregamento da lista.

## Ideias para depois

- Mais idiomas (arquivos JSON de tradução).
- Perfis de usuário (histórico e favoritos separados).
