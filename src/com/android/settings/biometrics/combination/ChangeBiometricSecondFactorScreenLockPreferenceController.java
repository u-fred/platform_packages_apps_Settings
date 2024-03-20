package com.android.settings.biometrics.combination;

import android.content.Context;
import androidx.preference.Preference;

import com.android.settings.security.ChangeScreenLockPreferenceController;
import com.android.settings.widget.GearPreference;

public class ChangeBiometricSecondFactorScreenLockPreferenceController
        extends ChangeScreenLockPreferenceController {

    private static final String KEY_SECONDARY_SCREEN_LOCK = "biometric_second_factor_screen_lock";
    public ChangeBiometricSecondFactorScreenLockPreferenceController(Context context,
            BiometricsSettingsBase host) {
        super(context, host);
        mScreenLockPreferenceDetailUtils = new
                BiometricSecondFactorScreenLockPreferenceDetailsUtils(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SECONDARY_SCREEN_LOCK;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        //mPreference.setEnabled(false);
        //mPreference.setSummary("Enroll fingerprint to enable");
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        // TODO: Don't require user to enter primary auth when opening ChooseLockGenericFragment
        //  because they already entered it to access this setting. Will probably need to modify
        //  the fragment to accept the password in an intent. Look to see if this already exists
        //  in the fragment, such as confirmCredentials, but probably not.

        // TODO: Disable optiosn other than "PIN" and "None" in ChooseLockGeneric. Could do this
        //  with intent, or maybe using a different xml file.

        // Prevent host activity from finishing itself.
        ((BiometricsSettingsBase)mHost).mDoNotFinishActivity = true;
        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public void onGearClick(GearPreference p) {
        // Prevent host activity from finishing itself.
        ((BiometricsSettingsBase)mHost).mDoNotFinishActivity = true;
        super.onGearClick(p);
    }

}
