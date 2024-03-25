package com.android.settings.biometrics.fingerprint;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.security.ChangeScreenLockPreferenceController;

public class ChangeBiometricSecondFactorScreenLockPreferenceController
        extends ChangeScreenLockPreferenceController {

    private static final String KEY_SECONDARY_SCREEN_LOCK = "biometric_second_factor_screen_lock";

    public ChangeBiometricSecondFactorScreenLockPreferenceController(Context context,
            FingerprintSettingsFragment host) {
        super(context, host);
        mScreenLockPreferenceDetailUtils = new
                BiometricSecondFactorScreenLockPreferenceDetailsUtils(context, host);
    }

    @Override
    public String getPreferenceKey() { return KEY_SECONDARY_SCREEN_LOCK; }


    @Override
    public void updateState(Preference preference) {
        updateSummary(preference, mUserId);
    }
}
