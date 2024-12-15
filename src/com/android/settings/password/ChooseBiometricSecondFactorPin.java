package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;
import static android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE;
import static com.android.internal.widget.LockDomain.Secondary;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_NONE;

import android.app.Activity;
import android.app.admin.DevicePolicyManager.PasswordComplexity;
import android.app.admin.PasswordMetrics;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImeAwareEditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.PasswordValidationError;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.core.InstrumentedFragment;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.List;

/**
 * Activity that allows the user to set/choose their biometric second factor PIN.
 * Based on upstream's ChooseLockPassword.
 */
public class ChooseBiometricSecondFactorPin extends SettingsActivity {
    private static final String TAG = "ChooseLockPassword";

    private static final String EXTRA_KEY_PRIMARY_CREDENTIAL = "primary_credential";
    // Not using Intent.EXTRA_USER_ID as we want to make this private to force use of the
    // IntentBuilder.
    private static final String EXTRA_USER_ID = "user_id";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    public static class IntentBuilder {

        private final Intent mIntent;

        public IntentBuilder(Context context) {
            mIntent = new Intent(context, ChooseBiometricSecondFactorPin.class);
        }

        public IntentBuilder setUserId(int userId) {
            mIntent.putExtra(EXTRA_USER_ID, userId);
            return this;
        }

        public IntentBuilder setPrimaryCredential(LockscreenCredential primaryCredential) {
            mIntent.putExtra(EXTRA_KEY_PRIMARY_CREDENTIAL, primaryCredential);
            return this;
        }

        public Intent build() {
            return mIntent;
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ChooseBiometricSecondFactorPinFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    protected boolean isToolbarEnabled() {
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return ChooseBiometricSecondFactorPinFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstanceState);
        findViewById(R.id.content_parent).setFitsSystemWindows(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static class ChooseBiometricSecondFactorPinFragment extends InstrumentedFragment
            implements OnEditorActionListener, TextWatcher,
            BiometricSecondFactorSaveAndFinishWorker.Listener {
        private static final String KEY_FIRST_PASSWORD = "first_password";
        private static final String KEY_UI_STAGE = "ui_stage";
        private static final String KEY_PRIMARY_CREDENTIAL = "primary_credential";
        private static final String FRAGMENT_BIOMETRIC_SECOND_FACTOR_TAG_SAVE_AND_FINISH =
                "save_and_finish_worker";
        private static final String KEY_IS_AUTO_CONFIRM_CHECK_MANUALLY_CHANGED =
                "auto_confirm_option_set_manually";

        private static final int MIN_AUTO_PIN_REQUIREMENT_LENGTH = 6;

        private LockscreenCredential mPrimaryCredential;
        private LockscreenCredential mChosenPassword;
        private ImeAwareEditText mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;

        private List<PasswordValidationError> mValidationErrors;

        protected int mUserId;

        private WrappedLockPatternUtils mLockPatternUtils;
        private BiometricSecondFactorSaveAndFinishWorker mSaveAndFinishWorker;

        protected Stage mUiStage = Stage.Introduction;
        private PasswordRequirementAdapter mPasswordRequirementAdapter;
        private GlifLayout mLayout;

        private LockscreenCredential mFirstPassword;
        private RecyclerView mPasswordRestrictionView;
        protected FooterButton mSkipOrClearButton;
        private FooterButton mNextButton;
        private TextView mMessage;
        protected CheckBox mAutoPinConfirmOption;
        protected TextView mAutoConfirmSecurityMessage;
        protected boolean mIsAutoPinConfirmOptionSetManually;

        private TextChangedHandler mTextChangedHandler;

        public static final int RESULT_FINISHED = RESULT_FIRST_USER;
        public static final int RESULT_NOT_FOREGROUND = RESULT_FIRST_USER + 1;

        private boolean mIsErrorTooShort = true;

        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(
                    R.string.biometric_second_factor_pin_choose_header,
                    R.string.next_label),

            NeedToConfirm(
                    R.string.biometric_second_factor_confirm_header,
                    R.string.lockpassword_confirm_label),

            ConfirmWrong(
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_confirm_label);

            Stage(int hintInNumeric, int nextButtonText) {
                this.numericHint = hintInNumeric;
                this.buttonText = nextButtonText;
            }

            public final int numericHint;
            public final int buttonText;

            public String getHint(Context context) {
                return context.getString(numericHint);
            }
        }

        // required constructor for fragments
        public ChooseBiometricSecondFactorPinFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent intent = getActivity().getIntent();
            if (!(getActivity() instanceof ChooseBiometricSecondFactorPin)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }

            Bundle extras = intent.getExtras();
            extras.getInt(EXTRA_USER_ID, -1);

            mLockPatternUtils = new WrappedLockPatternUtils(getActivity(), Secondary);

            mUserId = intent.getIntExtra(EXTRA_USER_ID, -1);

            mTextChangedHandler = new TextChangedHandler();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_biometric_second_factor_pin, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mLayout = (GlifLayout) view;

            // Make the password container consume the optical insets so the edit text is aligned
            // with the sides of the parent visually.
            ViewGroup container = view.findViewById(R.id.password_container);
            container.setOpticalInsets(Insets.NONE);

            final FooterBarMixin mixin = mLayout.getMixin(FooterBarMixin.class);
            mixin.setSecondaryButton(
                    new FooterButton.Builder(getActivity())
                            .setText(R.string.lockpassword_clear_label)
                            .setListener(this::onSkipOrClearButtonClick)
                            .setButtonType(FooterButton.ButtonType.SKIP)
                            .setTheme(
                                    com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                            .build()
            );
            mixin.setPrimaryButton(
                    new FooterButton.Builder(getActivity())
                            .setText(R.string.next_label)
                            .setListener(this::onNextButtonClick)
                            .setButtonType(FooterButton.ButtonType.NEXT)
                            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                            .build()
            );
            mSkipOrClearButton = mixin.getSecondaryButton();
            mNextButton = mixin.getPrimaryButton();

            mMessage = view.findViewById(R.id.sud_layout_description);
            mLayout.setIcon(getActivity().getDrawable(R.drawable.ic_lock));

            final LinearLayout headerLayout = view.findViewById(
                    com.google.android.setupdesign.R.id.sud_layout_header);
            setupPasswordRequirementsView(headerLayout);

            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordEntry = view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);
            mPasswordEntry.requestFocus();
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

            // Fetch the AutoPinConfirmOption
            mAutoPinConfirmOption = view.findViewById(R.id.auto_pin_confirm_enabler);
            mAutoConfirmSecurityMessage = view.findViewById(R.id.auto_pin_confirm_security_message);
            mIsAutoPinConfirmOptionSetManually = false;
            setOnAutoConfirmOptionClickListener();
            if (mAutoPinConfirmOption != null) {
                mAutoPinConfirmOption.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
                mAutoPinConfirmOption.setVisibility(View.GONE);
                mAutoPinConfirmOption.setChecked(false);
            }

            final Activity activity = getActivity();

            mPasswordEntry.setInputType(
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

            mPasswordEntry.setContentDescription(
                    getString(R.string.unlock_set_unlock_pin_title));

            // Can't set via XML since setInputType resets the fontFamily to null
            mPasswordEntry.setTypeface(Typeface.create(
                    getContext().getString(com.android.internal.R.string.config_headlineFontFamily),
                    Typeface.NORMAL));

            Intent intent = getActivity().getIntent();
            mPrimaryCredential = intent.getParcelableExtra(EXTRA_KEY_PRIMARY_CREDENTIAL);
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
            } else {
                // restore from previous state
                mFirstPassword = savedInstanceState.getParcelable(KEY_FIRST_PASSWORD);
                final String state = savedInstanceState.getString(KEY_UI_STAGE);
                if (state != null) {
                    mUiStage = Stage.valueOf(state);
                    updateStage(mUiStage);
                }
                mIsAutoPinConfirmOptionSetManually =
                        savedInstanceState.getBoolean(KEY_IS_AUTO_CONFIRM_CHECK_MANUALLY_CHANGED);

                mPrimaryCredential = savedInstanceState.getParcelable(KEY_PRIMARY_CREDENTIAL);

                // Re-attach to the exiting worker if there is one.
                mSaveAndFinishWorker = (BiometricSecondFactorSaveAndFinishWorker)
                        getFragmentManager().findFragmentByTag(
                                FRAGMENT_BIOMETRIC_SECOND_FACTOR_TAG_SAVE_AND_FINISH);
            }

            if (activity instanceof SettingsActivity) {
                final SettingsActivity sa = (SettingsActivity) activity;
                String title = Stage.Introduction.getHint(getContext());
                sa.setTitle(title);
                mLayout.setHeaderText(title);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mPrimaryCredential != null) {
                mPrimaryCredential.zeroize();
            }
            // Force a garbage collection immediately to remove remnant of user password shards
            // from memory.
            System.gc();
            System.runFinalization();
            System.gc();
        }

        private void setupPasswordRequirementsView(@Nullable ViewGroup view) {
            if (view == null) {
                return;
            }

            createHintMessageView(view);
            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordRequirementAdapter = new PasswordRequirementAdapter(getActivity());
            mPasswordRestrictionView.setAdapter(mPasswordRequirementAdapter);
            view.addView(mPasswordRestrictionView);
        }

        private void createHintMessageView(ViewGroup view) {
            if (mPasswordRestrictionView != null) {
                return;
            }

            final TextView sucTitleView = view.findViewById(R.id.suc_layout_title);
            final ViewGroup.MarginLayoutParams titleLayoutParams =
                    (ViewGroup.MarginLayoutParams) sucTitleView.getLayoutParams();
            mPasswordRestrictionView = new RecyclerView(getActivity());
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(titleLayoutParams.leftMargin, getResources().getDimensionPixelSize(
                    R.dimen.biometric_second_factor_pin_requirement_view_margin_top),
                    titleLayoutParams.leftMargin, 0);
            mPasswordRestrictionView.setLayoutParams(lp);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.PAGE_UNKNOWN;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(mUiStage);
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(this);
            } else {
                mPasswordEntry.requestFocus();
                mPasswordEntry.scheduleShowSoftInput();
            }
        }

        @Override
        public void onPause() {
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(null);
            }
            super.onPause();
        }

