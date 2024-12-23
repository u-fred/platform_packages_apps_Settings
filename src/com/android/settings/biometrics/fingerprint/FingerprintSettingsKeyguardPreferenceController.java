/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.biometrics.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

// based on src/com/android/settings/biometrics/combination/BiometricSettingsKeyguardPreferenceController.java
// from android-14.0.0_r1
public class FingerprintSettingsKeyguardPreferenceController extends TogglePreferenceController {
    private int mUserId;
    private final LockPatternUtils mLockPatternUtils;

    public FingerprintSettingsKeyguardPreferenceController(Context context, String key) {
        super(context, key);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
    }

    protected RestrictedLockUtils.EnforcedAdmin getRestrictingAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(mContext,
                DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS, mUserId);
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    @Override
    public boolean isChecked() {
        return mLockPatternUtils.isBiometricKeyguardEnabled(mUserId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return mLockPatternUtils.setBiometricKeyguardEnabled(mUserId, isChecked);
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserManager.get(mContext).isManagedProfile(mUserId)) {
            return DISABLED_FOR_USER;
        }

        return getAvailabilityFromRestrictingAdmin();
    }

    private int getAvailabilityFromRestrictingAdmin() {
        return getRestrictingAdmin() != null ? DISABLED_FOR_USER : AVAILABLE;
    }

    @Override
    public final boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
