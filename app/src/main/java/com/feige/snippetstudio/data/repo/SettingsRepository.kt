package com.feige.snippetstudio.data.repo

import com.feige.snippetstudio.data.local.SettingsDataStore
import com.feige.snippetstudio.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * [SettingsRepository] 是偏好设置仓库类。
 *
 * 架构原理：
 * 遵循 Repository 仓库设计模式，作为唯一可信数据源（Single Source of Truth）。
 * 将 DataStore 的具体 API 封装隔离在数据层内部，未来即使将 DataStore 替换为 MMKV 或服务器 API，ViewModel 层的代码亦无需任何更改。
 *
 * @param settingsDataStore 注入的底层数据源 [SettingsDataStore]
 */
class SettingsRepository(private val settingsDataStore: SettingsDataStore) : ISettingsRepository {

    /**
     * 实时可观察的全局应用设置 Flow 数据流。
     * 直接透传 DataStore 的映射流，供 UI / ViewModel 无缝订阅。
     */
    override val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow

    /**
     * 挂起函数：更新应用偏好设置。
     *
     * @param transform 偏好设置修改高阶闭包：传入旧 AppSettings 对象，返回期望保存的新对象
     */
    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsDataStore.update(transform)
    }
}


