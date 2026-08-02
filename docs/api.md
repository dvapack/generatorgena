# /users

По этому пути будут все необходимые для работы с пользователем методы.

# /generationEntities

По этому пути будут все необходимые для работы с генерациями методы.

## Общий формат ошибки

Все JSON-ошибки возвращаются в едином формате:

```json
{
  "status": 400,
  "message": "Описание ошибки",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

Для запросов к чужим генерациям backend возвращает `404 Not Found`.

## POST /

POST метод для отправки запроса на генерацию.

### request

```json
{
  "prompt": "test_generation"
}
```

### response

#### 201 Created

Запись генерации создана, задача поставлена в очередь.

```json
{
  "id": "acde070d-8c4c-4f0d-9d8a-162843c10333",
  "status": "QUEUED"
}
```

#### 400 Bad Request

##### Пустой/null prompt

###### request

```json
{
  "prompt": ""
}
```

###### response

```json
{
  "status": 400,
  "message": "Ошибка валидации",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": [
    "prompt: Промпт не может быть пустым"
  ]
}
```

#### 401 Unauthorized

###### response

```json
{
  "status": 401,
  "message": "Для генерации необходимо сначала авторизоваться",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 429 Too Many Requests

Пользователь слишком часто отправляет запросы на генерацию.

```json
{
  "status": 429,
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 503 Service Unavailable

Backend не смог поставить задачу в RabbitMQ или сервис генерации временно недоступен.

```json
{
  "status": 503,
  "message": "Сервис генерации временно недоступен",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

## GET /

GET метод для получения всех генераций пользователя.

### request query params

```text
page=0
size=20
```

### response

#### 200 OK

```json
{
  "generationEntities": [
    {
      "id": "acde070d-8c4c-4f0d-9d8a-162843c10333",
      "prompt": "test_generation",
      "status": "COMPLETED",
      "rating": 5,
      "createdAt": "2026-07-12T10:50:00+04:00",
      "completedAt": "2026-07-12T10:52:00+04:00",
      "asset": {
        "id": "acde070d-8c4c-4f0d-9d8a-162843c10333",
        "contentType": "image/png",
        "sizeBytes": 248193
      }
    },
    {
      "id": "acde070d-8c4c-4f0d-9d8a-162843c10333",
      "prompt": "test_generation",
      "status": "QUEUED",
      "rating": null,
      "createdAt": "2026-07-12T10:55:00+04:00",
      "completedAt": null,
      "asset": null
    }
  ],
  "page": 0,
  "size": 20,
  "total": 2
}
```

Если генераций нет, возвращается пустой массив:

```json
{
  "generationEntities": [],
  "page": 0,
  "size": 20,
  "total": 0
}
```

#### 400 Bad Request

Некорректные query-параметры пагинации.

```json
{
  "status": 400,
  "message": "Некорректные параметры пагинации",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 401 Unauthorized

###### response

```json
{
  "status": 401,
  "message": "Для получения генераций необходимо сначала авторизоваться",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 429 Too Many Requests

```json
{
  "status": 429,
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/generationEntities",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

## GET /{id}

GET метод для получения конкретной генерации пользователя.

### response

#### 200 OK

```json
{
  "id": "acde070d-8c4c-4f0d-9d8a-162843c10333",
  "prompt": "test_generation",
  "status": "COMPLETED",
  "rating": 5,
  "createdAt": "2026-07-12T10:50:00+04:00",
  "completedAt": "2026-07-12T10:52:00+04:00",
  "asset": {
    "id": "acde070d-8c4c-4f0d-9d8a-162843c10333",
    "contentType": "image/png",
    "sizeBytes": 248193
  }
}
```

Если генерация ещё не завершена, `asset` равен `null`:

```json
{
  "generationEntity": {
    "id": "acde070d-8c4c-4f0d-9d8a-162843c10333",
    "prompt": "test_generation",
    "status": "PROCESSING",
    "rating": null,
    "createdAt": "2026-07-12T10:55:00+04:00",
    "completedAt": null,
    "asset": null
  }
}
```

#### 401 Unauthorized

###### response

```json
{
  "status": 401,
  "message": "Для получения генерации необходимо сначала авторизоваться",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 404 Not Found

Генерация не найдена или принадлежит другому пользователю.

```json
{
  "status": 404,
  "message": "Генерация с данным id не найдена",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 429 Too Many Requests

```json
{
  "status": 429,
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

## PUT /{id}/rating

PUT метод для изменения оценки конкретной генерации.

### request

```json
{
  "rating": 4
}
```

### response

#### 204 No content

Rating успешно обновился

#### 400 Bad Request

##### Rating не является числом

###### request

```json
{
  "rating": "bla bla"
}
```

###### response

```json
{
  "status": 400,
  "message": "Некорректное тело запроса",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/rating",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

##### Rating вне допустимого диапазона

###### request

```json
{
  "rating": 6
}
```

###### response

```json
{
  "status": 400,
  "message": "Ошибка валидации",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/rating",
  "timestamp": "2026-07-23T12:00:00",
  "errors": [
    "rating: Rating должен быть от 1 до 5"
  ]
}
```

#### 401 Unauthorized

###### response

```json
{
  "status": 401,
  "message": "Для изменения оценки генерации необходимо сначала авторизоваться",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/rating",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 404 Not Found

Генерация не найдена или принадлежит другому пользователю.

```json
{
  "status": 404,
  "message": "Генерация с данным id не найдена",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/rating",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 409 Conflict

Нельзя оценить генерацию, пока она не завершена.

```json
{
  "status": 409,
  "message": "Нельзя оценить незавершённую генерацию",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/rating",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 429 Too Many Requests

```json
{
  "status": 429,
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/rating",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

## DELETE /{id}

DELETE метод для удаления конкретной генерации пользователя.

Удаляется запись генерации и связанные с ней метаданные результата. Файл результата удаляется из S3/MinIO.

### response

#### 204 No Content

Генерация успешно удалена. Тело ответа отсутствует.

#### 400 Bad Request

Некорректный UUID в path-параметре.

```json
{
  "status": 400,
  "message": "Некорректное значение параметра: id",
  "path": "/generationEntities/not-a-uuid",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 401 Unauthorized

```json
{
  "status": 401,
  "message": "Для удаления генерации необходимо сначала авторизоваться",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 404 Not Found

Генерация не найдена или принадлежит другому пользователю.

```json
{
  "status": 404,
  "message": "Генерация с данным id не найдена",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 429 Too Many Requests

```json
{
  "status": 429,
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 503 Service Unavailable

S3/MinIO временно недоступен, поэтому файл результата не удалось удалить.

```json
{
  "status": 503,
  "message": "Файловое хранилище временно недоступно",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

## GET /{id}/asset

GET метод для скачивания файла конкретной генерации.

### response

#### 200 OK

Возвращает файл из S3/MinIO.

Пример заголовков ответа:

```http
Content-Type: image/png
Content-Length: 248193
Content-Disposition: inline; filename="generationEntity-1.png"
```

#### 400 Bad Request

Некорректный id в path-параметре.

```json
{
  "status": 400,
  "message": "Некорректное значение параметра: id",
  "path": "/generationEntities/not-a-uuid/asset",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 401 Unauthorized

###### response

```json
{
  "status": 401,
  "message": "Для получения файла генерации необходимо сначала авторизоваться",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/asset",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 404 Not Found

Генерация не найдена, принадлежит другому пользователю или файл результата отсутствует.

```json
{
  "status": 404,
  "message": "Файл генерации не найден",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/asset",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 409 Conflict

Генерация ещё не завершена.

```json
{
  "status": 409,
  "message": "Генерация ещё не завершена",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/asset",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 416 Range Not Satisfiable

Запрошен некорректный диапазон байтов.

```json
{
  "status": 416,
  "message": "Некорректный диапазон файла",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/asset",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 429 Too Many Requests

```json
{
  "status": 429,
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/asset",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```

#### 503 Service Unavailable

S3/MinIO временно недоступен.

```json
{
  "status": 503,
  "message": "Файловое хранилище временно недоступно",
  "path": "/generationEntities/acde070d-8c4c-4f0d-9d8a-162843c10333/asset",
  "timestamp": "2026-07-23T12:00:00",
  "errors": []
}
```
