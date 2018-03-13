package com.criteo.hadoop.garmadon.benchmark;

import com.criteo.hadoop.garmadon.event.proto.DataAccessEventProtos;
import com.criteo.hadoop.garmadon.schema.events.*;
import com.google.protobuf.InvalidProtocolBufferException;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(3)
@Fork(1)
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 15, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class GarmadonEventSerializationBenchmark {

    Header header;
    byte[] headerByte;

    FsEvent fsEvent;
    byte[] fsEventByte;

    PathEvent pathEvent;
    byte[] pathEventByte;

    StateEvent stateEvent;
    byte[] stateEventByte;

    MetricEvent metricsEvent;
    byte[] metricsEventByte;

    @Setup()
    public void setUp() {
        header = Header.newBuilder()
                .withTag(Header.Tag.YARN_APPLICATION.toString())
                .withHostname("a4-5d-36-fd-71-30.hpc.criteo.preprod")
                .withApplicationID("application_1518449687010_960473")
                .withApplicationName("Transformer job: EVANEOSDE.29291.110132/20180216.125438.f9991803-3f05-446b-a8b1-2c36150eaaa8.F")
                .withAppAttemptID("appattempt_1518778939217_0068_000001")
                .withUser("lakeprobes")
                .withContainerID("container_e619_1518778939217_0068_01_000205")
                .build();
        headerByte = header.serialize();

        long timestamp = System.currentTimeMillis();

        fsEvent = new FsEvent(timestamp,
                "/tmp/bidata/testpatch/teragen/_temporary_src/1/task_1518778939217_0144_m_000025",
                "/tmp/bidata/testpatch/teragen/_temporary/1/task_1518778939217_0144_m_000025",
                FsEvent.Action.RENAME, "hdfs://root");
        fsEventByte = fsEvent.serialize();

        pathEvent = new PathEvent(timestamp,
                "viewfs://root/tmp/bidata/testpatch/terasort",
                PathEvent.Type.INPUT);
        pathEventByte = pathEvent.serialize();

        stateEvent = new StateEvent(timestamp, StateEvent.State.END.toString());
        stateEventByte = stateEvent.serialize();

        metricsEvent = new MetricEvent(timestamp);
        metricsEvent.addMetric(DataAccessEventProtos.Metric.newBuilder().setName("events-in-error").setValue(10).build());
        metricsEvent.addMetric(DataAccessEventProtos.Metric.newBuilder().setName(" greetings-in-error").setValue(0).build());
        metricsEvent.addMetric(DataAccessEventProtos.Metric.newBuilder().setName("events-received").setValue(1500).build());
        metricsEvent.addMetric(DataAccessEventProtos.Metric.newBuilder().setName("greetings-received").setValue(150).build());
        metricsEventByte = metricsEvent.serialize();
    }

    @Benchmark
    public Object test_serialize_FsEvent() {
        return fsEvent.serialize();
    }

    @Benchmark
    public Object test_deserialize_FsEvent() throws InvalidProtocolBufferException {
        return DataAccessEventProtos.FsEvent.parseFrom(fsEventByte);
    }


    @Benchmark
    public Object test_serialize_MetricEvent() {
        return metricsEvent.serialize();
    }

    @Benchmark
    public Object test_deserialize_MetricEvent() throws InvalidProtocolBufferException {
        return DataAccessEventProtos.MetricEvent.parseFrom(metricsEventByte);
    }


    @Benchmark
    public Object test_serialize_StateEvent() {
        return stateEvent.serialize();
    }

    @Benchmark
    public Object test_deserialize_StateEvent() throws InvalidProtocolBufferException {
        return DataAccessEventProtos.StateEvent.parseFrom(stateEventByte);
    }


    @Benchmark
    public Object test_serialize_PathEvent() {
        return pathEvent.serialize();
    }

    @Benchmark
    public Object test_deserialize_PathEvent() throws InvalidProtocolBufferException {
        return DataAccessEventProtos.PathEvent.parseFrom(pathEventByte);
    }

    @Benchmark
    public Object test_serialize_Header() {
        return header.serialize();
    }

    @Benchmark
    public Object test_deserialize_Header() throws InvalidProtocolBufferException {
        return DataAccessEventProtos.Header.parseFrom(headerByte);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(GarmadonEventSerializationBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
