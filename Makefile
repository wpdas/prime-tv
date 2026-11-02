# PrimeTV — atalhos de desenvolvimento.
# Uso: make <alvo>. `make help` lista os alvos.
#
# Variáveis úteis:
#   SERIAL=<id adb>   escolhe o aparelho quando há mais de um (emulador + box)
#   AVD=<nome>        emulador a iniciar em `make emulator`

GRADLEW   := ./gradlew
APP_ID    := com.wpdas.primetv
ADB       := adb $(if $(SERIAL),-s $(SERIAL),)
AVD       ?= $(shell emulator -list-avds 2>/dev/null | head -1)
DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk

.DEFAULT_GOAL := help
.PHONY: help build release install-release test lint check clean install run uninstall emulator devices logcat avds tag keystore-base64

help: ## Lista os alvos
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

build: ## Compila o APK debug
	$(GRADLEW) assembleDebug

release: ## Compila o APK release minificado (assina com keystore.properties; sem ele, chave de debug para teste)
	$(GRADLEW) assembleRelease

install-release: release ## Instala o APK release no aparelho conectado
	$(ADB) install -r app/build/outputs/apk/release/app-release.apk

tag: ## Cria e envia a tag de release: make tag V=1.0.0
	@test -n "$(V)" || (echo "Uso: make tag V=1.0.0"; exit 1)
	git tag -a v$(V) -m "PrimeTV v$(V)" && git push origin v$(V)

keystore-base64: ## Imprime o keystore em base64 para o secret KEYSTORE_BASE64: make keystore-base64 KS=caminho/primetv.jks
	@test -n "$(KS)" || (echo "Uso: make keystore-base64 KS=caminho/primetv.jks"; exit 1)
	@base64 -i $(KS) | tr -d '\n'; echo

build-old: ## APK debug fingindo ser a versão V (padrão 0.0.1), para testar a atualização: make build-old V=0.0.1
	$(GRADLEW) assembleDebug -PversionNameOverride=$(or $(V),0.0.1)

test: ## Roda os testes unitários
	$(GRADLEW) testDebugUnitTest

lint: ## Roda o lint do Android
	$(GRADLEW) lintDebug

check: test lint ## Testes + lint (o que o CI roda)

clean: ## Limpa as saídas de build
	$(GRADLEW) clean

install: build ## Compila e instala o APK debug no aparelho/emulador conectado
	$(ADB) install -r $(DEBUG_APK)

run: install ## Instala e abre o app
	$(ADB) shell monkey -p $(APP_ID) -c android.intent.category.LAUNCHER 1 >/dev/null

uninstall: ## Remove o app do aparelho
	$(ADB) uninstall $(APP_ID)

logcat: ## Logs só do app
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(APP_ID))

devices: ## Lista aparelhos conectados
	adb devices -l

avds: ## Lista emuladores disponíveis
	emulator -list-avds

emulator: ## Inicia o emulador (AVD=<nome>, padrão: o primeiro da lista)
	@test -n "$(AVD)" || (echo "Nenhum AVD encontrado. Crie um no Android Studio (Device Manager)."; exit 1)
	emulator -avd $(AVD) -no-boot-anim >/dev/null 2>&1 &
