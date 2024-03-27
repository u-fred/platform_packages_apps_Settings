package com.android.settings.biometrics.fingerprint;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.security.ChangeScreenLockPreferenceController;

// TODO: Delete biometric second factor on deletion of last fingerprint?

public class ChangeBiometricSecondFactorPreferenceController
        extends ChangeScreenLockPreferenceController {

    // We would prefer to pass this through the constructor, but this is how it's done in base
    // class.
    public static final String KEY_BIOMETRIC_SECOND_FACTOR = "biometric_second_factor";

    public ChangeBiometricSecondFactorPreferenceController(Context context,
            FingerprintSettingsFragment host) {
        super(context, host);
        mScreenLockPreferenceDetailUtils = new
                BiometricSecondFactorPreferenceDetailsUtils(context, host);
    }

    @Override
    public String getPreferenceKey() { return KEY_BIOMETRIC_SECOND_FACTOR; }


    @Override
    public void updateState(Preference preference) {
        updateSummary(preference, mUserId);
    }

}
