package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S10 - Supervisor Agent: Darth Vader's Command
 * Demonstrates a supervisor agent that decides which sub-agents to deploy
 * based on the situation. Darth Vader (the supervisor) has four units:
 *   - StormtrooperRegiment: ground operations, crowd control
 *   - BountyHunterAgent: tracking and capturing specific targets
 *   - StarDestroyerAgent: orbital bombardment and fleet engagements
 *   - DeathStarAgent: the Emperor's preferred solution to disagreements
 * The supervisor LLM analyzes the situation and routes to the right agent(s).
 * Uses SupervisorResponseStrategy.SUMMARY to get a unified response.
 */
public class S10_Supervisor {

    public static class Situation implements TypedKey<String> { }

    public interface StormtrooperRegiment {
        @Agent(name = "Storm trooper Regiment", description = "501st Legion stormtroopers for ground operations and crowd control")
        @UserMessage("""
                You are the commander of the 501st Legion. You specialize in ground assaults, \
                garrison duty, and crowd suppression. Report how your troops would handle \
                this situation. Be concise and military in tone (2-3 sentences).

                Situation: {{Situation}}""")
        String deploy(@K(Situation.class) String situation);
    }

    public interface BountyHunterAgent {
        @Agent(name="Bounty Hunter Agent", description = "Elite bounty hunter for tracking, capturing, or eliminating specific targets")
        @UserMessage("""
                You are Boba Fett, the galaxy's most feared bounty hunter. Report how you \
                would track and capture the target in this situation. Be terse and \
                professional (2-3 sentences).

                Situation: {{Situation}}""")
        String hunt(@K(Situation.class) String situation);
    }

    public interface StarDestroyerAgent {
        @Agent(name="Star Destroyer Agent", description = "Imperial Star Destroyer for orbital bombardment and space superiority")
        @UserMessage("""
                You are Admiral Piett commanding the Executor. Report how your Star Destroyer \
                group would handle this situation from orbit. Include tactical positioning \
                and firepower deployment (2-3 sentences).

                Situation: {{Situation}}""")
        String engage(@K(Situation.class) String situation);
    }

    public interface DeathStarAgent {
        @Agent(name="Death Star Agent", description = "Death Star battle station for planetary-scale operations and ultimate deterrence")
        @UserMessage("""
                You are Grand Moff Tarkin commanding the Death Star. Report how you would \
                resolve this situation using the battle station's capabilities. Be imperious \
                and decisive (2-3 sentences).

                Situation: {{Situation}}""")
        String obliterate(@K(Situation.class) String situation);
    }

    public interface DarthVaderCommand {
        @Agent(name="Darth Vader", description = "Darth Vader commanding Imperial forces to handle any situation")
        String command(@V("Situation") String situation);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        ChatModel plannerModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen3:14b")
                .build();

        var droid = new DroidListener();
        StormtrooperRegiment stormtrooperRegiment = AgenticServices
                .agentBuilder(StormtrooperRegiment.class)
                .chatModel(model)
                .outputKey("stormtrooperReport")
                .listener(droid)
                .build();

        BountyHunterAgent bountyHunter = AgenticServices
                .agentBuilder(BountyHunterAgent.class)
                .chatModel(model)
                .outputKey("bountyHunterReport")
                .listener(droid)
                .build();

        StarDestroyerAgent starDestroyer = AgenticServices
                .agentBuilder(StarDestroyerAgent.class)
                .chatModel(model)
                .outputKey("starDestroyerReport")
                .listener(droid)
                .build();

        DeathStarAgent deathStar = AgenticServices
                .agentBuilder(DeathStarAgent.class)
                .chatModel(model)
                .outputKey("deathStarReport")
                .listener(droid)
                .build();

        DarthVaderCommand vader = AgenticServices
                .supervisorBuilder(DarthVaderCommand.class)
                .chatModel(plannerModel)
                .subAgents(stormtrooperRegiment, bountyHunter, starDestroyer, deathStar)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .listener(droid)
                .build();

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
    }
}
