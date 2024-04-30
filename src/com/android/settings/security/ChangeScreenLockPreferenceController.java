/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class ChangeScreenLockPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, GearPreference.OnGearClickListener {

    public static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";

    protected final SettingsPreferenceFragment mHost;
    protected final UserManager mUm;
    protected final LockPatternUtils mLockPatternUtils;

    protected final int mUserId = UserHandle.myUserId();
    protected final int mProfileChallengeUserId;
    protected final MetricsFeatureProvider mMetricsFeatureProvider;
    protected ScreenLockPreferenceDetailsUtils mScreenLockPreferenceDetailUtils;

    protected RestrictedPreference mPreference;
    private boolean mIsForPrimaryScreenLock;

    public ChangeScreenLockPreferenceController(Context context, SettingsPreferenceFragment host,
                boolean isForPrimaryScreenLock) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mHost = host;
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mIsForPrimaryScreenLock = isForPrimaryScreenLock;
        mScreenLockPreferenceDetailUtils = new ScreenLockPreferenceDetailsUtils(context,
                mIsForPrimaryScreenLock);
    }

    @Override
    public boolean isAvailable() {
        boolean managedProfile = false;
        if (mHost instanceof FingerprintSettingsFragment) {
            // TODO: Look at ChangeProfileScreenLockPreferenceController for determining if managed
            //  profile.
            managedProfile = ((FingerprintSettingsFragment)mHost).getUserId() ==
                    mProfileChallengeUserId;
        }
        return mScreenLockPreferenceDetailUtils.isAvailable(managedProfile);

    }

    @Override
    public String getPreferenceKey() {
        return KEY_UNLOCK_SET_OR_CHANGE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        // In FingerprintSettings all the preferences are removed and added back, but
        // displayResourceTilesToScreen() (which calls displayPreference()) is only called the first
        // time.
        if (!mIsForPrimaryScreenLock && TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            mPreference = ((GearPreference) preference);
        }

        if (mPreference != null && mPreference instanceof GearPreference) {
            if (mScreenLockPreferenceDetailUtils.shouldShowGearMenu()) {
                ((GearPreference) mPreference).setOnGearClickListener(this);
            } else {
                ((GearPreference) mPreference).setOnGearClickListener(null);
            }
        }

        updateSummary(preference, mUserId);

        // There is no way to manage biometric second factor password quality.
        if (mIsForPrimaryScreenLock) {
            disableIfPasswordQualityManaged(mUserId);
            if (!mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)) {
                // PO may disallow to change password for the profile, but screen lock and managed
                // profile's lock is the same. Disable main "Screen lock" menu.
                disableIfPasswordQualityManaged(mProfileChallengeUserId);
            }
        }
    }

    @Override
    public void onGearClick(GearPreference p) {
        if (TextUtils.equals(p.getKey(), getPreferenceKey())) {
            if (mHost instanceof FingerprintSettingsFragment) {
                ((FingerprintSettingsFragment)mHost).setLaunchedBiometricSecondFactor(true);
                // TODO: Could call getPinLength and only launch confirm if it's not available.
                //  Issue is this makes the UI inconsistent. Verifying secondary PIN always isn't
                //  a big issue.
                //((FingerprintSettingsFragment)mHost).launchChooseOrConfirmLock();
                //return;
            }

            mMetricsFeatureProvider.logClickedPreference(p,
                    p.getExtras().getInt(DashboardFragment.CATEGORY));
            mScreenLockPreferenceDetailUtils.openScreenLockSettings(mHost.getMetricsCategory());
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        LockscreenCredential password = null;
        if (mHost instanceof FingerprintSettingsFragment) {
            password = ((FingerprintSettingsFragment) mHost).getUserPassword();
        }
        return mScreenLockPreferenceDetailUtils.openChooseLockGenericFragment(
                mHost.getMetricsCategory(), password);
    }

    protected void updateSummary(Preference preference, int userId) {
        preference.setSummary(mScreenLockPreferenceDetailUtils.getSummary(userId));
        mPreference.setEnabled(true);
    }

    /**
     * Sets the preference as disabled by admin if PASSWORD_QUALITY_MANAGED is set.
     * The preference must be a RestrictedPreference.
     * <p/>
     * DO or PO installed in the user may disallow to change password.
     */
    void disableIfPasswordQualityManaged(int userId) {
        final RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtilsInternal
                .checkIfPasswordQualityIsSet(mContext, userId);
        if (mScreenLockPreferenceDetailUtils.isPasswordQualityManaged(userId, admin)) {
            mPreference.setDisabledByAdmin(admin);
        }
    }
}
