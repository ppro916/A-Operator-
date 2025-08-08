import os
import json
import logging
from datetime import datetime, timedelta
from typing import List, Dict, Optional

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
    FIREBASE_AVAILABLE = True
except ImportError:
    FIREBASE_AVAILABLE = False
    logging.warning("Firebase Admin SDK not available. Install with: pip install firebase-admin")

logger = logging.getLogger(__name__)

class FirebaseManager:
    def __init__(self):
        self.db = None
        self.app = None
        self._initialized = False
        
        if FIREBASE_AVAILABLE:
            self._initialize_firebase()
    
    def _initialize_firebase(self):
        """Initialize Firebase connection"""
        try:
            # Try to get credentials from environment variable
            creds_json = os.getenv('FIREBASE_CREDENTIALS')
            
            if creds_json:
                # Parse JSON string from environment
                creds_dict = json.loads(creds_json)
                cred = credentials.Certificate(creds_dict)
            else:
                # Try to load from file
                creds_path = os.getenv('FIREBASE_CREDENTIALS_PATH', 'credentials.json')
                if os.path.exists(creds_path):
                    cred = credentials.Certificate(creds_path)
                else:
                    logger.error("Firebase credentials not found")
                    return
            
            # Initialize Firebase app
            if not firebase_admin._apps:
                self.app = firebase_admin.initialize_app(cred)
            else:
                self.app = firebase_admin.get_app()
            
            # Initialize Firestore
            self.db = firestore.client()
            self._initialized = True
            logger.info("Firebase initialized successfully")
            
        except Exception as e:
            logger.error(f"Firebase initialization error: {e}")
            self._initialized = False
    
    def is_configured(self) -> bool:
        """Check if Firebase is properly configured"""
        return FIREBASE_AVAILABLE and self._initialized and self.db is not None
    
    def is_connected(self) -> bool:
        """Test Firebase connection"""
        if not self.is_configured():
            return False
        
        try:
            # Try a simple read operation
            self.db.collection('test').limit(1).get()
            return True
        except Exception as e:
            logger.error(f"Firebase connection test failed: {e}")
            return False
    
    def get_connected_devices(self) -> List[Dict]:
        """Get list of connected devices"""
        if not self.is_configured():
            return []
        
        try:
            devices_ref = self.db.collection('devices')
            devices = []
            
            for doc in devices_ref.stream():
                device_data = doc.to_dict()
                device_data['id'] = doc.id
                
                # Check if device is online (last seen within 5 minutes)
                last_seen = device_data.get('last_seen')
                if last_seen:
                    if isinstance(last_seen, str):
                        last_seen = datetime.fromisoformat(last_seen.replace('Z', '+00:00'))
                    
                    device_data['online'] = (datetime.now() - last_seen) < timedelta(minutes=5)
                else:
                    device_data['online'] = False
                
                devices.append(device_data)
            
            return devices
            
        except Exception as e:
            logger.error(f"Error getting connected devices: {e}")
            return []
    
    def get_active_devices(self) -> List[Dict]:
        """Get devices that are currently sharing"""
        if not self.is_configured():
            return []
        
        try:
            devices_ref = self.db.collection('devices').where('sharing_active', '==', True)
            devices = []
            
            for doc in devices_ref.stream():
                device_data = doc.to_dict()
                device_data['id'] = doc.id
                devices.append(device_data)
            
            return devices
            
        except Exception as e:
            logger.error(f"Error getting active devices: {e}")
            return []
    
    def stop_sharing(self, device_id: str) -> bool:
        """Stop sharing for a specific device"""
        if not self.is_configured():
            return False
        
        try:
            device_ref = self.db.collection('devices').document(device_id)
            device_ref.update({
                'sharing_active': False,
                'stopped_at': datetime.now(),
                'stopped_by': 'web_interface'
            })
            
            logger.info(f"Sharing stopped for device: {device_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error stopping sharing for device {device_id}: {e}")
            return False
    
    def start_sharing(self, device_id: str) -> bool:
        """Start sharing for a specific device"""
        if not self.is_configured():
            return False
        
        try:
            device_ref = self.db.collection('devices').document(device_id)
            device_ref.update({
                'sharing_active': True,
                'started_at': datetime.now(),
                'started_by': 'web_interface'
            })
            
            logger.info(f"Sharing started for device: {device_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error starting sharing for device {device_id}: {e}")
            return False
    
    def get_recent_files(self, limit: int = 10) -> List[Dict]:
        """Get recently uploaded file metadata"""
        if not self.is_configured():
            return []
        
        try:
            files_ref = self.db.collection('file_metadata').order_by('uploaded_at', direction=firestore.Query.DESCENDING).limit(limit)
            files = []
            
            for doc in files_ref.stream():
                file_data = doc.to_dict()
                file_data['id'] = doc.id
                files.append(file_data)
            
            return files
            
        except Exception as e:
            logger.error(f"Error getting recent files: {e}")
            return []
    
    def get_recent_locations(self, limit: int = 5) -> List[Dict]:
        """Get recent location updates"""
        if not self.is_configured():
            return []
        
        try:
            locations_ref = self.db.collection('locations').order_by('timestamp', direction=firestore.Query.DESCENDING).limit(limit)
            locations = []
            
            for doc in locations_ref.stream():
                location_data = doc.to_dict()
                location_data['id'] = doc.id
                locations.append(location_data)
            
            return locations
            
        except Exception as e:
            logger.error(f"Error getting recent locations: {e}")
            return []
    
    def add_device(self, device_id: str, device_info: Dict) -> bool:
        """Add or update device information"""
        if not self.is_configured():
            return False
        
        try:
            device_ref = self.db.collection('devices').document(device_id)
            device_data = {
                'last_seen': datetime.now(),
                'sharing_active': True,
                **device_info
            }
            device_ref.set(device_data, merge=True)
            
            logger.info(f"Device added/updated: {device_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error adding device {device_id}: {e}")
            return False
