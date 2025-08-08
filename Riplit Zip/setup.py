#!/usr/bin/env python3
"""
MySharingApp Setup Script
Handles installation and configuration of the complete file sharing system.
"""

import os
import sys
import json
import subprocess
import shutil
from pathlib import Path

def print_banner():
    """Print setup banner"""
    print("=" * 60)
    print("    MySharingApp - Complete File Sharing System Setup")
    print("=" * 60)
    print()

def check_python_version():
    """Check Python version compatibility"""
    if sys.version_info < (3, 7):
        print("❌ Python 3.7 or higher is required")
        sys.exit(1)
    print(f"✅ Python {sys.version_info.major}.{sys.version_info.minor} detected")

def install_python_dependencies():
    """Install Python dependencies"""
    print("\n📦 Installing Python dependencies...")
    
    dependencies = [
        "flask>=2.3.0",
        "firebase-admin>=6.2.0",
        "requests>=2.31.0",
        "python-dotenv>=1.0.0"
    ]
    
    try:
        for dep in dependencies:
            print(f"Installing {dep}...")
            subprocess.run([sys.executable, "-m", "pip", "install", dep], 
                         check=True, capture_output=True)
        print("✅ All Python dependencies installed successfully")
    except subprocess.CalledProcessError as e:
        print(f"❌ Error installing dependencies: {e}")
        return False
    return True

def setup_firebase_credentials():
    """Setup Firebase credentials"""
    print("\n🔥 Setting up Firebase credentials...")
    
    credentials_path = "credentials.json"
    template_path = "credentials_template.json"
    
    if os.path.exists(credentials_path):
        print("✅ Firebase credentials already exist")
        return True
    
    if os.path.exists(template_path):
        print("📝 Found credentials template")
        print("Please edit credentials.json with your Firebase service account details")
        shutil.copy(template_path, credentials_path)
        return True
    
    print("❌ No Firebase credentials found. Please:")
    print("1. Go to Firebase Console")
    print("2. Project Settings > Service Accounts")
    print("3. Generate new private key")
    print("4. Save as credentials.json in project root")
    return False

def setup_environment_file():
    """Create .env file with configuration"""
    print("\n⚙️  Setting up environment configuration...")
    
    env_path = ".env"
    
    if os.path.exists(env_path):
        print("✅ Environment file already exists")
        return True
    
    env_content = """# MySharingApp Environment Configuration

# Firebase Configuration
FIREBASE_CREDENTIALS_PATH=credentials.json
FIREBASE_CREDENTIALS=

# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=

# Flask Configuration
SECRET_KEY=your-secret-key-change-this
FLASK_ENV=development
FLASK_DEBUG=True

# Server Configuration
HOST=0.0.0.0
PORT=5000
"""
    
    try:
        with open(env_path, 'w') as f:
            f.write(env_content)
        print("✅ Environment file created")
        print("📝 Please edit .env file with your configuration")
        return True
    except Exception as e:
        print(f"❌ Error creating environment file: {e}")
        return False

def check_android_setup():
    """Check Android development setup"""
    print("\n📱 Checking Android development setup...")
    
    android_dir = Path("android/MySharingApp")
    
    if not android_dir.exists():
        print("❌ Android project directory not found")
        return False
    
    gradle_wrapper = android_dir / "gradlew"
    if not gradle_wrapper.exists():
        print("❌ Gradle wrapper not found")
        return False
    
    print("✅ Android project structure found")
    print("📝 To build Android app:")
    print(f"   cd {android_dir}")
    print("   ./gradlew assembleDebug")
    
    return True

def create_firebase_rules():
    """Create Firestore security rules"""
    print("\n🔒 Creating Firestore security rules...")
    
    rules_content = '''rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to devices collection for authenticated users
    match /devices/{document=**} {
      allow read, write: if request.auth != null;
    }
    
    // Allow read/write access to locations collection for authenticated users
    match /locations/{document=**} {
      allow read, write: if request.auth != null;
    }
    
    // Allow read/write access to file_metadata collection for authenticated users
    match /file_metadata/{document=**} {
      allow read, write: if request.auth != null;
    }
    
    // Allow read/write access to file_deletions collection for authenticated users  
    match /file_deletions/{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}'''
    
    try:
        with open("firestore.rules", 'w') as f:
            f.write(rules_content)
        print("✅ Firestore rules created")
        print("📝 Deploy rules with: firebase deploy --only firestore:rules")
        return True
    except Exception as e:
        print(f"❌ Error creating Firestore rules: {e}")
        return False

def verify_setup():
    """Verify the setup is complete"""
    print("\n🔍 Verifying setup...")
    
    checks = [
        ("credentials.json", "Firebase credentials"),
        (".env", "Environment configuration"),
        ("app.py", "Flask application"),
        ("android/MySharingApp", "Android project")
    ]
    
    all_good = True
    for file_path, description in checks:
        if os.path.exists(file_path):
            print(f"✅ {description}")
        else:
            print(f"❌ {description} missing")
            all_good = False
    
    return all_good

def print_next_steps():
    """Print next steps for the user"""
    print("\n🚀 Setup Complete! Next Steps:")
    print("\n1. Configure Firebase:")
    print("   - Edit credentials.json with your Firebase service account key")
    print("   - Deploy Firestore rules: firebase deploy --only firestore:rules")
    
    print("\n2. Configure Telegram (optional):")
    print("   - Create a Telegram bot with @BotFather")
    print("   - Add TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID to .env")
    
    print("\n3. Start the web application:")
    print("   python app.py")
    print("   Access at: http://localhost:5000")
    
    print("\n4. Build Android app:")
    print("   cd android/MySharingApp")
    print("   ./gradlew assembleDebug")
    print("   Install APK on device")
    
    print("\n5. Connect Android app:")
    print("   - Grant all permissions in the app")
    print("   - Enable file sharing")
    print("   - Monitor from web interface")
    
    print("\n📚 Documentation:")
    print("   - Web Interface: http://localhost:5000")
    print("   - Firebase Console: https://console.firebase.google.com")
    print("   - Telegram Bot: https://core.telegram.org/bots")

def main():
    """Main setup function"""
    print_banner()
    
    # Check Python version
    check_python_version()
    
    # Install dependencies
    if not install_python_dependencies():
        print("❌ Failed to install Python dependencies")
        sys.exit(1)
    
    # Setup Firebase
    if not setup_firebase_credentials():
        print("⚠️  Firebase setup incomplete - manual configuration required")
    
    # Setup environment
    if not setup_environment_file():
        print("❌ Failed to create environment file")
        sys.exit(1)
    
    # Check Android setup
    check_android_setup()
    
    # Create Firestore rules
    create_firebase_rules()
    
    # Verify setup
    if verify_setup():
        print("\n✅ Setup verification passed!")
    else:
        print("\n⚠️  Some components need manual configuration")
    
    # Print next steps
    print_next_steps()

if __name__ == "__main__":
    main()
