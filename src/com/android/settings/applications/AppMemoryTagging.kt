package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.ExtSettings
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswUseMemoryTagging
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import com.android.internal.os.Zygote
import com.android.settings.R
import com.android.settings.ext.AppPrefUtils
import com.android.settings.ext.BoolSettingFragment
import com.android.settings.ext.BoolSettingFragmentPrefController
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterUseMemoryTagging : AswAdapter<AswUseMemoryTagging>() {

    override fun getAppSwitch() = AswUseMemoryTagging.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.aep_memtag)

    override fun getNotificationToggleTitle(ctx: Context) = ctx.getText(R.string.aep_memtag_notif_toggle_time)

    override fun getDetailFragmentClass() = AppMemtagFragment::class
}

private val isMemoryTaggingSupported = Zygote.nativeSupportsMemoryTagging()

@Composable
fun AppMemtagPreference(app: ApplicationInfo) {
    if (!isMemoryTaggingSupported) {
        return
    }

    val context = LocalContext.current
    AswPreference(context, app, AswAdapterUseMemoryTagging)
}

class AppMemtagFragment : AswExploitProtectionFragment<AswUseMemoryTagging>() {

    override fun getAswAdapter() = AswAdapterUseMemoryTagging

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.aep_memtag_dvr_is_system_app
            AppSwitch.IR_NO_NATIVE_CODE -> R.string.aep_memtag_dvr_no_native_code
            AppSwitch.IR_OPTED_IN_VIA_MANIFEST -> R.string.aep_memtag_dvr_manifest_opt_in
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_memtag_footer)
    }
}

class AppDefaultMemtagPrefController(ctx: Context, key: String) :
    BoolSettingFragmentPrefController(ctx, key, ExtSettings.FORCE_APP_MEMTAG_BY_DEFAULT) {

    private val isSupported = Zygote.nativeSupportsMemoryTagging()

    override fun getAvailabilityStatus(): Int {
        if (!isSupported) {
            return UNSUPPORTED_ON_DEVICE
        }

        return super.getAvailabilityStatus()
    }

    override fun getSummaryOn() = resText(R.string.aep_default_memtag_summary_on)
    override fun getSummaryOff() = resText(R.string.aep_default_memtag_summary_off)
}

class AppDefaultMemtagFragment : BoolSettingFragment() {

    override fun getSetting() = ExtSettings.FORCE_APP_MEMTAG_BY_DEFAULT

    override fun getTitle() = resText(R.string.aep_memtag)

    override fun getMainSwitchTitle() = resText(R.string.aep_default_memtag_main_switch_title)
    override fun getMainSwitchSummary() = resText(R.string.aep_default_memtag_main_switch_summary)

    override fun addExtraPrefs(screen: PreferenceScreen) {
        AswAdapterUseMemoryTagging.addAppListPageLink(screen)
    }

    override fun makeFooterPref(builder: FooterPreference.Builder): FooterPreference {
        val text = AppPrefUtils.getFooterForDefaultHardeningSetting(requireContext(),
                R.string.aep_default_memtag_footer)
        return builder.setTitle(text).build()
    }
}

class MemtagAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterUseMemoryTagging) {

    private val isSupported = Zygote.nativeSupportsMemoryTagging()

    override fun getAvailabilityStatus(): Int {
        if (!isSupported) {
            return UNSUPPORTED_ON_DEVICE
        }
        return ExtSettingControllerHelper
            .getSecondaryUserOnlySettingAvailability(mContext)
    }
}
