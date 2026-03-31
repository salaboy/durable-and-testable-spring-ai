package io.dapr.durable.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CountingChatModel implements ChatModel {

  private final AtomicInteger callCount = new AtomicInteger(0);

  public int getCallCount() {
    return callCount.get();
  }

  public void resetCount() {
    callCount.set(0);
  }

  @Override
  public ChatResponse call(Prompt prompt) {
    callCount.incrementAndGet();
    String contents = prompt.getContents();
    String response = determineResponse(contents);
    return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
  }

  private String determineResponse(String contents) {
    if (contents.contains("select the most appropriate support team")) {
      return """
          {"reasoning":"Routing to billing based on content","selection":"billing"}""";
    }
    if (contents.contains("Analyze this task and break it down")) {
      return """
          {"analysis":"Breaking down into formal and casual approaches","tasks":[{"type":"formal","description":"Write a formal version"},{"type":"casual","description":"Write a casual version"}]}""";
    }
    if (contents.contains("Your goal is to complete the task")) {
      return """
          {"thoughts":"Implementing the solution","response":"public class Solution {}"}""";
    }
    if (contents.contains("Evaluate this code implementation")) {
      return """
          {"evaluation":"PASS","feedback":"Code meets all criteria"}""";
    }
    return "mock response";
  }
}