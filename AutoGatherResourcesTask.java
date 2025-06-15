package newgame;

import javax.swing.SwingWorker;
import java.awt.AWTException;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * HIBERNATING AutoGatherResourcesTask - Closes instance while waiting, reopens when needed
 * FIXED: Applied hibernation to your working version with proper flow and real times
 */
public class AutoGatherResourcesTask extends SwingWorker<Void, String> {
    private final MemuInstance instance;
    private volatile boolean shouldStop = false;
    private AutoGatherModule.AutoGatherSettings gatherSettings;
    
    private String lastExtractedTime = "02:30:00";
    private List<MarchDeployInfo> deployedMarches = new ArrayList<>();
    
    // Helper components
    private MarchViewNavigator navigator;
    private MarchDetailsCollector detailsCollector;
    private ResourceGatheringController gatheringController;
    
    // Hibernation settings
    private static final long WAKE_UP_BUFFER_SECONDS = 120; // Wake up 2 minutes before completion
    private static final long MIN_HIBERNATION_TIME = 300;   // Only hibernate if sleep > 5 minutes
    private boolean hibernationEnabled = true;
    private LocalDateTime hibernationStartTime;
    private long hibernationDurationSeconds;

    public AutoGatherResourcesTask(MemuInstance instance) throws AWTException {
        this.instance = instance;
        this.navigator = new MarchViewNavigator(instance);
        this.detailsCollector = new MarchDetailsCollector(instance);
        this.gatheringController = new ResourceGatheringController(instance);
        loadGatherSettings();
    }
    
    private void loadGatherSettings() {
        Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new java.util.HashMap<>());
        ModuleState<?> gatherModule = modules.get("Auto Gather Resources");
        
