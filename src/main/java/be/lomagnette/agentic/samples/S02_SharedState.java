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
 * Two agents communicate through shared state (AgenticScope) without
 * knowing about each other. A TypedKey provides type-safe access to
 * scope values. The sequential workflow chains them together.
 * Key concept: @V binds external input (first agent), @K reads from
 * the AgenticScope (downstream agents). @K only works INSIDE a pipeline.
 *   interceptor (@V) --> scope["interceptedTransmission"] --> classifier (@K) --> scope["threatLevel"]
 */
public class S02_SharedState {

    public static class InterceptedTransmission implements TypedKey<String> { }

    public static class ThreatLevel implements TypedKey<String> { }

    public interface TransmissionInterceptor {
        @Agent("Intercepts an Imperial transmission and decodes it into plain text")
        @UserMessage("""
                You are a Rebel Alliance communications officer. \
                Decode this scrambled Imperial transmission into a clear, one-sentence summary \
                of what the Empire is planning: {{scrambled}}""")
        String intercept(@V("scrambled") String scrambled);
    }

    public interface ThreatClassifier {
        @Agent("Classifies the threat level of an intercepted Imperial transmission")
        @UserMessage("""
                You are a Rebel Alliance intelligence analyst. \
                Classify the threat level of this intercepted Imperial transmission as \
                LOW, MEDIUM, HIGH, or CRITICAL. Respond with the level and a brief justification.

                Transmission: {{InterceptedTransmission}}""")
        String classify(@K(InterceptedTransmission.class) String transmission);
    }

    public interface IntelPipeline {
        @Agent("Decodes and classifies an intercepted Imperial transmission")
        String analyze(@V("scrambled") String scrambled);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

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

        IntelPipeline pipeline = AgenticServices
                .sequenceBuilder(IntelPipeline.class)
                .subAgents(interceptor, classifier)
                .outputKey(ThreatLevel.class)
                .build();

        String scrambledInput = "Death Star... operational... Alderaan... target confirmed... fire when ready";

        IO.println("=== Rebel Intelligence Report ===");
        IO.println("Scrambled input: " + scrambledInput);
        IO.println();

        String threatAssessment = pipeline.analyze(scrambledInput);

        IO.println("Threat Assessment: " + threatAssessment);
    }
}
