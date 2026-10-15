package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles account-related dialogs: editing profile name, expanded avatar, account deletion,
 * backup restore confirmation, and cloud sync conflict resolution.
 */
class AccountDialogHelper(private val activity: MainActivity) {

    private val themeCoordinator get() = activity.themeCoordinator
    private val backupManager by lazy { BackupManager(activity) }
    private val dp = { v: Int -> activity.dp(v) }
    private fun tintedColor(color: Int, alpha: Int): Int = activity.tintedColor(color, alpha)

    fun showSignOutConfirmDialog(onConfirmed: (() -> Unit)? = null) {
        DeveloperToolsHelper.showThemedConfirmDialog(
            activity = activity,
            themeCoordinator = themeCoordinator,
            title = "Sign Out?",
            message = "Your local study records will remain on this device, but cloud sync will pause until you sign in again.",
            confirmText = "Sign Out",
            isDestructive = true
        ) {
            AuthManager.logout(activity)
            onConfirmed?.invoke() ?: run {
                Toast.makeText(activity, "Signed out successfully", Toast.LENGTH_SHORT).show()
                activity.navigateToPanel(AppPanel.SETTINGS)
            }
        }
    }

    fun showEditNameDialog() {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        content.addView(TextView(activity).apply {
            text = "PROFILE"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(activity).apply {
            text = "Change Account Name"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(14))
        })

        val currentName = AuthManager.getUserName(activity) ?: ""
        val inputEdit = EditText(activity).apply {
            setText(currentName)
            hint = "Enter your name"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        content.addView(inputEdit)

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val cancelBtn = Button(activity).apply {
            text = activity.getString(R.string.btn_cancel_upper)
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val saveBtn = Button(activity).apply {
            text = "SAVE"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 50f; setColor(themeCoordinator.primaryColor) }
            setOnClickListener {
                val newName = inputEdit.text.toString().trim()
                if (newName.isNotEmpty()) {
                    AuthManager.updateUserName(activity, newName)
                    activity.tabPageCache.remove(activity.settingsTabKey(AppSettingsTab.PROFILE))
                    activity.navigateToPanel(AppPanel.SETTINGS)
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        CloudSyncManager.syncDataToCloud(activity)
                    }
                }
                dialog.dismiss()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(saveBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showExpandedAvatarDialog() {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val size = (activity.resources.displayMetrics.widthPixels * 0.72f).toInt()
        val userName = AuthManager.getUserName(activity)
        val userEmail = AuthManager.getUserEmail(activity)
        val avatarBitmap = LocalAvatarManager.getCircularAvatarBitmap(activity, size)

        val frame = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        if (avatarBitmap != null) {
            val imgView = ImageView(activity).apply {
                setImageBitmap(avatarBitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeCoordinator.primaryColor)
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            frame.addView(imgView)
        } else {
            val textView = TextView(activity).apply {
                text = if (!userName.isNullOrBlank()) userName.take(1).uppercase() else if (!userEmail.isNullOrBlank()) userEmail.take(1).uppercase() else "G"
                textSize = 90f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeCoordinator.primaryColor)
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            frame.addView(textView)
        }

        frame.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(frame)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(size, size)
        dialog.show()
    }

    fun showDeleteAccountDialog() {
        val dialog = Dialog(activity)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        content.addView(TextView(activity).apply {
            text = "SEVERE WARNING • PERMANENT DELETION"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 11.5f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(activity).apply {
            text = "Delete Account & All Data"
            setTextColor(themeCoordinator.textColor)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })

        content.addView(TextView(activity).apply {
            text = "This action is permanent and will instantly erase all your focus history, subjects, cloud sync documents, and settings. Type 'DELETE' below to confirm."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 13.5f
            setPadding(0, dp(8), 0, dp(12))
        })

        val confirmInput = EditText(activity).apply {
            hint = "Type DELETE to confirm"
            setTextColor(themeCoordinator.textColor)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 90))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 40), 12f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        content.addView(confirmInput)

        val webInfoText = TextView(activity).apply {
            text = "Web Deletion Portal: https://get-studytimer.vercel.app/delete-account.html"
            setTextColor(themeCoordinator.primaryColor)
            alpha = 0.8f
            textSize = 11.5f
            setPadding(0, dp(10), 0, dp(14))
            setOnClickListener {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/delete-account.html")))
                } catch (_: Exception) {}
            }
        }
        content.addView(webInfoText)

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        val cancelBtn = Button(activity).apply {
            text = activity.getString(R.string.btn_cancel_upper)
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val deleteBtn = Button(activity).apply {
            text = "PERMANENTLY DELETE"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#DC2626"))
            }
            setOnClickListener {
                val inputStr = confirmInput.text.toString().trim()
                if (inputStr != "DELETE") {
                    Toast.makeText(activity, "Please type 'DELETE' exactly to confirm deletion", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                HapticFeedbackHelper.performHapticConfirm(activity)
                activity.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        CloudSyncManager.deleteUserCloudData(activity)
                    }
                    AuthManager.deleteLocalUserData(activity)
                    Toast.makeText(activity, "Account & all data permanently erased.", Toast.LENGTH_LONG).show()
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1.2f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(deleteBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun showRestoreConfirmDialog(uri: Uri) {
        val meta = backupManager.inspectBackupMetadata(uri)
        val currentLocalTs = backupManager.getLastModifiedTimestamp()
        val isOlderThanLocal = meta != null && meta.lastModifiedTimestamp > 0L && meta.lastModifiedTimestamp < currentLocalTs - 60000L
        val backupDateStr = if (meta != null && meta.backupCreatedAt > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(meta.backupCreatedAt))
        } else "Unknown Date"

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(activity).apply {
            text = if (isOlderThanLocal) "STALE BACKUP WARNING" else activity.getString(R.string.restore_backup)
            setTextColor(if (isOlderThanLocal) Color.parseColor("#F59E0B") else themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(activity).apply {
            text = if (isOlderThanLocal) "Import Older Backup?" else activity.getString(R.string.restore_backup_title)
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })
        content.addView(TextView(activity).apply {
            text = if (isOlderThanLocal) {
                "Warning: This backup file was created on $backupDateStr, which is OLDER than your current study data. Importing this will revert recent local progress."
            } else {
                "Backup from: $backupDateStr\n${activity.getString(R.string.restore_backup_message)}"
            }
            setTextColor(themeCoordinator.textColor)
            alpha = if (isOlderThanLocal) 0.9f else 0.6f
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
        })

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val cancelBtn = Button(activity).apply {
            text = activity.getString(R.string.btn_cancel_upper)
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val restoreBtn = Button(activity).apply {
            text = if (isOlderThanLocal) "REVERT & RESTORE" else activity.getString(R.string.btn_restore)
            setTextColor(themeCoordinator.bgColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = activity.rippleBackground(if (isOlderThanLocal) Color.parseColor("#F59E0B") else themeCoordinator.primaryColor)
            setOnClickListener {
                dialog.dismiss()
                performImportWithCloudConflictCheck(uri)
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(restoreBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun performImportWithCloudConflictCheck(uri: Uri) {
        val success = backupManager.importDataFromJSON(uri, allowCloudSync = false)
        if (!success) {
            Toast.makeText(activity, activity.getString(R.string.toast_backup_parse_failed), Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(activity, activity.getString(R.string.toast_logs_restored), Toast.LENGTH_SHORT).show()
        themeCoordinator.applyThemeCoordinates()
        activity.tabPageCache.clear()

        if (!AuthManager.isLoggedIn(activity)) {
            activity.recreate()
            return
        }

        activity.lifecycleScope.launch {
            val syncResult = withContext(Dispatchers.IO) {
                CloudSyncManager.syncWithConflictCheck(activity)
            }
            when (syncResult) {
                is CloudSyncManager.SyncCheckResult.Conflict -> {
                    showSyncConflictDialog(syncResult.localTimestamp, syncResult.cloudTimestamp, syncResult.cloudRecord)
                }
                is CloudSyncManager.SyncCheckResult.Success -> {
                    Toast.makeText(activity, "Cloud sync updated successfully", Toast.LENGTH_SHORT).show()
                    activity.recreate()
                }
                else -> {
                    activity.recreate()
                }
            }
        }
    }

    fun showSyncConflictDialog(localTs: Long, cloudTs: Long, cloudRecord: JSONObject) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val cloudDateStr = if (cloudTs > 0L) sdf.format(Date(cloudTs)) else "Recent Cloud State"
        val localDateStr = if (localTs > 0L) sdf.format(Date(localTs)) else "Imported Backup"

        content.addView(TextView(activity).apply {
            text = "SYNC CONFLICT DETECTED"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(activity).apply {
            text = "Cloud Has Newer Data"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(TextView(activity).apply {
            text = "Your Google/Cloud backup ($cloudDateStr) is newer than the imported data ($localDateStr).\n\nChoose how you want to resolve this:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 13f
            setPadding(0, dp(8), 0, dp(14))
        })

        val keepCloudBtn = Button(activity).apply {
            text = "Keep Newer Cloud Data (Recommended)"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                activity.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        CloudSyncManager.restoreDataFromCloud(activity)
                    }
                    Toast.makeText(activity, "Restored newer cloud data", Toast.LENGTH_SHORT).show()
                    activity.tabPageCache.clear()
                    activity.recreate()
                }
            }
        }
        content.addView(keepCloudBtn)

        val mergeBtn = Button(activity).apply {
            text = "Merge Both (Combine Records)"
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 90), 12f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                activity.lifecycleScope.launch {
                    val merged = withContext(Dispatchers.IO) {
                        CloudSyncManager.mergeCloudAndLocalData(activity, cloudRecord)
                    }
                    if (merged) {
                        Toast.makeText(activity, "Merged local and cloud data", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(activity, "Merge failed, keeping local state", Toast.LENGTH_SHORT).show()
                    }
                    activity.tabPageCache.clear()
                    activity.recreate()
                }
            }
        }
        content.addView(mergeBtn)

        val overwriteBtn = Button(activity).apply {
            text = "Overwrite Cloud with Local Data"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = themeCoordinator.createGlassChip(0x33EF4444.toInt(), 12f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
            setOnClickListener {
                dialog.dismiss()
                activity.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        CloudSyncManager.syncDataToCloud(activity, force = true)
                    }
                    Toast.makeText(activity, "Cloud overwritten with local backup", Toast.LENGTH_SHORT).show()
                    activity.recreate()
                }
            }
        }
        content.addView(overwriteBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}
