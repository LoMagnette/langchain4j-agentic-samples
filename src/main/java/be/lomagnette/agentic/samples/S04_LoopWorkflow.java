package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * S04 - Loop Workflow: Attack Plan Review
 * Two agents iterate in a loop: Admiral Ackbar reviews the plan and
 * a plan reviser improves it. The loop runs for a maximum of 3 iterations.
 *   admiralAckbar (review) <-> planReviser (improve)
 *        exit after maxIterations reached
 */
public class S04_LoopWorkflow {

    public static class AttackPlan implements TypedKey<String> { }
    public static class ReviewFeedback implements TypedKey<String> { }

    public interface AdmiralAckbar {
        @Agent("Reviews an attack plan for traps and tactical weaknesses")
        @UserMessage("""
                You are Admiral Ackbar, legendary tactician of the Rebel Alliance. \
                Review this attack plan. If you find issues, explain them briefly (2-3 sentences) \
                and say REJECTED. If the plan is solid and accounts for Imperial traps, \
                say APPROVED. Be tough but fair.

                Attack plan: {{AttackPlan}}""")
        String review(@K(AttackPlan.class) String plan);
    }

    public interface PlanReviser {
        @Agent("Revises the attack plan based on Admiral Ackbar's feedback")
        @UserMessage("""
                You are a Rebel Alliance battle strategist. Revise the attack plan \
                to address Admiral Ackbar's concerns. Produce an improved plan in 3-4 sentences.

                Current plan: {{AttackPlan}}

                Ackbar's feedback: {{ReviewFeedback}}""")
        String revise(@K(AttackPlan.class) String plan, @K(ReviewFeedback.class) String feedback);
    }

    public interface AttackPlanReviewPipeline {
        @Agent("Reviews and iteratively improves an attack plan through Admiral Ackbar's scrutiny")
        String review(@K(AttackPlan.class) String plan);
    }

    void main() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        AdmiralAckbar ackbar = AgenticServices
                .agentBuilder(AdmiralAckbar.class)
                .chatModel(model)
                .outputKey(ReviewFeedback.class)  // writes Ackbar's feedback here
                .build();

        PlanReviser reviser = AgenticServices
                .agentBuilder(PlanReviser.class)
                .chatModel(model)
                .outputKey(AttackPlan.class)
                .build();

        AttackPlanReviewPipeline pipeline = AgenticServices
                .loopBuilder(AttackPlanReviewPipeline.class)
                .subAgents(ackbar, reviser)
                .maxIterations(3)
                .outputKey(AttackPlan.class)
                .build();

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
