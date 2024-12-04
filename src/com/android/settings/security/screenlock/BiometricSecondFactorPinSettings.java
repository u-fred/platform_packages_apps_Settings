package com.android.settings.security.screenlock;

import static android.app.Activity.RESULT_FIRST_USER;
import static com.android.internal.widget.LockDomain.Secondary;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.ChooseBiometricSecondFactorPin;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that allows the user to configure the settings of their biometric second factor PIN.
 * Based on upstream's ScreenLockSettings.
 */
@SearchIndexable
public class BiometricSecondFactorPinSettings extends DashboardFragment
        implements DeleteBiometricSecondFactorPinPreferenceController.DeleteConfirmedCallback {

    private static final String TAG = "BiometricSecondFactorPinSettings";

    public static final int REQUEST_CHANGE_PIN = 1;

    public static final int RESULT_NOT_FOREGROUND = RESULT_FIRST_USER;

    private static final String EXTRA_PRIMARY_CREDENTIAL = "primary_credential";
    // Not using Intent.EXTRA_USER_ID as we want to make this private to force use of the
    // IntentBuilder.
    private static final String EXTRA_USER_ID = "user_id";

    private DeleteBiometricSecondFactorPinPreferenceController
            mDeleteBiometricSecondFactorPinPreferenceController;

    private boolean mLaunchedChooseBiometricSecondFactorPin;

    private int mUserId;
    private LockscreenCredential mPrimaryCredential;

    public static class IntentBuilder {

        private final SubSettingLauncher mLauncher;
        private final Bundle mExtras;


        public IntentBuilder(Context context) {
            mLauncher = new SubSettingLauncher(context);
            mLauncher.setDestination(BiometricSecondFactorPinSettings.class.getName());
            mExtras = new Bundle();
            mLauncher.setExtras(mExtras);
        }

        public IntentBuilder setUserId(int userId) {
            mExtras.putInt(EXTRA_USER_ID, userId);
            return this;
        }

        public IntentBuilder setPrimaryCredential(LockscreenCredential primaryCredential) {
            mExtras.putParcelable(EXTRA_PRIMARY_CREDENTIAL, primaryCredential);
            return this;
        }

        public IntentBuilder setSourceMetricsCateogry(int sourceMetricsCateogry) {
            mLauncher.setSourceMetricsCategory(sourceMetricsCateogry);
            return this;
        }

        public Intent build() {
            return mLauncher.toIntent();
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.biometric_second_factor_pin_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* parent */,
                            new WrappedLockPatternUtils(context, null), null, 0);
                }
            };

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            DashboardFragment parent, WrappedLockPatternUtils lockPatternUtils,
            LockscreenCredential primaryCredential, int userId) {

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new PinPrivacyPreferenceController(
                context, userId, lockPatternUtils));
        controllers.add(new PinScramblingPrefController(context, lockPatternUtils));
        controllers.add(new DeleteBiometricSecondFactorPinPreferenceController(context, parent,
                userId, primaryCredential, lockPatternUtils));
        controllers.add(new ChangeBiometricSecondFactorPinPreferenceController(context, parent,
                userId, lockPatternUtils, REQUEST_CHANGE_PIN, primaryCredential));

        return controllers;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        WrappedLockPatternUtils lpu = new WrappedLockPatternUtils(context, Secondary);
        List<AbstractPreferenceController> controllers =  buildPreferenceControllers(context,
                this /* parent */, lpu, mPrimaryCredential, mUserId);

        for (AbstractPreferenceController controller : controllers) {
            if (controller.getPreferenceKey() ==
                    DeleteBiometricSecondFactorPinPreferenceController.PREF_KEY) {
                mDeleteBiometricSecondFactorPinPreferenceController =
                        (DeleteBiometricSecondFactorPinPreferenceController) controller;
            }
        }

        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.biometric_second_factor_pin_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        // These must be set here so that #createPreferenceControllers can access them.
        mPrimaryCredential = getIntent().getParcelableExtra(
                EXTRA_PRIMARY_CREDENTIAL, LockscreenCredential.class);
        mUserId = getIntent().getIntExtra(EXTRA_USER_ID, -1);

        super.onAttach(context);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations() && !mLaunchedChooseBiometricSecondFactorPin) {
            setResult(RESULT_NOT_FOREGROUND);
            finish();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        String key = pref.getKey();
        if (ChangeBiometricSecondFactorPinPreferenceController.PREF_KEY.equals(key)) {
            mLaunchedChooseBiometricSecondFactorPin = true;
        }
        return super.onPreferenceTreeClick(pref);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHANGE_PIN) {
            mLaunchedChooseBiometricSecondFactorPin = false;
            if (resultCode == ChooseBiometricSecondFactorPin
                    .ChooseBiometricSecondFactorPinFragment.RESULT_NOT_FOREGROUND) {
                setResult(RESULT_NOT_FOREGROUND);
                finish();
            }
        }
    }

    @Override
    public void onBiometricSecondFactorPinDeleteConfirmed() {
        mDeleteBiometricSecondFactorPinPreferenceController
                .onBiometricSecondFactorPinDeleteConfirmed();
    }
}
