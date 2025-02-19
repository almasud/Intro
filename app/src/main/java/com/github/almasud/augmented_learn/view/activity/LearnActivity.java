package com.github.almasud.augmented_learn.view.activity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.github.almasud.augmented_learn.BaseApplication;
import com.github.almasud.augmented_learn.R;
import com.github.almasud.augmented_learn.databinding.ActivityLearnBinding;
import com.github.almasud.augmented_learn.model.entity.ArModel;
import com.github.almasud.augmented_learn.model.entity.Subject;
import com.github.almasud.augmented_learn.model.util.EventMessage;
import com.github.almasud.augmented_learn.util.AppResource;
import com.github.almasud.augmented_learn.view.adapter.LearnFSAdapter;
import com.github.almasud.augmented_learn.view.util.SnackbarHelper;
import com.github.almasud.augmented_learn.viewmodel.ArVM;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Completable;

public class LearnActivity extends AppCompatActivity {
    private static final String TAG = "LearnActivity";
    private ActivityLearnBinding mViewBinding;
    private LearnFSAdapter mPagerAdapter;
    private List<ArModel> mArModels = new ArrayList<>();
    // Declare static to hold the bundle data for starting a new screen with the bundle
    private static Bundle sBundle;
    private Subject mSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityLearnBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        // Get the bundle from intent if exists
       if (getIntent().getBundleExtra(BaseApplication.BUNDLE) != null) {
           sBundle = getIntent().getBundleExtra(BaseApplication.BUNDLE);
           mSubject = ((Subject) getIntent().getBundleExtra(BaseApplication.BUNDLE).getSerializable(ArModel.SUBJECT));
       }

        // Set toolbar as an actionbar
        setSupportActionBar(mViewBinding.toolbarLearn.getRoot());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set a subtitle of the actionbar
        getSupportActionBar().setSubtitle(
                getResources().getString(R.string.learn) + " | " + mSubject.getName()
        );
        // Change the toolbar title and subtitle font
        BaseApplication.changeToolbarTitleFont(
                this, mSubject.getLanguage().getId(),
                Typeface.NORMAL, mViewBinding.toolbarLearn.getRoot()
        );

        // Initialize the adapter
        mPagerAdapter = new LearnFSAdapter(this);
        // Set the adapter to view pager2
        mViewBinding.viewPagerLearn.setAdapter(mPagerAdapter);

        // Get an instance of ViewModel
        ArVM arVM = new ViewModelProvider(this).get(ArVM.class);
        // Get the list of live data of ArModel from ArViewModel
        LiveData<List<ArModel>> arModelsLiveData = arVM.getArModelsLivedData(mSubject.getId());
        // Observe the list of ArModel from ARViewModel
        arModelsLiveData.observe(this, arModels -> {
            // Set the value of mArModels (list of ARModel)
            mArModels = arModels;

            if (mArModels.size() > 0) {
                // Add the items (ArModel(s)) to the view pager2 adapter
                mPagerAdapter.addArModels(mArModels);

                // The previous button should be hidden for the first time
                mViewBinding.buttonPrevLearn.setVisibility(View.GONE);
                // The next button should be hidden while the size of items is less than or equal to 1
                if (mViewBinding.viewPagerLearn.getAdapter().getItemCount() <= 1)
                    mViewBinding.buttonNextLearn.setVisibility(View.GONE);
            }
        });

        // Set the previous button action
        mViewBinding.buttonPrevLearn.setOnClickListener(view ->
                mViewBinding.viewPagerLearn.setCurrentItem(
                        mViewBinding.viewPagerLearn.getCurrentItem() - 1, true));
        // Set the next button action
        mViewBinding.buttonNextLearn.setOnClickListener(view ->
                mViewBinding.viewPagerLearn.setCurrentItem(
                        mViewBinding.viewPagerLearn.getCurrentItem() + 1, true));

        // Set the vew page changed listener
        mViewBinding.viewPagerLearn.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Set whether the voice play form extra of the item or not
                boolean isExtraPlay = (mArModels.get(position).getExtraName() != null);
                // Play the voice of each item
                BaseApplication.playVoice(mArModels.get(position), isExtraPlay);

                // Set the visibility of the next and previous button.
                // For previous button
                if (position == 0)
                    mViewBinding.buttonPrevLearn.setVisibility(View.GONE);
                else
                    mViewBinding.buttonPrevLearn.setVisibility(View.VISIBLE);
                // For next button
                if (position == mViewBinding.viewPagerLearn.getAdapter().getItemCount() - 1)
                    mViewBinding.buttonNextLearn.setVisibility(View.GONE);
                else
                    mViewBinding.buttonNextLearn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * The callback method for going to the {@link LearnArActivity} (Augmented reality view) if
     * the device is supported otherwise show a dialog for not supported.
     * @param selectedItem The {@link ArModel} item to be selected in {@link LearnArActivity}.
     * @return A {@link Completable} observable to it's subscribers.
     */
    public Completable getArViewCallback(int selectedItem) {
        return Completable.create(emitter -> {
            try {
                // Check whether the sceneform is supported for this device or not
                // to avoid crashing the application.
                if (BaseApplication.isSupportedAROrShowDialog(this)) {
                    File downloadDirectory = getExternalFilesDir(
                            File.separator + AppResource.DIRECTORY_MODELS_ROOT
                    );
                    File modelDirectory = getExternalFilesDir(
                            File.separator + AppResource.getModelUri(mSubject.getId()).getPath()
                    );
                    Log.d(TAG, "getArViewCallback: modelDirectory: " + modelDirectory.getAbsolutePath());
                    Log.d(TAG, "getArViewCallback: modelDirectory list: "+ Arrays.asList(modelDirectory.list()));

                    // Check whether the model directory contains any item or not
                    if (modelDirectory.listFiles().length > 1) {
                        sBundle.putSerializable(ArModel.LIST_ITEM, (Serializable) mArModels);
                        sBundle.putInt(ArModel.SELECTED_ITEM, selectedItem);
                        BaseApplication.getInstance()
                                .startNewActivity(
                                        LearnActivity.this, LearnArActivity.class, sBundle
                                );
                    } else {
                        // If the model directory not contains any item
                        String downloadURL = mArModels.get(selectedItem).getSubject().getModelURL();
                        BaseApplication.setAlertDialog(
                                LearnActivity.this, null, getResources().getString(R.string.action_choose),
                                R.drawable.ic_help, getResources().getString(R.string.need_download_models),
                                () -> {
                                    if (downloadURL != null) {
                                        BaseApplication.download(
                                                LearnActivity.this, downloadURL, downloadDirectory
                                        );
                                    }
                                }, null, () -> {}, null
                        );
                    }
                }
                // Signal the subscribers for completing the task
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
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
}
