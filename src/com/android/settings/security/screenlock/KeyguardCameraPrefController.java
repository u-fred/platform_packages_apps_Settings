package com.android.settings.security.screenlock;

import static com.android.internal.widget.LockDomain.Secondary;

import android.content.Context;
import android.ext.settings.ExtSettings;

import com.android.internal.widget.LockDomain;
import com.android.settings.ext.BoolSettingPrefController;

public class KeyguardCameraPrefController extends BoolSettingPrefController {

    private final LockDomain mLockDomain;

    static final String PREF_KEY = "allow_keyguard_camera";

    public KeyguardCameraPrefController(Context ctx, LockDomain lockDomain) {
        super(ctx, PREF_KEY, ExtSettings.ALLOW_KEYGUARD_CAMERA);

        mLockDomain = lockDomain;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLockDomain == Secondary) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return super.getAvailabilityStatus();
    }
}
