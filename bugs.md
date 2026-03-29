

 

已创建 4 个待办事项

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/pom.xml#1-1)，行 1 到 30

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/package.json#1-1)，行 1 到 20

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/package.json#20-20)，行 20 到 36

先启动后端。 

在终端中运行

Ran terminal command: cd e:\workspace\backend\company\vision-repo\vision-admin; mvn spring-boot:run -Dspring-boot.run.profiles=dev 2>&1

Ran terminal command: cd e:\workspace\backend\company\vision-repo\vision-admin; mvn spring-boot:run "-Dspring-boot.run.profiles=dev"

端口 8080 已被占用，先释放它。 

Ran terminal command: for /f "tokens=5" %a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /F /PID %a

Ran terminal command: $p = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -First 1; if ($p) { Stop-Process -Id $p -Force; "Killed PID $p" } else { "Port 8080 is free" }

端口已释放，重新启动后端。  

Ran terminal command: cd e:\workspace\backend\company\vision-repo\vision-admin; mvn spring-boot:run "-Dspring-boot.run.profiles=dev"

已选中后台终端输出

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01QhzoxgBdUFHFvDaVPEZDED__vscode-1774708151502/content.txt#1-1)，行 1 到 30

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01QhzoxgBdUFHFvDaVPEZDED__vscode-1774708151502/content.txt#30-30)，行 30 到 80

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01QhzoxgBdUFHFvDaVPEZDED__vscode-1774708151502/content.txt#80-80)，行 80 到 117

这是之前那次失败的终端。让我用新终端重新启动。  

Ran terminal command: Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess

Ran terminal command: Stop-Process -Id 53896 -Force; Start-Sleep -Seconds 1; Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
Completed with input: Stop-Process -Id 53896 -Force; Start-Sleep -Seconds 1; Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue

端口已释放。现在启动后端。   

Ran terminal command: cd e:\workspace\backend\company\vision-repo\vision-admin; mvn spring-boot:run "-Dspring-boot.run.profiles=dev"

已选中后台终端输出

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01LJMb6n3k6DiubDPW1AsnD4__vscode-1774708151509/content.txt#60-60)，行 60 到 100

后端启动成功！`Tomcat started on port 8080`。现在启动前端。    

正在启动: *Start frontend service* (2/4)

Ran terminal command: cd e:\workspace\backend\company\vision-repo\frontend; npm install 2>&1 | Select-Object -Last 5

Ran terminal command: npm run dev

工作目录不对，需要明确进入 frontend 目录。    

Ran terminal command: npm run dev

The tool keeps running npm in the wrong directory. Let me try a different approach.      

Ran terminal command: Push-Location e:\workspace\backend\company\vision-repo\frontend; Get-Location

Ran terminal command: npx vite --host

前端也启动成功了！后端 `http://localhost:8080`，前端 `http://localhost:5173`。 

正在启动: *Browser automated testing* (3/4)

现在进行浏览器自动化测试。先加载浏览器工具。     好，工具已加载。让我打开前端页面开始测试。      

已运行 `new_page`
Completed with input: {
  "url": "http://localhost:5173"
}

已运行 `take_screenshot`
Completed with input: {}

Dashboard 页能加载。我来检查控制台错误，然后逐个页面测试。      

已运行 `list_console_messages`
Completed with input: {}

Dashboard 页面有渲染错误。查看网络请求：     

已运行 `list_network_requests`
Completed with input: {}

API 请求都是 200（由 MSW mock 处理）。Dashboard 有 Vue 渲染错误。让我先查看 DashboardPage 源码，然后继续测试其他页面。      

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/pages/dashboard/DashboardPage.vue#1-1)，行 1 到 100

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/pages/dashboard/DashboardPage.vue#100-100)，行 100 到 207

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/dashboard.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/types/index.ts#1-1)，行 1 到 100

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/types/index.ts#100-100)，行 100 到 177

Good, now let me check the mock data to understand what's being returned:       

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/mocks/handlers.ts)

Now let me navigate to the other pages and check for errors. Let me test each page:        

已运行 `navigate_page`
Completed with input: {
  "url": "http://localhost:5173/cameras"
}

已运行 `take_screenshot`
Completed with input: {}

摄像头页面：分组树显示了但名称渲染异常（只显示图标不显示文字），表格无数据显示"No Data"。查看控制台错误：        

已运行 `list_console_messages`
Completed with input: {}

已运行 `list_network_requests`
Completed with input: {}

Camera 页面有 500 错误在 `/api/v1/video-platforms`。让我检查 Vite 配置和继续测试其他页面：         

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/vite.config.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/mocks/browser.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/main.ts)

