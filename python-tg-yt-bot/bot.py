import os
import logging
import requests
import re
import asyncio
from telegram import Update
from telegram.ext import Application, MessageHandler, CommandHandler, filters, ContextTypes

# Configure logging
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO,
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('bot.log')
    ]
)
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Configuration
TELEGRAM_BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')
BACKEND_URL = os.getenv('BACKEND_URL', 'http://app:8080/api/v1/tasks')
TIMEOUT = 10  # seconds for backend request timeout
POLL_INTERVAL = 5  # seconds between status checks
MAX_POLL_ATTEMPTS = 12  # Maximum number of polling attempts (10 minutes)

YOUTUBE_VIDEO_URL_PATTERN = re.compile(
    r'^(https?://)?(www\.)?(youtube\.com|youtu\.be)/.+', re.IGNORECASE
)

def is_youtube_video_url(text: str) -> bool:
    """Return True if the text is a valid YouTube video URL."""
    # Accept only direct YouTube video URLs (not playlists, not channels, etc.)
    # Accepts: https://www.youtube.com/watch?v=XXXXXXXXXXX or https://youtu.be/XXXXXXXXXXX
    # Does not accept: playlist, channel, user, etc.
    # 11-char video id
    text = text.strip()
    
    # Check for double URL concatenation and fix it
    if 'https://youtu.be/https://' in text:
        logger.warning(f"Detected double URL concatenation: {text}")
        return False
    
    patterns = [
        r'(https?://)?(www\.)?youtube\.com/watch\?v=([A-Za-z0-9_-]{11})(&.*)?',
        r'(https?://)?youtu\.be/([A-Za-z0-9_-]{11})(\?.*)?'
    ]
    for pattern in patterns:
        if re.fullmatch(pattern, text):
            return True
    return False

async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle incoming messages and validate only user sent video url"""
    if not update.message or not update.message.text:
        return

    message_text = update.message.text.strip()
    chat_id = update.message.chat_id
    
    logger.info(f"Received message from chat {chat_id}: {message_text}")

    if is_youtube_video_url(message_text):
        logger.info(f"Valid YouTube URL detected: {message_text}")
        await process_video_request(update, message_text, chat_id)
    else:
        logger.info(f"Invalid URL format: {message_text}")
        await update.message.reply_text("Please send a valid YouTube video URL (e.g. https://www.youtube.com/watch?v=XXXXXXXXXXX)")

async def process_video_request(update: Update, url: str, chat_id: int):
    """Process a video request using the new task API"""
    processing_message = None
    try:
        logger.info(f"Processing video request for URL: {url}, Chat ID: {chat_id}")
        
        # Send initial processing message
        processing_message = await update.message.reply_text("🔄 Processing started...")

        # Create task in backend
        task_response = await create_task(url, chat_id)
        logger.info(f"Task creation response: {task_response}")

        if not task_response or not task_response.get('success'):
            error_msg = task_response.get('error', 'Unknown error') if task_response else 'No response from backend'
            logger.error(f"Failed to create task: {error_msg}")
            await processing_message.edit_text(f"❌ Failed to start processing: {error_msg}")
            return

        task_id = task_response.get('downloadId')
        message = task_response.get('message', '')
        logger.info(f"Task created with ID: {task_id}, Message: {message}")

        # Check if task was completed immediately (reused existing result)
        if 'reusing existing result' in message or 'completed by reusing' in message:
            await processing_message.edit_text("✅ Video ready! (Reused existing download)")
            logger.info(f"Task {task_id} completed immediately by reusing existing result")
            return

        # Update message to show task ID
        await processing_message.edit_text(f"🔄 Processing... Task ID: {task_id}")

        # Poll task status until completion (run in background)
        asyncio.create_task(poll_task_status(processing_message, task_id, url))

    except Exception as e:
        logger.error(f"Error processing video request: {e}", exc_info=True)
        try:
            if processing_message:
                await processing_message.edit_text("❌ An error occurred while processing your request.")
        except Exception as edit_error:
            logger.error(f"Error editing message: {edit_error}")

async def create_task(url: str, chat_id: int) -> dict:
    """Create a new task in the backend"""
    payload = {
        'url': url,
        'chatId': str(chat_id),
        'format': 'mp4',
        'quality': '720p',
        'resolution': '720p'
    }

    try:
        logger.info(f"Creating task with payload: {payload}")
        response = requests.post(
            BACKEND_URL,
            json=payload,
            timeout=TIMEOUT
        )
        logger.info(f"Backend response: {response.status_code} - {response.text}")

        if response.status_code == 200:
            return response.json()
        else:
            logger.error(f"Failed to create task: {response.status_code} - {response.text}")
            return None

    except requests.exceptions.RequestException as e:
        logger.error(f"Request failed: {e}", exc_info=True)
        return None

async def poll_task_status(message, task_id: str, url: str):
    """Poll task status until completion (runs in background)"""
    attempts = 0
    logger.info(f"Starting background polling for task {task_id}")

    while attempts < MAX_POLL_ATTEMPTS:
        try:
            # Get task status
            status_response = requests.get(
                f"{BACKEND_URL}/{task_id}",
                timeout=TIMEOUT
            )
            logger.debug(f"Task {task_id} status check: {status_response.status_code}")

            if status_response.status_code == 200:
                task_data = status_response.json()
                status = task_data.get('status')
                logger.info(f"Task {task_id} status: {status}")

                if status == 'COMPLETED':
                    # Get task results
                    results_response = requests.get(
                        f"{BACKEND_URL}/{task_id}/results",
                        timeout=TIMEOUT
                    )

                    if results_response.status_code == 200:
                        results = results_response.json()
                        if results:
                            primary_result = next((r for r in results if r.get('isPrimaryResult')), results[0])
                            file_name = primary_result.get('fileName', 'video')
                            file_size = primary_result.get('fileSizeBytes', 0)

                            # Format file size
                            if file_size > 0:
                                size_mb = file_size / (1024 * 1024)
                                size_text = f" ({size_mb:.1f} MB)"
                            else:
                                size_text = ""

                            await message.edit_text(f"✅ Video ready!{size_text}\n📁 {file_name}")
                        else:
                            await message.edit_text("✅ Video ready!")
                    else:
                        await message.edit_text("✅ Video ready!")
                    logger.info(f"Task {task_id} completed successfully")
                    return

                elif status == 'FAILED':
                    error_msg = task_data.get('errorMessage', 'Unknown error')
                    await message.edit_text(f"❌ Processing failed: {error_msg}")
                    logger.error(f"Task {task_id} failed: {error_msg}")
                    return

                elif status == 'CANCELLED':
                    await message.edit_text("❌ Processing was cancelled")
                    logger.info(f"Task {task_id} was cancelled")
                    return

                elif status == 'PROCESSING':
                    # Update message with progress indicator
                    dots = "." * ((attempts % 3) + 1)
                    await message.edit_text(f"🔄 Processing{dots} Task ID: {task_id}")

            else:
                logger.error(f"Failed to get task status: {status_response.status_code}")

        except requests.exceptions.RequestException as e:
            logger.error(f"Error polling task status: {e}")
        except Exception as e:
            logger.error(f"Unexpected error in polling: {e}")

        # Wait before next poll
        await asyncio.sleep(POLL_INTERVAL)
        attempts += 1

    # Timeout reached
    logger.warning(f"Task {task_id} polling timed out after {MAX_POLL_ATTEMPTS} attempts")
    await message.edit_text("⏰ Processing is taking longer than expected. Please check back later.")

async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /start command"""
    logger.info(f"Start command received from chat {update.message.chat_id}")
    await update.message.reply_text(
        "🎥 YouTube Video Downloader Bot\n\n"
        "Send me a YouTube video URL and I'll download it for you!\n\n"
        "Commands:\n"
        "/start - Show this message\n"
        "/status - Check bot status\n"
        "/help - Show help information"
    )

