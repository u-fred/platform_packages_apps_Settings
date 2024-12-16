package com.android.settings.security.screenlock;

import static com.android.internal.widget.LockDomain.Primary;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.ext.BoolSettingPrefController;
import com.android.settings.overlay.FeatureFactory;

public class PinScramblingPrefController extends BoolSettingPrefController {

    private final LockPatternUtils mLockPatternUtils;
    private final LockDomain mLockDomain;

    static final String PREF_KEY = "scramble_pin_layout";

    public PinScramblingPrefController(Context ctx, LockDomain lockDomain) {
        super(ctx, PREF_KEY, lockDomain == Primary ?
                ExtSettings.SCRAMBLE_LOCKSCREEN_PIN_LAYOUT_PRIMARY :
                ExtSettings.SCRAMBLE_LOCKSCREEN_PIN_LAYOUT_SECONDARY);
        mLockDomain = lockDomain;
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(ctx);
    }

    @Override
    public int getAvailabilityStatus() {
        int res = super.getAvailabilityStatus();
        if (res == AVAILABLE) {
            if (mLockPatternUtils.getCredentialTypeForUser(mContext.getUserId(), mLockDomain)
                    != LockPatternUtils.CREDENTIAL_TYPE_PIN) {
                return CONDITIONALLY_UNAVAILABLE;
            }
        }
        return res;
    }
}
