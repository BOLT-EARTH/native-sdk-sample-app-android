package com.example.testinguisdkgithub

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.boltearthsdk.BoltEarthUiSdk
import com.boltearthsdk.SdkEnvironment
import com.boltearthsdk.SdkFontOverrides
import com.example.testinguisdkgithub.databinding.ActivitySdkUserGateBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sample launcher: collects a user ID, SDK token, environment (production vs dev/staging/test),
 * a primary brand colour, and an optional SDK UI language before handing off to the booking flow via
 * [BoltEarthUiSdk.openChargerBookingFlow].
 * Fields are editable and pre-filled with previously saved values so locale and tinting apply to SDK
 * screens (hosts that embed SDK Activities do this via [com.boltearthsdk.BoltEarthHostActivity]; this
 * gate screen uses plain AppCompat so only custom fields need wrapping if expanded later).
 *
 * Kept separate from the booking NavHost Activity so booking UI is never
 * hosted while invisible (avoids camera / SDK-login race conditions).
 */
class SdkUserGateActivity : AppCompatActivity() {

    private data class SdkLocaleChoice(val languageTag: String, val label: String)

    private lateinit var sdkLocaleChoices: List<SdkLocaleChoice>

    // ── Preset palette ────────────────────────────────────────────────────────

    private data class ColorOption(val hex: String, val label: String)

    private val presetColors = listOf(
        ColorOption("#26C72D", "Green"),
        ColorOption("#2196F3", "Blue"),
        ColorOption("#E91E63", "Pink"),
        ColorOption("#FF6D00", "Orange"),
        ColorOption("#7B1FA2", "Purple"),
        ColorOption("#009688", "Teal"),
        ColorOption("#F44336", "Red"),
        ColorOption("#FF9800", "Amber"),
    )

    private var selectedColorHex: String = presetColors[0].hex

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySdkUserGateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val savedUserId = getSavedPref(KEY_USER_ID)
        val savedToken  = getSavedPref(KEY_SDK_TOKEN)
        val savedColor  = getSavedPref(KEY_THEME_COLOR)
        val savedLocale = getSavedPref(KEY_SDK_LANGUAGE)

