package com.soundcloud.android.framework.observers;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.ArrayDeque;

public class ToastObserver {
    private final ToastObserverRunnable toastObserverRunnable;
    private ArrayDeque<TextElement> toasts = new ArrayDeque<>();
    private Thread toastObserverThread;

    public ToastObserver(Han testDriver) {
        this.toastObserverRunnable = new ToastObserverRunnable(testDriver);
        this.toastObserverThread = new Thread(toastObserverRunnable);
    }

    public boolean assertToastOccurred(String text) {
        for (TextElement element : toasts) {
            if (element.getText().equals(text)) {
                return true;
            }
        }
        return false;
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

        public ToastObserverRunnable(Han testDriver) {
            this.testDriver = testDriver;
        }

        public void setRunning(boolean state) {
            running = state;
        }

        //TODO Refactor this later
        @Override
        public void run() {
            toasts = new ArrayDeque<>();
            while (running) {
                ViewElement element = testDriver.findElement(With.id(android.R.id.message));
                if (element.isVisible()) {
                    TextElement toast = new TextElement(element);
                    if (!toasts.isEmpty()) {
                        boolean match = false;
                        for (TextElement textElement : toasts) {
                            if (textElement.getText().equals(toast.getText())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            toasts.add(toast);
                        }
                    }
                    else {
                        toasts.add(toast);
                    }
                }
            }
        }
    }
}
