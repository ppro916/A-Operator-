// MySharingApp Main JavaScript File

// Global variables
let refreshInterval;
let notificationTimeout;

// Initialize when document is ready
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

// App initialization
function initializeApp() {
    console.log('MySharingApp initialized');
    
    // Initialize tooltips
    initializeTooltips();
    
    // Setup auto-refresh
    setupAutoRefresh();
    
    // Setup toast notifications
    setupToasts();
    
    // Initialize charts if needed
    initializeCharts();
}

// Initialize Bootstrap tooltips
function initializeTooltips() {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

// Setup auto-refresh functionality
function setupAutoRefresh() {
    // Refresh every 30 seconds
    refreshInterval = setInterval(function() {
        if (document.visibilityState === 'visible') {
            refreshData();
        }
    }, 30000);
    
    // Refresh when page becomes visible
    document.addEventListener('visibilitychange', function() {
        if (document.visibilityState === 'visible') {
            refreshData();
        }
    });
}

// Setup toast notifications
function setupToasts() {
    // Auto-hide toasts after 5 seconds
    const toasts = document.querySelectorAll('.toast');
    toasts.forEach(toast => {
        const bsToast = new bootstrap.Toast(toast, {
            autohide: true,
            delay: 5000
        });
        bsToast.show();
    });
}

// Check system status
function checkSystemStatus() {
    const statusElement = document.getElementById('system-status');
    if (!statusElement) return;
    
    fetch('/health')
        .then(response => response.json())
        .then(data => {
            if (data.status === 'healthy') {
                statusElement.innerHTML = `
                    <i class="fas fa-check-circle text-success me-2"></i>
                    <span class="text-success">System Online</span>
                `;
            } else {
                statusElement.innerHTML = `
                    <i class="fas fa-exclamation-triangle text-warning me-2"></i>
                    <span class="text-warning">System Issues</span>
                `;
            }
        })
        .catch(error => {
            console.error('System status check failed:', error);
            statusElement.innerHTML = `
                <i class="fas fa-times-circle text-danger me-2"></i>
                <span class="text-danger">System Offline</span>
            `;
        });
}

// Load device count
function loadDeviceCount() {
    const countElement = document.getElementById('device-count');
    if (!countElement) return;
    
    fetch('/api/devices')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const onlineCount = data.devices.filter(device => device.online).length;
                const totalCount = data.devices.length;
                
                countElement.innerHTML = `
                    <span class="h4 text-success">${onlineCount}</span>
                    <span class="text-muted">/ ${totalCount} online</span>
                `;
            } else {
                countElement.innerHTML = `
                    <i class="fas fa-exclamation-triangle text-warning me-2"></i>
                    <span class="text-warning">Error loading</span>
                `;
            }
        })
        .catch(error => {
            console.error('Device count load failed:', error);
            countElement.innerHTML = `
                <i class="fas fa-times-circle text-danger me-2"></i>
                <span class="text-danger">Connection failed</span>
            `;
        });
}

// Refresh data
function refreshData() {
    checkSystemStatus();
    loadDeviceCount();
    refreshDeviceTable();
    refreshRecentActivity();
}

// Refresh device table
function refreshDeviceTable() {
    const table = document.querySelector('.table tbody');
    if (!table) return;
    
    fetch('/api/devices')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                updateDeviceTable(data.devices);
            }
        })
        .catch(error => {
            console.error('Device table refresh failed:', error);
        });
}

// Update device table
function updateDeviceTable(devices) {
    const tbody = document.querySelector('.table tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    devices.forEach(device => {
        const row = createDeviceRow(device);
        tbody.appendChild(row);
    });
}

