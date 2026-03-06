package be.lomagnette.agentic.samples;

import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;

public class DroidListener implements AgentListener {
    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        IO.println("Agent called:"+ agentRequest.agentName()+ " with : "+agentRequest.inputs());
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        IO.println("Agent Done"+ agentResponse.agentName()+ " with : "+agentResponse.output());
    }
}
