package com.android.settings.security.screenlock;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ext.BoolSettingPrefController;
import com.android.settings.overlay.FeatureFactory;

public class PinScramblingPrefController extends BoolSettingPrefController {

    private final LockPatternUtils lockPatternUtils;

    public PinScramblingPrefController(Context ctx, String key) {
        super(ctx, key, ExtSettings.SCRAMBLE_LOCKSCREEN_PIN_LAYOUT);

        lockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(ctx);
    }

    @Override
    public int getAvailabilityStatus() {
        int res = super.getAvailabilityStatus();
        if (res == AVAILABLE) {
            if (lockPatternUtils.getCredentialTypeForUser(mContext.getUserId())
                    != LockPatternUtils.CREDENTIAL_TYPE_PIN) {
                return CONDITIONALLY_UNAVAILABLE;
            }
        }
        return res;
    }
}
