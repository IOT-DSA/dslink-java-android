package com.dglogik.wear;

public abstract class Action {
    private String name;

    public Action(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void invoke();
}
