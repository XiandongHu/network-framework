package com.example.huxiandong.network.api;

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

        void onFailure(ApiError apiError);
    }

    private static final int MAX_RETRY_TIMES = 2;

    private final ApiResponseHandler mApiResponseHandler;
    private Listener<T> mListener;

    private Observable<Response<T>> mObservable;
    private Subscription mSubscription;
    private volatile boolean mCanceled;

    private int mRetryTimes = 0;
    private boolean mRetrying = false;

    ApiRequest(ApiResponseHandler apiResponseHandler, Listener<T> listener) {
        mApiResponseHandler = apiResponseHandler;
        mListener = listener;
    }

    void setObservable(Observable<Response<T>> observable) {
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
            if (mRetrying) {
                mRetryTimes = 0;
                mRetrying = false;
                mApiResponseHandler.cancel(this);
            }
        }
    }

    synchronized boolean isCanceled() {
        return mCanceled;
    }

    synchronized void subscribe() {
        if (mObservable == null) {
            throw new IllegalStateException("ApiRequest has not an observable.");
        }
        if (mCanceled) {
            return;
        }

        mSubscription = mObservable.observeOn(mApiResponseHandler.getScheduler())
                .filter(new Func1<Response<T>, Boolean>() {
                    @Override
                    public Boolean call(Response<T> response) {
                        synchronized (ApiRequest.this) {
                            if (mCanceled) {
                                return false;
                            }
                        }

                        if (response.code() == 401) {
                            if (mRetryTimes >= MAX_RETRY_TIMES) {
                                error(ApiError.TOKEN_INVALID);
                            } else {
                                synchronized (ApiRequest.this) {
                                    mRetryTimes++;
                                    mRetrying = true;
                                }
                                mApiResponseHandler.retry(ApiRequest.this);
                            }
                            return false;
                        }

                        synchronized (ApiRequest.this) {
                            mRetryTimes = 0;
                            mRetrying = false;
                        }
                        return true;
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Response<T>>() {
                    @Override
                    public void call(Response<T> response) {
                        synchronized (ApiRequest.this) {
                            if (mCanceled) {
                                return;
                            }
                        }

                        if (mListener != null) {
                            if (response.isSuccessful()) {
                                BaseResponse baseResponse = response.body();
                                if (baseResponse.code == 0) {
                                    mListener.onSuccess(response.body());
                                } else {
                                    ApiError apiError = ApiError.generateError(baseResponse.code, baseResponse.message);
                                    mListener.onFailure(apiError);
                                }
                            } else {
                                int httpCode = response.code();
                                if (httpCode >= 400 && httpCode < 500) {
                                    mListener.onFailure(ApiError.HTTP_CODE_4XX);
                                } else if (httpCode >= 500) {
                                    mListener.onFailure(ApiError.HTTP_CODE_5XX);
                                } else {
                                    mListener.onFailure(ApiError.HTTP_CODE_OTHER);
                                }
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        synchronized (ApiRequest.this) {
                            if (mCanceled) {
                                return;
                            }
                            mRetryTimes = 0;
                            mRetrying = false;
                        }

                        if (mListener != null) {
                            mListener.onFailure(ApiError.HTTP_ENGINE_EXCEPTION);
                        }
                    }
                });
    }

    void error(ApiError apiError) {
        synchronized (ApiRequest.this) {
            if (mCanceled) {
                return;
            }
            mRetryTimes = 0;
            mRetrying = false;
        }

        if (mListener != null) {
            Observable.just(apiError)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<ApiError>() {
                        @Override
                        public void call(ApiError apiError) {
                            mListener.onFailure(apiError);
                        }
                    });
        }
    }

}
