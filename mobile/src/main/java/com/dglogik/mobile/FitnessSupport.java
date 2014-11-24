package com.dglogik.mobile;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGContext;
import com.dglogik.api.DGMetaData;
import com.dglogik.mobile.link.DataValueNode;
import com.dglogik.trend.DGValueTrend;
import com.dglogik.trend.Trend;
import com.dglogik.value.DGValue;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FitnessSupport {
    private DGMobileContext context;
    private DataValueNode heartRateNode;
    private HeartRateListener heartRateListener;
    private DataSource heartRateSource;

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
                heartRateSource = source;
                heartRateListener = new HeartRateListener();
                heartRateNode = new HeartRateNode();
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

    public class HeartRateNode extends DataValueNode {
        public HeartRateNode() {
            super("HeartRate", BasicMetaData.SIMPLE_INT);
        }

        @Override
        public Trend getValueHistory(DGContext cx) {
            List<DGValue> values = new ArrayList<>();
            DataReadResult result = Fitness.HistoryApi.readData(context.googleClient, new DataReadRequest.Builder()
                .setLimit(50)
                .aggregate(heartRateSource, DataType.TYPE_HEART_RATE_BPM)
                .build()
            ).await();
            DataSet dataSet = result.getDataSet(heartRateSource);
            List<DataPoint> points = dataSet.getDataPoints();
            for (DataPoint point : points) {
                Value value = point.getValue(Field.FIELD_BPM);
                DGValue dgValue = DGValue.make((double) value.asFloat());
                values.add(dgValue);
            }
            return new DGValueTrend(values.iterator());
        }
    }
}
