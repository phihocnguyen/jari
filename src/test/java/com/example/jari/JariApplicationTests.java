package com.example.jari;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"management.otlp.metrics.export.enabled=false",
	"management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:1/v1/traces"
})
@RabbitAvailable
class JariApplicationTests {

	@Test
	void contextLoads() {
	}

}
