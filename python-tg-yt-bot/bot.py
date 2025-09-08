import os
import logging
import requests
import re
import asyncio
from datetime import datetime
from telegram import Update
from telegram.ext import Application, MessageHandler, CommandHandler, filters, ContextTypes

# Configure logging
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

# Configuration
TELEGRAM_BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')
BACKEND_URL = os.getenv('BACKEND_URL', 'http://app:8080/api/v1/tasks')
TIMEOUT = 10  # seconds for backend request timeout
POLL_INTERVAL = 5  # seconds between status checks
MAX_POLL_ATTEMPTS = 120  # Maximum number of polling attempts (10 minutes)

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
            # Process each URL
            await process_video_request(update, url, chat_id)
    else:
        await update.message.reply_text("Please send a valid YouTube URL")

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

async def process_video_request(update: Update, url: str, chat_id: int):
    """Process a video request using the new task API"""
    try:
        # Send initial processing message
        processing_message = await update.message.reply_text("🔄 Processing started...")
        
        # Create task in backend
        task_response = await create_task(url, chat_id)
        
        if not task_response or not task_response.get('success'):
            await processing_message.edit_text("❌ Failed to start processing. Please try again.")
            return
        
        task_id = task_response.get('downloadId')
        message = task_response.get('message', '')
        
        # Check if task was completed immediately (reused existing result)
        if 'reusing existing result' in message or 'completed by reusing' in message:
            await processing_message.edit_text("✅ Video ready! (Reused existing download)")
            return
        
        # Update message to show task ID
        await processing_message.edit_text(f"🔄 Processing... Task ID: {task_id}")
        
        # Poll task status until completion
        await poll_task_status(processing_message, task_id, url)
        
    except Exception as e:
        logger.error(f"Error processing video request: {e}")
        try:
            await processing_message.edit_text("❌ An error occurred while processing your request.")
        except:
            pass

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
        response = requests.post(
            BACKEND_URL,
            json=payload,
            timeout=TIMEOUT
        )
        
        if response.status_code == 200:
            return response.json()
        else:
            logger.error(f"Failed to create task: {response.status_code} - {response.text}")
            return None
            
    except requests.exceptions.RequestException as e:
        logger.error(f"Request failed: {e}")
        return None

async def poll_task_status(message, task_id: str, url: str):
    """Poll task status until completion"""
    attempts = 0
    
    while attempts < MAX_POLL_ATTEMPTS:
        try:
            # Get task status
            status_response = requests.get(
                f"{BACKEND_URL}/{task_id}",
                timeout=TIMEOUT
            )
            
            if status_response.status_code == 200:
                task_data = status_response.json()
                status = task_data.get('status')
                
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
                    return
                    
                elif status == 'FAILED':
                    error_msg = task_data.get('errorMessage', 'Unknown error')
                    await message.edit_text(f"❌ Processing failed: {error_msg}")
                    return
                    
                elif status == 'CANCELLED':
                    await message.edit_text("❌ Processing was cancelled")
                    return
                    
                elif status == 'PROCESSING':
                    # Update message with progress indicator
                    dots = "." * ((attempts % 3) + 1)
                    await message.edit_text(f"🔄 Processing{dots} Task ID: {task_id}")
                    
            else:
                logger.error(f"Failed to get task status: {status_response.status_code}")
                
        except requests.exceptions.RequestException as e:
            logger.error(f"Error polling task status: {e}")
        
        # Wait before next poll
        await asyncio.sleep(POLL_INTERVAL)
        attempts += 1
    
    # Timeout reached
    await message.edit_text("⏰ Processing is taking longer than expected. Please check back later.")

async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /start command"""
    await update.message.reply_text(
        "🎥 YouTube Video Downloader Bot\n\n"
        "Send me a YouTube URL and I'll download it for you!\n\n"
        "Commands:\n"
        "/start - Show this message\n"
        "/status - Check bot status\n"
        "/help - Show help information"
    )

async def status_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /status command"""
    try:
        # Check backend status
        response = requests.get(f"{BACKEND_URL}/processing/status", timeout=TIMEOUT)
        
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
        logger.error(f"Error checking status: {e}")
        status_text = "❌ Backend: Unavailable"
    
    await update.message.reply_text(status_text)

async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /help command"""
    help_text = (
        "📖 Help - YouTube Video Downloader Bot\n\n"
        "How to use:\n"
        "1. Send me a YouTube URL (youtube.com or youtu.be)\n"
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