        sdkLocaleChoices = listOf(
            SdkLocaleChoice("", getString(R.string.sample_locale_follow_device)),
            SdkLocaleChoice("en", "English"),
            SdkLocaleChoice("hi", "हिंदी"),
            SdkLocaleChoice("mr", "मराठी"),
            SdkLocaleChoice("kn", "ಕನ್ನಡ"),
            SdkLocaleChoice("ta", "தமிழ்"),
            SdkLocaleChoice("te", "తెలుగు"),
        )
        val spinner = binding.sdkLanguageSpinner
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            sdkLocaleChoices.map { it.label },
        )
        val localeIndex = sdkLocaleChoices.indexOfFirst { it.languageTag == savedLocale }
            .takeIf { it >= 0 } ?: 0
        spinner.setSelection(localeIndex)

        // Pre-populate credential fields
        if (savedUserId.isNotBlank()) binding.optionalUserIdInput.setText(savedUserId)
        binding.sdkTokenInput.setText(savedToken.ifBlank { BuildConfig.BOLT_SDK_APP_TOKEN })

        // Set up colour picker
        selectedColorHex = savedColor.ifBlank { presetColors[0].hex }
        setupColorSwatches(binding)
        binding.customColorInput.setText(selectedColorHex)

        // Sync hex input → swatch selection
        binding.customColorInput.doAfterTextChanged { editable ->
            val text = editable?.toString()?.trim().orEmpty()
            if (isValidHexColor(text)) {
                selectedColorHex = text
                updateSwatchHighlight(binding, text)
            }
        }

        binding.initializeSdkButton.setOnClickListener {
            val enteredUserId = binding.optionalUserIdInput.text?.toString()?.trim().orEmpty()
            val enteredToken  = binding.sdkTokenInput.text?.toString()?.trim().orEmpty()
            val enteredColor  = binding.customColorInput.text?.toString()?.trim().orEmpty()

            val userId = enteredUserId.ifBlank { savedUserId }
            val token  = enteredToken.ifBlank { BuildConfig.BOLT_SDK_APP_TOKEN }
            val color  = if (isValidHexColor(enteredColor)) enteredColor else selectedColorHex
            val localeTag = sdkLocaleChoices[binding.sdkLanguageSpinner.selectedItemPosition].languageTag

            if (userId.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_user_id_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (token.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_sdk_token_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (enteredUserId.isNotBlank()) savePref(KEY_USER_ID, enteredUserId)
            savePref(KEY_SDK_TOKEN, token)
            savePref(KEY_THEME_COLOR, color)
            savePref(KEY_SDK_LANGUAGE, localeTag)

            binding.initializeSdkButton.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                BoltEarthUiSdk.initialize(
                    context      = applicationContext,
                    userId       = userId,
                    sdkToken     = token,
                    environment  = SdkEnvironment.Development,
                    primaryColor = color,
                    fonts = SdkFontOverrides(
                        light = R.font.cause_light,
                        regular = R.font.cause_regular,
                        medium = R.font.cause_medium,
                        semiBold = R.font.cause_semibold,
                        bold = R.font.cause_bold,
                    ),
                    localeLanguageTag = localeTag,
                    sdkHeaderLogoBase64 = SAMPLE_HEADER_LOGO_BASE64,
                    sdkHeaderHomeIconBase64 = SAMPLE_HEADER_HOME_ICON_BASE64,
                    onHeaderHomeTapped = {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "SDK header home tapped",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
                withContext(Dispatchers.Main.immediate) {
                    binding.initializeSdkButton.isEnabled = true
                    android.widget.Toast.makeText(
                        this@SdkUserGateActivity,
                        "SDK initialized",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }

        binding.openChargerBookingFlowButton.setOnClickListener {
            // Blank is fine — the SDK falls back to the client's "other" vehicle post-login.
            val vehicleId = binding.vehicleIdInput.text?.toString()?.trim().orEmpty()

            binding.openChargerBookingFlowButton.isEnabled = false
            val socPercentInput = readCurrentSocInput(binding) ?: 0
            val preselectedChargerId = binding.preselectedChargerIdInput.text
                ?.toString()?.trim()?.ifBlank { null }
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    BoltEarthUiSdk.openChargerBookingFlow(
                        context = this@SdkUserGateActivity,
                        vehicleId = vehicleId,
                        initialSOCPercent = socPercentInput,
                        chargerId = preselectedChargerId
                    )
                } catch (e: IllegalArgumentException) {
                    withContext(Dispatchers.Main.immediate) {
                        android.widget.Toast.makeText(
                            this@SdkUserGateActivity,
                            e.message,
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                } finally {
                    withContext(Dispatchers.Main.immediate) {
                        binding.openChargerBookingFlowButton.isEnabled = true
                    }
                }
            }
        }

        binding.openBookingsListButton.setOnClickListener {
            val enteredUserId = binding.optionalUserIdInput.text?.toString()?.trim().orEmpty()
            val enteredToken  = binding.sdkTokenInput.text?.toString()?.trim().orEmpty()
            val enteredColor  = binding.customColorInput.text?.toString()?.trim().orEmpty()

            val userId = enteredUserId.ifBlank { savedUserId }
            val token  = enteredToken.ifBlank { BuildConfig.BOLT_SDK_APP_TOKEN }
            val color  = if (isValidHexColor(enteredColor)) enteredColor else selectedColorHex
            val localeTag = sdkLocaleChoices[binding.sdkLanguageSpinner.selectedItemPosition].languageTag

            if (userId.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_user_id_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (token.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_sdk_token_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (enteredUserId.isNotBlank()) savePref(KEY_USER_ID, enteredUserId)
            savePref(KEY_SDK_TOKEN, token)
            savePref(KEY_THEME_COLOR, color)
            savePref(KEY_SDK_LANGUAGE, localeTag)

            binding.openBookingsListButton.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                BoltEarthUiSdk.initialize(
                    context      = applicationContext,
                    userId       = userId,
                    sdkToken     = token,
                    environment  = SdkEnvironment.Development,
                    primaryColor = color,
                    fonts = SdkFontOverrides(
                        light = R.font.cause_light,
                        regular = R.font.cause_regular,
                        medium = R.font.cause_medium,
                        semiBold = R.font.cause_semibold,
                        bold = R.font.cause_bold,
                    ),
                    localeLanguageTag = localeTag,
                    sdkHeaderLogoBase64 = SAMPLE_HEADER_LOGO_BASE64,
                    sdkHeaderHomeIconBase64 = SAMPLE_HEADER_HOME_ICON_BASE64,
                    onHeaderHomeTapped = {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "SDK header home tapped",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
                withContext(Dispatchers.Main.immediate) {
                    binding.openBookingsListButton.isEnabled = true
                    BoltEarthUiSdk.openUsersBookingsList(this@SdkUserGateActivity)
                }
            }
        }

        binding.fetchChargerButton.setOnClickListener {
            val enteredUserId = binding.optionalUserIdInput.text?.toString()?.trim().orEmpty()
            val enteredToken  = binding.sdkTokenInput.text?.toString()?.trim().orEmpty()
            val enteredColor  = binding.customColorInput.text?.toString()?.trim().orEmpty()
            val chargerId     = binding.chargerIdInput.text?.toString()?.trim().orEmpty()

            val userId = enteredUserId.ifBlank { savedUserId }
            val token  = enteredToken.ifBlank { BuildConfig.BOLT_SDK_APP_TOKEN }
            val color  = if (isValidHexColor(enteredColor)) enteredColor else selectedColorHex
            val localeTag = sdkLocaleChoices[binding.sdkLanguageSpinner.selectedItemPosition].languageTag

            if (userId.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_user_id_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (token.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_sdk_token_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (chargerId.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_charger_id_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (enteredUserId.isNotBlank()) savePref(KEY_USER_ID, enteredUserId)
            savePref(KEY_SDK_TOKEN, token)
            savePref(KEY_THEME_COLOR, color)
            savePref(KEY_SDK_LANGUAGE, localeTag)

            binding.fetchChargerButton.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                BoltEarthUiSdk.initialize(
                    context      = applicationContext,
                    userId       = userId,
                    sdkToken     = token,
                    environment  = SdkEnvironment.Development,
                    primaryColor = color,
                    fonts = SdkFontOverrides(
                        light = R.font.cause_light,
                        regular = R.font.cause_regular,
                        medium = R.font.cause_medium,
                        semiBold = R.font.cause_semibold,
                        bold = R.font.cause_bold,
                    ),
                    localeLanguageTag = localeTag,
                    sdkHeaderLogoBase64 = SAMPLE_HEADER_LOGO_BASE64,
                    sdkHeaderHomeIconBase64 = SAMPLE_HEADER_HOME_ICON_BASE64,
                    onHeaderHomeTapped = {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "SDK header home tapped",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
                withContext(Dispatchers.Main.immediate) {
                    try {
                        // Runs the SDK login gate internally (same as the bookings list) before
                        // hitting the API; the callback always arrives on the main thread.
                        BoltEarthUiSdk.fetchCharger(this@SdkUserGateActivity, chargerId) { response ->
                            binding.fetchChargerButton.isEnabled = true
                            if (isFinishing || isDestroyed) return@fetchCharger
                            val pretty = runCatching { response.toString(2) }
                                .getOrDefault(response.toString())
                            androidx.appcompat.app.AlertDialog.Builder(this@SdkUserGateActivity)
                                .setTitle(R.string.sample_charger_response_title)
                                .setMessage(pretty)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                    } catch (e: IllegalArgumentException) {
                        // Invalid charger ID format — thrown synchronously, like on iOS.
                        binding.fetchChargerButton.isEnabled = true
                        android.widget.Toast.makeText(
                            this@SdkUserGateActivity,
                            e.message,
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }

        binding.logoutButton.setOnClickListener {
            BoltEarthUiSdk.logout(applicationContext){

            }
        }
        binding.fetchContactSupportInfoButton.setOnClickListener {
            val response = BoltEarthUiSdk.fetchContactSupportInfo()
            val pretty = runCatching { response.toString(2) }.getOrDefault(response.toString())
            androidx.appcompat.app.AlertDialog.Builder(this@SdkUserGateActivity)
                .setTitle(R.string.sample_charger_response_title)
                .setMessage(pretty)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        binding.openUserWallet.setOnClickListener {
            val enteredUserId = binding.optionalUserIdInput.text?.toString()?.trim().orEmpty()
            val enteredToken  = binding.sdkTokenInput.text?.toString()?.trim().orEmpty()
            val enteredColor  = binding.customColorInput.text?.toString()?.trim().orEmpty()

            val userId = enteredUserId.ifBlank { savedUserId }
            val token  = enteredToken.ifBlank { BuildConfig.BOLT_SDK_APP_TOKEN }
            val color  = if (isValidHexColor(enteredColor)) enteredColor else selectedColorHex
            val localeTag = sdkLocaleChoices[binding.sdkLanguageSpinner.selectedItemPosition].languageTag

            if (userId.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_user_id_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (token.isBlank()) {
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.sample_sdk_token_required),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@setOnClickListener
            }

            if (enteredUserId.isNotBlank()) savePref(KEY_USER_ID, enteredUserId)
            savePref(KEY_SDK_TOKEN, token)
            savePref(KEY_THEME_COLOR, color)
            savePref(KEY_SDK_LANGUAGE, localeTag)

            binding.openUserWallet.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                BoltEarthUiSdk.initialize(
                    context      = applicationContext,
                    userId       = userId,
                    sdkToken     = token,
                    environment  = SdkEnvironment.Development,
                    primaryColor = color,
                    fonts = SdkFontOverrides(
                        light = R.font.cause_light,
                        regular = R.font.cause_regular,
                        medium = R.font.cause_medium,
                        semiBold = R.font.cause_semibold,
                        bold = R.font.cause_bold,
                    ),
                    localeLanguageTag = localeTag,
                    sdkHeaderLogoBase64 = SAMPLE_HEADER_LOGO_BASE64,
                    sdkHeaderHomeIconBase64 = SAMPLE_HEADER_HOME_ICON_BASE64,
                    onHeaderHomeTapped = {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "SDK header home tapped",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
                withContext(Dispatchers.Main.immediate) {
                    binding.openUserWallet.isEnabled = true
                    BoltEarthUiSdk.presentWalletFlow(this@SdkUserGateActivity)
                }
            }
        }
    }

    // ── Color picker helpers ──────────────────────────────────────────────────

    private fun setupColorSwatches(binding: ActivitySdkUserGateBinding) {
        val container = binding.colorSwatchContainer
        val swatchSizePx = dpToPx(44)
        val swatchMarginPx = dpToPx(10)

        presetColors.forEach { option ->
            val swatch = createSwatch(option, swatchSizePx, swatchMarginPx)
            swatch.setOnClickListener {
                selectedColorHex = option.hex
                binding.customColorInput.setText(option.hex)
                updateSwatchHighlight(binding, option.hex)
            }
            container.addView(swatch)
        }
        updateSwatchHighlight(binding, selectedColorHex)
    }

    private fun createSwatch(
        option: ColorOption,
        sizePx: Int,
        marginPx: Int,
    ): android.view.View {
        val frame = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx + dpToPx(6), sizePx + dpToPx(6)).apply {
                setMargins(0, 0, marginPx, 0)
            }
            tag = option.hex
        }

        val circle = android.view.View(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = android.view.Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(option.hex))
            }
            contentDescription = option.label
        }

        frame.addView(circle)
        return frame
    }

    private fun updateSwatchHighlight(
        binding: ActivitySdkUserGateBinding,
        activeHex: String,
    ) {
        val container = binding.colorSwatchContainer
        val ringWidthPx = dpToPx(3)
        val ringGap = dpToPx(3)
        val swatchSizePx = dpToPx(44)

        for (i in 0 until container.childCount) {
            val frame = container.getChildAt(i)
            val hex = frame.tag as? String ?: continue
            val isSelected = hex.equals(activeHex, ignoreCase = true)

            // Ring is drawn on the frame's background; the inner circle stays inside
            val ring = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isSelected) {
                    setStroke(ringWidthPx, Color.parseColor(hex))
                    setColor(Color.TRANSPARENT)
                } else {
                    setColor(Color.TRANSPARENT)
                }
            }
            frame.background = ring
        }
    }

    /**
     * Parses the "Current SOC %" field for the mandatory `initialSOCPercent` argument to
     * [BoltEarthUiSdk.openChargerBookingFlow]; blank/invalid → 0, clamped to 0–100.
     */
    private fun readCurrentSocInput(binding: ActivitySdkUserGateBinding): Int? {
        val input = binding.currentSocInput.text?.toString()?.trim()

        if (input.isNullOrBlank()) {
            binding.currentSocInputLayout.error = "Please enter current SOC"
            return null
        }

        binding.currentSocInputLayout.error = null

        return input.toIntOrNull()
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private fun getSavedPref(key: String): String =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null).orEmpty()

    private fun savePref(key: String, value: String) =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    private fun isValidHexColor(hex: String): Boolean {
        if (hex.isBlank()) return false
        return try {
            Color.parseColor(hex.trim())
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val PREFS_NAME        = "sdk_sample_prefs"
        private const val KEY_USER_ID       = "sdk_sample_user_id"
        private const val KEY_SDK_TOKEN     = "sdk_sample_sdk_token"
        private const val KEY_SDK_PACKAGE   = "sdk_sample_sdk_package"
        private const val KEY_THEME_COLOR   = "sdk_sample_theme_color"
        private const val KEY_SDK_LANGUAGE  = "sdk_sample_language_tag"
    }
}
