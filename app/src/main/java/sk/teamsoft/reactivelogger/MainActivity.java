package sk.teamsoft.reactivelogger;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import sk.teamsoft.rxlog.RxLog;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
//    private final CompositeSubscription compositeSubscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.plant(new Timber.DebugTree());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override protected void onResume() {
        super.onResume();

        compositeDisposable.add(Observable.interval(500, TimeUnit.MILLISECONDS)
                .compose(RxLog.<Long>log("rx2:interval"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override public void accept(@NonNull Long aLong) throws Exception {}
                }, new Consumer<Throwable>() {
                    @Override public void accept(@NonNull Throwable throwable) throws Exception {
                        Timber.e("error %d", throwable.getMessage());
                    }
                }));

        Single.just("prd")
                .compose(RxLog.<String>logSingle("ddd", RxLog.LOG_COMPLETE))
                .subscribe(new Consumer<String>() {
                    @Override public void accept(@NonNull String s) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override public void accept(@NonNull Throwable throwable) throws Exception {

                    }
                });

        /*compositeSubscription.add(rx.Observable.interval(700, TimeUnit.MILLISECONDS)
                .compose(RxLog1.<Long>log("rx1:interval"))
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override public void call(@NonNull Long aLong) {}
                }, new Action1<Throwable>() {
                    @Override public void call(@NonNull Throwable throwable) {
                        Timber.e("error %d", throwable.getMessage());
                    }
                }));*/
    }

    @Override protected void onPause() {
        compositeDisposable.clear();
//        compositeSubscription.clear();
        super.onPause();
    }
}
