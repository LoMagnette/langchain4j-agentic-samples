package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 * S11 - Hybrid Agents: Mixing AI and Non-AI Steps
 * Demonstrates AgenticServices.agentAction() which wraps plain Java code
 * as an agent (no LLM call). This lets you mix database lookups, API calls,
 * and business logic with AI agents in the same workflow.
 * Pipeline:
 *   distressAnalyzer (AI)    -> reads distress signal, produces structured analysis
 *   analysisParser (non-AI)  -> parses SYSTEM/THREAT from the analysis text
 *   fleetLookup (non-AI)     -> simulates database lookup for available ships
 *   rescueCoordinator (AI)   -> creates a rescue plan using the available ships
 *   broadcastAction (non-AI) -> formats and "transmits" the final orders
 */
public class S11_HybridAgents {

    // TypedKeys for scope communication
    public static class DistressSignal implements TypedKey<String> { }
    public static class TargetSystem implements TypedKey<String> { }
    public static class ThreatType implements TypedKey<String> { }
    public static class AvailableShips implements TypedKey<String> { }
    public static class Analysis implements TypedKey<String> { }
    public static class RescuePlan implements TypedKey<String> { }

    // AI Agent 1: Analyzes the distress signal
    // Uses @V because it's the entry point - receives external input directly
    public interface DistressAnalyzer {
        @Agent("Analyzes an incoming distress signal and identifies the target system and threat")
        @UserMessage("""
                You are a Rebel Alliance communications officer. Analyze this distress signal. \
                Identify: 1) the star system under attack, 2) the type of threat \
                (Imperial fleet, ground assault, blockade, etc.). \
                Respond in exactly this format:
                SYSTEM: <system name>
                THREAT: <threat type>
                SUMMARY: <1-2 sentence summary>

                Distress signal: {{DistressSignal}}""")
        String analyze(@K(DistressSignal.class) String distressSignal);
    }

    public interface RescueCoordinator {
        @Agent("Creates a tactical rescue plan using available ships and threat assessment")
        @UserMessage("""
                You are Admiral Ackbar coordinating a rescue mission. \
                Create a brief tactical plan (3-4 sentences) using these available ships \
                to respond to the threat. Assign specific roles to each ship.

                Target system: {{TargetSystem}}
                Threat: {{ThreatType}}
                Available ships: {{AvailableShips}}""")
        String coordinate(@K(TargetSystem.class) String targetSystem,
                          @K(ThreatType.class) String threatType,
                          @K(AvailableShips.class) String availableShips);
    }

    public interface RescuePipeline {
        @Agent("Analyzes a distress signal, looks up available fleet, creates rescue plan, and broadcasts orders")
        String rescue(@K(DistressSignal.class) String signal);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        DistressAnalyzer distressAnalyzer = AgenticServices
                .agentBuilder(DistressAnalyzer.class)
                .chatModel(model)
                .outputKey(Analysis.class)
                .build();

        var analysisParser = AgenticServices.agentAction(scope -> {
            String analysis = scope.readState(Analysis.class);
            IO.println("  [analysisParser] Parsing analysis...");

            String targetSystem = "Unknown";
            String threatType = "Unknown";
            if (analysis != null) {
                for (String line : analysis.split("\n")) {
                    if (line.startsWith("SYSTEM:")) targetSystem = line.substring(7).trim();
                    if (line.startsWith("THREAT:")) threatType = line.substring(7).trim();
                }
            }

            scope.writeState(TargetSystem.class, targetSystem);
            scope.writeState(ThreatType.class, threatType);
            IO.println("  [analysisParser] Target system: " + targetSystem);
            IO.println("  [analysisParser] Threat type: " + threatType);
        });

        var fleetLookup = AgenticServices.agentAction(scope -> {
            String system = scope.readState(TargetSystem.class);
            IO.println("  [fleetLookup] Looking up available ships near " + system + "...");

            List<String> ships;
            if (system != null && system.toLowerCase().contains("hoth")) {
                ships = List.of("Millennium Falcon", "Rogue Squadron (12 X-Wings)", "GR-75 Transports");
            } else if (system != null && system.toLowerCase().contains("endor")) {
                ships = List.of("Home One (MC80)", "A-Wing Interceptors", "B-Wing Assault Fighters");
            } else {
                ships = List.of("X-Wing Squadron", "Millennium Falcon", "Tantive IV");
            }

            scope.writeState(AvailableShips.class, ships.toString());
            IO.println("  [fleetLookup] Found " + ships.size() + " units: " + ships);
        });

        RescueCoordinator rescueCoordinator = AgenticServices
                .agentBuilder(RescueCoordinator.class)
                .chatModel(model)
                .outputKey(RescuePlan.class)
                .build();

        var broadcastAction = AgenticServices.agentAction(scope -> {
            String plan = scope.readState(RescuePlan.class);
            String system = scope.readState(TargetSystem.class);

            IO.println("  [broadcastAction] Encrypting orders...");
            IO.println("  [broadcastAction] Transmitting on Rebel secure frequency...");

            String broadcast = String.format("""
                    === PRIORITY ONE - ENCRYPTED ===
                    FROM: Rebel Alliance High Command
                    TO: All units in range of %s
                    ORDERS: %s
                    === MAY THE FORCE BE WITH YOU ===""",
                    system != null ? system : "UNKNOWN SYSTEM", plan);

            scope.writeState("broadcast", broadcast);
            IO.println("  [broadcastAction] Broadcast sent!");
        });

        RescuePipeline pipeline = AgenticServices
                .sequenceBuilder(RescuePipeline.class)
                .subAgents(distressAnalyzer, analysisParser, fleetLookup, rescueCoordinator, broadcastAction)
                .outputKey("broadcast")
                .build();

        String distressSignal = "Mayday! Mayday! This is Echo Base on Hoth! Imperial Star Destroyers " +
                "have exited hyperspace. AT-AT walkers are approaching the shield generator. " +
                "We need immediate evacuation support!";

        IO.println("=== Rebel Rescue Pipeline (Hybrid AI + Non-AI) ===");
        IO.println("Incoming distress signal: " + distressSignal);
        IO.println();

        String result = pipeline.rescue(distressSignal);

        IO.println();
        IO.println(result);
        IO.println();

        IO.println("=== Rescue Pipeline Complete ===");
    }
}
