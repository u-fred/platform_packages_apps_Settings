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

package com.android.settings.testutils.shadow;

import static com.android.internal.widget.LockDomain.Primary;

import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Implements(LockPatternUtils.class)
public class ShadowLockPatternUtils {

    private static boolean sDeviceEncryptionEnabled;
    private static Map<Integer, Integer> sUserToActivePasswordQualityMap = new HashMap<>();
    private static Map<Integer, Integer> sUserToActivePasswordQualityMapSecondary = new HashMap<>();
    private static Map<Integer, Integer> sUserToComplexityMap = new HashMap<>();
    private static Map<Integer, Integer> sUserToComplexityMapSecondary = new HashMap<>();
    private static Map<Integer, Integer> sUserToProfileComplexityMap = new HashMap<>();
    private static Map<Integer, PasswordMetrics> sUserToMetricsMap = new HashMap<>();
    private static Map<Integer, PasswordMetrics> sUserToMetricsMapSecondary = new HashMap<>();
    private static Map<Integer, PasswordMetrics> sUserToProfileMetricsMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToIsSecureMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToIsSecureMapSecondary = new HashMap<>();
    private static Map<Integer, Boolean> sUserToVisiblePatternEnabledMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToBiometricAllowedMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToLockPatternEnabledMap = new HashMap<>();
    private static Map<Integer, Integer> sKeyguardStoredPasswordQualityMap = new HashMap<>();
    private static Map<Integer, Integer> sKeyguardStoredPasswordQualityMapSecondary =
            new HashMap<>();

    private static boolean sIsUserOwnsFrpCredential;

    @Resetter
    public static void reset() {
        sUserToActivePasswordQualityMap.clear();
        sUserToActivePasswordQualityMapSecondary.clear();
        sUserToComplexityMap.clear();
        sUserToComplexityMapSecondary.clear();
        sUserToProfileComplexityMap.clear();
        sUserToMetricsMap.clear();
        sUserToMetricsMapSecondary.clear();
        sUserToProfileMetricsMap.clear();
        sUserToIsSecureMap.clear();
        sUserToIsSecureMapSecondary.clear();
        sUserToVisiblePatternEnabledMap.clear();
        sUserToBiometricAllowedMap.clear();
        sUserToLockPatternEnabledMap.clear();
        sDeviceEncryptionEnabled = false;
        sIsUserOwnsFrpCredential = false;
        sKeyguardStoredPasswordQualityMap.clear();
        sKeyguardStoredPasswordQualityMapSecondary.clear();
    }

    @Implementation
    protected boolean hasSecureLockScreen() {
        return true;
    }

    @Implementation
    protected boolean isSecure(int userId) {
        return isSecure(userId, Primary);
    }

    @Implementation
    protected boolean isSecure(int userId, LockDomain lockDomain) {
        Map<Integer, Boolean> isSecureMap = lockDomain == Primary ? sUserToIsSecureMap :
                sUserToIsSecureMapSecondary;
        Boolean isSecure = isSecureMap.get(userId);
        if (isSecure == null) {
            return true;
        }
        return isSecure;
    }

    public static void setIsSecure(int userId, LockDomain lockDomain, boolean isSecure) {
        Map<Integer, Boolean> isSecureMap = lockDomain == Primary ? sUserToIsSecureMap :
                sUserToIsSecureMapSecondary;
        isSecureMap.put(userId, isSecure);
    }

    @Implementation
    protected int getActivePasswordQuality(int userId) {
        return getActivePasswordQuality(userId, Primary);
    }

    @Implementation
    protected int getActivePasswordQuality(int userId, LockDomain lockDomain) {
        Map<Integer, Integer> activePasswordQualityMap = lockDomain == Primary ?
                sUserToActivePasswordQualityMap : sUserToActivePasswordQualityMapSecondary;
        final Integer activePasswordQuality = activePasswordQualityMap.get(userId);
        if (activePasswordQuality == null) {
            return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
        }
        return activePasswordQuality;
    }

    @Implementation
    protected int getKeyguardStoredPasswordQuality(int userHandle) {
        return getKeyguardStoredPasswordQuality(userHandle, Primary);
    }

    @Implementation
    protected int getKeyguardStoredPasswordQuality(int userHandle, LockDomain lockDomain) {
        Map<Integer, Integer> passwordQualityMap = lockDomain == Primary ?
                sKeyguardStoredPasswordQualityMap : sKeyguardStoredPasswordQualityMapSecondary;
        return passwordQualityMap.getOrDefault(userHandle, /* defaultValue= */ 1);
    }

