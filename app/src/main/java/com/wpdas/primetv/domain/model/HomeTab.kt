package com.wpdas.primetv.domain.model

/** Abas da barra superior. [labelKey] é a chave de i18n. */
enum class HomeTab(val labelKey: String) {
    HOME("nav.home"),
    LIVE("nav.live"),
    MOVIES("nav.movies"),
    SERIES("nav.series"),
    /** Só aparece quando a lista tem categorias de novelas. */
    NOVELAS("nav.novelas"),
    FOOTBALL("nav.football"),
    /** Só aparece quando a lista tem categorias infantis (ver TabKeywords). */
    KIDS("nav.kids"),
    /** Só aparece quando a lista tem categorias de anime. */
    ANIME("nav.anime"),
    MY_LIST("nav.my_list"),
    ;

    companion object {
        /** Abas que dependem do conteúdo da lista. */
        val OPTIONAL: Set<HomeTab> = setOf(NOVELAS, KIDS, ANIME)
    }
}
