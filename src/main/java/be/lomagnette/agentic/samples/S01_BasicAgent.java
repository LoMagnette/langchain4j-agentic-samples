package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S01 - Basic Agent
 *
 * The simplest possible agentic setup: a single agent that transforms
 * a Jedi name into a Sith name. Demonstrates @Agent, @UserMessage,
 * and AgenticServices.agentBuilder().
 */
public class S01_BasicAgent {

    // Define the agent interface - @Agent describes what this agent does,
    // @UserMessage provides the prompt template, @V binds method parameters.
    public interface SithNameGenerator {
        @UserMessage("""
                You are a Sith naming council. Transform this boring name into something \
                properly villainous. Return ONLY the Sith name, nothing else: {{jediName}}""")
        @Agent("Transforms a Jedi name into a suitably menacing Sith identity")
        String darken(@V("jediName") String jediName);
    }

    public static void main(String... args) {
        // Build the chat model - uses Ollama with llama3.2:1b locally
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        // Build the agent from the interface using AgenticServices.
        // outputKey stores the result in the AgenticScope under "sithName".
        // This is relevant when this agent participates in a workflow scope
        // (e.g., a sequential or loop pipeline) so downstream agents can read
        // the result via @K. For a standalone call like this, it has no effect.
        SithNameGenerator sithNamer = AgenticServices
                .agentBuilder(SithNameGenerator.class)
                .chatModel(model)
                .outputKey("sithName")
                .build();

        // Invoke the agent - it calls the LLM behind the scenes
        String sithName = sithNamer.darken("Obi-Wan Kenobi");
        IO.println("Jedi: Obi-Wan Kenobi -> Sith: " + sithName);

        // Try another one
        String sithName2 = sithNamer.darken("Luke Skywalker");
        IO.println("Jedi: Luke Skywalker -> Sith: " + sithName2);
    }
}
