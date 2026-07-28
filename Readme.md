# Учебный проект по предмету "Технологии сетевого программирования"

![интересная картинка](screenshots/интересная_картинка.png)

**Тема проекта:** *Гена* - генератор изображений по текстовому описанию.

---
## **Screenshots**

### **Страница регистрации**
![страница регистрации](screenshots/registration_page.png)
### **Страница логина**
![страница логина](screenshots/login_page.png)
### **Главная страница**
![главная страница](screenshots/main_page.png)
### **Главная страница с результатом генерации**
![главная страница с результатом генерации](screenshots/main_page_with_generated_image.png)

---


## **Стек:**

1. Spring Boot, FastAPI, PyTorch
2. PostgreSQL, RabbitMQ, MinIO
3. React
4. Docker Compose

## **Запуск**

В проекте используется единый Compose-файл в корневой директории:

```shell
docker compose up --build --detach
```

После запуска frontend доступен на `http://localhost:3000`, backend — на
`http://localhost:8080`, ML health API — на
`http://localhost:5001/health/ready`.

Переменные окружения, тестовый профиль и команды остановки описаны в
[документации Docker](docs/docker.md).

