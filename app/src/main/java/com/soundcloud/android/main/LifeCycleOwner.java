package com.soundcloud.android.main;

public interface LifeCycleOwner<ComponentT> {

    /**
     * Subclasses call this to attach an individual component to the owner's life-cycle. It us up the owner to
     * provide a suitable implementation.
     */
    void addLifeCycleComponent(ComponentT lifeCycleComponent);

    /**
     * Subclasses implement this to make their calls to {@link #addLifeCycleComponent}. The owner then ensures that
     * this method is called before any life-cycle methods are being dispatched.
     */
    void addLifeCycleComponents();
}
