package com.feige.snippetstudio.ui.nav

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Files : Screen("files")
    object Settings : Screen("settings")

    object Editor : Screen("editor/{id}?type={type}") {
        fun new(type: String) = "editor/new?type=$type"
        fun edit(id: String) = "editor/$id"
    }

    object Detail : Screen("detail/{id}") {
        fun of(id: String) = "detail/$id"
    }

    object SubPage : Screen("sub/{key}") {
        fun of(key: String) = "sub/$key"
    }
}
