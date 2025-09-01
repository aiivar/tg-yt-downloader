import os
import logging
import requests
import re
from datetime import datetime
from telegram import Update
from telegram.ext import Application, MessageHandler, filters, ContextTypes

# Configure logging
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

# Configuration
TELEGRAM_BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')
BACKEND_URL = os.getenv('BACKEND_URL', 'http://app:8080/api/videos')
TIMEOUT = 3  # seconds for backend request timeout

async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle incoming messages and extract YouTube URLs"""
    if not update.message or not update.message.text:
        return

    message_text = update.message.text
    chat_id = update.message.chat_id

    # Extract YouTube URLs
    youtube_urls = extract_youtube_urls(message_text)

    if youtube_urls:
        for url in youtube_urls:
            # Send data to backend without waiting for response
            send_to_backend_async(url, chat_id)

        # Optional: Send quick acknowledgment to user
        await update.message.reply_text("✅ Video link received! Processing...")

def extract_youtube_urls(text: str) -> list:
    """Extract YouTube URLs from text"""
    patterns = [
        r'(https?://)?(www\.)?(youtube|youtu|youtube-nocookie)\.(com|be)/(watch\?v=|embed/|v/|.+\?v=)?([^&=%\?]{11})',
        r'youtu\.be/([^&=%\?]{11})'
    ]

    urls = []
    for pattern in patterns:
        matches = re.finditer(pattern, text, re.IGNORECASE)
        for match in matches:
            if 'youtu.be' in match.group():
                url = f"https://youtu.be/{match.group(1)}"
            else:
                url = match.group(0)
                if not url.startswith('http'):
                    url = 'https://' + url
            urls.append(url)

    return list(set(urls))  # Remove duplicates

def send_to_backend_async(url: str, chat_id: int):
    """Send data to backend without blocking"""
    import threading

    def send_request():
        payload = {
            'url': url,
            'chatId': chat_id,
            'timestamp': datetime.now().isoformat()
        }

        try:
            # Use a short timeout to avoid blocking
            response = requests.post(
                BACKEND_URL,
                json=payload,
                timeout=TIMEOUT
            )
            logger.info(f"Backend response: {response.status_code}")

        except requests.exceptions.Timeout:
            logger.warning("Backend request timed out (expected)")
        except requests.exceptions.RequestException as e:
            logger.error(f"Backend request failed: {e}")

    # Start the request in a separate thread
    thread = threading.Thread(target=send_request)
    thread.daemon = True  # Thread won't block program exit
    thread.start()

def main():
    """Start the bot"""
    if not TELEGRAM_BOT_TOKEN:
        logger.error("TELEGRAM_BOT_TOKEN environment variable is required")
        return

    # Create Application
    application = Application.builder().token(TELEGRAM_BOT_TOKEN).build()

    # Add message handler
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))

    # Start the Bot
    logger.info("Bot is running...")
    application.run_polling()

if __name__ == '__main__':
    main()