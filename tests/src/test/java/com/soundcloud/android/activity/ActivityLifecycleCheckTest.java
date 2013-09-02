package com.soundcloud.android.activity;

import static org.junit.Assert.fail;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import org.junit.Test;

import android.app.Activity;
import android.os.Bundle;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ActivityLifecycleCheckTest {

    private static Predicate<ClassPath.ClassInfo> ACTIVITY_CLASS_PREDICATE = new Predicate<ClassPath.ClassInfo>() {
        @Override
        public boolean apply(ClassPath.ClassInfo classInfo) {
            Class<?> clazz = classInfo.load();
            return Activity.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers());
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
            if(!overridesMethod(clazz, "onCreate", Bundle.class) || !overridesMethod(clazz, "onPause") && !overridesMethod(clazz, "onResume")){
                problemClasses.add(clazz);
            }

        }

        if(!problemClasses.isEmpty()){
            Collection<String> classNames = Lists.transform(problemClasses, CLASS_NAME_FUNCTION);
            fail("Activity classes which do not override onCreate, onPause or onResume exist\n" + Joiner.on("\n").join(classNames));
        }

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
