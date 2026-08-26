package com.example.cabinguard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class được đánh dấu bằng @HiltAndroidApp để khởi tạo
 * Hilt dependency injection container cho toàn bộ ứng dụng.
 *
 * Đây là entry point của Hilt — tất cả các component sẽ được
 * inject thông qua class này.
 */
@HiltAndroidApp
class CabinGuardApplication : Application()
