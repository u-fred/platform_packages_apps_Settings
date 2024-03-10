/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static com.android.settings.security.screenlock.AutoPinConfirmPreferenceController.AutoPinConfirmSettingChangeCallback;
import static com.android.settings.security.screenlock.AutoPinConfirmPreferenceController.PREF_KEY_PIN_AUTO_CONFIRM;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.OwnerInfoPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ScreenLockSettings extends DashboardFragment
        implements OwnerInfoPreferenceController.OwnerInfoCallback {

    private static final String TAG = "ScreenLockSettings";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private static final String KEY_LAUNCHED_CONFIRM = "key_launched_confirm";
    private static final String KEY_CREDENTIAL_CONFIRMED = "key_credential_confirmed";

    static final int AUTO_PIN_SETTING_ENABLING_REQUEST_CODE = 111;
    static final int AUTO_PIN_SETTING_DISABLING_REQUEST_CODE = 112;

    public static final int REQUEST_CONFIRM_CREDENTIAL = 113;
    public static final int RESULT_NOT_FOREGROUND = RESULT_FIRST_USER;

    private LockPatternUtils mLockPatternUtils;
    private boolean mIsForPrimaryScreenLock;
    private boolean mForegroundOnly;
    private boolean mLaunchedConfirm;
    private boolean mCredentialConfirmed;
    private AutoPinConfirmPreferenceController mAutoPinConfirmPreferenceController;

    @Override
    public void onAttach(Context context) {
        mIsForPrimaryScreenLock = getIntent().getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_PRIMARY_CREDENTIAL, true);
        mForegroundOnly = getIntent().getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_FOREGROUND_ONLY, false);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLaunchedConfirm = savedInstanceState.getBoolean(KEY_LAUNCHED_CONFIRM);
            mCredentialConfirmed = savedInstanceState.getBoolean(KEY_CREDENTIAL_CONFIRMED);
        }

        if (!mIsForPrimaryScreenLock) {
            if (!mLaunchedConfirm && !mCredentialConfirmed) {
                if (mLockPatternUtils.getPinLength(MY_USER_ID, false) ==
                        LockPatternUtils.PIN_LENGTH_UNAVAILABLE) {
                    mLaunchedConfirm = true;
                    confirmBiometricSecondFactor();
                }
            }
        } else {
            mCredentialConfirmed = true;
        }
    }

    public void confirmBiometricSecondFactor() {
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(getActivity(), this);
        builder.setTitle(getString(R.string.security_settings_fingerprint_preference_title))
                .setUserId(MY_USER_ID)
                .setForegroundOnly(true)
                .setNotForegroundDistinctResultCode(true)
                .setPrimaryCredential(false)
                .setRequestCode(REQUEST_CONFIRM_CREDENTIAL)
                .show();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_LOCK_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_lock_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mLockPatternUtils = new LockPatternUtils(context);
        List<AbstractPreferenceController> controllers =  buildPreferenceControllers(context,
                this /* parent */, mLockPatternUtils, mIsForPrimaryScreenLock,
                this::onAutoPinConfirmSettingChange);
        for (AbstractPreferenceController controller : controllers) {
            if (controller.getPreferenceKey() == PREF_KEY_PIN_AUTO_CONFIRM) {
                mAutoPinConfirmPreferenceController =
                        (AutoPinConfirmPreferenceController) controller;
            }
        }
        return controllers;
    }

    @Override
    public void onOwnerInfoUpdated() {
        use(OwnerInfoPreferenceController.class).updateSummary();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations() && !mLaunchedConfirm && mForegroundOnly) {
            setResult(RESULT_NOT_FOREGROUND);
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
        outState.putBoolean(KEY_CREDENTIAL_CONFIRMED, mCredentialConfirmed);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            DashboardFragment parent, LockPatternUtils lockPatternUtils,
            boolean isForPrimaryScreenLock, AutoPinConfirmSettingChangeCallback callback) {

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new PatternVisiblePreferenceController(
                context, MY_USER_ID, lockPatternUtils, isForPrimaryScreenLock));
        controllers.add(new PinPrivacyPreferenceController(
                context, MY_USER_ID, lockPatternUtils, isForPrimaryScreenLock));
        controllers.add(new PowerButtonInstantLockPreferenceController(
                context, MY_USER_ID, lockPatternUtils, isForPrimaryScreenLock));
        controllers.add(new LockAfterTimeoutPreferenceController(
                context, MY_USER_ID, lockPatternUtils, isForPrimaryScreenLock));
        controllers.add(new AutoPinConfirmPreferenceController(
                context, MY_USER_ID, lockPatternUtils, parent, isForPrimaryScreenLock, callback));
        controllers.add(new OwnerInfoPreferenceController(context, parent, isForPrimaryScreenLock));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_lock_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* parent */,
                            new LockPatternUtils(context), true, null);
                }
            };

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTO_PIN_SETTING_ENABLING_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                onAutoPinConfirmSettingChange(/* newState= */ true);
            }
        } else if (requestCode == AUTO_PIN_SETTING_DISABLING_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                onAutoPinConfirmSettingChange(/* newState= */ false);
            }
        } else if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            mLaunchedConfirm = false;
            if (resultCode == RESULT_OK) {
                // Requires biometric second factor metrics to be cached in LockSettingService.
                mAutoPinConfirmPreferenceController.displayPreference(getPreferenceScreen());
            } else if (resultCode == RESULT_NOT_FOREGROUND){
                setResult(RESULT_NOT_FOREGROUND);
                finish();
            } else {
                // Cancelled.
                finish();
            }
        }
    }

    private void onAutoPinConfirmSettingChange(boolean newState) {
        // update the auto pin confirm setting.
        mLockPatternUtils.setAutoPinConfirm(newState, MY_USER_ID, mIsForPrimaryScreenLock);
        // store the pin length info to disk; If it fails, reset the setting to prev state.
        if (!mLockPatternUtils.refreshStoredPinLength(MY_USER_ID, mIsForPrimaryScreenLock)) {
            mLockPatternUtils.setAutoPinConfirm(!newState, MY_USER_ID, mIsForPrimaryScreenLock);
        }
    }
}
