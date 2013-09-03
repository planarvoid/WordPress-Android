package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.fail;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.soundcloud.android.activity.auth.AbstractLoginActivity;
import com.soundcloud.android.cropimage.MonitoredActivity;
import org.junit.Test;

import android.app.Activity;
import android.os.Bundle;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ActivityLifecycleCheckTest {

    private static final Collection<Class> EXCLUDED_CLASSES = Lists.<Class>newArrayList(MonitoredActivity.class);

    private static Predicate<ClassPath.ClassInfo> ACTIVITY_CLASS_PREDICATE = new Predicate<ClassPath.ClassInfo>() {
        @Override
        public boolean apply(ClassPath.ClassInfo classInfo) {
            Class<?> clazz = classInfo.load();
            return Activity.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) &&
                    !isSubclassOfScActivity(clazz) && !isSubclassOfAbstractLoginActivity(clazz) && !EXCLUDED_CLASSES.contains(clazz);
        }

        private boolean isSubclassOfScActivity(Class<?> clazz) {
            return ScActivity.class.isAssignableFrom(clazz);
        }

        private boolean isSubclassOfAbstractLoginActivity(Class<?> clazz) {
            return AbstractLoginActivity.class.isAssignableFrom(clazz);
        }
    };

    private static Function<Class, String> CLASS_NAME_FUNCTION = new Function<Class, String>() {
        @Override
        public String apply(Class clazz) {
            return clazz.getSimpleName();
        }
    };

    @Test
    public void allActivitiesShouldOverrideExpectedLifecycleMethods() throws IOException, NoSuchMethodException {
        ClassPath classPath  = ClassPath.from(Activity.class.getClassLoader());


        Set<ClassPath.ClassInfo> classes = Sets.filter(classPath.getTopLevelClassesRecursive("com.soundcloud.android"), ACTIVITY_CLASS_PREDICATE);
        List<Class> problemClasses = Lists.newArrayList();

        for(ClassPath.ClassInfo classInfo : classes){
            Class clazz = classInfo.load();
            if(activityDoesNotOverrideLifecycleMethods(clazz)){
                problemClasses.add(clazz);
            }

        }

        if(!problemClasses.isEmpty()){
            Collection<String> classNames = Lists.transform(problemClasses, CLASS_NAME_FUNCTION);
            fail("Activity classes which do not override onCreate, onPause or onResume exist\n" + Joiner.on("\n").join(classNames));
        }

    }

    private boolean activityDoesNotOverrideLifecycleMethods(Class clazz) throws NoSuchMethodException {
        return !overridesMethod(clazz, "onCreate", Bundle.class) || !overridesMethod(clazz, "onPause") || !overridesMethod(clazz, "onResume");
    }

    @Test
    public void scActivityShouldOverrideLifecycleMethods() throws NoSuchMethodException {
        expect(activityDoesNotOverrideLifecycleMethods(ScActivity.class)).toBeFalse();
    }

    @Test
    public void loginActivityShouldOverrideLifecycleMethods() throws NoSuchMethodException {
        expect(activityDoesNotOverrideLifecycleMethods(AbstractLoginActivity.class)).toBeFalse();
    }

    private boolean overridesMethod(Class clazz, String methodName, Class... arguments) throws NoSuchMethodException {
        try{
            clazz.getDeclaredMethod(methodName, arguments);
        }catch(Exception e){
            return false;
        }
        return true;
    }
}
