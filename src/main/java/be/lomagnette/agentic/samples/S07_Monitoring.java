package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.nio.file.Path;


/**
 * S07 - Composing Patterns: Rebellion HQ Assault Planner
 * Demonstrates that workflows are agents, so they can be sub-agents of
 * other workflows. Here a sequence contains a loop:
 *   imperialDecoder -> councilLoop(jediCouncilCritic <-> planReviser) -> holonetBroadcaster
 * The councilLoop iterates until 4 Jedi Masters approve or 3 iterations pass.
 * Then holonetBroadcaster turns the approved plan into a call to arms.
 */
public class S07_Monitoring {

    public static class BattlePlan implements TypedKey<String> { }
    public static class CouncilFeedback implements TypedKey<String> { }
    public static class MasterApprovals implements TypedKey<Integer> {
        @Override
        public Integer defaultValue() {
            return 0; // start with 0 approvals
        }
    }
    public static class CallToArms implements TypedKey<String> { }

    public interface ImperialDecoder {
        @Agent("Decodes intercepted Imperial transmissions into actionable intelligence")
        @UserMessage("""
                You are a Rebel Alliance signals officer. Decode this intercepted Imperial \
                transmission into a clear intelligence summary. Identify the target, \
                Imperial forces, and any vulnerabilities. Keep it to 3-4 sentences.

                Intercepted data: {{data}}""")
        String decode(@V("data") String data);
    }

    public interface JediCouncilCritic {
        @Agent("Reviews a battle plan as the Jedi Council and provides critique")
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
        @Agent("Revises the battle plan based on Jedi Council feedback")
        @UserMessage("""
                You are Mon Mothma, leader of the Rebel Alliance. Revise the battle plan \
                to address the Jedi Council's concerns. Produce an improved plan in 4-5 sentences.

                Current plan: {{BattlePlan}}

                Council feedback: {{CouncilFeedback}}""")
        String revise(@K(BattlePlan.class) String plan, @K(CouncilFeedback.class) String feedback);
    }

    public interface HolonetBroadcaster {
        @Agent("Transforms an approved battle plan into an inspiring call to arms")
        @UserMessage("""
                You are Princess Leia Organa. Transform this approved battle plan into \
                an inspiring call to arms for the entire Rebel Alliance. Make it stirring \
                and hopeful. Keep it to 3-4 sentences.

                Approved battle plan: {{BattlePlan}}""")
        String broadcast(@K(BattlePlan.class) String plan);
    }

    public interface RebellionHQ {
        @Agent("Decodes intelligence, refines a battle plan through council review, and broadcasts the result")
        String plan(@V("data") String interceptedData);
    }

    void main() {
        var monitor = new AgentMonitor();

        var model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var jediCouncilCritic = AgenticServices
                .agentBuilder(JediCouncilCritic.class)
                .chatModel(model)
                .outputKey(CouncilFeedback.class)
                .build();

        var planReviser = AgenticServices
                .agentBuilder(PlanReviser.class)
                .chatModel(model)
                .outputKey(BattlePlan.class)
                .build();

        UntypedAgent councilLoop = AgenticServices
                .loopBuilder()
                .subAgents(jediCouncilCritic, planReviser)
                .outputKey(BattlePlan.class)
                .listener(monitor)
                .exitCondition(scope -> scope.readState(MasterApprovals.class) >= 4)
                .maxIterations(3)
                .build();

        var imperialDecoder = AgenticServices
                .agentBuilder(ImperialDecoder.class)
                .chatModel(model)
                .outputKey(BattlePlan.class)
                .build();

        var holonetBroadcaster = AgenticServices
                .agentBuilder(HolonetBroadcaster.class)
                .chatModel(model)
                .outputKey(CallToArms.class)
                .build();

        var hq = AgenticServices
                .sequenceBuilder(RebellionHQ.class)
                .subAgents(imperialDecoder, councilLoop, holonetBroadcaster)
                .outputKey(CallToArms.class)
                .listener(monitor)
                .build();

        var interceptedData = "DS-2... construction 60%... shield generator moon of Endor... " +
                "Emperor arriving... fleet massing at Sullust... trap suspected";

        IO.println("=== Rebellion HQ: Composed Workflow ===");
        IO.println("Intercepted data: " + interceptedData);
        IO.println();

        var callToArms = hq.plan(interceptedData);

        IO.println("Call to Arms: " + callToArms);

        var execution = monitor.successfulExecutions().get(0);
        IO.println(execution);

        HtmlReportGenerator.generateReport(monitor, Path.of("target/review-composing-patterns.html"));
    }
}
