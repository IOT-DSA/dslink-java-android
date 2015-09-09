package com.dglogik.mobile;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Html;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    @NonNull
    public static Thread startThread(Runnable action) {
        Thread thread = new Thread(action);
        thread.start();
        return thread;
    }

    public static Class<?>[] getObjectTypes(Object... inputs) {
        List<Class<?>> list = new ArrayList<>();
        for (Object obj : inputs) {
            list.add(obj.getClass());
        }
        return list.toArray(new Class<?>[list.size()]);
    }

    public static boolean invokeIfExists(Object instance, String name, Object... args) {
        if (instance == null) return false;
        try {
            Method method = instance.getClass().getMethod(name, getObjectTypes(args));

            method.invoke(instance, args);
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (IllegalAccessException ignored) {
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean methodExists(Class<?> clazz, String name, Class<?>... args) {
        try {
            clazz.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    public static void applyDGTheme(Activity activity) {
        ActionBar bar = activity.getActionBar();

        if (bar == null) {
            return;
        }

        bar.setBackgroundDrawable(new ColorDrawable(Constants.BRAND_COLOR));

        if (activity instanceof UsesActionBar) {
            ((UsesActionBar) activity).onActionBarReady(bar);
        }

        activity.setTitle(Html.fromHtml("<font color=\"black\">" + activity.getTitle() + "</font>"));
    }

    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }

    public static String getForegroundActivityPackage() {
        ActivityManager am = (ActivityManager) DSContext.CONTEXT.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> infos = am.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return info.processName;
            }
        }

        return null;
    }

    public static Context context() {
        return DSContext.CONTEXT.getApplicationContext();
    }
}
