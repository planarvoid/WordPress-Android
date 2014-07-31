package com.soundcloud.android.main;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class LifecycleDispatcher {
    private final List<LifecycleComponent> components;

    public LifecycleDispatcher() {
        this.components = new ArrayList<LifecycleComponent>();
    }

    public LifecycleDispatcher add(LifecycleComponent component) {
        this.components.add(component);
        return this;
    }

    public Notifier attach(Activity activity, ActionBarController actionBarController) {
        for (LifecycleComponent component : components) {
            component.attach(activity, actionBarController);
        }
        return new Notifier(components.toArray(new LifecycleComponent[components.size()]));
    }

    public static class Notifier {
        private final LifecycleComponent[] components;

        public Notifier(LifecycleComponent[] components) {
            this.components = components;
        }

        public void onCreate(Bundle bundle) {
            for (LifecycleComponent component : components) {
                component.onCreate(bundle);
            }
        }

        public void onStart() {
            for (LifecycleComponent component : components) {
                component.onStart();
            }
        }

        public void onResume() {
            for (LifecycleComponent component : components) {
                component.onResume();
            }
        }

        public void onPause() {
            for (LifecycleComponent component : components) {
                component.onPause();
            }
        }

        public void onStop() {
            for (LifecycleComponent component : components) {
                component.onStop();
            }
        }

        public void onSaveInstanceState(Bundle bundle) {
            for (LifecycleComponent component : components) {
                component.onSaveInstanceState(bundle);
            }
        }


        public void onRestoreInstanceState(Bundle bundle) {
            for (LifecycleComponent component : components) {
                component.onRestoreInstanceState(bundle);
            }
        }

        public void onDestroy() {
            for (LifecycleComponent component : components) {
                component.onDestroy();
            }
        }
    }
}
