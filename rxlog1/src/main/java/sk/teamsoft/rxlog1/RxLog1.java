package sk.teamsoft.rxlog1;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * @author Dusan Bartos
 */
public final class RxLog1 {

    public static final int LOG_NEXT_DATA = 1;
    public static final int LOG_NEXT_EVENT = 2;
    public static final int LOG_ERROR = 4;
    public static final int LOG_COMPLETE = 8;
    public static final int LOG_SUBSCRIBE = 16;
    public static final int LOG_UNSUBSCRIBE = 32;

    public static <T> Observable.Transformer<T, T> log(final String msg, final int bitMask) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream
                        .map(new Func1<T, Observable<T>>() {
                            @Override public Observable<T> call(T t) {
                                return Observable.just(t);
                            }
                        })
                        .flatMap(new Func1<Observable<T>, Observable<T>>() {
                            @Override
                            public Observable<T> call(Observable<T> tObservable) {
                                if ((bitMask & LOG_SUBSCRIBE) > 0) {
                                    tObservable.compose(logSubscribe(msg));
                                }
                                if ((bitMask & LOG_UNSUBSCRIBE) > 0) {
                                    tObservable.compose(logTerminate(msg));
                                }
                                if ((bitMask & LOG_ERROR) > 0) {
                                    tObservable.compose(logError(msg));
                                }
                                if ((bitMask & LOG_COMPLETE) > 0) {
                                    tObservable.compose(logComplete(msg));
                                }
                                if ((bitMask & LOG_NEXT_DATA) > 0) {
                                    tObservable.compose(logNext(msg));
                                }
                                if ((bitMask & LOG_NEXT_EVENT) > 0) {
                                    tObservable.compose(logNextEvent(msg));
                                }
                                return tObservable;
                            }
                        });
            }
        };
    }

    /**
     * Creates transform operator, which logs important events in observable's lifecycle
     * @param msg message
     * @param <T> type
     * @return transformer
     */
    public static <T> Observable.Transformer<T, T> log(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream
                        .compose(RxLog1.<T>logAll(msg))
                        .compose(RxLog1.<T>logNext(msg));
            }
        };
    }

    /**
     * Creates transform operator, which logs important events in observable's lifecycle
     * This one is different from {@link #log(String)} in a way it logs only lifecycle events, and
     * not actual data passed in these events (which can be slow when serialized data are huge)
     * @param msg message
     * @param <T> type
     * @return transformer
     */
    public static <T> Observable.Transformer<T, T> logLifecycle(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream
                        .compose(RxLog1.<T>logAll(msg))
                        .compose(RxLog1.<T>logNextEvent(msg));
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logAll(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream
                        .compose(RxLog1.<T>logError(msg))
                        .compose(RxLog1.<T>logComplete(msg))
                        .compose(RxLog1.<T>logSubscribe(msg))
                        .compose(RxLog1.<T>logTerminate(msg))
                        .compose(RxLog1.<T>logUnsubscribe(msg));
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logNext(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream.doOnNext(new Action1<T>() {
                    @Override public void call(T data) {
                        Timber.d("[onNext] %s %s [Thread:%s]", msg, data, Thread.currentThread().getName());
                    }
                });
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logNextEvent(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream.doOnNext(new Action1<T>() {
                    @Override public void call(T t) {
                        Timber.d("[onNext] %s [Thread:%s]", msg, Thread.currentThread().getName());
                    }
                });
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logError(final String msg) {
        final Func1<Throwable, String> message = new Func1<Throwable, String>() {
            @Override public String call(Throwable e) {
                return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            }
        };

        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream.doOnError(new Action1<Throwable>() {
                    @Override public void call(Throwable e) {
                        Timber.e("[onError] %s - %s", msg, message.call(e));
                    }
                });
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logComplete(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream.doOnCompleted(new Action0() {
                    @Override public void call() {
                        Timber.i("[onComplete] %s", msg);
                    }
                });
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logSubscribe(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream.doOnSubscribe(new Action0() {
                    @Override public void call() {
                        Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName());
                    }
                });
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logTerminate(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream.doOnTerminate(new Action0() {
                    @Override public void call() {
                        Timber.v("[terminate] %s", msg);
                    }
                });
            }
        };
    }

    private static <T> Observable.Transformer<T, T> logUnsubscribe(final String msg) {
        return new Observable.Transformer<T, T>() {
            @Override public Observable<T> call(Observable<T> upstream) {
                return upstream.doOnUnsubscribe(new Action0() {
                    @Override public void call() {
                        Timber.v("[unsubscribe] %s", msg);
                    }
                });
            }
        };
    }
}
