package com.soundcloud.android.utils.extensions

import io.reactivex.Maybe
import io.reactivex.MaybeSource
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.functions.Function4
import io.reactivex.rxkotlin.Maybes
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.Singles

//Observables combine functions
fun <T1, T2, T3, R> Observables.combineLatest(source1: ObservableSource<out T1>,
                                              source2: ObservableSource<out T2>,
                                              source3: ObservableSource<out T3>,
                                              combiner: (T1, T2, T3) -> R): Observable<R> {
    return Observable.combineLatest(source1, source2, source3, Function3<T1, T2, T3, R> { t1, t2, t3 ->
        combiner.invoke(t1, t2, t3)
    })
}

fun <T1, T2, R> Observables.combineLatest(source1: ObservableSource<out T1>,
                                          source2: ObservableSource<out T2>,
                                          combiner: (T1, T2) -> R): Observable<R> {
    return Observable.combineLatest(source1, source2, BiFunction<T1, T2, R> { t1, t2 ->
        combiner.invoke(t1, t2)
    })
}

//Maybes combine functions
fun <T1, T2, R> Maybes.zip(source1: MaybeSource<out T1>, source2: MaybeSource<out T2>, zipper: (T1, T2) -> R): Maybe<R> {
    return Maybe.zip(source1, source2, BiFunction { t1, t2 -> zipper.invoke(t1, t2) })
}

fun <T1, T2, T3, R> Maybes.zip(source1: MaybeSource<out T1>, source2: MaybeSource<out T2>, source3: MaybeSource<out T3>, zipper: (T1, T2, T3) -> R): Maybe<R> {
    return Maybe.zip(source1, source2, source3, Function3 { t1, t2, t3 -> zipper.invoke(t1, t2, t3) })
}

//Singles combine functions
fun <T1 : Any, T2 : Any, R> Singles.zip(source1: SingleSource<out T1>, source2: SingleSource<out T2>, zipper: (T1, T2) -> R): Single<R> {
    return Single.zip(source1, source2, BiFunction { t1, t2 -> zipper.invoke(t1, t2) })
}

fun <T1, T2, T3, R> Singles.zip(source1: SingleSource<out T1>, source2: SingleSource<out T2>, source3: SingleSource<out T3>, zipper: (T1, T2, T3) -> R): Single<R> {
    return Single.zip(source1, source2, source3, Function3 { t1, t2, t3 -> zipper.invoke(t1, t2, t3) })
}

fun <T1, T2, T3, T4, R> Singles.zip(source1: SingleSource<out T1>,
                                    source2: SingleSource<out T2>,
                                    source3: SingleSource<out T3>,
                                    source4: SingleSource<out T4>,
                                    zipper: (T1, T2, T3, T4) -> R): Single<R> {
    return Single.zip(source1, source2, source3, source4, Function4 { t1, t2, t3, t4 -> zipper.invoke(t1, t2, t3, t4) })
}
