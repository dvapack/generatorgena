Будет две очереди:
1. generationEntity.requests
2. generationEntity.results

Backend пишет в свой exchange(generationEntity.commands) события на генерацию с routing_key generationEntity.generate, которые 
попадают в generationEntity.requests. Мл сервис читает из этой очереди. Далее при получении запроса на генерацию и при начале
генерации он отправляет в свой exchange(generationEntity.events) событие о начале генерации с routing_key generationEntity.processing,
в случае успешной генерации отправляет generationEntity.completed, в случае неудачи - generationEntity.failed. Для всех этих событий
одна очередь - generationEntity.results.