// Create device table row
function createDeviceRow(device) {
    const row = document.createElement('tr');
    row.innerHTML = `
        <td>
            <code>${device.id.substring(0, 12)}...</code>
            <br>
            <small class="text-muted">${device.device_name || 'Unknown'}</small>
        </td>
        <td>
            ${device.online ? 
                '<span class="badge bg-success"><i class="fas fa-circle me-1"></i>Online</span>' : 
                '<span class="badge bg-secondary"><i class="fas fa-circle me-1"></i>Offline</span>'
            }
        </td>
        <td>
            ${device.sharing_active ? 
                '<span class="badge bg-warning"><i class="fas fa-play me-1"></i>Active</span>' : 
                '<span class="badge bg-secondary"><i class="fas fa-pause me-1"></i>Stopped</span>'
            }
        </td>
        <td>
            <small>${device.last_seen || 'Never'}</small>
        </td>
        <td>
            <div class="btn-group btn-group-sm">
                ${device.sharing_active ? 
                    `<button class="btn btn-outline-danger btn-sm" onclick="toggleSharing('${device.id}', false)">
                        <i class="fas fa-stop"></i>
                    </button>` :
                    `<button class="btn btn-outline-success btn-sm" onclick="toggleSharing('${device.id}', true)">
                        <i class="fas fa-play"></i>
                    </button>`
                }
            </div>
        </td>
    `;
    return row;
}

// Refresh recent activity
function refreshRecentActivity() {
    refreshRecentFiles();
    refreshRecentLocations();
}

// Refresh recent files
function refreshRecentFiles() {
    fetch('/api/files?limit=5')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                updateRecentFiles(data.files);
            }
        })
        .catch(error => {
            console.error('Recent files refresh failed:', error);
        });
}

// Update recent files display
function updateRecentFiles(files) {
    const container = document.querySelector('.recent-files-container');
    if (!container) return;
    
    if (files.length === 0) {
        container.innerHTML = `
            <div class="text-center py-3">
                <i class="fas fa-file-alt fa-2x text-muted mb-2"></i>
                <p class="text-muted mb-0 small">No recent files</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = '';
    files.forEach(file => {
        const fileItem = createFileItem(file);
        container.appendChild(fileItem);
    });
}

// Create file item
function createFileItem(file) {
    const item = document.createElement('div');
    item.className = 'file-item';
    item.innerHTML = `
        <div class="file-icon">
            <i class="fas fa-file text-primary"></i>
        </div>
        <div class="flex-grow-1">
            <h6 class="mb-0 small">${(file.file_name || 'Unknown').substring(0, 20)}...</h6>
            <small class="text-muted">${(file.device_id || 'Unknown').substring(0, 8)}...</small>
        </div>
        <div class="file-size">
            <small class="text-muted">${file.size || 'Unknown'}</small>
        </div>
    `;
    return item;
}

// Refresh recent locations
function refreshRecentLocations() {
    fetch('/api/locations?limit=5')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                updateRecentLocations(data.locations);
            }
        })
        .catch(error => {
            console.error('Recent locations refresh failed:', error);
        });
}

// Update recent locations display
function updateRecentLocations(locations) {
    const container = document.querySelector('.recent-locations-container');
    if (!container) return;
    
    if (locations.length === 0) {
        container.innerHTML = `
            <div class="text-center py-3">
                <i class="fas fa-map-marker-alt fa-2x text-muted mb-2"></i>
                <p class="text-muted mb-0 small">No recent locations</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = '';
    locations.forEach(location => {
        const locationItem = createLocationItem(location);
        container.appendChild(locationItem);
    });
}

// Create location item
function createLocationItem(location) {
    const item = document.createElement('div');
    item.className = 'location-item';
    item.innerHTML = `
        <div class="location-icon">
            <i class="fas fa-map-pin text-danger"></i>
        </div>
        <div class="flex-grow-1 ms-3">
            <h6 class="mb-0 small">${(location.device_id || 'Unknown').substring(0, 8)}...</h6>
            <small class="text-muted">
                ${(location.latitude || 0).toFixed(4)}, ${(location.longitude || 0).toFixed(4)}
            </small>
        </div>
        <div class="location-accuracy">
            <small class="text-muted">${Math.round(location.accuracy || 0)}m</small>
        </div>
    `;
    return item;
}

// Toggle sharing for device
function toggleSharing(deviceId, start) {
    const url = start ? '/start_sharing' : '/stop_sharing';
    const formData = new FormData();
    formData.append('device_id', deviceId);
    
    // Show loading state
    showLoadingState();
    
    fetch(url, {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        hideLoadingState();
        
        if (data.success) {
            showToast('Success', data.message, 'success');
            setTimeout(() => {
                refreshData();
            }, 1000);
        } else {
            showToast('Error', data.message, 'error');
        }
    })
    .catch(error => {
        hideLoadingState();
        console.error('Toggle sharing failed:', error);
        showToast('Error', 'Network error occurred', 'error');
    });
}

// Show loading state
function showLoadingState() {
    document.body.classList.add('loading');
}

// Hide loading state
function hideLoadingState() {
    document.body.classList.remove('loading');
}

// Show toast notification
function showToast(title, message, type = 'info') {
    // Clear existing timeout
    if (notificationTimeout) {
        clearTimeout(notificationTimeout);
    }
    
    // Create toast element
    const toastContainer = getOrCreateToastContainer();
    const toast = createToastElement(title, message, type);
    
    toastContainer.appendChild(toast);
    
    // Show toast
    const bsToast = new bootstrap.Toast(toast, {
        autohide: true,
        delay: 5000
    });
    bsToast.show();
    
    // Remove toast after hiding
    toast.addEventListener('hidden.bs.toast', function() {
        toast.remove();
    });
}

// Get or create toast container
function getOrCreateToastContainer() {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '1050';
        document.body.appendChild(container);
    }
    return container;
}

