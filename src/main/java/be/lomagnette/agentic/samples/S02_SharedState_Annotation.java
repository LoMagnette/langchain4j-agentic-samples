package be.lomagnette.agentic.samples;


import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

/**
 * S02 - Shared State with AgenticScope and TypedKey (Annotation / Declarative)
 * Same wiring as {@link S02_SharedState}, but the pipeline is described
 * directly on the {@code IntelPipeline} interface with {@link SequenceAgent}.
 * Each sub-agent declares its outputKey through {@link Agent#typedOutputKey()}.
 */
public class S02_SharedState_Annotation {

    public static class InterceptedTransmission implements TypedKey<String> { }

    public static class ThreatLevel implements TypedKey<String> { }

    public static class Scrambled implements TypedKey<String> { }

    public interface TransmissionInterceptor {
        @Agent(description = "Intercepts an Imperial transmission and decodes it into plain text",
                typedOutputKey = InterceptedTransmission.class)
        @UserMessage("""
                You are a Rebel Alliance communications officer. \
                Decode this scrambled Imperial transmission into a clear, one-sentence summary \
                of what the Empire is planning: {{Scrambled}}""")
        String intercept(@K(Scrambled.class) String scrambled);
    }

    public interface ThreatClassifier {
        @Agent(description = "Classifies the threat level of an intercepted Imperial transmission",
                typedOutputKey = ThreatLevel.class)
        @UserMessage("""
                You are a Rebel Alliance intelligence analyst. \
                Classify the threat level of this intercepted Imperial transmission as \
                LOW, MEDIUM, HIGH, or CRITICAL. Respond with the level and a brief justification.

                Transmission: {{InterceptedTransmission}}""")
        String classify(@K(InterceptedTransmission.class) String transmission);
    }

    public interface IntelPipeline {
        @SequenceAgent(description = "Decodes and classifies an intercepted Imperial transmission",
                typedOutputKey = ThreatLevel.class,
                subAgents = { TransmissionInterceptor.class, ThreatClassifier.class })
        String analyze(@K(Scrambled.class) String scrambled);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var pipeline = AgenticServices.createAgenticSystem(IntelPipeline.class, model);

        String scrambledInput = "Death Star... operational... Alderaan... target confirmed... fire when ready";

        IO.println("=== Rebel Intelligence Report ===");
        IO.println("Scrambled input: " + scrambledInput);
        IO.println();

        String threatAssessment = pipeline.analyze(scrambledInput);

        IO.println("Threat Assessment: " + threatAssessment);
    }
}
