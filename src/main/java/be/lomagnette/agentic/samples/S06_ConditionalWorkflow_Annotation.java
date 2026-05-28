package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * S06 - Conditional Workflow (Annotation / Declarative)
 * Same two-step routing as {@link S06_ConditionalWorkflow}, expressed as
 * a {@link SequenceAgent} ({@code MissionRouter}) that runs the classifier
 * followed by a {@link ConditionalAgent} ({@code BriefingDispatcher}) whose
 * branches are gated by {@link ActivationCondition} predicates.
 */
public class S06_ConditionalWorkflow_Annotation {

    public static class CharacterName implements TypedKey<String> { }
    public static class Alignment implements TypedKey<AlignmentType> {
        @Override
        public AlignmentType defaultValue() {
            return AlignmentType.UNKNOWN;
        }
    }
    public static class MissionBriefing implements TypedKey<String> { }

    public enum AlignmentType {
        LIGHT_SIDE, DARK_SIDE, NEUTRAL, UNKNOWN
    }

    public interface AlignmentClassifier {
        @Agent(description = "Classifies a Star Wars character's Force alignment",
                typedOutputKey = Alignment.class)
        @UserMessage("""
                You are a Force-sensitive oracle. Classify this character's alignment as
                exactly one of: LIGHT_SIDE, DARK_SIDE, or NEUTRAL.
                Respond with ONLY the alignment label, nothing else.

                Character: {{CharacterName}}""")
        AlignmentType classify(@V("CharacterName") String name);
    }

    public interface JediCouncilAgent {
        @Agent(description = "Provides a Jedi Council mission briefing for Light Side operatives",
                typedOutputKey = MissionBriefing.class)
        @UserMessage("""
                You are Master Yoda, head of the Jedi Council. Provide a mission briefing \
                for this Light Side operative. Include their mission objective, the Force \
                guidance they should follow, and a warning. Keep it to 3-4 sentences.

                Operative: {{CharacterName}}""")
        String brief(@K(CharacterName.class) String name);
    }

    public interface SithLordAgent {
        @Agent(description = "Provides a Sith Lord mission briefing for Dark Side operatives",
                typedOutputKey = MissionBriefing.class)
        @UserMessage("""
                You are Emperor Palpatine. Provide a mission briefing for this Dark Side \
                operative. Include their objective, how to use their anger, and a veiled \
                threat if they fail. Keep it to 3-4 sentences.

                Operative: {{CharacterName}}""")
        String brief(@K(CharacterName.class) String name);
    }

    public interface MandalorianAgent {
        @Agent(description = "Provides a Mandalorian guild mission briefing for Neutral operatives",
                typedOutputKey = MissionBriefing.class)
        @UserMessage("""
                You are the Armorer of the Mandalorian covert. Provide a mission briefing \
                for this Neutral operative. Include the bounty details, rules of engagement, \
                and a reminder of the Way. Keep it to 3-4 sentences.

                Operative: {{CharacterName}}""")
        String brief(@K(CharacterName.class) String name);
    }

    public interface BriefingDispatcher {
        @ConditionalAgent(description = "Routes to the right faction briefing based on alignment",
                typedOutputKey = MissionBriefing.class,
                subAgents = { JediCouncilAgent.class, SithLordAgent.class, MandalorianAgent.class })
        String dispatch(@K(CharacterName.class) String name);

        @ActivationCondition(JediCouncilAgent.class)
        static boolean isLightSide(@K(Alignment.class) AlignmentType alignment) {
            return alignment == AlignmentType.LIGHT_SIDE;
        }

        @ActivationCondition(SithLordAgent.class)
        static boolean isDarkSide(@K(Alignment.class) AlignmentType alignment) {
            return alignment == AlignmentType.DARK_SIDE;
        }

        @ActivationCondition(MandalorianAgent.class)
        static boolean isNeutral(@K(Alignment.class) AlignmentType alignment) {
            return alignment == AlignmentType.NEUTRAL;
        }
    }

    public interface MissionRouter {
        @SequenceAgent(description = "Classifies a character and routes to the appropriate mission briefing",
                typedOutputKey = MissionBriefing.class,
                subAgents = { AlignmentClassifier.class, BriefingDispatcher.class })
        String briefing(@K(CharacterName.class) String characterName);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var router = AgenticServices.createAgenticSystem(MissionRouter.class, model);

        var characters = List.of("Luke Skywalker", "Darth Maul", "Din Djarin");

        characters.forEach(character -> {
            IO.println("=== Mission Briefing for: " + character + " ===");
            var briefing = router.briefing(character);
            IO.println(briefing);
            IO.println();
        });
    }
}
