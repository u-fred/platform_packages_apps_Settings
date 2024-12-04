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

    private final WrappedLockPatternUtils lockPatternUtils;

    static final String PREF_KEY = "scramble_pin_layout";

    public PinScramblingPrefController(Context ctx, WrappedLockPatternUtils wlpu) {
        super(ctx, PREF_KEY, wlpu.getLockDomain() == Primary ?
                ExtSettings.SCRAMBLE_LOCKSCREEN_PIN_LAYOUT_PRIMARY :
                ExtSettings.SCRAMBLE_LOCKSCREEN_PIN_LAYOUT_SECONDARY);

        lockPatternUtils = wlpu;
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
