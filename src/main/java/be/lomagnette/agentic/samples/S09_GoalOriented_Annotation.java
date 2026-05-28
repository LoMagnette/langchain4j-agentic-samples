package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

import java.nio.file.Path;

/**
 * S09 - Goal-Oriented Planner (Annotation / Declarative)
 * Same {@link GoalOrientedPlanner}-driven forging pipeline as
 * {@link S09_GoalOriented}, but described with {@link PlannerAgent} and a
 * static {@link PlannerSupplier}. The planner resolves the execution order
 * from each sub-agent's input/output keys.
 */
public class S09_GoalOriented_Annotation {

    static final AgentMonitor MONITOR = new AgentMonitor();

    public static class JediName implements TypedKey<String> { }
    public static class KyberCrystal implements TypedKey<String> { }
    public static class HiltDesign implements TypedKey<String> { }
    public static class Lightsaber implements TypedKey<String> { }

    public interface CrystalForagerAgent {
        @Agent(description = "Forages a kyber crystal on Ilum based on the Jedi's Force affinity",
                typedOutputKey = KyberCrystal.class)
        @UserMessage("""
                You are a kyber crystal guide on the planet Ilum. Based on this Jedi's name \
                and identity, describe the kyber crystal that calls to them through the Force. \
                Include the crystal's color, size, and the emotion it resonates with. \
                Keep it to 2-3 sentences.

                Jedi: {{JediName}}""")
        String forage(@K(JediName.class) String jediName);
    }

    public interface HiltCrafterAgent {
        @Agent(description = "Crafts a lightsaber hilt design based on the kyber crystal properties",
                typedOutputKey = HiltDesign.class)
        @UserMessage("""
                You are a lightsaber hilt artisan at the Jedi Temple. Design a hilt \
                that complements this kyber crystal. Describe the materials, grip style, \
                and any unique features. Keep it to 2-3 sentences.

                Kyber crystal: {{KyberCrystal}}""")
        String craft(@K(KyberCrystal.class) String kyberCrystal);
    }

    public interface BladeCalibrationAgent {
        @Agent(description = "Calibrates and activates the completed lightsaber",
                typedOutputKey = Lightsaber.class)
        @UserMessage("""
                You are a Jedi weapon master performing the final lightsaber calibration. \
                Based on the hilt design, describe the blade's characteristics when ignited: \
                blade length, stability, sound, and combat suitability. \
                Keep it to 2-3 sentences.

                Hilt design: {{HiltDesign}}""")
        String calibrate(@K(HiltDesign.class) String hiltDesign);
    }

    public interface LightsaberForge {
        @PlannerAgent(description = "Forges a complete lightsaber for a Jedi",
                typedOutputKey = Lightsaber.class,
                subAgents = { CrystalForagerAgent.class, HiltCrafterAgent.class, BladeCalibrationAgent.class })
        String forge(@K(JediName.class) String jediName);

        @PlannerSupplier
        static Planner planner() {
            return new GoalOrientedPlanner();
        }

        @AgentListenerSupplier
        static AgentListener listener() {
            return MONITOR;
        }
    }

    public interface HiltOnlyForge {
        @PlannerAgent(description = "Forges a lightsaber hilt for a Jedi (skips blade calibration)",
                typedOutputKey = HiltDesign.class,
                subAgents = { CrystalForagerAgent.class, HiltCrafterAgent.class, BladeCalibrationAgent.class })
        String forge(@K(JediName.class) String jediName);

        @PlannerSupplier
        static Planner planner() {
            return new GoalOrientedPlanner();
        }

        @AgentListenerSupplier
        static AgentListener listener() {
            return MONITOR;
        }
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var forge = AgenticServices.createAgenticSystem(LightsaberForge.class, model);
        var hiltOnly = AgenticServices.createAgenticSystem(HiltOnlyForge.class, model);

        IO.println("=== Lightsaber Forge: Full Pipeline ===");
        String lightsaber = forge.forge("Cal Kestis");
        IO.println(lightsaber);
        IO.println();

        IO.println("=== Lightsaber Forge: Partial Pipeline (outputKey = \"hiltDesign\") ===");
        String hilt = hiltOnly.forge("Ahsoka Tano");
        IO.println(hilt);
        IO.println();

        IO.println("=== Forge Complete ===");

        HtmlReportGenerator.generateReport(MONITOR, Path.of("target/goap.html"));
    }
}