        if (gatherModule != null && gatherModule.settings != null) {
            gatherSettings = AutoGatherModule.AutoGatherSettings.fromString(gatherModule.settings.toString());
            System.out.println("✅ Loaded Auto Gather settings for instance " + instance.index + ": " + gatherSettings.toString());
        } else {
            gatherSettings = new AutoGatherModule.AutoGatherSettings();
            System.out.println("⚠️ No Auto Gather settings found, using defaults for instance " + instance.index);
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        try {
            instance.setAutoGatherRunning(true);
            instance.setState("Starting hibernating auto gather...");
            
            Main.addToConsole("🔄 Hibernating Auto Gather started for " + instance.name);
            System.out.println("🔄 Starting Hibernating AutoGatherResourcesTask for instance " + instance.index);
            
            // MAIN HIBERNATION LOOP
            boolean initialDeploymentDone = false;
            
            while (!shouldStop && !isCancelled()) {
                try {
                    if (!initialDeploymentDone) {
                        // PHASE 1: Deploy marches, collect details, then hibernate
                        if (performFullDeploymentCycleAndHibernate()) {
                            initialDeploymentDone = true;
                            Main.addToConsole("😴 " + instance.name + " entered hibernation with real times");
                        } else {
                            Main.addToConsole("⚠️ " + instance.name + " deployment failed, retrying in 30s");
                            Thread.sleep(30000);
                            continue;
                        }
                    } else {
                        // PHASE 2: Hibernation monitoring and wake-up
                        if (monitorHibernationAndWakeup()) {
                            Main.addToConsole("🌅 " + instance.name + " waking up for new deployment");
                            // Reset to deploy new marches
                            initialDeploymentDone = false;
                        } else {
                            // Still hibernating - update status with countdown
                            updateHibernationStatus();
                            Thread.sleep(30000); // Check every 30 seconds
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Main.addToConsole("🛑 " + instance.name + " hibernating auto gather interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("Error in hibernating gather loop: " + e.getMessage());
                    publish("❌ Error: " + e.getMessage());
                    Thread.sleep(10000);
                }
            }
            
        } finally {
            // Ensure instance is running when we exit
            if (!BotUtils.isInstanceRunning(instance.index)) {
                wakeUpInstance();
            }
            
            instance.setAutoGatherRunning(false);
            instance.setState("Hibernating auto gather stopped");
            Main.addToConsole("🛑 " + instance.name + " hibernating auto gather stopped");
        }
        
        return null;
    }
    
    /**
     * FIXED: Complete deployment cycle - Deploy → Wait → Collect Details → Add to GUI → Hibernate
     */
    private boolean performFullDeploymentCycleAndHibernate() {
        try {
            // STEP 1: Ensure instance is running for deployment
            if (!ensureInstanceRunning()) {
                return false;
            }
            
            // STEP 2: Deploy marches (but don't add to GUI yet)
            publish("🚀 Deploying marches...");
            if (!deployMarchesForHibernation()) {
                return false;
            }
            
            // STEP 3: Wait for deployment to complete (longest deploy time)
            long waitTime = calculateDeploymentWaitTime();
            if (waitTime > 0) {
                publish("⏳ Waiting " + TimeUtils.formatTime(waitTime) + " for deployments to complete...");
                Main.addToConsole("⏳ " + instance.name + " waiting " + TimeUtils.formatTime(waitTime) + " for deployments");
                Thread.sleep(waitTime * 1000);
            }
            
            // STEP 4: Collect real times from detail pages
            publish("📊 Collecting real march times...");
            if (!collectRealMarchDetailsAndAddToGUI()) {
                Main.addToConsole("⚠️ " + instance.name + " failed to collect real times, using estimates");
                // Add estimated times as fallback
                addEstimatedTimesToGUI();
            }
            
            // STEP 5: Calculate hibernation time from real data
            long hibernationTime = calculateHibernationTimeFromTracker();
            
            // STEP 6: Hibernate if time is sufficient
            if (hibernationTime > MIN_HIBERNATION_TIME) {
                Main.addToConsole("💤 " + instance.name + " hibernating for " + TimeUtils.formatTime(hibernationTime) + " (real times)");
                hibernateInstance(hibernationTime);
                return true;
            } else {
                Main.addToConsole("⏰ " + instance.name + " hibernation time too short (" + 
                                TimeUtils.formatTime(hibernationTime) + "), staying awake");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in full deployment cycle: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deploy marches without adding to GUI (that comes later)
     */
    private boolean deployMarchesForHibernation() {
        try {
            // Setup march view
            if (!navigator.setupMarchView()) {
                publish("❌ Failed to setup march view");
                return false;
            }
            
            // Read queue statuses
            List<MarchDetector.MarchInfo> queues = MarchDetector.readMarchQueues(instance.index);
            if (queues.isEmpty()) {
                publish("⚠️ No queues detected");
                return false;
            }
            
            // Find idle queues and deploy
            List<Integer> idleQueues = findIdleQueues(queues);
            int activeQueues = countActiveQueues(queues);
            
            if (idleQueues.isEmpty() || activeQueues >= gatherSettings.maxQueues) {
                Main.addToConsole("ℹ️ " + instance.name + " no deployment needed");
                return true;
            }
            
            int slotsAvailable = gatherSettings.maxQueues - activeQueues;
            int marchesToStart = Math.min(slotsAvailable, idleQueues.size());
            
            Main.addToConsole("🚀 " + instance.name + " deploying " + marchesToStart + " march(es)");
            
            int successCount = 0;
            boolean isFirstMarch = true;
            deployedMarches.clear();
            
            // Deploy all marches
            for (int i = 0; i < marchesToStart; i++) {
                String resourceType = gatherSettings.getNextResource();
                int queueNumber = idleQueues.get(i);
                
                LocalDateTime deployStartTime = LocalDateTime.now();
                boolean marchSuccess;
                
                if (isFirstMarch) {
                    marchSuccess = gatheringController.startFirstMarchFast(resourceType, queueNumber);
                    isFirstMarch = false;
                } else {
                    marchSuccess = gatheringController.startSubsequentMarchFast(resourceType, queueNumber);
                }
                
                if (marchSuccess) {
                    successCount++;
                    String deployTime = gatheringController.getLastExtractedTime();
                    if (deployTime == null) deployTime = lastExtractedTime;
                    
                    MarchDeployInfo deployInfo = new MarchDeployInfo(
                        queueNumber, resourceType, deployStartTime, deployTime
                    );
                    deployedMarches.add(deployInfo);
                    
                    Main.addToConsole("✅ " + instance.name + " deployed " + resourceType + 
                                    " on Queue " + queueNumber + " (deploy time: " + deployTime + ")");
                }
            }
            
            return successCount > 0;
            
        } catch (Exception e) {
            System.err.println("❌ Error deploying marches: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Collect real march details and add to GUI with real times
     */
    private boolean collectRealMarchDetailsAndAddToGUI() {
        try {
            Main.addToConsole("📊 " + instance.name + " collecting real march times from " + deployedMarches.size() + " deployments");
            
            boolean anySuccess = false;
            
            for (int i = 0; i < deployedMarches.size(); i++) {
                MarchDeployInfo deployInfo = deployedMarches.get(i);
                
                try {
                    publish("📊 Collecting details for Queue " + deployInfo.queueNumber + " (" + (i+1) + "/" + deployedMarches.size() + ")");
                    
                    // Collect real details using your working details collector
                    MarchDetailsCollector.MarchDetails details = collectDetailsForQueue(deployInfo);
                    
                    if (details != null && details.gatheringTime != null && !details.gatheringTime.equals("00:00:00")) {
                        // SUCCESS: Add to GUI with real times
                        addToMarchTrackerWithRealTimes(deployInfo, details);
                        anySuccess = true;
                        
                        Main.addToConsole("✅ " + instance.name + " Queue " + deployInfo.queueNumber + 
                                        " real times - Gathering: " + details.gatheringTime + 
                                        ", Total: " + details.totalTime);
                    } else {
                        // FAILED: Add with estimated times
                        Main.addToConsole("⚠️ " + instance.name + " Queue " + deployInfo.queueNumber + 
                                        " failed to get real times, using estimates");
                        addToMarchTrackerEstimated(deployInfo);
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ Error collecting details for Queue " + deployInfo.queueNumber + ": " + e.getMessage());
                    // Fallback to estimated times
                    addToMarchTrackerEstimated(deployInfo);
                }
            }
            
            return anySuccess;
            
        } catch (Exception e) {
            System.err.println("❌ Error in detail collection: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Collect details for a single queue using your existing MarchDetailsCollector
     */
    private MarchDetailsCollector.MarchDetails collectDetailsForQueue(MarchDeployInfo deployInfo) {
        try {
            // Setup march view before collecting details
            if (!navigator.setupMarchView()) {
                System.err.println("❌ Failed to setup march view for Queue " + deployInfo.queueNumber);
                return null;
            }
            
            // Use your existing details collector to get real gathering time
            if (detailsCollector.collectMarchDetailsFromAllDeployedMarches(List.of(deployInfo))) {
                // Calculate total time using the real gathering time
                String totalTime = detailsCollector.calculateTotalTimeFixed(deployInfo.estimatedDeployDuration, deployInfo.actualGatheringTime);
                
                // Create details object with real times
                MarchDetailsCollector.MarchDetails details = new MarchDetailsCollector.MarchDetails();
                details.gatheringTime = deployInfo.actualGatheringTime;
                details.marchingTime = deployInfo.estimatedDeployDuration;
                details.totalTime = totalTime;
                
                return details;
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error collecting details for queue: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Add march to tracker with real times from detail page
     */
    private void addToMarchTrackerWithRealTimes(MarchDeployInfo deployInfo, MarchDetailsCollector.MarchDetails details) {
        try {
            MarchTrackerGUI.getInstance().addMarch(
                instance.index,
                deployInfo.queueNumber,
                deployInfo.resourceType,
                details.gatheringTime,
                details.marchingTime,
                details.totalTime
            );
            
            System.out.println("✅ [REAL TIMES] Added to tracker: Queue " + deployInfo.queueNumber + 
                             ", " + deployInfo.resourceType + 
                             ", Gathering: " + details.gatheringTime + 
                             ", Total: " + details.totalTime);
            
        } catch (Exception e) {
            System.err.println("❌ Error adding real times to tracker: " + e.getMessage());
        }
    }
    
    /**
     * Fallback: Add estimated times to GUI
     */
    private void addEstimatedTimesToGUI() {
        for (MarchDeployInfo deployInfo : deployedMarches) {
            addToMarchTrackerEstimated(deployInfo);
        }
    }
    
    /**
     * Calculate hibernation time from march tracker real data
     */
    private long calculateHibernationTimeFromTracker() {
        try {
            List<ActiveMarch> activeMarches = MarchTrackerGUI.getInstance().getActiveMarches();
            List<ActiveMarch> instanceMarches = new ArrayList<>();
            
            for (ActiveMarch march : activeMarches) {
                if (march.getInstanceIndex() == instance.index) {
                    instanceMarches.add(march);
                }
            }
            
            if (instanceMarches.isEmpty()) {
                return 0;
            }
            
            // Find the longest total time
            long maxTotalSeconds = 0;
            for (ActiveMarch march : instanceMarches) {
                long totalSeconds = TimeUtils.parseTimeToSeconds(march.getTotalTime());
                if (totalSeconds > maxTotalSeconds) {
                    maxTotalSeconds = totalSeconds;
                }
            }
            
            // Subtract buffer time for wake-up
            return Math.max(0, maxTotalSeconds - WAKE_UP_BUFFER_SECONDS);
            
        } catch (Exception e) {
            System.err.println("❌ Error calculating hibernation time from tracker: " + e.getMessage());
            // Fallback to deployment-based calculation
            return calculateDeploymentBasedHibernation();
        }
    }
    
    /**
     * Fallback hibernation calculation
     */
    private long calculateDeploymentBasedHibernation() {
        if (deployedMarches.isEmpty()) {
            return 0;
        }
        
        long maxCompletionTime = 0;
        
        for (MarchDeployInfo march : deployedMarches) {
            long deploySeconds = TimeUtils.parseTimeToSeconds(march.estimatedDeployDuration);
            long estimatedGatheringSeconds = deploySeconds * 2; // Rough estimate
            long totalSeconds = deploySeconds + estimatedGatheringSeconds;
            
            if (totalSeconds > maxCompletionTime) {
                maxCompletionTime = totalSeconds;
            }
        }
        
        return Math.max(0, maxCompletionTime - WAKE_UP_BUFFER_SECONDS);
    }
    
    /**
     * Calculate wait time before collecting details
     */
    private long calculateDeploymentWaitTime() {
        if (deployedMarches.isEmpty()) {
            return 0;
        }
        
        long maxDeployTime = 0;
        
        for (MarchDeployInfo march : deployedMarches) {
            long deploySeconds = TimeUtils.parseTimeToSeconds(march.estimatedDeployDuration);
            if (deploySeconds > maxDeployTime) {
                maxDeployTime = deploySeconds;
            }
        }
        
        return maxDeployTime;
    }
    
    /**
     * Monitor hibernation and wake up when marches are about to complete
     */
    private boolean monitorHibernationAndWakeup() {
        try {
            // Check if hibernation time is up
            if (hibernationStartTime != null && hibernationDurationSeconds > 0) {
                long elapsedHibernation = java.time.Duration.between(hibernationStartTime, LocalDateTime.now()).getSeconds();
                long remainingHibernation = hibernationDurationSeconds - elapsedHibernation;
                
                if (remainingHibernation <= 0) {
                    // Time to wake up
                    Main.addToConsole("🌅 " + instance.name + " hibernation time complete, waking up");
                    wakeUpInstance();
                    hibernationStartTime = null;
                    hibernationDurationSeconds = 0;
                    return true;
                }
                
                // Update hibernation status
                updateHibernationStatus();
                return false;
            }
            
            // Fallback: Check march completion times from tracker
            List<ActiveMarch> activeMarches = MarchTrackerGUI.getInstance().getActiveMarches();
            List<ActiveMarch> instanceMarches = new ArrayList<>();
            
            for (ActiveMarch march : activeMarches) {
                if (march.getInstanceIndex() == instance.index) {
                    instanceMarches.add(march);
                }
            }
            
            if (instanceMarches.isEmpty()) {
                // No active marches - wake up and deploy new ones
                Main.addToConsole("🌅 " + instance.name + " no active marches, waking up");
                wakeUpInstance();
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error monitoring hibernation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Update hibernation status with countdown in GUI
     */
    private void updateHibernationStatus() {
        try {
            if (hibernationStartTime != null && hibernationDurationSeconds > 0) {
                long elapsedHibernation = java.time.Duration.between(hibernationStartTime, LocalDateTime.now()).getSeconds();
                long remainingHibernation = hibernationDurationSeconds - elapsedHibernation;
                
                if (remainingHibernation > 0) {
                    String remainingTime = TimeUtils.formatTime(remainingHibernation);
                    String status = "😴 Hibernating - Wake in " + remainingTime;
                    
                    // FIXED: Force update the instance state and mark as hibernating
                    instance.setState(status);
                    instance.setAutoGatherRunning(true); // Keep this true during hibernation
                    publish(status);
                    
                    // FIXED: Also log for debugging
                    System.out.println("🔄 [HIBERNATION] " + instance.name + " status: " + status);
                } else {
                    System.out.println("🔄 [HIBERNATION] " + instance.name + " hibernation time expired");
                }
            } else {
                System.out.println("🔄 [HIBERNATION] " + instance.name + " no hibernation timing available");
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating hibernation status: " + e.getMessage());
        }
    }
    
    /**
     * FIXED: Hibernate the instance (stop it) and track hibernation time
     */
    private void hibernateInstance(long hibernationSeconds) {
        try {
            if (BotUtils.isInstanceRunning(instance.index)) {
                Main.addToConsole("😴 " + instance.name + " entering hibernation (stopping instance)");
                
                // Store hibernation timing
                hibernationStartTime = LocalDateTime.now();
                hibernationDurationSeconds = hibernationSeconds;
                
                // FIXED: Set hibernation status IMMEDIATELY before stopping
                String remainingTime = TimeUtils.formatTime(hibernationSeconds);
                String hibernationStatus = "😴 Hibernating - Wake in " + remainingTime;
                instance.setState(hibernationStatus);
                publish(hibernationStatus);
                
                System.out.println("🔄 [HIBERNATION] " + instance.name + " set initial status: " + hibernationStatus);
                
                // Stop the instance
                MemuActions.stopInstance(null, instance.index, () -> {
                    Main.addToConsole("💤 " + instance.name + " hibernation complete");
                    
                    // FIXED: Maintain hibernation status even after instance stops
                    String currentRemaining = TimeUtils.formatTime(hibernationDurationSeconds);
                    String currentStatus = "😴 Hibernating - Wake in " + currentRemaining;
                    instance.setState(currentStatus);
                    System.out.println("🔄 [HIBERNATION] " + instance.name + " maintained status after stop: " + currentStatus);
                });
                
                // Wait for stop to complete
                Thread.sleep(5000);
                
                // FIXED: Ensure hibernation status is still set after stop
                String finalStatus = "😴 Hibernating - Wake in " + TimeUtils.formatTime(hibernationSeconds);
                instance.setState(finalStatus);
                System.out.println("🔄 [HIBERNATION] " + instance.name + " final status set: " + finalStatus);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error hibernating instance: " + e.getMessage());
        }
    }
    
    /**
     * Wake up the instance (start it)
     */
    private void wakeUpInstance() {
        try {
            if (!BotUtils.isInstanceRunning(instance.index)) {
                Main.addToConsole("🌅 " + instance.name + " waking up (starting instance)");
                
                instance.setState("🌅 Waking up...");
                
                // Start the instance
                MemuActions.startInstance(null, instance.index, () -> {
                    Main.addToConsole("☀️ " + instance.name + " wake up complete");
                });
                
                // Wait for startup
                Thread.sleep(10000);
                
                instance.setState("☀️ Awake - Ready for deployment");
                
                // Clear hibernation tracking
                hibernationStartTime = null;
                hibernationDurationSeconds = 0;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error waking up instance: " + e.getMessage());
        }
    }
    
    /**
     * Ensure instance is running, start if needed
     */
    private boolean ensureInstanceRunning() {
        try {
            if (!BotUtils.isInstanceRunning(instance.index)) {
                Main.addToConsole("🔧 " + instance.name + " starting instance for deployment");
                wakeUpInstance();
                
                // Verify it started
                for (int i = 0; i < 10; i++) {
                    if (BotUtils.isInstanceRunning(instance.index)) {
                        return true;
                    }
                    Thread.sleep(2000);
                }
                
                System.err.println("❌ Failed to start instance " + instance.index);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error ensuring instance running: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Add march to tracker with estimated times (fallback only)
     */
    private void addToMarchTrackerEstimated(MarchDeployInfo deployInfo) {
        try {
            // Use estimated times since we're hibernating during details collection
            long deploySeconds = TimeUtils.parseTimeToSeconds(deployInfo.estimatedDeployDuration);
            long estimatedGatheringSeconds = deploySeconds * 2; // Rough 2:1 ratio
            long estimatedMarchingSeconds = deploySeconds;
            
            String estimatedGatheringTime = TimeUtils.formatTime(estimatedGatheringSeconds);
            String estimatedMarchingTime = TimeUtils.formatTime(estimatedMarchingSeconds);
            String estimatedTotalTime = TimeUtils.formatTime(estimatedGatheringSeconds + estimatedMarchingSeconds);
            
            MarchTrackerGUI.getInstance().addMarch(
                instance.index,
                deployInfo.queueNumber,
                deployInfo.resourceType,
                estimatedGatheringTime,
                estimatedMarchingTime,
                estimatedTotalTime
            );
            
            System.out.println("⚠️ [ESTIMATED] Added to tracker: Queue " + deployInfo.queueNumber + 
                             ", " + deployInfo.resourceType + ", Total: " + estimatedTotalTime);
            
        } catch (Exception e) {
            System.err.println("❌ Error adding estimated times to tracker: " + e.getMessage());
        }
    }
    
    // Legacy methods for compatibility
    private List<Integer> findIdleQueues(List<MarchDetector.MarchInfo> queues) {
        List<Integer> idleQueues = new ArrayList<>();
        for (MarchDetector.MarchInfo queue : queues) {
            if (queue.status == MarchDetector.MarchStatus.IDLE) {
                idleQueues.add(queue.queueNumber);
            }
        }
        return idleQueues;
    }
    
    private int countActiveQueues(List<MarchDetector.MarchInfo> queues) {
        int count = 0;
        for (MarchDetector.MarchInfo queue : queues) {
            if (queue.status == MarchDetector.MarchStatus.GATHERING) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    protected void process(List<String> chunks) {
        if (!chunks.isEmpty()) {
            String latestMessage = chunks.get(chunks.size() - 1);
            instance.setState(latestMessage);
        }
    }
    
    @Override
    protected void done() {
        try {
            get();
            Main.addToConsole("✅ " + instance.name + " hibernating auto gather completed");
        } catch (Exception e) {
            Main.addToConsole("❌ " + instance.name + " hibernating auto gather failed");
        }
    }
    
    public void stopGathering() {
        shouldStop = true;
        cancel(true);
        
        // Wake up instance before stopping
        if (!BotUtils.isInstanceRunning(instance.index)) {
            wakeUpInstance();
        }
        
        Main.addToConsole("🛑 " + instance.name + " hibernating auto gather stop requested");
    }
    
    /**
     * Enable/disable hibernation mode
     */
    public void setHibernationEnabled(boolean enabled) {
        this.hibernationEnabled = enabled;
        Main.addToConsole("💤 " + instance.name + " hibernation mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }
}

/**
 * Simple inner class for MarchDetails since the actual one might have compatibility issues
 */
class MarchDetails {
    public String gatheringTime;
    public String marchingTime; 
    public String totalTime;
}