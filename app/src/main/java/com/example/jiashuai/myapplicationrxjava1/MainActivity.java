package com.example.jiashuai.myapplicationrxjava1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private TextView my_tv;
    private TextView my_tv2;
    private TextView my_tv3;
    private TextView my_tv4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        my_tv = findViewById(R.id.my_tv);
        my_tv.setOnClickListener(v -> rx_scheduler());
        my_tv2 = findViewById(R.id.my_tv2);
        my_tv2.setOnClickListener(v -> rx_interva());
        my_tv3 = findViewById(R.id.my_tv3);
        my_tv3.setOnClickListener(v -> backpressureTest());
        my_tv4 = findViewById(R.id.my_tv4);
        my_tv4.setOnClickListener(v -> backpressureFrom());

    }

    public void rx_scheduler() {
        List<String> list = new ArrayList<>(4);
        list.add("aa");
        list.add("bb");
        list.add("cc");
        list.add("dd");
        Observable.from(list).map(s -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("卡顿两秒");
            return s.concat("经过map");
        })
                /**
                 *
                 * subscribeOn 作用于该操作符之前的 Observable 的创建操符作以及 doOnSubscribe 操作符 ，
                 * 换句话说就是 doOnSubscribe 以及 Observable 的创建操作符总是被其之后最近的 subscribeOn 控制
                 * 当使用了多个 subscribeOn() 的时候，只有第一个 subscribeOn() 起作用。
                 */
                .subscribeOn(Schedulers.io())
                /**
                 *
                 * observeOn作用于该操作符之后操作符直到出现新的observeOn操作符
                 * 可以多次使用
                 */
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::tvShow);
        tvShow("ok主线程跑到这里");
    }


    private void rx_interva() {
        Observable.interval(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(this::tv2Show);
    }

    /**
     * 背压崩溃实例，上游发送速度快，下游处理速度慢
     *  Caused by: rx.exceptions.MissingBackpressureException
     */
    private void backpressureTest() {
/**
 * 实例1
 */
        //被观察者在主线程中，每1ms发送一个事件
        Observable.interval(1, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                //将观察者的工作放在新线程环境中
                .observeOn(Schedulers.newThread())
                //观察者处理每1000ms才处理一个事件
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.w("TAG", "---->" + aLong);
                    }
                });

        /**
         * 实例2
         */
        Observable.interval(1, TimeUnit.MILLISECONDS)
                .doOnNext(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        System.out.println(Thread.currentThread().getName() + ":   " + aLong);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        try {
                            Thread.sleep(3000);
                            System.out.println(Thread.currentThread().getName() + ":   " + aLong);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });


    }

    /**
     * 不支持背压的
     * 流速控制，缓存
     */
    private void backpressureFrom() {
        List<String> list = new ArrayList();
        for (int i = 0; i < 3000; i++) {
            list.add("hello" + i);
        }

        Observable
                /**
                 * 每次16条
                 * 缓存
                 * observeOn这个操作符内部有一个缓冲区，Android环境下长度是16，最多发送16个事件，充满缓冲区即可
                 */
                .from(list)
                .doOnNext(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        System.out.println(Thread.currentThread().getName() + ":   " + s);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        try {
                            Thread.sleep(3000);
                            System.out.println(Thread.currentThread().getName() + "    " + s);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void tvShow(String s) {
        my_tv.setText(s);
    }

    private void tv2Show(long l) {
        my_tv2.setText(l + "秒");
    }

    private void tv3SHow(long l) {
        my_tv3.setText(l + "秒");
    }
}
