# 🤖 AI 智能记账系统

基于 Vue3 + Spring Boot + Spring AI 的智能记账系统，支持自然语言记账、AI 智能分类、统计分析。

## 🏗️ 项目结构

```
ai-bookkeeping/
├── backend/              # Spring Boot 后端
│   ├── src/              # Java 源码
│   ├── sql/              # 数据库初始化脚本
│   ├── pom.xml           # Maven 配置
│   ├── Dockerfile        # 后端镜像构建
│   └── docker-compose.yml # MySQL + Redis
├── frontend/             # Vue3 前端
│   ├── src/              # Vue 源码
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── docs/                 # 项目文档
├── .gitignore
└── README.md
```

## 🛠️ 技术栈

### 后端
- Spring Boot 3.2 + Java 17
- Spring AI (DeepSeek + GLM 双模型)
- MyBatis Plus
- Spring Security + JWT
- Redis 缓存
- Knife4j API 文档

### 前端
- Vue 3 (Composition API)
- Vite 5
- Element Plus
- Pinia + Vue Router
- ECharts
- Axios

## 🚀 快速开始

### 后端

```bash
cd backend

# 启动 MySQL + Redis
docker-compose up -d

# 配置 API Key
# 编辑 src/main/resources/application-dev.yml
# 填入 DEEPSEEK_API_KEY 和 GLM_API_KEY

# 编译运行
mvn clean package -DskipTests
java -jar target/ai-bookkeeping-1.0.0-SNAPSHOT.jar
```

后端启动后访问 http://localhost:8080/doc.html 查看 API 文档。

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问 http://localhost:3000。

## 📡 API 接口

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | POST /api/auth/register | 用户注册 |
| 认证 | POST /api/auth/login | 用户登录 |
| AI记账 | POST /api/bill/ai-parse | AI 解析自然语言 |
| 账单 | POST /api/bill | 创建账单 |
| 账单 | GET /api/bill/list | 账单列表 |
| 分类 | GET /api/category/list | 分类列表 |
| 统计 | GET /api/stat/monthly | 月度统计 |

## 📄 License

MIT
