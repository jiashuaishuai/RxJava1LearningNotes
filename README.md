# RxJava1LearningNotes
Rxjava1的学习笔记

## RxJava是什么

> a library for composing asynchronous and event-based programs using observable sequences for the Java VM
解释：一个对于构成使用的Java虚拟机观察序列异步和基于事件的程序库

**RxJava开源地址**https://github.com/ReactiveX/RxJava

**RxAndroid开源地址** https://github.com/ReactiveX/RxAndroid

## 概念关键字

* **Observable:** 发射源，译“可观察的”，观察者模式中称为“被观察者”或者“可观察对象”；
* **Observer:** 接受源，译“观察者”，观察者模式中称为“观察者”，可以接受Observable，Subject发射的数据
* **Subscriber:** 接受源，译“订阅者”，和Observer区别：Subscriber实现了Observer接口，比Observer多了一个方法unsubscribe(),用来取消订阅，当满足一定条件不想继续接收数据时可以调用该方法停止接收；Observer在subscribe()过程中，最终也会转成Subscriber对象，一般情况建议使用Subscriber作为接收源；
* **Subscription:** Observable调用subscribe()方法返回对象，同样拥有unsubscribe()方法，可以用来取消订阅事件；
* Action0：接口，只有一个无参数call方法，同样Action1(1个参数)，Action2(2个参数)...Action9(9个参数)，均无返回值。
* Func0：同上有返回值\<T>
* Subject：先放着不太懂 http://www.jianshu.com/p/240f1c8ebf9d 


## Observable创建
多种创建方式这里举例常用：

```java
Observable.create(new Observable.OnSubscribe<String>(){
 @Override
  public void call(Subscriber<? super String> subscriber) {
      subscriber.onNext("Hello, world!"); //发射一个"Hello, world!"的String
      subscriber.onCompleted();//发射完成,这种方法需要手动调用onCompleted，才会回调Observer的onCompleted方法
  }});
  
   Observable.just(1, 2, 4, 5, 6, 7);//分别发送1，2，3...
   
   Observable.from(list);//遍历list分别发送item
   
   /**
   *使用defer()有观察者订阅时才会创建Observable并且为每个观察者创建一个新的Observable
   **/
   Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just("Hell", "Hi");
            }
        });
    Observable.defer(() -> Observable.just("Hell", "Hi"));//lambda表达式简写
    
    /**
    *创建一个按照固定事件间隔发射整数序列的Observable，可作用定时器；take限定三十个
    *有个坑看看这个链接：interval的坑http://blog.csdn.net/u011033906/article/details/59753576
    **/
    Observable.interval(1, TimeUnit.SECONDS, Schedulers.trampoline()).take(30);

```
> interval的坑：http://blog.csdn.net/u011033906/article/details/59753576

## Subscriber的创建 RxJava1.x

```java
/**
*Subscriber实现了Observer接口
**/
observable.subscribe(new Subscriber<Integer>() {
            /**
             * 开始
             * 它会在 subscribe() 刚开始，而事件还未发送之前被调用，不能指定线程
             * 用于做一些对线程没有要求的准备工作，例如数据的清零或重置。
             * 可选的，默认为空
             * 如果准备工作对线程有要求请用doOnSubscribe
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
                     * 取消订阅，Subscriber 将不再接收事件。
                     * 调用 unsubscribe() 来解除引用关系，以避免内存泄露的发生。
                     */
                    unsubscribe();
                }
                System.out.println(integer);
            }
        });
        
        /**
        *Observer
        **/
         new Observer<String>() {
            @Override
            public void onCompleted() {
                
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {

            }
        };
        
```
其中**Subscriber.onStart注意事项：**
如果对准备工作的线程有要求(如：弹出一个现实进度的对话框，这必须在主线程执行)呢么onStart就不适用了，因为它总是在subscribe所发生的线程调用，而不能指定线程。要在指定线程来做准备工作，可以使用**doOnSubscribe()**方法，下面会讲。


## 操作符(Operators)
```java
    /**
     * 常用操作符
     */
    public  void commonOperator(){
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
                //允许我们在每次输出一个元素之前做一些额外操作，例如保存
                .doOnNext(this::saveTest)
                  //重复发射两次
                .repeat(2)
                .subscribe(System.out::println);
    }
      /**
     * 比如保存操作
     * @param s 字符
     */
    private void saveTest(String s) {
        System.out.println(s+"保存了。。。。");
    }
```
> lambda表达式（LamdTest::con）之的是LamdTest类里的带参数静态方法，简单介绍，后期学习
> http://daidingkang.cc/2017/05/11/java8-Lambda/

## 变换的原理：lift()

flatMap，map等变化原理：针对时间序列的处理和再次发送。它们都是基于同一个基础的变换方法：
lift(Operator)。
核心代码：

