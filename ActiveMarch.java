package newgame;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents an active march with timing information
 * FIXED: Better status detection logic and corrected time remaining calculation
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
    private final long marchingDurationSeconds;
    private final long gatheringDurationSeconds;
    
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
        this.status = "🚶 Marching to Resource";
        this.statusChangeTime = startTime;
        
        // FIXED: Pre-calculate durations for better performance
        this.marchingDurationSeconds = parseTimeToSeconds(marchingTime);
        this.gatheringDurationSeconds = parseTimeToSeconds(gatheringTime);
        
        // FIXED: Calculate correct total duration based on actual phases
        // Since troops are already at resource, total = gathering + march back home
        // But we still need the original total for status transitions
        this.totalDurationSeconds = parseTimeToSeconds(totalTime);
        
        // FIXED: Set initial status correctly based on current time
        updateStatusImmediate();
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
     * FIXED: Get time remaining - only gathering + return march (not initial march)
     * Since troops are already at the resource location
     */
    public long getTimeRemaining() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        // FIXED: If already marching to resource, skip that phase for time calculation
        if (elapsedSeconds < marchingDurationSeconds) {
            // Still in initial march phase - remaining = gathering + return march
            return gatheringDurationSeconds + marchingDurationSeconds;
        } else {
            // Past initial march - remaining = total - elapsed
            long remaining = totalDurationSeconds - elapsedSeconds;
            return Math.max(0, remaining);
        }
    }

    /**
     * FIXED: Get progress percentage based on the corrected time calculation
     */
    public double getProgressPercentage() {
        if (totalDurationSeconds <= 0) return 100.0;
        
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        // FIXED: Progress calculation should account for the actual meaningful time
        // Once troops reach resource, progress is based on gathering + return
        if (elapsedSeconds < marchingDurationSeconds) {
            // Still marching to resource - progress based on march completion
            double marchProgress = (double) elapsedSeconds / marchingDurationSeconds * 25.0; // 25% for reaching resource
            return Math.min(25.0, Math.max(0.0, marchProgress));
        } else {
            // At resource or returning - progress based on remaining phases
            long remainingPhaseTime = totalDurationSeconds - marchingDurationSeconds;
            long elapsedInRemaining = elapsedSeconds - marchingDurationSeconds;
            double remainingProgress = (double) elapsedInRemaining / remainingPhaseTime * 75.0; // 75% for gathering + return
            return Math.min(100.0, Math.max(25.0, 25.0 + remainingProgress));
        }
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
     * FIXED: Update march status with better logic and performance
     */
    public void updateStatus() {
        updateStatusImmediate();
    }
    
    /**
     * FIXED: Immediate status update without calling getStatus()
     */
    private void updateStatusImmediate() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        String newStatus;
        
        if (elapsedSeconds < marchingDurationSeconds) {
            // Still marching to resource
            newStatus = "🚶 Marching to Resource";
        } else if (elapsedSeconds < marchingDurationSeconds + gatheringDurationSeconds) {
            // Currently gathering
            newStatus = "⛏️ Gathering " + resourceType;
        } else if (elapsedSeconds < totalDurationSeconds) {
            // Returning home
            newStatus = "🏠 Returning Home";
        } else {
            // Completed
            newStatus = "✅ Completed";
        }
        
        // FIXED: Only update if status actually changed to avoid unnecessary operations
        if (!newStatus.equals(this.status)) {
            System.out.println("📊 [STATUS UPDATE] Queue " + queueNumber + ": " + this.status + " → " + newStatus + 
                             " (elapsed: " + formatTime(elapsedSeconds) + "/" + totalTime + ")");
            setStatus(newStatus);
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
    
    /**
     * FIXED: Get status with automatic update but prevent infinite recursion
     */
    public String getStatus() { 
        // Don't call updateStatus() here to avoid recursion in GUI updates
        // Status should be updated by the timer in MarchTrackerGUI
        return status; 
    }

    public void setStatus(String status) {
        if (!this.status.equals(status)) {
            this.status = status;
            this.statusChangeTime = LocalDateTime.now();
        }
    }

    /**
     * FIXED: Get current phase with better performance and corrected time display
     */
    public String getCurrentPhase() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        if (elapsedSeconds < marchingDurationSeconds) {
            long marchingRemaining = marchingDurationSeconds - elapsedSeconds;
            return "Marching (" + formatTime(marchingRemaining) + " remaining)";
        } else if (elapsedSeconds < marchingDurationSeconds + gatheringDurationSeconds) {
            long gatheringRemaining = marchingDurationSeconds + gatheringDurationSeconds - elapsedSeconds;
            return "Gathering (" + formatTime(gatheringRemaining) + " remaining)";
        } else if (elapsedSeconds < totalDurationSeconds) {
            long returningRemaining = totalDurationSeconds - elapsedSeconds;
            return "Returning (" + formatTime(returningRemaining) + " remaining)";
        } else {
            return "Completed";
        }
    }

    /**
     * ADDED: Get the effective remaining time for display purposes
     * This shows only the time that matters to the user (gathering + return)
     */
    public long getEffectiveTimeRemaining() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        if (elapsedSeconds < marchingDurationSeconds) {
            // Still marching - show gathering + return time
            return gatheringDurationSeconds + marchingDurationSeconds;
        } else {
            // Already at resource or returning - show actual remaining time
            long remaining = totalDurationSeconds - elapsedSeconds;
            return Math.max(0, remaining);
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