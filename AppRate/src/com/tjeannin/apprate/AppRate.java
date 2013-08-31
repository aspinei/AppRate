package com.tjeannin.apprate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;
import com.facebook.widget.FacebookDialog;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

public class AppRate implements android.content.DialogInterface.OnClickListener, OnCancelListener {

    private static final String TAG = "AppRater";
    private Activity hostActivity;
    private OnClickListener clickListener;
    private SharedPreferences preferences;
    private AlertDialog.Builder dialogBuilder = null;
    private long minLaunchesUntilPrompt = 0;
    private long minDaysUntilPrompt = 0;
    private long minLaunchesUntilSocial = 2;
    private long minDaysUntilSocial = 0;
    private boolean showIfHasCrashed = true;
    private boolean mShowSocial = false;


    public AppRate(Activity hostActivity) {
        this.hostActivity = hostActivity;
        preferences = hostActivity.getSharedPreferences(PrefsContract.SHARED_PREFS_NAME, 0);
    }

    /**
     * Reset all the data collected about number of launches and days until first launch.
     * @param context A context.
     */
    public static void reset(Context context) {
        context.getSharedPreferences(PrefsContract.SHARED_PREFS_NAME, 0).edit().clear().commit();
        Log.d(TAG, "Cleared AppRate shared preferences.");
    }