```java
  // 注意：这不是 lift() 的源码，而是将源码中与性能、兼容性、扩展性有关的代码剔除后的核心代码。
  public final <R> Observable<R> lift(final Operator<? extends R, ? super T> operator) {
        return new Observable<R>(new OnSubscribe<R>() {//创建并返回一个新的Observable
            @Override
            public void call(Subscriber<? super R> o) {
            /**
            *创建一个新的Subscriber，参数为原始Subscriber    o
            **/
                    Subscriber<? super T> st = hook.onLift(operator).call(o);
                    st.onStart();
                    onSubscribe.call(st);
            }
        });
    }
```    
原博客解释：

lift它生成了一个新的 Observable 并返回，而且创建新 Observable 所用的参数 OnSubscribe 的回调方法 call() 中的实现竟然看起来和前面讲过的 Observable.subscribe() 一样！然而它们并不一样哟~不一样的地方关键就在于第二行 onSubscribe.call(subscriber) 中的 onSubscribe **所指代的对象不同**（高能预警：接下来的几句话可能会导致身体的严重不适）——

* subscribe() 中这句话的 onSubscribe 指的是 Observable 中的 onSubscribe 对象，这个没有问题，但是 lift() 之后的情况就复杂了点。
* 当含有 lift() 时： 
    1. lift() 创建了一个 Observable 后，加上之前的原始 Observable，已经有两个 Observable 了； 
    2. 而同样地，新 Observable 里的新 OnSubscribe 加上之前的原始 Observable 中的原始 OnSubscribe，也就有了两个 OnSubscribe； 
    3. 当用户调用经过 lift() 后的 Observable 的 subscribe() 的时候，使用的是 lift() 所返回的新的 Observable ，于是它所触发的 onSubscribe.call(subscriber)，也是用的新 Observable 中的新 OnSubscribe，即在 lift() 中生成的那个 OnSubscribe； 
    4. 而这个新 OnSubscribe 的 call() 方法中的 onSubscribe ，就是指的原始 Observable 中的原始 OnSubscribe ，在这个 call() 方法里，新 OnSubscribe 利用 operator.call(subscriber) 生成了一个新的 Subscriber（Operator 就是在这里，通过自己的 call() 方法将新 Subscriber 和原始 Subscriber 进行关联，并插入自己的『变换』代码以实现变换），然后利用这个新 Subscriber 向原始 Observable 进行订阅。这样就实现了 lift() 过程，**有点像一种代理机制，通过事件拦截和处理实现事件序列的变换。**

**简单的说：在 Observable 执行了 lift(Operator) 方法之后，会返回一个新的 Observable，这个新的 Observable 会像一个代理一样，负责接收原始的 Observable 发出的事件，并在处理后发送给 Subscriber。**

