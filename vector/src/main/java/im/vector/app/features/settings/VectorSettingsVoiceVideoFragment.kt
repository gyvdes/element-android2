/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.utils.RingtoneUtils
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsVoiceVideoFragment : VectorSettingsBaseFragment() {

    @Inject lateinit var ringtoneUtils: RingtoneUtils

    override var titleRes = CommonStrings.preference_voice_and_video
    override val preferenceXmlRes = R.xml.vector_settings_voice_video

    private val mUseRiotCallRingtonePreference by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY)!!
    }
    private val mPreferredDomain by lazy {
        findPreference<EditTextPreference>("SETTINGS_PREFERRED_JITSI_DOMAIN")!!
    }
    private val mJitsiWidgetURL by lazy {
        findPreference<EditTextPreference>("SETTINGS_JITSI_WIDGET_URL")!!
    }
    private val mCallRingtonePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CALL_RINGTONE_URI_PREFERENCE_KEY)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsVoiceVideo
    }

    override fun bindPref() {
        // Incoming call sounds
        mUseRiotCallRingtonePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ringtoneUtils.setUseRiotDefaultRingtone(mUseRiotCallRingtonePreference.isChecked)
            false
        }

        mCallRingtonePreference.let {
            it.summary = ringtoneUtils.getCallRingtoneName()
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                displayRingtonePicker()
                false
            }
        }
        // PreferredJitsiDomain
        mPreferredDomain.let {
            it.summary = ringtoneUtils.getPrefJitsiDomain()
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                newValue
                        ?.let { value -> (value as? String)?.trim() }
                        ?.let { value -> onPrefDomainChanged(value) }
                false
            }
        }
        // PreferredJitsiDomain
        mJitsiWidgetURL.let {
            it.summary = ringtoneUtils.getJitsiWidgetUrl()
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                newValue
                        ?.let { value -> (value as? String)?.trim() }
                        ?.let { value -> onJitsiWidgetUrlChanged(value) }
                false
            }
        }
    }

    private val ringtoneStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val callRingtoneUri: Uri? = activityResult.data?.getParcelableExtraCompat(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (callRingtoneUri != null) {
                ringtoneUtils.setCallRingtoneUri(callRingtoneUri)
                mCallRingtonePreference.summary = ringtoneUtils.getCallRingtoneName()
            }
        }
    }

    private fun displayRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(CommonStrings.settings_call_ringtone_dialog_title))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUtils.getCallRingtoneUri())
        }
        ringtoneStartForActivityResult.launch(intent)
    }

    /**
     * Update the Preferred Domain.
     */

    private fun onPrefDomainChanged(value: String) {
        val currentPrefJitsiDomain = ringtoneUtils.getPrefJitsiDomain()
        if (currentPrefJitsiDomain != value) {
            displayLoadingView()
            lifecycleScope.launch {
                val result = runCatching { ringtoneUtils.setPrefJitsiDomain(value) }
                if (!isAdded) return@launch
                result.fold(
                        onSuccess = {
                            // refresh the settings value
                            mPreferredDomain.summary = value
                            mPreferredDomain.text = value
                            hideLoadingView()
                        },
                        onFailure = {
                            hideLoadingView()
                            displayErrorDialog(it)
                        }
                )
            }
        }
    }

    /**
     * Update the Jitsi Widget URL.
     */

    private fun onJitsiWidgetUrlChanged(value: String) {
        val currentJitsiWidgetUrl = ringtoneUtils.getJitsiWidgetUrl()
        if (currentJitsiWidgetUrl != value) {
            displayLoadingView()
            lifecycleScope.launch {
                val result = runCatching { ringtoneUtils.setJitsiWidgetUrl(value) }
                if (!isAdded) return@launch
                result.fold(
                        onSuccess = {
                            // refresh the settings value
                            mJitsiWidgetURL.summary = value
                            mJitsiWidgetURL.text = value
                            hideLoadingView()
                        },
                        onFailure = {
                            hideLoadingView()
                            displayErrorDialog(it)
                        }
                )
            }
        }
    }
}
