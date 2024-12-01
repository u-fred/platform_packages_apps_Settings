package com.android.settings.password;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.WrappedLockPatternUtils;
import com.android.settings.R;

/**
 * An invisible retained worker fragment to track the AsyncWork that saves the chosen biometric
 * second factor.
 * Based on upstream's SaveAndFinishWorker.
 */
public class BiometricSecondFactorSaveAndFinishWorker extends Fragment {
    private static final String TAG = "BiometricSecondFactorSaveAndFinishWorker";

    private Listener mListener;
    private boolean mFinished;

    private WrappedLockPatternUtils mUtils;

    private int mUserId;

    private LockscreenCredential mChosenCredential;
    private LockscreenCredential mPrimaryCredential;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public BiometricSecondFactorSaveAndFinishWorker setListener(Listener listener) {
        if (mListener == listener) {
            return this;
        }

        mListener = listener;
        if (mFinished && mListener != null) {
            mListener.onChosenLockSaveFinished();
        }
        return this;
    }

    private void prepare(WrappedLockPatternUtils utils, LockscreenCredential chosenCredential,
            LockscreenCredential primaryCredential, int userId) {
        mUtils = utils;
        mUserId = userId;
        mFinished = false;

        mChosenCredential = chosenCredential;
        mPrimaryCredential = primaryCredential;
    }

    public void start(WrappedLockPatternUtils utils, LockscreenCredential chosenCredential,
            LockscreenCredential primaryCredential, int userId) {
        prepare(utils, chosenCredential, primaryCredential, userId);
        new BiometricSecondFactorSaveAndFinishWorker.Task().execute();
    }

    /**
     * Executes the save and verify work in background.
     * @return whether change was successful
     */
    private boolean saveAndVerifyInBackground() {
        final int userId = mUserId;
        try {
            if (!mUtils.setLockCredential(mChosenCredential, mPrimaryCredential, userId)) {
                return false;
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to set credential", e);
            return false;
        }

        return true;
    }

    private void finish() {
        mFinished = true;
        if (mListener != null) {
            mListener.onChosenLockSaveFinished();
        }
    }

    private class Task extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params){
            return saveAndVerifyInBackground();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Toast.makeText(getContext(),
                        R.string.lockpassword_credential_changed_unable_to_update_biometric_second_factor,
                        Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    interface Listener {
        void onChosenLockSaveFinished();
    }
}
