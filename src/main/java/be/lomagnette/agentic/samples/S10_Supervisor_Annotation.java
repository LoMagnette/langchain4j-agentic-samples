package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

import java.nio.file.Path;

/**
 * S10 - Supervisor Agent (Annotation / Declarative)
 * Same supervisor wiring as {@link S10_Supervisor}, expressed with
 * {@link SupervisorAgent} on the {@code DarthVaderCommand} interface.
 * The planner model is provided by {@link ChatModelSupplier}, while the
 * default {@link ChatModel} passed to {@code createAgenticSystem} is used
 * for the sub-agents.
 */
public class S10_Supervisor_Annotation {

    static final AgentMonitor MONITOR = new AgentMonitor();
    static final DroidListener DROID = new DroidListener();

    public static class Situation implements TypedKey<String> { }
    public static class StormtrooperReport implements TypedKey<String> { }
    public static class BountyHunterReport implements TypedKey<String> { }
    public static class StarDestroyerReport implements TypedKey<String> { }
    public static class DeathStarReport implements TypedKey<String> { }

    public interface StormtrooperRegiment {
        @Agent(name = "Storm trooper Regiment",
                description = "501st Legion stormtroopers for ground operations and crowd control",
                typedOutputKey = StormtrooperReport.class)
        @UserMessage("""
                You are the commander of the 501st Legion. You specialize in ground assaults, \
                garrison duty, and crowd suppression. Report how your troops would handle \
                this situation. Be concise and military in tone (2-3 sentences).

                Situation: {{Situation}}""")
        String deploy(@K(Situation.class) String situation);

        @AgentListenerSupplier
        static AgentListener listener() {
            return DROID;
        }
    }

    public interface BountyHunterAgent {
        @Agent(name = "Bounty Hunter Agent",
                description = "Elite bounty hunter for tracking, capturing, or eliminating specific targets",
                typedOutputKey = BountyHunterReport.class)
        @UserMessage("""
                You are Boba Fett, the galaxy's most feared bounty hunter. Report how you \
                would track and capture the target in this situation. Be terse and \
                professional (2-3 sentences).

                Situation: {{Situation}}""")
        String hunt(@K(Situation.class) String situation);

        @AgentListenerSupplier
        static AgentListener listener() {
            return DROID;
        }
    }

    public interface StarDestroyerAgent {
        @Agent(name = "Star Destroyer Agent",
                description = "Imperial Star Destroyer for orbital bombardment and space superiority",
                typedOutputKey = StarDestroyerReport.class)
        @UserMessage("""
                You are Admiral Piett commanding the Executor. Report how your Star Destroyer \
                group would handle this situation from orbit. Include tactical positioning \
                and firepower deployment (2-3 sentences).

                Situation: {{Situation}}""")
        String engage(@K(Situation.class) String situation);

        @AgentListenerSupplier
        static AgentListener listener() {
            return DROID;
        }
    }

    public interface DeathStarAgent {
        @Agent(name = "Death Star Agent",
                description = "Death Star battle station for planetary-scale operations and ultimate deterrence",
                typedOutputKey = DeathStarReport.class)
        @UserMessage("""
                You are Grand Moff Tarkin commanding the Death Star. Report how you would \
                resolve this situation using the battle station's capabilities. Be imperious \
                and decisive (2-3 sentences).

                Situation: {{Situation}}""")
        String obliterate(@K(Situation.class) String situation);

        @AgentListenerSupplier
        static AgentListener listener() {
            return DROID;
        }
    }

    public interface DarthVaderCommand {
        @SupervisorAgent(name = "Darth Vader",
                description = "Darth Vader commanding Imperial forces to handle any situation",
                responseStrategy = SupervisorResponseStrategy.SUMMARY,
                subAgents = { StormtrooperRegiment.class, BountyHunterAgent.class,
                              StarDestroyerAgent.class, DeathStarAgent.class })
        String command(@K(Situation.class) String situation);

        @SupervisorRequest
        static String request(@K(Situation.class) String situation) {
            return "Handle this situation using the appropriate Imperial resources: " + situation;
        }

        @ChatModelSupplier
        static ChatModel plannerModel() {
            return OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("gemma4")
                    .build();
        }

        @AgentListenerSupplier
        static AgentListener listener() {
            return MONITOR;
        }
    }

    void main() {
        ChatModel subAgentModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var vader = AgenticServices.createAgenticSystem(DarthVaderCommand.class, subAgentModel);

        IO.println("=== Scenario 1: Fugitive Jedi ===");
        String scenario1 = "A Jedi survivor has been spotted in the lower levels of Coruscant. " +
                           "They are disguised as a merchant and moving between safe houses.";
        IO.println("Situation: " + scenario1);
        IO.println();
        IO.println("Vader's report: " + vader.command(scenario1));
        IO.println();

        IO.println("=== Scenario 2: Rebel Base on Hoth ===");
        String scenario2 = "Probe droids have confirmed a Rebel base on the ice planet Hoth. " +
                           "The base has a shield generator and an ion cannon protecting it.";
        IO.println("Situation: " + scenario2);
        IO.println();
        IO.println("Vader's report: " + vader.command(scenario2));
        IO.println();

        IO.println("=== Scenario 3: Pirate Fleet ===");
        String scenario3 = "A pirate fleet is raiding Imperial supply convoys near Kessel. " +
                           "Multiple fast corvettes are hitting shipping lanes and jumping to hyperspace.";
        IO.println("Situation: " + scenario3);
        IO.println();
        IO.println("Vader's report: " + vader.command(scenario3));
        IO.println();

        IO.println("=== Scenario 4: Defiant Planet ===");
        String scenario4 = "The planet Alderaan is suspected of funding the Rebel Alliance. " +
                           "Their senate delegation has refused all Imperial demands and is harboring traitors.";
        IO.println("Situation: " + scenario4);
        IO.println();
        IO.println("Vader's report: " + vader.command(scenario4));
        IO.println();

        IO.println("=== Lord Vader's Command Complete ===");

        HtmlReportGenerator.generateReport(MONITOR, Path.of("target/supervisor.html"));
    }
}
