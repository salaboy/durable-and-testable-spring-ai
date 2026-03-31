package io.dapr.durable.ai;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ActivityCallCounter {

  private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

  public void increment(String activityName) {
    counters.computeIfAbsent(activityName, k -> new AtomicInteger(0)).incrementAndGet();
  }

  public int getCount(String activityName) {
    AtomicInteger counter = counters.get(activityName);
    return counter != null ? counter.get() : 0;
  }

  public void resetAll() {
    counters.clear();
  }
}
