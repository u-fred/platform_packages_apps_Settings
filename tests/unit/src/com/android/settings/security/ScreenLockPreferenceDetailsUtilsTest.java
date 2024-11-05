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
import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.StorageManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ScreenLockPreferenceDetailsUtilsTest {

    private static final int SOURCE_METRICS_CATEGORY = 10;
    private static final int USER_ID = 11;

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private UserManager mUserManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private Resources mResources;
    @Mock
    private StorageManager mStorageManager;

    private Context mContext;

    private ScreenLockPreferenceDetailsUtils mScreenLockPreferenceDetailsUtils;
    private ScreenLockPreferenceDetailsUtils mScreenLockPreferenceDetailsUtilsSecondary;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(StorageManager.class)).thenReturn(mStorageManager);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        doNothing().when(mContext).startActivity(any());
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});

        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);

        mScreenLockPreferenceDetailsUtils = new ScreenLockPreferenceDetailsUtils(mContext);
        mScreenLockPreferenceDetailsUtilsSecondary = new ScreenLockPreferenceDetailsUtils(mContext,
                Secondary);
    }

    @Test
    public void isAvailable_whenEnabled_shouldReturnTrue() {
        whenConfigShowUnlockSetOrChangeIsEnabled(true);

        assertThat(mScreenLockPreferenceDetailsUtils.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_whenDisabled_shouldReturnFalse() {
        whenConfigShowUnlockSetOrChangeIsEnabled(false);
        assertThat(mScreenLockPreferenceDetailsUtils.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_secondaryWhenDisabled_shouldReturnFalse() {
        whenConfigShowUnlockSetOrChangeIsEnabled(false);
        assertThat(mScreenLockPreferenceDetailsUtilsSecondary.isAvailable(USER_ID)).isFalse();
    }


    @Test
    public void isAvailable_secondaryForUserNotSupport_returnsFalse() {
        whenConfigShowUnlockSetOrChangeIsEnabled(true);
        when(mLockPatternUtils.checkUserSupportsBiometricSecondFactor(USER_ID, false))
                .thenReturn(false);
        assertThat(mScreenLockPreferenceDetailsUtilsSecondary.isAvailable(USER_ID)).isFalse();
    }

    @Test
    public void isAvailable_secondaryWithKeyguardDisabled_returnFalse() {
        whenConfigShowUnlockSetOrChangeIsEnabled(true);
        when(mLockPatternUtils.checkUserSupportsBiometricSecondFactor(USER_ID, false))
                .thenReturn(true);
        when(mLockPatternUtils.isBiometricKeyguardEnabled(USER_ID)).thenReturn(false);

        assertThat(mScreenLockPreferenceDetailsUtilsSecondary.isAvailable(USER_ID)).isFalse();
    }

    @Test
    public void isAvailable_secondaryWithKeyguardEnabled_returnTrue() {
        // Make RestrictedLockUtilsInternal#checkIfKeyguardFeaturesDisabled work.
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(null);
        UserManager um = mock(UserManager.class);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(um);
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.isManagedProfile()).thenReturn(false);
        when(um.getUserInfo(anyInt())).thenReturn(userInfo);

        whenConfigShowUnlockSetOrChangeIsEnabled(true);
        when(mLockPatternUtils.checkUserSupportsBiometricSecondFactor(USER_ID, false))
                .thenReturn(true);
        when(mLockPatternUtils.isBiometricKeyguardEnabled(USER_ID)).thenReturn(true);

        assertThat(mScreenLockPreferenceDetailsUtilsSecondary.isAvailable(USER_ID)).isTrue();
    }


    @Test
    public void getSummary_unsecureAndDisabledPattern_shouldReturnUnlockModeOff() {
        final String summary = prepareString("unlock_set_unlock_mode_off", "unlockModeOff");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(true);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_unsecurePattern_shouldReturnUnlockModeNone() {
        final String summary =
                prepareString("unlock_set_unlock_mode_none", "unlockModeNone");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_secondaryNotSecure_returnsOff() {
        final String summary =
                prepareString("unlock_set_unlock_mode_off", "unlockModeOff");

        when(mLockPatternUtils.isSecure(USER_ID, Secondary)).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt(), eq(Secondary))).thenReturn(true);

        assertThat(mScreenLockPreferenceDetailsUtilsSecondary.getSummary(USER_ID)).isEqualTo(
                summary);
    }

    @Test
    public void getSummary_secondaryPin_returnsPin() {
        final String summary =
                prepareString("unlock_set_unlock_mode_pin", "unlockModePin");

        when(mLockPatternUtils.isSecure(USER_ID, Secondary)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID, Secondary))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);

        assertThat(mScreenLockPreferenceDetailsUtilsSecondary.getSummary(USER_ID)).isEqualTo(
                summary);
    }

    @Test
    public void getSummary_passwordQualitySomething_shouldUnlockModePattern() {
        final String summary =
                prepareString("unlock_set_unlock_mode_pattern", "unlockModePattern");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_passwordQualityNumeric_shouldUnlockModePin() {
        final String summary =
                prepareString("unlock_set_unlock_mode_pin", "unlockModePin");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_passwordQualityNumericComplex_shouldUnlockModePin() {
        final String summary = prepareString("unlock_set_unlock_mode_pin", "unlockModePin");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_passwordQualityAlphabetic_shouldUnlockModePassword() {
        final String summary =
                prepareString("unlock_set_unlock_mode_password", "unlockModePassword");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_passwordQualityAlphanumeric_shouldUnlockModePassword() {
        final String summary =
                prepareString("unlock_set_unlock_mode_password", "unlockModePassword");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_passwordQualityComplex_shouldUnlockModePassword() {
        final String summary =
                prepareString("unlock_set_unlock_mode_password", "unlockModePassword");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_COMPLEX);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }

    @Test
    public void getSummary_passwordQualityManaged_shouldUnlockModePassword() {
        final String summary =
                prepareString("unlock_set_unlock_mode_password", "unlockModePassword");

        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_MANAGED);

        assertThat(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID)).isEqualTo(summary);
    }


    @Test
    public void getSummary_unsupportedPasswordQuality_shouldReturnNull() {
        when(mLockPatternUtils.isSecure(USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        assertNull(mScreenLockPreferenceDetailsUtils.getSummary(USER_ID));
    }

    @Test
    public void isPasswordQualityManaged_withoutAdmin_shouldReturnFalse() {
        final RestrictedLockUtils.EnforcedAdmin admin = null;

        assertThat(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(USER_ID, admin))
                .isFalse();
    }

    @Test
    public void isPasswordQualityManaged_passwordQualityIsManaged_shouldReturnTrue() {
        final RestrictedLockUtils.EnforcedAdmin admin = new RestrictedLockUtils.EnforcedAdmin();

        when(mDevicePolicyManager.getPasswordQuality(admin.component, USER_ID, Primary))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_MANAGED);

        assertThat(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(USER_ID, admin))
                .isTrue();
    }

    @Test
    public void isPasswordQualityManaged_secondaryPasswordQualityNotManaged_returnFalse() {
        final RestrictedLockUtils.EnforcedAdmin admin = new RestrictedLockUtils.EnforcedAdmin();

        when(mDevicePolicyManager.getPasswordQuality(admin.component, USER_ID, Secondary))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);

        assertThat(mScreenLockPreferenceDetailsUtilsSecondary.isPasswordQualityManaged(USER_ID,
                admin)).isFalse();
    }

    @Test
    public void isPasswordQualityManaged_passwordQualityIsNotManaged_shouldReturnFalse() {
        final RestrictedLockUtils.EnforcedAdmin admin = new RestrictedLockUtils.EnforcedAdmin();

        when(mDevicePolicyManager.getPasswordQuality(admin.component, USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        assertThat(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(USER_ID, admin))
                .isFalse();
    }

    @Test
    public void isLockPatternSecure_patternIsSecure_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        assertThat(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).isTrue();
    }

    @Test
    public void isLockPatternSecure_patternIsNotSecure_shouldReturnFalse() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);

        assertThat(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).isFalse();
    }

    @Test
    public void shouldShowGearMenu_patternIsSecure_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        assertThat(mScreenLockPreferenceDetailsUtils.shouldShowGearMenu()).isTrue();
    }

    @Test
    public void shouldShowGearMenu_patternIsNotSecure_shouldReturnFalse() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);

        assertThat(mScreenLockPreferenceDetailsUtils.shouldShowGearMenu()).isFalse();
    }

    @Test
    public void openScreenLockSettings_shouldSendIntent() {
        mScreenLockPreferenceDetailsUtils.openScreenLockSettings(SOURCE_METRICS_CATEGORY, null, 0);

        assertFragmentLaunchRequested(ScreenLockSettings.class.getName());
    }

    @Test
    public void openScreenLockSettings_secondary_sendsIntent() {
        SettingsPreferenceFragment resultListener = mock(SettingsPreferenceFragment.class);
        int requestCode = 1000;

        mScreenLockPreferenceDetailsUtilsSecondary.openScreenLockSettings(SOURCE_METRICS_CATEGORY,
                resultListener, requestCode);

        assertFragmentLaunchRequestedForResult(ScreenLockSettings.class.getName(),
                resultListener, requestCode);
    }

    @Test
    public void getLaunchScreenLockSettingsIntent_returnsIntent() {
        final Intent intent = mScreenLockPreferenceDetailsUtils.getLaunchScreenLockSettingsIntent(
                SOURCE_METRICS_CATEGORY);

        assertFragmentLaunchIntent(intent, ScreenLockSettings.class.getName());
    }

    @Test
    public void getLaunchScreenLockSettingsIntent_secondary_returnsIntent() {
        final Intent intent =
                mScreenLockPreferenceDetailsUtilsSecondary.getLaunchScreenLockSettingsIntent(
                        SOURCE_METRICS_CATEGORY);

        assertFragmentLaunchIntent(intent, ScreenLockSettings.class.getName());
        assertThat(intent.getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_LOCK_DOMAIN, LockDomain.class))
                .isEqualTo(Secondary);
        assertThat(intent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_FOREGROUND_ONLY, false))
                .isEqualTo(true);
    }

    @Test
    public void openChooseLockGenericFragment_noQuietMode_shouldSendIntent_shouldReturnTrue() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(false);

        assertThat(mScreenLockPreferenceDetailsUtils
                .openChooseLockGenericFragment(SOURCE_METRICS_CATEGORY, null, null, 0)).isTrue();
        assertFragmentLaunchRequested(ChooseLockGeneric.ChooseLockGenericFragment.class.getName());
    }

    @Test
    public void openChooseLockGenericFragment_secondary_sendsIntent() {
        LockscreenCredential credential = LockscreenCredential.createPassword("password");
        SettingsPreferenceFragment resultListener = mock(SettingsPreferenceFragment.class);
        int requestCode = 1000;

        assertThat(mScreenLockPreferenceDetailsUtilsSecondary
                .openChooseLockGenericFragment(SOURCE_METRICS_CATEGORY, credential, resultListener,
                        requestCode)).isTrue();
        assertFragmentLaunchRequestedForResult(
                ChooseLockGeneric.ChooseLockGenericFragment.class.getName(), resultListener,
                requestCode);
    }

    @Test
    public void getLaunchChooseLockGenericFragmentIntent_noQuietMode_returnsIntent() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(false);

        final Intent intent = mScreenLockPreferenceDetailsUtils
                .getLaunchChooseLockGenericFragmentIntent(SOURCE_METRICS_CATEGORY);

        assertFragmentLaunchIntent(intent,
                ChooseLockGeneric.ChooseLockGenericFragment.class.getName());
    }

    @Test
    public void getChooseLockGenericFragmentIntent_secondary_returnsIntent() {
        LockscreenCredential credential = LockscreenCredential.createPassword("password");
        final Intent intent =
                mScreenLockPreferenceDetailsUtilsSecondary.getChooseLockGenericFragmentIntent(
                        SOURCE_METRICS_CATEGORY, credential);

        assertFragmentLaunchIntent(intent,
                ChooseLockGeneric.ChooseLockGenericFragment.class.getName());
        assertThat(intent.getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_LOCK_DOMAIN, LockDomain.class))
                .isEqualTo(Secondary);
        Bundle args = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertNotNull(args);
        LockscreenCredential credentialActual = args.getParcelable(
                ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, LockscreenCredential.class);
        assertEquals(credential, credentialActual);
    }

    private void whenConfigShowUnlockSetOrChangeIsEnabled(boolean enabled) {
        final int resId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool",
                "config_show_unlock_set_or_change");
        when(mResources.getBoolean(resId)).thenReturn(enabled);
    }

    private String prepareString(String stringResName, String string) {
        final int stringResId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "string", stringResName);
        when(mResources.getString(stringResId)).thenReturn(string);
        return string;
    }

    private void assertFragmentLaunchRequested(String fragmentClassName) {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertFragmentLaunchIntent(intent, fragmentClassName);
    }

    private void assertFragmentLaunchRequestedForResult(String fragmentClassName,
            SettingsPreferenceFragment resultListener, int requestCode) {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(resultListener).startActivityForResult(intentCaptor.capture(), eq(requestCode));

        Intent intent = intentCaptor.getValue();
        assertFragmentLaunchIntent(intent, fragmentClassName);
    }

    private void assertFragmentLaunchIntent(Intent intent, String fragmentClassName) {
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(fragmentClassName);
        assertThat(intent.getIntExtra(
                MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1 /* defaultValue */))
                .isEqualTo(SOURCE_METRICS_CATEGORY);
    }
}
