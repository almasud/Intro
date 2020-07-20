package com.almasud.intro;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.almasud.intro.model.entity.ArModel;
import com.almasud.intro.model.entity.Voice;
import com.almasud.intro.service.DownloadService;
import com.almasud.intro.service.UnzipService;
import com.almasud.intro.util.OnSingleAction;
import com.google.ar.core.ArCoreApk;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * A base {@link Application} class of the app.
 *
 * @author Abdullah Almasud
 */
public class BaseApplication extends Application implements LifecycleObserver {
    public static final String BUNDLE = "Bundle";
    public static final String SERVICE_NAME = "Service_Name";
    public static final String SERVICE_LEARN = "Learn";
    public static final String SERVICE_TEST = "Test";
    public static final String SERVICE_SCAN = "Scan";
    public static final int LEARN = 0;
    public static final int TEST = 1;
    public static final int SCAN = 2;
    public static final String DIRECTORY_MODELS = "models";
    public static final String DIRECTORY_VOWELS_BENGALI = "vowels_bengali_with_extra";
    public static final String DIRECTORY_ALPHABETS_BENGALI = "alphabets_bengali_with_extra";
    public static final String DIRECTORY_NUMBERS_BENGALI = "numbers_bengali";
    public static final String DIRECTORY_ALPHABETS_ENGLISH = "alphabets";
    public static final String DIRECTORY_NUMBERS_ENGLISH = "numbers";
    public static final String DIRECTORY_ANIMALS_ENGLISH = "animals";
    public static final String DOWNLOAD_URL_VOWELS_BENGALI = "http://almasud.000webhostapp.com/download/vowels_bengali.zip";
    public static final String DOWNLOAD_URL_ALPHABETS_BENGALI = "http://almasud.000webhostapp.com/download/alphabets_bengali.zip";
    public static final String DOWNLOAD_URL_NUMBERS_BENGALI = "http://almasud.000webhostapp.com/download/numbers_bengali.zip";
    public static final String DOWNLOAD_URL_ALPHABETS_ENGLISH = "http://almasud.000webhostapp.com/download/alphabets.zip";
    public static final String DOWNLOAD_URL_NUMBERS_ENGLISH = "http://almasud.000webhostapp.com/download/numbers.zip";
    public static final String DOWNLOAD_URL_ANIMALS_ENGLISH = "http://almasud.000webhostapp.com/download/animals.zip";

    private static final double MIN_OPEN_GL_VERSION = 3.0;
    public static final String NOTIFICATION_CHANNEL_DOWNLOADER = "Downloader_Channel";
    public static final String NOTIFICATION_CHANNEL_UNZIP = "Unzip_Channel";

    private static final String TAG = BaseApplication.class.getSimpleName();
    private static final BaseApplication INSTANCE = new BaseApplication();
    private static AppVisibilityListener sAppVisibilityListener;
    private static volatile TextToSpeech sTTS;
    private static MediaPlayer sMediaVowelsBengali, sMediaVowelsBengaliWithExtra,
            sMediaAlphabetsBengali, sMediaAlphabetsBengaliWithExtra, sMediaNumbersBengali,
            sMediaAlphabetsEnglish, sMediaAlphabetsEnglishWithExtra, sMediaNumbersEnglish,
            sMediaAnimalsEnglish;

    @Override
    public void onCreate() {
        super.onCreate();

        // Add observer
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // Create downloader and unzip notification channel
        createDownloadNotificationChannel();
        createUnzipNotificationChannel();
    }

    /**
     * Used to get the instance of {@link BaseApplication}.
     * @return The instance of {@link BaseApplication}.
     */
    public static BaseApplication getInstance() {
        return INSTANCE;
    }

    /**
     * A listener for the visibility of the {@link Application}.
     */
    public interface AppVisibilityListener {
        /**
         * Used to determine the visibility of the {@link Application}.
         * @param isBackground true if the {@link Application} is in background otherwise false.
         */
        void onAppVisibility(boolean isBackground);
    }

