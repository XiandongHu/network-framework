package com.example.huxiandong.network.api;

import android.os.Looper;

import com.example.huxiandong.network.api.model.BaseResponse;

import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by huxiandong
 * on 17-2-23.
 */

public class ApiRequest<T extends BaseResponse> {

    public interface Listener<T> {
        void onSuccess(T response);

        void onFailure(Throwable e);
    }

    private Listener<T> mListener;

    private Observable<Response<T>> mObservable;
    private Subscription mSubscription;
    private volatile boolean mCanceled;

    public ApiRequest(Listener<T> listener) {
        mListener = listener;
    }

    public void setObservable(Observable<Response<T>> observable) {
        if (mObservable != null) {
            throw new IllegalStateException("ApiRequest already has an observable.");
        }
        mObservable = observable;
    }

    public synchronized void cancel() {
        if (!mCanceled) {
            mCanceled = true;
            if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                mSubscription.unsubscribe();
                mSubscription = null;
            }
        }
    }

    public synchronized boolean isCanceled() {
        return mCanceled;
    }

    public synchronized void subscribe(Looper looper) {
        if (mObservable == null) {
            throw new IllegalStateException("ApiRequest has not an observable.");
        }
        if (mCanceled) {
            return;
        }

        mSubscription = mObservable.observeOn(AndroidSchedulers.from(looper))
                .filter(new Func1<Response<T>, Boolean>() {
                    @Override
                    public Boolean call(Response<T> response) {
                        if (mCanceled) {
                            return false;
                        }
                        if (response.code() == 401) {
                            // TODO: token invalid
                            return false;
                        }

                        return true;
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Response<T>>() {
                    @Override
                    public void call(Response<T> response) {
                        if (mCanceled) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            if (mListener != null) {
                                mListener.onSuccess(response.body());
                            }
                        } else {
                            if (mListener != null) {
                                mListener.onFailure(null);
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (mCanceled) {
                            return;
                        }

                        if (mListener != null) {
                            mListener.onFailure(throwable);
                        }
                    }
                });
    }

}
