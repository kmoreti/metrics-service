package com.moreti.metricsservice.configs;

import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    OpenTelemetrySdk openTelemetry(ObjectProvider<SdkTracerProvider> tracerProvider,
                                   ObjectProvider<ContextPropagators> propagators,
                                   ObjectProvider<SdkLoggerProvider> loggerProvider,
                                   ObjectProvider<SdkMeterProvider> meterProvider) {
        OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
        tracerProvider.ifAvailable(builder::setTracerProvider);
        propagators.ifAvailable(builder::setPropagators);
        loggerProvider.ifAvailable(builder::setLoggerProvider);
        meterProvider.ifAvailable(builder::setMeterProvider);
        OpenTelemetrySdk openTelemetrySdk = builder.build();
        OpenTelemetryAppender.install(openTelemetrySdk);
        return openTelemetrySdk;
    }

    @Bean
    SdkLoggerProvider otelSdkLoggerProvider(Environment environment, ObjectProvider<LogRecordProcessor> logRecordProcessor) {
        String applicationName = environment.getProperty("spring.application.name", "application");
        Resource springResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName));
        SdkLoggerProviderBuilder builder = SdkLoggerProvider.builder()
                .setResource(Resource.getDefault().merge(springResource));
        logRecordProcessor.orderedStream().forEach(builder::addLogRecordProcessor);
        return builder.build();
    }

    @Bean
    LogRecordProcessor otelLogRecordProcessor() {
        return BatchLogRecordProcessor.builder(
                OtlpGrpcLogRecordExporter.builder()
                        .setEndpoint("http://localhost:4317")
                        .build())
                .build();
    }
}
