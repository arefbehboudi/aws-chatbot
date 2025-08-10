# AWS Cloud Assistant Chatbot

An **interactive AWS management assistant** with a **Vaadin-based web UI** and **LangChain4j-powered backend**, enabling you to securely manage AWS resources such as **EC2**, **S3**, and **IAM** through natural conversation.

## ✨ Features

- **Conversational AWS Control**  
  Create, start, stop, and terminate EC2 instances, manage S3 buckets, and retrieve IAM information.
- **Secure by Design**  
  No AWS credentials are exposed; destructive actions require explicit confirmation.
- **Real-Time Streaming Responses**  
  AI responses stream token-by-token for fast feedback.
- **Tool Integration**  
  Automatic detection and execution of AWS tool calls during chat.
- **Conversation History**  
  Messages are persisted with pagination and optional summarization.
- **JSON Tool Output**  
  Collapsible, syntax-highlighted JSON blocks for AWS API responses.
- **Multiple Conversations**  
  Start new chats, rename them, and switch easily via the sidebar.

## 🛠 Tech Stack

**Backend**
- Java 17  
- Spring Boot  
- LangChain4j (OpenAI integration + tool execution)  
- Reactor (Flux streaming)  
- AWS SDK (via `AWSTools` strategies)  
- PostgreSQL/MySQL (via `CustomJdbcChatMemoryRepositoryImp`)

**Frontend**
- Vaadin Flow  
- Highlight.js for code highlighting  
- Live updates via Vaadin Push

## 📦 Setup

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- AWS credentials configured (e.g., via `~/.aws/credentials` or environment variables)
- Database (PostgreSQL/MySQL)

### 2. Configure
Update `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cloud_assistant
spring.datasource.username=your_user
spring.datasource.password=your_pass

openai.api.key=sk-...
