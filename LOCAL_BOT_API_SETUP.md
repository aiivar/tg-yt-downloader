# Настройка локального Telegram Bot API сервера

Это руководство поможет настроить локальный Bot API сервер для загрузки файлов размером до 2GB.

## Преимущества локального Bot API сервера

- **Поддержка файлов до 2GB** (вместо 50MB в официальном API)
- **Более высокая скорость загрузки**
- **Отсутствие лимитов на количество запросов**
- **Локальный контроль над данными**

## Шаг 1: Получение API credentials

1. Перейдите на https://my.telegram.org/apps
2. Войдите с помощью номера телефона
3. Создайте новое приложение или используйте существующее
4. Сохраните:
   - **api_id** (число)
   - **api_hash** (строка)

## Шаг 2: Установка и настройка сервера

### Вариант A: Docker (Рекомендуется)

```bash
# Создайте docker-compose файл для Bot API сервера
cat > docker-compose.bot-api.yml << EOF
version: '3.8'
services:
  telegram-bot-api:
    image: aiogram/telegram-bot-api:latest
    container_name: telegram-bot-api
    ports:
      - "8081:8081"
    environment:
      - TELEGRAM_API_ID=your_api_id
      - TELEGRAM_API_HASH=your_api_hash
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
    volumes:
      - bot_api_data:/var/lib/telegram-bot-api
    restart: unless-stopped

volumes:
  bot_api_data:
EOF

# Запустите сервер
docker-compose -f docker-compose.bot-api.yml up -d
```

### Вариант B: Сборка из исходников

```bash
# Клонируйте репозиторий
git clone https://github.com/tdlib/telegram-bot-api.git
cd telegram-bot-api

# Соберите сервер
mkdir build && cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make -j$(nproc)

# Запустите сервер
./telegram-bot-api --api-id=your_api_id --api-hash=your_api_hash --local
```

## Шаг 3: Настройка приложения

### Вариант A: Автоматический запуск с Docker Compose (Рекомендуется)

Приложение теперь автоматически запускает Bot API сервер вместе с основным приложением:

```bash
# Development environment
docker-compose -f docker-compose.dev.yml up --build

# Production environment  
docker-compose -f docker-compose.prod.yml up --build

# Standard environment
docker-compose up --build
```

### Вариант B: Ручная настройка переменных окружения

```bash
# Включите локальный API
export TELEGRAM_API_USE_LOCAL=true
export TELEGRAM_API_LOCAL_URL=http://telegram-bot-api:8081
export TELEGRAM_API_LOCAL_CREDENTIALS_PATH=/path/to/credentials.json

# Или в .env файле:
TELEGRAM_API_USE_LOCAL=true
TELEGRAM_API_LOCAL_URL=http://telegram-bot-api:8081
TELEGRAM_API_LOCAL_CREDENTIALS_PATH=/path/to/credentials.json
```

## Шаг 4: Проверка настройки

```bash
# Проверьте конфигурацию
curl -X GET http://localhost:8080/api/video/telegram/config

# Проверьте здоровье API
curl -X GET http://localhost:8080/api/video/telegram/health
```

## Шаг 5: Тестирование больших файлов

```bash
# Скачайте большой файл
curl -X POST http://localhost:8080/api/video/download \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.youtube.com/watch?v=large_video",
    "format": "mp4",
    "resolution": "720p"
  }'
```

## Мониторинг и логи

```bash
# Просмотр логов Bot API сервера
docker logs telegram-bot-api-dev  # для development
docker logs telegram-bot-api      # для production
docker logs telegram-bot-api-prod # для production

# Проверка статуса сервиса
docker ps | grep telegram-bot-api

# Проверка здоровья всех сервисов
docker-compose ps

# Просмотр логов всех сервисов
docker-compose logs -f
```

## Устранение неполадок

### Ошибка "Local Bot API server is not configured"
- Убедитесь, что `TELEGRAM_API_USE_LOCAL=true`
- Проверьте доступность сервера на порту 8081

### Ошибка "API credentials not found"
- Проверьте правильность api_id и api_hash
- Убедитесь, что credentials переданы в сервер

### Ошибка "Connection refused"
- Проверьте, что Bot API сервер запущен
- Убедитесь, что порт 8081 открыт

## Безопасность

- **Храните api_id и api_hash в секрете**
- **Используйте HTTPS в продакшене**
- **Ограничьте доступ к Bot API серверу**
- **Регулярно обновляйте сервер**

## Производительность

- **Рекомендуется 4+ CPU cores**
- **Минимум 8GB RAM**
- **SSD для хранения данных**
- **Стабильное интернет-соединение**
