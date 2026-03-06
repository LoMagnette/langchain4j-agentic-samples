package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S01 - Basic Agent
 * The simplest possible agentic setup: a single agent that transforms
 * a Jedi name into a Sith name. Demonstrates @Agent, @UserMessage,
 * and AgenticServices.agentBuilder().
 */
public class S01_BasicAgent {

    public interface SithNameGenerator {
        @UserMessage("""
                You are a Sith naming council. Transform this boring name into something \
                properly villainous. Return ONLY the Sith name, nothing else: {{jediName}}""")
        @Agent("Transforms a Jedi name into a suitably menacing Sith identity")
        String darken(@V("jediName") String jediName);
    }

    public static void main(String... args) {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        SithNameGenerator sithNamer = AgenticServices
                .agentBuilder(SithNameGenerator.class)
                .chatModel(model)
                .outputKey("sithName")
                .build();

        String sithName = sithNamer.darken("Obi-Wan Kenobi");
        IO.println("Jedi: Obi-Wan Kenobi -> Sith: " + sithName);

        String sithName2 = sithNamer.darken("Luke Skywalker");
        IO.println("Jedi: Luke Skywalker -> Sith: " + sithName2);
    }
}
