package com.madeby.JAI

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = config.fontScale.coerceIn(0.85f, 1.05f)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            overrideConfiguration.fontScale = overrideConfiguration.fontScale.coerceIn(0.85f, 1.05f)
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnGuest: MaterialButton

    private val googleSignInLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account?.idToken
                val displayName = account?.displayName ?: account?.email?.substringBefore("@") ?: "Student"
                val email = account?.email ?: ""
                val avatarUrl = account?.photoUrl?.toString()

                if (!idToken.isNullOrBlank()) {
                    exchangeTokenWithSupabase(idToken, displayName, email, avatarUrl)
                } else if (email.isNotBlank()) {
                    // ID token empty locally but account authenticated: complete login directly
                    completeLoginAndSync(email, displayName, null, account?.id ?: email, avatarUrl)
                } else {
                    AppToast.show(this, "Connecting with Google via browser...")
                    launchBrowserOAuth()
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Log.e("LoginActivity", "Google Sign-In failed: code=${e.statusCode}", e)
                when (e.statusCode) {
                    com.google.android.gms.common.api.CommonStatusCodes.CANCELED -> {
                        // User explicitly pressed back on account chooser
                    }
                    10 -> {
                        AppToast.show(this, "Google Sign-In (Code 10). Connecting via browser...", Toast.LENGTH_LONG)
                        launchBrowserOAuth()
                    }
                    12501, 12500, 12502 -> {
                        // Play services abort or failed: fallback seamlessly to web OAuth
                        AppToast.show(this, "Completing Google Sign-In...")
                        launchBrowserOAuth()
                    }
                    com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR, 7 -> {
                        AppToast.show(this, "Network error. Opening browser sign-in...", Toast.LENGTH_LONG)
                        launchBrowserOAuth()
                    }
                    else -> {
                        AppToast.show(this, "Opening browser sign-in...")
                        launchBrowserOAuth()
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Unexpected error processing Google Sign-In", e)
                launchBrowserOAuth()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(R.layout.activity_login)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnGuest = findViewById(R.id.btnGuest)
        val btnBrowserSignIn = findViewById<TextView?>(R.id.btnBrowserSignIn)

        attachPressScale(btnGoogleSignIn, 0.97f)
        attachPressScale(btnGuest, 0.97f)
        btnBrowserSignIn?.let { attachPressScale(it, 0.97f) }

        btnGoogleSignIn.setOnClickListener {
            val epoch = System.currentTimeMillis()
            AuthManager.recordTermsConsent(this, epochMillis = epoch)
            performGoogleSignIn()
        }

        btnBrowserSignIn?.setOnClickListener {
            val epoch = System.currentTimeMillis()
            AuthManager.recordTermsConsent(this, epochMillis = epoch)
            launchBrowserOAuth()
        }

        btnGuest.setOnClickListener {
            AuthManager.setGuestMode(this)
            proceedToMain()
        }

        val contentRoot = findViewById<View>(R.id.loginContentRoot)
        ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                navBars.bottom + 16
            )
            insets
        }

        val tvLegalNotice = findViewById<TextView>(R.id.tvLegalNotice)
        setupLegalNotice(tvLegalNotice)

        // Handle OAuth Deep-Link return if applicable
        intent?.data?.let { handleDeepLink(it) }

        if (intent?.getBooleanExtra("auto_google", false) == true) {
            val epoch = System.currentTimeMillis()
            AuthManager.recordTermsConsent(this, epochMillis = epoch)
            performGoogleSignIn()
        }
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun attachPressScale(view: View, targetScale: Float = 0.97f) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(targetScale).scaleY(targetScale).setDuration(90).start()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }
            }
            false
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.data?.let { handleDeepLink(it) }
    }

    private fun showTermsConsentDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.TransparentBottomSheetDialogTheme)
        val dialogView = layoutInflater.inflate(R.layout.dialog_terms_consent, null)
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
            bottomSheet?.background = null
            if (bottomSheet is android.view.ViewGroup) {
                bottomSheet.clipToOutline = false
            }
        }

        val tvLinks = dialogView.findViewById<TextView>(R.id.tvDialogLegalLinks)
        val cbAccept = dialogView.findViewById<android.widget.CheckBox>(R.id.cbAcceptTerms)
        val btnAgree = dialogView.findViewById<MaterialButton>(R.id.btnAgreeAndContinue)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelConsent)

        setupDialogLegalLinks(tvLinks)
        attachPressScale(btnAgree, 0.97f)
        attachPressScale(btnCancel, 0.97f)

        btnAgree.setOnClickListener {
            if (!cbAccept.isChecked) {
                AppToast.show(this, "Please check the box to accept the Terms & Privacy Policy.")
                return@setOnClickListener
            }
            val epoch = System.currentTimeMillis()
            AuthManager.recordTermsConsent(this, epochMillis = epoch)
            dialog.dismiss()
            performGoogleSignIn()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDialogLegalLinks(textView: TextView) {
        val fullText = "Read the complete Terms of Service and Privacy Policy."
        val spannable = SpannableStringBuilder(fullText)

        val termsText = "Terms of Service"
        val termsStart = fullText.indexOf(termsText)
        if (termsStart != -1) {
            val termsEnd = termsStart + termsText.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWebUrl("https://get-studytimer.vercel.app/terms.html")
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.parseColor("#818CF8")
                    ds.isUnderlineText = true
                }
            }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val privacyText = "Privacy Policy"
        val privacyStart = fullText.indexOf(privacyText)
        if (privacyStart != -1) {
            val privacyEnd = privacyStart + privacyText.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWebUrl("https://get-studytimer.vercel.app/privacy.html")
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.parseColor("#818CF8")
                    ds.isUnderlineText = true
                }
            }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }

    private fun performGoogleSignIn() {
        val googleClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (googleClientId.isBlank()) {
            AppToast.show(this, "Google Web Client ID missing in config.", Toast.LENGTH_LONG)
            return
        }

        try {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(googleClientId)
                .requestEmail()
                .requestProfile()
                .build()

            val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)
            var hasLaunched = false
            fun doLaunch() {
                if (!hasLaunched) {
                    hasLaunched = true
                    try {
                        googleSignInLauncher.launch(client.signInIntent)
                    } catch (ex: Exception) {
                        Log.e("LoginActivity", "Failed to launch Google Sign-In intent", ex)
                        launchBrowserOAuth()
                    }
                }
            }
            val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable { doLaunch() }
            timeoutHandler.postDelayed(timeoutRunnable, 1000L)
            client.signOut().addOnCompleteListener {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                doLaunch()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error launching Google Sign-In popup", e)
            launchBrowserOAuth()
        }
    }

    private fun launchBrowserOAuth() {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        if (supabaseUrl.isBlank()) {
            AppToast.show(this, "Supabase URL is missing.", Toast.LENGTH_LONG)
            return
        }
        val redirectUri = "studytimer://login-callback"
        val authUrl = "$supabaseUrl/auth/v1/authorize?provider=google&redirect_to=$redirectUri"
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(
                    androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(Color.BLACK)
                        .build()
                )
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(authUrl))
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                startActivity(intent)
            } catch (ex: Exception) {
                AppToast.show(this, "Could not open browser for sign-in: ${ex.message}")
            }
        }
    }

    private fun completeLoginAndSync(email: String, name: String, accessToken: String?, userId: String, avatarUrl: String? = null) {
        val backupMgr = BackupManager(this)
        val wasGuest = AuthManager.isGuest(this)
        val hasLocalData = backupMgr.hasLocalStudyData()
        val isSameUser = AuthManager.isSameUser(this, userId, email)

        // Always create a pre-auth safety checkpoint before modifying any state
        backupMgr.createPreAuthSafetySnapshot("pre_login_${userId.take(8)}")

        if (!isSameUser && !wasGuest) {
            // Switched from a different signed-in user: reset current memory to avoid leaking previous user profile
            AuthManager.resetLocalUserData(this)
        }

        AuthManager.saveUserSession(this, email, name, accessToken, userId)
        if (!avatarUrl.isNullOrBlank()) {
            AuthManager.saveProfileImageUri(this, avatarUrl)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Record consent to Supabase with exact immutable timestamp
                TermsConsentManager.syncConsentToServer(
                    this@LoginActivity,
                    userId = userId,
                    email = email,
                    name = name,
                    termsVersion = AuthManager.getTermsVersion(this@LoginActivity)
                )

                if (!avatarUrl.isNullOrBlank()) {
                    LocalAvatarManager.downloadAndSaveRemoteAvatar(this@LoginActivity, avatarUrl)
                }
                val (remoteMeta, rawRecord) = CloudSyncManager.fetchRemoteMetadata(this@LoginActivity)
                if (remoteMeta != null && remoteMeta.updatedAt > 0L && rawRecord != null) {
                    if (wasGuest && hasLocalData) {
                        // Smart non-destructive merge so guest progress merges seamlessly with the account
                        CloudSyncManager.mergeCloudAndLocalData(this@LoginActivity, rawRecord)
                    } else {
                        CloudSyncManager.restoreDataFromCloud(this@LoginActivity)
                    }
                } else {
                    // Fresh cloud account with no existing backup: upload local study data to the account
                    CloudSyncManager.syncDataToCloud(this@LoginActivity, force = true)
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Post-login sync error", e)
            } finally {
                withContext(Dispatchers.Main) {
                    AppToast.show(this@LoginActivity, "Welcome, $name!")
                    proceedToMain()
                }
            }
        }
    }

    private fun exchangeTokenWithSupabase(idToken: String, displayName: String, googleEmail: String, googleAvatarUrl: String? = null) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            completeLoginAndSync(googleEmail, displayName, idToken, googleEmail, googleAvatarUrl)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$supabaseUrl/auth/v1/token?grant_type=id_token")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("provider", "google")
                    put("id_token", idToken)
                }

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    val accessToken = json.optString("access_token")
                    val userObj = json.optJSONObject("user")
                    val metaObj = userObj?.optJSONObject("user_metadata") ?: userObj?.optJSONObject("raw_user_meta_data")
                    val fetchedName = metaObj?.optString("full_name")?.takeIf { it.isNotBlank() }
                        ?: metaObj?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: displayName.takeIf { it.isNotBlank() }
                        ?: googleEmail.substringBefore("@")
                    val email = userObj?.optString("email")?.takeIf { it.isNotBlank() } ?: googleEmail
                    val userId = userObj?.optString("id")?.takeIf { it.isNotBlank() } ?: email
                    val avatarUrl = metaObj?.optString("avatar_url")?.takeIf { it.isNotBlank() }
                        ?: metaObj?.optString("picture")?.takeIf { it.isNotBlank() }
                        ?: metaObj?.optString("avatar")?.takeIf { it.isNotBlank() }
                        ?: googleAvatarUrl

                    withContext(Dispatchers.Main) {
                        completeLoginAndSync(email, fetchedName, accessToken, userId, avatarUrl)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        completeLoginAndSync(googleEmail, displayName, idToken, googleEmail, googleAvatarUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Supabase token exchange error", e)
                withContext(Dispatchers.Main) {
                    completeLoginAndSync(googleEmail, displayName, idToken, googleEmail, googleAvatarUrl)
                }
            }
        }
    }

    private fun handleDeepLink(uri: Uri) {
        if (uri.scheme == "studytimer" && uri.host == "login-callback") {
            val fragment = uri.fragment ?: ""
            val query = uri.query ?: ""
            var accessToken: String? = null

            // Check fragment (e.g. #access_token=...&refresh_token=...)
            if (fragment.contains("access_token=")) {
                val params = fragment.split("&")
                for (p in params) {
                    if (p.startsWith("access_token=")) {
                        accessToken = try {
                            java.net.URLDecoder.decode(p.substringAfter("access_token="), "UTF-8")
                        } catch (_: Exception) {
                            p.substringAfter("access_token=")
                        }
                    }
                }
            }

            // Check query parameters (e.g. ?access_token=...)
            if (accessToken.isNullOrBlank() && query.contains("access_token=")) {
                accessToken = uri.getQueryParameter("access_token")
            }

            if (!accessToken.isNullOrBlank()) {
                fetchSupabaseProfileAndCompleteLogin(accessToken)
            } else {
                val errorDesc = uri.getQueryParameter("error_description")
                    ?: if (fragment.contains("error_description=")) fragment.substringAfter("error_description=").substringBefore("&") else null
                if (!errorDesc.isNullOrBlank()) {
                    val cleanError = try { java.net.URLDecoder.decode(errorDesc, "UTF-8") } catch (_: Exception) { errorDesc }
                    AppToast.show(this, "Sign-In notice: $cleanError")
                }
            }
        }
    }

    private fun fetchSupabaseProfileAndCompleteLogin(accessToken: String) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        lifecycleScope.launch(Dispatchers.IO) {
            var userId = ""
            var email = ""
            var name = ""
            var avatarUrl: String? = null

            if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                try {
                    val url = URL("$supabaseUrl/auth/v1/user")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("apikey", anonKey)
                    conn.setRequestProperty("Authorization", "Bearer $accessToken")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000

                    if (conn.responseCode in 200..299) {
                        val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val userObj = JSONObject(responseStr)
                        userId = userObj.optString("id")
                        email = userObj.optString("email")
                        val meta = userObj.optJSONObject("user_metadata") ?: userObj.optJSONObject("raw_user_meta_data")
                        name = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                            ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
                            ?: email.substringBefore("@")
                        avatarUrl = meta?.optString("avatar_url")?.takeIf { it.isNotBlank() }
                            ?: meta?.optString("picture")?.takeIf { it.isNotBlank() }
                    }
                } catch (e: Exception) {
                    Log.w("LoginActivity", "Failed to fetch user from Supabase auth endpoint", e)
                }
            }

            // Fallback: parse JWT payload from accessToken if available
            if (userId.isBlank() || email.isBlank()) {
                try {
                    val parts = accessToken.split(".")
                    if (parts.size >= 2) {
                        val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
                        val jwtObj = JSONObject(String(payloadBytes, Charsets.UTF_8))
                        if (userId.isBlank()) userId = jwtObj.optString("sub")
                        if (email.isBlank()) email = jwtObj.optString("email")
                        if (name.isBlank()) {
                            val meta = jwtObj.optJSONObject("user_metadata")
                            name = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                                ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
                                ?: email.substringBefore("@")
                        }
                    }
                } catch (_: Exception) {}
            }

            if (email.isBlank()) email = "user_${System.currentTimeMillis()}@studytimer.app"
            if (userId.isBlank()) userId = email
            if (name.isBlank()) name = email.substringBefore("@")

            withContext(Dispatchers.Main) {
                completeLoginAndSync(email, name, accessToken, userId, avatarUrl)
            }
        }
    }

    private fun setupLegalNotice(textView: TextView) {
        val fullText = "By continuing, you agree to our Terms of Service and Privacy Policy."
        val spannable = SpannableStringBuilder(fullText)

        val termsText = "Terms of Service"
        val termsStart = fullText.indexOf(termsText)
        if (termsStart != -1) {
            val termsEnd = termsStart + termsText.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWebUrl("https://get-studytimer.vercel.app/terms.html")
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.parseColor("#818CF8")
                    ds.isUnderlineText = true
                }
            }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val privacyText = "Privacy Policy"
        val privacyStart = fullText.indexOf(privacyText)
        if (privacyStart != -1) {
            val privacyEnd = privacyStart + privacyText.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWebUrl("https://get-studytimer.vercel.app/privacy.html")
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.parseColor("#818CF8")
                    ds.isUnderlineText = true
                }
            }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }

    private fun openWebUrl(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                AppToast.show(this, "Could not open browser")
            }
        }
    }

    private fun proceedToMain() {
        AuthManager.setOnboardingCompleted(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
