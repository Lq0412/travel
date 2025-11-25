## RabbitMQ 改造计划（AI 重任务：回忆图为例）

### 1. 目标
- 将耗时 AI 任务（回忆图）从同步改为“入库→MQ→Worker 异步执行”，提升并发与稳定性。
- 保证可回退：开关可关闭 MQ 回退同步；消费者异常不影响主流程。

### 2. 范围
- 任务类型：回忆图（Memory Card），后续可扩展行程规划/对话总结。
- 组件：RabbitMQ（direct 交换机 + 正常队列 + DLQ）、AiTask 表、MemoryCard 表、消费者/定时补偿。
- 相关配置：`ai.mq.enabled`、`ai.mq.memory-card-enabled`、RabbitMQ 连接、线程池。

### 3. 架构与链路
1) 前端发起生成 → 后端创建 MemoryCard 记录 + AiTask 记录（taskId 本地唯一） → 发送 MQ（路由 `MEMORY_CARD`）。  
2) Consumer 收到消息 → 调用模型服务创建远端任务 `remoteTaskId` → 更新 MemoryCard.remoteTaskId & status=processing；AiTask.result 写入 remoteTaskId。  
3) Consumer 轮询远端状态（或交由定时补偿）：  
   - 成功：上传 COS，MemoryCard.status=success，写 imageUrl/history，AiTask.status=SUCCESS，result 写 cosUrl/remoteTaskId。  
   - 失败：MemoryCard.status=failed，写 error；AiTask.status=FAILED。  
   - 处理中：更新 result=PROCESSING，等待补偿/下次轮询。  
4) 前端轮询 `/ai/images/memory-card/status/{taskId}`，后端根据 remoteTaskId 查询远端状态并回写。

### 4. 数据与配置
- MemoryCard 表新增 `remote_task_id` 列（索引 idx_remote_task_id）。  
- AiTask 表：taskId、bizKey、status、result、error 等。  
- 配置：`ai.mq.enabled=true`，`ai.mq.memory-card-enabled=true`，RabbitMQ 连接、prefetch、DLQ。

### 5. 状态管理
- MemoryCard.status：pending / processing / success / failed。  
- AiTask.status：PENDING / RUNNING / SUCCESS / FAILED。  
- 远端任务 ID 仅存 remoteTaskId，不覆盖本地 taskId；查询时优先用 remoteTaskId。  
- 定时补偿：扫描 pending/processing 的 MemoryCard，调用远端状态并更新 MemoryCard + AiTask。

### 6. 风险与回退
- MQ 不可用或消费异常：关闭 `ai.mq.memory-card-enabled` 回到同步链路。  
- DashScope 限流/失败：配置重试/退避、DLQ；前端可重试生成。  
- 数据不一致：定时补偿校正 MemoryCard 与 AiTask；可一键脚本对齐状态。

### 7. 验证
- 开关 MQ 的 A/B 验证：同步模式基准、MQ 模式压测。  
- 用例：生成成功、生成失败、远端长时间 processing、网络异常、COS 上传失败回退原图 URL。  
- 观测：队列堆积、消费者日志、数据库状态一致性、前端轮询停止点。 
