# AI 智能记账系统

基于 Spring Boot 3 + Spring AI + Vue 3 的智能记账应用。用户可通过自然语言快速录入账单，AI 自动识别金额、分类、时间。

## 项目结构

```
ai-bookkeeping/
├── backend/              ← 后端（Spring Boot 3）
│   ├── src/
│   ├── sql/
│   ├── pom.xml
│   ├── Dockerfile
│   └── docker-compose.yml
├── frontend/             ← 前端（Vue 3）
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── .gitignore
└── README.md
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 + Java 17 |
| AI 框架 | Spring AI (OpenAI 兼容协议) |
| AI 模型 | DeepSeek + 智谱GLM（双模型策略，自动降级） |
| ORM | MyBatis Plus 3.5 |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7.x |
| 认证 | Spring Security + JWT |
| API 文档 | Knife4j 4.x |
| 前端框架 | Vue 3 + TypeScript + Vite 5 |
| UI 组件 | Element Plus |
| 状态管理 | Pinia |
| 图表 | ECharts |

## 功能模块

- **用户模块**：注册、登录、登出、个人信息管理
- **AI 记账**：自然语言 → 结构化账单（"今天午饭35元" → 自动解析）
- **手动记账**：传统表单录入
- **分类管理**：系统预设 22 个分类 + 自定义分类
- **统计分析**：月度总览、分类占比饼图、收支趋势折线图
- **个人中心**：信息修改、密码修改

## 快速开始

### 后端

```bash
cd backend

# 1. 启动依赖服务（MySQL + Redis）
docker-compose up -d

# 2. 配置 AI API Key（编辑 src/main/resources/application-dev.yml）
#    spring.ai.openai.api-key=your-deepseek-key
#    glm.api-key=your-glm-key

# 3. 构建运行
mvn clean package -DskipTests
java -jar target/ai-bookkeeping-1.0.0-SNAPSHOT.jar

# 4. 访问 API 文档
# http://localhost:8080/doc.html
```

### 前端

```bash
cd frontend
npm install
npm run dev

# 访问 http://localhost:5173
```

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

## License

MIT
