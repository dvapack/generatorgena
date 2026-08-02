Будет две очереди:
1. generationEntity.requests
2. generationEntity.results

Backend пишет в свой exchange(generationEntity.commands) события на генерацию с routing_key generationEntity.generate, которые 
попадают в generationEntity.requests. Мл сервис читает из этой очереди. Далее при получении запроса на генерацию и при начале
генерации он отправляет в свой exchange(generationEntity.events) событие о начале генерации с routing_key generationEntity.processing,
в случае успешной генерации отправляет generationEntity.completed, в случае неудачи - generationEntity.failed. Для всех этих событий
одна очередь - generationEntity.results.

Команда генерации соответствует контракту backend:

```json
{
  "commandId": "0198f23d-fd40-7000-8000-000000000001",
  "generationId": "0198f23d-fd40-7000-8000-000000000002",
  "prompt": "flowers in a vase"
}
```

Успешное событие содержит единый набор метаданных результата:

```json
{
  "asset": {
    "objectKey": "images/requests/0198f23d-fd40-7000-8000-000000000002/result.png",
    "contentType": "image/png",
    "sizeBytes": 248193
  }
}
```
