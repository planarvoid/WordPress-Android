package com.soundcloud.android.presentation;

import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

public abstract class PageTransformer<T, R> implements Func1<List<T>, List<R>> {

    @Override
    public List<R> call(List<T> input) {
        final List<R> output = new ArrayList<>(input.size());
        for (T item : input) {
            output.add(map(item));
        }
        return output;
    }

    public abstract R map(T input);
}
