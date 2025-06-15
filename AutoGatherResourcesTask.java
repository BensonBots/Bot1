package newgame;

import javax.swing.SwingUtilities;
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
 * CLEANED: AutoGatherResourcesTask with simplified hibernation status updates and smart hibernation logic
 * Removed complex GUI forcing code and streamlined status management
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
    
    // Smart hibernation settings
    private static final long COMPLETION_SPREAD_THRESHOLD = 600; // 10 minutes in seconds
    private static final long MIN_RESTART_INTERVAL = 300;       // 5 minutes minimum between restarts
    private static final long WAKE_UP_BUFFER_SECONDS = 120;     // 2 minutes before completion
    private static final long MIN_HIBERNATION_TIME = 300;       // Only hibernate if sleep > 5 minutes
    
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
    
    /**
     * SIMPLIFIED: Update hibernation status with countdown
     */
    private void updateHibernationStatus() {
        try {
            if (hibernationStartTime != null && hibernationDurationSeconds > 0) {
                long elapsedHibernation = java.time.Duration.between(hibernationStartTime, LocalDateTime.now()).getSeconds();
                long remainingHibernation = hibernationDurationSeconds - elapsedHibernation;
                
                if (remainingHibernation > 0) {
                    String remainingTime = TimeUtils.formatTime(remainingHibernation);
                    String status = "😴 Hibernating - Wake in " + remainingTime;
                    updateInstanceStatus(status);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating hibernation status: " + e.getMessage());
        }
    }
    
    /**
     * Hibernate the instance and track timing
     */
    private void hibernateInstance(long hibernationSeconds) {
        try {
            if (BotUtils.isInstanceRunning(instance.index)) {
                Main.addToConsole("😴 " + instance.name + " entering hibernation (stopping instance)");
                
                // Store hibernation timing
                hibernationStartTime = LocalDateTime.now();
                hibernationDurationSeconds = hibernationSeconds;
                
                // Set hibernation status
                String remainingTime = TimeUtils.formatTime(hibernationSeconds);
                String hibernationStatus = "😴 Hibernating - Wake in " + remainingTime;
                updateInstanceStatus(hibernationStatus);
                
                // Stop the instance
                MemuActions.stopInstance(null, instance.index, () -> {
                    Main.addToConsole("💤 " + instance.name + " hibernation complete");
                });
                
                Thread.sleep(5000); // Wait for stop to complete
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error hibernating instance: " + e.getMessage());
        }
    }
    
    /**
     * Wake up the instance
     */
    private void wakeUpInstance() {
        try {
            if (!BotUtils.isInstanceRunning(instance.index)) {
                Main.addToConsole("🌅 " + instance.name + " waking up (starting instance)");
                
                updateInstanceStatus("🌅 Waking up...");
                
                // Start the instance
                MemuActions.startInstance(null, instance.index, () -> {
                    Main.addToConsole("☀️ " + instance.name + " wake up complete");
                });
                
                Thread.sleep(10000); // Wait for startup
                
                updateInstanceStatus("☀️ Awake - Ready for deployment");
                
                // Clear hibernation tracking
                hibernationStartTime = null;
                hibernationDurationSeconds = 0;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error waking up instance: " + e.getMessage());
        }
    }
    
    /**
     * Ensure instance is running
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
    
    // Helper methods
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
    
    public void setHibernationEnabled(boolean enabled) {
        this.hibernationEnabled = enabled;
        Main.addToConsole("💤 " + instance.name + " hibernation mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }
 

    private void updateInstanceStatus(String status) {
        try {
            instance.setState(status);
            
            // Simple status update through Main's clean method
            SwingUtilities.invokeLater(() -> {
                Main mainInstance = Main.getInstance();
                if (mainInstance != null) {
                    mainInstance.forceUpdateInstanceStatus(instance.index, status);
                }
            });
            
            System.out.println("🔄 [STATUS] " + instance.name + " status: " + status);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating instance status: " + e.getMessage());
        }
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
            updateInstanceStatus("Starting hibernating auto gather...");
            
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
                            Main.addToConsole("😴 " + instance.name + " entered hibernation with smart timing");
                        } else {
                            Main.addToConsole("⚠️ " + instance.name + " deployment failed, retrying in 30s");
                            Thread.sleep(30000);
                            continue;
                        }
                    } else {
                        // PHASE 2: Hibernation monitoring and wake-up
                        if (monitorHibernationAndWakeup()) {
                            Main.addToConsole("🌅 " + instance.name + " waking up for new deployment");
                            
                            // Run Auto Start Game when waking up from hibernation
                            if (!runAutoStartGameAfterWakeup()) {
                                Main.addToConsole("⚠️ " + instance.name + " Auto Start Game failed after wake-up, continuing anyway");
                            }
                            
                            // Reset to deploy new marches
                            initialDeploymentDone = false;
                        } else {
                            // Update hibernation countdown
                            updateHibernationStatus();
                            Thread.sleep(10000); // Check every 10 seconds
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Main.addToConsole("🛑 " + instance.name + " hibernating auto gather interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("Error in hibernating gather loop: " + e.getMessage());
                    updateInstanceStatus("❌ Error: " + e.getMessage());
                    Thread.sleep(10000);
                }
            }
            
        } finally {
            // Ensure instance is running when we exit
            if (!BotUtils.isInstanceRunning(instance.index)) {
                wakeUpInstance();
            }
            
            instance.setAutoGatherRunning(false);
            updateInstanceStatus("Hibernating auto gather stopped");
            Main.addToConsole("🛑 " + instance.name + " hibernating auto gather stopped");
        }
        
        return null;
    }
    
    /**
     * Run Auto Start Game after waking up from hibernation (if enabled)
     */
    private boolean runAutoStartGameAfterWakeup() {
        try {
            // Check if Auto Start Game is enabled for this instance
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new java.util.HashMap<>());
            ModuleState<?> autoStartModule = modules.get("Auto Start Game");
            
            boolean autoStartEnabled = autoStartModule != null && autoStartModule.enabled;
            
            if (!autoStartEnabled) {
                System.out.println("ℹ️ Auto Start Game not enabled for instance " + instance.index + " after wake-up");
                return true; // Not an error, just not enabled
            }
            
            Main.addToConsole("🎮 " + instance.name + " running Auto Start Game after hibernation wake-up");
            updateInstanceStatus("🎮 Starting game after wake-up...");
            
            // Run Auto Start Game task
            AutoStartGameTask autoStartTask = new AutoStartGameTask(instance, 10, () -> {
                Main.addToConsole("✅ " + instance.name + " game started successfully after hibernation");
            });
            
            // Execute and wait for completion
            autoStartTask.execute();
            
            // Wait for Auto Start Game to complete (with timeout)
            int waitTime = 0;
            int maxWaitTime = 120; // 2 minutes maximum wait
            
            while (!autoStartTask.isDone() && waitTime < maxWaitTime) {
                Thread.sleep(1000);
                waitTime++;
                
                if (waitTime % 10 == 0) {
                    updateInstanceStatus("🎮 Starting game... (" + waitTime + "s)");
                }
            }
            
            if (autoStartTask.isDone()) {
                Main.addToConsole("✅ " + instance.name + " Auto Start Game completed after hibernation");
                Thread.sleep(8000); // Give game time to fully load
                return true;
            } else {
                Main.addToConsole("⚠️ " + instance.name + " Auto Start Game timed out after hibernation");
                autoStartTask.cancel(true);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error running Auto Start Game after wake-up: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " Auto Start Game error after wake-up: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Complete deployment cycle - Deploy → Wait → Collect Details → Add to GUI → Hibernate
     */
    private boolean performFullDeploymentCycleAndHibernate() {
        try {
            // STEP 1: Ensure instance is running for deployment
            if (!ensureInstanceRunning()) {
                return false;
            }
            
            // STEP 2: Deploy marches
            updateInstanceStatus("🚀 Deploying marches...");
            if (!deployMarchesForHibernation()) {
                return false;
            }
            
            // STEP 3: Wait for deployment to complete
            long waitTime = calculateDeploymentWaitTime();
            if (waitTime > 0) {
                updateInstanceStatus("⏳ Waiting " + TimeUtils.formatTime(waitTime) + " for deployments to complete...");
                Main.addToConsole("⏳ " + instance.name + " waiting " + TimeUtils.formatTime(waitTime) + " for deployments");
                Thread.sleep(waitTime * 1000);
            }
            
            // STEP 4: Collect real times from detail pages
            updateInstanceStatus("📊 Collecting real march times...");
            if (!collectRealMarchDetailsAndAddToGUI()) {
                Main.addToConsole("⚠️ " + instance.name + " failed to collect real times, using estimates");
                addEstimatedTimesToGUI();
            }
            
            // STEP 5: Calculate smart hibernation time from real data
            long hibernationTime = calculateSmartHibernationTime();
            
            // STEP 6: Hibernate if time is sufficient
            if (hibernationTime > MIN_HIBERNATION_TIME) {
                Main.addToConsole("💤 " + instance.name + " hibernating for " + TimeUtils.formatTime(hibernationTime) + " (smart timing)");
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
     * SMART: Calculate hibernation time considering queue completion spread
     */
    private long calculateSmartHibernationTime() {
        try {
            List<ActiveMarch> activeMarches = MarchTrackerGUI.getInstance().getActiveMarches();
            List<ActiveMarch> instanceMarches = new ArrayList<>();
            
            // Get all marches for this instance
            for (ActiveMarch march : activeMarches) {
                if (march.getInstanceIndex() == instance.index) {
                    instanceMarches.add(march);
                }
            }
            
            if (instanceMarches.isEmpty()) {
                System.out.println("🔍 [SMART HIBERNATION] No active marches found");
                return 0;
            }
            
            // Get completion times for all marches
            List<Long> completionTimes = new ArrayList<>();
            for (ActiveMarch march : instanceMarches) {
                long totalSeconds = TimeUtils.parseTimeToSeconds(march.getTotalTime());
                completionTimes.add(totalSeconds);
            }
            
            // Sort completion times
            completionTimes.sort(Long::compareTo);
            
            long shortestTime = completionTimes.get(0);
            long longestTime = completionTimes.get(completionTimes.size() - 1);
            long spread = longestTime - shortestTime;
            
            System.out.println("🔍 [SMART HIBERNATION] Analysis:");
            System.out.println("  Shortest completion: " + TimeUtils.formatTime(shortestTime));
            System.out.println("  Longest completion: " + TimeUtils.formatTime(longestTime));
            System.out.println("  Spread: " + TimeUtils.formatTime(spread));
            System.out.println("  Threshold: " + TimeUtils.formatTime(COMPLETION_SPREAD_THRESHOLD));
            
            // STRATEGY 1: Small spread - wait for all to complete
            if (spread <= COMPLETION_SPREAD_THRESHOLD) {
                long hibernationTime = longestTime - WAKE_UP_BUFFER_SECONDS;
                System.out.println("✅ [SMART HIBERNATION] Small spread detected - hibernating until all complete");
                System.out.println("  Hibernation time: " + TimeUtils.formatTime(hibernationTime));
                return Math.max(0, hibernationTime);
            }
            
            // STRATEGY 2: Large spread - calculate optimal restart points
            return calculateOptimalRestartStrategy(completionTimes);
            
        } catch (Exception e) {
            System.err.println("❌ Error in smart hibernation calculation: " + e.getMessage());
            return calculateDeploymentBasedHibernation(); // Fallback to old logic
        }
    }
    
    /**
     * STRATEGY 2: Calculate optimal restart points for large completion spreads
     */
    private long calculateOptimalRestartStrategy(List<Long> completionTimes) {
        System.out.println("🔍 [OPTIMAL RESTART] Large spread detected - analyzing restart points");
        
        // Group completion times into clusters
        List<CompletionCluster> clusters = createCompletionClusters(completionTimes);
        
        System.out.println("🔍 [OPTIMAL RESTART] Found " + clusters.size() + " completion clusters:");
        for (int i = 0; i < clusters.size(); i++) {
            CompletionCluster cluster = clusters.get(i);
            System.out.println("  Cluster " + (i+1) + ": " + cluster.size + " queue(s) completing around " + 
                              TimeUtils.formatTime(cluster.averageTime));
        }
        
        // OPTION A: Wake up for the first significant cluster
        CompletionCluster firstCluster = clusters.get(0);
        if (firstCluster.size >= 2 || firstCluster.averageTime >= MIN_RESTART_INTERVAL) {
            long hibernationTime = firstCluster.averageTime - WAKE_UP_BUFFER_SECONDS;
            System.out.println("✅ [OPTIMAL RESTART] Strategy: Wake for first cluster");
            System.out.println("  Will restart when " + firstCluster.size + " queue(s) complete");
            System.out.println("  Hibernation time: " + TimeUtils.formatTime(hibernationTime));
            return Math.max(0, hibernationTime);
        }
        
        // OPTION B: If first cluster is too small/soon, look for next good opportunity
        for (int i = 1; i < clusters.size(); i++) {
            CompletionCluster cluster = clusters.get(i);
            long timeDifference = cluster.averageTime - firstCluster.averageTime;
            
            // If this cluster is significantly later and has multiple queues
            if (timeDifference >= MIN_RESTART_INTERVAL && cluster.size >= 2) {
                long hibernationTime = cluster.averageTime - WAKE_UP_BUFFER_SECONDS;
                System.out.println("✅ [OPTIMAL RESTART] Strategy: Skip first cluster, wake for cluster " + (i+1));
                System.out.println("  Will restart when " + cluster.size + " queue(s) complete");
                System.out.println("  Hibernation time: " + TimeUtils.formatTime(hibernationTime));
                return Math.max(0, hibernationTime);
            }
        }
        
        // OPTION C: Fallback - just wake for the first completion
        long hibernationTime = firstCluster.averageTime - WAKE_UP_BUFFER_SECONDS;
        System.out.println("⚠️ [OPTIMAL RESTART] Fallback: Wake for first completion");
        System.out.println("  Hibernation time: " + TimeUtils.formatTime(hibernationTime));
        return Math.max(0, hibernationTime);
    }
    
    /**
     * Group completion times into clusters (queues completing around the same time)
     */
    private List<CompletionCluster> createCompletionClusters(List<Long> completionTimes) {
        List<CompletionCluster> clusters = new ArrayList<>();
        long clusterTolerance = 300; // 5 minutes tolerance for same cluster
        
        for (long completionTime : completionTimes) {
            boolean addedToCluster = false;
            
            // Try to add to existing cluster
            for (CompletionCluster cluster : clusters) {
                if (Math.abs(cluster.averageTime - completionTime) <= clusterTolerance) {
                    cluster.addTime(completionTime);
                    addedToCluster = true;
                    break;
                }
            }
            
            // Create new cluster if needed
            if (!addedToCluster) {
                clusters.add(new CompletionCluster(completionTime));
            }
        }
        
        // Sort clusters by average completion time
        clusters.sort((a, b) -> Long.compare(a.averageTime, b.averageTime));
        
        return clusters;
    }
    
    /**
     * Helper class to represent a cluster of completion times
     */
    private static class CompletionCluster {
        long averageTime;
        int size;
        long totalTime;
        
        public CompletionCluster(long firstTime) {
            this.averageTime = firstTime;
            this.totalTime = firstTime;
            this.size = 1;
        }
        
        public void addTime(long time) {
            totalTime += time;
            size++;
            averageTime = totalTime / size;
        }
    }
    
    /**
     * Deploy marches based on settings and available queues
     */
    private boolean deployMarchesForHibernation() {
        try {
            // Setup march view
            if (!navigator.setupMarchView()) {
                updateInstanceStatus("❌ Failed to setup march view");
                return false;
            }
            
            // Read queue statuses
            List<MarchDetector.MarchInfo> queues = MarchDetector.readMarchQueues(instance.index);
            if (queues.isEmpty()) {
                updateInstanceStatus("⚠️ No queues detected");
                return false;
            }
            
            // Find idle queues and count active ones
            List<Integer> idleQueues = findIdleQueues(queues);
            int activeQueues = countActiveQueues(queues);
            
            // Use settings to determine how many marches to deploy
            int maxQueues = gatherSettings.maxQueues;
            int availableSlots = maxQueues - activeQueues;
            int marchesToDeploy = Math.min(availableSlots, idleQueues.size());
            
            if (marchesToDeploy <= 0) {
                Main.addToConsole("ℹ️ " + instance.name + " no deployment needed - " + activeQueues + "/" + maxQueues + " queues active");
                return true;
            }
            
            Main.addToConsole("🚀 " + instance.name + " deploying " + marchesToDeploy + " march(es) to idle queues: " + idleQueues);
            updateInstanceStatus("🚀 Deploying " + marchesToDeploy + " march(es)...");
            
            int successCount = 0;
            boolean isFirstMarch = true;
            deployedMarches.clear();
            
            // Deploy to available idle queues using resource loop
            for (int i = 0; i < marchesToDeploy; i++) {
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
                    
                    updateInstanceStatus("🚀 Deployed " + (i+1) + "/" + marchesToDeploy + " (" + resourceType + " Q" + queueNumber + ")");
                } else {
                    Main.addToConsole("❌ " + instance.name + " failed to deploy " + resourceType + " on Queue " + queueNumber);
                }
            }
            
            Main.addToConsole("📊 " + instance.name + " deployment summary: " + successCount + "/" + marchesToDeploy + " successful");
            return successCount > 0;
            
        } catch (Exception e) {
            System.err.println("❌ Error deploying marches: " + e.getMessage());
            updateInstanceStatus("❌ Deployment error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Collect real march details and add to GUI with real times
     */
    private boolean collectRealMarchDetailsAndAddToGUI() {
        try {
            Main.addToConsole("📊 " + instance.name + " collecting real march times from " + deployedMarches.size() + " deployments");
            
            boolean anySuccess = false;
            
            for (int i = 0; i < deployedMarches.size(); i++) {
                MarchDeployInfo deployInfo = deployedMarches.get(i);
                
                try {
                    updateInstanceStatus("📊 Collecting details for Queue " + deployInfo.queueNumber + " (" + (i+1) + "/" + deployedMarches.size() + ")");
                    
                    // Collect real details
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
     * Collect details for a single queue
     */
    private MarchDetailsCollector.MarchDetails collectDetailsForQueue(MarchDeployInfo deployInfo) {
        try {
            if (!navigator.setupMarchView()) {
                System.err.println("❌ Failed to setup march view for Queue " + deployInfo.queueNumber);
                return null;
            }
            
            if (detailsCollector.collectMarchDetailsFromAllDeployedMarches(List.of(deployInfo))) {
                String totalTime = detailsCollector.calculateTotalTimeFixed(deployInfo.estimatedDeployDuration, deployInfo.actualGatheringTime);
                
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
     * Add march to tracker with real times
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
     * Add estimated times to GUI (fallback)
     */
    private void addEstimatedTimesToGUI() {
        for (MarchDeployInfo deployInfo : deployedMarches) {
            addToMarchTrackerEstimated(deployInfo);
        }
    }
    
    /**
     * Add march to tracker with estimated times
     */
    private void addToMarchTrackerEstimated(MarchDeployInfo deployInfo) {
        try {
            long deploySeconds = TimeUtils.parseTimeToSeconds(deployInfo.estimatedDeployDuration);
            long estimatedGatheringSeconds = deploySeconds * 2;
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
            long estimatedGatheringSeconds = deploySeconds * 2;
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
}