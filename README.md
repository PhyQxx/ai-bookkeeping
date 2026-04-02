# AI 智能记账系统

基于 Spring Boot 3 + Spring AI 的智能记账后端服务。用户可通过自然语言快速录入账单，AI 自动识别金额、分类、时间。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 + Java 17 |
| AI 框架 | Spring AI (OpenAI 兼容协议) |
| AI 模型 | DeepSeek + 智谱GLM |
| ORM | MyBatis Plus 3.5 |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7.x |
| 认证 | Spring Security + JWT |
| API 文档 | Knife4j 4.x |

## 功能模块

- **用户模块**：注册、登录、个人信息管理
- **AI 记账**：自然语言 → 结构化账单（"今天午饭35元" → 自动解析）
- **手动记账**：传统表单录入
- **分类管理**：系统预设 + 自定义分类
- **统计分析**：月度总览、分类占比、消费趋势

## 快速开始

### 1. 启动依赖服务

```bash
docker-compose up -d
```

### 2. 配置 AI API Key

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-deepseek-api-key  # DeepSeek API Key

glm:
  api-key: your-glm-api-key              # 智谱GLM API Key
```

也可以通过环境变量配置：

```bash
export DEEPSEEK_API_KEY=sk-xxx
export GLM_API_KEY=xxx
```

### 3. 构建运行

```bash
mvn clean package -DskipTests
java -jar target/ai-bookkeeping-1.0.0-SNAPSHOT.jar
```

### 4. 访问 API 文档

浏览器打开：http://localhost:8080/doc.html

## API 接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册 | ❌ |
| POST | /api/auth/login | 用户登录 | ❌ |
| POST | /api/bill/ai-parse | AI 记账 | ✅ |
| POST | /api/bill | 手动创建账单 | ✅ |
| GET | /api/bill/list | 账单列表 | ✅ |
| PUT | /api/bill/{id} | 更新账单 | ✅ |
| DELETE | /api/bill/{id} | 删除账单 | ✅ |
| GET | /api/category/list | 分类列表 | ✅ |
| POST | /api/category | 创建分类 | ✅ |
| GET | /api/stat/monthly | 月度总览 | ✅ |
| GET | /api/stat/category-ratio | 分类占比 | ✅ |
| GET | /api/stat/trend | 消费趋势 | ✅ |

## 项目结构

```
ai-bookkeeping/
├── src/main/java/com/aibookkeeping/
│   ├── ai/                    # AI 模块（解析、分类、Prompt）
│   ├── config/                # 配置类
│   ├── controller/            # 控制器
│   ├── dto/                   # 请求 DTO
│   ├── entity/                # 数据库实体
│   ├── exception/             # 全局异常处理
│   ├── mapper/                # MyBatis Plus Mapper
│   ├── security/              # JWT + Spring Security
│   ├── service/               # 业务服务
│   ├── util/                  # 工具类
│   └── vo/                    # 视图对象
├── src/main/resources/
│   └── application.yml        # 配置文件
├── sql/init.sql               # 数据库初始化
├── docker-compose.yml         # Docker 编排
└── Dockerfile                 # 构建镜像
```

## License

MIT
