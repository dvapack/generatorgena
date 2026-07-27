Будет две очереди:
1. generation.requests
2. generation.results

Backend пишет в свой exchange(generation.commands) события на генерацию с routing_key generation.generate, которые 
попадают в generation.requests. Мл сервис читает из этой очереди. Далее при получении запроса на генерацию и при начале
генерации он отправляет в свой exchange(generation.events) событие о начале генерации с routing_key generation.processing,
в случае успешной генерации отправляет generation.completed, в случае неудачи - generation.failed. Для всех этих событий
одна очередь - generation.results.