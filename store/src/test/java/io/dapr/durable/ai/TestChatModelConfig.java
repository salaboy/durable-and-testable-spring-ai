package io.dapr.durable.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestChatModelConfig {

  @Bean
  public CountingChatModel countingChatModel() {
    return new CountingChatModel();
  }
}
