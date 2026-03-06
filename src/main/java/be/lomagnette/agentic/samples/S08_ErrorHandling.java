package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;


/**
 * S08 - Error Handling: Sith Name Generator with Recovery
 * Demonstrates error handling in agentic workflows. A sequence of agents
 * transforms a Jedi into a Sith, but things can go wrong:
 *   jediProfiler -> darken (Sith namer) -> sithAnnouncer
 * The error handler:
 *   - If the "darken" agent fails (e.g., it refuses to process the name),
 *     retry with a fallback name written to scope
 *   - For any other agent failure, throw the exception
 */
public class S08_ErrorHandling {

    public static class JediName implements TypedKey<String> { }
    public static class JediProfile implements TypedKey<String> { }
    public static class SithName implements TypedKey<String> { }
    public static class Announcement implements TypedKey<String> { }

    public interface JediProfiler {
        @Agent("Creates a brief profile of a Jedi before their fall to the Dark Side")
        @UserMessage("""
                You are a Jedi archivist. Write a brief profile (2-3 sentences) of this Jedi, \
                including their strengths and the weakness that could lead to their downfall.

                Jedi: {{JediName}}""")
        String profile(@V("JediName") String name);
    }

    public interface SithNamer {
        @Agent("Transforms a Jedi name into a menacing Sith name")
        @UserMessage("""
                You are a Sith naming council. Transform this Jedi's name into a properly \
                villainous Sith name. Return ONLY the Sith name, nothing else.

                Jedi: {{JediName}}""")
        String darken(@K(JediName.class) String name);
    }

    public interface SithAnnouncer {
        @Agent("Dramatically announces the rise of a new Sith Lord")
        @UserMessage("""
                You are Emperor Palpatine. Dramatically announce the rise of this new Sith Lord \
                to the galaxy. Include their new name and a dark prophecy. \
                Keep it to 2-3 sentences.

                Sith name: {{SithName}}""")
        String announce(@K(SithName.class) String sithName);
    }

    public interface SithPipeline {
        @Agent("Transforms a Jedi into a Sith Lord with profiling, naming, and announcement")
        String transform(@V("JediName") String jediName);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        JediProfiler jediProfiler = AgenticServices
                .agentBuilder(JediProfiler.class)
                .chatModel(model)
                .outputKey(JediProfile.class)
                .build();

        SithNamer sithNamer = AgenticServices
                .agentBuilder(SithNamer.class)
                .chatModel(model)
                .outputKey(SithName.class)
                .build();

        SithAnnouncer sithAnnouncer = AgenticServices
                .agentBuilder(SithAnnouncer.class)
                .chatModel(model)
                .outputKey(Announcement.class)
                .build();

        SithPipeline pipeline = AgenticServices
                .sequenceBuilder(SithPipeline.class)
                .subAgents(jediProfiler, sithNamer, sithAnnouncer)
                .outputKey(Announcement.class)
                .errorHandler(errorContext -> {
                    IO.println("[ERROR HANDLER] Agent '" + errorContext.agentName()
                            + "' failed: " + errorContext.exception().getMessage());

                    if (errorContext.agentName().equals("darken")) {
                        IO.println("[ERROR HANDLER] Retrying with fallback name 'Dave'...");
                        errorContext.agenticScope().writeState("JediName", "Dave");
                        return ErrorRecoveryResult.retry();
                    }

                    IO.println("[ERROR HANDLER] Unrecoverable error, re-throwing.");
                    return ErrorRecoveryResult.throwException();
                })
                .build();

        IO.println("=== Sith Pipeline: Transforming a Jedi ===");
        String jediName = "Anakin Skywalker";
        IO.println("Input Jedi: " + jediName);
        IO.println();

        String announcement = pipeline.transform(jediName);

        IO.println("Result: " + announcement);
    }
}
