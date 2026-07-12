# Описание базы данных

База данных содержит три основные таблицы:

1. `users`
2. `generation_requests`
3. `generated_assets`

Связи:

```text
User 1 -> N GenerationRequest
GenerationRequest 1 -> 0..1 GeneratedAsset
```

`GeneratedAsset` может отсутствовать, пока генерация находится в статусе `QUEUED` или `PROCESSING`.

## users

Таблица пользователей.

| Поле         | Тип            | Ограничения             | Описание                    |
|--------------|----------------|--------------------------|-----------------------------|
| id           | UUID           | PK                       | Идентификатор пользователя  |
| email        | String         | NOT NULL, UNIQUE         | Email пользователя          |
| passwordHash | String         | NOT NULL                 | Хеш пароля                  |


## generation_requests

Таблица запросов на генерацию.

| Поле        | Тип            | Ограничения     | Описание                                           |
|-------------|----------------|------------------|----------------------------------------------------|
| id          | UUID           | PK               | Идентификатор генерации                            |
| userId      | UUID           | FK, NOT NULL     | Пользователь, который создал генерацию             |
| prompt      | String         | NOT NULL         | Текстовое описание для генерации                   |
| type        | ENUM           | NOT NULL         | Тип запрошенной генерации                          |
| status      | ENUM           | NOT NULL         | Текущий статус генерации                           |
| rating      | Integer        | NULL             | Оценка генерации от 1 до 5                         |
| createdAt   | LocalDateTime  | NOT NULL         | Дата создания запроса                              |
| completedAt | LocalDateTime  | NULL             | Дата завершения генерации                          |

`rating` равен `null`, пока пользователь не поставил оценку.

`completedAt` заполняется только при переходе в финальный статус `COMPLETED` или `FAILED`.

### generation_type

Это enum в приложении.

| Значение |
|----------|
| IMAGE    |
| VIDEO    |
| AUDIO    |

### generation_status

Это enum в приложении.

| Значение   | Описание                                      |
|------------|-----------------------------------------------|
| QUEUED     | Задача поставлена в RabbitMQ                  |
| PROCESSING | Worker забрал задачу и выполняет генерацию    |
| COMPLETED  | Генерация завершена, файл загружен в MinIO    |
| FAILED     | Генерация завершилась ошибкой                 |

## generated_assets

Таблица результата генерации.

| Поле        | Тип            | Ограничения           | Описание                                |
|-------------|----------------|------------------------|-----------------------------------------|
| id          | UUID           | PK                     | Идентификатор файла результата          |
| requestId   | UUID           | FK, NOT NULL, UNIQUE   | Запрос генерации                        |
| objectKey   | String         | NOT NULL, UNIQUE       | Ключ объекта в S3                       |
| assetType   | ENUM           | NOT NULL               | Тип файла результата                    |
| contentType | String         | NOT NULL               | MIME-тип файла, например `image/png`    |
| sizeBytes   | Long           | NULL                   | Размер файла в байтах                   |
| duration    | Integer        | NULL                   | Длительность в секундах для аудио/видео |
| width       | Integer        | NULL                   | Ширина для изображения/видео            |
| height      | Integer        | NULL                   | Высота для изображения/видео            |
| createdAt   | LocalDateTime  | NOT NULL               | Дата создания записи                    |

### asset_type

Это enum в приложении.

| Значение |
|----------|
| IMAGE    |
| VIDEO    |
| AUDIO    |

## Индексы и ограничения

### Индексы

| Таблица              | Поле              |
|----------------------|-------------------|
| users                | email             |
| generation_requests  | userId            |
| generation_requests  | userId, createdAt |
| generated_assets     | requestId         |

### Ограничения

```text
users.email UNIQUE
generated_assets.requestId UNIQUE
generated_assets.objectKey UNIQUE
generation_requests.rating BETWEEN 1 AND 5 OR NULL
```

## Удаление данных

При удалении пользователя:

```text
users -> generation_requests -> generated_assets
```

Записи генераций и assets можно удалять каскадно.

## Пример данных

### users

| id                                   | email           | passwordHash |
|--------------------------------------|-----------------|--------------|
| 550e8400-e29b-41d4-a716-446655440000 | user@test.local | `$2a$10...`  |

### generation_requests

| id                                   | userId                               | prompt          | type  | status    | rating | createdAt                 | completedAt               |
|--------------------------------------|--------------------------------------|-----------------|-------|-----------|--------|---------------------------|---------------------------|
| 111e8400-e29b-41d4-a716-446655440000 | 550e8400-e29b-41d4-a716-446655440000 | test_generation | IMAGE | COMPLETED | 5      | 2026-07-12T10:50:00+04:00 | 2026-07-12T10:52:00+04:00 |

### generated_assets

| id                                   | requestId                            | assetType | contentType | objectKey                                             |
|--------------------------------------|--------------------------------------|-----------|-------------|-------------------------------------------------------|
| 222e8400-e29b-41d4-a716-446655440000 | 111e8400-e29b-41d4-a716-446655440000 | IMAGE     | image/png   | images/requests/111e8400-e29b-41d4-a716/result.png   |
