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
 *
 * Two agents iterate in a loop: Admiral Ackbar reviews the plan and
 * a plan reviser improves it. The loop runs for a maximum of 3 iterations.
 *
 *   admiralAckbar (review) <-> planReviser (improve)
 *        exit after maxIterations reached
 */
public class S04_LoopWorkflow {

    // TypedKeys for scope communication
    public static class AttackPlan implements TypedKey<String> { }
    public static class ReviewFeedback implements TypedKey<String> { }

    // Agent 1: Admiral Ackbar reviews the attack plan
    // He is notoriously cautious and will reject plans that smell like traps
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

    // Agent 2: Revises the plan based on Ackbar's feedback
    public interface PlanReviser {
        @Agent("Revises the attack plan based on Admiral Ackbar's feedback")
        @UserMessage("""
                You are a Rebel Alliance battle strategist. Revise the attack plan \
                to address Admiral Ackbar's concerns. Produce an improved plan in 3-4 sentences.

                Current plan: {{AttackPlan}}

                Ackbar's feedback: {{ReviewFeedback}}""")
        String revise(@K(AttackPlan.class) String plan, @K(ReviewFeedback.class) String feedback);
    }

    // Typed pipeline interface - invoke the loop with a single call.
    // @V seeds the initial "attackPlan" value into the scope.
    public interface AttackPlanReviewPipeline {
        @Agent("Reviews and iteratively improves an attack plan through Admiral Ackbar's scrutiny")
        String review(@V("AttackPlan") String plan);
    }

    public static void main(String... args) {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:1b")
                .build();

        // Build the two agents
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

        // Build the loop pipeline.
        // Agents iterate: ackbar reviews, reviser improves, repeat.
        // The loop runs for a maximum of 3 iterations.
        AttackPlanReviewPipeline pipeline = AgenticServices
                .loopBuilder(AttackPlanReviewPipeline.class)
                .subAgents(ackbar, reviser)
                .maxIterations(3)
                .outputKey(AttackPlan.class)
                .build();

        // Invoke the pipeline - one call runs the entire review loop
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
