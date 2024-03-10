package com.android.settings.biometrics.combination;

import android.content.Context;
import android.content.Intent;

import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;


public class BiometricSecondFactorScreenLockPreferenceDetailsUtils extends
        ScreenLockPreferenceDetailsUtils {

    public BiometricSecondFactorScreenLockPreferenceDetailsUtils(Context context) {
        super(context, false);
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
