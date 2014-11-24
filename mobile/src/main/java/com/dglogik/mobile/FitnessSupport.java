package com.dglogik.mobile;

import com.dglogik.api.BasicMetaData;
import com.dglogik.mobile.link.DataValueNode;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FitnessSupport {
    private DGMobileContext context;
    private DataValueNode heartRateNode;
    private HeartRateListener heartRateListener;

    public FitnessSupport(DGMobileContext context) {
        this.context = context;
    }

    public void initialize() {
        Fitness.SensorsApi.findDataSources(context.googleClient, new DataSourcesRequest.Builder()
                        .setDataSourceTypes(DataSource.TYPE_DERIVED, DataSource.TYPE_RAW)
                        .setDataTypes(DataType.TYPE_HEART_RATE_BPM)
                        .build()
        ).setResultCallback(new ResultCallback<DataSourcesResult>() {
            @Override
            public void onResult(DataSourcesResult dataSourcesResult) {
                List<DataSource> sourceList = dataSourcesResult.getDataSources();
                if (sourceList.isEmpty()) {
                    return;
                }
                DataSource source = sourceList.get(0);
                heartRateListener = new HeartRateListener();
                heartRateNode = new DataValueNode("HeartRate", BasicMetaData.SIMPLE_INT);
                Fitness.SensorsApi.add(context.googleClient, new SensorRequest.Builder()
                                .setDataType(DataType.TYPE_HEART_RATE_BPM)
                                .setFastestRate(1, TimeUnit.SECONDS)
                                .setSamplingRate(3, TimeUnit.SECONDS)
                                .setDataSource(source)
                                .build(),
                        heartRateListener
                );
                context.currentDeviceNode.addChild(heartRateNode);
            }
        });

        context.onCleanup(new Action() {
            @Override
            public void run() {
                Fitness.SensorsApi.remove(context.googleClient, heartRateListener);
            }
        });
    }

    public class HeartRateListener implements OnDataPointListener {
        @Override
        public void onDataPoint(DataPoint dataPoint) {
            float value = dataPoint.getValue(Field.FIELD_BPM).asFloat();
            heartRateNode.update((double) value);
        }
    }
}
