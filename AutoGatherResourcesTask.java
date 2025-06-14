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
 * FIXED: Proper hibernation implementation with GUI status updates
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
                        // PHASE 1: Deploy marches and hibernate
                        if (performInitialDeploymentAndHibernate()) {
                            initialDeploymentDone = true;
                            Main.addToConsole("😴 " + instance.name + " entered hibernation");
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
     * Deploy marches and then hibernate the instance
     */
    private boolean performInitialDeploymentAndHibernate() {
        try {
            publish("📋 Deploying marches before hibernation...");
            
            // Ensure instance is running for deployment
            if (!ensureInstanceRunning()) {
                return false;
            }
            
            // Step 1: Setup march view
            if (!navigator.setupMarchView()) {
                publish("❌ Failed to setup march view");
                return false;
            }
            
            // Step 2: Read queue statuses
            List<MarchDetector.MarchInfo> queues = MarchDetector.readMarchQueues(instance.index);
            if (queues.isEmpty()) {
                publish("⚠️ No queues detected");
                return false;
            }
            
            // Step 3: Find idle queues and deploy
            List<Integer> idleQueues = findIdleQueues(queues);
            int activeQueues = countActiveQueues(queues);
            
            if (!idleQueues.isEmpty() && activeQueues < gatherSettings.maxQueues) {
                deployedMarches.clear();
                
                if (deployMarchesForHibernation(idleQueues, activeQueues)) {
                    // Calculate hibernation time
                    long hibernationTime = calculateHibernationTime();
                    
                    if (hibernationTime > MIN_HIBERNATION_TIME) {
                        Main.addToConsole("💤 " + instance.name + " hibernating for " + TimeUtils.formatTime(hibernationTime));
                        
                        // FIXED: Actually hibernate the instance
                        hibernateInstance(hibernationTime);
                        return true;
                    } else {
                        Main.addToConsole("⏰ " + instance.name + " hibernation time too short (" + 
                                        TimeUtils.formatTime(hibernationTime) + "), staying awake");
                        return true;
                    }
                } else {
                    return false;
                }
            } else {
                Main.addToConsole("ℹ️ " + instance.name + " no deployment needed");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in deployment and hibernation: " + e.getMessage());
            return false;
        }
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
            List<ActiveMarch> instanceMarches = activeMarches.stream()
                .filter(march -> march.getInstanceIndex() == instance.index)
                .collect(java.util.stream.Collectors.toList());
            
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
                    instance.setState(status);
                    publish(status);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating hibernation status: " + e.getMessage());
        }
    }
    
    /**
     * Deploy marches optimized for hibernation
     */
    private boolean deployMarchesForHibernation(List<Integer> idleQueues, int currentActive) {
        try {
            int slotsAvailable = gatherSettings.maxQueues - currentActive;
            int marchesToStart = Math.min(slotsAvailable, idleQueues.size());
            
            Main.addToConsole("🚀 " + instance.name + " deploying " + marchesToStart + 
                            " march(es) for hibernation");
            
            int successCount = 0;
            boolean isFirstMarch = true;
            List<MarchDeployInfo> successfulDeployments = new ArrayList<>();
            
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
                    successfulDeployments.add(deployInfo);
                    
                    Main.addToConsole("✅ " + instance.name + " deployed " + resourceType + 
                                    " on Queue " + queueNumber + " (hibernation mode)");
                }
            }
            
            if (successCount > 0) {
                // Add to march tracker immediately (no details collection during hibernation)
                for (MarchDeployInfo deployInfo : successfulDeployments) {
                    addToMarchTrackerEstimated(deployInfo);
                }
                
                Main.addToConsole("🎉 " + instance.name + " deployed " + successCount + 
                                " marches, ready for hibernation");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error deploying marches for hibernation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculate how long to hibernate based on march completion times
     */
    private long calculateHibernationTime() {
        if (deployedMarches.isEmpty()) {
            return 0;
        }
        
        long maxCompletionTime = 0;
        
        for (MarchDeployInfo march : deployedMarches) {
            // Estimate completion time = deploy time + estimated gathering time
            long deploySeconds = TimeUtils.parseTimeToSeconds(march.estimatedDeployDuration);
            long estimatedGatheringSeconds = deploySeconds * 2; // Rough estimate
            long totalSeconds = deploySeconds + estimatedGatheringSeconds;
            
            if (totalSeconds > maxCompletionTime) {
                maxCompletionTime = totalSeconds;
            }
        }
        
        // Subtract buffer time for wake-up
        return Math.max(0, maxCompletionTime - WAKE_UP_BUFFER_SECONDS);
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
                
                // Stop the instance
                MemuActions.stopInstance(null, instance.index, () -> {
                    Main.addToConsole("💤 " + instance.name + " hibernation complete");
                });
                
                // Wait for stop to complete
                Thread.sleep(5000);
                
                // Set hibernation status
                String remainingTime = TimeUtils.formatTime(hibernationSeconds);
                instance.setState("😴 Hibernating - Wake in " + remainingTime);
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
     * Add march to tracker with estimated times (for hibernation mode)
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
            
            System.out.println("📊 Added hibernation march: Queue " + deployInfo.queueNumber + 
                             ", " + deployInfo.resourceType + ", Total: " + estimatedTotalTime);
            
        } catch (Exception e) {
            System.err.println("❌ Error adding hibernation march to tracker: " + e.getMessage());
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