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
 * limitations under the License
 */

package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_LOW;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_MEDIUM;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_MANAGED;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static com.android.internal.widget.LockDomain.Primary;
import static com.android.internal.widget.LockDomain.Secondary;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordPolicy;
import android.os.UserHandle;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, SettingsShadowResources.class})
public class ChooseLockGenericControllerTest {

    private ChooseLockGenericController mController;
    private ChooseLockGenericController mControllerSecondary;

    @Mock
    private ManagedLockPasswordProvider mManagedLockPasswordProvider;

    @Mock
    private WrappedLockPatternUtils mLockPatternUtils;
    @Mock
    private WrappedLockPatternUtils mLockPatternUtilsSecondary;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mLockPatternUtils.hasSecureLockScreen()).thenReturn(true);
        when(mLockPatternUtilsSecondary.hasSecureLockScreen()).thenReturn(true);

        setDevicePolicyPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        when(mLockPatternUtils.getLockDomain()).thenReturn(Primary);
        mController = createBuilder().build();

        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED, Secondary);
        when(mLockPatternUtilsSecondary.getLockDomain()).thenReturn(Secondary);
        when(mLockPatternUtilsSecondary.checkUserSupportsBiometricSecondFactor(anyInt(), eq(true)))
                .thenReturn(true);
        mControllerSecondary = createBuilder(Secondary).build();
        when(mLockPatternUtilsSecondary.getRequestedPasswordComplexity(anyInt(), anyBoolean()))
                .thenReturn(PASSWORD_COMPLEXITY_NONE);

        SettingsShadowResources.overrideResource(R.bool.config_hide_none_security_option, false);
        SettingsShadowResources.overrideResource(R.bool.config_hide_swipe_security_option, false);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void constructor_SecondaryForUserNotSupportingSecondary_ThrowsException() {
        final int userId = 10;

        clearInvocations(mLockPatternUtilsSecondary);

        when(mLockPatternUtilsSecondary.checkUserSupportsBiometricSecondFactor(eq(userId),
                eq(true))).thenThrow(IllegalArgumentException.class);

        ChooseLockGenericController.Builder builder = new ChooseLockGenericController.Builder(
                application, userId, null, mLockPatternUtilsSecondary);

        assertThrows(IllegalArgumentException.class, builder::build);
        // Verify that exception was as a result of this call.
        verify(mLockPatternUtilsSecondary).checkUserSupportsBiometricSecondFactor(eq(userId),
                eq(true));
    }

    @Test
    public void constructor_SecondaryWithUnifiedProfile_ThrowsException() {
        clearInvocations(mLockPatternUtilsSecondary);

        ChooseLockGenericController.Builder builder = new ChooseLockGenericController.Builder(
                application, 0, null, mLockPatternUtilsSecondary);
        builder.setProfileToUnify(1);

        assertThrows("unificationProfileId must be USER_NULL when lockDomain is Secondary",
                IllegalArgumentException.class, builder::build);
    }

    @Test
    public void constructor_SecondaryWithManagedPasswordProvider_ThrowsException() {
        clearInvocations(mLockPatternUtilsSecondary);

        ChooseLockGenericController.Builder builder = new ChooseLockGenericController.Builder(
                application, 0, mManagedLockPasswordProvider, mLockPatternUtilsSecondary);

        assertThrows("managedPasswordProvider must be null when lockDomain is Secondary",
                IllegalArgumentException.class, builder::build);
    }

    @Test
    public void isScreenLockVisible_shouldRespectResourceConfig() {
        for (ScreenLockType lock : ScreenLockType.values()) {
            // All locks except managed and pattern defaults to visible
            assertWithMessage(lock + " visible").that(mController.isScreenLockVisible(lock))
                    .isEqualTo(lock != ScreenLockType.MANAGED && lock != ScreenLockType.PATTERN);
        }

        SettingsShadowResources.overrideResource(R.bool.config_hide_none_security_option, true);
        SettingsShadowResources.overrideResource(R.bool.config_hide_swipe_security_option, true);
        assertWithMessage("NONE visible").that(mController.isScreenLockVisible(ScreenLockType.NONE))
                .isFalse();
        assertWithMessage("SWIPE visible").that(
                mController.isScreenLockVisible(ScreenLockType.SWIPE)).isFalse();
    }

    @Test
    public void isScreenLockVisible_ManagedProfile_shouldHideInsecure() {
        ShadowUserManager.getShadow().setManagedProfiles(Set.of(0));
        assertWithMessage("SWIPE visible").that(
                mController.isScreenLockVisible(ScreenLockType.SWIPE)).isFalse();
        assertWithMessage("NONE visible").that(mController.isScreenLockVisible(ScreenLockType.NONE))
                .isFalse();
    }

    @Test
    public void isScreenLockVisible_managedPasswordChoosable_shouldShowManaged() {
        doReturn(true).when(mManagedLockPasswordProvider).isManagedPasswordChoosable();

        assertWithMessage("MANAGED visible").that(
                mController.isScreenLockVisible(ScreenLockType.MANAGED)).isTrue();
    }

    @Test
    public void isScreenLockVisible_NoneSecondary_ReturnsTrue() {
        assertThat(mControllerSecondary.isScreenLockVisible(ScreenLockType.NONE)).isTrue();
    }

    @Test
    public void isScreenLockVisible_NoneSecondaryWithResourceConfig_ReturnsFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_hide_none_security_option, true);
        assertThat(mControllerSecondary.isScreenLockVisible(ScreenLockType.NONE)).isFalse();
    }

    @Test
    public void isScreenLockVisible_SwipeSecondary_ReturnsFalse() {
        assertThat(mControllerSecondary.isScreenLockVisible(ScreenLockType.SWIPE)).isFalse();
    }

    @Test
    public void isScreenLockVisible_SwipeSecondaryWithResourceConfig_ReturnsFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_hide_swipe_security_option, true);
        assertThat(mControllerSecondary.isScreenLockVisible(ScreenLockType.SWIPE)).isFalse();
    }

    @Test
    public void isScreenLockVisible_PatternSecondary_ReturnsFalse() {
        assertThat(mControllerSecondary.isScreenLockVisible(ScreenLockType.PATTERN)).isFalse();
    }

    @Test
    public void isScreenLockVisible_PINSecondary_ReturnsTrue() {
        assertThat(mControllerSecondary.isScreenLockVisible(ScreenLockType.PIN)).isTrue();
    }

    @Test
    public void isScreenLockVisible_PasswordSecondary_ReturnsFalse() {
        assertThat(mControllerSecondary.isScreenLockVisible(ScreenLockType.PASSWORD)).isFalse();
    }

    @Test
    public void isScreenLockEnabled_SwipeSecondary_ReturnsTrue() {
        assertThat(mControllerSecondary.isScreenLockEnabled(ScreenLockType.SWIPE)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_PatternSecondary_ReturnsTrue() {
        assertThat(mControllerSecondary.isScreenLockEnabled(ScreenLockType.PATTERN)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_PinSecondary_ReturnsTrue() {
        assertThat(mControllerSecondary.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_PasswordSecondary_ReturnsTrue() {
        assertThat(mControllerSecondary.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockRestrictedByAdmin_Secondary_ReturnsFalse() {
        assertThat(mControllerSecondary.isScreenLockRestrictedByAdmin()).isFalse();
    }

    @Test
    public void isScreenLockEnabled_Default() {
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_QualityUnspecified() {
        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_QualitySomething() {
        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_SOMETHING);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_QualityNumeric() {
        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_NUMERIC);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_QualityNumericComplex() {
        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_NUMERIC_COMPLEX);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_QualityAlphabetic() {
        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_ALPHABETIC);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_QualityComplex() {
        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_COMPLEX);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_QualityManaged() {
        setDevicePolicyPasswordQuality(PASSWORD_QUALITY_MANAGED);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isFalse();
    }

    @Test
    public void isScreenLockEnabled_NoneComplexity() {
        when(mLockPatternUtils.getRequestedPasswordComplexity(anyInt(), anyBoolean()))
                .thenReturn(PASSWORD_COMPLEXITY_NONE);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_lowComplexity() {
        when(mLockPatternUtils.getRequestedPasswordComplexity(anyInt(), anyBoolean()))
                .thenReturn(PASSWORD_COMPLEXITY_LOW);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_mediumComplexity() {
        when(mLockPatternUtils.getRequestedPasswordComplexity(anyInt(), anyBoolean()))
                .thenReturn(PASSWORD_COMPLEXITY_MEDIUM);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void isScreenLockEnabled_highComplexity() {
        when(mLockPatternUtils.getRequestedPasswordComplexity(anyInt(), anyBoolean()))
                .thenReturn(PASSWORD_COMPLEXITY_HIGH);
        assertThat(mController.isScreenLockEnabled(ScreenLockType.NONE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.SWIPE)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PATTERN)).isFalse();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PIN)).isTrue();
        assertThat(mController.isScreenLockEnabled(ScreenLockType.PASSWORD)).isTrue();
    }

    @Test
    public void getTitle_shouldContainEnumName() {
        doReturn("MANAGED").when(mManagedLockPasswordProvider).getPickerOptionTitle(anyBoolean());
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(mController.getTitle(lock).toString())
                    .containsMatch(Pattern.compile(lock.toString(), Pattern.CASE_INSENSITIVE));
        }
    }

    @Test
    public void getVisibleScreenLockTypes_qualitySomething_shouldReturnPatterPinPassword() {
        mController = createBuilder().setHideInsecureScreenLockTypes(true).build();
        assertThat(mController.getVisibleAndEnabledScreenLockTypes())
                .isEqualTo(Arrays.asList(
                        ScreenLockType.PIN,
                        ScreenLockType.PASSWORD));
    }

    @Test
    public void getVisibleScreenLockTypes_showDisabled_shouldReturnAllButManaged() {
        assertThat(mController.getVisibleAndEnabledScreenLockTypes())
                .isEqualTo(Arrays.asList(
                        ScreenLockType.NONE,
                        ScreenLockType.SWIPE,
                        ScreenLockType.PIN,
                        ScreenLockType.PASSWORD));
    }

    @Test
    public void upgradeQuality_noDpmRequirement_shouldReturnQuality() {
        setDevicePolicyPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        final int upgradedQuality =
            mController.upgradeQuality(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
        assertWithMessage("upgradedQuality").that(upgradedQuality)
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
    }

    @Test
    public void upgradeQuality_dpmRequirement_shouldReturnRequiredQuality() {
        setDevicePolicyPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);

        final int upgradedQuality =
            mController.upgradeQuality(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        assertWithMessage("upgradedQuality").that(upgradedQuality)
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
    }

    @Test
    public void upgradeQuality_complexityHigh_minQualityNumericComplex() {
        mController = createBuilder().setAppRequestedMinComplexity(PASSWORD_COMPLEXITY_HIGH)
                .build();
        setDevicePolicyPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        assertThat(mController.upgradeQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED))
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX);
    }

    @Test
    public void upgradeQuality_complexityMedium_minQualityNumericComplex() {
        mController = createBuilder().setAppRequestedMinComplexity(PASSWORD_COMPLEXITY_MEDIUM)
                .build();
        setDevicePolicyPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        assertThat(mController.upgradeQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED))
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX);
    }

    @Test
    public void upgradeQuality_complexityLow_minQualitySomething() {
        mController = createBuilder().setAppRequestedMinComplexity(PASSWORD_COMPLEXITY_LOW)
                .build();
        setDevicePolicyPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        assertThat(mController.upgradeQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED))
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
    }

    @Test
    public void getAggregatedPasswordComplexity_AppRequest() {
        mController = createBuilder().setAppRequestedMinComplexity(PASSWORD_COMPLEXITY_HIGH)
                .build();
        assertThat(mController.getAggregatedPasswordComplexity())
                .isEqualTo(PASSWORD_COMPLEXITY_HIGH);
    }

    @Test
    public void getAggregatedPasswordComplexity_DevicePolicy() {
        mController = createBuilder().setAppRequestedMinComplexity(PASSWORD_COMPLEXITY_LOW)
                .build();
        when(mLockPatternUtils.getRequestedPasswordComplexity(eq(UserHandle.myUserId()), eq(false)))
                .thenReturn(PASSWORD_COMPLEXITY_MEDIUM);

        assertThat(mController.getAggregatedPasswordComplexity())
                .isEqualTo(PASSWORD_COMPLEXITY_MEDIUM);
    }

    @Test
    public void getAggregatedPasswordComplexity_ProfileUnification() {
        mController = createBuilder()
                .setProfileToUnify(123)
                .setAppRequestedMinComplexity(PASSWORD_COMPLEXITY_LOW)
                .build();
        when(mLockPatternUtils.getRequestedPasswordComplexity(eq(UserHandle.myUserId()), eq(false)))
                .thenReturn(PASSWORD_COMPLEXITY_MEDIUM);
        when(mLockPatternUtils.getRequestedPasswordComplexity(eq(123)))
                .thenReturn(PASSWORD_COMPLEXITY_HIGH);

        assertThat(mController.getAggregatedPasswordComplexity())
                .isEqualTo(PASSWORD_COMPLEXITY_HIGH);
    }

    private void setDevicePolicyPasswordQuality(int quality) {
        setDevicePolicyPasswordQuality(quality, Primary);
    }

    private void setDevicePolicyPasswordQuality(int quality, LockDomain lockDomain) {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = quality;

        WrappedLockPatternUtils lpu = lockDomain == Primary ? mLockPatternUtils :
                mLockPatternUtilsSecondary;

        when(lpu.getRequestedPasswordMetrics(anyInt(), anyBoolean()))
                .thenReturn(policy.getMinMetrics());

        when(lpu.isCredentialsDisabledForUser(anyInt()))
                .thenReturn(quality == PASSWORD_QUALITY_MANAGED);
    }

    private ChooseLockGenericController.Builder createBuilder() {
        return createBuilder(Primary);
    }

    private ChooseLockGenericController.Builder createBuilder(LockDomain lockDomain) {
        return new ChooseLockGenericController.Builder(
                application,
                0 /* userId */,
                lockDomain == Primary ? mManagedLockPasswordProvider : null,
                lockDomain == Primary ? mLockPatternUtils : mLockPatternUtilsSecondary);
    }
}
