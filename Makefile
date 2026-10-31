# Atalhos. Rode sempre da raiz do projeto: `make dev`, `make up`, `make teste`.
#
# `npm run dev` na raiz NAO funciona: o package.json mora em frontend/.
# Estes alvos existem para voce nunca mais precisar lembrar disso.

.PHONY: ajuda dev banco up down logs teste tokens limpar

ajuda:
	@echo "make dev     - banco no docker + backend e frontend com recarga automatica"
	@echo "make up      - tudo no docker, como em producao (http://localhost)"
	@echo "make down    - desliga o que esta no docker"
	@echo "make logs    - acompanha o log dos containers"
	@echo "make teste   - testes do backend e build do frontend"
	@echo "make tokens  - regera o CSS a partir do design system"
	@echo "make limpar  - apaga os containers E o banco local (comeca do zero)"

banco:
	docker compose up -d db

## Desenvolvimento: dois terminais.
dev: banco
	@echo
	@echo "  Banco de pe na porta 5433."
	@echo
	@echo "  Agora abra DOIS terminais:"
	@echo "    1)  cd backend  && ./mvnw spring-boot:run     -> http://localhost:8080"
	@echo "    2)  cd frontend && npm install && npm run dev -> http://localhost:3000"
	@echo
	@echo "  Use http://localhost:3000. O Next fala com o Spring sozinho."
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
