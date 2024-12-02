package com.android.settings.biometrics.fingerprint;

import static com.android.internal.widget.LockDomain.Secondary;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseBiometricSecondFactorPin;
import com.android.settings.security.screenlock.BiometricSecondFactorPinSettings;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

/**
 * Controller for the biometric second factor PIN preference. If the user does not have a second
 * factor PIN set, launches an Activity for creating one. Otherwise, launches the settings for the
 * second factor PIN.
 */
public class BiometricSecondFactorPinPreferenceController extends BasePreferenceController {

    private final WrappedLockPatternUtils mLockPatternUtils;

    protected final Fragment mHost;

    private int mUserId = -1;
    private LockscreenCredential mPrimaryCredential;

    private int mSettingsRequestCode;
    private int mChooseRequestCode;

    public BiometricSecondFactorPinPreferenceController(Context context, Fragment host, String key) {
        super(context, key);

        mHost = host;
        LockPatternUtils lockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mLockPatternUtils = new WrappedLockPatternUtils(lockPatternUtils, Secondary);
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }
    public void setPrimaryCredential(LockscreenCredential primaryCredential) {
        mPrimaryCredential = primaryCredential;
    }

    public void setSettingsRequestCode(int value) {
        mSettingsRequestCode = value;
    }

    public void setChooseRequestCode(int value) {
        mChooseRequestCode = value;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT;
    }

    @Override
    public CharSequence getSummary() {
        if (mLockPatternUtils.isSecure(mUserId)) {
            return mContext.getResources().getString(
                    R.string.security_settings_biometric_second_factor_pin_empty_summary);
        }
        return mContext.getResources().getString(
                R.string.security_settings_biometric_second_factor_pin_setup_summary);

    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        if (mLockPatternUtils.isSecure(mUserId)) {
            launchSettings();
        } else {
            launchChoose();
        }

        return true;
    }

    public void launchSettings() {
        mHost.startActivityForResult(getSettingsIntent(), mSettingsRequestCode);
    }

    private Intent getSettingsIntent() {
        BiometricSecondFactorPinSettings.IntentBuilder builder =
                new BiometricSecondFactorPinSettings.IntentBuilder(mContext)
                        .setSourceMetricsCateogry(getMetricsCategory())
                        .setUserId(mUserId)
                        .setPrimaryCredential(mPrimaryCredential);

        return builder.build();
    }

    private void launchChoose() {
        mHost.startActivityForResult(getChooseIntent(), mChooseRequestCode);
    }

    private Intent getChooseIntent() {
        ChooseBiometricSecondFactorPin.IntentBuilder builder =
                new ChooseBiometricSecondFactorPin.IntentBuilder(mContext)
                        .setUserId(mUserId)
                        .setPrimaryCredential(mPrimaryCredential);

        return builder.build();
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLockPatternUtils.checkUserSupportsBiometricSecondFactor(mUserId, false)
                && mLockPatternUtils.isBiometricKeyguardEnabled(mUserId)
                && getRestrictingAdmin() == null) {
            return AVAILABLE;
        }
        return DISABLED_FOR_USER;
    }

    protected RestrictedLockUtils.EnforcedAdmin getRestrictingAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                mContext, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId);
    }
}