关键发现：MSW 未在 main.ts 中启动，所有 API 请求直接通过 Vite proxy 转发到后端。让我检查前端 API client 和后端返回格式的兼容性：           

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/client.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/camera.ts#1-1)，行 1 到 50

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/dashboard.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/camera.ts)

Now let me check the backend response wrapper:          

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/common/response/R.java#1-1)，行 1 到 60

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/common/response/PageResult.java#1-1)，行 1 到 30

现在让我继续测试其他页面，并检查后端返回的实际数据格式：           

已运行 `navigate_page`
Completed with input: {
  "url": "http://localhost:5173/models"
}

已运行 `take_screenshot`
Completed with input: {}

已压缩对话

已运行 `list_console_messages`
Completed with input: {}

已运行 `list_network_requests`
Completed with input: {}

正在启动: *Test Rules page* (2/5)

已运行 `navigate_page`
Completed with input: {
  "url": "http://localhost:5173/rules",
  "type": "url"
}

已运行 `take_screenshot`
Completed with input: {}

已运行 `list_console_messages`
Completed with input: {}

已运行 `list_network_requests`
Completed with input: {}

Rules page has a 500 error on `/api/v1/rules`. Now let me test the Inference page. 

正在启动: *Test Inference page* (3/5)

已运行 `navigate_page`
Completed with input: {
  "type": "url",
  "url": "http://localhost:5173/inference"
}

已运行 `take_screenshot`
Completed with input: {}

已运行 `list_console_messages`
Completed with input: {}

已运行 `list_network_requests`
Completed with input: {}

正在启动: *Analyze root causes of bugs* (4/5)

Now let me check the backend logs for the 500 errors and read the relevant frontend stores and backend controllers. 

已选中后台终端输出

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01EsPLBaRmsNAMoEkPFDzsky__vscode-1774708151557/content.txt#1-1)，行 1 到 100

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01EsPLBaRmsNAMoEkPFDzsky__vscode-1774708151557/content.txt#1-1)，行 1 到 50

Let me look at the full backend logs to find all errors. Let me also read the critical source files. 

Ran terminal command: Get-Content "e:\workspace\backend\company\vision-repo\vision-admin\logs\vision-admin.log" -Tail 200 | Select-String -Pattern "ERROR|Exception|column.*does not exist" -Context 2,0

Found two key backend errors. Now let me read the frontend stores, backend entities and DB schema to understand all issues. 

Research data flow between frontend and backend

搜索匹配 `**/vision-admin/**/R.java` 的文件，1 个匹配项

搜索匹配 `**/vision-admin/**/PageResult.java` 的文件，1 个匹配项

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/client.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/dashboard.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/camera.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/model.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/rule.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/inference.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/dashboard.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/camera.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/model.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/rule.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/inference.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/common/response/R.java)

搜索匹配 `**/vision-admin/**/Rule.java` 的文件，1 个匹配项

搜索匹配 `**/vision-admin/**/InferenceController.java` 的文件，1 个匹配项

搜索与 `**/vision-admin/**/InferenceMapper.xml` 匹配的文件，2 匹配项

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/common/response/PageResult.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/pages/dashboard/DashboardPage.vue#1-1)，行 1 到 100

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/pages/model/ModelPage.vue#1-1)，行 1 到 100

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/inference/controller/InferenceController.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/rule/entity/Rule.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/resources/mapper/inference/InferenceMapper.xml)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/resources/db/migration/V1__init_schema.sql)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/resources/db/migration/V2__init_data.sql)

搜索文本 `deployed` (`**/vision-admin/src/main/resources/db/migration/**`)，无结果

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/types/index.ts)

搜索与 `**/vision-admin/src/main/resources/db/migration/*.sql` 匹配的文件，2 匹配项

搜索文本 `deployed` (`**/vision-admin/src/main/java/com/vision/rule/**`)，8 个结果

搜索文本 `IPage` (`**/vision-admin/src/main/java/com/vision/inference/**`)，9 个结果

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/rule/service/RuleService.java#80-80)，行 80 到 100

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/inference/mapper/InferenceMapper.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/inference/service/InferenceService.java#40-40)，行 40 到 100

搜索文本 `getRules` (`**/vision-admin/src/main/java/com/vision/rule/**`)，无结果

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/rule/dto/RuleVO.java)

搜索匹配 `**/vision-admin/**/RuleController.java` 的文件，1 个匹配项

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/rule/controller/RuleController.java)

