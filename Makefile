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
.PHONY: help build release test lint check clean install run uninstall emulator devices logcat avds

help: ## Lista os alvos
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

build: ## Compila o APK debug
	$(GRADLEW) assembleDebug

release: ## Compila o APK release (assina se houver keystore.properties)
	$(GRADLEW) assembleRelease

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