    /**
     * @param context A context of the current application.
     * @return The application name of the current application.
     */
    private static final String getApplicationName(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            applicationInfo = null;
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "(unknown)");
    }

    /**
     * @param minLaunchesUntilSocial The minimum number of times the user lunches the application before showing the sharing dialog.<br/>
     *                               Default value is 10 times.
     * @return This {@link AppRate} object to allow chaining.
     */
    public AppRate setMinLaunchesUntilSocial(long minLaunchesUntilSocial) {
        this.minLaunchesUntilSocial = minLaunchesUntilSocial;
        return this;
    }

    /**
     * @param minDaysUntilSocial The minimum number of days before showing the sharing dialog.<br/>
     *                           Default value is 10 days.
     * @return This {@link AppRate} object to allow chaining.
     */
    public AppRate setMinDaysUntilSocial(long minDaysUntilSocial) {
        this.minDaysUntilSocial = minDaysUntilSocial;
        return this;
    }

    /**
     * @param minLaunchesUntilPrompt The minimum number of times the user lunches the application before showing the rate dialog.<br/>
     *                               Default value is 0 times.
     * @return This {@link AppRate} object to allow chaining.
     */
    public AppRate setMinLaunchesUntilPrompt(long minLaunchesUntilPrompt) {
        this.minLaunchesUntilPrompt = minLaunchesUntilPrompt;
        return this;
    }

    /**
     * @param minDaysUntilPrompt The minimum number of days before showing the rate dialog.<br/>
     *                           Default value is 0 days.
     * @return This {@link AppRate} object to allow chaining.
     */
    public AppRate setMinDaysUntilPrompt(long minDaysUntilPrompt) {
        this.minDaysUntilPrompt = minDaysUntilPrompt;
        return this;
    }

    /**
     * @param showIfCrash If <code>false</code> the rate dialog will not be shown if the application has crashed once.<br/>
     *                    Default value is <code>false</code>.
     * @return This {@link AppRate} object to allow chaining.
     */
    public AppRate setShowIfAppHasCrashed(boolean showIfCrash) {
        showIfHasCrashed = showIfCrash;
        return this;
    }

    /**
     * Use this method if you want to customize the style and content of the rate dialog.<br/>
     * When using the {@link AlertDialog.Builder} you should use:
     * <ul>
     * <li>{@link AlertDialog.Builder#setPositiveButton} for the <b>rate</b> button.</li>
     * <li>{@link AlertDialog.Builder#setNeutralButton} for the <b>rate later</b> button.</li>
     * <li>{@link AlertDialog.Builder#setNegativeButton} for the <b>never rate</b> button.</li>
     * </ul>
     * @param customBuilder The custom dialog you want to use as the rate dialog.
     * @return This {@link AppRate} object to allow chaining.
     */
    public AppRate setCustomDialog(AlertDialog.Builder customBuilder) {
        dialogBuilder = customBuilder;
        return this;
    }

    /**
     * Display the rate dialog if needed.
     */
    public void init() {

        Log.d(TAG, "Init AppRate");

        if ((preferences.getBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, false)
                && preferences.getBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN_SOCIAL, false))
                || (preferences.getBoolean(PrefsContract.PREF_APP_HAS_CRASHED, false) && !showIfHasCrashed)) {
            return;
        }

        if (!showIfHasCrashed) {
            initExceptionHandler();
        }

        Editor editor = preferences.edit();

        // Get and increment launch counter.
        long launch_count = preferences.getLong(PrefsContract.PREF_LAUNCH_COUNT, 0) + 1;
        editor.putLong(PrefsContract.PREF_LAUNCH_COUNT, launch_count);

        // Get date of first launch.
        Long date_firstLaunch = preferences.getLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, date_firstLaunch);
        }

        // Show the rate or share dialog if needed, but not in the same time
        if (!preferences.getBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, false) && launch_count >= minLaunchesUntilPrompt) {
            if (System.currentTimeMillis() >= date_firstLaunch + (minDaysUntilPrompt * DateUtils.DAY_IN_MILLIS)) {

                mShowSocial = false;
                if (dialogBuilder != null) {
                    showDialog(dialogBuilder);
                } else {
                    showDefaultDialog();
                }
            }
        } else if (!preferences.getBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN_SOCIAL, false) && launch_count >= minLaunchesUntilSocial
                && (findFacebookClient() != null || findTwitterClient() != null)) {
            if (System.currentTimeMillis() >= date_firstLaunch + (minDaysUntilSocial * DateUtils.DAY_IN_MILLIS)) {

                mShowSocial = true;
                if (dialogBuilder != null) {
                    showDialog(dialogBuilder);
                } else {
                    showDefaultDialog();
                }
            }
        }

        editor.commit();
    }

    /**
     * Initialize the {@link ExceptionHandler}.
     */
    private void initExceptionHandler() {

        Log.d(TAG, "Init AppRate ExceptionHandler");

        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();

        // Don't register again if already registered.
        if (!(currentHandler instanceof ExceptionHandler)) {

            // Register default exceptions handler.
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(currentHandler, hostActivity));
        }
    }

    /**
     * Shows the default rate dialog.
     * @return
     */
    private void showDefaultDialog() {

        Log.d(TAG, "Create default dialog.");

        String title = (mShowSocial) ? String.format(hostActivity.getString(R.string.title_social), getApplicationName(hostActivity.getApplicationContext()))
                : String.format(hostActivity.getString(R.string.title), getApplicationName(hostActivity.getApplicationContext()));
        String message = (mShowSocial) ? String.format(hostActivity.getString(R.string.message_social), getApplicationName(hostActivity.getApplicationContext()))
                : String.format(hostActivity.getString(R.string.message), getApplicationName(hostActivity.getApplicationContext()));

        new AlertDialog.Builder(hostActivity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton((mShowSocial) ? hostActivity.getString(R.string.rate_social)
                        : hostActivity.getString(R.string.rate), this)
                .setNegativeButton(hostActivity.getString(R.string.nothanks), this)
                .setNeutralButton(hostActivity.getString(R.string.remindlater), this)
                .setOnCancelListener(this)
                .create().show();
    }

    /**
     * Show the custom rate dialog.
     * @return
     */
    private void showDialog(AlertDialog.Builder builder) {

        Log.d(TAG, "Create custom dialog.");

        AlertDialog dialog = builder.create();
        dialog.show();

        String rate = (String) dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText();
        String remindLater = (String) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getText();
        String dismiss = (String) dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText();

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, rate, this);
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, remindLater, this);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, dismiss, this);

        dialog.setOnCancelListener(this);
    }

    @Override
    public void onCancel(DialogInterface dialog) {

        Editor editor = preferences.edit();
        editor.putLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, System.currentTimeMillis());
        editor.putLong(PrefsContract.PREF_LAUNCH_COUNT, 0);
        editor.commit();
    }

    /**
     * @param onClickListener A listener to be called back on.
     * @return This {@link AppRate} object to allow chaining.
     */
    public AppRate setOnClickListener(OnClickListener onClickListener) {
        clickListener = onClickListener;
        return this;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        Editor editor = preferences.edit();

        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mShowSocial) {
                    Intent fbIntent = findFacebookClient();
                    Intent twIntent = findTwitterClient();
                    if (false && fbIntent != null && hostActivity instanceof IHasUiHelper) {
                        FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(hostActivity)
                                .setCaption("caption").setDescription("description").setName("wahh").setPicture("")
                                .setLink("https://play.google.com/store/apps/details?id=".concat(hostActivity.getPackageName()))
                                .build();
                        ((IHasUiHelper) hostActivity).getUiHelper().trackPendingDialogCall(shareDialog.present());
                    } else if (twIntent != null) {
                        twIntent.setType("text/plain");
                        twIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        twIntent.putExtra(Intent.EXTRA_TEXT, String.format(hostActivity.getString(R.string.recommend), getApplicationName(hostActivity.getApplicationContext()))
                                .concat(" ").concat("https://play.google.com/store/apps/details?id=").concat(hostActivity.getPackageName()));
                        hostActivity.startActivity(Intent.createChooser(twIntent, hostActivity.getString(R.string.rate_social)));
                    }
                    editor.putBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN_SOCIAL, true);
                } else {
                    try {
                        hostActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + hostActivity.getPackageName())));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(hostActivity, hostActivity.getString(R.string.noplay), Toast.LENGTH_SHORT).show();
                    }
                    editor.putBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, true);
                }
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                editor.putBoolean((mShowSocial) ? PrefsContract.PREF_DONT_SHOW_AGAIN_SOCIAL : PrefsContract.PREF_DONT_SHOW_AGAIN, true);
                break;

            case DialogInterface.BUTTON_NEUTRAL:
                editor.putLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, System.currentTimeMillis());
                editor.putLong(PrefsContract.PREF_LAUNCH_COUNT, 0);
                break;

            default:
                break;
        }

        editor.commit();
        dialog.dismiss();

        if (clickListener != null) {
            clickListener.onClick(dialog, which);
        }
    }

    public Intent findFacebookClient() {
        final String[] facebookApps = {
                "com.facebook.katana",
                "com.seesmic",
                "com.hootsuite.droid.full",
        };
        Intent fbIntent = new Intent();
        fbIntent.setType("text/plain");
        final PackageManager packageManager = hostActivity.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(
                fbIntent, PackageManager.MATCH_DEFAULT_ONLY);

        for (int i = 0; i < facebookApps.length; i++) {
            for (ResolveInfo resolveInfo : list) {
                String p = resolveInfo.activityInfo.packageName;
                if (p != null && p.startsWith(facebookApps[i])) {
                    fbIntent.setPackage(p);
                    return fbIntent;
                }
            }
        }
        return null;
    }

    public Intent findTwitterClient() {
        final String[] twitterApps = {
                "com.twitter.android",
                "com.handmark.tweetcaster.premium",
                "com.seesmic",
                "com.com.levelup.touiteur",
                "com.handmark.tweetcaster",
                "com.hootsuite.droid.full",
                "com.alphascope",
                "com.dotsandlines.carbon",
                "com.echofon",
                "com.jv.falcon",
                "com.tweetlanes.android",
        };
        Intent tweetIntent = new Intent();
        tweetIntent.setType("text/plain");
        final PackageManager packageManager = hostActivity.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(
                tweetIntent, PackageManager.MATCH_DEFAULT_ONLY);

        for (int i = 0; i < twitterApps.length; i++) {
            for (ResolveInfo resolveInfo : list) {
                String p = resolveInfo.activityInfo.packageName;
                if (p != null && p.startsWith(twitterApps[i])) {
                    tweetIntent.setPackage(p);
                    return tweetIntent;
                }
            }
        }
        return null;
    }
}