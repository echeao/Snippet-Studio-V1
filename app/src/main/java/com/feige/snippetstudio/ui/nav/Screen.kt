package com.feige.snippetstudio.ui.nav

/**
 * [Screen] 密封类 (Sealed Class) 定义了应用程序所有的页面导航路由 (Navigation Routes)。
 *
 * 密封类特性：限制路由类型，保证在 [AppNavGraph] 构建路由映射时的类型安全。
 *
 * @param route 路由匹配模板字符串
 */
sealed class Screen(val route: String) {
    /** 首页：片段列表、分类过滤与快捷动作 */
    object Home : Screen("home")

    /** 文件与仓库页：按文件夹树结构查看与 Git 仓同步 */
    object Files : Screen("files")

    /** 设置页：系统语言、主题模式、编辑器样式与 Git 账号配置 */
    object Settings : Screen("settings")

    /** 编辑器页面：新建代码片段或修改既有片段 */
    object Editor : Screen("editor/{id}?type={type}") {
        /** 构建新建指定类型片段的路由路径 */
        fun new(type: String) = "editor/new?type=$type"
        /** 构建编辑已有片段的路由路径 */
        fun edit(id: String) = "editor/$id"
    }

    /** 片段详情查看页面 */
    object Detail : Screen("detail/{id}") {
        /** 构建查看指定 ID 片段详情的路由路径 */
        fun of(id: String) = "detail/$id"
    }

    /** 通用子设置页面（如 Git 配置页、关于页） */
    object SubPage : Screen("sub/{key}") {
        /** 构建子页面路由路径 */
        fun of(key: String) = "sub/$key"
    }
}

