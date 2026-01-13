"""
Progress hook for yt-dlp downloads to report progress back to Kotlin
"""

class ProgressHook:
    def __init__(self):
        self.progress = 0.0
        self.downloaded = 0
        self.total = 0
        self.speed = 0
        self.status = "downloading"
        self.filename = ""
    
    def __call__(self, d):
        """Called by yt-dlp during download"""
        self.status = d.get('status', 'downloading')
        self.filename = d.get('filename', '')
        
        if self.status == 'downloading':
            # Get download progress
            downloaded = d.get('downloaded_bytes', 0)
            total = d.get('total_bytes') or d.get('total_bytes_estimate', 0)
            speed = d.get('speed', 0) or 0
            
            self.downloaded = downloaded
            self.total = total
            self.speed = int(speed)
            
            if total > 0:
                self.progress = (downloaded / total) * 100.0
            else:
                self.progress = 0.0
        
        elif self.status == 'finished':
            self.progress = 100.0
    
    def get_progress(self):
        """Get current progress as percentage (0-100)"""
        return self.progress
    
    def get_downloaded(self):
        """Get downloaded bytes"""
        return self.downloaded
    
    def get_total(self):
        """Get total bytes"""
        return self.total
    
    def get_speed(self):
        """Get download speed in bytes/sec"""
        return self.speed
    
    def get_status(self):
        """Get current status"""
        return self.status
