## 后续规划（在回忆图 MQ 打通基础上）

### 当前基础
- 回忆图链路已打通：本地 taskId + 远端 remoteTaskId，MQ 异步消费，MemoryCard 与 AiTask 状态闭环，前端轮询正常。
- 文档：`rabbitmq-plan.md`（改造计划）、`rabbitmq-notes.md`（实现/学习笔记）。

### 1. 扩展 MQ 到其他 AI 任务
- 行程规划：创建 AiTask + MQ 路由 `TRIP_PLAN`，Worker 异步调用大模型写入方案/COS，补偿与状态回写同回忆图。
- 对话总结/批量摘要：路由 `CHAT_SUMMARY`，按会话/批次维度异步处理，状态写 AiTask + 业务表。
- 公共能力：复用 AiTask 表、TaskMessageProducer、Reconcile 定时补偿。

### 2. 健壮性与观测
- 重试与退避：在 Consumer/模型调用层增加可配置重试（次数、退避），失败入 DLQ。  
- 补偿策略：现有定时轮询 MemoryCard，可抽象为通用 AiTask 补偿（按 type、status=pending/processing）。  
- 监控告警：队列堆积、消费失败、DashScope 错误率、AiTask 长期 RUNNING。  
- 压测：MQ 模式的吞吐与模型 QPS 上限，确认触发限流时的退避效果。

### 3. 前端体验
- 统一任务中心：轮询 `/ai/tasks/{taskId}` 展示所有异步任务状态，业务页面只展示结果。  
- 失败重试入口：前端支持“一键重试”调用对应 regenerate 接口。  
- 进度提示：远端 processing 时展示“排队/生成中”文案，超时兜底。

### 4. 运维与回滚
- 配置化开关：`ai.mq.enabled`、`ai.mq.memory-card-enabled` 等保持可灰度/回退。  
- 补偿工具：提供管理接口/脚本，按 tripId 或 taskId 对齐 MemoryCard 与 AiTask 状态。  
- 发布策略：先灰度开启 MQ，观测队列与失败率，再全量。

### 5. 安全与成本
- 模型调用限额：配额/限流配置（Spring limiter 或网关层），避免突发成本。  
- COS 存储清理：历史回忆图定期归档/删除策略（可选）。  
- 权限：确保任务查询接口校验 userId，前端仅展示本人任务。
