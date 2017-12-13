package com.example.jiashuai.myapplicationrxjava1;


import android.util.Log;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;

/**
 * Created by JiaShuai on 2017/12/11.
 */

public class RxText_Subscriber {
    public static void main(String[] arg) {
        Observable observable = Observable.just(1, 2, 4, 5, 56, 6, 7, 7, 8, 9);
        Subscriber<Integer> subscriber = new Subscriber<Integer>() {
            /**
             * 开始
             */
            @Override
            public void onStart() {
                super.onStart();
            }

            /**
             * 完成
             */
            @Override
            public void onCompleted() {

            }

            /**
             * 错误
             * @param e error
             */
            @Override
            public void onError(Throwable e) {

            }

            /**
             * 下一个
             * @param integer object
             */
            @Override
            public void onNext(Integer integer) {
                if (integer == 56) {
                    /**
                     * 取消订阅，停止发送
                     */
                    unsubscribe();
                }
                System.out.println(integer);
            }
        };

        observable.subscribe(subscriber);


    }
}
