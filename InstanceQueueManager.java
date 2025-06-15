package newgame;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages instance queue and concurrent execution limits
 */
public class InstanceQueueManager {
    
    public enum InstanceStatus {
        STOPPED,
        RUNNING,
        HIBERNATING,
        QUEUED
    }
    
    public enum Priority {
        HIGH,
        NORMAL,
        LOW
    }
    
    public static class QueueStatus {
        public int runningCount;
        public int hibernatingCount;
        public int queuedCount;
        public String nextSlotETA;
        
        public QueueStatus(int running, int hibernating, int queued, String eta) {
            this.runningCount = running;
            this.hibernatingCount = hibernating;
            this.queuedCount = queued;
            this.nextSlotETA = eta;
        }
    }
    
    private final SystemSettings systemSettings;
    private final Map<Integer, InstanceStatus> instanceStatuses = new ConcurrentHashMap<>();
    private final Map<Integer, Priority> instancePriorities = new ConcurrentHashMap<>();
    private final LinkedList<Integer> queuedInstances = new LinkedList<>();
    private final Set<Integer> runningInstances = new HashSet<>();
    
    public InstanceQueueManager(SystemSettings settings) {
        this.systemSettings = settings;
    }
    
    public void updateSettings(SystemSettings newSettings) {
        // Update settings and rebalance queue if needed
        if (newSettings.maxConcurrentInstances < systemSettings.maxConcurrentInstances) {
            rebalanceQueue();
        }
    }
    
    public void registerInstance(int instanceIndex, Priority priority) {
        instancePriorities.put(instanceIndex, priority);
        if (!instanceStatuses.containsKey(instanceIndex)) {
            instanceStatuses.put(instanceIndex, InstanceStatus.STOPPED);
        }
    }
    
    public boolean requestInstanceStart(int instanceIndex) {
        if (runningInstances.size() < systemSettings.maxConcurrentInstances) {
            // Slot available - start immediately
            startInstanceImmediately(instanceIndex);
            return true;
        } else {
            // Queue the instance
            addToQueue(instanceIndex);
            return false;
        }
    }
    
    public void updateInstanceStatus(int instanceIndex, InstanceStatus status) {
        InstanceStatus oldStatus = instanceStatuses.get(instanceIndex);
        instanceStatuses.put(instanceIndex, status);
        
        if (oldStatus == InstanceStatus.RUNNING && status != InstanceStatus.RUNNING) {
            // Instance stopped running - free up slot
            runningInstances.remove(instanceIndex);
            promoteFromQueue();
        } else if (status == InstanceStatus.HIBERNATING && oldStatus == InstanceStatus.RUNNING) {
            // Instance hibernated - free up slot but keep tracking
            runningInstances.remove(instanceIndex);
            promoteFromQueue();
        } else if (status == InstanceStatus.RUNNING) {
            runningInstances.add(instanceIndex);
        }
    }
    
    public InstanceStatus getInstanceStatus(int instanceIndex) {
        return instanceStatuses.getOrDefault(instanceIndex, InstanceStatus.STOPPED);
    }
    
    public Priority getInstancePriority(int instanceIndex) {
        return instancePriorities.getOrDefault(instanceIndex, Priority.NORMAL);
    }
    
    public int getQueuePosition(int instanceIndex) {
        int position = queuedInstances.indexOf(instanceIndex);
        return position >= 0 ? position + 1 : 0;
    }
    
    public QueueStatus getQueueStatus() {
        int running = runningInstances.size();
        int hibernating = 0;
        int queued = queuedInstances.size();
        
        for (InstanceStatus status : instanceStatuses.values()) {
            if (status == InstanceStatus.HIBERNATING) {
                hibernating++;
            }
        }
        
        String nextETA = "";
        if (queued > 0 && running >= systemSettings.maxConcurrentInstances) {
            nextETA = "Waiting for slot";
        } else if (queued > 0) {
            nextETA = "Available";
        }
        
        return new QueueStatus(running, hibernating, queued, nextETA);
    }
    
    private void startInstanceImmediately(int instanceIndex) {
        runningInstances.add(instanceIndex);
        instanceStatuses.put(instanceIndex, InstanceStatus.RUNNING);
        
        // Trigger actual instance start through Main
        SwingUtilities.invokeLater(() -> {
            Main mainInstance = Main.getInstance();
            if (mainInstance != null) {
                mainInstance.startInstance(instanceIndex);
            }
        });
    }
    
    private void addToQueue(int instanceIndex) {
        if (!queuedInstances.contains(instanceIndex)) {
            instanceStatuses.put(instanceIndex, InstanceStatus.QUEUED);
            
            // Insert based on priority
            Priority priority = getInstancePriority(instanceIndex);
            if (priority == Priority.HIGH) {
                // Add to front of queue
                queuedInstances.addFirst(instanceIndex);
            } else if (priority == Priority.LOW) {
                // Add to end of queue
                queuedInstances.addLast(instanceIndex);
            } else {
                // Add after other normal priority instances
                int insertIndex = queuedInstances.size();
                for (int i = 0; i < queuedInstances.size(); i++) {
                    if (getInstancePriority(queuedInstances.get(i)) == Priority.LOW) {
                        insertIndex = i;
                        break;
                    }
                }
                queuedInstances.add(insertIndex, instanceIndex);
            }
            
            Main.addToConsole("⏳ Instance " + instanceIndex + " queued (Position #" + getQueuePosition(instanceIndex) + ")");
        }
    }
    
    private void promoteFromQueue() {
        if (!queuedInstances.isEmpty() && runningInstances.size() < systemSettings.maxConcurrentInstances) {
            Integer nextInstance = queuedInstances.removeFirst();
            if (nextInstance != null) {
                Main.addToConsole("🚀 Promoting Instance " + nextInstance + " from queue");
                startInstanceImmediately(nextInstance);
            }
        }
    }
    
    private void rebalanceQueue() {
        // If max concurrent reduced, might need to stop some instances
        while (runningInstances.size() > systemSettings.maxConcurrentInstances) {
            // Find lowest priority running instance to queue
            Integer instanceToQueue = findLowestPriorityRunningInstance();
            if (instanceToQueue != null) {
                addToQueue(instanceToQueue);
                runningInstances.remove(instanceToQueue);
                // Don't actually stop it - let user decide
            } else {
                break;
            }
        }
    }
    
    private Integer findLowestPriorityRunningInstance() {
        Integer lowestPriorityInstance = null;
        Priority lowestPriority = Priority.HIGH;
        
        for (Integer instanceIndex : runningInstances) {
            Priority priority = getInstancePriority(instanceIndex);
            if (priority.ordinal() > lowestPriority.ordinal()) {
                lowestPriority = priority;
                lowestPriorityInstance = instanceIndex;
            }
        }
        
        return lowestPriorityInstance;
    }
}