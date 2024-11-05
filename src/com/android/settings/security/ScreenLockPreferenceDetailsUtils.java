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

import static com.android.internal.widget.LockDomain.Primary;
import static com.android.internal.widget.LockDomain.Secondary;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.transition.SettingsTransitionHelper;

/**
 * Utilities for screen lock details shared between Security Settings and Safety Center.
 */
public class ScreenLockPreferenceDetailsUtils {

    private final int mUserId = UserHandle.myUserId();
    private final Context mContext;
    private final WrappedLockPatternUtils mLockPatternUtils;
    private final int mProfileChallengeUserId;
    private final UserManager mUm;
    private LockDomain mLockDomain;

    public ScreenLockPreferenceDetailsUtils(Context context) {
        this(context, Primary);
    }

    public ScreenLockPreferenceDetailsUtils(Context context, LockDomain lockDomain) {
        mContext = context;
        mUm = context.getSystemService(UserManager.class);
        mLockDomain = lockDomain;
        LockPatternUtils inner = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mLockPatternUtils = new WrappedLockPatternUtils(inner, lockDomain);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
    }

    protected boolean isFingerprintKeyguardDisabledByAdmin(int userId) {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(mContext,
                DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, userId) != null;
    }

    public boolean isAvailable() {
        return isAvailable(-1 /** ignored **/);
    }

    /**
     * Returns whether the screen lock settings entity should be shown.
     *
     * @param userId The user for which to check availability. Ignored if mLockDomain is Primary.
     */
    public boolean isAvailable(int userId) {
        if (!mContext.getResources().getBoolean(R.bool.config_show_unlock_set_or_change)) {
            return false;
        } else if (mLockDomain == Primary) {
            return true;
        } else if (!mLockPatternUtils.checkUserSupportsBiometricSecondFactor(userId, false)) {
           return false;
        } else {
            return mLockPatternUtils.isBiometricKeyguardEnabled(userId) &&
                    !isFingerprintKeyguardDisabledByAdmin(userId);
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
        final DevicePolicyManager dpm = (DevicePolicyManager) mContext
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        return admin != null && dpm.getPasswordQuality(admin.component, userId, mLockDomain)
                == DevicePolicyManager.PASSWORD_QUALITY_MANAGED;
    }

    /**
     * Returns whether the lock pattern is secure.
     */
    public boolean isLockPatternSecure() {
        return mLockPatternUtils.isSecure(mUserId);
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
    public void openScreenLockSettings(int sourceMetricsCategory,
            @Nullable SettingsPreferenceFragment resultListener, int requestCode) {
        if (resultListener == null) {
            mContext.startActivity(getLaunchScreenLockSettingsIntent(sourceMetricsCategory));
        } else {
            resultListener.startActivityForResult(
                    getLaunchScreenLockSettingsIntent(sourceMetricsCategory), requestCode);
        }
    }

    /**
     * Returns {@link Intent} to launch the {@link ScreenLockSettings}.
     */
    public Intent getLaunchScreenLockSettingsIntent(int sourceMetricsCategory) {
        Bundle extras = new Bundle();
        if (mLockDomain == Secondary) {
            extras.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_LOCK_DOMAIN, Secondary);
            extras.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOREGROUND_ONLY, true);
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
            @Nullable LockscreenCredential password,
            @Nullable SettingsPreferenceFragment resultListener,
            int requestCode) {
        final Intent quietModeDialogIntent = getQuietModeDialogIntent();
        if (quietModeDialogIntent != null) {
            mContext.startActivity(quietModeDialogIntent);
            return false;
        }

        Intent chooseLockGenericIntent = getChooseLockGenericFragmentIntent(
                sourceMetricsCategory, password);
        if (resultListener == null) {
            mContext.startActivity(chooseLockGenericIntent);
        } else {
            resultListener.startActivityForResult(chooseLockGenericIntent, requestCode);
        }
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

    private Intent getQuietModeDialogIntent() {
        if (mLockDomain == Secondary) {
            return null;
        }

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
        if (mLockDomain == Secondary) {
            extras.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_LOCK_DOMAIN, Secondary);
        }

        Bundle args = new Bundle();
        if (password != null) {
            args.putObject(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, password);
        }

        return new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGenericFragment.class.getName())
                .setSourceMetricsCategory(sourceMetricsCategory)
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                .setExtras(extras)
                .setArguments(args)
                .toIntent();
    }

    @StringRes
    private Integer getSummaryResId(int userId) {
        if (!mLockPatternUtils.isSecure(userId)) {
            if (userId == mProfileChallengeUserId
                    || mLockPatternUtils.isLockScreenDisabled(userId)) {
                return R.string.unlock_set_unlock_mode_off;
            } else {
                return R.string.unlock_set_unlock_mode_none;
            }
        } else {
            int keyguardStoredPasswordQuality =
                    mLockPatternUtils.getKeyguardStoredPasswordQuality(userId);
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
