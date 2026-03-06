package be.lomagnette.agentic.samples;


import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S02 - Shared State with AgenticScope and TypedKey
 *
 * Two agents communicate through shared state (AgenticScope) without
 * knowing about each other. A TypedKey provides type-safe access to
 * scope values. The sequential workflow chains them together.
 *
 * Key concept: @V binds external input (first agent), @K reads from
 * the AgenticScope (downstream agents). @K only works INSIDE a pipeline.
 *
 *   interceptor (@V) --> scope["interceptedTransmission"] --> classifier (@K) --> scope["threatLevel"]
 */
public class S02_SharedState {

    // TypedKey for type-safe scope access - this key holds the intercepted transmission text
    public static class InterceptedTransmission implements TypedKey<String> { }

    // TypedKey for the threat level result
    public static class ThreatLevel implements TypedKey<String> { }

    // Agent 1: Intercepts and decodes a transmission.
    // @V("scrambled") binds external input (method parameter) - used for the first agent in a chain.
    public interface TransmissionInterceptor {
        @Agent("Intercepts an Imperial transmission and decodes it into plain text")
        @UserMessage("""
                You are a Rebel Alliance communications officer. \
                Decode this scrambled Imperial transmission into a clear, one-sentence summary \
                of what the Empire is planning: {{scrambled}}""")
        String intercept(@V("scrambled") String scrambled);
    }

    // Agent 2: Reads the decoded transmission from scope via @K and classifies the threat.
    // @K reads from the AgenticScope - the value is populated by Agent 1's outputKey.
    // IMPORTANT: @K only works when this agent runs inside a workflow (sequence, loop, etc.)
    // because the AgenticScope is created and managed by the workflow.
    public interface ThreatClassifier {
        @Agent("Classifies the threat level of an intercepted Imperial transmission")
        @UserMessage("""
                You are a Rebel Alliance intelligence analyst. \
                Classify the threat level of this intercepted Imperial transmission as \
                LOW, MEDIUM, HIGH, or CRITICAL. Respond with the level and a brief justification.

                Transmission: {{InterceptedTransmission}}""")
        String classify(@K(InterceptedTransmission.class) String transmission);
    }

    // Typed pipeline interface - lets us invoke the full sequence with a single call.
    // The method parameter uses @V to receive external input (the scrambled message).
    // Internally, the pipeline manages the AgenticScope so @K bindings work.
    public interface IntelPipeline {
        @Agent("Decodes and classifies an intercepted Imperial transmission")
        String analyze(@V("scrambled") String scrambled);
    }

    public static void main(String... args) {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        // Build the two agents - each writes its output to a different scope key.
        // outputKey determines where each agent's return value is stored in AgenticScope.
        TransmissionInterceptor interceptor = AgenticServices
                .agentBuilder(TransmissionInterceptor.class)
                .chatModel(model)
                .outputKey(InterceptedTransmission.class)  // writes decoded text here
                .build();

        ThreatClassifier classifier = AgenticServices
                .agentBuilder(ThreatClassifier.class)
                .chatModel(model)
                .outputKey(ThreatLevel.class)  // writes threat assessment here
                .build();

        // Chain them in a typed sequence.
        // The AgenticScope flows data automatically:
        //   1. interceptor receives "scrambled" via @V, writes result to "InterceptedTransmission"
        //   2. classifier reads "InterceptedTransmission" via @K, writes result to "ThreatLevel"
        IntelPipeline pipeline = AgenticServices
                .sequenceBuilder(IntelPipeline.class)
                .subAgents(interceptor, classifier)
                .outputKey(ThreatLevel.class)
                .build();

        // AgenticScope API reference (used internally by the pipeline):
        //   scope.writeState("key", value)   - write a value to scope
        //   scope.readState("key")           - read a value from scope
        //   scope.readState("key", default)  - read with a default
        //   scope.hasState("key")            - check if a key exists

        // --- Invoke the pipeline ---
        // One call runs both agents. The scope carries data between them.
        String scrambledInput = "Death Star... operational... Alderaan... target confirmed... fire when ready";

        IO.println("=== Rebel Intelligence Report ===");
        IO.println("Scrambled input: " + scrambledInput);
        IO.println();

        String threatAssessment = pipeline.analyze(scrambledInput);

        IO.println("Threat Assessment: " + threatAssessment);
    }
}
