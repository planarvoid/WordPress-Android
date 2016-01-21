package com.soundcloud.android.framework.observers;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.LinkedHashMap;

public class ToastObserver {
    private LinkedHashMap<String, TextElement> observedToasts = new LinkedHashMap<>();
    private final ToastObserverRunnable toastObserverRunnable;
    private Thread toastObserverThread;

    public ToastObserver(Han testDriver) {
        this.toastObserverRunnable = new ToastObserverRunnable(testDriver, observedToasts);
        this.toastObserverThread = new Thread(toastObserverRunnable);
    }

    public boolean wasToastObserved(String text) {
        return observedToasts.containsKey(text);
    }

    public void observe() {
        toastObserverThread.start();
    }

    public void stopObserving() {
        toastObserverRunnable.setRunning(false);
    }

    private class ToastObserverRunnable implements Runnable{
        private Han testDriver;
        private boolean running = true;
        private LinkedHashMap<String, TextElement> observedToasts;

        public ToastObserverRunnable(Han testDriver, LinkedHashMap<String, TextElement> observedToasts) {
            this.testDriver = testDriver;
            this.observedToasts = observedToasts;
        }

        public void setRunning(boolean state) {
            running = state;
        }

        @Override
        public void run() {
            while (running) {
                ViewElement element = testDriver.findOnScreenElement(With.id(android.R.id.message));
                if (element.isVisible()) {
                    TextElement toast = new TextElement(element);
                    observedToasts.put(toast.getText(), toast);
                }
            }
        }
    }
}
