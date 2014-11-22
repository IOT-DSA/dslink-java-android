package com.dglogik.mobile;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.text.Html;

import com.dglogik.api.DGAction;
import com.dglogik.api.DGNode;
import com.dglogik.dslink.node.base.BaseNode;

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
        List<Class<?>> list = new ArrayList<Class<?>>();
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

    public static String createNodeTree(DGNode rootDG, int level) {
        BaseNode root = (BaseNode) rootDG;
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= level; i++) {
            builder.append(">");
        }

        builder.append(" ").append(root.getName()).append('\n');


        for (Object it : root.getChildren()) {
            BaseNode node = (BaseNode) it;

            String part = createNodeTree(node, level + 1);
            builder.append(part);

            for (Object i : root.getActions(null)) {
                DGAction action = (DGAction) i;
                String name = action.getName();
                builder.append("@ ").append(name).append("\n");
            }
        }

        return builder.toString();
    }
}
