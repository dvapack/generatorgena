Будет две очереди:
1. generationEntity.requests
2. generationEntity.results

Backend сначала сохраняет команду в таблицу `outbox_messages` в той же транзакции,
что и `generation_requests`. Фоновый отправитель публикует неотправленные команды в
exchange `generation.commands` с routing key `generation.generate`. После подтверждения
RabbitMQ запись outbox отмечается отправленной. При ошибке команда остаётся в PostgreSQL
и отправляется повторно с увеличивающейся задержкой.

Команды попадают в `generation.requests`, откуда их читает ML-сервис. Далее при получении запроса на генерацию и при начале
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
