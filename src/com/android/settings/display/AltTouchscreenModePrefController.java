package com.android.settings.display;

import android.app.AlertDialog;
import android.content.Context;
import android.ext.settings.AltTouchscreenMode;
import android.os.Handler;
import android.os.PowerManager;

import com.android.settings.ext.BoolSettingPrefController;
import com.android.settings.R;

public class AltTouchscreenModePrefController extends BoolSettingPrefController {

    public AltTouchscreenModePrefController(Context ctx, String key) {
        super(ctx, key, AltTouchscreenMode.getSetting());
    }

    @Override
    public int getAvailabilityStatus() {
        if (!AltTouchscreenMode.isAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return super.getAvailabilityStatus();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        super.setChecked(isChecked);
        if (!isChecked) {
            return true;
        }

        Handler handler = mContext.getMainThreadHandler();
        Runnable undo = () -> {
            super.setChecked(false);
            mContext.getSystemService(PowerManager.class).reboot(null);
        };
        // disable the alt mode after a delay unless the user confirms that the touchscreen is
        // still working
        handler.postDelayed(undo, 10_000);

        var b = new AlertDialog.Builder(mContext);
        b.setMessage(R.string.alt_touchscreen_mode_confirm_message);
        b.setPositiveButton(R.string.alt_touchscreen_mode_confirm_button, (d, btn) -> {
            handler.removeCallbacks(undo);
        });
        b.show();

        return true;
    }
}
