package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

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

    public static class JediName implements TypedKey<String> { }
    public static class KyberCrystal implements TypedKey<String> { }
    public static class HiltDesign implements TypedKey<String> { }
    public static class Lightsaber implements TypedKey<String> { }

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

    public interface HiltCrafterAgent {
        @Agent("Crafts a lightsaber hilt design based on the kyber crystal properties")
        @UserMessage("""
                You are a lightsaber hilt artisan at the Jedi Temple. Design a hilt \
                that complements this kyber crystal. Describe the materials, grip style, \
                and any unique features. Keep it to 2-3 sentences.

                Kyber crystal: {{KyberCrystal}}""")
        String craft(@K(KyberCrystal.class) String kyberCrystal);
    }

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

    public interface LightsaberForge {
        @Agent("Forges a complete lightsaber for a Jedi")
        String forge(@K(JediName.class) String jediName);
    }

    public interface HiltOnlyForge {
        @Agent("Forges a lightsaber hilt for a Jedi (skips blade calibration)")
        String forge(@K(JediName.class) String jediName);
    }

    void main() {
        //var listener = new DroidListener();

        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

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

        LightsaberForge forge = AgenticServices
                .plannerBuilder(LightsaberForge.class)
                .subAgents(crystalForager, hiltCrafter, bladeCalibration)
                .outputKey(Lightsaber.class)
                .planner(GoalOrientedPlanner::new)
                //.listener(listener)
                .build();

        HiltOnlyForge hiltOnly = AgenticServices
                .plannerBuilder(HiltOnlyForge.class)
                .subAgents(crystalForager, hiltCrafter, bladeCalibration)
                .outputKey(HiltDesign.class)
                .planner(GoalOrientedPlanner::new)
                //.listener(listener)
                .build();

        IO.println("=== Lightsaber Forge: Full Pipeline ===");
        String lightsaber = forge.forge("Cal Kestis");
        IO.println(lightsaber);
        IO.println();

        IO.println("=== Lightsaber Forge: Partial Pipeline (outputKey = \"hiltDesign\") ===");
        String hilt = hiltOnly.forge("Ahsoka Tano");
        IO.println(hilt);
        IO.println();

        IO.println("=== Forge Complete ===");
    }
}