    /**
     * Used to set whether the app is in background or not.
     */
    private void setAppInBackground(boolean isBackground) {
        if (sAppVisibilityListener != null)
            sAppVisibilityListener.onAppVisibility(isBackground);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        Log.d(TAG, "onEnterForeground: The app is in foreground.");
        setAppInBackground(false);

        // Create media players for Vowels, Alphabets, Numbers and Animals
        sMediaVowelsBengali = MediaPlayer.create(
                getApplicationContext(), R.raw.vowels_bengali
        );
        sMediaVowelsBengaliWithExtra = MediaPlayer.create(
                getApplicationContext(), R.raw.vowels_bengali_with_extra
        );

        sMediaAlphabetsBengali = MediaPlayer.create(
                getApplicationContext(), R.raw.alphabets_bengali
        );
        sMediaAlphabetsBengaliWithExtra = MediaPlayer.create(
                getApplicationContext(), R.raw.alphabets_bengali_with_extra
        );

        sMediaNumbersBengali = MediaPlayer.create(
                getApplicationContext(), R.raw.numbers_bengali
        );

        sMediaAlphabetsEnglish = MediaPlayer.create(
                getApplicationContext(), R.raw.alphabets_english
        );
        sMediaAlphabetsEnglishWithExtra = MediaPlayer.create(
                getApplicationContext(), R.raw.alphabets_english_with_extra
        );

        sMediaNumbersEnglish = MediaPlayer.create(
                getApplicationContext(), R.raw.numbers_english
        );

        sMediaAnimalsEnglish = MediaPlayer.create(
                getApplicationContext(), R.raw.animals_english
        );
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        Log.d(TAG, "onEnterBackground: The app is in background.");
        setAppInBackground(true);

        // Stop the media players if playing
        if (sMediaVowelsBengali.isPlaying())
            sMediaVowelsBengali.stop();
        if (sMediaVowelsBengaliWithExtra.isPlaying())
            sMediaVowelsBengaliWithExtra.stop();

        if (sMediaAlphabetsBengali.isPlaying())
            sMediaAlphabetsBengali.stop();
        if (sMediaAlphabetsBengaliWithExtra.isPlaying())
            sMediaAlphabetsBengaliWithExtra.stop();

        if (sMediaNumbersBengali.isPlaying())
            sMediaNumbersBengali.stop();

        if (sMediaAlphabetsEnglish.isPlaying())
            sMediaAlphabetsEnglish.stop();
        if (sMediaAlphabetsEnglishWithExtra.isPlaying())
            sMediaAlphabetsEnglishWithExtra.stop();

        if (sMediaNumbersEnglish.isPlaying())
            sMediaNumbersEnglish.stop();

        if (sMediaAnimalsEnglish.isPlaying())
            sMediaAnimalsEnglish.stop();

        // Release the medial players
        sMediaVowelsBengali.release();
        sMediaVowelsBengaliWithExtra.release();

        sMediaAlphabetsBengali.release();
        sMediaAlphabetsBengaliWithExtra.release();

        sMediaNumbersBengali.release();

        sMediaAlphabetsEnglish.release();
        sMediaAlphabetsEnglishWithExtra.release();

        sMediaNumbersEnglish.release();

        sMediaAnimalsEnglish.release();
    }

    /**
     * Used to set an {@link AppVisibilityListener}.
     * @param listener An {@link AppVisibilityListener}.
     */
    public void setOnAppVisibilityListener(AppVisibilityListener listener) {
        sAppVisibilityListener = listener;
    }

