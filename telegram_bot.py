import os
import logging
import requests
from typing import Optional

logger = logging.getLogger(__name__)

class TelegramBot:
    def __init__(self):
        self.bot_token = os.getenv('TELEGRAM_BOT_TOKEN')
        self.chat_id = os.getenv('TELEGRAM_CHAT_ID')
        self.base_url = f"https://api.telegram.org/bot{self.bot_token}" if self.bot_token else None
    
    def is_configured(self) -> bool:
        """Check if Telegram bot is properly configured"""
        return bool(self.bot_token and self.chat_id)
    
    def send_message(self, message: str, chat_id: Optional[str] = None) -> bool:
        """Send a message via Telegram bot"""
        if not self.is_configured():
            logger.warning("Telegram bot not configured")
            return False
        
        target_chat_id = chat_id or self.chat_id
        
        try:
            url = f"{self.base_url}/sendMessage"
            payload = {
                'chat_id': target_chat_id,
                'text': message,
                'parse_mode': 'HTML'
            }
            
            response = requests.post(url, json=payload, timeout=10)
            
            if response.status_code == 200:
                logger.info(f"Telegram message sent successfully to {target_chat_id}")
                return True
            else:
                logger.error(f"Telegram API error: {response.status_code} - {response.text}")
                return False
                
        except requests.RequestException as e:
            logger.error(f"Error sending Telegram message: {e}")
            return False
    
    def send_notification(self, message: str) -> bool:
        """Send a notification with formatting"""
        formatted_message = f"🔔 <b>MySharingApp Notification</b>\n\n{message}\n\n⏰ {self._get_timestamp()}"
        return self.send_message(formatted_message)
    
    def send_device_alert(self, device_id: str, alert_type: str, details: str = "") -> bool:
        """Send device-specific alert"""
        emoji_map = {
            'connected': '🟢',
            'disconnected': '🔴',
            'sharing_started': '▶️',
            'sharing_stopped': '⏸️',
            'error': '⚠️',
            'file_uploaded': '📁'
        }
        
        emoji = emoji_map.get(alert_type, '📱')
        
        message = f"{emoji} <b>Device Alert</b>\n\n"
        message += f"Device: <code>{device_id}</code>\n"
        message += f"Type: {alert_type.replace('_', ' ').title()}\n"
        
        if details:
            message += f"Details: {details}\n"
        
        message += f"\n⏰ {self._get_timestamp()}"
        
        return self.send_message(message)
    
    def send_file_notification(self, device_id: str, file_name: str, file_type: str, file_size: str) -> bool:
        """Send file upload notification"""
        message = f"📁 <b>File Uploaded</b>\n\n"
        message += f"Device: <code>{device_id}</code>\n"
        message += f"File: <code>{file_name}</code>\n"
        message += f"Type: {file_type}\n"
        message += f"Size: {file_size}\n"
        message += f"\n⏰ {self._get_timestamp()}"
        
        return self.send_message(message)
    
    def send_location_update(self, device_id: str, latitude: float, longitude: float, accuracy: float) -> bool:
        """Send location update notification"""
        message = f"📍 <b>Location Update</b>\n\n"
        message += f"Device: <code>{device_id}</code>\n"
        message += f"Coordinates: {latitude:.6f}, {longitude:.6f}\n"
        message += f"Accuracy: {accuracy:.1f}m\n"
        message += f"\n⏰ {self._get_timestamp()}"
        
        return self.send_message(message)
    
    def test_connection(self) -> bool:
        """Test bot connection"""
        if not self.is_configured():
            return False
        
        try:
            url = f"{self.base_url}/getMe"
            response = requests.get(url, timeout=10)
            
            if response.status_code == 200:
                bot_info = response.json()
                logger.info(f"Telegram bot connected: {bot_info.get('result', {}).get('username', 'Unknown')}")
                return True
            else:
                logger.error(f"Telegram bot test failed: {response.status_code}")
                return False
                
        except requests.RequestException as e:
            logger.error(f"Telegram bot test error: {e}")
            return False
    
    def _get_timestamp(self) -> str:
        """Get formatted timestamp"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    def get_bot_info(self) -> dict:
        """Get bot information"""
        if not self.is_configured():
            return {}
        
        try:
            url = f"{self.base_url}/getMe"
            response = requests.get(url, timeout=10)
            
            if response.status_code == 200:
                return response.json().get('result', {})
            else:
                return {}
                
        except requests.RequestException:
            return {}