    @Implementation
    protected static boolean isDeviceEncryptionEnabled() {
        return sDeviceEncryptionEnabled;
    }

    @Implementation
    protected List<ComponentName> getEnabledTrustAgents(int userId) {
        return null;
    }

    public static void setDeviceEncryptionEnabled(boolean deviceEncryptionEnabled) {
        sDeviceEncryptionEnabled = deviceEncryptionEnabled;
    }

    @Implementation
    protected byte[] getPasswordHistoryHashFactor(
            LockscreenCredential currentPassword, int userId) {
        return null;
    }

    @Implementation
    protected boolean checkPasswordHistory(byte[] passwordToCheck, byte[] hashFactor, int userId) {
        return checkPasswordHistory(passwordToCheck, hashFactor, userId, Primary);
    }

    @Implementation
    protected boolean checkPasswordHistory(byte[] passwordToCheck, byte[] hashFactor, int userId,
            LockDomain lockDomain) {
        return false;
    }

    @Implementation
    public @DevicePolicyManager.PasswordComplexity int getRequestedPasswordComplexity(int userId) {
        return getRequestedPasswordComplexity(userId, Primary);
    }

    @Implementation
    public @DevicePolicyManager.PasswordComplexity int getRequestedPasswordComplexity(int userId,
            LockDomain lockDomain) {
        return getRequestedPasswordComplexity(userId, lockDomain, false);
    }

    @Implementation
    public @DevicePolicyManager.PasswordComplexity int getRequestedPasswordComplexity(int userId,
            boolean deviceWideOnly) {
        return getRequestedPasswordComplexity(userId, Primary, false);
    }

    @Implementation
    @DevicePolicyManager.PasswordComplexity
    public int getRequestedPasswordComplexity(int userId, LockDomain lockDomain,
            boolean deviceWideOnly) {
        Map<Integer, Integer> complexityMap = lockDomain == Primary ? sUserToComplexityMap :
                sUserToComplexityMapSecondary;
        int complexity = complexityMap.getOrDefault(userId,
                DevicePolicyManager.PASSWORD_COMPLEXITY_NONE);
        if (lockDomain == Primary && !deviceWideOnly) {
            complexity = Math.max(complexity, sUserToProfileComplexityMap.getOrDefault(userId,
                    DevicePolicyManager.PASSWORD_COMPLEXITY_NONE));
        }
        return complexity;
    }

    @Implementation
    public static boolean userOwnsFrpCredential(Context context, UserInfo info) {
        return sIsUserOwnsFrpCredential;
    }

    public static void setUserOwnsFrpCredential(boolean isUserOwnsFrpCredential) {
        sIsUserOwnsFrpCredential = isUserOwnsFrpCredential;
    }

    @Implementation
    public boolean isVisiblePatternEnabled(int userId) {
        return sUserToVisiblePatternEnabledMap.getOrDefault(userId, false);
    }

    public static void setIsVisiblePatternEnabled(int userId, boolean isVisiblePatternEnabled) {
        sUserToVisiblePatternEnabledMap.put(userId, isVisiblePatternEnabled);
    }

    @Implementation
    public boolean isBiometricAllowedForUser(int userId) {
        return sUserToBiometricAllowedMap.getOrDefault(userId, false);
    }

    public static void setIsBiometricAllowedForUser(int userId, boolean isBiometricAllowed) {
        sUserToBiometricAllowedMap.put(userId, isBiometricAllowed);
    }

    @Implementation
    public boolean isLockPatternEnabled(int userId) {
        return sUserToLockPatternEnabledMap.getOrDefault(userId, false);
    }

    public static void setIsLockPatternEnabled(int userId, boolean isLockPatternEnabled) {
        sUserToLockPatternEnabledMap.put(userId, isLockPatternEnabled);
    }

    @Implementation
    public boolean setLockCredential(
            @NonNull LockscreenCredential newCredential,
            @NonNull LockscreenCredential savedCredential, int userHandle) {
        return setLockCredential(newCredential, savedCredential, Primary, userHandle);
    }

    @Implementation
    public boolean setLockCredential(
            @NonNull LockscreenCredential newCredential,
            @NonNull LockscreenCredential savedCredential, LockDomain lockDomain, int userHandle) {
        setIsSecure(userHandle, Primary, true);
        return true;
    }

