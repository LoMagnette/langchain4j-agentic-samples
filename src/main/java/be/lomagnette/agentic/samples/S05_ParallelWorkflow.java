package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

import java.util.concurrent.Executors;

/**
 * S05 - Parallel Workflow: Battle of Endor
 * Three agents run in parallel to plan different aspects of the Battle of Endor:
 *   - Space Fleet Strategist (fleet disposition above Endor)
 *   - Ground Assault Agent (Ewok ground strategy on the forest moon)
 *   - Jedi Mission Planner (Luke's objective aboard the Death Star)
 * Results are merged via the .output() function into a unified battle plan.
 */
public class S05_ParallelWorkflow {

    public static class Briefing implements TypedKey<String> { }
    public static class FleetDisposition implements TypedKey<String> { }
    public static class EwokGroundStrategy implements TypedKey<String> { }
    public static class LukeObjective implements TypedKey<String> { }
    public static class BattleOfEndor implements TypedKey<String> { }

    public interface SpaceFleetStrategist {
        @Agent(name = "Space Fleet Strategist", description = "Plans the Rebel fleet engagement in the space battle above Endor")
        @UserMessage("""
                You are Admiral Ackbar, commanding the Rebel fleet at the Battle of Endor. \
                Plan the fleet disposition: where to position capital ships, fighter squadrons, \
                and how to deal with the Imperial Star Destroyers and the Death Star's shield. \
                Keep it to 3-4 sentences.

                Mission briefing: {{Briefing}}""")
        String plan(@K(Briefing.class) String briefing);
    }

    public interface GroundAssaultAgent {
        @Agent(name = "Ground Assault Planner", description = "Plans the Ewok-assisted ground assault on the shield generator bunker")
        @UserMessage("""
                You are General Han Solo, leading the strike team on Endor's forest moon. \
                Plan the ground assault to destroy the shield generator bunker with Ewok allies. \
                Include troop positioning, diversions, and the bunker breach. \
                Keep it to 3-4 sentences.

                Mission briefing: {{Briefing}}""")
        String plan(@K(Briefing.class) String briefing);
    }

    public interface JediMissionPlanner {
        @Agent(name = "Jedi Mission Planner", description = "Plans Luke Skywalker's mission to confront the Emperor aboard the Death Star")
        @UserMessage("""
                You are Luke Skywalker. Plan your mission to board the Death Star II, \
                confront Darth Vader, and turn him back to the light side while the Emperor watches. \
                What is your approach? Keep it to 3-4 sentences.

                Mission briefing: {{Briefing}}""")
        String plan(@K(Briefing.class) String briefing);
    }

    public interface BattleOfEndorPipeline {
        @Agent(name="Battle of Endor Planner", description = "Plans all aspects of the Battle of Endor in parallel")
        String plan(@K(Briefing.class) String briefing);
    }

    static String assembleBattlePlan(String fleet, String ground, String jedi) {
        return """
                === BATTLE OF ENDOR - UNIFIED PLAN ===

                [SPACE] Fleet Disposition:
                %s

                [GROUND] Ewok Ground Strategy:
                %s

                [JEDI] Luke's Objective:
                %s

                === END BATTLE PLAN ==="""
                .formatted(fleet, ground, jedi);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        //var droid = new DroidListener();

        SpaceFleetStrategist spaceFleetStrategist = AgenticServices
                .agentBuilder(SpaceFleetStrategist.class)
                .chatModel(model)
                .outputKey(FleetDisposition.class)
                //.listener(droid)
                .build();

        GroundAssaultAgent groundAssaultAgent = AgenticServices
                .agentBuilder(GroundAssaultAgent.class)
                .chatModel(model)
                .outputKey(EwokGroundStrategy.class)
                //.listener(droid)
                .build();

        JediMissionPlanner jediMissionPlanner = AgenticServices
                .agentBuilder(JediMissionPlanner.class)
                .chatModel(model)
                .outputKey(LukeObjective.class)
                //.listener(droid)
                .build();

        BattleOfEndorPipeline pipeline = AgenticServices
                .parallelBuilder(BattleOfEndorPipeline.class)
                .subAgents(spaceFleetStrategist, groundAssaultAgent, jediMissionPlanner)
                .executor(Executors.newFixedThreadPool(3))
                .output(scope -> assembleBattlePlan(
                        scope.readState(FleetDisposition.class),
                        scope.readState(EwokGroundStrategy.class),
                        scope.readState(LukeObjective.class)))
                .outputKey(BattleOfEndor.class)
                //.listener(droid)
                .build();

        String missionBriefing = "The second Death Star is under construction above the forest moon of Endor. " +
                "Its shield generator is on the moon's surface. The Emperor himself is aboard.";

        IO.println("Mission briefing: " + missionBriefing);
        IO.println();
        IO.println("Planning Battle of Endor in parallel...");
        IO.println();

        String result = pipeline.plan(missionBriefing);

        IO.println(result);
    }
}
