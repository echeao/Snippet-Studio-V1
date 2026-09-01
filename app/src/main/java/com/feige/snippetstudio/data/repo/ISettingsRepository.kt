package com.feige.snippetstudio.data.repo

import com.feige.snippetstudio.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * [ISettingsRepository] 是全局偏好设置仓库的抽象契约接口。
 *
 * 架构设计职责：
 * 1. 隔离 DataStore / MMKV / SharedPreferences 等具体存储实现。
 * 2. 暴露应用配置只读响应式流 [settingsFlow]，并提供原子修改方法 [updateSettings]。
 */
interface ISettingsRepository {

    /**
     * 实时可观察的全局应用设置 Flow 数据流。
     */
    val settingsFlow: Flow<AppSettings>

    /**
     * 挂起函数：更新应用偏好设置。
     *
     * @param transform 偏好设置修改高阶闭包：传入旧 AppSettings 对象，返回期望保存的新对象
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
}
