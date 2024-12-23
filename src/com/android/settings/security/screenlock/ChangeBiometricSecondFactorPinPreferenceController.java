package com.android.settings.security.screenlock;

import static com.android.internal.widget.LockDomain.Secondary;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseBiometricSecondFactorPin;

/**
 * Controller for launching an Activity to allow the user to choose a new biometric second factor
 * PIN.
 */
public class ChangeBiometricSecondFactorPinPreferenceController extends BasePreferenceController {

    public static final String PREF_KEY = "change_pin";

    private final Fragment mHost;

    private final LockPatternUtils mLockPatternUtils;

    // The result that mHost's Activity will return to indicate that the user requested a change of
    // PIN.
    private final int mChangePinRequestCode;

    private final int mUserId;
    private final LockscreenCredential mPrimaryCredential;

    public ChangeBiometricSecondFactorPinPreferenceController(Context context, Fragment host,
            int userId, int changePinRequestCode, LockscreenCredential primaryCredential) {
        super(context, PREF_KEY);
        mHost = host;
        mUserId = userId;
        mChangePinRequestCode = changePinRequestCode;
        mPrimaryCredential = primaryCredential;
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLockPatternUtils.isSecure(mUserId, Secondary)) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    public void launchChoose() {
        mHost.startActivityForResult(getChooseIntent(), mChangePinRequestCode);
    }

    private Intent getChooseIntent() {
        ChooseBiometricSecondFactorPin.IntentBuilder builder =
                new ChooseBiometricSecondFactorPin.IntentBuilder(mContext)
                        .setUserId(mUserId)
                        .setPrimaryCredential(mPrimaryCredential);

        return builder.build();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        launchChoose();

        return true;
    }
}