已创建记忆文件 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/memory-tool/memories/MjY1YjE5YmQtZDAzMi00NDNhLTgwZmEtMzAyMzc0Y2FlYTgx/data-flow-analysis.md)

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01H4xnxH35ggcvWCnVcQYYhP__vscode-1774708151561/content.txt#1-1)，行 1 到 250

Excellent research. Now let me read the key frontend files that have rendering issues to understand exactly what's breaking.   

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/dashboard.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/model.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/dashboard.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/model.ts)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/client.ts)

Now let me read the backend controllers for dashboard and models to understand what they return. 

Find backend controllers and entities

搜索匹配 `**/vision-admin/**/DashboardController.java` 的文件，1 个匹配项

搜索匹配 `**/vision-admin/**/ModelController.java` 的文件，1 个匹配项

搜索匹配 `**/vision-admin/**/CameraController.java` 的文件，1 个匹配项

搜索匹配 `**/vision-admin/**/InferenceController.java` 的文件，1 个匹配项

搜索匹配 `**/vision-admin/**/Rule.java` 的文件，1 个匹配项

搜索与 `**/vision-admin/**/V1__init_schema.sql` 匹配的文件，2 匹配项

搜索匹配 `**/vision-admin/**/InferenceQueryDTO.java` 的文件，1 个匹配项

搜索匹配 `**/vision-admin/**/InferenceMapper.java` 的文件，1 个匹配项

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/dashboard/controller/DashboardController.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/model/controller/ModelController.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/camera/controller/CameraController.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/inference/controller/InferenceController.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/rule/entity/Rule.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/resources/db/migration/V1__init_schema.sql)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/inference/mapper/InferenceMapper.java)

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/inference/dto/InferenceQueryDTO.java)

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01BrxVDFumAwcQoBauKSuyk2__vscode-1774708151606/content.txt#1-1)，行 1 到 500

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01BrxVDFumAwcQoBauKSuyk2__vscode-1774708151606/content.txt#500-500)，行 500 到 600

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01BrxVDFumAwcQoBauKSuyk2__vscode-1774708151606/content.txt#600-600)，行 600 到 700

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01BrxVDFumAwcQoBauKSuyk2__vscode-1774708151606/content.txt#700-700)，行 700 到 800

读取 [](file:///c%3A/Users/seefly/AppData/Roaming/Code/User/workspaceStorage/94085e83db757a736c2094f8e0c38941/GitHub.copilot-chat/chat-session-resources/265b19bd-d032-443a-80fa-302374caea81/toolu_bdrk_01BrxVDFumAwcQoBauKSuyk2__vscode-1774708151606/content.txt#800-800)，行 800 到 830

Now let me read the InferenceService, DashboardPage.vue, ModelPage.vue and CameraPage.vue to complete the analysis.    

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/pages/dashboard/DashboardPage.vue#1-1)，行 1 到 200

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/pages/model/ModelPage.vue#1-1)，行 1 到 200

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/pages/camera/CameraPage.vue#1-1)，行 1 到 200

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/api/modules/camera.ts#1-1)，行 1 到 60

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/stores/camera.ts)

搜索正则表达式 `video-platform|VideoPlatform` (`**/vision-admin/src/**/*.java`)，无结果

搜索正则表达式 `VideoPlatform|video.platform` (`**/vision-admin/**"`)，无结果

搜索匹配 `**/InferenceService.java` 的文件，1 个匹配项

