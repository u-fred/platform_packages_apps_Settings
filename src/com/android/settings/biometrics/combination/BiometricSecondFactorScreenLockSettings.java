package com.android.settings.biometrics.combination;

import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class BiometricSecondFactorScreenLockSettings extends DashboardFragment {
    private static final String TAG = "BiometricScreenLockSettings";

    protected final int mUserId = UserHandle.myUserId();

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.biometric_second_factor_screen_lock_settings;
    }

    // TODO: Change this (refer to ScreenLockSettings).
    @Override
    public int getMetricsCategory() { return 0; }
}
