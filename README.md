# Cloud Assistant MCP (Spring Boot + Vaadin + LangChain4j)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/projects/jdk/17/)
[![Vaadin](https://img.shields.io/badge/UI-Vaadin-00b4f0)](https://vaadin.com/)
[![LangChain4j](https://img.shields.io/badge/LLM-LangChain4j-green)](https://github.com/langchain4j/langchain4j)

An AWS management chatbot built with **Spring Boot** (backend), **Vaadin** (UI), and **LangChain4j** for LLM integration.  
It supports managing **EC2** and **S3** resources, streaming chat responses, and persisting conversation history in a database.

---

## 🎥 Demo

Below is a short demo showing how Cloud Assistant MCP interacts with the user and manages AWS resources such as EC2 and S3:

![Cloud Assistant Demo](docs/demo.gif)

---

## ✨ Features
- Real-time **streaming chat responses**
- **AWS Tools**:
  - EC2: create, list, describe, start/stop/reboot/terminate, tag/rename
  - S3: create/delete buckets, list buckets/objects, head/copy/delete objects, presigned GET/PUT URLs
- Persistent **conversation memory** with JDBC/JPA
- **Vaadin UI** (no separate SPA required)
- Safety rules enforced via System Prompt (confirmation required for destructive/expensive ops)

---

## 🧱 Architecture Overview

```

Vaadin UI ── ViewChatService ── ConversationService (Flux/stream)
│
▼
ChatService  ──(Tool Calling)──▶  AWSEc2Tools / AWSS3Tools
│
▼
CustomJdbcChatMemoryRepositoryImp (JDBC/JPA Persistence)

````

---

## ⚙️ Prerequisites
- Java 17+ (Java 21 recommended)
- Maven or Gradle
- **MySQL** running
- AWS credentials configured (`~/.aws/credentials` or environment variables)
- API key for your LLM provider (Groq in this configuration)

---

## 🔐 Configuration

### `application.properties`
```properties
spring.application.name=cloud-assistant-mcp
spring.main.banner-mode=off

spring.datasource.url=jdbc:mysql://127.0.0.1:3306/cloud-assistant
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.tomcat.initSQL=SET sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''));
spring.jpa.database=mysql
spring.jpa.open-in-view=false

server.port=5001

langchain4j.open-ai.streaming-chat-model.api-key=${OPEN_AI_API_KEY}
langchain4j.openai.streaming-chat-model.base-url=https://api.groq.com/openai/v1
langchain4j.open-ai.streaming-chat-model.model-name=moonshotai/kimi-k2-instruct

langchain4j.open-ai.chat-model.api-key=${OPEN_AI_API_KEY}
langchain4j.openai.chat-model.base-url=https://api.groq.com/openai/v1
langchain4j.open-ai.chat-model.model-name=moonshotai/kimi-k2-instruct
````

### Environment Variables

```bash
export OPEN_AI_API_KEY=sk-xxxx
export AWS_REGION=eu-central-1
```

### MySQL Setup

```sql
CREATE DATABASE IF NOT EXISTS `cloud-assistant`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 🚀 Run

### Maven

```bash
mvn clean spring-boot:run
```

### Gradle

```bash
./gradlew bootRun
```

### Access

* UI: `http://localhost:5001/`

---

## 🧠 Model & Streaming

* Uses **OpenAiStreamingChatModel** for live responses and **OpenAiChatModel** for synchronous tasks (e.g., conversation title generation).
* Configured with **Groq** API (`https://api.groq.com/openai/v1`) and model:

  * `moonshotai/kimi-k2-instruct`

---

## 🧰 AWS Tools

### EC2 – `AWSEc2Tools`

* `aws_ec2_create`
* `aws_ec2_list`
* `aws_ec2_describe`
* `aws_ec2_start` / `aws_ec2_stop` / `aws_ec2_reboot` / `aws_ec2_terminate`
* `aws_ec2_tag` / `aws_ec2_rename`

### S3 – `AWSS3Tools`

* `aws_s3_create_bucket` / `aws_s3_delete_bucket`
* `aws_s3_list_buckets` / `aws_s3_list_objects`
* `aws_s3_set_versioning`
* `aws_s3_delete_object` / `aws_s3_copy_object`
* `aws_s3_head_object`
* `aws_s3_presign_get` / `aws_s3_presign_put`

---

## 🗃️ Conversation Persistence

* Managed by `CustomJdbcChatMemoryRepositoryImp`
* Best practices:

  * Use a **stable `chatId`** per turn
  * Use a **sequence field** instead of manipulating timestamps
  * Always `complete` the stream after the final AI response

---

## 🔒 Security

* IAM roles with **least privilege**
* Explicit **confirmation** required for destructive/expensive actions
* Sanitize logs (no secrets)
* Validate AWS region (especially for S3 bucket creation and AMIs)

---

## 🧪 Troubleshooting

* **Stream doesn’t complete** → ensure `sink.complete()` is called after final AI response
* **S3 copy errors** → use `copySource` correctly in AWS SDK v2
* **AMI not found** → default AMI is region-specific, resolve dynamically
* **ONLY\_FULL\_GROUP\_BY SQL error** → fixed by disabling in `application.properties` or DB config

---


## 🛣️ Roadmap

- Add **`dryRun/confirm` flag** for destructive operations (Terminate/Delete)
- Implement new S3 methods:
  - `s3_put_object`
  - `s3_get_object_base64`
- **Normalize tool output schema**:
  - Fields `ok`, `items`, `error`
- Add **conversation summarization** for long histories
- Provide **Dockerfile** and **Helm chart** for Kubernetes deployment
- Improve **ChatService**:
  - Ensure `sink.complete()` is always called
  - Use a stable `chatId` for each session
- Improve **ChatView (Vaadin UI)**:
  - Properly display **streaming responses** in real-time
  - Reload conversation history after page refresh
  - Add explicit **confirmation button** for destructive actions


---

## 🙌 Contributing

Contributions are welcome! Please ensure:

* Tool outputs remain consistent
* Add tests for new features
* Keep destructive operations behind explicit confirmations
