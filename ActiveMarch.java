package newgame;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents an active march with timing information
 */
public class ActiveMarch {
    private final int instanceIndex;
    private final int queueNumber;
    private final String resourceType;
    private final String gatheringTime;
    private final String marchingTime;
    private final String totalTime;
    private final LocalDateTime startTime;
    private final long totalDurationSeconds;
    
    private String status;
    private LocalDateTime statusChangeTime;

    public ActiveMarch(int instanceIndex, int queueNumber, String resourceType,
                      String gatheringTime, String marchingTime, String totalTime,
                      LocalDateTime startTime) {
        this.instanceIndex = instanceIndex;
        this.queueNumber = queueNumber;
        this.resourceType = resourceType;
        this.gatheringTime = gatheringTime;
        this.marchingTime = marchingTime;
        this.totalTime = totalTime;
        this.startTime = startTime;
        this.status = "Marching to Resource";
        this.statusChangeTime = startTime;
        this.totalDurationSeconds = parseTimeToSeconds(totalTime);
    }

    /**
     * Convert time string (HH:MM:SS) to seconds
     */
    private long parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 3) {
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                long seconds = Long.parseLong(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error parsing time: " + timeStr);
            return 0;
        }
    }

    /**
     * Get time remaining until march completion
     */
    public long getTimeRemaining() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        long remaining = totalDurationSeconds - elapsedSeconds;
        return Math.max(0, remaining);
    }

    /**
     * Get progress percentage (0-100)
     */
    public double getProgressPercentage() {
        if (totalDurationSeconds <= 0) return 100.0;
        
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        double progress = (double) elapsedSeconds / totalDurationSeconds * 100.0;
        return Math.min(100.0, Math.max(0.0, progress));
    }

    /**
     * Get estimated time of arrival (completion)
     */
    public LocalDateTime getETA() {
        return startTime.plusSeconds(totalDurationSeconds);
    }

    /**
     * Check if march is completed
     */
    public boolean isCompleted() {
        return getTimeRemaining() <= 0;
    }

    /**
     * Update march status with smart status detection
     */
    public void updateStatus() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        long marchingSeconds = parseTimeToSeconds(marchingTime);
        long gatheringSeconds = parseTimeToSeconds(gatheringTime);
        
        if (elapsedSeconds < marchingSeconds) {
            setStatus("🚶 Marching to Resource");
        } else if (elapsedSeconds < marchingSeconds + gatheringSeconds) {
            setStatus("⛏️ Gathering " + resourceType);
        } else if (elapsedSeconds < totalDurationSeconds) {
            setStatus("🏠 Returning Home");
        } else {
            setStatus("✅ Completed");
        }
    }

    // Getters and setters
    public int getInstanceIndex() { return instanceIndex; }
    public int getQueueNumber() { return queueNumber; }
    public String getResourceType() { return resourceType; }
    public String getGatheringTime() { return gatheringTime; }
    public String getMarchingTime() { return marchingTime; }
    public String getTotalTime() { return totalTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public String getStatus() { 
        updateStatus(); // Auto-update status when requested
        return status; 
    }

    public void setStatus(String status) {
        if (!this.status.equals(status)) {
            this.status = status;
            this.statusChangeTime = LocalDateTime.now();
        }
    }

    /**
     * Get current phase of the march
     */
    public String getCurrentPhase() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        long marchingSeconds = parseTimeToSeconds(marchingTime);
        long gatheringSeconds = parseTimeToSeconds(gatheringTime);
        
        if (elapsedSeconds < marchingSeconds) {
            return "Marching (" + formatTime(marchingSeconds - elapsedSeconds) + " remaining)";
        } else if (elapsedSeconds < marchingSeconds + gatheringSeconds) {
            long gatheringRemaining = marchingSeconds + gatheringSeconds - elapsedSeconds;
            return "Gathering (" + formatTime(gatheringRemaining) + " remaining)";
        } else if (elapsedSeconds < totalDurationSeconds) {
            long returningRemaining = totalDurationSeconds - elapsedSeconds;
            return "Returning (" + formatTime(returningRemaining) + " remaining)";
        } else {
            return "Completed";
        }
    }

    /**
     * Format seconds to HH:MM:SS
     */
    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public String toString() {
        return String.format("March[Instance:%d, Queue:%d, Resource:%s, Status:%s, Remaining:%s]",
            instanceIndex, queueNumber, resourceType, getStatus(), formatTime(getTimeRemaining()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ActiveMarch that = (ActiveMarch) obj;
        return instanceIndex == that.instanceIndex && queueNumber == that.queueNumber;
    }

    @Override
    public int hashCode() {
        return instanceIndex * 100 + queueNumber;
    }
}