        @Override
        public void onStop() {
            super.onStop();
            if (!getActivity().isChangingConfigurations()) {
                getActivity().setResult(RESULT_NOT_FOREGROUND);
                getActivity().finish();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(KEY_UI_STAGE, mUiStage.name());
            outState.putParcelable(KEY_FIRST_PASSWORD, mFirstPassword);
            outState.putParcelable(KEY_PRIMARY_CREDENTIAL, mPrimaryCredential.duplicate());
            outState.putBoolean(KEY_IS_AUTO_CONFIRM_CHECK_MANUALLY_CHANGED,
                    mIsAutoPinConfirmOptionSetManually);
        }

        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;
            mUiStage = stage;
            updateUi();

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage) {
                mLayout.announceForAccessibility(mLayout.getHeaderText());
            }
        }

        /**
         * Validates PIN and returns the validation result and updates mValidationErrors
         * to reflect validation results.
         *
         * @param credential credential the user typed in.
         * @return whether password satisfies all the requirements.
         */
        @VisibleForTesting
        boolean validatePassword(LockscreenCredential credential) {
            PasswordMetrics minMetrics = new PasswordMetrics(CREDENTIAL_TYPE_NONE);
            @PasswordComplexity int minComplexity = PASSWORD_COMPLEXITY_NONE;
            mValidationErrors = PasswordMetrics.validateCredential(minMetrics, minComplexity,
                    credential);
            return mValidationErrors.isEmpty();
        }

        public void handleNext() {
            if (mSaveAndFinishWorker != null) return;
            // TODO(b/120484642): This is a point of entry for passwords from the UI
            final Editable passwordText = mPasswordEntry.getText();
            if (TextUtils.isEmpty(passwordText)) {
                return;
            }
            mChosenPassword = LockscreenCredential.createPin(passwordText);
            if (mUiStage == Stage.Introduction) {
                if (validatePassword(mChosenPassword)) {
                    mFirstPassword = mChosenPassword;
                    mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                } else {
                    mChosenPassword.zeroize();
                }
            } else if (mUiStage == Stage.NeedToConfirm) {
                if (mChosenPassword.equals(mFirstPassword)) {
                    startSaveAndFinish();
                } else {
                    CharSequence tmp = mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    updateStage(Stage.ConfirmWrong);
                    mChosenPassword.zeroize();
                }
            }
        }

        protected void setNextEnabled(boolean enabled) {
            mNextButton.setEnabled(enabled);
        }

        protected void setNextText(int text) {
            mNextButton.setText(getActivity(), text);
        }

        protected void onSkipOrClearButtonClick(View view) {
            mPasswordEntry.setText("");
        }

        protected void onNextButtonClick(View view) {
            handleNext();
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleNext();
                return true;
            }
            return false;
        }

    String[] convertErrorCodeToMessages() {
        var pvec =
                new ChooseLockPassword.ChooseLockPasswordFragment.PasswordValidationErrorConverter(
                        getContext(), false, mValidationErrors);
        String[] res = pvec.convertErrorCodeToMessages();
        mIsErrorTooShort = pvec.mIsErrorTooShort;
        return res;
    }

        /**
         * Update the hint based on current Stage and length of password entry
         */
        protected void updateUi() {
            final boolean canInput = mSaveAndFinishWorker == null;

            LockscreenCredential password = LockscreenCredential.createPin(mPasswordEntry.getText());
            final int length = password.size();
            if (mUiStage == Stage.Introduction) {
                mLayout.setDescriptionText(R.string.biometric_second_factor_pin_choose_description);
                mPasswordRestrictionView.setVisibility(View.VISIBLE);
                final boolean passwordCompliant = validatePassword(password);
                String[] messages = convertErrorCodeToMessages();
                // Update the fulfillment of requirements.
                mPasswordRequirementAdapter.setRequirements(messages, mIsErrorTooShort);
                // set the visibility of pin_auto_confirm option accordingly
                setAutoPinConfirmOption(passwordCompliant, length);
                // Enable/Disable the next button accordingly.
                setNextEnabled(passwordCompliant);
            } else {
                mLayout.setDescriptionText(
                        R.string.biometric_second_factor_pin_choose_description_empty);
                // Hide password requirement view when we are just asking user to confirm the pw.
                mPasswordRestrictionView.setVisibility(View.GONE);
                setHeaderText(mUiStage.getHint(getContext()));
                setNextEnabled(canInput && length >= LockPatternUtils.MIN_LOCK_PASSWORD_SIZE);
                mSkipOrClearButton.setVisibility(toVisibility(canInput && length > 0));

                // Hide the pin_confirm option when we are just asking user to confirm the pwd.
                mAutoPinConfirmOption.setVisibility(View.GONE);
                mAutoConfirmSecurityMessage.setVisibility(View.GONE);
            }
            mMessage.setVisibility(View.GONE);

            setNextText(mUiStage.buttonText);
            mPasswordEntryInputDisabler.setInputEnabled(canInput);
            password.zeroize();
        }

        protected int toVisibility(boolean visibleOrGone) {
            return visibleOrGone ? View.VISIBLE : View.GONE;
        }

        private void setAutoPinConfirmOption(boolean enabled, int length) {
            if (!LockPatternUtils.isAutoPinConfirmFeatureAvailable(Secondary)
                    || mAutoPinConfirmOption == null) {
                return;
            }
            if (enabled && isAutoPinConfirmPossible(length)) {
                mAutoPinConfirmOption.setVisibility(View.VISIBLE);
                mAutoConfirmSecurityMessage.setVisibility(View.VISIBLE);
            } else {
                mAutoPinConfirmOption.setVisibility(View.GONE);
                mAutoConfirmSecurityMessage.setVisibility(View.GONE);
                mAutoPinConfirmOption.setChecked(false);
            }
        }

        private boolean isAutoPinConfirmPossible(int currentPinLength) {
            return currentPinLength >= MIN_AUTO_PIN_REQUIREMENT_LENGTH;
        }

        private void setOnAutoConfirmOptionClickListener() {
            if (mAutoPinConfirmOption != null) {
                mAutoPinConfirmOption.setOnClickListener((v) -> {
                    mIsAutoPinConfirmOptionSetManually = true;
                });
            }
        }

        private void setHeaderText(String text) {
            // Only set the text if it is different than the existing one to avoid announcing again.
            if (!TextUtils.isEmpty(mLayout.getHeaderText())
                    && mLayout.getHeaderText().toString().equals(text)) {
                return;
            }
            mLayout.setHeaderText(text);
        }

        public void afterTextChanged(Editable s) {
            // Changing the text while error displayed resets to NeedToConfirm state
            if (mUiStage == Stage.ConfirmWrong) {
                mUiStage = Stage.NeedToConfirm;
            }
            // Schedule the UI update.
            mTextChangedHandler.notifyAfterTextChanged();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        private void startSaveAndFinish() {
            if (mSaveAndFinishWorker != null) {
                Log.w(TAG, "startSaveAndFinish with an existing " +
                        "BiometricSecondFactorSaveAndFinishWorker.");
                return;
            }

            ConfirmDeviceCredentialUtils.hideImeImmediately(
                    getActivity().getWindow().getDecorView());

            mPasswordEntryInputDisabler.setInputEnabled(false);
            mSaveAndFinishWorker = new BiometricSecondFactorSaveAndFinishWorker();
            mSaveAndFinishWorker
                    .setListener(this);

            getFragmentManager().beginTransaction().add(mSaveAndFinishWorker,
                    FRAGMENT_BIOMETRIC_SECOND_FACTOR_TAG_SAVE_AND_FINISH).commit();
            getFragmentManager().executePendingTransactions();

            // update the setting before triggering the password save workflow,
            // so that pinLength information is stored accordingly when setting is turned on.
            mLockPatternUtils.setAutoPinConfirm(
                    (mAutoPinConfirmOption != null && mAutoPinConfirmOption.isChecked()),
                    mUserId);

            mSaveAndFinishWorker.start(mLockPatternUtils,
                    mChosenPassword, mPrimaryCredential, mUserId);
        }

        @Override
        public void onChosenLockSaveFinished() {
            getActivity().setResult(RESULT_FINISHED);

            if (mChosenPassword != null) {
                mChosenPassword.zeroize();
            }

            if (mPrimaryCredential != null) {
                mPrimaryCredential.zeroize();
            }
            if (mFirstPassword != null) {
                mFirstPassword.zeroize();
            }

            mPasswordEntry.setText("");

            if (mLayout != null) {
                mLayout.announceForAccessibility(
                        getString(R.string.accessibility_setup_password_complete));
            }

            getActivity().finish();
        }

        class TextChangedHandler extends Handler {
            private static final int ON_TEXT_CHANGED = 1;
            private static final int DELAY_IN_MILLISECOND = 100;

            /**
             * With the introduction of delay, we batch processing the text changed event to reduce
             * unnecessary UI updates.
             */
            private void notifyAfterTextChanged() {
                removeMessages(ON_TEXT_CHANGED);
                sendEmptyMessageDelayed(ON_TEXT_CHANGED, DELAY_IN_MILLISECOND);
            }

            @Override
            public void handleMessage(Message msg) {
                if (getActivity() == null) {
                    return;
                }
                if (msg.what == ON_TEXT_CHANGED) {
                    updateUi();
                }
            }
        }
    }
}
