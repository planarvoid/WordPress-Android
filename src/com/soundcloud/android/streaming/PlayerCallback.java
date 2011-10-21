package com.soundcloud.android.streaming;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlayerCallback implements Future<ByteBuffer> {
    StreamItem scStreamItem;
    Range byteRange;
    ByteBuffer byteBuffer;
    boolean ready = false;

    public PlayerCallback(StreamItem scStreamItem, Range byteRange) {
        this.scStreamItem = scStreamItem;
        this.byteRange = byteRange;
    }

    public void setByteBuffer(ByteBuffer byteBuffer){
        this.byteBuffer = byteBuffer;
        ready = true;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return ready;
    }

    @Override
    public ByteBuffer get() throws InterruptedException, ExecutionException {
        synchronized (this) {
        while (!ready) {
            try {wait(); } catch (InterruptedException e) { return null; }
        }
        }
        return byteBuffer;
    }

    @Override
    public ByteBuffer get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new RuntimeException("not implemented");
    }
}
