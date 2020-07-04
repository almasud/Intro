package com.almasud.intro.ui.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.almasud.intro.BaseApplication;
import com.almasud.intro.R;
import com.almasud.intro.databinding.ActivityLearnBinding;
import com.almasud.intro.model.entity.ArModel;
import com.almasud.intro.model.util.EventMessage;
import com.almasud.intro.ui.adapter.LearnFSAdapter;
import com.almasud.intro.ui.util.SnackbarHelper;
import com.almasud.intro.viewmodel.ArViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;

public class LearnActivity extends AppCompatActivity {
    private static final String TAG = LearnActivity.class.getSimpleName();
    private ActivityLearnBinding mViewBinding;
    private LearnFSAdapter mPagerAdapter;
    private List<ArModel> mArModels = new ArrayList<>();
    private static int MODEL_TYPE = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityLearnBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        // Get the bundle from intent if exists
        Bundle bundle = getIntent().getBundleExtra(BaseApplication.BUNDLE);
        if (bundle != null) {
            if (bundle.getString(BaseApplication.MODEL_TYPE).equals(BaseApplication.MODEL_ALPHABET))
                MODEL_TYPE = BaseApplication.ALPHABET;
            else if (bundle.getString(BaseApplication.MODEL_TYPE).equals(BaseApplication.MODEL_NUMBER))
                MODEL_TYPE = BaseApplication.NUMBER;
            else if (bundle.getString(BaseApplication.MODEL_TYPE).equals(BaseApplication.MODEL_ANIMAL))
                MODEL_TYPE = BaseApplication.ANIMAL;
        }

        // Set toolbar as an actionbar
        setSupportActionBar((Toolbar) mViewBinding.toolbarLearnAlphabet.getRoot());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set a subtitle of the actionbar
        if (MODEL_TYPE == BaseApplication.ALPHABET)
            getSupportActionBar().setSubtitle(new StringBuilder(
                    getResources().getString(R.string.learn)).append(" | ")
                    .append(getResources().getString(R.string.alphabet))
            );
        else if (MODEL_TYPE == BaseApplication.NUMBER)
            getSupportActionBar().setSubtitle(new StringBuilder(
                            getResources().getString(R.string.learn)).append(" | ")
                            .append(getResources().getString(R.string.number))
            );
        else if (MODEL_TYPE == BaseApplication.ANIMAL)
            getSupportActionBar().setSubtitle(new StringBuilder(
                            getResources().getString(R.string.learn)).append(" | ")
                            .append(getResources().getString(R.string.animal))
            );

        // Initialize the adapter
        mPagerAdapter = new LearnFSAdapter(this);
        // Set the adapter to view pager2
        mViewBinding.viewPagerLearn.setAdapter(mPagerAdapter);

        // Get an instance of ARViewModel
        ArViewModel arViewModel = new ViewModelProvider(this).get(ArViewModel.class);
        // Get the list of live data of ARModel from ARViewModel
        LiveData<List<ArModel>> arModelListLiveData = (MODEL_TYPE == BaseApplication.ALPHABET)?
                arViewModel.getAlphabetsLivedData(): (MODEL_TYPE == BaseApplication.NUMBER)?
                arViewModel.getNumbersLivedData(): arViewModel.getAnimalsLivedData();

        // Observe the list of ARModel from ARViewModel
        arModelListLiveData.observe(this, arModels -> {
            // Set the value of mARModels (list of ARModel)
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

                // Get the speak of different type of ArModel
                String speak = (MODEL_TYPE == BaseApplication.ALPHABET)?
                        mArModels.get(position).getName() + ", for, " + mArModels.get(position).getLabelName()
                                : mArModels.get(position).getName();
                // Speak the name of each item
                BaseApplication.speak(speak);

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

        // Download test Emulator - /storage/14EE-270F Real Device - /storage/6405-3F21
//        BaseApplication.download(
//                this,
//                BaseApplication.DOWNLOAD_URL_NUMBERS,
//                new File("/storage/6405-3F21/Android/data/com.almasud.intro/files/models")
//        );

        // Unzip test Emulator - /storage/14EE-270F Real Device - /storage/6405-3F21
//        BaseApplication.unzip(
//                this,
//                new File("/storage/14EE-270F/Android/data/com.almasud.intro/files/models/numbers.zip"),
//                new File("/storage/14EE-270F/Android/data/com.almasud.intro/files/models")
//        );
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
        // Get the type of ARModel to determine what type should be rendered in the real view.
        String modelType = (MODEL_TYPE == BaseApplication.ALPHABET)?
                BaseApplication.MODEL_ALPHABET: (MODEL_TYPE == BaseApplication.NUMBER)?
                BaseApplication.MODEL_NUMBER: BaseApplication.MODEL_ANIMAL;

        return Completable.create(emitter -> {
            try {
                // Check whether the sceneform is supported for this device or not
                // to avoid crashing the application.
                if (BaseApplication.isSupportedSceneformOrShowDialog(this)) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(BaseApplication.ITEM_LIST, (Serializable) mArModels);
                    bundle.putString(BaseApplication.MODEL_TYPE, modelType);
                    bundle.putInt(BaseApplication.SELECTED_ITEM, selectedItem);
                    BaseApplication.getInstance()
                            .startNewActivity(LearnActivity.this,
                                    LearnArActivity.class, bundle);
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
