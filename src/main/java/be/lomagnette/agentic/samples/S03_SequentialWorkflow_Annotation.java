package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S03 - Sequential Workflow (Annotation / Declarative)
 * Same pipeline as {@link S03_SequentialWorkflow}, but described through
 * {@link SequenceAgent} on the {@code JediTrainingPipeline} interface.
 * Sub-agents publish their results to the scope via {@code typedOutputKey}.
 */
public class S03_SequentialWorkflow_Annotation {

    public static class AptitudeReport implements TypedKey<String> { }
    public static class AssignedMaster implements TypedKey<String> { }
    public static class LightsaberRecommendation implements TypedKey<String> { }
    public static class Name implements TypedKey<String> { }

    public interface AptitudeTester {
        @Agent(description = "Evaluates a youngling's Force sensitivity and aptitudes",
                typedOutputKey = AptitudeReport.class)
        @UserMessage("""
                You are a Jedi Temple instructor. Evaluate this youngling's Force aptitude. \
                Provide a short report (2-3 sentences) covering their strengths \
                (combat, diplomacy, healing, or foresight).

                Youngling: {{Name}}""")
        String test(@K(Name.class) String name);
    }

    public interface MasterAssigner {
        @Agent(description = "Assigns the most suitable Jedi Master based on the youngling's aptitudes",
                typedOutputKey = AssignedMaster.class)
        @UserMessage("""
                You are the Jedi Council. Based on this aptitude report, assign the most \
                suitable Jedi Master from the Order (use known Star Wars masters). \
                Explain the match in 2-3 sentences.

                Aptitude report: {{AptitudeReport}}""")
        String assign(@K(AptitudeReport.class) String aptitude);
    }

    public interface LightsaberGuide {
        @Agent(description = "Recommends lightsaber form and crystal based on master assignment and aptitude",
                typedOutputKey = LightsaberRecommendation.class)
        @UserMessage("""
                You are the Jedi Temple lightsaber instructor. Based on the assigned master \
                and training path, recommend a lightsaber form (I-VII) and kyber crystal color. \
                Keep it to 2-3 sentences.

                Assigned master: {{AssignedMaster}}""")
        String guide(@K(AssignedMaster.class) String master);
    }

    public interface JediTrainingPipeline {
        @SequenceAgent(description = "Trains a youngling through aptitude testing, master assignment, and lightsaber selection",
                typedOutputKey = LightsaberRecommendation.class,
                subAgents = { AptitudeTester.class, MasterAssigner.class, LightsaberGuide.class })
        String train(@K(Name.class) String name);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var pipeline = AgenticServices.createAgenticSystem(JediTrainingPipeline.class, model);

        IO.println("=== Jedi Training Pipeline ===");
        IO.println("Youngling: Ahsoka Tano");
        IO.println();

        String result = pipeline.train("Ahsoka Tano");

        IO.println("Jedi Knight Profile: " + result);
        IO.println();
        IO.println("=== Jedi Knight Profile Complete ===");
    }
}
