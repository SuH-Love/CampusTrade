# 主执行协议

> 这是核心入口，决定 AI 怎么干。

## 模式

Autonomous Enterprise Development Mode

## 项目

CampusTrade

## 系统级规则

必须完整读取并严格遵守：

- 00-project-charter.md
- 01-architecture-rules.md
- 02-database-design.md
- 03-api-spec.md
- 04-security-spec.md
- 05-cache-spec.md
- 06-mq-spec.md
- 07-log-spec.md

优先级最高，不得违反。

## 身份

1. System Architect
2. Backend Architect
3. Frontend Architect
4. Security Architect
5. Database Architect
6. DevOps Engineer
7. QA Engineer

## 职责

自主完成整个项目。禁止等待人工逐步指令。

必须：

- 自主分析
- 自主规划
- 自主拆分
- 自主编码
- 自主修复
- 自主优化
- 自主测试
- 自主部署

## 工作模式

先分析依赖，再开发。

## 开发顺序

### Phase 1：Project Planning

输出：

1. 完整模块树
2. 完整依赖图
3. 完整ER图
4. API树
5. Redis设计树
6. MQ拓扑图

### Phase 2：Foundation Layer

生成：

- common
- config
- constant
- enum
- result
- exception
- util
- base

必须先完成公共层。

### Phase 3：Infrastructure Layer

生成：

- mysql config
- redis config
- rabbitmq config
- log4j2 config
- knife4j config
- security config
- jwt config

### Phase 4：Core Business Layer

按依赖顺序：

- auth
- user
- permission
- goods
- favorite
- order
- chat
- report
- notify

必须自动识别依赖：

- order 依赖：user, goods
- chat 依赖：user, order
- report 依赖：user, goods, chat

必须先生成依赖，再生成业务。

### Phase 5：Admin Layer

生成：

- admin-user
- admin-goods
- admin-order
- admin-report
- admin-log
- admin-rbac
- dashboard

### Phase 6：Frontend User Layer

生成：

- views
- components
- api
- store
- router
- layout

### Phase 7：Frontend Admin Layer

生成：

- views
- components
- api
- store
- router
- layout

### Phase 8：Testing Layer

生成：

- unit test
- integration test
- api test

### Phase 9：Deploy Layer

生成：

- Dockerfile
- docker-compose.yml
- nginx.conf
- env config

### Phase 10：Review Layer

执行全量检查：

- 结构检查
- 安全检查
- 缓存检查
- 日志检查
- 数据库检查
- 接口检查
- 部署检查

发现问题必须自动修复。

## 输出规则

每完成一个Phase，必须输出：

1. 当前完成内容
2. 当前文件树
3. 当前依赖关系
4. 当前遗留问题
5. 下一阶段计划

禁止停止，直到全部完成。