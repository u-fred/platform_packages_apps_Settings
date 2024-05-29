package com.android.settings.security;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.ext.ExtSettingControllerHelper;

public class DuressPasswordPrefController extends BasePreferenceController {

    public DuressPasswordPrefController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return ExtSettingControllerHelper.getGlobalSettingAvailability(mContext);
    }

    // dynamic preference summary is intentionally skipped to avoid revealing whether duress
    // password is active

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            Context ctx = mContext;
            ctx.startActivity(new Intent(ctx, DuressPasswordMainActivity.class));
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }
}
