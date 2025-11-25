## RabbitMQ 引入与实践笔记（回忆图链路）

### 背景
- 回忆图生成调用外部图像模型，耗时长且易限流；同步接口体验差。
- 目标：异步化+可补偿，支持高并发，兼容老链路。

### 核心设计
- 交换机/队列：direct 交换机 `ai.task.direct`，路由键 `MEMORY_CARD`，队列 `ai.task.memory-card`，DLQ `ai.task.memory-card.dlq`。
- 任务表：AiTask 记录本地任务 ID、bizKey、status、result/error；MemoryCard 记录业务状态和远端 taskId。
- 远端任务 ID：新增 `remoteTaskId`，仅存模型侧任务号，不覆盖本地 taskId。
- 开关：`ai.mq.enabled`、`ai.mq.memory-card-enabled`，可回退同步。

### 流程（回忆图）
1) 创建 MemoryCard + AiTask（taskId 本地 UUID），发送 MQ。  
2) Consumer 调模型，获取 remoteTaskId，MemoryCard.status=processing，写 remoteTaskId；AiTask.result 写 remoteTaskId。  
3) 轮询远端：  
   - success：上传 COS，MemoryCard success + history，AiTask SUCCESS 写 cosUrl/remoteTaskId。  
   - failed：MemoryCard failed 写 error，AiTask FAILED。  
   - processing：更新 result=PROCESSING，交由补偿/下次轮询。  
4) 定时补偿：扫描 pending/processing 的 MemoryCard，用 remoteTaskId 查询远端并对齐 MemoryCard + AiTask。
5) 前端轮询 `/ai/images/memory-card/status/{taskId}`，后端按 remoteTaskId 查询远端并回写。

### 数据结构
- MemoryCard：新增 `remote_task_id` 列（索引 idx_remote_task_id）。  
- AiTask：status/result/error 管理异步任务状态；bizKey 去重。  
- 历史：成功时写 MemoryCardHistory（保留旧图）。

### 关键实现点
- Consumer 不再覆盖 MemoryCard.taskId，仅写 remoteTaskId。  
- 状态回写封装：markAiTaskProcessing/Success/Failed，保证 AiTask 收敛。  
- 定时任务/查询使用 remoteTaskId 调远端，缺失时回退本地 taskId（同步模式）。  
- 异常容错：DashScope 失败/限流 → 失败落库；COS 上传异常 → 回退原始 URL。

### 验证要点
- MQ 模式下生成成功/失败都能让 MemoryCard 与 AiTask 状态一致。  
- 远端长时间 processing 时：result=PROCESSING，补偿能最终收敛。  
- 开关回退：关闭 `ai.mq.memory-card-enabled` 回到同步模式仍可用。  
- 前端：taskId 轮询停止点正确，成功提示不重复。

### 实现细节（便于面试说明）
**整体流程（回忆图）**  
1) Controller `/ai/images/memory-card` → Service `MemoryCardServiceImpl.generateMemoryCard`：  
   - 校验 trip/用户/照片数；新建/复用 MemoryCard，状态置 pending，taskId=本地 UUID，remoteTaskId 置空。  
   - 创建 AiTask 记录（PENDING），bizKey=用户+trip+照片 hash。  
   - 生成 payload（prompt/照片 URL/模板/尺寸等）发送 MQ 路由 `MEMORY_CARD`。  
   - 返回本地 taskId 给前端，前端只拿这个 taskId 轮询。  
2) Consumer `MemoryCardTaskConsumer.handleMemoryCard`：  
   - 查 AiTask，跳过已完成的；标记 RUNNING。  
   - 调 DashScope 创建远端任务，得到 remoteTaskId。  
   - 更新 MemoryCard.remoteTaskId + status=processing，调用 `markAiTaskProcessing` 把 remoteTaskId 写入 AiTask.result（status=PROCESSING）。  
   - 轮询远端状态（简单 3 次 x 2s）：  
     - 成功：COS 上传，MemoryCard success + imageUrl + history，`markAiTaskSuccess`(写 cosUrl/remoteTaskId)；  
     - 失败：MemoryCard failed + error，`markAiTaskFailed`；  
     - 仍 processing：仅写 PROCESSING result，等待定时补偿/下次轮询。  
3) 定时补偿/状态查询 `MemoryCardServiceImpl.updateMemoryCardStatus/getMemoryCardStatus`：  
   - 通过 `resolveRemoteTaskId` 取 remoteTaskId（优先 memory_card.remote_task_id，再从 AiTask.result 解析），不存在且 MQ 开关打开时不打远端；同步模式回退本地 taskId。  
   - 用远端 taskId 查询模型状态，刷新 MemoryCard + AiTask（Success/Failed/Processing），处理 COS 上传和历史写入。  
