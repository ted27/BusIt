package com.example.busit.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.example.busit.api.BusItConnection.OnDoneCallback;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;

public class GoogleAuth {
    private static final String DEBUG_TAG = "BusIt";
    private static final int GOOGLE_GOT_AUTH_CODE = 89230589;
    private static final int GOOGLE_AUTH_CODE = 102938102;
    private static final String AUTH_SETTINGS_FILE = "access_prefs";
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
    private final Activity context;
    private OnDoneCallback<String> callback;
    private final SharedPreferences auth_settings;

    public GoogleAuth(Activity context) {
        this.context = context;
        this.auth_settings = context.getSharedPreferences(AUTH_SETTINGS_FILE, 0);
    }

    public boolean needsToSignIn() {
        return !this.auth_settings.contains(ACCESS_TOKEN_KEY);
    }

    public String getSavedAuthToken() {
        return this.auth_settings.getString(ACCESS_TOKEN_KEY, "");
    }

    public void getAuthToken(OnDoneCallback<String> callback) {
        Log.d(DEBUG_TAG, "checking logins...");
        this.callback = callback; // XXX: this is pretty weird
        AccountManager mAccountManager = AccountManager.get(this.context);
        Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

        if (accounts.length == 0) {
            new AlertDialog.Builder(this.context).setMessage("No Google Account Found :(").show();
        }

        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                new String[] { "com.google" }, false, null, null, null, null);
        this.context.startActivityForResult(intent, GOOGLE_AUTH_CODE);
    }

    // TODO: This might be a weird way of approaching this.
    // I'm leaving this comment here so that 3-year-future-me will look back
    // with consternation.
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == GOOGLE_AUTH_CODE) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Log.d(DEBUG_TAG, "Logged in as: " + accountName);
            new GetTokenTask().execute(accountName);
        }
        if (requestCode == GOOGLE_GOT_AUTH_CODE) {
            Log.d(DEBUG_TAG, "GOT AUTH TOKEN WOOHOOO: " + data.toString());
        }
    }

    private class GetTokenTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... arg0) {
            try {
                return GoogleAuthUtil.getToken(context, arg0[0], SCOPE);
            } catch (UserRecoverableAuthException e) {
                context.startActivityForResult(e.getIntent(), GOOGLE_GOT_AUTH_CODE);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String r) {
            Log.d(DEBUG_TAG, "got an auth token: " + r);
            auth_settings.edit().putString(ACCESS_TOKEN_KEY, r).commit();
            callback.onDone(r);
        }
    }
}