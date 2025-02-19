package com.github.almasud.augmented_learn.view.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.github.almasud.augmented_learn.BaseApplication;
import com.github.almasud.augmented_learn.BuildConfig;
import com.github.almasud.augmented_learn.R;
import com.github.almasud.augmented_learn.databinding.AboutDialogBinding;
import com.github.almasud.augmented_learn.databinding.ActivityHomeBinding;
import com.github.almasud.augmented_learn.model.entity.App;
import com.github.almasud.augmented_learn.model.entity.Language;
import com.github.almasud.augmented_learn.model.util.EventMessage;
import com.github.almasud.augmented_learn.util.AppPreference;
import com.github.almasud.augmented_learn.view.util.SnackbarHelper;
import com.github.almasud.augmented_learn.viewmodel.AppVM;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private ActivityHomeBinding mViewBinding;
    private Animation mAnimation;
    private App mApp;
    private File mAppDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        // Set a toolbar as an actionbar
        setSupportActionBar(mViewBinding.toolbarHome.getRoot());
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setSubtitle(R.string.learn_with_reality);

        // Change the toolbar title and subtitle font
        BaseApplication.changeToolbarTitleFont(
                this, Language.ENGLISH, Typeface.NORMAL,
                mViewBinding.toolbarHome.getRoot()
        );

        // Add text view font according to language type
        BaseApplication.changeTextViewFont(
                this, Language.ENGLISH, Typeface.NORMAL,
                mViewBinding.tvHomeLearn, mViewBinding.tvHomeTest, mViewBinding.tvHomeScan
        );

        // Load the images using Glide to remove the unexpected white background for gif transparent images
        Glide.with(this).load(R.drawable.learn).into(mViewBinding.ivLearn);
        Glide.with(this).load(R.drawable.test).into(mViewBinding.ivTest);
        Glide.with(this).load(R.drawable.scan_book).into(mViewBinding.ivScan);

        // Load an animation for navigation items
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.click_item);
        // Load an existing App data
        mApp = AppPreference.getAppInfo(this);

