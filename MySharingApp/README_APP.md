# MySharingApp

## Overview
MySharingApp is an Android application that runs as a background service, tracks device location, monitors media files in real-time, and uploads their metadata to Firebase. The app also allows you to stop sharing data via a web interface.

## Features
- **Background Service**: The app runs continuously in the background, monitoring files and uploading metadata.
- **Location Tracking**: Tracks the device's location periodically and uploads it to Firebase.
- **File Monitoring**: Watches specified folders for new files and uploads metadata to Firebase.
- **Telegram Integration**: Sends notifications via Telegram bot for important events.
- **Web Interface**: Allows stopping the file sharing service through a web page hosted via GitHub.

## Setup Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/MySharingApp.git
