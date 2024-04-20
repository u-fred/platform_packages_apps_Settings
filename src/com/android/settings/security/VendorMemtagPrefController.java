package com.android.settings.security;

import android.app.AlertDialog;
import android.content.Context;
import android.ext.settings.BoolSysProperty;
import android.os.PowerManager;

import com.android.internal.os.Zygote;
import com.android.settings.R;
import com.android.settings.ext.BoolSettingPrefController;

import static java.util.Objects.requireNonNull;

public class VendorMemtagPrefController extends BoolSettingPrefController {

    public VendorMemtagPrefController(Context ctx, String key) {
        super(ctx, key,
                // this sysprop is checked in bionic during process init
                new BoolSysProperty("persist.arm64.memtag.vendor", false));
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Zygote.nativeSupportsMemoryTagging()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return super.getAvailabilityStatus();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        var b = new AlertDialog.Builder(mContext);
        b.setMessage(R.string.memtag_in_vendor_processes_dialog_message);
        b.setPositiveButton(R.string.memtag_in_vendor_processes_dialog_btn, (d, btn) -> {
            super.setChecked(isChecked);
            var powerManager = requireNonNull(mContext.getSystemService(PowerManager.class));
            powerManager.reboot(null);
        });
        b.show();

        return false;
    }
}
