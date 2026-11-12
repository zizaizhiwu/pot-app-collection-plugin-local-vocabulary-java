# pot-vocabulary-server

Pot 收集插件「本地生词本」的后端服务：接收翻译结果、按主题归并、提供列表与详情查询。

本项目模块目录为 `pot-app-collection-plugin-local-vocabulary-java`，Maven 坐标与产物名仍是 `pot-vocabulary-server`。

## 环境

- JDK 8+（当前基于 Spring Boot 2.7.18 构建，可直接用 JDK 8 运行）
- MySQL 5.7 / 8.0
- Maven 3.6+

## 启动步骤

1. 建库建表：

   ```bash
   mysql -u root -p < sql/schema.sql
   ```

2. 修改 `src/main/resources/application.yml` 中的数据库地址、库名、账号密码（当前是占位值）。

3. 启动：

   ```bash
   mvn spring-boot:run
   ```

   默认端口 `8050`（见 `application.yml`）。打包后运行：`mvn clean package`，再 `java -jar target/pot-vocabulary-server-1.0.0.jar`。

## 接口

统一响应：`{ "code": 200, "message": "ok", "data": ... }`，`code` 与 HTTP 状态码一致。

### 1. 保存翻译结果

`POST /api/topics`

```json
{
  "sourceText": "hello",
  "sourceLang": "en",
  "targetLang": "",
  "contentText": "你好；喂；哈喽",
  "contentJson": ""
}
```

规则：

- 主题按 `source_text + source_lang + target_lang` 唯一，重复提交不会新建主题。
- `provider` 为可选字段：Pot 收集插件拿不到翻译服务名，当前不传；表结构保留该字段，将来可区分来源。不传时按「该主题只保留一条结果」处理，重复收藏会更新该主题最早的一条结果，不会新增行。
- `sourceText` 必填且不超过 255 字符，`contentText` 必填。
- `sourceLang` / `targetLang` 建议不传：Pot 收集插件拿不到翻译方向，服务端会用 Lingua 自动识别（见下文「语言识别」）。传了就按传入值存。

### 2. 主题列表

`POST /api/topics/list`

```json
{
  "page": 1,
  "size": 20,
  "keyword": "hello",
  "collectionId": 1
}
```

`page` / `size` 定义在 `PageRequest` 基类中，`keyword`、`collectionId` 定义在 `TopicListRequest` 子类中，均可为空；`collectionId` 不传查全部收藏夹。请求体可以省略，默认第 1 页、每页 20 条。响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 1,
        "sourceText": "hello",
        "sourceLang": "en",
        "targetLang": "",
        "resultCount": 2,
        "createdAt": "2026-11-09 12:00:00",
        "updatedAt": "2026-11-09 12:00:00"
      }
    ]
  }
}
```

按创建时间倒序；`keyword` 对原文做模糊匹配；`size` 上限 200。

### 3. 收藏夹

```bash
# 收藏夹列表（首次调用自动创建「默认收藏集」，返回项带主题数量 topicCount）
curl http://127.0.0.1:8050/api/collections
# 新建收藏夹
curl -X POST http://127.0.0.1:8050/api/collections -H "Content-Type: application/json" -d '{"name":"日语"}'
# 删除收藏夹（其中主题移动到默认收藏集；默认收藏集不可删除，返回 400）
curl -X DELETE http://127.0.0.1:8050/api/collections/2
```

### 4. 移动 / 删除单词

```bash
# 移动到指定收藏夹
curl -X PUT http://127.0.0.1:8050/api/topics/1/collection -H "Content-Type: application/json" -d '{"collectionId":2}'
# 删除单词（级联删除其全部翻译结果）
curl -X DELETE http://127.0.0.1:8050/api/topics/1
```

### 5. 主题详情

`GET /api/topics/{id}`

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 1,
    "sourceText": "hello",
    "sourceLang": "en",
    "targetLang": "",
    "resultCount": 1,
    "createdAt": "2026-11-09 12:00:00",
    "updatedAt": "2026-11-09 12:05:00",
    "results": [
      {
        "id": 1,
        "contentText": "你好；喂；哈喽",
        "createdAt": "2026-11-09 12:00:00",
        "updatedAt": "2026-11-09 12:05:00"
      }
    ]
  }
}
```

`provider` 为空时不返回该字段（`default-property-inclusion: non_null` 只过滤 null，空串会返回 `""`，前端按空值处理）。

主题不存在时返回 HTTP 404。

### 6. 语言筛选与补全

```bash
# 库中已有单词的语言分布（按原文语言），用于列表页筛选条
curl http://127.0.0.1:8050/api/topics/languages
# → [{ "code": "ja", "label": "日语", "count": 2 }]

# 为存量数据补全缺失的源/目标语言，返回实际更新条数（幂等）
curl -X POST http://127.0.0.1:8050/api/topics/languages/backfill
```

列表接口请求体可带 `sourceLangs` 做多选过滤，不传或传空数组表示全部：

```json
{ "page": 1, "size": 20, "sourceLangs": ["ja", "en"] }
```

## 语言识别

Pot 的收集插件只能拿到原文和译文（调用签名 `collection(source, target, options)`，`options` 里只有插件配置和工具函数），**拿不到翻译方向**，所以语言由服务端识别：

- 依赖 `com.github.pemistahl:lingua:1.2.2`，候选语言限定为英/日/中/韩/法/德/西/俄/葡/意，短文本识别更稳
- 纯 ASCII 文本直接判为 `en`（避免 `hello` 这类短词被误判成意大利语/葡萄牙语）
- **原文语言识别为中文时统一改判为日文**（日文短词常被误判成中文，按使用习惯处理）
- 目标语言从译文文本识别，不做上面的改判
- 识别结果写回 `vocabulary_topic.source_lang` / `target_lang`；识别不出（纯符号/数字）时留空

## 表结构

- `vocabulary_collection`：收藏夹表，`is_default = 1` 的为默认收藏集（全局唯一，名称「默认收藏集」）。
- `vocabulary_topic`：主题表，一条记录对应生词本中的一个词/句，含 `source_lang` / `target_lang`。
- `vocabulary_topic_collection`：主题与收藏夹的多对多关联表，复合主键 `(topic_id, collection_id)`，两端外键级联删除。
- `vocabulary_result`：翻译结果表，`topic_id` 外键关联主题，`(topic_id, provider)` 唯一，删除主题时级联删除。

建表语句见 `sql/schema.sql`；已有库按顺序执行增量脚本 `sql/upgrade-collections.sql` → `sql/upgrade-topic-collections.sql`（全新安装不需要执行增量脚本）。
