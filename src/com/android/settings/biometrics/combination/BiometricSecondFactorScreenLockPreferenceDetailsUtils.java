package com.android.settings.biometrics.combination;

import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;


public class BiometricSecondFactorScreenLockPreferenceDetailsUtils extends
        ScreenLockPreferenceDetailsUtils {

    private FingerprintManager mFingerprintManager;

    public BiometricSecondFactorScreenLockPreferenceDetailsUtils(Context context) {
        super(context, false);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
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
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_unlock_set_or_change);
        // TODO: Implement this.
        // if (mFingerprintManager.hasEnrolledTemplates()) {
            // return DISABLED_DEPENDENT_SETTING
        // }
    }

    /**
     * Returns {@link Intent} to launch the {@link BiometricSecondFactorScreenLockSettings}.
     */
    @Override
    public Intent getLaunchScreenLockSettingsIntent(int sourceMetricsCategory) {
        // TODO: add
        return new SubSettingLauncher(mContext)
                .setDestination(BiometricSecondFactorScreenLockSettings.class.getName())
                .setSourceMetricsCategory(sourceMetricsCategory)
                .toIntent();
    }

    /**
     * Returns {@link Intent} to launch {@link ChooseLockGeneric.ChooseLockGenericFragment}.
     */
    protected Intent getChooseLockGenericFragmentIntent(int sourceMetricsCategory) {
        Intent i = super.getChooseLockGenericFragmentIntent(sourceMetricsCategory);
        return i.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PRIMARY_CREDENTIAL, false);
    }

}