//        // Set the directory to keep the updated app apk that will download from server
//        mAppDirectory = getExternalFilesDir(File.separator + App.DIRECTORY_APP);
//        // Create the directory if is not exist
//        if (!mAppDirectory.exists())
//            mAppDirectory.mkdirs();
//
//        if (mAppDirectory.isDirectory()) {
//            // Check whether the directory contains any item (old apk) or not, delete all if exists
//            if (mAppDirectory.listFiles().length >= 1) {
//                Log.d(TAG, "onCreate: " + mAppDirectory.getAbsolutePath() + " contains old file (s).");
//                // Delete the existing files from the directory
//                for (File file: mAppDirectory.listFiles()) {
//                    file.delete();
//                    Log.d(TAG, "onCreate: " + file.getName() + " is deleted.");
//                }
//            }
//        }

        // Check an update after launching the app
        if (BaseApplication.isInternetAvailable(this)) {
            // Get app data from server whether any update is available or not
            AppVM appVM = new ViewModelProvider(this).get(AppVM.class);
            LiveData<App> appLiveData = appVM.getAppLiveData();
            appLiveData.observe(this, app -> {
                // Store the value of App data.
                AppPreference.setAppInfo(this, app);
                // Show an update available alert dialog
                if (app.getVersionCode() > BuildConfig.VERSION_CODE)
                    BaseApplication.showUpdateAvailableWithDownloadOption(this, app, mAppDirectory);
            });
        } else {
            if (mApp != null) {
                // Show an update available dialog from stored App data
                if (mApp.getVersionCode() > BuildConfig.VERSION_CODE)
                    new Handler().postDelayed(
                            () -> BaseApplication.showUpdateAvailableWithDownloadOption(this, mApp, mAppDirectory),
                            1500
                    );
            }
        }

    }

    @Override
    public void onBackPressed() {
        BaseApplication.setAlertDialog(
                this, null, getResources().getString(R.string.action_choose),
                R.drawable.ic_help, getResources().getString(R.string.want_to_exit),
                () -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }, null, () ->{}, null
        );
    }

    /**
     * Event for each item clicked
     * @param view The view to click
     */
    public void onClickItem(View view) {
        // start an animation for each item
        switch (view.getId()) {
            case R.id.wrapperHomeLearn:
                mViewBinding.wrapperHomeLearn.startAnimation(mAnimation);
                break;
            case R.id.wrapperHomeTest:
                mViewBinding.wrapperHomeTest.startAnimation(mAnimation);
                break;
            case R.id.wrapperHomeScan:
                mViewBinding.wrapperHomeScan.startAnimation(mAnimation);
                break;
        }
        // Set an animation listener for each item
        mAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // To avoid the block of UI (main) thread execute the tasks within a new thread.
                new Handler().post(() -> {
                    Bundle bundle = new Bundle();
                    // Set an Activity for each item
                    switch (view.getId()) {
                        case R.id.wrapperHomeLearn:
                            bundle.putString(BaseApplication.SERVICE_NAME, BaseApplication.SERVICE_LEARN);
                            BaseApplication.getInstance()
                                    .startNewActivity(HomeActivity.this,
                                            SubjectChooseActivity.class, bundle
                                    );
                            break;
                        case R.id.wrapperHomeTest:
                            bundle.putString(BaseApplication.SERVICE_NAME, BaseApplication.SERVICE_TEST);
                            BaseApplication.getInstance()
                                    .startNewActivity(HomeActivity.this,
                                            SubjectChooseActivity.class, bundle
                                    );
                            break;
                        case R.id.wrapperHomeScan:
                            // Check whether the AR is supported for this device or not
                            // to avoid crashing the application.
                            if (BaseApplication.isSupportedAROrShowDialog(
                                    HomeActivity.this)) {
                                bundle.putString(BaseApplication.SERVICE_NAME, BaseApplication.SERVICE_SCAN);
                                BaseApplication.getInstance()
                                        .startNewActivity(HomeActivity.this,
                                                SubjectChooseActivity.class, bundle
                                        );
                            }
                            break;
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMessage(EventMessage eventMessage) {
        SnackbarHelper.getInstance().showMessage(this, eventMessage.getMessage());
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(
                        Intent.EXTRA_TEXT, "I am using the “" + getResources().getString(R.string.app_name) + "” application that is developed to provide interactive learning with Augmented Reality." +
                                "This application is AR-Optional that means designed for both non-AR and AR supported devices.\n\nYou can download this app from: " +
                                "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
                );
                startActivity(Intent.createChooser(
                        shareIntent, "Share the link of this application to..."
                ));
                return true;

            case R.id.action_check_update:
                if (BaseApplication.isInternetAvailable(this)) {
                    // Get app data from server whether any update is available or not
                    AppVM appVM = new ViewModelProvider(this).get(AppVM.class);
                    LiveData<App> appLiveData = appVM.getAppLiveData();
                    appLiveData.observe(this, app -> {
                        // Store the value of App data.
                        AppPreference.setAppInfo(this, app);
                        // Show an update available alert dialog otherwise show no update available message
                        if (app.getVersionCode() > BuildConfig.VERSION_CODE)
                            BaseApplication.showUpdateAvailableWithDownloadOption(this, app, mAppDirectory);
                        else
                            SnackbarHelper.getInstance().showMessageWithDismiss(this, getResources().getString(R.string.not_update_available));
                    });
                } else {
                    SnackbarHelper.getInstance().showMessageWithDismiss(this, getResources().getString(R.string.internet_not_available));
                }
                return true;

            case R.id.action_rate_us:
                final String appPackageName = BuildConfig.APPLICATION_ID;
                try {
                    startActivity(
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=" + appPackageName)
                            )
                    );
                } catch (ActivityNotFoundException ex) {
                    startActivity(
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)
                            )
                    );
                }
                return true;

            case R.id.action_report:
                Intent reportEmail = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                "mailto", getResources().getString(R.string.report_email), null
                ));
                reportEmail.putExtra(
                        Intent.EXTRA_SUBJECT, "Report for \"" + getResources().getString(R.string.app_name) + "\" app: "
                );
                startActivity(Intent.createChooser(
                        reportEmail, "Send an email to report a problem"
                ));
                return true;

            case R.id.action_about:
                AboutDialogBinding aboutDialogBinding = AboutDialogBinding.inflate(LayoutInflater.from(this));
                aboutDialogBinding.coverPhotoAboutDialog.setImageResource(R.drawable.app_logo);
                aboutDialogBinding.titleAboutDialog.setText(String.format("%s (version %s)", getResources().getString(R.string.app_name), BuildConfig.VERSION_NAME));
                // Load content text from assets
                try {
                    InputStream inputStream = getAssets().open("about_content.txt");
                    int size = inputStream.available();
                    byte[] buffer = new byte[size];
                    inputStream.read(buffer);
                    inputStream.close();
                    // Set the text into UI
                    aboutDialogBinding.contentAboutDialog.setText(new String(buffer));
                    // Change the font of text view
                    BaseApplication.changeTextViewFont(
                            this, Language.BENGALI, Typeface.NORMAL, aboutDialogBinding.contentAboutDialog
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                aboutDialogBinding.developerNameAboutDialog.setText(getResources().getString(R.string.developer_name));
                aboutDialogBinding.developerNameAboutDialog.setOnClickListener(view -> {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW);
                    webIntent.setDataAndType(Uri.parse(getResources().getString(R.string.developer_website)), "text/plain");
                    startActivity(Intent.createChooser(
                            webIntent, "Visit developer website"
                    ));
                });
                // Change the font of text view (s)
                BaseApplication.changeTextViewFont(
                        this, Language.ENGLISH, Typeface.NORMAL,
                        aboutDialogBinding.titleAboutDialog, aboutDialogBinding.developerNameAboutDialog
                );

                // Set the custom view into alert dialog
                BaseApplication.setAlertDialog(
                        this, aboutDialogBinding.getRoot(), null, -1,
                        null, () -> {}, getResources().getString(R.string.action_close)
                );
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
