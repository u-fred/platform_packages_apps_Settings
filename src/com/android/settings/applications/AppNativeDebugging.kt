package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.BoolSetting
import android.ext.settings.ExtSettings
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswDenyNativeDebug
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.ext.AppPrefUtils
import com.android.settings.ext.BoolSettingFragment
import com.android.settings.ext.BoolSettingFragmentPrefController
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterNativeDebugging : AswAdapter<AswDenyNativeDebug>() {

    override fun getAppSwitch() = AswDenyNativeDebug.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.aep_native_debug_title)

    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.aep_blocked)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.aep_allowed)

    override fun getNotificationToggleTitle(ctx: Context) = ctx.getText(R.string.aep_native_debug_notif_toggle_title)

    override fun getDetailFragmentClass() = AppNativeDebuggingFragment::class
}

@Composable
fun AppNativeDebuggingPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterNativeDebugging)
}

class AppNativeDebuggingFragment : AswExploitProtectionFragment<AswDenyNativeDebug>() {

    override fun getAswAdapter() = AswAdapterNativeDebugging

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.aep_native_debug_dvr_is_system_app
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_native_debug_footer)
    }
}

class AppDefaultNativeDebuggingPrefController(ctx: Context, key: String) :
    BoolSettingFragmentPrefController(ctx, key, ExtSettings.ALLOW_NATIVE_DEBUG_BY_DEFAULT) {

    override fun getSummary(): CharSequence {
        return resText(if (setting.get(mContext)) R.string.aep_default_summary_allowed_for_3p_apps else R.string.aep_default_summary_blocked)
    }
}

class AppDefaultNativeDebuggingFragment : BoolSettingFragment() {

    override fun getSetting(): BoolSetting {
        invertSetting = true
        return ExtSettings.ALLOW_NATIVE_DEBUG_BY_DEFAULT
    }

    override fun getTitle() = getText(R.string.aep_native_debug_title)

    override fun getMainSwitchTitle() = getText(R.string.aep_default_main_switch_block_for_3p_apps)

    override fun addExtraPrefs(screen: PreferenceScreen) {
        AswAdapterNativeDebugging.addAppListPageLink(screen)
    }

    override fun makeFooterPref(builder: FooterPreference.Builder): FooterPreference {
        val text = AppPrefUtils.getFooterForDefaultHardeningSetting(
            requireContext(),
            R.string.aep_native_debug_footer
        )
        return builder.setTitle(text).build()
    }
}

class NativeDebuggingAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterNativeDebugging) {

    override fun getAvailabilityStatus() = ExtSettingControllerHelper
        .getSecondaryUserOnlySettingAvailability(mContext)
}
