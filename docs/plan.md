**Необходимо полностью переделать бекенд:**
1. Изменить API
2. Добавить брокер сообщений - RabbitMQ
3. Перенести хранение файлов генерации в S3/MinIO
4. Добавить observability

**Что вообще должен делать сайтик?**
1. Позволять регистрироваться - email + password
2. Позволять входить в аккаунт
3. Позволять менять пароль
4. Позволять удалять аккаунт
5. Позволять генерировать картинку, аудио или видео по текстовому описанию
6. Позволять ставить оценку генерации
7. Позволять просматривать историю генераций
8. Позволять переходить из истории генераций к конкретной генерации
9. Позволять скачивать/отображать файл результата генерации

**Сущности в базе данных:**

| User         | GenerationRequest | GeneratedAsset |
|--------------|-------------------|----------------|
| id           | id                | id             |
| email        | userId            | requestId      |
| passwordHash | prompt            | objectKey      |
|              | type              | assetType      |
|              | status            | contentType    |
|              | rating            | sizeBytes      |
|              | createdAt         | duration       |
|              | completedAt       | width          |
|              |                   | height         |
|              |                   | createdAt      |

**Связи:**

```text
User 1 -> N GenerationRequest
GenerationRequest 1 -> 0..1 GeneratedAsset
```

`GeneratedAsset` отсутствует, пока генерация ещё не завершена.

**Основные enum:**

```text
GenerationRequest.type:
- IMAGE
- VIDEO
- AUDIO

GenerationRequest.status:
- QUEUED
- PROCESSING
- COMPLETED
- FAILED

GeneratedAsset.assetType:
- IMAGE
- VIDEO
- AUDIO
```

`GeneratedAsset.contentType` - это MIME-тип файла, например:

```text
image/png
video/mp4
audio/mpeg
```

**Хранение файлов:**

Файлы генерации хранятся в S3/MinIO.

В базе хранится только `GeneratedAsset.objectKey`, например:

```text
images/requests/111e8400-e29b-41d4-a716/result.png
```

Bucket хранится в конфигурации backend, а не в таблице.

**Методы API:**

1. `/users`
   1. `POST /login` - вход
   2. `POST /refresh` - обновление токена
   3. `POST /register` - регистрация
   4. `POST /logout` - выход
   5. `PUT /change-password` - смена пароля

2. `/generations`
   1. `POST /` - создать запрос генерации
   2. `GET /` - получить историю генераций текущего пользователя
   3. `GET /{id}` - получить конкретную генерацию и metadata результата
   4. `PUT /{id}/rating` - оценить генерацию
   5. `GET /{id}/asset` - скачать/отобразить файл результата генерации
