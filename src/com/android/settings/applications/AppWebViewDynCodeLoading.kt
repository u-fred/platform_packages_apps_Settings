package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.ext.settings.ExtSettings
import android.ext.settings.app.AswRestrictWebViewDynCodeLoading
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.ext.BoolSettingFragment
import com.android.settings.ext.BoolSettingFragmentPrefController
import com.android.settings.ext.ExtSettingControllerHelper
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterWebViewDynCodeLoading : AswAdapter<AswRestrictWebViewDynCodeLoading>() {

    override fun getAppSwitch() = AswRestrictWebViewDynCodeLoading.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.aep_webview_jit)

    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.aep_disabled)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.aep_enabled)

    override fun getDetailFragmentClass() = AppWebViewDynCodeLoadingFragment::class
}

@Composable
fun AppWebViewDynCodeLoadingPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterWebViewDynCodeLoading)
}

class AppWebViewDynCodeLoadingFragment : AswExploitProtectionFragment<AswRestrictWebViewDynCodeLoading>() {

    override fun getAswAdapter() = AswAdapterWebViewDynCodeLoading

    override fun getTitle() = getText(R.string.aep_webview_jit)

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.aep_webview_jit_footer)
    }
}

class AppDefaultWebViewDynCodeLoadingPrefController(ctx: Context, key: String) :
        BoolSettingFragmentPrefController(ctx, key, ExtSettings.RESTRICT_WEBVIEW_DYN_CODE_LOADING_BY_DEFAULT) {

    override fun getSummaryOn() = resText(R.string.aep_default_summary_disabled)
    override fun getSummaryOff() = resText(R.string.aep_default_summary_enabled_for_3p_apps)
}

class AppDefaultWebViewDynCodeLoadingFragment : BoolSettingFragment() {

    override fun getSetting() = ExtSettings.RESTRICT_WEBVIEW_DYN_CODE_LOADING_BY_DEFAULT

    override fun getTitle() = resText(R.string.aep_webview_jit)

    override fun getMainSwitchTitle() = resText(R.string.aep_default_main_switch_disable_for_3p_apps)

    override fun addExtraPrefs(screen: PreferenceScreen) {
        AswAdapterWebViewDynCodeLoading.addAppListPageLink(screen)
    }

    override fun makeFooterPref(builder: FooterPreference.Builder): FooterPreference {
        return builder.setTitle(R.string.aep_webview_jit_footer).build()
    }
}

class WebViewDynCodeLoadingAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterWebViewDynCodeLoading) {

    override fun getAvailabilityStatus() = ExtSettingControllerHelper
        .getSecondaryUserOnlySettingAvailability(mContext)
}
