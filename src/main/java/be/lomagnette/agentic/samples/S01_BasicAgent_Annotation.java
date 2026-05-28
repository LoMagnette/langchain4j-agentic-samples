package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S01 - Basic Agent (Annotation / Declarative)
 * Same agent as {@link S01_BasicAgent} but wired with the declarative
 * annotation API: configuration lives on the interface and the system is
 * created in one shot via {@link AgenticServices#createAgenticSystem}.
 */
public class S01_BasicAgent_Annotation {

    public interface SithNameGenerator {
        @UserMessage("""
                You are a Sith naming council. Transform this boring name into something \
                properly villainous. Return ONLY the Sith name, nothing else: {{jediName}}""")
        @Agent(description = "Transforms a Jedi name into a suitably menacing Sith identity",
                outputKey = "sithName")
        String darken(@V("jediName") String jediName);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var sithNamer = AgenticServices.createAgenticSystem(SithNameGenerator.class, model);

        var sithName = sithNamer.darken("Obi-Wan Kenobi");
        IO.println("Jedi: Obi-Wan Kenobi -> Sith: " + sithName);

        var sithName2 = sithNamer.darken("Luke Skywalker");
        IO.println("Jedi: Luke Skywalker -> Sith: " + sithName2);
    }
}
