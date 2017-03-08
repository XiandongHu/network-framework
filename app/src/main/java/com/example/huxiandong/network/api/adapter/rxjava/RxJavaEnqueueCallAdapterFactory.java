package com.example.huxiandong.network.api.adapter.rxjava;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;

/**
 * Created by huxiandong
 * on 17-2-23.
 */

public class RxJavaEnqueueCallAdapterFactory extends CallAdapter.Factory {

    public static RxJavaEnqueueCallAdapterFactory create() {
        return new RxJavaEnqueueCallAdapterFactory(null);
    }

    public static RxJavaEnqueueCallAdapterFactory createWithScheduler(Scheduler scheduler) {
        return new RxJavaEnqueueCallAdapterFactory(scheduler);
    }

    private Scheduler mScheduler;

    private RxJavaEnqueueCallAdapterFactory(Scheduler scheduler) {
        mScheduler = scheduler;
    }

    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        if (rawType != Observable.class) {
            return null;
        }

        if (returnType instanceof ParameterizedType) {
            Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
            Class<?> rawObservableType = getRawType(observableType);
            if (rawObservableType == Response.class) {
                if (!(observableType instanceof ParameterizedType)) {
                    throw new IllegalStateException("Response must be parameterized"
                            + " as Response<Foo> or Response<? extends Foo>");
                }
                Type responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
                return new ResponseCallAdapter(responseType, mScheduler);
            }
        }

        throw new IllegalStateException("Observable return type must be parameterized as Observable<Response>");
    }

    private static final class ResponseCallAdapter implements CallAdapter<Observable<?>> {
        private final Type responseType;
        private final Scheduler scheduler;

        ResponseCallAdapter(Type responseType, Scheduler scheduler) {
            this.responseType = responseType;
            this.scheduler = scheduler;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public <R> Observable<Response<R>> adapt(Call<R> call) {
            Observable<Response<R>> observable = Observable.create(new CallOnSubscribe<>(call));
            if (scheduler != null) {
                return observable.subscribeOn(scheduler);
            }
            return observable;
        }
    }

    private static final class CallOnSubscribe<T> implements Observable.OnSubscribe<Response<T>> {
        private final Call<T> originalCall;

        CallOnSubscribe(Call<T> originalCall) {
            this.originalCall = originalCall;
        }

        @Override
        public void call(final Subscriber<? super Response<T>> subscriber) {
            // Since Call is a one-shot type, clone it for each new subscriber.
            Call<T> call = originalCall.clone();

            // Wrap the call in a helper which handles both unsubscription and backpressure.
            RequestArbiter<T> requestArbiter = new RequestArbiter<>(call, subscriber);
            subscriber.add(requestArbiter);
            subscriber.setProducer(requestArbiter);
        }
    }

    private static final class RequestArbiter<T> extends AtomicBoolean implements Subscription, Producer {
        private static final long serialVersionUID = 7982451379763135859L;

        private final Call<T> call;
        private final Subscriber<? super Response<T>> subscriber;

        RequestArbiter(Call<T> call, Subscriber<? super Response<T>> subscriber) {
            this.call = call;
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n < 0) throw new IllegalArgumentException("n < 0: " + n);
            if (n == 0) return; // Nothing to do when requesting 0.
            if (!compareAndSet(false, true)) return; // Request was already triggered.

            try {
                call.enqueue(new Callback<T>() {
                    @Override
                    public void onResponse(Call<T> call, Response<T> response) {
                        if (!subscriber.isUnsubscribed()) {
                            try {
                                subscriber.onNext(response);
                            } catch (Throwable t) {
                                onError(t);
                                return;
                            }
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onCompleted();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<T> call, Throwable t) {
                        onError(t);
                    }
                });
            } catch (Throwable t) {
                onError(t);
            }
        }

        private void onError(Throwable t) {
            Exceptions.throwIfFatal(t);
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(t);
            }
        }

        @Override
        public void unsubscribe() {
            call.cancel();
        }

        @Override
        public boolean isUnsubscribed() {
            return call.isCanceled();
        }
    }

}