4) 数据结构与状态：  
   - MemoryCard：taskId（本地不变）、remoteTaskId（远端）、status、imageUrl、errorMessage、retryCount、历史表。  
   - AiTask：taskId、bizKey、status(PENDING/RUNNING/SUCCESS/FAILED)、result(JSON: remoteTaskId/cosUrl/status)、errorMessage。  
5) 前端（TripMemoryPage.vue）：  
   - 创建任务后只用本地 taskId 轮询 `/ai/images/memory-card/status/{taskId}`。  
   - 成功提示加幂等保护（success 只弹一次）；超时/失败有文案提示。  
6) 回退与容错：  
   - 开关 `ai.mq.enabled` / `ai.mq.memory-card-enabled` 关闭时走老的同步链路。  
   - COS 上传失败：回退使用模型原始 URL 也标记成功。  
   - 远端 taskId 缺失或模型查询异常：不覆盖本地 taskId，等待补偿或同步模式。  

**关键代码参照**  
- Producer：`src/main/java/com/lq/travel/service/impl/MemoryCardServiceImpl.java`（generateMemoryCard、resolveRemoteTaskId、markAiTaskProcessing/Success/Failed）。  
- Consumer：`src/main/java/com/lq/travel/mq/MemoryCardTaskConsumer.java`。  
- 状态补偿：同 MemoryCardServiceImpl 的定时任务 `updateMemoryCardStatus`、状态查询 `getMemoryCardStatus`。  
- 数据结构：`MemoryCard`/`MemoryCardVO` 新增 `remoteTaskId`；SQL `memory_card` 表新增列与索引。  
- 前端轮询：`travel_frontend/src/pages/trips/TripMemoryPage.vue`（taskId 轮询 + 成功提示幂等）。

### 运维与排障
- 监控：队列堆积、消费者日志、DashScope 请求错误、数据库状态不一致。  
- 补偿：对 RUNNING 但 MemoryCard 已 success/failed 的 AiTask，可一键脚本对齐。  
- 回滚：关闭 MQ 开关，重启后端走同步链路；恢复后重新开启。 

• 你可以这样拆解回答（条理清晰，突出关键点）：

1. 为什么要做 MQ：回忆图调用外部图像模型耗时且易限流，改为“入库→MQ→Worker 异步”，提升并发与稳定性，同时保留开关回 退
   同步。
2. 整体链路（回忆图为例）：

- Controller/Service 创建 MemoryCard + AiTask（本地 taskId），生成 payload，发 MQ 路由 MEMORY_CARD，把本地 taskId 返回
  前端。
- Consumer 收消息 → 调模型创建远端任务 remoteTaskId → MemoryCard.status=processing，写 remoteTaskId；AiTask.result  写
  remoteTaskId/status=PROCESSING。
- Consumer 轮询远端（或交由定时补偿）：成功则上传 COS、MemoryCard success + history，AiTask SUCCESS（写 cosUrl/
  remoteTaskId）；失败则 MemoryCard failed，AiTask FAILED；仍 processing 则仅写 PROCESSING result。
- 定时补偿/状态查询使用 remoteTaskId 调模型，刷新 MemoryCard + AiTask，缺失时不强行查模型，等待补偿或回退同步。
- 前端只用初始 taskId 轮询 /ai/images/memory-card/status/{taskId}，成功提示幂等（只弹一次）。

3. 数据与状态：

- MemoryCard 新增 remote_task_id（索引），本地 taskId 不被覆盖，状态 pending/processing/success/failed。
- AiTask 记录任务状态与 result(JSON: remoteTaskId/cosUrl/status)；bizKey 去重。

4. 关键代码位置：

- Producer/状态回写：MemoryCardServiceImpl（generateMemoryCard、resolveRemoteTaskId、markAiTaskProcessing/Success/
  Failed、updateMemoryCardStatus）。
- Consumer：MemoryCardTaskConsumer（创建远端任务、轮询、回写）。
- 前端轮询：travel_frontend/src/pages/trips/TripMemoryPage.vue。
- SQL 变更：memory_card 表新增 remote_task_id + 索引。

5. 容错与回退：

- 开关 ai.mq.enabled / ai.mq.memory-card-enabled 关闭即回同步。
- COS 上传失败回退原始 URL 也标记成功；远端 taskId 缺失不覆盖本地 taskId，等待补偿。
- 补偿脚本/定时任务可对齐 MemoryCard 与 AiTask 状态。

一句话总结：本地 taskId 负责前端轮询和业务索引，remoteTaskId 负责调外部模型；Producer→MQ→Consumer→状态回写 + 定时补
偿，确保 MemoryCard 与 AiTask 状态一致，可开关回退。
