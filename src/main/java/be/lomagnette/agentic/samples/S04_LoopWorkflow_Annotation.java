package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;

/**
 * S04 - Loop Workflow (Annotation / Declarative)
 * Same review loop as {@link S04_LoopWorkflow}, but expressed with
 * {@link LoopAgent} on the {@code AttackPlanReviewPipeline} interface.
 */
public class S04_LoopWorkflow_Annotation {

    public static class AttackPlan implements TypedKey<String> { }
    public static class ReviewFeedback implements TypedKey<String> { }

    public interface AdmiralAckbar {
        @Agent(description = "Reviews an attack plan for traps and tactical weaknesses",
                typedOutputKey = ReviewFeedback.class)
        @UserMessage("""
                You are Admiral Ackbar, legendary tactician of the Rebel Alliance. \
                Review this attack plan. If you find issues, explain them briefly (2-3 sentences) \
                and say REJECTED. If the plan is solid and accounts for Imperial traps, \
                say APPROVED. Be tough but fair.

                Attack plan: {{AttackPlan}}""")
        String review(@K(AttackPlan.class) String plan);
    }

    public interface PlanReviser {
        @Agent(description = "Revises the attack plan based on Admiral Ackbar's feedback",
                typedOutputKey = AttackPlan.class)
        @UserMessage("""
                You are a Rebel Alliance battle strategist. Revise the attack plan \
                to address Admiral Ackbar's concerns. Produce an improved plan in 3-4 sentences.

                Current plan: {{AttackPlan}}

                Ackbar's feedback: {{ReviewFeedback}}""")
        String revise(@K(AttackPlan.class) String plan, @K(ReviewFeedback.class) String feedback);
    }

    public interface AttackPlanReviewPipeline {
        @LoopAgent(description = "Reviews and iteratively improves an attack plan through Admiral Ackbar's scrutiny",
                typedOutputKey = AttackPlan.class,
                maxIterations = 3,
                subAgents = { AdmiralAckbar.class, PlanReviser.class })
        String review(@K(AttackPlan.class) String plan);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        var pipeline = AgenticServices.createAgenticSystem(AttackPlanReviewPipeline.class, model);

        String initialPlan = "Fly X-wings straight at the Death Star's main reactor port. " +
                             "No distractions, no diversions. Just a direct assault.";

        IO.println("=== Attack Plan Review Loop ===");
        IO.println("Initial plan: " + initialPlan);
        IO.println();

        String finalPlan = pipeline.review(initialPlan);

        IO.println("=== Final Plan ===");
        IO.println(finalPlan);
    }
}