### 思维理解图
![](https://github.com/jiashuaishuai/RxJava1LearningNotes/tree/master/media/15131361430879.jpg)

![](https://github.com/jiashuaishuai/RxJava1LearningNotes/tree/master/media/15130620756024/15131338578373.gif)

多次lift()
![](https://github.com/jiashuaishuai/RxJava1LearningNotes/tree/mastermedia/15130620756024/15131361769836.jpg)


###Operator实现String转成Integer

>当然不建议这么使用RxJava为我们包装了丰富的变换方法（如 map() flatMap() 等）进行组合来实现需求，因为直接使用 lift() 非常容易发生一些难以发现的错误。

```java 

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
                       double d= Double.parseDouble(s);
                        subscriber.onNext((int)d);
                    }
                };
            }
        }).subscribe(System.out::println);

```



##compose: 对 Observable 整体的变换

Observable还有一个变换方法叫做compose(Transformer)。
它和lift的区别在于：
**lift()：**是针对事件项和时间序列的，
**compose()：**是针对Observable自身进行变换

如：在程序中有多个Observable，并且他们都需要应用一组相同的lift()变换。

```java
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
```


##线程控制(Scheduler)

###Scheduler 的 API

* **AndroidSchedulers.mainThread：**它指定的操作将在Android主线程运行，
* **Schedulers.newThread：**总是启用新县城，并且在新线程中执行操作
* **Schedulers.io：**I/O操作所使用的Scheduler和Schedulers.newThread行为和模式差不多，区别io内部实现了一个无数量上限的线程池，重用空闲线程，比newThread()更有效率，常用
* Schedulers.immediate()：直接在当前线程运行，相当于不指定线程，默认的
* Schedulers.computation：计算所使用的Scheduler。指：CPU密集型计算，即不会被I/O等操作限制性能的操作，如：图形计算。这个Scheduler使用的固定线程池，大小为CPU内核数，建议：不要把I/O操作，会浪费CPU


###subscribeOn()
指定subscribe() 所发生的线程，即 Observable.OnSubscribe 被激活时所处的线程。或者叫做事件产生的线程;

* 作用于该操作符之前的 Observable 的创建操符作，以及doOnSubscribe 操作符；
* 换句话说就是 doOnSubscribe 以及 Observable 的创建操作符，总是被其之后最近的subscribeOn 控制；
* 当Observable 的创建操作符使用了多个 subscribeOn() 的时候，只有第一个 subscribeOn() 起作用；
* Observable创建操作符和doOnSubscribe各自使用subscribeOn()时互不干扰，后边讲解doOnSubscribe时会说明

>当Observable 的创建操作符使用了多个 subscribeOn() 的时候，只有第一个 subscribeOn() 起作用。原理图会有讲解

###observeOn()
指定 Subscriber 所运行在的线程。或者叫做事件消费的线程;

* observeOn作用于该操作符之后操作符直到出现新的observeOn操作符；
* 可以多次使用



```java
 my_tv.setOnClickListener(v -> rx_scheduler());
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
    
    /**
    *observeOn()多次切换线程
    **/
    Observable.just(1, 2, 3, 4) // IO 线程，由 subscribeOn() 指定
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.newThread())
    .map(mapOperator) // 新线程，由 observeOn() 指定
    .observeOn(Schedulers.io())
    .map(mapOperator2) // IO 线程，由 observeOn() 指定
    .observeOn(AndroidSchedulers.mainThread) 
    .subscribe(subscriber);  // Android 主线程，由 observeOn() 指定


```

##Scheduler 的原理
subscribeOn() 和 observeOn() 的内部实现，也是用的 lift()。
线程切换的工作（图中的 “schedule…” 部位）具体看图（不同颜色的箭头表示不同的线程）
> lift()原理 http://www.jianshu.com/p/c8a365592ac4

**subscribeOn原理图：**线程切换发生在 OnSubscribe 中，即在它通知上一级 OnSubscribe 时，这时事件还没有开始发送，因此 subscribeOn() 的线程控制可以从事件发出的开端就造成影响；

![](https://github.com/jiashuaishuai/RxJava1LearningNotes/tree/master/media/15130620756024/15130683533916.jpg)

**observeOn原理图：**发生在它内建的 Subscriber 中，即发生在它即将给下一级 Subscriber 发送事件时，因此 observeOn() 控制的是它后面的线程。
![](https://github.com/jiashuaishuai/RxJava1LearningNotes/tree/master/media/15130620756024/15130683793019.jpg)


**当多个subscribeOn() 和 observeOn() 混合使用时：**

![](https://github.com/jiashuaishuai/RxJava1LearningNotes/tree/master/media/15130620756024/15130684845709.jpg)

图中共有 5 处含有对事件的操作。由图中可以看出，①和②两处受第一个 subscribeOn() 影响，运行在红色线程；③和④处受第一个 observeOn() 的影响，运行在绿色线程；⑤处受第二个 onserveOn() 影响，运行在紫色线程；而第二个 subscribeOn() ，由于在通知过程中线程就被第一个 subscribeOn() 截断，因此对整个流程并没有任何影响。这里也就回答了前面的问题：当使用了多个 subscribeOn() 的时候，只有第一个 subscribeOn() 起作用。


##doOnSubscribe 对线程有要求的初始化工作
然而，虽然超过一个subscribeOn()对事件处理的流程没有影响，但是流程之前却可以利用的。

上面提到的Subscriber的onStart方法可以作用于流程开始前的初始化。然而onStart()由于在subscribe()发生时就被调用了，因此不能指定线程，而是只能执行在subscribe()被调用时的线程。
这就导致如果onStart()中含有对线程要求的代码(例如在界面上显示一个progressBar，就必须在主线程中执行)将会有线程非法的风险，因为有时你无法预测subscribe()将会在什么线程执行。

这是就需要**Observable.doOnSubscribe()**。它和Subscriber.onStart()同样是在subscribe()调用后且时间发送前执行，但是区别在于他可以指定线程。默认情况下，doOnSubscribe()执行在subscribe()发生的线程；**而如果在doOnSubscribe()之后有subscribeOn()的话，他将执行离他最近的subscribeOn()所指定的线程。**

###doOnSubscribe（）例如：
```java
Observable.create(onSubscribe)
    .subscribeOn(Schedulers.io())
    .doOnSubscribe(new Action0() {
        @Override
        public void call() {
            progressBar.setVisibility(View.VISIBLE); // 需要在主线程执行
        }
    })
    .subscribeOn(AndroidSchedulers.mainThread()) // 指定主线程
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(subscriber);
```
##参考博客：
（1）https://www.daidingkang.cc/2017/05/19/Rxjava/
（2）http://gank.io/post/560e15be2dca930e00da1083



[TOC]
