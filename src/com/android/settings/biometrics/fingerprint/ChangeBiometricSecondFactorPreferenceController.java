package com.android.settings.biometrics.fingerprint;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.security.ChangeScreenLockPreferenceController;

// TODO: rename to biometricsecondfactor
// TODO: Delete biometric second factor on deletion of last fingerprint?
// TODO: all UI strings
// TODO: credential handling


public class ChangeBiometricSecondFactorPreferenceController
        extends ChangeScreenLockPreferenceController {

    private static final String KEY_SECONDARY_SCREEN_LOCK = "biometric_second_factor_screen_lock";

    public ChangeBiometricSecondFactorPreferenceController(Context context,
            FingerprintSettingsFragment host) {
        super(context, host);
        mScreenLockPreferenceDetailUtils = new
                BiometricSecondFactorPreferenceDetailsUtils(context, host);
    }

    @Override
    public String getPreferenceKey() { return KEY_SECONDARY_SCREEN_LOCK; }


    @Override
    public void updateState(Preference preference) {
        updateSummary(preference, mUserId);
    }

}
