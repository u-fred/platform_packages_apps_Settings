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
import static com.android.internal.widget.LockDomain.Primary;
import static com.android.internal.widget.LockDomain.Secondary;
import static com.android.settings.security.screenlock.AutoPinConfirmPreferenceController.PREF_KEY_PIN_AUTO_CONFIRM;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.WrappedLockPatternUtils;
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
        implements OwnerInfoPreferenceController.OwnerInfoCallback,
        AutoPinConfirmPreferenceController.AutoPinConfirmCallback {

    private static final String TAG = "ScreenLockSettings";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private static final String KEY_LAUNCHED_CONFIRM = "key_launched_confirm";
    private static final String KEY_CREDENTIAL_CONFIRMED = "key_credential_confirmed";

    static final int AUTO_PIN_SETTING_ENABLING_REQUEST_CODE = 111;
    static final int AUTO_PIN_SETTING_DISABLING_REQUEST_CODE = 112;

    public static final int REQUEST_CONFIRM_CREDENTIAL = 113;
    public static final int RESULT_NOT_FOREGROUND = RESULT_FIRST_USER;

    private WrappedLockPatternUtils mLockPatternUtils;
    private LockDomain mLockDomain;
    private boolean mForegroundOnly;
    private boolean mLaunchedConfirm;
    private boolean mCredentialConfirmed;
    private AutoPinConfirmPreferenceController mAutoPinConfirmPreferenceController;

    @Override
    public void onAttach(Context context) {
        // This must be set here so that createPreferenceControllers() can access it.
        mLockDomain = getIntent().getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_LOCK_DOMAIN, LockDomain.class);
        mLockDomain = mLockDomain == null ? Primary : mLockDomain;
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mForegroundOnly = getIntent().getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_FOREGROUND_ONLY, false);

        if (savedInstanceState != null) {
            mLaunchedConfirm = savedInstanceState.getBoolean(KEY_LAUNCHED_CONFIRM);
            mCredentialConfirmed = savedInstanceState.getBoolean(KEY_CREDENTIAL_CONFIRMED);
        }

        if (mLockDomain == Secondary) {
            if (!mLaunchedConfirm && !mCredentialConfirmed) {
                // isPinLock() will always be be true with current implementation of second factor.
                if (LockPatternUtils.isAutoPinConfirmFeatureAvailable() && isPinLock() &&
                        !mLockPatternUtils.refreshStoredPinLength(MY_USER_ID)) {
                    mLaunchedConfirm = true;
                    confirmBiometricSecondFactor();
                }
            }
        }
    }

    private boolean isPinLock() {
        return mLockPatternUtils.getCredentialTypeForUser(MY_USER_ID)
                == LockPatternUtils.CREDENTIAL_TYPE_PIN;
    }

    public void confirmBiometricSecondFactor() {
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(getActivity(), this);
        builder.setTitle(getString(R.string.security_settings_fingerprint_preference_title))
                .setUserId(MY_USER_ID)
                .setForegroundOnly(true)
                .setNotForegroundResultCode(RESULT_NOT_FOREGROUND)
                .setLockDomain(Secondary)
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
        mLockPatternUtils = new WrappedLockPatternUtils(context, mLockDomain);
        List<AbstractPreferenceController> controllers =  buildPreferenceControllers(context,
                this /* parent */, mLockPatternUtils);
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
            DashboardFragment parent, WrappedLockPatternUtils lockPatternUtils) {

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new PatternVisiblePreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new PinPrivacyPreferenceController(
                context, MY_USER_ID, lockPatternUtils));
        controllers.add(new PowerButtonInstantLockPreferenceController(
                context, MY_USER_ID, lockPatternUtils.getInner(), lockPatternUtils.getLockDomain()));
        controllers.add(new LockAfterTimeoutPreferenceController(
                context, MY_USER_ID, lockPatternUtils.getInner(), lockPatternUtils.getLockDomain()));
        controllers.add(new AutoPinConfirmPreferenceController(
                context, MY_USER_ID, lockPatternUtils, parent));
        controllers.add(new OwnerInfoPreferenceController(context, parent,
                lockPatternUtils.getLockDomain()));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_lock_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* parent */,
                            new WrappedLockPatternUtils(context, null));
                }
            };

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTO_PIN_SETTING_ENABLING_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onAutoPinConfirmSettingChange(/* newState= */ true);
            }
        } else if (requestCode == AUTO_PIN_SETTING_DISABLING_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onAutoPinConfirmSettingChange(/* newState= */ false);
            }
        } else if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            mLaunchedConfirm = false;
            if (resultCode == RESULT_OK) {
                mCredentialConfirmed = true;
                // This was originally not available, but confirming credential may have changed
                // it. Try to display it again.
                mAutoPinConfirmPreferenceController.displayPreference(getPreferenceScreen());
            } else if (resultCode == RESULT_NOT_FOREGROUND) {
                setResult(RESULT_NOT_FOREGROUND);
                finish();
            } else {
                // Cancelled.
                finish();
            }
        }
    }

    public void onAutoPinConfirmSettingChange(boolean newState) {
        // update the auto pin confirm setting.
        mLockPatternUtils.setAutoPinConfirm(newState, MY_USER_ID);
        // store the pin length info to disk; If it fails, reset the setting to prev state.
        if (!mLockPatternUtils.refreshStoredPinLength(MY_USER_ID)) {
            mLockPatternUtils.setAutoPinConfirm(!newState, MY_USER_ID);
        }
    }
}
