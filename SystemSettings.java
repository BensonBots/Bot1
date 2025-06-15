package newgame;

/**
 * System-wide settings for resource management
 */
public class SystemSettings {
    public int maxConcurrentInstances = 2;
    public boolean autoRotationEnabled = true;
    public int rotationIntervalHours = 4;
    public boolean hibernationEnabled = true;
    public boolean resourceMonitoringEnabled = true;
    
    // Resource thresholds
    public double maxMemoryUsageGB = 8.0;
    public double maxCpuUsagePercent = 80.0;
    
    public SystemSettings() {
        // Default values set above
    }
    
    public SystemSettings(int maxConcurrent) {
        this.maxConcurrentInstances = maxConcurrent;
    }
    
    public boolean isWithinResourceLimits() {
        // Simple resource check
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double usedMemoryGB = usedMemory / (1024.0 * 1024.0 * 1024.0);
        
        return usedMemoryGB <= maxMemoryUsageGB;
    }
    
    @Override
    public String toString() {
        return String.format("SystemSettings[maxConcurrent=%d, autoRotation=%s, hibernation=%s]", 
            maxConcurrentInstances, autoRotationEnabled, hibernationEnabled);
    }
}