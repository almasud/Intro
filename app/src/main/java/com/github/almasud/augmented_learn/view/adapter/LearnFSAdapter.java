package com.github.almasud.augmented_learn.view.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.github.almasud.augmented_learn.model.entity.ArModel;
import com.github.almasud.augmented_learn.view.fragment.LearnFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * The adapter subclass of {@link FragmentStateAdapter} for {@link LearnFragment}
 *
 * @author Abdullah Almasud
 */
public class LearnFSAdapter extends FragmentStateAdapter {
    private List<Fragment> mFragments = new ArrayList<>();

    public LearnFSAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return mFragments.size();
    }

    /**
     * Create a {@link Fragment} for each {@link ArModel}.
     * @param ArModels The list of {@link ArModel}s.
     */
    public void addArModels(List<ArModel> ArModels) {
        for (ArModel ARModel : ArModels) {
            mFragments.add(new LearnFragment(ARModel));
        }
    }
}
