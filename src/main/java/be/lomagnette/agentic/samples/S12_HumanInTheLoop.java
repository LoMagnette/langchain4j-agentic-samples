package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S12 - Human-in-the-Loop: Death Star Firing Protocol
 * Some decisions should not be automated. Firing a superlaser at a planet
 * is one of them. This sample demonstrates HumanInTheLoop, which pauses
 * the workflow and asks a human for input before proceeding.
 * Pipeline:
 *   targetAnalyzer (AI)      -> analyzes the proposed target
 *   commanderApproval (human) -> asks the commander to confirm
 *   superlaserAgent (AI)     -> fires (or stands down) based on confirmation
 */
public class S12_HumanInTheLoop {

    // TypedKeys for scope communication
    public static class ProposedTarget implements TypedKey<String> { }
    public static class TargetAnalysis implements TypedKey<String> { }
    public static class ConfirmedTarget implements TypedKey<Object> { }
    public static class FiringResult implements TypedKey<String> { }

    public interface TargetAnalyzer {
        @Agent("Analyzes a proposed planetary target for the Death Star's superlaser")
        @UserMessage("""
                You are an Imperial Intelligence officer aboard the Death Star. \
                Analyze this proposed target planet. Provide a brief tactical assessment: \
                planetary defenses, civilian population estimate, strategic value, \
                and any risks. Keep it to 3-4 sentences.

                Proposed target: {{ProposedTarget}}""")
        String analyze(@K(ProposedTarget.class) String target);
    }

    public interface SuperlaserAgent {
        @Agent("Executes or aborts the Death Star superlaser firing based on commander confirmation")
        @UserMessage("""
                You are the Death Star's superlaser weapons officer. The commander has responded \
                to the firing confirmation. If the response is "yes", describe the superlaser \
                firing sequence in dramatic fashion (3-4 sentences). If the response is anything \
                else, report that the firing has been aborted and the Death Star stands down. \
                Be dramatic either way.

                Target: {{ProposedTarget}}
                Target analysis: {{TargetAnalysis}}
                Commander's response: {{ConfirmedTarget}}""")
        String fire(@K(ProposedTarget.class) String target,
                    @K(TargetAnalysis.class) String analysis,
                    @K(ConfirmedTarget.class) String confirmation);
    }

    public interface DeathStarProtocol {
        @Agent("Analyzes a target, requests commander confirmation, and fires or aborts the superlaser")
        String execute(@V("ProposedTarget") String target);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("gemma4")
                .build();

        TargetAnalyzer targetAnalyzer = AgenticServices
                .agentBuilder(TargetAnalyzer.class)
                .chatModel(model)
                .outputKey(TargetAnalysis.class)
                .build();

        HumanInTheLoop commanderApproval = AgenticServices.humanInTheLoopBuilder()
                .description("An agent that asks for validation")
                .outputKey("ConfirmedTarget")
                .responseProvider(scope -> {

                    String target = scope.readState(ProposedTarget.class);
                    String analysis = scope.readState(TargetAnalysis.class);
                    String message = String.format("""
                            === COMMANDER APPROVAL REQUIRED ===
                            
                            Target analysis: %s
                            
                            Commander, the Death Star is in position above %s.
                            Confirm target to proceed with superlaser firing? (yes/no)
                            """, analysis,target);
                    return IO.readln(message);
                })
                .build();

        SuperlaserAgent superlaserAgent = AgenticServices
                .agentBuilder(SuperlaserAgent.class)
                .chatModel(model)
                .outputKey(FiringResult.class)
                .build();

        DeathStarProtocol protocol = AgenticServices
                .sequenceBuilder(DeathStarProtocol.class)
                .subAgents(targetAnalyzer, commanderApproval, superlaserAgent)
                .outputKey(FiringResult.class)
                .build();

        String target = "Alderaan";

        IO.println("=== Death Star Firing Protocol ===");
        IO.println("Proposed target: " + target);
        IO.println("Analyzing target...");

        String result = protocol.execute(target);

        IO.println();
        IO.println(result);
        IO.println();
        IO.println("=== Protocol Complete ===");
    }
}
