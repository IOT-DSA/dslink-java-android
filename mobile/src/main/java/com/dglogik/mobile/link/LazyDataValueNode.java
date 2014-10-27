package com.dglogik.mobile.link;

import com.dglogik.api.DGContext;
import com.dglogik.api.DGMetaData;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.value.DGValue;

import java.util.TimerTask;

public abstract class LazyDataValueNode extends DataValueNode {
    public boolean setup = false;

    public LazyDataValueNode(String name, DGMetaData metaData) {
        super(name, metaData);
        DGMobileContext.CONTEXT.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!hasSubscriptions() && setup) {
                    onDestroy();
                    setup = false;
                }
            }
        }, 1000, 2000);
    }

    @Override
    public DGValue getValue(DGContext context) {
        if (!setup) {
            onSetup();
            setup = true;
        }
        return super.getValue(context);
    }

    public abstract void onSetup();
    public abstract void onDestroy();
}
