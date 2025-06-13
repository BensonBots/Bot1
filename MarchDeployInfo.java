package newgame;

import java.time.LocalDateTime;

/**
 * Data class to track information about deployed marches
 * Used to manage march lifecycle from deployment to details collection
 * FIXED: Removed unnecessary buffer time from arrival detection
 */
public class MarchDeployInfo {
    public int queueNumber;
    public String resourceType;
    public LocalDateTime deployTime;
    public String estimatedDeployDuration;
    public boolean detailsCollected = false;
    public String actualGatheringTime = null;
    
    public MarchDeployInfo(int queueNumber, String resourceType, LocalDateTime deployTime, String estimatedDeployDuration) {
        this.queueNumber = queueNumber;
        this.resourceType = resourceType;
        this.deployTime = deployTime;
        this.estimatedDeployDuration = estimatedDeployDuration;
    }
    
    /**
     * FIXED: Check if this march should have arrived at its destination (NO buffer)
     */
    public boolean hasArrived() {
        long deploySeconds = TimeUtils.parseTimeToSeconds(estimatedDeployDuration);
        LocalDateTime expectedArrival = deployTime.plusSeconds(deploySeconds);
        // FIXED: No buffer time - check as soon as march should have arrived
        return LocalDateTime.now().isAfter(expectedArrival) || LocalDateTime.now().isEqual(expectedArrival);
    }
    
    /**
     * Get seconds remaining until march arrives
     */
    public long getSecondsUntilArrival() {
        long deploySeconds = TimeUtils.parseTimeToSeconds(estimatedDeployDuration);
        LocalDateTime expectedArrival = deployTime.plusSeconds(deploySeconds);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(expectedArrival) || now.isEqual(expectedArrival)) {
            return 0;
        }
        
        return java.time.Duration.between(now, expectedArrival).getSeconds();
    }
    
    /**
     * Calculate total march time: deploy + gathering + return
     */
    public String calculateTotalTime() {
        if (actualGatheringTime == null) {
            return null;
        }
        
        long deploySeconds = TimeUtils.parseTimeToSeconds(estimatedDeployDuration);
        long gatheringSeconds = TimeUtils.parseTimeToSeconds(actualGatheringTime);
        long totalSeconds = deploySeconds + gatheringSeconds + deploySeconds; // Round trip
        
        return TimeUtils.formatTime(totalSeconds);
    }
    
    @Override
    public String toString() {
        return String.format("Queue %d: %s (deployed at %s, estimated: %s, details collected: %s, gathering time: %s)", 
            queueNumber, resourceType, deployTime.toString(), estimatedDeployDuration, detailsCollected, actualGatheringTime);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarchDeployInfo that = (MarchDeployInfo) obj;
        return queueNumber == that.queueNumber && 
               deployTime.equals(that.deployTime);
    }
    
    @Override
    public int hashCode() {
        return queueNumber * 1000 + deployTime.hashCode();
    }
}