package io.nosidelines;

import com.google.pubsub.v1.PubsubMessage;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import org.apache.flink.api.common.serialization.Encoder;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSource;
import org.apache.flink.streaming.connectors.gcp.pubsub.common.PubSubDeserializationSchema;
import org.apache.flink.streaming.connectors.gcp.pubsub.emulator.PubSubSubscriberFactoryForEmulator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Duration;


class StringDeserializer implements PubSubDeserializationSchema<String>, SerializationSchema<String> {
    @Override
    public String deserialize(PubsubMessage message) throws IOException {
        return message.getData().toStringUtf8();
    }

    @Override
    public boolean isEndOfStream(String string) {
        return false;
    }

    @Override
    public TypeInformation<String> getProducedType() {
        return TypeInformation.of(String.class);
    }

    @Override
    public byte[] serialize(String string) {
        return string.getBytes();
    }
}

public class AnalyticsStreamBatcherJob {
	public static void main(String[] args) throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		String hostAndPort = "pubsub:8085";
        PubSubDeserializationSchema<String> deserializationSchema = new StringDeserializer();

        PubSubSource<String> source = PubSubSource.newBuilder()
                .withDeserializationSchema(deserializationSchema)
                .withProjectName("local")
                .withSubscriptionName("sub-1")
                .withPubSubSubscriberFactory(new PubSubSubscriberFactoryForEmulator(hostAndPort, "local", "sub-1", 10, Duration.ofSeconds(15), 100))
                .build();

        final FileSink<String> sink = FileSink
                .forRowFormat(new Path("file:///opt/flink/output"), new SimpleStringEncoder<String>())
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(Duration.ofMinutes(5))
                                .withInactivityInterval(Duration.ofMinutes(5))
                                .withMaxPartSize(MemorySize.ofMebiBytes(1024))
                                .build())
                .withOutputFileConfig(
                        OutputFileConfig.builder()
                                .withPartSuffix(".json")
                                .build()
                )
                .build();

		env.addSource(source).sinkTo(sink);
        env.enableCheckpointing(1000);
		env.execute("Fraud Detection");
	}
}

