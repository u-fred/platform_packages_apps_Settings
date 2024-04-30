/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.provider.Settings.Secure.BIOMETRIC_KEYGUARD_ENABLED;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.fingerprint.FingerprintSettingsKeyguardPreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.transition.SettingsTransitionHelper;

/**
 * Utilities for screen lock details shared between Security Settings and Safety Center.
 */
public class ScreenLockPreferenceDetailsUtils {

    protected final int mUserId = UserHandle.myUserId();
    protected final Context mContext;
    protected final LockPatternUtils mLockPatternUtils;
    protected final int mProfileChallengeUserId;
    protected final UserManager mUm;
    protected boolean mIsForPrimaryScreenLock;

    public ScreenLockPreferenceDetailsUtils(Context context, boolean isForPrimaryScreenLock) {
        mContext = context;
        mUm = context.getSystemService(UserManager.class);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
        mIsForPrimaryScreenLock = isForPrimaryScreenLock;
    }

    /**
     * Returns whether the screen lock settings entity should be shown.
     */
    public boolean isAvailable(boolean managedProfile) {
        if (!mContext.getResources().getBoolean(R.bool.config_show_unlock_set_or_change)) {
            return false;
        }

        if (mIsForPrimaryScreenLock) {
            return true;
        } else {
            // TODO: Call this method in FingerprintSettingsKeyguardPreferenceController possibly
            //  via FingerprintSettings.
            return !managedProfile && Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    BIOMETRIC_KEYGUARD_ENABLED,
                    FingerprintSettingsKeyguardPreferenceController.DEFAULT,
                    mUserId) == FingerprintSettingsKeyguardPreferenceController.ON;
        }
    }

    /**
     * Returns the summary of screen lock settings entity.
     */
    public String getSummary(int userId) {
        final Integer summaryResId = getSummaryResId(userId);
        return summaryResId != null ? mContext.getResources().getString(summaryResId) : null;
    }


    /**
     * Returns whether the password quality is managed by device admin.
     */
    public boolean isPasswordQualityManaged(int userId, RestrictedLockUtils.EnforcedAdmin admin) {
        if (!mIsForPrimaryScreenLock) {
            return false;
        }
        final DevicePolicyManager dpm = (DevicePolicyManager) mContext
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        return admin != null && dpm.getPasswordQuality(admin.component, userId)
                == DevicePolicyManager.PASSWORD_QUALITY_MANAGED;
    }

    /**
     * Returns whether the lock pattern is secure.
     */
    public boolean isLockPatternSecure() {
        return mLockPatternUtils.isSecure(mUserId, mIsForPrimaryScreenLock);
    }

    /**
     * Returns whether the Gear Menu should be shown.
     */
    public boolean shouldShowGearMenu() {
        return isLockPatternSecure();
    }

    /**
     * Launches the {@link ScreenLockSettings}.
     */
    public void openScreenLockSettings(int sourceMetricsCategory) {
        mContext.startActivity(getLaunchScreenLockSettingsIntent(sourceMetricsCategory));
    }

    /**
     * Returns {@link Intent} to launch the {@link ScreenLockSettings}.
     */
    public Intent getLaunchScreenLockSettingsIntent(int sourceMetricsCategory) {
        Bundle extras = new Bundle();
        if (!mIsForPrimaryScreenLock) {
            extras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_PRIMARY_CREDENTIAL, false);
        }
        return new SubSettingLauncher(mContext)
                .setDestination(ScreenLockSettings.class.getName())
                .setSourceMetricsCategory(sourceMetricsCategory)
                .setExtras(extras)
                .toIntent();
    }

    /**
     * Tries to launch the {@link ChooseLockGenericFragment} if Quiet Mode is not enabled
     * for managed profile, otherwise shows a dialog to disable the Quiet Mode.
     *
     * @return true if the {@link ChooseLockGenericFragment} is launching.
     */
    public boolean openChooseLockGenericFragment(int sourceMetricsCategory,
            @Nullable LockscreenCredential password) {
        final Intent quietModeDialogIntent = getQuietModeDialogIntent();
        if (quietModeDialogIntent != null) {
            mContext.startActivity(quietModeDialogIntent);
            return false;
        }
        mContext.startActivity(getChooseLockGenericFragmentIntent(sourceMetricsCategory, password));
        return true;
    }


    /**
     * Returns {@link Intent} to launch an appropriate Settings screen.
     *
     * <p>If Quiet Mode is enabled for managed profile, returns {@link Intent} to launch a dialog
     * to disable the Quiet Mode, otherwise returns {@link Intent} to launch
     * {@link ChooseLockGenericFragment}.
     */
    public Intent getLaunchChooseLockGenericFragmentIntent(int sourceMetricsCategory) {
        final Intent quietModeDialogIntent = getQuietModeDialogIntent();
        return quietModeDialogIntent != null ? quietModeDialogIntent
                : getChooseLockGenericFragmentIntent(sourceMetricsCategory, null);
    }

    protected Intent getQuietModeDialogIntent() {
        // TODO(b/35930129): Remove once existing password can be passed into vold directly.
        // Currently we need this logic to ensure that the QUIET_MODE is off for any work
        // profile with unified challenge on FBE-enabled devices. Otherwise, vold would not be
        // able to complete the operation due to the lack of (old) encryption key.
        if (mProfileChallengeUserId != UserHandle.USER_NULL
                && !mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)
                && StorageManager.isFileEncrypted()) {
            if (mUm.isQuietModeEnabled(UserHandle.of(mProfileChallengeUserId))) {
                return UnlaunchableAppActivity.createInQuietModeDialogIntent(
                        mProfileChallengeUserId);
            }
        }
        return null;
    }

    protected Intent getChooseLockGenericFragmentIntent(int sourceMetricsCategory,
            @Nullable LockscreenCredential password) {
        Bundle extras = new Bundle();
        if (!mIsForPrimaryScreenLock) {
            extras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_PRIMARY_CREDENTIAL, false);
        }
        if (password != null) {
            // TODO: Test if user password is not set. Make sure it doesn't happen.
            extras.putObject(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, password);
            // TODO: Why need this if we also have password?
            extras.putBoolean(ChooseLockGeneric.CONFIRM_CREDENTIALS, false);
        }

        return new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGenericFragment.class.getName())
                .setSourceMetricsCategory(sourceMetricsCategory)
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                .setExtras(extras)
                .toIntent();
    }

    @StringRes
    private Integer getSummaryResId(int userId) {
        // TODO: Look at base commit when updating this.
        if (!mLockPatternUtils.isSecure(userId, mIsForPrimaryScreenLock)) {
            if (userId == mProfileChallengeUserId
                    || mLockPatternUtils.isLockScreenDisabled(userId, mIsForPrimaryScreenLock)) {
                return R.string.unlock_set_unlock_mode_off;
            } else {
                return R.string.unlock_set_unlock_mode_none;
            }
        } else {
            int keyguardStoredPasswordQuality =
                    mLockPatternUtils.getKeyguardStoredPasswordQuality(userId, mIsForPrimaryScreenLock);
            switch (keyguardStoredPasswordQuality) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return R.string.unlock_set_unlock_mode_pattern;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return R.string.unlock_set_unlock_mode_pin;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    return R.string.unlock_set_unlock_mode_password;
                default:
                    return null;
            }
        }
    }
}
