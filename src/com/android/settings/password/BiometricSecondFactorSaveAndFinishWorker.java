/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.password;

import static com.android.internal.widget.LockDomain.Secondary;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
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

    private LockPatternUtils mUtils;

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

    private void prepare(LockPatternUtils utils, LockscreenCredential chosenCredential,
            LockscreenCredential primaryCredential, int userId) {
        mUtils = utils;
        mUserId = userId;
        mFinished = false;

        mChosenCredential = chosenCredential;
        mPrimaryCredential = primaryCredential;
    }

    public void start(LockPatternUtils utils, LockscreenCredential chosenCredential,
            LockscreenCredential primaryCredential, int userId) {
        prepare(utils, chosenCredential, primaryCredential, userId);
        new BiometricSecondFactorSaveAndFinishWorker.Task().execute();
    }

    private boolean saveAndVerifyInBackground() {
        final int userId = mUserId;
        try {
            if (!mUtils.setLockCredential(mChosenCredential, mPrimaryCredential, Secondary,
                    userId)) {
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
                        R.string.biometric_second_factor_pin_change_failed, Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    interface Listener {
        void onChosenLockSaveFinished();
    }
}
