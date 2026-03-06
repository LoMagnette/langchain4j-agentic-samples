# From AI to Agent with LangChain4j

Conference talk exploring how to move from simple AI calls to structured agentic systems using [LangChain4j](https://docs.langchain4j.dev/tutorials/agents).

## About

You've built AI features into your application — models are wrapped in services, RAG is in place, tools are wired. Then requirements evolve. A single response is no longer enough. You need steps that follow each other, branches based on decisions, retries when things fail, and sometimes multiple actions happening at the same time.

This talk shows how to structure that logic using LangChain4j's agentic APIs, moving step by step from basic agents to full workflow patterns.

## Samples

All samples use a Star Wars theme and run against a local [Ollama](https://ollama.com/) instance (`llama3.2:1b`).

| # | Sample | Pattern |
|---|--------|---------|
| S01 | `S01_BasicAgent` | Single agent with `@Agent` and `@UserMessage` |
| S02 | `S02_SharedState` | Shared state via `AgenticScope` |
| S03 | `S03_SequentialWorkflow` | Sequential pipeline of agents |
| S04 | `S04_LoopWorkflow` | Loop with review feedback |
| S05 | `S05_ParallelWorkflow` | Parallel agent execution |
| S06 | `S06_ConditionalWorkflow` | Conditional branching |
| S07 | `S07_ComposingPatterns` | Combining multiple patterns |
| S08 | `S08_ErrorHandling` | Error handling as a first-class concern |
| S09 | `S09_GoalOriented` | Goal-oriented planning |
| S10 | `S10_Supervisor` | Supervisor agent |
| S11 | `S11_HybridAgents` | Mixing AI and non-AI agents |
| S12 | `S12_HumanInTheLoop` | Human-in-the-loop interactions |

## Prerequisites

- Java 25+
- [Ollama](https://ollama.com/) running locally with `llama3.2:1b` pulled
- Maven

## Running

```bash
# Pull the model
ollama pull llama3.2:1b

# Run a sample
mvn compile exec:java -Dexec.mainClass="be.lomagnette.agentic.samples.S01_BasicAgent"
```

## Dependencies

- [langchain4j](https://github.com/langchain4j/langchain4j) 1.12.x
- [langchain4j-ollama](https://github.com/langchain4j/langchain4j) 1.12.1
- [langchain4j-agentic](https://github.com/langchain4j/langchain4j) 1.12.1-beta21
- [langchain4j-agentic-patterns](https://github.com/langchain4j/langchain4j) 1.12.1-beta21
