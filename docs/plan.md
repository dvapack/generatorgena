**Необходимо полностью переделать бекенд:**
1. Изменить API
2. Добавить брокер сообщений - RabbitMQ
3. Перенести хранение изображений в S3 хранилище - MiniO
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

**Соответственно нужны будут следующие сущности в базе данных:**

| User     | GeneratedAsset | GenerationRequest |
|----------|----------------|-------------------|
| id       | id             | id                |
| email    | requestId      | userId            |
| password | contentType    | prompt            |
|          | assetType      | status            |
|          | objectKey      | rating            |
|          | duration       | createdAt         |
|          | width          | completedAt       |
|          | height         |                   |
|          | createdAt      |                   |

User 1 -> N GenerationRequest

GenerationRequest 1 -> 1 GeneratedAsset

**Методы API:**
1. /users
   1. /login - вход
   2. /refresh - обновление токена
   3. /register - регистрация
   4. /logout - выход (удаление токена)
   5. /change-password - смена пароля
2. /generations
   1. POST / - отправить generationRequest
   2. GET / - получить все generationRequests
   3. GET /{id} - получить generationRequest + generatedAsset
   4. PUT /{id}/rating - оценить generatedAsset
   5. GET /{id}/asset/ - скачать файл генерации