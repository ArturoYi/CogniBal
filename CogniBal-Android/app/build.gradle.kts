plugins {
    alias(libs.plugins.android.application)
    // 处理 @Composable。需要下面的 buildFeatures.compose。
    alias(libs.plugins.kotlin.compose)
    // JSON 备份的 @Serializable。
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    // R 类与生成代码的包名。
    namespace = "com.example.cognibal"
    compileSdk {
        // 已发布的 Android SDK 37，用于编译。
        version = release(37)
    }

    defaultConfig {
        // 安装到设备上的应用 ID，可与 namespace 不同。
        applicationId = "com.example.cognibal"
        // Android 8.0。
        minSdk = 26
        // 运行时按此版本检查行为变更，与 compileSdk 对齐。
        targetSdk = 37
        // versionCode 给系统比较新旧，versionName 给人看。
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                // 关闭 R8 压缩与混淆，正式包保持可读，便于当前阶段排查。
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Room 把当前表结构导出到 app/schemas，供以后手写 Migration 对照。
// 不开启 autoMigrations。
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    // 只提供版本约束。后面没写版本号的 Compose 库跟它对齐。
    implementation(platform(libs.androidx.compose.bom))
    // ComponentActivity.setContent 的入口。
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.material3)
    // 界面图标统一用 Material Icons Extended，风格为 Rounded。
    implementation(libs.androidx.compose.material.icons.extended)
    // 按窗口宽度在底栏和侧栏之间切换。
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.navigation.suite)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    // 预览注解。检查器只在 debug 变体里，不进正式包。
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    // 系统启动页，交给 MainActivity 里的 StartupSplash。
    implementation(libs.androidx.core.splashscreen)

    // viewModel()，以及在 Compose 里收集 StateFlow。
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // 设置项的本地存储，由 AppPreferencesRepository 封装。
    implementation(libs.androidx.datastore.preferences)

    // 账单库。查询走 Room 自带的后台线程，仓库入口再切到 Dispatchers.IO。
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // 业务 JSON 导入导出，不负责 SQLite 文件本身。
    implementation(libs.kotlinx.serialization.json)

    // 全局返回栈。路由表在 navigation 包，设置二级页是其中的目的地。
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    // 宽屏把设置列表和二级页并排。窄屏仍一次只显示一页。
    implementation(libs.androidx.adaptive.navigation3)
}
