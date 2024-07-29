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

import static com.android.internal.widget.LockDomain.Primary;
import static com.android.internal.widget.LockDomain.Secondary;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
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
    // For Primary LockDomain this should always be mUserId as there is a separate controller
    // for profiles. For Secondary it should either be mUserId or mProfileChallengeUserId.
    private int mEffectiveUserId;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final ScreenLockPreferenceDetailsUtils mScreenLockPreferenceDetailUtils;

    protected RestrictedPreference mPreference;

    private final LockDomain mLockDomain;
    @Nullable private LockscreenCredential mUserPassword;
    private int mScreenLockSettingsRequestCode;
    private int mChooseLockRequestCode;
    @Nullable private SettingsPreferenceFragment mResultListener;

    public ChangeScreenLockPreferenceController(Context context, SettingsPreferenceFragment host) {
        this(context, host, Primary);
    }

    public ChangeScreenLockPreferenceController(Context context, SettingsPreferenceFragment host,
                LockDomain lockDomain) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mHost = host;
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mLockDomain = lockDomain;
        mScreenLockPreferenceDetailUtils = new ScreenLockPreferenceDetailsUtils(context, lockDomain);
        mEffectiveUserId = mUserId;
    }

    public void setEffectiveUserId(int userId) {
        mEffectiveUserId = userId;
    }

    public void setUserPassword(LockscreenCredential password) {
        mUserPassword = password;
    }

    public void setScreenLockSettingsRequestCode(int value) {
        mScreenLockSettingsRequestCode = value;
    }

    public void setChooseLockRequestCode(int value) {
        mChooseLockRequestCode = value;
    }

    public void setResultListener(SettingsPreferenceFragment value) {
        mResultListener = value;
    }

    @Override
    public boolean isAvailable() {
        return mScreenLockPreferenceDetailUtils.isAvailable(mEffectiveUserId);
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
        // Need to set mPreference here for Secondary as FingerprintSettings removes all the prefs
        // then adds them back. displayResourceTilesToScreen() which calls displayPreference() is
        // only called the first time.
        if (mLockDomain == Secondary && TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            mPreference = ((GearPreference) preference);
        }

        if (mPreference != null && mPreference instanceof GearPreference) {
            if (mScreenLockPreferenceDetailUtils.shouldShowGearMenu()) {
                if (mHost instanceof GearPreference.OnGearClickListener) {
                    ((GearPreference) mPreference).setOnGearClickListener(
                            (GearPreference.OnGearClickListener)mHost);
                } else {
                    ((GearPreference) mPreference).setOnGearClickListener(this);
                }
            } else {
                ((GearPreference) mPreference).setOnGearClickListener(null);
            }
        }

        // updateState() is only called if preference is available so we can be sure
        // mEffectiveUserId supports second factor if this is a Secondary controller.
        updateSummary(preference, mEffectiveUserId);

        // There is no way to manage biometric second factor password quality.
        if (mLockDomain == Primary) {
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
            mMetricsFeatureProvider.logClickedPreference(p,
                    p.getExtras().getInt(DashboardFragment.CATEGORY));
            mScreenLockPreferenceDetailUtils.openScreenLockSettings(mHost.getMetricsCategory(),
                    mResultListener, mScreenLockSettingsRequestCode);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        return mScreenLockPreferenceDetailUtils.openChooseLockGenericFragment(
                mHost.getMetricsCategory(), mUserPassword, mResultListener, mChooseLockRequestCode);
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
