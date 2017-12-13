package com.example.jiashuai.myapplicationrxjava1;

import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action7;

/**
 * Created by JiaShuai on 2017/12/11.
 */

public class RxTest1 {
    public static void main(String[] arg){
        Observable<Integer> observable = Observable.just(1, 2, 4, 5, 6, 7);
        observable.subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                System.out.println(integer);
            }
        }).unsubscribe();






    }
}
