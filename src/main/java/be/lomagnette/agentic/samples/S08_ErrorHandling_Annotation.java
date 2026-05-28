package be.lomagnette.agentic.samples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.agent.ErrorContext;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.declarative.ErrorHandler;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;


/**
 * S08 - Error Handling (Annotation / Declarative)
 * Same recovery semantics as {@link S08_ErrorHandling}: when the {@code darken}
 * agent fails, retry with a fallback Jedi name. With the annotation API the
 * handler is a static method annotated with {@link ErrorHandler} on the
 * pipeline interface.
 */
public class S08_ErrorHandling_Annotation {

    public static class JediName implements TypedKey<String> { }
    public static class JediProfile implements TypedKey<String> { }
    public static class SithName implements TypedKey<String> { }
    public static class Announcement implements TypedKey<String> { }

    public interface JediProfiler {
        @Agent(description = "Creates a brief profile of a Jedi before their fall to the Dark Side",
                typedOutputKey = JediProfile.class)
        @UserMessage("""
                You are a Jedi archivist. Write a brief profile (2-3 sentences) of this Jedi, \
                including their strengths and the weakness that could lead to their downfall.

                Jedi: {{JediName}}""")
        String profile(@K(JediName.class) String name);
    }

    public interface SithNamer {
        @Agent(name = "darken",
                description = "Transforms a Jedi name into a menacing Sith name",
                typedOutputKey = SithName.class)
        @UserMessage("""
                You are a Sith naming council. Transform this Jedi's name into a properly \
                villainous Sith name. Return ONLY the Sith name, nothing else.

                Jedi: {{JediName}}""")
        String darken(@K(JediName.class) String name);
    }

    public interface SithAnnouncer {
        @Agent(description = "Dramatically announces the rise of a new Sith Lord",
                typedOutputKey = Announcement.class)
        @UserMessage("""
                You are Emperor Palpatine. Dramatically announce the rise of this new Sith Lord \
                to the galaxy. Include their new name and a dark prophecy. \
                Keep it to 2-3 sentences.

                Sith name: {{sithName}}""")
        String announce(@K(SithName.class) String sithName);
    }

    public interface SithPipeline {
        @SequenceAgent(description = "Transforms a Jedi into a Sith Lord with profiling, naming, and announcement",
                typedOutputKey = Announcement.class,
                subAgents = { JediProfiler.class, SithNamer.class, SithAnnouncer.class })
        String transform(@K(JediName.class) String jediName);

        @ErrorHandler
        static ErrorRecoveryResult onError(ErrorContext errorContext) {
            IO.println("❌[ERROR HANDLER] Agent '" + errorContext.agentName()
                    + "' failed: " + errorContext.exception().getMessage());

            if (errorContext.agentName().equals("darken")) {
                IO.println("❌ [ERROR HANDLER] Retrying with fallback name 'Dave'...");
                errorContext.agenticScope().writeState(JediName.class, "Dave");
                return ErrorRecoveryResult.retry();
            }

            IO.println("❌[ERROR HANDLER] Unrecoverable error, re-throwing.");
            return ErrorRecoveryResult.result("Emperor Palpatine: I have no idea what happened but he's dead.");
        }
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var pipeline = AgenticServices.createAgenticSystem(SithPipeline.class, model);

        IO.println("=== Sith Pipeline: Transforming a Jedi ===");
        String jediName = "Anakin Skywalker";
        IO.println("Input Jedi: " + jediName);
        IO.println();

        String announcement = pipeline.transform(jediName);

        IO.println("Result: " + announcement);
    }
}
