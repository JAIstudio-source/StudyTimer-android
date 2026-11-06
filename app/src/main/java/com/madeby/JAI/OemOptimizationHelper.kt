package com.madeby.JAI

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Detects device manufacturer and provides tailored battery/autostart instructions
 * and direct intent dispatchers so background study timers run reliably without interruption.
 *
 * Written in clean, friendly language with zero engineering jargon.
 */
object OemOptimizationHelper {

    enum class OemBrand(
        val brandName: String,
        val steps: List<String>
    ) {
        XIAOMI(
            brandName = "Xiaomi / Redmi / POCO",
            steps = listOf(
                "Enable 'Autostart' for StudyTimer so your timer can wake you up.",
                "Set Battery Saver to 'No restrictions' to prevent sleep drops.",
                "In Recent Apps, press and hold StudyTimer and tap the Lock icon."
            )
        ),
        SAMSUNG(
            brandName = "Samsung Galaxy",
            steps = listOf(
                "Open App info → Battery and select 'Unrestricted'.",
                "Ensure StudyTimer is not added to 'Deep sleeping apps'.",
                "In Recent Apps, tap the StudyTimer icon and select 'Keep open'."
            )
        ),
        VIVO(
            brandName = "Vivo / iQOO",
            steps = listOf(
                "Allow StudyTimer in 'Autostart' management.",
                "Set Background power consumption to 'Allow high background power consumption'.",
                "In Recent Apps, swipe down on the StudyTimer card to lock it in memory."
            )
        ),
        HUAWEI(
            brandName = "Huawei / Honor",
            steps = listOf(
                "Open Battery → App Launch and set StudyTimer to 'Manage manually'.",
                "Turn ON: Auto-launch, Secondary launch, and Run in background.",
                "In Recent Apps, swipe down on StudyTimer to lock it."
            )
        ),
        ASUS(
            brandName = "Asus",
            steps = listOf(
                "Open Mobile Manager → Auto-start Manager and enable StudyTimer.",
                "Ensure Notification access and background execution are allowed."
            )
        ),
        OTHER(
            brandName = "Standard Android",
            steps = listOf(
                "Go to App info → Battery and choose 'Unrestricted'.",
                "Turn off 'Pause app activity if unused'.",
                "Ensure battery saver does not stop background notifications."
            )
        );

        fun openSettings(activity: Activity): Boolean {
            val pkg = activity.packageName
            val intents = mutableListOf<Intent>()

            when (this) {
                XIAOMI -> {
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartManagementActivity"
                            )
                        )
                    )
                    intents.add(Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").addCategory(Intent.CATEGORY_DEFAULT))
                }
                SAMSUNG -> {
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.samsung.android.lool",
                                "com.samsung.android.sm.ui.battery.BatteryActivity"
                            )
                        )
                    )
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.samsung.android.sm",
                                "com.samsung.android.sm.ui.battery.BatteryActivity"
                            )
                        )
                    )
                }
                VIVO -> {
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.vivo.permissionmanager",
                                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                            )
                        )
                    )
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.iqoo.secure",
                                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                            )
                        )
                    )
                }
                HUAWEI -> {
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.huawei.systemmanager",
                                "com.huawei.systemmanager.optimize.process.ProtectActivity"
                            )
                        )
                    )
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.huawei.systemmanager",
                                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                            )
                        )
                    )
                }
                ASUS -> {
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.asus.mobilemanager",
                                "com.asus.mobilemanager.autostart.AutoStartActivity"
                            )
                        )
                    )
                    intents.add(
                        Intent().setComponent(
                            ComponentName(
                                "com.asus.mobilemanager",
                                "com.asus.mobilemanager.entry.FunctionActivity"
                            )
                        )
                    )
                }
                OTHER -> {}
            }

            // Always add fallbacks
            intents.add(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$pkg")
                }
            )
            intents.add(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$pkg")
                }
            )
            intents.add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

            for (intent in intents) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                    return true
                } catch (_: Exception) {
                    // Try next fallback intent
                }
            }
            return false
        }
    }

    fun detectBrand(): OemBrand {
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
        val brand = Build.BRAND?.lowercase().orEmpty()
        val combined = "$manufacturer $brand"

        return when {
            combined.contains("xiaomi") || combined.contains("redmi") || combined.contains("poco") -> OemBrand.XIAOMI
            combined.contains("samsung") -> OemBrand.SAMSUNG
            combined.contains("vivo") || combined.contains("iqoo") -> OemBrand.VIVO
            combined.contains("huawei") || combined.contains("honor") -> OemBrand.HUAWEI
            combined.contains("asus") -> OemBrand.ASUS
            else -> OemBrand.OTHER
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(false)
    }
}

