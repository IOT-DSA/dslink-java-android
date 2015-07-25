package com.dglogik.mobile;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Html;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

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

        bar.setBackgroundDrawable(new ColorDrawable(DGConstants.BRAND_COLOR));

        if (activity instanceof UsesActionBar) {
            ((UsesActionBar) activity).onActionBarReady(bar);
        }

        activity.setTitle(Html.fromHtml("<font color=\"black\">" + activity.getTitle() + "</font>"));
    }

    @SuppressWarnings({"deprecation", "ResourceType"})
    public static String getForegroundActivityPackage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                return getProcessNew();
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                return getProcessOld();
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static Context context() {
        return DGMobileContext.CONTEXT.getApplicationContext();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("ResourceType")
    public static String getProcessNew() throws Exception {
        String topPackageName = null;
        UsageStatsManager usage = (UsageStatsManager) context().getSystemService("usagestats");
        long time = System.currentTimeMillis();
        List<UsageStats> stats = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        if (stats != null) {
            SortedMap<Long, UsageStats> runningTask = new TreeMap<>();
            for (UsageStats usageStats : stats) {
                runningTask.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (runningTask.isEmpty()) {
                return null;
            }
            topPackageName =  runningTask.get(runningTask.lastKey()).getPackageName();
        }
        return topPackageName;
    }

    @SuppressWarnings("deprecation")
    public static String getProcessOld() throws Exception {
        String topPackageName = null;
        ActivityManager activity = (ActivityManager) context().getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> runningTask = activity.getRunningTasks(1);
        if (runningTask != null) {
            RunningTaskInfo taskTop = runningTask.get(0);
            ComponentName componentTop = taskTop.topActivity;
            topPackageName = componentTop.getPackageName();
        }
        return topPackageName;
    }
}