    /**
     * Start an activity with a new task.
     * @param activity An instance of the {@link Activity} where start from.
     * @param destination A {@link Class} of an {@link Activity} to be started.
     * @param bundle The bundle to be send with the {@link Intent}.
     */
    public void startNewActivity(@NonNull Activity activity,
                                 @NonNull Class destination, @Nullable Bundle bundle) {
        Intent intent = new Intent(activity, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (bundle != null)
            intent.putExtra(BUNDLE, bundle);
        activity.startActivity(intent);
    }

    /**
     * Set an alert dialog with only a positive button to inform a user.
     * @param activity An instance of the {@link Activity} where the function to be called.
     * @param title A {@link String} for {@link AlertDialog} title.
     * @param iconRes A {@link DrawableRes} for {@link AlertDialog} icon.
     * @param message A {@link String} for {@link AlertDialog} message.
     * @param positiveButtonAction An {@link OnSingleAction} for positive button of {@link AlertDialog}.
     * @param positiveButtonText A {@link String} for positive button
     */
    public static void setAlertDialog(
            Activity activity, String title, int iconRes, String message,
            OnSingleAction positiveButtonAction, String positiveButtonText) {

        // Set only required parameter to the setAlertDialog
        setAlertDialog(
                activity, title, iconRes, message,
                positiveButtonAction, positiveButtonText,
                null, null,
                null, null
        );
    }

    /**
     * Set an alert dialog with positive and negative button to get an action form the user.
     * @param activity An instance of the {@link Activity} where the function to be called.
     * @param title A {@link String} for {@link AlertDialog} title.
     * @param iconRes A {@link DrawableRes} for {@link AlertDialog} icon.
     * @param message A {@link String} for {@link AlertDialog} message.
     * @param positiveButtonAction An {@link OnSingleAction} for positive button of {@link AlertDialog}.
     * @param positiveButtonText A {@link String} for positive button
     * @param negativeButtonAction An {@link OnSingleAction} for negative button of {@link AlertDialog}.
     * @param negativeButtonText A {@link String} for negative button
     */
    public static void setAlertDialog(
            Activity activity, String title, int iconRes, String message,
            OnSingleAction positiveButtonAction, String positiveButtonText,
            OnSingleAction negativeButtonAction, String negativeButtonText) {

        // Set only required parameter to the setAlertDialog
        setAlertDialog(
                activity, title, iconRes, message,
                positiveButtonAction, positiveButtonText,
                negativeButtonAction, negativeButtonText,
                null, null
        );
    }

    /**
     * Set an alert dialog with positive, negative and neutral button to get an action form the user.
     * @param activity An instance of the {@link Activity} where the function to be called.
     * @param title A {@link String} for {@link AlertDialog} title.
     * @param iconRes A {@link DrawableRes} for {@link AlertDialog} icon.
     * @param message A {@link String} for {@link AlertDialog} message.
     * @param positiveButtonAction An {@link OnSingleAction} for positive button of {@link AlertDialog}.
     * @param positiveButtonText A {@link String} for positive button
     * @param negativeButtonAction An {@link OnSingleAction} for negative button of {@link AlertDialog}.
     * @param negativeButtonText A {@link String} for negative button
     * @param neutralButtonAction An {@link OnSingleAction} for neutral button of {@link AlertDialog}.
     * @param neutralButtonText A {@link String} for neutral button
     */
    public static void setAlertDialog(
            Activity activity, String title, int iconRes, String message,
            OnSingleAction positiveButtonAction, String positiveButtonText,
            OnSingleAction negativeButtonAction, String negativeButtonText,
            OnSingleAction neutralButtonAction, String neutralButtonText) {

        // Create an alert dialog to show a dialog message
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setTitle(title);
        dialogBuilder.setIcon(iconRes);
        dialogBuilder.setMessage(message);

        // Set an action for positive button
        if (positiveButtonAction != null) {
            dialogBuilder.setPositiveButton(
                    (positiveButtonText != null)? positiveButtonText
                            : activity.getResources().getString(R.string.yes),
                    (dialog, which) -> positiveButtonAction.onAction()
            );
        }

        // Set an action for negative button
        if (negativeButtonAction != null) {
            dialogBuilder.setNegativeButton(
                    (negativeButtonText != null)? negativeButtonText
                            : activity.getResources().getString(R.string.no),
                    (dialog, which) -> negativeButtonAction.onAction()
            );
        }

        // Set an action for neutral button
        if (neutralButtonAction != null) {
            dialogBuilder.setNeutralButton(
                    (neutralButtonText != null)? neutralButtonText
                            : activity.getResources().getString(R.string.not_sure),
                    (dialog, which) -> neutralButtonAction.onAction()
            );
        }

        // To avoid the block of UI (main) thread execute the task within a new thread.
        new Handler().post(() -> {
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();

            // This line always placed after the dialog.show() otherwise get a Null Pinter Exception.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setAllCaps(false);
        });
    }

    /**
     * Check whether the AR is supported for this device or not. Show a dialog message
     * if not supported.
     * @param activity An instance of the {@link Activity} where the function to be called.
     * @return true if AR is supported for this device otherwise false.
     */
    public static boolean isSupportedAROrShowDialog(@NonNull Activity activity) {
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        Log.i(TAG, "OpenGL ES Version: " + openGlVersionString);

        // Check for Sceneform support
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                || (Double.parseDouble(openGlVersionString) < MIN_OPEN_GL_VERSION)) {

            // Set an alert dialog to inform
            setAlertDialog(
                    activity, activity.getResources().getString(R.string.opps),
                    R.drawable.ic_sentiment_dissatisfied_black,
                    activity.getResources().getString(R.string.not_supported_sceneform),
                    () -> Toast.makeText(
                            activity,
                            activity.getResources().getString(R.string.hope_understand),
                            Toast.LENGTH_SHORT
                    ).show(), activity.getResources().getString(R.string.okay_understand)
            );

        } // Check for ArCore support
        else if (!ArCoreApk.getInstance().checkAvailability(activity).isSupported()) {

            // Set an alert dialog
            setAlertDialog(
                    activity, activity.getResources().getString(R.string.action_choose),
                    R.drawable.ic_help_gray,
                    activity.getResources().getString(R.string.not_supported_ar_core),
                    () -> {
                        final String appPackageName = "com.google.ar.core";
                        try {
                            activity.startActivity(
                                    new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=" + appPackageName)
                                    )
                            );
                        } catch (ActivityNotFoundException ex) {
                            activity.startActivity(
                                    new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)
                                    )
                            );
                        }
                    }, null, () -> {}, null);
        }

        return !((Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                || (Double.parseDouble(openGlVersionString) < MIN_OPEN_GL_VERSION)
                || (!ArCoreApk.getInstance().checkAvailability(activity).isSupported()));
    }

    /**
     * Generate an {@link Integer} array of a given number of unique {@link Random}
     * number from a given bound number.
     * @param boundNumber The number to be bounded for generating {@link Random} number.
     * @param totalNumber The number of array size to generated.
     * @return An {@link Integer} array of unique {@link Random} numbers.
     */
    public static int[] getUniqueRandNumbers(int boundNumber, int totalNumber) {
        // Random set to hold unique random number.
        Set<Integer> randSet = new HashSet<>(totalNumber);
        // Add resultSize of random numbers to set
        while (randSet.size() < totalNumber)
            while (!randSet.add(new Random().nextInt(boundNumber)));
        // Convert the randSet into an integer array to return
        int[] randArray = new int[totalNumber];
        int i = 0;
        for (Integer integer : randSet)
            randArray[i++] = integer;
        return randArray;
    }

    /**
     * Play a {@link Voice} from a given {@link ArModel}.
     * @param arModel An instance of {@link ArModel} to be played.
     * @param isExtraPlay true if the {@link Voice} to be played form extra of
     * {@link ArModel} otherwise false.
     */
    public static void playVoice(ArModel arModel, boolean isExtraPlay) {
        int start = isExtraPlay?
                arModel.getVoice().getExtraStart(): arModel.getVoice().getStart();
        int end = isExtraPlay?
                arModel.getVoice().getExtraEnd(): arModel.getVoice().getEnd();

        // Check whether the end time is grater than start or not
        if (end > start) {
            new Thread(() -> {
                // Determine which media player need to play
                MediaPlayer[] mediaPlayer = new MediaPlayer[1];
                if (!isExtraPlay) {
                    switch (arModel.getVoice().getId()) {
                        case Voice.VOICE_VOWELS_BENGALI:
                            mediaPlayer[0] = sMediaVowelsBengali;
                            Log.d(TAG, "playVoice: playing vowels bengali");
                            break;
                        case Voice.VOICE_ALPHABETS_BENGALI:
                            mediaPlayer[0] = sMediaAlphabetsBengali;
                            Log.d(TAG, "playVoice: playing alphabets bengali");
                            break;
                        case Voice.VOICE_NUMBERS_BENGALI:
                            mediaPlayer[0] = sMediaNumbersBengali;
                            Log.d(TAG, "playVoice: playing numbers bengali");
                            break;
                        case Voice.VOICE_ALPHABETS_ENGLISH:
                            mediaPlayer[0] = sMediaAlphabetsEnglish;
                            Log.d(TAG, "playVoice: playing alphabets english");
                            break;
                        case Voice.VOICE_NUMBERS_ENGLISH:
                            mediaPlayer[0] = sMediaNumbersEnglish;
                            Log.d(TAG, "playVoice: playing numbers english");
                            break;
                        case Voice.VOICE_ANIMALS_ENGLISH:
                            mediaPlayer[0] = sMediaAnimalsEnglish;
                            Log.d(TAG, "playVoice: playing animals english");
                            break;
                    }
                 } else {
                    switch (arModel.getVoice().getId()) {
                        case Voice.VOICE_VOWELS_BENGALI:
                            mediaPlayer[0] = sMediaVowelsBengaliWithExtra;
                            Log.d(TAG, "playVoice: playing vowels bengali with extra");
                            break;
                        case Voice.VOICE_ALPHABETS_BENGALI:
                            mediaPlayer[0] = sMediaAlphabetsBengaliWithExtra;
                            Log.d(TAG, "playVoice: playing alphabets bengali with extra");
                            break;
                        case Voice.VOICE_ALPHABETS_ENGLISH:
                            mediaPlayer[0] = sMediaAlphabetsEnglishWithExtra;
                            Log.d(TAG, "playVoice: playing vowels bengali with extra");
                            break;
                    }
                }

                // Start and pause the media player according to start and end time
                if (mediaPlayer[0] != null) {
                    if (!mediaPlayer[0].isPlaying()) {
                        mediaPlayer[0].seekTo(start);
                        mediaPlayer[0].start();
                        try {
                            Thread.sleep((end - start));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mediaPlayer[0].pause();
                    }
                }
            }).start();
        } else {
            Log.e(TAG, "playVoice: end must be grater than start time");
        }
    }

    /**
     * Load a TTS ({@link TextToSpeech}) engine (TTS engine initialization takes some times) with thread safe.
     * @param context The {@link Context} of {@link Application}.
     */
    private static void loadTTSEngine(@NonNull Context context) {
        synchronized (BaseApplication.class) {
            try {
                sTTS = new TextToSpeech(context, status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        int ttsLang = sTTS.setLanguage(new Locale("EN_US"));
                        if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                                || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e(TAG, "Language is not supported.");
                            Toast.makeText(context, "TTS language is not supported to your device.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i(TAG, "Language is supported.");
                        }
                        Log.i(TAG, "TTS initialization success.");

                    } else {
                        Log.e(TAG, "TTS initialization failed.");
                        Toast.makeText(context, "Couldn't setup TTS engine to your device.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "loadTTSEngine: Exception occurred while trying to load TTS engine");
            }
        }
    }

    private static void unloadTTSEngine() {
        // Close the Text To Speech
        if (sTTS != null) {
            sTTS.stop();
            sTTS.shutdown();
            Log.i(TAG, "TTS shutdown successful.");
        }
    }

    /**
     * Check whether the TTS {@link TextToSpeech} engine is loaded or not.
     * @return true if the {@link TextToSpeech} is loaded otherwise false.
     */
    public static boolean isTTSEngineLoaded() {
        return sTTS != null;
    }

    /**
     * The custom speak() method of {@link TextToSpeech}.
     * @param text A {@link String} to be spoken.
     */
    public static void speak(@NonNull String text) {
        if (isTTSEngineLoaded()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                if (sTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) == TextToSpeech.ERROR)
                    Log.e(TAG, "Error in converting text to speech.");
            } else {
                if (sTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null) == TextToSpeech.ERROR)
                    Log.e(TAG, "Error in converting text to speech.");
            }
        } else {
            Log.e(TAG, "speak: The TTS engine is not loaded yet!");
        }
    }

    /**
     * check whether the external storage is available for write (and read) or not.
     * @return true if the external storage is writable otherwise false.
     */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * @return A {@link File} directory of {@link ArModel}s.
     */
    public static File getExternalFileModelsDir(@NonNull Context context, @NonNull String directory) {
        return context.getExternalFilesDir(
                File.separator + DIRECTORY_MODELS + File.separator + directory
        );
    }

    private void createDownloadNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_DOWNLOADER, getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationChannel.setLightColor(Color.GREEN);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                Log.d(TAG, "createDownloaderNotificationChannel: Notification channel created.");
            }
        }
    }

    private void createUnzipNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_UNZIP, getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationChannel.setLightColor(Color.GREEN);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                Log.d(TAG, "createUnzipNotificationChannel: Notification channel created.");
            }
        }
    }

    /**
     * Unzip a given zip {@link File} into a given target directory.
     * @param context The context of {@link Application}.
     * @param zipFile A zip {@link File} to be extracted.
     * @param targetDirectory A target directory {@link File} where extracted files to be placed.
     */
    public static void unzip(
            @NonNull Context context, @NonNull File zipFile, @NonNull File targetDirectory) {
        // Start unzip service to unzip the file
        Intent unzipServiceIntent = new Intent(context, UnzipService.class);
        unzipServiceIntent.putExtra(UnzipService.ZIP_FILE, zipFile);
        unzipServiceIntent.putExtra(UnzipService.TARGET_DIRECTORY, targetDirectory);

        // Start the service as foreground
        ContextCompat.startForegroundService(context, unzipServiceIntent);

        // Use the code for unzip where needed
//        BaseApplication.unzip(
//                this,
//                new File("/storage/14EE-270F/Android/data/com.almasud.intro/files/models/numbers.zip"),
//                new File("/storage/14EE-270F/Android/data/com.almasud.intro/files/models")
//        );
    }

    /**
     * Download a {@link File} from a given {@link URL} and unzip if downloaded file is zip
     * to a given target {@link File} directory.
     * @param context The context of {@link Application}.
     * @param downloadURL An {@link URL} to be download from.
     * @param targetDirectory A {@link File} directory to be placed the downloaded {@link File}.
     */
    public static void download(
            @NonNull Context context, @NonNull final String downloadURL, @NonNull File targetDirectory) {
        // Start download service to download the file
        Intent downloadServiceIntent = new Intent(context, DownloadService.class);
        downloadServiceIntent.putExtra(DownloadService.DOWNLOAD_URL, downloadURL);
        downloadServiceIntent.putExtra(DownloadService.TARGET_DIRECTORY, targetDirectory);

        // Start the service as foreground
        ContextCompat.startForegroundService(context, downloadServiceIntent);

        // Use the code for download where needed
//        BaseApplication.download(
//                this,
//                BaseApplication.DOWNLOAD_URL_NUMBERS,
//                new File("/storage/6405-3F21/Android/data/com.almasud.intro/files/models")
//        );
    }
}
