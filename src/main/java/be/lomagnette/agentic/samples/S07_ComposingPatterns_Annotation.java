package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;


/**
 * S07 - Composing Patterns (Annotation / Declarative)
 * Same composition as {@link S07_ComposingPatterns}: a sequence contains
 * a loop. With the annotation API, the loop is its own interface
 * ({@code CouncilLoop} - {@link LoopAgent}) used as a sub-agent of the
 * outer {@link SequenceAgent} ({@code RebellionHQ}).
 */
public class S07_ComposingPatterns_Annotation {

    public static class BattlePlan implements TypedKey<String> { }
    public static class CouncilFeedback implements TypedKey<String> { }
    public static class MasterApprovals implements TypedKey<Integer> {
        @Override
        public Integer defaultValue() {
            return 0;
        }
    }
    public static class CallToArms implements TypedKey<String> { }

    public interface ImperialDecoder {
        @Agent(description = "Decodes intercepted Imperial transmissions into actionable intelligence",
                typedOutputKey = BattlePlan.class)
        @UserMessage("""
                You are a Rebel Alliance signals officer. Decode this intercepted Imperial \
                transmission into a clear intelligence summary. Identify the target, \
                Imperial forces, and any vulnerabilities. Keep it to 3-4 sentences.

                Intercepted data: {{data}}""")
        String decode(@V("data") String data);
    }

    public interface JediCouncilCritic {
        @Agent(description = "Reviews a battle plan as the Jedi Council and provides critique",
                typedOutputKey = CouncilFeedback.class)
        @UserMessage("""
                You are the Jedi Council (Yoda, Mace Windu, Ki-Adi-Mundi, Plo Koon). \
                Review this battle plan. Each master votes APPROVE or REJECT with a brief reason. \
                Count the total approvals. Format:
                Yoda: APPROVE/REJECT - reason
                Mace Windu: APPROVE/REJECT - reason
                Ki-Adi-Mundi: APPROVE/REJECT - reason
                Plo Koon: APPROVE/REJECT - reason
                Total approvals: N/4

                Battle plan: {{BattlePlan}}""")
        String review(@K(BattlePlan.class) String plan);
    }

    public interface PlanReviser {
        @Agent(description = "Revises the battle plan based on Jedi Council feedback",
                typedOutputKey = BattlePlan.class)
        @UserMessage("""
                You are Mon Mothma, leader of the Rebel Alliance. Revise the battle plan \
                to address the Jedi Council's concerns. Produce an improved plan in 4-5 sentences.

                Current plan: {{BattlePlan}}

                Council feedback: {{CouncilFeedback}}""")
        String revise(@K(BattlePlan.class) String plan, @K(CouncilFeedback.class) String feedback);
    }

    public interface HolonetBroadcaster {
        @Agent(description = "Transforms an approved battle plan into an inspiring call to arms",
                typedOutputKey = CallToArms.class)
        @UserMessage("""
                You are Princess Leia Organa. Transform this approved battle plan into \
                an inspiring call to arms for the entire Rebel Alliance. Make it stirring \
                and hopeful. Keep it to 3-4 sentences.

                Approved battle plan: {{BattlePlan}}""")
        String broadcast(@K(BattlePlan.class) String plan);
    }

    public interface CouncilLoop {
        @LoopAgent(description = "Iterates Council critique and plan revision until 4 approvals or 3 rounds",
                typedOutputKey = BattlePlan.class,
                maxIterations = 3,
                subAgents = { JediCouncilCritic.class, PlanReviser.class })
        String refine(@K(BattlePlan.class) String plan);

        @ExitCondition(testExitAtLoopEnd = true, description = "exit when 4 masters approve")
        static boolean enoughApprovals(@K(MasterApprovals.class) int approvals) {
            return approvals >= 4;
        }
    }

    public interface RebellionHQ {
        @SequenceAgent(description = "Decodes intelligence, refines a battle plan through council review, and broadcasts the result",
                typedOutputKey = CallToArms.class,
                subAgents = { ImperialDecoder.class, CouncilLoop.class, HolonetBroadcaster.class })
        String plan(@V("data") String interceptedData);

        @ChatModelSupplier
        static ChatModel model() {
            return OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("gemma4")
                    .build();
        }
    }

    void main() {
        var hq = AgenticServices.createAgenticSystem(RebellionHQ.class);

        var interceptedData = "DS-2... construction 60%... shield generator moon of Endor... " +
                "Emperor arriving... fleet massing at Sullust... trap suspected";

        IO.println("=== Rebellion HQ: Composed Workflow ===");
        IO.println("Intercepted data: " + interceptedData);
        IO.println();

        String callToArms = hq.plan(interceptedData);

        IO.println("Call to Arms: " + callToArms);
    }
}
