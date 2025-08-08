# MySharingApp - Complete File Sharing System

## Overview

MySharingApp is a comprehensive file sharing and device monitoring system that combines an Android application with a web-based control panel. The system provides real-time location tracking, file monitoring, and remote device management capabilities. The Android app runs background services to monitor files and locations, while the web interface offers a dashboard for device management, file activity monitoring, and location tracking with Telegram notifications.

## User Preferences

Preferred communication style: Simple, everyday language.

## System Architecture

### Frontend Architecture
- **Web Interface**: Built with Flask templating engine using Bootstrap 5 for responsive UI
- **Static Assets**: CSS and JavaScript files served from static directory
- **Navigation**: Multi-page application with index, dashboard, and stop sharing pages
- **Real-time Updates**: JavaScript-based auto-refresh functionality with 30-second intervals

### Backend Architecture
- **Flask Web Server**: Python-based web application with route handlers for dashboard, device management, and sharing control
- **Firebase Integration**: Centralized data management using Firestore for real-time synchronization
- **Modular Design**: Separate modules for Firebase operations (`firebase_config.py`) and Telegram integration (`telegram_bot.py`)
- **Error Handling**: Comprehensive logging and exception handling throughout the application

### Data Management
- **Firebase Firestore**: Primary database for device information, file activity, and location data
- **Real-time Sync**: Automatic data synchronization between Android app and web interface
- **Data Models**: Collections for connected devices, recent files, and location history

### Authentication & Security
- **Firebase Service Account**: Server-side authentication using service account credentials
- **Environment Variables**: Secure storage of sensitive credentials and API tokens
- **Session Management**: Flask session handling for web interface state

### Notification System
- **Telegram Bot Integration**: Instant notifications for device events and system alerts
- **Configurable Notifications**: Optional Telegram setup with fallback operation
- **HTML-formatted Messages**: Rich text notifications with timestamps and formatting

### Device Communication
- **Android Background Services**: Continuous monitoring services for file changes and location updates
- **Firebase SDK Integration**: Direct communication between Android app and Firestore database
- **Remote Control**: Web interface can trigger start/stop commands for Android services

## External Dependencies

### Cloud Services
- **Firebase Firestore**: Real-time NoSQL database for data synchronization and storage
- **Firebase Admin SDK**: Server-side Firebase operations and authentication
- **Google Location Services**: GPS location tracking on Android devices

### Third-party APIs
- **Telegram Bot API**: Notification delivery system for device events and alerts
- **Bootstrap CDN**: Frontend CSS framework for responsive design
- **Font Awesome CDN**: Icon library for user interface elements

### Python Libraries
- **Flask**: Web framework for the control panel interface
- **firebase-admin**: Firebase server SDK for database operations
- **requests**: HTTP client for Telegram API communication
- **python-dotenv**: Environment variable management

### Development Tools
- **Android Studio**: Development environment for Android application
- **Firebase Console**: Database management and configuration
- **Git**: Version control system for code management

### Optional Integrations
- **Telegram Bot**: Configurable notification system (optional but recommended)
- **Custom Domain**: Web interface can be deployed with custom domain configuration