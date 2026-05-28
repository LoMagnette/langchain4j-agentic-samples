package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

import java.nio.file.Path;
import java.util.List;

/**
 * S11 - Hybrid Agents (Annotation / Declarative)
 * Same hybrid pipeline as {@link S11_HybridAgents}, declared with annotations.
 * AI agents are interfaces with {@link Agent} + {@link UserMessage}; non-AI
 * steps are regular classes with a {@code static} method annotated
 * {@link Agent} that receives the {@link AgenticScope} (this is how the
 * framework recognises non-AI agents in the declarative API).
 */
public class S11_HybridAgents_Annotation {

    static final AgentMonitor MONITOR = new AgentMonitor();
    public static class DistressSignal implements TypedKey<String> { }
    public static class TargetSystem implements TypedKey<String> { }
    public static class ThreatType implements TypedKey<String> { }
    public static class AvailableShips implements TypedKey<String> { }
    public static class Analysis implements TypedKey<String> { }
    public static class RescuePlan implements TypedKey<String> { }
    public static class Broadcast implements TypedKey<String> { }

    public interface DistressAnalyzer {
        @Agent(description = "Analyzes an incoming distress signal and identifies the target system and threat",
                typedOutputKey = Analysis.class)
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

    public static class AnalysisParser {
        @Agent(description = "Parses the SYSTEM and THREAT lines from the analyzer's free text")
        public static void parse(AgenticScope scope) {
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
        }
    }

    public static class FleetLookup {
        @Agent(description = "Looks up the ships available near the target system")
        public static void lookup(AgenticScope scope) {
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
        }
    }

    public interface RescueCoordinator {
        @Agent(description = "Creates a tactical rescue plan using available ships and threat assessment",
                typedOutputKey = RescuePlan.class)
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

    public static class BroadcastAction {
        @Agent(description = "Formats and transmits the rescue plan on Rebel secure frequency")
        public static void broadcast(AgenticScope scope) {
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

            scope.writeState(Broadcast.class, broadcast);
            IO.println("  [broadcastAction] Broadcast sent!");
        }
    }

    public interface RescuePipeline {
        @SequenceAgent(description = "Analyzes a distress signal, looks up available fleet, creates rescue plan, and broadcasts orders",
                typedOutputKey = Broadcast.class,
                subAgents = { DistressAnalyzer.class, AnalysisParser.class, FleetLookup.class,
                              RescueCoordinator.class, BroadcastAction.class })
        String rescue(@K(DistressSignal.class) String signal);

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

        var pipeline = AgenticServices.createAgenticSystem(RescuePipeline.class, model);

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
        HtmlReportGenerator.generateReport(MONITOR, Path.of("target/hybrid.html"));
    }
}
