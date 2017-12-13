package com.example.jiashuai.myapplicationrxjava1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        my_tv = findViewById(R.id.my_tv);
        my_tv.setOnClickListener(v -> rx_scheduler());
        my_tv2 = findViewById(R.id.my_tv2);
        my_tv2.setOnClickListener(v -> rx_interva());


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

    private void tvShow(String s) {
        my_tv.setText(s);
    }


    private void rx_interva() {
        Observable.interval(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(this::tv2Show);
    }

    private void tv2Show(long l) {
        my_tv2.setText(l + "秒");
    }
}
