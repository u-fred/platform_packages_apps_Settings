package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.ExtSettings
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswRestrictStorageDynCodeLoading
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.ext.BoolSettingFragment
import com.android.settings.ext.BoolSettingFragmentPrefController
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterStorageDynCodeLoading : AswAdapter<AswRestrictStorageDynCodeLoading>() {

    override fun getAppSwitch() = AswRestrictStorageDynCodeLoading.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.aep_storage_dcl)
    override fun getShortAswTitle(ctx: Context) = ctx.getText(R.string.aep_storage_dcl_short)

    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.aep_restricted)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.aep_allowed)

    override fun getNotificationToggleTitle(ctx: Context) = ctx.getText(R.string.dcl_notif_toggle_title)

    override fun getDetailFragmentClass() = AppStorageDynCodeLoadingFragment::class
}

@Composable
fun AppStorageDynCodeLoadingPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterStorageDynCodeLoading)
}

class AppStorageDynCodeLoadingFragment : AswExploitProtectionFragment<AswRestrictStorageDynCodeLoading>() {

    override fun getAswAdapter() = AswAdapterStorageDynCodeLoading

    override fun getTitle() = getText(R.string.aep_storage_dcl_short)

    override fun getSummaryForImmutabilityReason(ir: Int): CharSequence? {
        val id = when (ir) {
            AppSwitch.IR_IS_SYSTEM_APP -> R.string.aep_storage_dcl_dvr_is_system_app
            else -> return null
        }
        return getText(id)
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_storage_dcl_footer)
    }
}

class AppDefaultStorageDynCodeLoadingPrefController(ctx: Context, key: String) :
        BoolSettingFragmentPrefController(ctx, key, ExtSettings.RESTRICT_STORAGE_DYN_CODE_LOADING_BY_DEFAULT) {

    override fun getSummaryOn() = resText(R.string.aep_default_summary_restricted)
    override fun getSummaryOff() = resText(R.string.aep_default_summary_allowed_for_3p_apps)
}

class AppDefaultStorageDynCodeLoadingFragment : BoolSettingFragment() {

    override fun getSetting() = ExtSettings.RESTRICT_STORAGE_DYN_CODE_LOADING_BY_DEFAULT

    override fun getTitle() = resText(R.string.aep_storage_dcl_short)

    override fun getMainSwitchTitle() = resText(R.string.aep_default_main_switch_restrict_for_3p_apps)

    override fun addExtraPrefs(screen: PreferenceScreen) {
        AswAdapterStorageDynCodeLoading.addAppListPageLink(screen)
    }

    override fun makeFooterPref(builder: FooterPreference.Builder): FooterPreference {
        val s = getString(R.string.app_exploit_protection_default_value_warning) + "\n\n" +
                getString(R.string.aep_storage_dcl_footer) + "\n\n" +
                getString(R.string.aep_default_dcl_footer_ending)
        return builder.setTitle(s).build()
    }
}

class StorageDynCodeLoadingAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterStorageDynCodeLoading) {

    override fun getAvailabilityStatus() = ExtSettingControllerHelper
        .getSecondaryUserOnlySettingAvailability(mContext)
}
