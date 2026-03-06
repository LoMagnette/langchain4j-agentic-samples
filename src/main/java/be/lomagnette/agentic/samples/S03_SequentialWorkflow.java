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
 * S03 - Sequential Workflow: Jedi Training Pipeline
 * Three agents run in sequence, each reading from and writing to the
 * shared AgenticScope. The pipeline takes a youngling's name and produces
 * a fully trained Jedi Knight profile.
 *   aptitudeTester -> masterAssigner -> lightsaberGuide
 */
public class S03_SequentialWorkflow {

    public static class AptitudeReport implements TypedKey<String> { }
    public static class AssignedMaster implements TypedKey<String> { }
    public static class LightsaberRecommendation implements TypedKey<String> { }

    public interface AptitudeTester {
        @Agent("Evaluates a youngling's Force sensitivity and aptitudes")
        @UserMessage("""
                You are a Jedi Temple instructor. Evaluate this youngling's Force aptitude. \
                Provide a short report (2-3 sentences) covering their strengths \
                (combat, diplomacy, healing, or foresight).

                Youngling: {{name}}""")
        String test(@V("name") String name);
    }

    public interface MasterAssigner {
        @Agent("Assigns the most suitable Jedi Master based on the youngling's aptitudes")
        @UserMessage("""
                You are the Jedi Council. Based on this aptitude report, assign the most \
                suitable Jedi Master from the Order (use known Star Wars masters). \
                Explain the match in 2-3 sentences.

                Aptitude report: {{AptitudeReport}}""")
        String assign(@K(AptitudeReport.class) String aptitude);
    }

    public interface LightsaberGuide {
        @Agent("Recommends lightsaber form and crystal based on master assignment and aptitude")
        @UserMessage("""
                You are the Jedi Temple lightsaber instructor. Based on the assigned master \
                and training path, recommend a lightsaber form (I-VII) and kyber crystal color. \
                Keep it to 2-3 sentences.

                Assigned master: {{AssignedMaster}}""")
        String guide(@K(AssignedMaster.class) String master);
    }

    public interface JediTrainingPipeline {
        @Agent("Trains a youngling through aptitude testing, master assignment, and lightsaber selection")
        String train(@V("name") String name);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        AptitudeTester aptitudeTester = AgenticServices
                .agentBuilder(AptitudeTester.class)
                .chatModel(model)
                .outputKey(AptitudeReport.class)  // writes report here
                .build();

        MasterAssigner masterAssigner = AgenticServices
                .agentBuilder(MasterAssigner.class)
                .chatModel(model)
                .outputKey(AssignedMaster.class)  // writes assigned master here
                .build();

        LightsaberGuide lightsaberGuide = AgenticServices
                .agentBuilder(LightsaberGuide.class)
                .chatModel(model)
                .outputKey(LightsaberRecommendation.class)
                .build();

        JediTrainingPipeline pipeline = AgenticServices
                .sequenceBuilder(JediTrainingPipeline.class)
                .subAgents(aptitudeTester, masterAssigner, lightsaberGuide)
                .outputKey(LightsaberRecommendation.class)
                .build();

        IO.println("=== Jedi Training Pipeline ===");
        IO.println("Youngling: Ahsoka Tano");
        IO.println();

        String result = pipeline.train("Ahsoka Tano");

        IO.println("Jedi Knight Profile: " + result);
        IO.println();
        IO.println("=== Jedi Knight Profile Complete ===");
    }
}
