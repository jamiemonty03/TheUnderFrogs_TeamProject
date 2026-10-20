# AI Assistant Chat – UML Sequence Diagram

Traced from `AssistantController.chat` in instruments-service for one question, e.g.
"Why is Microsoft down today?". The AI never reads data itself: it asks for tools, and
`AssistantTools` calls the existing services. Every reply is checked against the tool data
before the user sees it.

```mermaid
sequenceDiagram
    actor User
    participant Security as Spring Security
    participant Controller as AssistantController
    participant Limiter as AssistantRateLimiter
    participant Service as AssistantService
    participant Client as AssistantModelClient
    participant Groq as Groq (gpt-oss)
    participant Tools as AssistantTools
    participant Data as Existing services
    participant Verifier as ReplyVerifier

    User->>Security: POST /api/assistant/chat {message} + Bearer token
    Security->>Security: verify JWT with auth-service JWKS

    rect rgb(255, 235, 235)
    alt no or invalid token
        Security--xUser: 401
    else SERVICE token
        Security--xUser: 403 (people only)
    end
    end

    Security->>Controller: chat(request, authentication)

    rect rgb(255, 235, 235)
    opt blank or over 500 characters
        Controller--xUser: 422 VAL-422
    end
    end

    Controller->>Limiter: check(user id)

    rect rgb(255, 235, 235)
    opt over 5 questions in the last minute
        Limiter--xUser: 429 AI-429
    end
    end

    Controller->>Service: respond(question)
    Service->>Service: system prompt (10 rules + current time) + question

    loop up to 4 tool rounds, then one last call with no tools
        Service->>Client: chat(messages, tool menu)
        Client->>Groq: POST /chat/completions
        Note over Client,Groq: 429 / 5xx / network error: retry 3 times,<br/>then switch to gpt-oss-20b
        Groq-->>Client: tool_calls, e.g. get_price_change(MSFT), get_news(MSFT)
        Client-->>Service: ModelReply

        loop each tool call (max 6 per round)
            Service->>Tools: execute(call)
            Tools->>Tools: clean symbol ("MSF T" to MSFT), validate arguments
            Tools->>Data: PriceSnapshotService / NewsCache / PriceHistoryService / StockService ...
            Data-->>Tools: prices, news, history, fundamentals
            Tools-->>Service: ToolResult (JSON, sources) or error result
        end
        Service->>Service: add tool results to the conversation
    end

    Groq-->>Service: written answer (via Client)
    Service->>Verifier: verify(reply, tool results, question + time)

    alt every figure and link is in the data
        Verifier-->>Service: passed
        Service-->>Controller: AssistantAnswer (aiSummary = true)
    else a figure or link is not in the data
        Verifier-->>Service: problems, e.g. "2.3% does not appear in the data"
        Service->>Client: chat(conversation + correction, no tools)
        Client->>Groq: POST /chat/completions
        Groq-->>Service: corrected answer (via Client)
        Service->>Verifier: verify again
        alt passes
            Service-->>Controller: AssistantAnswer (aiSummary = true, revised)
        else still fails
            Service->>Service: RawDataAnswer from the tool results
            Service-->>Controller: AssistantAnswer (aiSummary = false)
        end
    end

    rect rgb(255, 245, 220)
    opt AI unavailable at any point
        Service->>Service: RawDataAnswer from data gathered so far,<br/>or "unavailable, try again"
        Service-->>Controller: AssistantAnswer (aiSummary = false)
    end
    end

    Controller-->>User: 200 {reply, aiSummary, checksPassed, model, sources, disclaimer}
```

## Notes

- **The AI cannot reach any data on its own.** It only sees the text it is sent. It asks for a tool by
  name, and `AssistantTools` decides whether the request is valid and which existing service to call.
- **All tools are read-only.** There is no tool that places orders or reads portfolios, orders or cash.
- **The maths is done in Java** (`BigDecimal`), so the AI only quotes numbers. `ReplyVerifier` checks that
  every quoted figure appears in the tool data, allowing for honest rounding.
- **The disclaimer is added by the controller,** not the AI, so it is always present.
- **Not covered yet:** an audit log of each answer, and earnings reports (planned with SEC EDGAR).
