package newgame;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents an active march with timing information
 * FIXED: Better status detection logic and corrected time remaining calculation
 * FIXED: Simplified progress calculation that actually works
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
        this.totalDurationSeconds = parseTimeToSeconds(totalTime);
        
        // Debug log the parsed durations
        System.out.println("📊 [MARCH CREATED] Queue " + queueNumber + ":");
        System.out.println("  Marching: " + marchingTime + " (" + marchingDurationSeconds + "s)");
        System.out.println("  Gathering: " + gatheringTime + " (" + gatheringDurationSeconds + "s)");
        System.out.println("  Total: " + totalTime + " (" + totalDurationSeconds + "s)");
        
        // FIXED: Set initial status correctly based on current time
        updateStatusImmediate();
    }

    /**
     * Convert time string (HH:MM:SS) to seconds
     */
    private long parseTimeToSeconds(String timeStr) {
        try {
            if (timeStr == null || timeStr.trim().isEmpty()) {
                return 0;
            }
            
            String[] parts = timeStr.split(":");
            if (parts.length == 3) {
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                long seconds = Long.parseLong(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            } else if (parts.length == 2) {
                long minutes = Long.parseLong(parts[0]);
                long seconds = Long.parseLong(parts[1]);
                return minutes * 60 + seconds;
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error parsing time: " + timeStr);
            return 0;
        }
    }

    /**
     * FIXED: Get time remaining with correct calculation
     */
    public long getTimeRemaining() {
        if (totalDurationSeconds <= 0) {
            return 0;
        }
        
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        long remaining = totalDurationSeconds - elapsedSeconds;
        return Math.max(0, remaining);
    }

    /**
     * FIXED: Reduced logging frequency to prevent console spam
     */
    public double getProgressPercentage() {
        if (totalDurationSeconds <= 0) {
            return 100.0;
        }
        
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        // FIXED: Simple progress calculation
        double progress = (double) elapsedSeconds / (double) totalDurationSeconds * 100.0;
        
        // Ensure progress is within bounds
        progress = Math.min(100.0, Math.max(0.0, progress));
        
        // FIXED: Much less frequent logging - only every 5 minutes or status changes
        if (elapsedSeconds % 300 == 0 || (elapsedSeconds < 60 && elapsedSeconds % 30 == 0)) {
            System.out.println("📊 [PROGRESS] Queue " + queueNumber + 
                             " - Elapsed: " + formatTime(elapsedSeconds) + 
                             " / Total: " + formatTime(totalDurationSeconds) + 
                             " = " + String.format("%.1f", progress) + "%");
        }
        
        return progress;
    }

    /**
     * FIXED: Debug method for progress calculation troubleshooting
     */
    public double getProgressPercentageDetailed() {
        if (totalDurationSeconds <= 0) {
            System.out.println("🔍 [PROGRESS DEBUG] Queue " + queueNumber + ": Total duration is 0 or invalid");
            return 100.0;
        }
        
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        System.out.println("🔍 [PROGRESS DEBUG] Queue " + queueNumber + ":");
        System.out.println("  Start time: " + startTime);
        System.out.println("  Current time: " + LocalDateTime.now());
        System.out.println("  Elapsed seconds: " + elapsedSeconds);
        System.out.println("  Total duration seconds: " + totalDurationSeconds);
        System.out.println("  Marching duration: " + marchingDurationSeconds);
        System.out.println("  Gathering duration: " + gatheringDurationSeconds);
        
        double progress = (double) elapsedSeconds / (double) totalDurationSeconds * 100.0;
        progress = Math.min(100.0, Math.max(0.0, progress));
        
        System.out.println("  Calculated progress: " + String.format("%.1f", progress) + "%");
        
        return progress;
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
        if (seconds < 0) seconds = 0;
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * ADDED: Get progress as a formatted string for display
     */
    public String getProgressString() {
        double progress = getProgressPercentage();
        return String.format("%.1f%%", progress);
    }

    /**
     * ADDED: Check if march is in a specific phase
     */
    public boolean isInPhase(String phase) {
        String currentStatus = getStatus().toLowerCase();
        return currentStatus.contains(phase.toLowerCase());
    }

    /**
     * ADDED: Get time spent in current phase
     */
    public long getTimeInCurrentPhase() {
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        
        if (elapsedSeconds < marchingDurationSeconds) {
            // In marching phase
            return elapsedSeconds;
        } else if (elapsedSeconds < marchingDurationSeconds + gatheringDurationSeconds) {
            // In gathering phase
            return elapsedSeconds - marchingDurationSeconds;
        } else if (elapsedSeconds < totalDurationSeconds) {
            // In returning phase
            return elapsedSeconds - (marchingDurationSeconds + gatheringDurationSeconds);
        } else {
            // Completed
            return 0;
        }
    }

    @Override
    public String toString() {
        return String.format("March[Instance:%d, Queue:%d, Resource:%s, Status:%s, Progress:%.1f%%, Remaining:%s]",
            instanceIndex, queueNumber, resourceType, getStatus(), getProgressPercentage(), formatTime(getTimeRemaining()));
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

    /**
     * ADDED: Create a summary string for logging/debugging
     */
    public String getSummary() {
        return String.format("Q%d:%s[%s]-%.0f%%", 
            queueNumber, resourceType, getStatus().replaceAll("[^A-Za-z]", ""), getProgressPercentage());
    }

    /**
     * ADDED: Validate march data integrity
     */
    public boolean isValid() {
        return instanceIndex >= 0 && 
               queueNumber >= 1 && 
               resourceType != null && !resourceType.trim().isEmpty() &&
               totalDurationSeconds > 0 &&
               startTime != null;
    }
}