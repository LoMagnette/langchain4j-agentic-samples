package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 * S09 - Goal-Oriented Planner: Lightsaber Forging Pipeline
 * <p>
 * Demonstrates the GoalOrientedPlanner which automatically resolves
 * agent dependencies based on their input/output keys:
 * <p>
 * CrystalForager (needs "jediName" -> produces "kyberCrystal")
 * HiltCrafter    (needs "kyberCrystal" -> produces "hiltDesign")
 * BladeCalibration (needs "hiltDesign" -> produces "lightsaber")
 * <p>
 * The planner figures out the execution order from the dependency chain.
 * Changing the outputKey to "hiltDesign" causes it to skip BladeCalibration.
 */
public class S09_GoalOriented {

    public static class JediName implements TypedKey<String> {
    }

    public static class KyberCrystal implements TypedKey<String> {
    }

    public static class HiltDesign implements TypedKey<String> {
    }

    public static class Lightsaber implements TypedKey<String> {
    }

    // Agent 1: Forages for a kyber crystal suited to the Jedi
    public interface CrystalForagerAgent {
        @Agent("Forages a kyber crystal on Ilum based on the Jedi's Force affinity")
        @UserMessage("""
                You are a kyber crystal guide on the planet Ilum. Based on this Jedi's name \
                and identity, describe the kyber crystal that calls to them through the Force. \
                Include the crystal's color, size, and the emotion it resonates with. \
                Keep it to 2-3 sentences.

                Jedi: {{JediName}}""")
        String forage(@K(JediName.class) String jediName);
    }

    // Agent 2: Designs the hilt around the crystal
    public interface HiltCrafterAgent {
        @Agent("Crafts a lightsaber hilt design based on the kyber crystal properties")
        @UserMessage("""
                You are a lightsaber hilt artisan at the Jedi Temple. Design a hilt \
                that complements this kyber crystal. Describe the materials, grip style, \
                and any unique features. Keep it to 2-3 sentences.

                Kyber crystal: {{KyberCrystal}}""")
        String craft(@K(KyberCrystal.class) String kyberCrystal);
    }

    // Agent 3: Calibrates the final lightsaber
    public interface BladeCalibrationAgent {
        @Agent("Calibrates and activates the completed lightsaber")
        @UserMessage("""
                You are a Jedi weapon master performing the final lightsaber calibration. \
                Based on the hilt design, describe the blade's characteristics when ignited: \
                blade length, stability, sound, and combat suitability. \
                Keep it to 2-3 sentences.

                Hilt design: {{HiltDesign}}""")
        String calibrate(@K(HiltDesign.class) String hiltDesign);
    }

    // Typed pipeline interface for the full forge
    public interface LightsaberForge {
        @Agent("Forges a complete lightsaber for a Jedi")
        String forge(@K(JediName.class) String jediName);
    }

    // Typed pipeline interface for the partial forge (hilt only)
    public interface HiltOnlyForge {
        @Agent("Forges a lightsaber hilt for a Jedi (skips blade calibration)")
        String forge(@K(JediName.class) String jediName);
    }

    public static void main(String... args) {
        //var listener = new DroidListener();

        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        // Build each agent with its output key - this tells the planner what each agent produces
        CrystalForagerAgent crystalForager = AgenticServices
                .agentBuilder(CrystalForagerAgent.class)
                .chatModel(model)
                .outputKey(KyberCrystal.class)
                //.listener(listener)
                .build();

        HiltCrafterAgent hiltCrafter = AgenticServices
                .agentBuilder(HiltCrafterAgent.class)
                .chatModel(model)
                .outputKey(HiltDesign.class)
                //.listener(listener)
                .build();

        BladeCalibrationAgent bladeCalibration = AgenticServices
                .agentBuilder(BladeCalibrationAgent.class)
                .chatModel(model)
                .outputKey(Lightsaber.class)
                //.listener(listener)
                .build();

        // GoalOrientedPlanner resolves the dependency chain automatically:
        // "lightsaber" needs "hiltDesign" needs "kyberCrystal" needs "jediName"
        LightsaberForge forge = AgenticServices
                .plannerBuilder(LightsaberForge.class)
                .subAgents(crystalForager, hiltCrafter, bladeCalibration)
                .outputKey(Lightsaber.class)
                .planner(GoalOrientedPlanner::new)
                //.listener(listener)
                .build();

        // Partial forge: changing outputKey to "hiltDesign" skips BladeCalibration
        // The planner only resolves: "hiltDesign" <- "kyberCrystal" <- "jediName"
        HiltOnlyForge hiltOnly = AgenticServices
                .plannerBuilder(HiltOnlyForge.class)
                .subAgents(crystalForager, hiltCrafter, bladeCalibration)
                .outputKey(HiltDesign.class)
                .planner(GoalOrientedPlanner::new)
                //.listener(listener)
                .build();

        // --- Full forge: all three agents execute ---
        IO.println("=== Lightsaber Forge: Full Pipeline ===");
        String lightsaber = forge.forge("Cal Kestis");
        IO.println(lightsaber);
        IO.println();

        // --- Partial forge: BladeCalibration is skipped ---
        IO.println("=== Lightsaber Forge: Partial Pipeline (outputKey = \"hiltDesign\") ===");
        String hilt = hiltOnly.forge("Ahsoka Tano");
        IO.println(hilt);
        IO.println();

        IO.println("=== Forge Complete ===");
    }
}
