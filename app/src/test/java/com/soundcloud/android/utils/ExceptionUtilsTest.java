package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class ExceptionUtilsTest {

    @Test
    public void shouldBeCausedByOOMWhenThrowableIsAnOOM() {
        Throwable e = new OutOfMemoryError();

        expect(ExceptionUtils.isCausedByOutOfMemory(e)).toBeTrue();
    }

    @Test
    public void shouldBeCausedByOOMWhenThrowableHasOOMInChain() {
        Exception e = new Exception();
        Exception e1 = new Exception();
        e.initCause(e1);
        e1.initCause(new OutOfMemoryError());

        expect(ExceptionUtils.isCausedByOutOfMemory(e)).toBeTrue();
    }

    @Test
    public void shouldNotBeCausedByOOMIfThrowableIsNotAnOOM() {
        Exception e = new Exception();

        expect(ExceptionUtils.isCausedByOutOfMemory(e)).toBeFalse();
    }

    @Test
    public void shouldNotBeCausedByOOMIfThrowableHasNoOOMInChain() {
        Exception e = new Exception();
        e.initCause(new Exception());

        expect(ExceptionUtils.isCausedByOutOfMemory(e)).toBeFalse();
    }

}
