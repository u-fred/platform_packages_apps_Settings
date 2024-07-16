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

import static com.android.internal.widget.LockDomain.Primary;
import static com.android.internal.widget.LockDomain.Secondary;
import static com.android.internal.widget.LockPatternUtils.MIN_AUTO_PIN_REQUIREMENT_LENGTH;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

/**
 * Preference controller for the pin_auto_confirm setting.
 */
public class AutoPinConfirmPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    public static final String PREF_KEY_PIN_AUTO_CONFIRM = "auto_pin_confirm";

    private final int mUserId;
    private final WrappedLockPatternUtils mLockPatternUtils;
    private final ObservablePreferenceFragment mParentFragment;
    private final AutoPinConfirmSettingChangeCallback mCallback;

    public AutoPinConfirmPreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils,
            ObservablePreferenceFragment parentFragment,
            LockDomain lockDomain,
            AutoPinConfirmSettingChangeCallback callback) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = new WrappedLockPatternUtils(lockPatternUtils, lockDomain);
        mParentFragment = parentFragment;
        mCallback = callback;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mLockPatternUtils.getLockDomain() == Primary) {
            launchPinConfirmActivity((boolean) newValue);
        } else if (mCallback != null) {
            mCallback.call((boolean) newValue);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(getPinAutoConfirmSettingState());
    }

    @Override
    public boolean isAvailable() {
        return LockPatternUtils.isAutoPinConfirmFeatureAvailable() &&
                isPinLock() && isPinLengthEligibleForAutoConfirmation();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_PIN_AUTO_CONFIRM;
    }

    private boolean isPinLock() {
        // TODO: Make sure mUserId supports second factor if secondary.
        return mLockPatternUtils.getCredentialTypeForUser(mUserId)
                == LockPatternUtils.CREDENTIAL_TYPE_PIN;
    }

    private boolean isPinLengthEligibleForAutoConfirmation() {
        return mLockPatternUtils.getPinLength(mUserId) >= MIN_AUTO_PIN_REQUIREMENT_LENGTH;
    }

    private boolean getPinAutoConfirmSettingState() {
        return mLockPatternUtils.isAutoPinConfirmEnabled(mUserId);
    }

    private void setPinAutoConfirmSettingState(boolean state) {
        mLockPatternUtils.setAutoPinConfirm(state, mUserId);
    }

    private void launchPinConfirmActivity(boolean newState) {
        new ChooseLockSettingsHelper.Builder(mParentFragment.getActivity(), mParentFragment)
                .setUserId(mUserId)
                .setRequestCode(newState
                        ? ScreenLockSettings.AUTO_PIN_SETTING_ENABLING_REQUEST_CODE
                        : ScreenLockSettings.AUTO_PIN_SETTING_DISABLING_REQUEST_CODE)
                .setTitle(mContext.getString(R.string.lock_screen_auto_pin_confirm_title))
                .setDescription(newState
                        ? mContext.getString(R.string.auto_confirm_on_pin_verify_description)
                        : mContext.getString(R.string.auto_confirm_off_pin_verify_description))
                .setReturnCredentials(true)
                .show();
    }

    public interface AutoPinConfirmSettingChangeCallback {
        void call(boolean newState);
    }
}
