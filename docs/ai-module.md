# AI 模块说明

## 模块定位

`module/ai` 是项目中的独立 AI 能力模块，负责统一封装 OpenAI API 兼容模型调用、主备模型降级、配置绑定、错误脱敏和服务接口暴露。

模块设计目标：
- 可复用：业务模块通过接口直接调用，不感知 LangChain4J 细节
- 低耦合：Controller 仅做 HTTP 适配，核心能力不依赖 Web 层
- 可替换：后续可扩展更多模型实现或接入 RAG 链路，而不影响调用方
- 可测试：核心服务可通过假模型单元测试，Web 层可单独做接口测试

## 目录结构

```text
src/main/java/com/bingli/lihuaAgent/module/ai
├── api
│   ├── AiChatService.java
│   └── model
│       └── AiChatResult.java
├── config
│   ├── AiModuleConfiguration.java
│   └── AiModuleProperties.java
├── core
│   ├── AiGatewayChatService.java
│   └── AiModelClient.java
└── util
    └── AiErrorSanitizer.java
```

## 对外接口

### `AiChatService`

```java
@SystemMessage(fromResource = "system-prompt.txt")
AiChatResult chat(String userMessage)
```

### 输入参数 `userMessage`

- `userMessage`：用户消息，必填
- 系统提示词从 `system-prompt.txt` 自动加载

### 输出结果 `AiChatResult`

- `content`：模型返回内容
- `modelAlias`：命中的模型别名
- `modelName`：命中的实际模型名称
- `fallbackUsed`：是否发生降级
- `costMs`：本次调用耗时
- `inputTokens`：输入 token 数
- `outputTokens`：输出 token 数
- `totalTokens`：总 token 数

## 配置说明

配置前缀：`ai.models`

主要配置项：
- `logRequests`：是否记录请求日志
- `logResponses`：是否记录响应日志
- `primary`：主模型配置
- `fallbacks`：备用模型配置列表

单个模型配置字段：
- `enabled`：是否启用
- `alias`：模型别名
- `baseUrl`：OpenAI 兼容接口地址
- `apiKey`：访问密钥
- `modelName`：模型名称
- `timeout`：请求超时
- `maxRetries`：单模型内部重试次数

## 使用方式

### 在业务模块中注入

```java
@RequiredArgsConstructor
@Service
public class DemoService {

    private final AiChatService aiChatService;

    public String ask(String message) {
        return aiChatService.chat(message).getContent();
    }
}
```

### HTTP 调试入口

当前项目保留 `POST /api/ai/chat` 作为调试接口，仅做协议适配，内部委托 `AiChatService`。

请求体：

```json
{
  "message": "你好"
}
```

## 设计说明

- `api`：模块对外公开接口与返回模型
- `core`：主备降级、请求构造、响应转换等核心逻辑
- `config`：Spring Bean 装配与配置绑定
- `util`：异常消息脱敏，避免敏感信息写入日志
- `controller/AiController`：项目接入层，不属于模块核心
- `system-prompt.txt`：模块级系统提示词资源，由接口注解声明

## 测试覆盖

- `AiGatewayChatServiceTest`：验证主备降级、参数校验、主模型成功、全部失败、注解资源加载场景
- `AiControllerTest`：验证 HTTP 层到模块接口的映射是否正确
- `LihuaTemplateApplicationTests`：验证 Spring 上下文与模块装配可正常启动
