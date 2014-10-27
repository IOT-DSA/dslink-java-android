package com.dglogik.mobile;

import android.content.Context;
import android.preference.CheckBoxPreference;

public class NodeDescriptor {
    private final String name;
    private final String id;
    private final boolean defaultEnabled;

    public NodeDescriptor(String name, String id, boolean defaultEnabled) {
        this.name = name;
        this.id = id;
        this.defaultEnabled = defaultEnabled;
    }

    public NodeDescriptor(String name, String id) {
        this(name, id, false);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CheckBoxPreference createCheckboxPreference(Context context) {
        CheckBoxPreference preference = new CheckBoxPreference(context);
        preference.setTitle(name);
        preference.setKey("providers." + id);
        preference.getExtras().putString("id", id);
        return preference;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }
}
