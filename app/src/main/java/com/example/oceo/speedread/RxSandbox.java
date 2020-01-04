package com.example.oceo.speedread;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.internal.operators.observable.ObservableRange;


public class RxSandbox {
    static String TAG = "RxSandbox";

    public static void testBasicObs() {
        Log.d(TAG, "testBasics");
        ArrayList numberList = new ArrayList<>(Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9, 10));
//        Log.d(TAG, numberList.toString());
        Observable.fromIterable(numberList)
                .subscribe(num -> Log.d(TAG, String.valueOf(num)));
    }

    public static void testMappingObs() {
        Log.d(TAG, "testMappingOBs");
        Observable rangeObs = Observable.range(1, 10);

        /* map
         * map to value, not a new observable
         */
//        rangeObs = rangeObs.map(i -> Observable.just(i));
//        rangeObs.subscribe(d -> Log.d(TAG, String.valueOf(System.currentTimeMillis()) + ": " + String.valueOf(d)));


        /* flatmap
         * map item in rangeobs to new observable, subscribe to it then move to next item
         */
//        rangeObs = rangeObs.flatMap(i -> Observable.just(i).delay(500, TimeUnit.MILLISECONDS));
//        rangeObs.subscribe(d -> Log.d(TAG, String.valueOf(System.currentTimeMillis()) + ": " + String.valueOf(d)));

        /* concatmap
         * map item in rangeobs to new observable, subscribe to it(applyin delay) then move to next item
         *
         */
        rangeObs = rangeObs.concatMap(i -> Observable.just(i).delay(500, TimeUnit.MILLISECONDS));
        rangeObs.subscribe(d -> Log.d(TAG, String.valueOf(System.currentTimeMillis()) + ": " + String.valueOf(d)));


    }
}
