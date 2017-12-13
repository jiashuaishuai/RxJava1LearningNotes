package com.example.jiashuai.myapplicationrxjava1;

import android.database.CursorJoiner;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by JiaShuai on 2017/12/11.
 */

public class RxTest_Operator {

    public static void main(String[] agr) {
        RxTest_Operator rx = new RxTest_Operator();
//        rx.rx_from();
//        rx.customType();
//        rx.rx_defer();
//        rx.rx_interval();
//        rx.commonOperator();
//        rx.rx_lift();
//        rx.rx_compose();


    }

    /**
     * from
     * 使用from()便利集合发送每个Itme
     */
    private void rx_from() {
        List<String> list = new ArrayList<>(4);
        list.add("aa");
        list.add("bb");
        list.add("cc");
        list.add("dd");

        Observable observable = Observable.from(list);
        observable.subscribe(System.out::println).unsubscribe();
    }
    /**
     * 发送自定义类型
     */
    public void customType() {
        Observable.from(CustomType.values()).subscribe(new Action1<CustomType>() {
            @Override
            public void call(CustomType customType) {
                System.out.println(customType.getValue());
            }
        });
    }

    enum CustomType {
        TYPE_HE("hehehe"),
        TYPE_HI("hi"),
        TYPE_HELLO("Hello");

        private String value;

        CustomType(String s) {
            value = s;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * defer
     * 使用defer()有观察者订阅时才会创建Observable并且为每个观察者创建一个新的Observable
     */
    private void rx_defer() {
        Observable ob = Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just("Hello");
            }
        });
        ob.subscribe(System.out::println).unsubscribe();
    }

    /**
     * interval
     * 创建一个按照固定事件间隔发射整数序列的Observable，可作用定时器；
     * interval的坑http://blog.csdn.net/u011033906/article/details/59753576
     */
    private void rx_interval() {
        Observable<Long> observable = Observable.interval(1, TimeUnit.SECONDS, Schedulers.trampoline()).take(30);
        observable.subscribe(new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Long aLong) {
                System.out.println(aLong);
            }


        });


    }


    /**
     * 常用操作符
     */
    public void commonOperator() {
        Observable.just("test").map(s -> {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                list.add(s + i);
            }
            return list;
        })
                //转型
                .flatMap(Observable::from)
                //过滤
                .filter(strings -> !"test4".equals(strings))
                //输出最多制定数量的结果
                .take(6)
                //测试lambda表达式
                .map(LamdTest::con)
                //重复发射两次
                .repeat(2)
                //允许我们在每次输出一个元素之前做一些额外操作，例如保存
                .doOnNext(this::saveTest)
                .subscribe(System.out::println);

    }

    /**
     * 比如保存操作
     *
     * @param s 字符
     */
    private void saveTest(String s) {
        System.out.println(s + "保存了。。。。");
    }

    public void rx_lift() {
        Observable.just("1.00").lift(new Observable.Operator<Integer, String>() {

            @Override
            public Subscriber<? super String> call(Subscriber<? super Integer> subscriber) {
                return new Subscriber<String>(subscriber) {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(String s) {
                        double d = Double.parseDouble(s);
                        subscriber.onNext((int) d);
                    }
                };
            }
        }).subscribe(System.out::println);
    }

    /**
     * 假设在程序中有多个 Observable ，并且他们都需要应用一组相同的 lift() 变换。将String("1.00")变换为1
     * 使用compose
     *
     */
    public void rx_compose() {

        Observable.Transformer transformer = new LiftAllTransformer();
        Observable.just("1.00").compose(transformer).subscribe(System.out::println);
        Observable.just("2.00").compose(transformer).subscribe(System.out::println);

    }


class LiftAllTransformer implements Observable.Transformer<String,Integer>{

    @Override
    public Observable<Integer> call(Observable<String> stringObservable) {
        return stringObservable
                //string -> double
                .map(Double::parseDouble)
                //double -> int
                .map(Double::intValue);
    }
}

}

