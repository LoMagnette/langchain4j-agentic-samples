package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.*;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.nio.file.Path;

/**
 * S12 - Human-in-the-Loop (Annotation / Declarative)
 * Same protocol as {@link S12_HumanInTheLoop}: analyze the target, ask the
 * commander, then fire or stand down. With the annotation API the human
 * step is a regular class with a static method annotated
 * {@link HumanInTheLoop}, used as a sub-agent of the outer
 * {@link SequenceAgent}.
 */
public class S12_HumanInTheLoop_Annotation {

    static final AgentMonitor MONITOR = new AgentMonitor();
    public static class ProposedTarget implements TypedKey<String> { }
    public static class TargetAnalysis implements TypedKey<String> { }
    public static class ConfirmedTarget implements TypedKey<String> { }
    public static class FiringResult implements TypedKey<String> { }

    public interface TargetAnalyzer {
        @Agent(description = "Analyzes a proposed planetary target for the Death Star's superlaser",
                typedOutputKey = TargetAnalysis.class)
        @UserMessage("""
                You are an Imperial Intelligence officer aboard the Death Star. \
                Analyze this proposed target planet. Provide a brief tactical assessment: \
                planetary defenses, civilian population estimate, strategic value, \
                and any risks. Keep it to 3-4 sentences.

                Proposed target: {{ProposedTarget}}""")
        String analyze(@K(ProposedTarget.class) String target);
    }

    public static class CommanderApproval {
        @HumanInTheLoop(description = "Asks the commander to confirm the firing order",
                outputKey = "ConfirmedTarget")
        public static String confirm(@K(ProposedTarget.class) String target,
                                     @K(TargetAnalysis.class) String analysis) {
            String message = String.format("""
                    === COMMANDER APPROVAL REQUIRED ===

                    Target analysis: %s

                    Commander, the Death Star is in position above %s.
                    Confirm target to proceed with superlaser firing? (yes/no)
                    """, analysis, target);
            return IO.readln(message);
        }
    }

    public interface SuperlaserAgent {
        @Agent(description = "Executes or aborts the Death Star superlaser firing based on commander confirmation",
                typedOutputKey = FiringResult.class)
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
        @SequenceAgent(description = "Analyzes a target, requests commander confirmation, and fires or aborts the superlaser",
                typedOutputKey = FiringResult.class,
                subAgents = { TargetAnalyzer.class, CommanderApproval.class, SuperlaserAgent.class })
        String execute(@V("ProposedTarget") String target);

        @AgentListenerSupplier
        static AgentListener listener() {
            return MONITOR;
        }
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("gemma4")
                .build();

        var protocol = AgenticServices.createAgenticSystem(DeathStarProtocol.class, model);

        String target = "Alderaan";

        IO.println("=== Death Star Firing Protocol ===");
        IO.println("Proposed target: " + target);
        IO.println("Analyzing target...");

        String result = protocol.execute(target);

        IO.println();
        IO.println(result);
        IO.println();
        IO.println("=== Protocol Complete ===");
        HtmlReportGenerator.generateReport(MONITOR, Path.of("target/human-in-the-loop.html"));
    }
}
