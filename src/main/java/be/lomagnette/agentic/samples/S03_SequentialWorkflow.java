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
 *
 * Three agents run in sequence, each reading from and writing to the
 * shared AgenticScope. The pipeline takes a youngling's name and produces
 * a fully trained Jedi Knight profile.
 *
 *   aptitudeTester -> masterAssigner -> lightsaberGuide
 */
public class S03_SequentialWorkflow {

    // TypedKeys for scope communication between agents
    public static class AptitudeReport implements TypedKey<String> { }
    public static class AssignedMaster implements TypedKey<String> { }
    public static class LightsaberRecommendation implements TypedKey<String> { }

    // Agent 1: Tests the youngling's Force aptitude.
    // @V("name") binds the method parameter as external input - used for the first agent
    // in a chain that receives its input from outside the scope.
    public interface AptitudeTester {
        @Agent("Evaluates a youngling's Force sensitivity and aptitudes")
        @UserMessage("""
                You are a Jedi Temple instructor. Evaluate this youngling's Force aptitude. \
                Provide a short report (2-3 sentences) covering their strengths \
                (combat, diplomacy, healing, or foresight).

                Youngling: {{name}}""")
        String test(@V("name") String name);
    }

    // Agent 2: Assigns a Jedi Master based on the aptitude report.
    // @K reads from the AgenticScope - the value was written by Agent 1's outputKey.
    // This is the key difference: @V = external input, @K = scope input.
    public interface MasterAssigner {
        @Agent("Assigns the most suitable Jedi Master based on the youngling's aptitudes")
        @UserMessage("""
                You are the Jedi Council. Based on this aptitude report, assign the most \
                suitable Jedi Master from the Order (use known Star Wars masters). \
                Explain the match in 2-3 sentences.

                Aptitude report: {{AptitudeReport}}""")
        String assign(@K(AptitudeReport.class) String aptitude);
    }

    // Agent 3: Recommends a lightsaber form and crystal color.
    // Also reads from scope via @K - it receives Agent 2's output.
    public interface LightsaberGuide {
        @Agent("Recommends lightsaber form and crystal based on master assignment and aptitude")
        @UserMessage("""
                You are the Jedi Temple lightsaber instructor. Based on the assigned master \
                and training path, recommend a lightsaber form (I-VII) and kyber crystal color. \
                Keep it to 2-3 sentences.

                Assigned master: {{AssignedMaster}}""")
        String guide(@K(AssignedMaster.class) String master);
    }

    // Typed pipeline interface - invoke the full sequence with a single call.
    // @V receives external input; internally the scope handles @K bindings.
    public interface JediTrainingPipeline {
        @Agent("Trains a youngling through aptitude testing, master assignment, and lightsaber selection")
        String train(@V("name") String name);
    }

    public static void main(String... args) {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        // Build each agent - outputKey determines where each writes in scope.
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

        // Chain them into a typed sequential pipeline.
        // The AgenticScope flows data automatically:
        //   1. aptitudeTester receives "name" via @V, writes to "AptitudeReport"
        //   2. masterAssigner reads "AptitudeReport" via @K, writes to "AssignedMaster"
        //   3. lightsaberGuide reads "AssignedMaster" via @K, writes to "lightsaberRecommendation"
        JediTrainingPipeline pipeline = AgenticServices
                .sequenceBuilder(JediTrainingPipeline.class)
                .subAgents(aptitudeTester, masterAssigner, lightsaberGuide)
                .outputKey(LightsaberRecommendation.class)
                .build();

        // Invoke the pipeline - one call runs all three agents
        IO.println("=== Jedi Training Pipeline ===");
        IO.println("Youngling: Ahsoka Tano");
        IO.println();

        String result = pipeline.train("Ahsoka Tano");

        IO.println("Jedi Knight Profile: " + result);
        IO.println();
        IO.println("=== Jedi Knight Profile Complete ===");
    }
}
