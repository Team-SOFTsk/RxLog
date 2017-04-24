package sk.teamsoft.rxlog;

import io.reactivex.CompletableTransformer;
import io.reactivex.FlowableTransformer;
import io.reactivex.MaybeTransformer;
import io.reactivex.ObservableTransformer;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Function;
import timber.log.Timber;

/**
 * @author Dusan Bartos
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class RxLog {

    public static final int LOG_NEXT_DATA = 1;
    public static final int LOG_NEXT_EVENT = 2;
    public static final int LOG_ERROR = 4;
    public static final int LOG_COMPLETE = 8;
    public static final int LOG_SUBSCRIBE = 16;
    public static final int LOG_TERMINATE = 32;
    public static final int LOG_DISPOSE = 64;

    //region Observable

    /**
     * Creates transform operator, which logs defined events in observable's lifecycle
     * @param msg     message
     * @param bitMask bitmask of events which you want to log
     * @param <T>     type
     * @return transformer
     */
    public static <T> ObservableTransformer<T, T> logObservable(final String msg, final int bitMask) {
        return upstream -> {
            if ((bitMask & LOG_SUBSCRIBE) > 0) {
                upstream = upstream.compose(oLogSubscribe(msg));
            }
            if ((bitMask & LOG_TERMINATE) > 0) {
                upstream = upstream.compose(oLogTerminate(msg));
            }
            if ((bitMask & LOG_ERROR) > 0) {
                upstream = upstream.compose(oLogError(msg));
            }
            if ((bitMask & LOG_COMPLETE) > 0) {
                upstream = upstream.compose(oLogComplete(msg));
            }
            if ((bitMask & LOG_NEXT_DATA) > 0) {
                upstream = upstream.compose(oLogNext(msg));
            } else if ((bitMask & LOG_NEXT_EVENT) > 0) {
                upstream = upstream.compose(oLogNextEvent(msg));
            }
            if ((bitMask & LOG_DISPOSE) > 0) {
                upstream = upstream.compose(oLogDispose(msg));
            }
            return upstream;
        };
    }

    /**
     * Creates transform operator, which logs important events in observable's lifecycle
     * @param msg message
     * @param <T> type
     * @return transformer
     */
    public static <T> ObservableTransformer<T, T> log(final String msg) {
        return upstream -> upstream
                .compose(oLogAll(msg))
                .compose(oLogNext(msg));
    }

    private static <T> ObservableTransformer<T, T> oLogAll(final String msg) {
        return upstream -> upstream
                .compose(oLogError(msg))
                .compose(oLogComplete(msg))
                .compose(oLogSubscribe(msg))
                .compose(oLogTerminate(msg))
                .compose(oLogDispose(msg));
    }

    private static <T> ObservableTransformer<T, T> oLogNext(final String msg) {
        return upstream -> upstream.doOnNext(data -> Timber.d("[onNext] %s %s [Thread:%s]", msg, data, Thread.currentThread().getName()));
    }

    private static <T> ObservableTransformer<T, T> oLogNextEvent(final String msg) {
        return upstream -> upstream.doOnNext(t -> Timber.d("[onNext] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> ObservableTransformer<T, T> oLogError(final String msg) {
        final Function<Throwable, String> message = e -> e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return upstream -> upstream.doOnError(e -> Timber.e("[onError] %s - %s", msg, message.apply(e)));
    }

    private static <T> ObservableTransformer<T, T> oLogComplete(final String msg) {
        return upstream -> upstream.doOnComplete(() -> Timber.i("[onComplete] %s", msg));
    }

    private static <T> ObservableTransformer<T, T> oLogSubscribe(final String msg) {
        return upstream -> upstream.doOnSubscribe(disposable -> Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> ObservableTransformer<T, T> oLogTerminate(final String msg) {
        return upstream -> upstream.doOnTerminate(() -> Timber.v("[terminate] %s", msg));
    }

    private static <T> ObservableTransformer<T, T> oLogDispose(final String msg) {
        return upstream -> upstream.doOnDispose(() -> Timber.v("[dispose] %s", msg));
    }
    //endregion

    //region Single

    /**
     * Creates transform operator, which logs defined events in observable's lifecycle
     * @param msg     message
     * @param bitMask bitmask of events which you want to log
     * @param <T>     type
     * @return transformer
     */
    public static <T> SingleTransformer<T, T> logSingle(final String msg, final int bitMask) {
        return upstream -> {
            if ((bitMask & LOG_SUBSCRIBE) > 0) {
                upstream = upstream.compose(sLogSubscribe(msg));
            }
            if ((bitMask & LOG_ERROR) > 0) {
                upstream = upstream.compose(sLogError(msg));
            }
            if ((bitMask & LOG_NEXT_DATA) > 0) {
                upstream = upstream.compose(sLogSuccess(msg));
            } else if ((bitMask & LOG_NEXT_EVENT) > 0) {
                upstream = upstream.compose(sLogSuccessEvent(msg));
            }
            if ((bitMask & LOG_DISPOSE) > 0) {
                upstream = upstream.compose(sLogDispose(msg));
            }
            return upstream;
        };
    }

    private static <T> SingleTransformer<T, T> sLogSuccess(final String msg) {
        return upstream -> upstream.doOnSuccess(data -> Timber.d("[onSuccess] %s %s [Thread:%s]", msg, data, Thread.currentThread().getName()));
    }

    private static <T> SingleTransformer<T, T> sLogSuccessEvent(final String msg) {
        return upstream -> upstream.doOnSuccess(x -> Timber.d("[onSuccess] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> SingleTransformer<T, T> sLogError(final String msg) {
        final Function<Throwable, String> message = e -> e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return upstream -> upstream.doOnError(e -> Timber.e("[onError] %s - %s", msg, message.apply(e)));
    }

    private static <T> SingleTransformer<T, T> sLogSubscribe(final String msg) {
        return upstream -> upstream.doOnSubscribe(disposable -> Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> SingleTransformer<T, T> sLogDispose(final String msg) {
        return upstream -> upstream.doOnDispose(() -> Timber.v("[dispose] %s", msg));
    }
    //endregion

    //region Maybe

    /**
     * Creates transform operator, which logs defined events in observable's lifecycle
     * @param msg     message
     * @param bitMask bitmask of events which you want to log
     * @param <T>     type
     * @return transformer
     */
    public static <T> MaybeTransformer<T, T> logMaybe(final String msg, final int bitMask) {
        return upstream -> {
            if ((bitMask & LOG_SUBSCRIBE) > 0) {
                upstream = upstream.compose(mLogSubscribe(msg));
            }
            if ((bitMask & LOG_ERROR) > 0) {
                upstream = upstream.compose(mLogError(msg));
            }
            if ((bitMask & LOG_NEXT_DATA) > 0) {
                upstream = upstream.compose(mLogSuccess(msg));
            } else if ((bitMask & LOG_NEXT_EVENT) > 0) {
                upstream = upstream.compose(mLogSuccessEvent(msg));
            }
            if ((bitMask & LOG_COMPLETE) > 0) {
                upstream = upstream.compose(mLogComplete(msg));
            }
            if ((bitMask & LOG_DISPOSE) > 0) {
                upstream = upstream.compose(mLogDispose(msg));
            }
            return upstream;
        };
    }

    private static <T> MaybeTransformer<T, T> mLogSuccess(final String msg) {
        return upstream -> upstream.doOnSuccess(data -> Timber.d("[onSuccess] %s %s [Thread:%s]", msg, data, Thread.currentThread().getName()));
    }

    private static <T> MaybeTransformer<T, T> mLogSuccessEvent(final String msg) {
        return upstream -> upstream.doOnSuccess(x -> Timber.d("[onSuccess] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> MaybeTransformer<T, T> mLogError(final String msg) {
        final Function<Throwable, String> message = e -> e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return upstream -> upstream.doOnError(e -> Timber.e("[onError] %s - %s", msg, message.apply(e)));
    }

    private static <T> MaybeTransformer<T, T> mLogComplete(final String msg) {
        return upstream -> upstream.doOnComplete(() -> Timber.v("[onComplete] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> MaybeTransformer<T, T> mLogSubscribe(final String msg) {
        return upstream -> upstream.doOnSubscribe(disposable -> Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> MaybeTransformer<T, T> mLogDispose(final String msg) {
        return upstream -> upstream.doOnDispose(() -> Timber.v("[dispose] %s", msg));
    }
    //endregion

    //region Completable

    /**
     * Creates transform operator, which logs defined events in observable's lifecycle
     * @param msg     message
     * @param bitMask bitmask of events which you want to log
     * @return transformer
     */
    public static CompletableTransformer logCompletable(final String msg, final int bitMask) {
        return upstream -> {
            if ((bitMask & LOG_SUBSCRIBE) > 0) {
                upstream = upstream.compose(cLogSubscribe(msg));
            }
            if ((bitMask & LOG_ERROR) > 0) {
                upstream = upstream.compose(cLogError(msg));
            }
            if ((bitMask & LOG_COMPLETE) > 0) {
                upstream = upstream.compose(cLogComplete(msg));
            }
            if ((bitMask & LOG_DISPOSE) > 0) {
                upstream = upstream.compose(cLogDispose(msg));
            }
            return upstream;
        };
    }

    private static CompletableTransformer cLogError(final String msg) {
        final Function<Throwable, String> message = e -> e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return upstream -> upstream.doOnError(e -> Timber.e("[onError] %s - %s", msg, message.apply(e)));
    }

    private static CompletableTransformer cLogComplete(final String msg) {
        return upstream -> upstream.doOnComplete(() -> Timber.v("[onComplete] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static CompletableTransformer cLogSubscribe(final String msg) {
        return upstream -> upstream.doOnSubscribe(disposable -> Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static CompletableTransformer cLogDispose(final String msg) {
        return upstream -> upstream.doOnDispose(() -> Timber.v("[dispose] %s", msg));
    }
    //endregion

    //region Flowable

    /**
     * Creates transform operator, which logs defined events in observable's lifecycle
     * @param msg     message
     * @param bitMask bitmask of events which you want to log
     * @param <T>     type
     * @return transformer
     */
    public static <T> FlowableTransformer<T, T> logFlowable(final String msg, final int bitMask) {
        return upstream -> {
            if ((bitMask & LOG_SUBSCRIBE) > 0) {
                upstream = upstream.compose(fLogSubscribe(msg));
            }
            if ((bitMask & LOG_TERMINATE) > 0) {
                upstream = upstream.compose(fLogTerminate(msg));
            }
            if ((bitMask & LOG_ERROR) > 0) {
                upstream = upstream.compose(fLogError(msg));
            }
            if ((bitMask & LOG_COMPLETE) > 0) {
                upstream = upstream.compose(fLogComplete(msg));
            }
            if ((bitMask & LOG_NEXT_DATA) > 0) {
                upstream = upstream.compose(fLogNext(msg));
            } else if ((bitMask & LOG_NEXT_EVENT) > 0) {
                upstream = upstream.compose(fLogNextEvent(msg));
            }
            return upstream;
        };
    }

    private static <T> FlowableTransformer<T, T> fLogNext(final String msg) {
        return upstream -> upstream.doOnNext(data -> Timber.d("[onNext] %s %s [Thread:%s]", msg, data, Thread.currentThread().getName()));
    }

    private static <T> FlowableTransformer<T, T> fLogNextEvent(final String msg) {
        return upstream -> upstream.doOnNext(t -> Timber.d("[onNext] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> FlowableTransformer<T, T> fLogError(final String msg) {
        final Function<Throwable, String> message = e -> e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return upstream -> upstream.doOnError(e -> Timber.e("[onError] %s - %s", msg, message.apply(e)));
    }

    private static <T> FlowableTransformer<T, T> fLogComplete(final String msg) {
        return upstream -> upstream.doOnComplete(() -> Timber.i("[onComplete] %s", msg));
    }

    private static <T> FlowableTransformer<T, T> fLogSubscribe(final String msg) {
        return upstream -> upstream.doOnSubscribe(disposable -> Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> FlowableTransformer<T, T> fLogTerminate(final String msg) {
        return upstream -> upstream.doOnTerminate(() -> Timber.v("[terminate] %s", msg));
    }
    //endregion
}
