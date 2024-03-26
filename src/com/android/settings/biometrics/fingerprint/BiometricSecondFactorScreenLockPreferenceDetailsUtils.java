package com.android.settings.biometrics.fingerprint;

import static android.provider.Settings.Secure.BIOMETRIC_KEYGUARD_ENABLED;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;

public class BiometricSecondFactorScreenLockPreferenceDetailsUtils extends
        ScreenLockPreferenceDetailsUtils {
    private FingerprintSettingsFragment mHost;

    public BiometricSecondFactorScreenLockPreferenceDetailsUtils(Context context,
            FingerprintSettingsFragment host) {
        super(context, false);
        mHost = host;
    }

    /**
     * Returns whether the Gear Menu should be shown. Currently it is unused, but this could change
     * in future.
     */
    public boolean shouldShowGearMenu() {
        return false;
    }

    /**
     * Returns whether the screen lock settings entity should be shown.
     */
    @Override
    public boolean isAvailable() {
        // This preference is never available for managed profiles.
        boolean managedProfile = mHost.getUserId() == mProfileChallengeUserId;

        boolean biometricKeyguardEnabled = Settings.Secure.getIntForUser(
                mContext.getContentResolver(),
                BIOMETRIC_KEYGUARD_ENABLED,
                FingerprintSettingsKeyguardPreferenceController.DEFAULT,
                mUserId) == FingerprintSettingsKeyguardPreferenceController.ON;
        //boolean managedProfile = UserManager.get(mContext).isManagedProfile(mHost.getUserId());

        return super.isAvailable() && biometricKeyguardEnabled && !managedProfile;
    }

    /**
     * Returns {@link Intent} to launch {@link ChooseLockGeneric.ChooseLockGenericFragment}.
     */
    @Override
    protected Intent getChooseLockGenericFragmentIntent(int sourceMetricsCategory) {
        Intent i = super.getChooseLockGenericFragmentIntent(sourceMetricsCategory);
        i.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PRIMARY_CREDENTIAL, false);
        i.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, mHost.mUserPassword);
        i.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, false);
        return i;
    }

}
