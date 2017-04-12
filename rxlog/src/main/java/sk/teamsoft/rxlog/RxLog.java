package sk.teamsoft.rxlog;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import timber.log.Timber;

/**
 * @author Dusan Bartos
 */
public final class RxLog {

    public static final int LOG_NEXT_DATA = 1;
    public static final int LOG_NEXT_EVENT = 2;
    public static final int LOG_ERROR = 4;
    public static final int LOG_COMPLETE = 8;
    public static final int LOG_SUBSCRIBE = 16;
    public static final int LOG_UNSUBSCRIBE = 32;

    public static <T> ObservableTransformer<T, T> log(final String msg, final int bitMask) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .map(new Function<T, Observable<T>>() {
                            @Override public Observable<T> apply(@NonNull T t) throws Exception {
                                return Observable.just(t);
                            }
                        })
                        .flatMap(new Function<Observable<T>, ObservableSource<T>>() {
                            @Override
                            public ObservableSource<T> apply(@NonNull Observable<T> tObservable) throws Exception {
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
    public static <T> ObservableTransformer<T, T> log(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .compose(RxLog.<T>logAll(msg))
                        .compose(RxLog.<T>logNext(msg));
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
    public static <T> ObservableTransformer<T, T> logLifecycle(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .compose(RxLog.<T>logAll(msg))
                        .compose(RxLog.<T>logNextEvent(msg));
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logAll(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .compose(RxLog.<T>logError(msg))
                        .compose(RxLog.<T>logComplete(msg))
                        .compose(RxLog.<T>logSubscribe(msg))
                        .compose(RxLog.<T>logTerminate(msg))
                        .compose(RxLog.<T>logDispose(msg));
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logNext(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.doOnNext(new Consumer<T>() {
                    @Override public void accept(@NonNull T data) throws Exception {
                        Timber.d("[onNext] %s %s [Thread:%s]", msg, data, Thread.currentThread().getName());
                    }
                });
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logNextEvent(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.doOnNext(new Consumer<T>() {
                    @Override public void accept(@NonNull T t) throws Exception {
                        Timber.d("[onNext] %s [Thread:%s]", msg, Thread.currentThread().getName());
                    }
                });
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logError(final String msg) {
        final Function<Throwable, String> message = new Function<Throwable, String>() {
            @Override public String apply(@NonNull Throwable e) throws Exception {
                return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            }
        };

        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.doOnError(new Consumer<Throwable>() {
                    @Override public void accept(@NonNull Throwable e) throws Exception {
                        Timber.e("[onError] %s - %s", msg, message.apply(e));
                    }
                });
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logComplete(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.doOnComplete(new Action() {
                    @Override public void run() throws Exception {
                        Timber.i("[onComplete] %s", msg);
                    }
                });
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logSubscribe(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.doOnSubscribe(new Consumer<Disposable>() {
                    @Override public void accept(@NonNull Disposable disposable) throws Exception {
                        Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName());
                    }
                });
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logTerminate(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.doOnTerminate(new Action() {
                    @Override public void run() throws Exception {
                        Timber.v("[terminate] %s", msg);
                    }
                });
            }
        };
    }

    private static <T> ObservableTransformer<T, T> logDispose(final String msg) {
        return new ObservableTransformer<T, T>() {
            @Override public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.doOnDispose(new Action() {
                    @Override public void run() throws Exception {
                        Timber.v("[dispose] %s", msg);
                    }
                });
            }
        };
    }
}
