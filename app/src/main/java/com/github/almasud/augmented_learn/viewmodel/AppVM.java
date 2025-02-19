package com.github.almasud.augmented_learn.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.almasud.augmented_learn.api.ApiClient;
import com.github.almasud.augmented_learn.api.request.AppRequest;
import com.github.almasud.augmented_learn.model.entity.App;
import com.github.almasud.augmented_learn.model.util.EventMessage;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * An {@link AndroidViewModel} class for the {@link App}.
 *
 * @author Abdullah Almasud
 */
public class AppVM extends AndroidViewModel {
    private MutableLiveData<App> mAppMutableLiveData;

    public AppVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<App> getAppLiveData() {
        if (mAppMutableLiveData == null) {
            mAppMutableLiveData = new MutableLiveData<>();
            ApiClient.getClient().create(AppRequest.class)
                    .getAppInfo()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<App>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(App app) {
                            // Set the value of MutableLiveData
                            mAppMutableLiveData.setValue(app);
                        }

                        @Override
                        public void onError(Throwable e) {
                            mAppMutableLiveData = null;
                            // Dispatch an EventMessage to it's subscribers
                            EventBus.getDefault().post(
                                    new EventMessage(
                                            "Failed to establish connection! Couldn't fetch the app information.",
                                            EventMessage.TYPE_ERROR
                                    )
                            );
                        }
                    });
        }
        return mAppMutableLiveData;
    }
}
