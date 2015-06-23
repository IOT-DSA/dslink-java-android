package com.dglogik.mobile;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Html;

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
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) DGMobileContext.CONTEXT.getApplicationContext().getSystemService("usagestats");
            long time = System.currentTimeMillis();
            List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
            if (stats != null) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : stats) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }

                if (!mySortedMap.isEmpty()) {
                    return mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager) DGMobileContext.CONTEXT.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            return am.getRunningTasks(0).get(0).topActivity.getPackageName();
        }
        return null;
    }
}
