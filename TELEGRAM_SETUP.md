# Настройка Telegram Bot для TG YouTube Downloader

Это руководство поможет вам настроить Telegram Bot для интеграции с приложением TG YouTube Downloader.

## Шаг 1: Создание Telegram Bot

### 1.1 Найти BotFather
1. Откройте Telegram
2. Найдите пользователя `@BotFather`
3. Нажмите "Start" или отправьте команду `/start`

### 1.2 Создать нового бота
1. Отправьте команду `/newbot`
2. Введите имя для вашего бота (например, "My Video Downloader")
3. Введите username для бота (должен заканчиваться на 'bot', например "my_video_downloader_bot")
4. BotFather выдаст вам токен бота - **сохраните его!**

Пример ответа BotFather:
```
Use this token to access the HTTP API:
1234567890:ABCdefGHIjklMNOpqrsTUVwxyz

Keep your token secure and store it safely, it can be used by anyone to control your bot.
```

## Шаг 2: Получение Chat ID

### 2.1 Начать чат с ботом
1. Найдите вашего бота по username
2. Нажмите "Start" или отправьте команду `/start`
3. Отправьте любое сообщение боту (например, "Hello")

### 2.2 Получить Chat ID
1. Откройте браузер
2. Перейдите по ссылке: `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
3. Замените `<YOUR_BOT_TOKEN>` на ваш токен бота
4. Найдите в ответе поле `"chat":{"id":123456789}` - это ваш Chat ID

Пример ответа:
```json
{
  "ok": true,
  "result": [
    {
      "update_id": 123456789,
      "message": {
        "message_id": 1,
        "from": {
          "id": 123456789,
          "first_name": "Your Name",
          "username": "your_username"
        },
        "chat": {
          "id": 123456789,
          "first_name": "Your Name",
          "username": "your_username",
          "type": "private"
        },
        "date": 1705312345,
        "text": "Hello"
      }
    }
  ]
}
```

## Шаг 3: Настройка приложения

### 3.1 Локальная разработка

**Вариант A: Переменные окружения (Рекомендуется)**
```bash
export TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
export TELEGRAM_CHAT_ID=123456789
```

**Вариант B: Файл application.properties**
```properties
# Telegram Bot Configuration
telegram.bot.token=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
telegram.chat.id=123456789
```

### 3.2 Docker окружение

**Вариант A: Переменные окружения**
```bash
export TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
export TELEGRAM_CHAT_ID=123456789
docker-compose up --build
```

**Вариант B: Файл .env**
```bash
cp env.example .env
# Отредактируйте .env файл с вашими значениями
docker-compose up --build
```

**Вариант C: Передача переменных при запуске**
```bash
docker-compose up -e TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz -e TELEGRAM_CHAT_ID=123456789
```

## Шаг 4: Проверка настройки

### 4.1 Проверить здоровье API
```bash
curl -X GET http://localhost:8080/api/video/telegram/health
```

Ожидаемый ответ:
```json
{
  "healthy": true,
  "timestamp": 1705312345678
}
```

### 4.2 Тестовое скачивание
```bash
curl -X POST http://localhost:8080/api/video/download \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "format": "mp4",
    "resolution": "720p"
  }'
```

## Шаг 5: Безопасность

### 5.1 Защита токена
- **Никогда не коммитьте токен в Git**
- Используйте переменные окружения вместо хардкода в файлах
- Добавьте `.env` в `.gitignore`
- Не используйте реальные токены в примерах кода
- Регулярно ротируйте токены бота

### 5.2 Ограничения Telegram
- Максимальный размер файла: 50 MB
- Для файлов больше 20 MB используйте `sendDocument`
- Лимит на количество запросов к API

## Шаг 6: Устранение неполадок

### 6.1 Ошибка "Unauthorized"
- Проверьте правильность токена бота
- Убедитесь, что бот не был удален

### 6.2 Ошибка "Chat not found"
- Убедитесь, что вы начали чат с ботом
- Проверьте правильность Chat ID

### 6.3 Ошибка "File too large"
- Telegram имеет лимит 50 MB на файл
- Используйте более низкое качество видео

### 6.4 Ошибка "Network timeout"
- Проверьте интернет соединение
- Убедитесь, что Telegram API доступен

## Примеры использования

### Проверка информации о файле
```bash
curl -X GET http://localhost:8080/api/video/telegram/file/BAACAgIAAxkBAAIBQ2WX_1234567890
```

### Получение списка загруженных видео
```sql
SELECT * FROM video WHERE file_id IS NOT NULL;
```

## Дополнительные возможности

### Настройка webhook (опционально)
Для получения уведомлений о новых сообщениях:

```bash
curl -X POST https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook \
  -H "Content-Type: application/json" \
  -d '{"url": "https://your-domain.com/api/telegram/webhook"}'
```

### Команды бота
Добавьте команды для вашего бота через BotFather:
- `/setcommands` - установить список команд
- `/download <url>` - скачать видео
- `/status` - проверить статус загрузки
