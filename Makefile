# Atalhos. Rode sempre da raiz do projeto: `make dev`, `make up`, `make teste`.
#
# `npm run dev` na raiz NAO funciona: o package.json mora em frontend/.
# Estes alvos existem para voce nunca mais precisar lembrar disso.

.PHONY: ajuda dev banco up down logs teste tokens limpar

ajuda:
	@echo "make dev     - sobe SO o banco e diz o que rodar nos outros dois terminais"
	@echo "               (depois disso o app fica em http://localhost:3000)"
	@echo "make up      - tudo no docker, como em producao (http://localhost, porta 80)"
	@echo "               neste modo a porta 3000 NAO responde: quem atende e o Caddy"
	@echo "make down    - desliga o que esta no docker"
	@echo "make logs    - acompanha o log dos containers"
	@echo "make teste   - testes do backend e build do frontend"
	@echo "make tokens  - regera o CSS a partir do design system"
	@echo "make limpar  - apaga os containers E o banco local (comeca do zero)"

banco:
	docker compose up -d db

## Desenvolvimento: dois terminais.
##
## Este alvo sobe SO o banco. Ele nao inicia o backend nem o frontend — os dois
## comandos abaixo sao seus, em outros terminais. Enquanto voce nao rodar o (2),
## a porta 3000 responde "conexao recusada", e isso nao e defeito.
dev: banco
	@echo
	@echo "  Banco de pe na porta 5433. O backend e o frontend NAO subiram:"
	@echo "  os dois comandos abaixo sao seus, cada um no seu terminal."
	@echo
	@echo "    1)  cd backend  && ./mvnw spring-boot:run     -> http://localhost:8080"
	@echo "    2)  cd frontend && npm install && npm run dev -> http://localhost:3000"
	@echo
	@echo "  Com os dois de pe, use http://localhost:3000 (o Next fala com o Spring)."
	@echo "  Antes disso, a 3000 recusa conexao — nao e defeito, e so que ninguem subiu."
	@echo
	@echo "  Se o que voce quer e a pilha inteira sem abrir terminal nenhum:"
	@echo "    make up   ->  http://localhost   (porta 80, via Caddy; a 3000 nao responde)"
	@echo

up:
	docker compose up -d --build
	@echo "Pronto: http://localhost"

down:
	docker compose down

logs:
	docker compose logs -f --tail=100

teste:
	cd backend && ./mvnw -B verify
	cd frontend && npm run lint && npm run build

tokens:
	cd frontend && npm run tokens

## Apaga o banco local junto. Use quando quiser comecar do zero.
limpar:
	docker compose down -v
