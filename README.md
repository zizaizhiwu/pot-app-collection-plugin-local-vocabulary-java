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

   默认端口 `8080`。打包后运行：`mvn clean package`，再 `java -jar target/pot-vocabulary-server-1.0.0.jar`。

## 接口

统一响应：`{ "code": 200, "message": "ok", "data": ... }`，`code` 与 HTTP 状态码一致。

### 1. 保存翻译结果

`POST /api/topics`

```json
{
  "sourceText": "hello",
  "sourceLang": "en",
  "targetLang": "",
  "provider": "OpenAI",
  "contentText": "你好；喂；哈喽",
  "contentJson": "{\"explanations\":[{\"trait\":\"int.\",\"explains\":[\"你好\"]}]}"
}
```

规则：

- 主题按 `source_text + source_lang + target_lang` 唯一，重复提交不会新建主题。
- 同一主题下按 `provider` 唯一，同一来源再次提交会更新该条结果。
- `sourceText` 必填且不超过 255 字符，`contentText` 必填。

### 2. 主题列表

`POST /api/topics/list`

```json
{
  "page": 1,
  "size": 20,
  "keyword": "hello"
}
```

`page` / `size` 定义在 `PageRequest` 基类中，`keyword` 定义在 `TopicListRequest` 子类中，可为空；请求体可以省略，默认第 1 页、每页 20 条。响应：

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

### 3. 主题详情

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
    "resultCount": 2,
    "createdAt": "2026-11-09 12:00:00",
    "updatedAt": "2026-11-09 12:05:00",
    "results": [
      {
        "id": 1,
        "provider": "OpenAI",
        "contentText": "你好；喂；哈喽",
        "contentJson": "{\"explanations\":[...]}",
        "createdAt": "2026-11-09 12:00:00",
        "updatedAt": "2026-11-09 12:00:00"
      },
      {
        "id": 2,
        "provider": "DeepL",
        "contentText": "你好",
        "createdAt": "2026-11-09 12:05:00",
        "updatedAt": "2026-11-09 12:05:00"
      }
    ]
  }
}
```

主题不存在时返回 HTTP 404。

## 表结构

- `vocabulary_topic`：主题表，一条记录对应生词本中的一个词/句。
- `vocabulary_result`：翻译结果表，`topic_id` 外键关联主题，`(topic_id, provider)` 唯一，删除主题时级联删除。

建表语句见 `sql/schema.sql`。