// Create toast element
function createToastElement(title, message, type) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.setAttribute('role', 'alert');
    
    const iconMap = {
        success: 'fas fa-check-circle text-success',
        error: 'fas fa-exclamation-triangle text-danger',
        warning: 'fas fa-exclamation-triangle text-warning',
        info: 'fas fa-info-circle text-info'
    };
    
    toast.innerHTML = `
        <div class="toast-header">
            <i class="${iconMap[type] || iconMap.info} me-2"></i>
            <strong class="me-auto">${title}</strong>
            <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
        </div>
        <div class="toast-body">${message}</div>
    `;
    
    return toast;
}

// Test connections
function testConnections() {
    showLoadingState();
    showToast('Testing', 'Checking system connections...', 'info');
    
    fetch('/health')
        .then(response => response.json())
        .then(data => {
            hideLoadingState();
            
            let message = 'Connection Test Results:\n';
            message += `• System: ${data.status === 'healthy' ? '✓' : '✗'}\n`;
            message += `• Firebase: ${data.firebase_connected ? '✓' : '✗'}\n`;
            message += `• Telegram: ${data.telegram_configured ? '✓' : '✗'}`;
            
            const allGood = data.status === 'healthy' && data.firebase_connected && data.telegram_configured;
            showToast('Connection Test', message, allGood ? 'success' : 'warning');
        })
        .catch(error => {
            hideLoadingState();
            console.error('Connection test failed:', error);
            showToast('Error', 'Connection test failed', 'error');
        });
}

// Initialize charts (placeholder for future implementation)
function initializeCharts() {
    // Chart.js implementation can be added here
    console.log('Charts initialized');
}

// Utility functions
function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatTime(timestamp) {
    if (!timestamp) return 'Never';
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
}

function formatDate(timestamp) {
    if (!timestamp) return 'Never';
    const date = new Date(timestamp);
    return date.toLocaleDateString();
}

// Cleanup on page unload
window.addEventListener('beforeunload', function() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
    if (notificationTimeout) {
        clearTimeout(notificationTimeout);
    }
});

// Export functions for global use
window.MySharingApp = {
    refreshData,
    toggleSharing,
    testConnections,
    showToast,
    checkSystemStatus,
    loadDeviceCount
};
