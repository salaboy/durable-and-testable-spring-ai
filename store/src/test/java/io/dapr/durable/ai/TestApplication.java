package io.dapr.durable.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {

  public static void main(String[] args) {
    SpringApplication.from(Application::main)
            .with(DaprTestContainersConfig.class)
            .run(args);
  }
}