async def status_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /status command"""
    logger.info(f"Status command received from chat {update.message.chat_id}")
    try:
        # Check backend status
        response = requests.get(f"{BACKEND_URL}/processing/status", timeout=TIMEOUT)
        logger.info(f"Status check response: {response.status_code}")

        if response.status_code == 200:
            status_data = response.json()
            available_slots = status_data.get('availableSlots', 0)
            currently_processing = status_data.get('currentlyProcessing', 0)
            memory_pressure = status_data.get('memoryPressure', 'UNKNOWN')

            status_text = (
                f"🤖 Bot Status: Online\n"
                f"🔄 Processing slots: {currently_processing}/{currently_processing + available_slots}\n"
                f"💾 Memory pressure: {memory_pressure}\n"
                f"✅ Backend: Connected"
            )
        else:
            status_text = "❌ Backend: Connection failed"

    except Exception as e:
        logger.error(f"Error checking status: {e}", exc_info=True)
        status_text = "❌ Backend: Unavailable"

    await update.message.reply_text(status_text)

async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /help command"""
    logger.info(f"Help command received from chat {update.message.chat_id}")
    help_text = (
        "📖 Help - YouTube Video Downloader Bot\n\n"
        "How to use:\n"
        "1. Send me a YouTube video URL (e.g. https://www.youtube.com/watch?v=XXXXXXXXXXX)\n"
        "2. I'll start processing your video\n"
        "3. Wait for the video to be ready\n"
        "4. The video will be sent to you when complete\n\n"
        "Features:\n"
        "• Automatic video download and upload\n"
        "• Reuse of previously downloaded videos\n"
        "• Real-time processing status\n"
        "• Support for various video qualities\n\n"
        "Commands:\n"
        "/start - Show welcome message\n"
        "/status - Check bot and backend status\n"
        "/help - Show this help message"
    )
    await update.message.reply_text(help_text)

def main():
    """Start the bot"""
    if not TELEGRAM_BOT_TOKEN:
        logger.error("TELEGRAM_BOT_TOKEN environment variable is required")
        return

    logger.info(f"Starting bot with backend URL: {BACKEND_URL}")
    
    # Create Application
    application = Application.builder().token(TELEGRAM_BOT_TOKEN).build()

    # Add handlers
    application.add_handler(CommandHandler("start", start_command))
    application.add_handler(CommandHandler("status", status_command))
    application.add_handler(CommandHandler("help", help_command))
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))

    # Start the Bot
    logger.info("Bot is running...")
    application.run_polling()

if __name__ == '__main__':
    main()