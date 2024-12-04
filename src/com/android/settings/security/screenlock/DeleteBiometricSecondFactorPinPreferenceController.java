package com.android.settings.security.screenlock;

import static com.android.internal.widget.LockDomain.Secondary;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;

/**
 * Controller for deleting a user's biometric second factor PIN after they confirm the delete by
 * way of confirmation dialog.
 */
public class DeleteBiometricSecondFactorPinPreferenceController extends BasePreferenceController {

    public static final String PREF_KEY = "delete_pin";

    public static final String TAG_DELETE_BIOMETRIC_SECOND_FACTOR_PIN_CONFIRM_DIALOG =
            "delete_biometric_second_factor_pin_confirm_dialog";

    private final Fragment mHost;

    private final LockPatternUtils mLockPatternUtils;

    private final int mUserId;
    private final LockscreenCredential mPrimaryCredential;

    public interface DeleteConfirmedCallback {
        void onBiometricSecondFactorPinDeleteConfirmed();
    }

    public DeleteBiometricSecondFactorPinPreferenceController(Context context, Fragment host,
            int userId, LockscreenCredential primaryCredential) {
        super(context, PREF_KEY);
        mHost = host;
        mUserId = userId;
        mPrimaryCredential = primaryCredential;
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLockPatternUtils.isSecure(mUserId, Secondary)) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    public void onBiometricSecondFactorPinDeleteConfirmed() {
        mLockPatternUtils.setLockCredential(
                LockscreenCredential.createNone(), mPrimaryCredential, Secondary, mUserId);
        mHost.getActivity().setResult(Activity.RESULT_OK);
        mHost.getActivity().finish();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        DeleteBiometricSecondFactorPinConfirmationDialog.newInstance(
                R.string.biometric_second_factor_pin_delete_confirmation_dialog_title,
                R.string.biometric_second_factor_pin_delete_confirmation_dialog_message)
                .show(mHost.getChildFragmentManager(),
                        TAG_DELETE_BIOMETRIC_SECOND_FACTOR_PIN_CONFIRM_DIALOG);

        return true;
    }

    /**
     * Dialog for confirming that the user wants to delete their biometric second factor PIN.
     * Based on upstream's ChooseLockGeneric.FactoryResetProtectionWarningDialog.
     */
    public static class DeleteBiometricSecondFactorPinConfirmationDialog
            extends InstrumentedDialogFragment {

        private static final String ARG_TITLE_RES = "titleRes";
        private static final String ARG_MESSAGE_RES = "messageRes";

        public static DeleteBiometricSecondFactorPinConfirmationDialog newInstance(
                int titleRes, int messageRes) {
            DeleteBiometricSecondFactorPinConfirmationDialog frag =
                    new DeleteBiometricSecondFactorPinConfirmationDialog();
            Bundle args = new Bundle();
            args.putInt(ARG_TITLE_RES, titleRes);
            args.putInt(ARG_MESSAGE_RES, messageRes);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void show(FragmentManager manager, String tag) {
            if (manager.findFragmentByTag(tag) == null) {
                // Prevent opening multiple dialogs if tapped on button quickly
                super.show(manager, tag);
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();

            return new AlertDialog.Builder(getActivity())
                    .setTitle(args.getInt(ARG_TITLE_RES))
                    .setMessage(args.getInt(ARG_MESSAGE_RES))
                    .setPositiveButton(
                            R.string.biometric_second_factor_pin_delete_confirmation_dialog_positive_button,
                            (dialog, whichButton) -> ((DeleteConfirmedCallback) getParentFragment())
                                    .onBiometricSecondFactorPinDeleteConfirmed())
                    .setNegativeButton(R.string.cancel, (dialog, whichButton) -> dismiss())
                    .create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.PAGE_UNKNOWN;
        }
    }
}
