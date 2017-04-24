package sk.teamsoft.rxlog1;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * @author Dusan Bartos
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class RxLog1 {

    public static final int LOG_NEXT_DATA = 1;
    public static final int LOG_NEXT_EVENT = 2;
    public static final int LOG_ERROR = 4;
    public static final int LOG_COMPLETE = 8;
    public static final int LOG_SUBSCRIBE = 16;
    public static final int LOG_UNSUBSCRIBE = 32;

    public static <T> Observable.Transformer<T, T> log(final String msg, final int bitMask) {
        return upstream -> {
            if ((bitMask & LOG_SUBSCRIBE) > 0) {
                upstream = upstream.compose(logSubscribe(msg));
            }
            if ((bitMask & LOG_UNSUBSCRIBE) > 0) {
                upstream = upstream.compose(logTerminate(msg));
            }
            if ((bitMask & LOG_ERROR) > 0) {
                upstream = upstream.compose(logError(msg));
            }
            if ((bitMask & LOG_COMPLETE) > 0) {
                upstream = upstream.compose(logComplete(msg));
            }
            if ((bitMask & LOG_NEXT_DATA) > 0) {
                upstream = upstream.compose(logNext(msg));
            }
            if ((bitMask & LOG_NEXT_EVENT) > 0) {
                upstream = upstream.compose(logNextEvent(msg));
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
    public static <T> Observable.Transformer<T, T> log(final String msg) {
        return upstream -> upstream
                .compose(logAll(msg))
                .compose(logNext(msg));
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
        return upstream -> upstream
                .compose(logAll(msg))
                .compose(logNextEvent(msg));
    }

    private static <T> Observable.Transformer<T, T> logAll(final String msg) {
        return upstream -> upstream
                .compose(logError(msg))
                .compose(logComplete(msg))
                .compose(logSubscribe(msg))
                .compose(logTerminate(msg))
                .compose(logUnsubscribe(msg));
    }

    private static <T> Observable.Transformer<T, T> logNext(final String msg) {
        return upstream -> upstream.doOnNext(data -> Timber.d("[onNext] %s %s [Thread:%s]", msg, data, Thread.currentThread().getName()));
    }

    private static <T> Observable.Transformer<T, T> logNextEvent(final String msg) {
        return upstream -> upstream.doOnNext(t -> Timber.d("[onNext] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> Observable.Transformer<T, T> logError(final String msg) {
        final Func1<Throwable, String> message = e -> e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return upstream -> upstream.doOnError(e -> Timber.e("[onError] %s - %s", msg, message.call(e)));
    }

    private static <T> Observable.Transformer<T, T> logComplete(final String msg) {
        return upstream -> upstream.doOnCompleted(() -> Timber.i("[onComplete] %s", msg));
    }

    private static <T> Observable.Transformer<T, T> logSubscribe(final String msg) {
        return upstream -> upstream.doOnSubscribe(() -> Timber.v("[subscribe] %s [Thread:%s]", msg, Thread.currentThread().getName()));
    }

    private static <T> Observable.Transformer<T, T> logTerminate(final String msg) {
        return upstream -> upstream.doOnTerminate(() -> Timber.v("[terminate] %s", msg));
    }

    private static <T> Observable.Transformer<T, T> logUnsubscribe(final String msg) {
        return upstream -> upstream.doOnUnsubscribe(() -> Timber.v("[unsubscribe] %s", msg));
    }
}
