package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S06 - Conditional Workflow: Mission Briefing Router
 * <p>
 * A two-step workflow:
 * 1. AlignmentClassifier - analyzes a character and writes their alignment
 * (LIGHT_SIDE, DARK_SIDE, or NEUTRAL) to scope
 * 2. Conditional router - dispatches to the appropriate specialist agent
 * based on the alignment value in scope
 * <p>
 * alignmentClassifier -> conditional(jediCouncil | sithLord | mandalorian)
 */
public class S06_ConditionalWorkflow {

    // TypedKeys for scope communication
    public static class CharacterName implements TypedKey<String> { }
    public static class Alignment implements TypedKey<AlignmentType> {
        @Override
        public AlignmentType defaultValue() {
            return AlignmentType.UNKNOWN;
        }
    }
    public static class MissionBriefing implements TypedKey<String> { }

    public enum AlignmentType {
        LIGHT_SIDE, DARK_SIDE, NEUTRAL, UNKNOWN;
    }

    // Step 1: Classifies a character's alignment
    public interface AlignmentClassifier {
        @Agent(value = "Classifies a Star Wars character's Force alignment")
        @UserMessage("""
                You are a Force-sensitive oracle. Classify this character's alignment as 
                exactly one of: LIGHT_SIDE, DARK_SIDE, or NEUTRAL.
                Respond with ONLY the alignment label, nothing else.
                
                
                Character: {{CharacterName}}""")
        AlignmentType classify(@V("CharacterName") String name);
    }

    // Conditional branch: Jedi Council briefing for Light Side characters
    public interface JediCouncilAgent {
        @Agent("Provides a Jedi Council mission briefing for Light Side operatives")
        @UserMessage("""
                You are Master Yoda, head of the Jedi Council. Provide a mission briefing \
                for this Light Side operative. Include their mission objective, the Force \
                guidance they should follow, and a warning. Keep it to 3-4 sentences.

                Operative: {{CharacterName}}""")
        String brief(@K(CharacterName.class) String name);
    }

    // Conditional branch: Sith Lord briefing for Dark Side characters
    public interface SithLordAgent {
        @Agent("Provides a Sith Lord mission briefing for Dark Side operatives")
        @UserMessage("""
                You are Emperor Palpatine. Provide a mission briefing for this Dark Side \
                operative. Include their objective, how to use their anger, and a veiled \
                threat if they fail. Keep it to 3-4 sentences.

                Operative: {{CharacterName}}""")
        String brief(@K(CharacterName.class) String name);
    }

    // Conditional branch: Mandalorian briefing for Neutral characters
    public interface MandalorianAgent {
        @Agent("Provides a Mandalorian guild mission briefing for Neutral operatives")
        @UserMessage("""
                You are the Armorer of the Mandalorian covert. Provide a mission briefing \
                for this Neutral operative. Include the bounty details, rules of engagement, \
                and a reminder of the Way. Keep it to 3-4 sentences.

                Operative: {{CharacterName}}""")
        String brief(@K(CharacterName.class) String name);
    }

    // Typed pipeline interface - invoke the full conditional workflow with a single call.
    public interface MissionRouter {
        @Agent("Classifies a character and routes to the appropriate mission briefing")
        String briefing(@K(CharacterName.class) String characterName);
    }

    public static void main(String... args) {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        // Build the alignment classifier - writes alignment to scope
        AlignmentClassifier alignmentClassifier = AgenticServices
                .agentBuilder(AlignmentClassifier.class)
                .chatModel(model)
                .outputKey(Alignment.class)
                .build();

        // Build the three specialist agents - one for each alignment
        JediCouncilAgent jediCouncilAgent = AgenticServices
                .agentBuilder(JediCouncilAgent.class)
                .chatModel(model)
                .outputKey(MissionBriefing.class)
                .build();

        SithLordAgent sithLordAgent = AgenticServices
                .agentBuilder(SithLordAgent.class)
                .chatModel(model)
                .outputKey(MissionBriefing.class)
                .build();

        MandalorianAgent mandalorianAgent = AgenticServices
                .agentBuilder(MandalorianAgent.class)
                .chatModel(model)
                .outputKey(MissionBriefing.class)
                .build();

        // Build the conditional router - reads alignment from scope and dispatches
        var conditionalRouter = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState(Alignment.class) == AlignmentType.LIGHT_SIDE, jediCouncilAgent)
                .subAgents(scope -> scope.readState(Alignment.class) == AlignmentType.DARK_SIDE, sithLordAgent)
                .subAgents(scope -> scope.readState(Alignment.class) == AlignmentType.NEUTRAL, mandalorianAgent)
                .build();

        // Build the full sequence: classify alignment, then route to specialist
        MissionRouter router = AgenticServices
                .sequenceBuilder(MissionRouter.class)
                .subAgents(alignmentClassifier, conditionalRouter)
                .outputKey(MissionBriefing.class)
                .build();

        // Invoke the pipeline for each character
        String[] characters = {"Luke Skywalker", "Darth Maul", "Din Djarin"};

        for (String character : characters) {
            IO.println("=== Mission Briefing for: " + character + " ===");
            String briefing = router.briefing(character);
            IO.println(briefing);
            IO.println();
        }
    }
}
