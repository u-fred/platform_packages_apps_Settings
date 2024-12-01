/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.security.screenlock;

import static com.android.internal.widget.LockPatternUtils.MIN_AUTO_PIN_REQUIREMENT_LENGTH;

import android.annotation.NonNull;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

/**
 * Controller for enabling/disabling biometric second factor PIN auto confirm.
 * Based on upstream's AutoPinConfirmPreferenceController.
 */
public class BiometricSecondFactorPinAutoConfirmPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin,
        Preference.OnPreferenceChangeListener {

    public static final String PREF_KEY_PIN_AUTO_CONFIRM = "auto_pin_confirm";

    private final int mUserId;
    private final WrappedLockPatternUtils mLockPatternUtils;

    public BiometricSecondFactorPinAutoConfirmPreferenceController(Context context, int userId,
            WrappedLockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_PIN_AUTO_CONFIRM;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(getPinAutoConfirmSettingState());
    }

    private boolean getPinAutoConfirmSettingState() {
        return mLockPatternUtils.isAutoPinConfirmEnabled(mUserId);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        mLockPatternUtils.setAutoPinConfirm((boolean)newValue, mUserId);
        // store the pin length info to disk; If it fails, reset the setting to prev state.
        if (!mLockPatternUtils.refreshStoredPinLength(mUserId)) {
            mLockPatternUtils.setAutoPinConfirm(!(boolean)newValue, mUserId);
            updateState(preference);
        }
        return true;
    }

    @Override
    public boolean isAvailable() {
        return LockPatternUtils.isAutoPinConfirmFeatureAvailable()
                && isPinLengthEligibleForAutoConfirmation();
    }

    private boolean isPinLengthEligibleForAutoConfirmation() {
        return mLockPatternUtils.getPinLength(mUserId) >= MIN_AUTO_PIN_REQUIREMENT_LENGTH;
    }
}
