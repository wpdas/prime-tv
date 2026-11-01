package com.wpdas.primetv.domain.model

/** Abas da barra superior. [labelKey] é a chave de i18n. */
enum class HomeTab(val labelKey: String) {
    HOME("nav.home"),
    LIVE("nav.live"),
    MOVIES("nav.movies"),
    SERIES("nav.series"),
    FOOTBALL("nav.football"),
    MY_LIST("nav.my_list"),
}