读取 [](file:///e%3A/workspace/backend/company/vision-repo/vision-admin/src/main/java/com/vision/inference/service/InferenceService.java#1-1)，行 1 到 80

正在启动: *Compile comprehensive bug report* (5/5)

Now let me also verify the Dashboard page's specific error by checking what the backend actually returns. 

Ran terminal command: Invoke-RestMethod -Uri "http://localhost:8080/api/v1/dashboard/stats" -Method Get | ConvertTo-Json -Depth 5

Ran terminal command: Invoke-RestMethod -Uri "http://localhost:8080/api/v1/models?page=1&size=20" -Method Get | ConvertTo-Json -Depth 5

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/types/index.ts#1-1)，行 1 到 150

读取 [](file:///e%3A/workspace/backend/company/vision-repo/frontend/src/types/index.ts#150-150)，行 150 到 177

已完成: *Compile comprehensive bug report* (5/5)

以下是对所有页面自动化测试后发现的 **完整 Bug 列表**：

---

## Bug 1 (严重): 前后端数据格式不匹配 — 影响所有页面

**现象**: Dashboard 渲染报错、Models 列表为空、Cameras 表格无数据

**根因**: 后端所有接口返回 `R<T>` 包装格式：
```json
{ "code": 200, "message": "success", "data": { ... } }
```

前端 client.ts 的 Axios 拦截器 `response.data` 只去掉了 Axios 层，返回的仍是完整的 `R<T>` 对象。而所有 Store 直接将此对象当作业务数据使用：

- dashboard.ts: `stats.value = s as any` → stats 是 `{code, message, data}` 而非 `DashboardStats`
- model.ts: `models.value = res` → res 是 `{code, message, data: {items, total}}` 而非 `Model[]`
- camera.ts: `cameras.value = res.items` → res.items 为 `undefined`

**修复方案**: 修改 Axios 拦截器自动解包 `R<T>`:
```typescript
client.interceptors.response.use(
  (response) => response.data.data,  // 解包到业务数据层
  ...
)
```

---

## Bug 2 (严重): Dashboard 字段名不一致

**现象**: 即使修复 Bug 1，Dashboard 统计卡片仍会显示 0

**根因**: 前端 index.ts 字段名与后端 `DashboardStatsVO` 完全不同：

| 前端字段 | 后端实际返回字段 |
|---|---|
| `todayAnalyses` | `todayInferenceCount` |
| `todayAnalysesTrend` | `todayInferenceChange` |
| `todayAlerts` | `todayAlertCount` |
| `todayAlertsTrend` | `todayAlertChange` |
| `totalCameras` | `totalCameraCount` |
| `aiEnabledCameras` | `aiEnabledCameraCount` |
| `weeklyInferenceCount` | 后端 stats 接口不返回此字段 |

---

## Bug 3 (严重): 数据库缺少 `deployed` 列 — Rules 页 500 报错

**现象**: `GET /api/v1/rules` 返回 500

**根因**: Rule.java 实体定义了 `private Boolean deployed;`，但 V1__init_schema.sql 的 `rule` 表中没有 `deployed` 列。

**报错**: `PSQLException: ERROR: column "deployed" does not exist`

**修复**: 新增 Flyway 迁移：
```sql
ALTER TABLE rule ADD COLUMN deployed BOOLEAN DEFAULT FALSE;
```

---

## Bug 4 (严重): InferenceMapper 分页查询错误 — Inference 页 500 报错

**现象**: `GET /api/v1/inference` 返回 500

**根因**: InferenceMapper.java 的 `selectPageByCondition()` 方法用 `@Select` 注解手动写了 `LIMIT/OFFSET`，但返回类型是 `IPage<InferenceRecord>`。MyBatis-Plus 要求方法参数中必须有 `Page` 对象才能填充 `IPage`。

**报错**: `MybatisPlusException: can't found IPage for args!`

**修复**: 将返回类型改为 `List<InferenceRecord>`，在 Service 层手动构建 Page；或添加 `Page` 参数。

---

## Bug 5 (中等): 缺少 VideoPlatform 后端接口 — Camera 页控制台报错

**现象**: Camera 页加载时控制台报 500，视频平台列表为空

**根因**: 前端 camera.ts 调用 `GET /api/v1/video-platforms`，但后端完全没有 `VideoPlatformController`。`video_platform` 表在数据库中已存在，但没有对应的 Java Controller/Service/Mapper。

**修复**: 创建 `VideoPlatformController`、`VideoPlatformService`、`VideoPlatformMapper` 等后端代码。

---

## Bug 6 (中等): Model `engineSupport` 类型不匹配

**根因**: 前端 index.ts 定义 `engineSupport: string[]`（数组），模板中用 `v-for="e in model.engineSupport"` 遍历。但后端数据库 `engine_support` 是 `VARCHAR(200)` 字符串，返回值类似 `"TensorRT,ONNX"`，不是 JSON 数组。遍历字符串会逐字符输出。

---

## Bug 总结

| # | 严重度 | 模块 | 问题 | 影响 |
|---|---|---|---|---|
| 1 | **严重** | 全局 | Axios 未解包 R\<T\> | 所有页面数据无法正确渲染 |
| 2 | **严重** | Dashboard | VO 字段名不匹配 | 统计卡片全部显示 0 |
| 3 | **严重** | Rules | DB 缺 `deployed` 列 | 规则页 500 崩溃 |
| 4 | **严重** | Inference | Mapper 分页参数错误 | 推理历史页 500 崩溃 |
| 5 | **中等** | Camera | 缺少 VideoPlatform API | 视频平台功能不可用 |
| 6 | **中等** | Model | engineSupport 类型错误 | 引擎标签渲染异常 | 

