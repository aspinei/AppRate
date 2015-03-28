package com.tjeannin.apprate.sample;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.widget.ShareDialog;
import com.tjeannin.apprate.AppRate;

public class MainActivity extends Activity implements OnClickListener {

    private static final String LUNCH_COUNT = "lunch_count";
    private int lunchCount;
    private SharedPreferences sharedPreferences;
    private CallbackManager callbackManager;
    private ShareDialog shareDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        // this part is optional
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        lunchCount = sharedPreferences.getInt(LUNCH_COUNT, 0) + 1;
        sharedPreferences.edit().putInt(LUNCH_COUNT, lunchCount).commit();

        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.lunch_count)).setText(String.valueOf(lunchCount));
        findViewById(R.id.restart_activity).setOnClickListener(this);
        findViewById(R.id.force_crash).setOnClickListener(this);
        findViewById(R.id.reset_apprate_prefs).setOnClickListener(this);

        // Init AppRater.
        new AppRate(this)
                .setMinDaysUntilPrompt(0)
                .setMinLaunchesUntilPrompt(3)
                .setShowIfAppHasCrashed(false)
                .init();
    }

    @SuppressWarnings("null")
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.restart_activity:
                finish();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;

            case R.id.reset_apprate_prefs:
                AppRate.reset(this);
                sharedPreferences.edit().putInt(LUNCH_COUNT, 0).commit();
                ((TextView) findViewById(R.id.lunch_count)).setText(String.valueOf(0));
                break;

            case R.id.force_crash:
                String crash = null;
                crash.toString();
                break;

            default:
                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