    @Implementation
    public boolean checkCredential(
            @NonNull LockscreenCredential credential, int userId,
            @Nullable LockPatternUtils.CheckCredentialProgressCallback progressCallback)
            throws LockPatternUtils.RequestThrottledException {
        return checkCredential(credential, Primary, userId, progressCallback);
    }

    @Implementation
    public boolean checkCredential(
            @NonNull LockscreenCredential credential, LockDomain lockDomain, int userId,
            @Nullable LockPatternUtils.CheckCredentialProgressCallback progressCallback)
            throws LockPatternUtils.RequestThrottledException {
        return true;
    }

    public static void setRequiredPasswordComplexity(int userHandle, LockDomain lockDomain,
            int complexity) {
        Map<Integer, Integer> complexityMap = lockDomain == Primary ? sUserToComplexityMap :
                sUserToComplexityMapSecondary;
        complexityMap.put(userHandle, complexity);
    }

    public static void setRequiredPasswordComplexity(int complexity, LockDomain lockDomain) {
        setRequiredPasswordComplexity(UserHandle.myUserId(), lockDomain, complexity);
    }

    public static void setRequiredPasswordComplexity(int complexity) {
        setRequiredPasswordComplexity(complexity, Primary);
    }

    public static void setRequiredProfilePasswordComplexity(int complexity) {
        sUserToProfileComplexityMap.put(UserHandle.myUserId(), complexity);
    }

    @Implementation
    public PasswordMetrics getRequestedPasswordMetrics(int userId) {
        return getRequestedPasswordMetrics(userId, false);
    }

    @Implementation
    public PasswordMetrics getRequestedPasswordMetrics(int userId, LockDomain lockDomain) {
        return getRequestedPasswordMetrics(userId, lockDomain, false);
    }

    @Implementation
    public PasswordMetrics getRequestedPasswordMetrics(int userId, boolean deviceWideOnly) {
        return getRequestedPasswordMetrics(userId, Primary, deviceWideOnly);
    }

    @Implementation
    public PasswordMetrics getRequestedPasswordMetrics(int userId, LockDomain lockDomain,
            boolean deviceWideOnly) {
        Map<Integer, PasswordMetrics> metricsMap = lockDomain == Primary ? sUserToMetricsMap :
                sUserToMetricsMapSecondary;
        PasswordMetrics metrics = metricsMap.getOrDefault(userId,
                new PasswordMetrics(LockPatternUtils.CREDENTIAL_TYPE_NONE));
        if (lockDomain == Primary && !deviceWideOnly) {
            metrics.maxWith(sUserToProfileMetricsMap.getOrDefault(userId,
                    new PasswordMetrics(LockPatternUtils.CREDENTIAL_TYPE_NONE)));
        }
        return metrics;
    }

    public static void setRequestedPasswordMetrics(PasswordMetrics metrics) {
        setRequestedPasswordMetrics(metrics, Primary);
    }

    public static void setRequestedPasswordMetrics(PasswordMetrics metrics, LockDomain lockDomain) {
        Map<Integer, PasswordMetrics> metricsMap = lockDomain == Primary ? sUserToMetricsMap :
                sUserToMetricsMapSecondary;
        metricsMap.put(UserHandle.myUserId(), metrics);
    }

    public static void setRequestedProfilePasswordMetrics(PasswordMetrics metrics) {
        sUserToProfileMetricsMap.put(UserHandle.myUserId(), metrics);
    }

    public static void setActivePasswordQuality(int quality, boolean primary) {
        Map<Integer, Integer> activePasswordQualityMap = primary ? sUserToActivePasswordQualityMap :
                sUserToActivePasswordQualityMapSecondary;
        sUserToActivePasswordQualityMap.put(UserHandle.myUserId(), quality);
    }

    @Implementation
    public boolean isLockScreenDisabled(int userId) {
        return isLockScreenDisabled(userId, Primary);
    }

    @Implementation
    public boolean isLockScreenDisabled(int userId, LockDomain lockDomain) {
        return false;
    }

    @Implementation
    public boolean isSeparateProfileChallengeEnabled(int userHandle) {
        return false;
    }

    public static void setKeyguardStoredPasswordQuality(int quality, boolean primary) {
        Map<Integer, Integer> passwordQualityMap = primary ? sKeyguardStoredPasswordQualityMap :
                sKeyguardStoredPasswordQualityMapSecondary;
        sKeyguardStoredPasswordQualityMap.put(UserHandle.myUserId(), quality);
    }
}
