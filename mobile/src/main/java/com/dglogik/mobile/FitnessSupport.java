package com.dglogik.mobile;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGContext;
import com.dglogik.mobile.link.DataValueNode;
import com.dglogik.trend.Trend;
import com.dglogik.trend.Trends;
import com.dglogik.value.DGValue;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FitnessSupport {
    private DGMobileContext context;
    private DataValueNode heartRateNode;

    public FitnessSupport(DGMobileContext context) {
        this.context = context;
    }

    public void initialize() {
        Fitness.RecordingApi.subscribe(context.googleClient, DataType.TYPE_HEART_RATE_BPM);
        context.poller(new Action() {
            @Override
            public void run() {
                Fitness.HistoryApi.readData(context.googleClient, new DataReadRequest.Builder()
                        .setLimit(1)
                        .setTimeRange(1000000000000L, System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .read(DataType.TYPE_HEART_RATE_BPM)
                        .build()).setResultCallback(new ResultCallback<DataReadResult>() {
                    @Override
                    public void onResult(DataReadResult dataReadResult) {
                        if (!dataReadResult.getStatus().isSuccess()) {
                            return;
                        }

                        if (heartRateNode == null) {
                            heartRateNode = new HeartRateNode();
                            context.currentDeviceNode.addChild(heartRateNode);
                        }

                        List<DataPoint> points = dataReadResult.getDataSet(DataType.TYPE_HEART_RATE_BPM)
                                .getDataPoints();

                        if (points.isEmpty()) {
                            DGMobileContext.log("Heart Rate Data Points are empty.");
                            return;
                        }

                        DataPoint point = points.get(0);

                        Value value = point.getValue(Field.FIELD_BPM);
                        DGValue dgValue = DGValue.make((double) value.asFloat());
                        dgValue.setTimestamp(point.getTimestamp(TimeUnit.MILLISECONDS));
                        heartRateNode.setValue(dgValue);
                    }
                });
            }
        }).poll(TimeUnit.SECONDS, 15, false);

        context.onCleanup(new Action() {
            @Override
            public void run() {
                Fitness.RecordingApi.unsubscribe(context.googleClient, DataType.TYPE_HEART_RATE_BPM);
            }
        });
    }

    public class HeartRateNode extends DataValueNode {
        public HeartRateNode() {
            super("HeartRate", BasicMetaData.SIMPLE_INT);
            setDisplayName("Heart Rate");
            setFormatter(new DisplayFormatter() {
                @Override
                public String handle(DGValue dgValue) {
                    return "" + dgValue.toDouble() + " bpm";
                }
            });
        }

        @Override
        public Trend getValueHistory(DGContext cx) {
            DGMobileContext.log("Fetching Heart Rate History");
            List<DGValue> values = new ArrayList<>();
            cx.getRollup().makeRollup();
            DataReadResult result = Fitness.HistoryApi.readData(context.googleClient, new DataReadRequest.Builder()
                            .setTimeRange(cx.getTimeRange().getStart(), cx.getTimeRange().getEnd(), TimeUnit.MILLISECONDS)
                            .enableServerQueries()
                            .read(DataType.TYPE_HEART_RATE_BPM)
                            .build()
            ).await(5000, TimeUnit.MILLISECONDS);
            DGMobileContext.log("Got Heart Rate Results");
            if (!result.getStatus().isSuccess() || result.getStatus().isCanceled() || result.getStatus().isInterrupted()) {
                DGMobileContext.log("Failed to fetch heart rate history: " + result.getStatus().getStatusMessage());
                return Trends.make(new ArrayList<DGValue>(), BasicMetaData.SIMPLE_INT, cx);
            }
            DataSet dataSet = result.getDataSet(DataType.TYPE_HEART_RATE_BPM);
            if (dataSet.getDataPoints().isEmpty()) {
                DGMobileContext.log("Warning: Heart Rate Data Points are empty.");
            }
            List<DataPoint> points = dataSet.getDataPoints();
            for (DataPoint point : points) {
                Value value = point.getValue(Field.FIELD_BPM);
                double bpm = (double) value.asFloat();
                DGMobileContext.log("Got Heart Rate Value: " + bpm + " (timestamp: " + point.getTimestamp(TimeUnit.MILLISECONDS) + ")");
                DGValue dgValue = DGValue.make(bpm);
                dgValue.setTimestamp(point.getTimestamp(TimeUnit.MILLISECONDS));
                values.add(dgValue);
            }
            return Trends.make(values, BasicMetaData.SIMPLE_INT, cx);
        }

        @Override
        public boolean hasValueHistory(DGContext cx) {
            return true;
        }
    }
}
