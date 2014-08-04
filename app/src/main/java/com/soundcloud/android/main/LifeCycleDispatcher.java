package com.soundcloud.android.main;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class LifeCycleDispatcher {
    private final List<LifeCycleComponent> components;

    public LifeCycleDispatcher() {
        this.components = new ArrayList<LifeCycleComponent>();
    }

    public LifeCycleDispatcher add(LifeCycleComponent component) {
        this.components.add(component);
        return this;
    }

    public Notifier attach(Activity activity, ActionBarController actionBarController) {
        for (LifeCycleComponent component : components) {
            component.attach(activity, actionBarController);
        }
        return new Notifier(components.toArray(new LifeCycleComponent[components.size()]));
    }

    public static class Notifier {
        private final LifeCycleComponent[] components;

        public Notifier(LifeCycleComponent[] components) {
            this.components = components;
        }

        public void onCreate(Bundle bundle) {
            for (LifeCycleComponent component : components) {
                component.onCreate(bundle);
            }
        }

        public void onNewIntent(Intent intent) {
            for (LifeCycleComponent component : components) {
                component.onNewIntent(intent);
            }
        }

        public void onStart() {
            for (LifeCycleComponent component : components) {
                component.onStart();
            }
        }

        public void onResume() {
            for (LifeCycleComponent component : components) {
                component.onResume();
            }
        }

        public void onPause() {
            for (LifeCycleComponent component : components) {
                component.onPause();
            }
        }

        public void onStop() {
            for (LifeCycleComponent component : components) {
                component.onStop();
            }
        }

        public void onSaveInstanceState(Bundle bundle) {
            for (LifeCycleComponent component : components) {
                component.onSaveInstanceState(bundle);
            }
        }


        public void onRestoreInstanceState(Bundle bundle) {
            for (LifeCycleComponent component : components) {
                component.onRestoreInstanceState(bundle);
            }
        }

        public void onDestroy() {
            for (LifeCycleComponent component : components) {
                component.onDestroy();
            }
        }
    }
}
