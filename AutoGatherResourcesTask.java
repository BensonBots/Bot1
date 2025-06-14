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
 * FIXED AutoGatherResourcesTask - Eliminates unnecessary march panel checking after deployment
 * 
 * Key fixes:
 * 1. After successful march deployment, wait for marches to complete instead of checking panels
 * 2. Only check march panels when we need to deploy new marches
 * 3. Proper queue number handling (no conversion)
 * 4. Intelligent waiting based on march completion times
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
            instance.setState("Starting enhanced auto gather...");
            
            Main.addToConsole("🔄 Enhanced Auto Gather started for " + instance.name);
            System.out.println("🔄 Starting Enhanced AutoGatherResourcesTask for instance " + instance.index);
            
            // MAIN LOOP: Deploy marches once, then wait for completion
            boolean initialDeploymentDone = false;
            
            while (!shouldStop && !isCancelled()) {
                try {
                    if (!initialDeploymentDone) {
                        // PHASE 1: Initial deployment - check march panels and deploy
                        if (performInitialMarchDeployment()) {
                            initialDeploymentDone = true;
                            Main.addToConsole("🎉 " + instance.name + " initial march deployment complete");
                            publish("🎉 Initial deployment complete - now waiting for marches to finish");
                        } else {
                            Main.addToConsole("⚠️ " + instance.name + " initial deployment failed, retrying in 30s");
                            Thread.sleep(30000);
                            continue;
                        }
                    } else {
                        // PHASE 2: Wait for marches to complete (no march panel checking!)
                        if (waitForMarchCompletionAndRedeploy()) {
                            Main.addToConsole("🔄 " + instance.name + " marches completed, deploying new ones");
                            publish("🔄 Marches completed - deploying new batch");
                        } else {
                            Main.addToConsole("⏳ " + instance.name + " marches still active, continuing to wait");
                            publish("⏳ Waiting for active marches to complete...");
                            Thread.sleep(30000); // Wait 30 seconds before checking again
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Main.addToConsole("🛑 " + instance.name + " auto gather interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("Error in gather loop: " + e.getMessage());
                    publish("❌ Error: " + e.getMessage());
                    Thread.sleep(10000);
                }
            }
            
        } finally {
            instance.setAutoGatherRunning(false);
            instance.setState("Auto gather stopped");
            Main.addToConsole("🛑 " + instance.name + " auto gather stopped");
        }
        
        return null;
    }
    
    /**
     * FIXED: Perform initial march deployment only once
     */
    private boolean performInitialMarchDeployment() {
        try {
            publish("📋 Checking march queues for initial deployment...");
            
            // Step 1: Setup march view
            if (!navigator.setupMarchView()) {
                publish("❌ Failed to setup march view, retrying...");
                return false;
            }
            
            // Step 2: Read queue statuses ONLY for initial deployment
            List<MarchDetector.MarchInfo> queues = MarchDetector.readMarchQueues(instance.index);
            if (queues.isEmpty()) {
                publish("⚠️ No queues detected");
                return false;
            }
            
            // Step 3: Find idle queues
            List<Integer> idleQueues = findIdleQueues(queues);
            int activeQueues = countActiveQueues(queues);
            
            String status = String.format("%d active, %d idle", activeQueues, idleQueues.size());
            Main.addToConsole("📊 " + instance.name + " initial status: " + status);
            publish("📊 " + status);
            
            // Step 4: Deploy marches if we have idle queues
            if (!idleQueues.isEmpty() && activeQueues < gatherSettings.maxQueues) {
                deployedMarches.clear();
                boolean deploymentSuccess = startMarchesAndCollectDetailsWithFixedTiming(idleQueues, activeQueues);
                
                if (deploymentSuccess) {
                    Main.addToConsole("✅ " + instance.name + " initial deployment successful");
                    return true;
                } else {
                    Main.addToConsole("❌ " + instance.name + " initial deployment failed");
                    return false;
                }
            } else {
                if (idleQueues.isEmpty()) {
                    Main.addToConsole("ℹ️ " + instance.name + " no idle queues available");
                    return true; // Consider this successful - we're already gathering
                } else {
                    Main.addToConsole("ℹ️ " + instance.name + " max queues already in use");
                    return true; // Consider this successful - we're at capacity
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in initial march deployment: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Wait for march completion and redeploy when ready
     * NO MARCH PANEL CHECKING - Use march tracker data instead
     */
    private boolean waitForMarchCompletionAndRedeploy() {
        try {
            // Check if we have any active marches in the tracker
            List<ActiveMarch> activeMarches = MarchTrackerGUI.getInstance().getActiveMarches();
            
            // Filter marches for this instance
            List<ActiveMarch> instanceMarches = activeMarches.stream()
                .filter(march -> march.getInstanceIndex() == instance.index)
                .collect(java.util.stream.Collectors.toList());
            
            if (instanceMarches.isEmpty()) {
                // No active marches for this instance - time to deploy new ones
                Main.addToConsole("🔄 " + instance.name + " no active marches, ready for new deployment");
                publish("🔄 No active marches - deploying new batch");
                return performInitialMarchDeployment(); // Reuse deployment logic
            }
            
            // Check if any marches are completed
            int completedCount = 0;
            long shortestTimeRemaining = Long.MAX_VALUE;
            
            for (ActiveMarch march : instanceMarches) {
                if (march.isCompleted()) {
                    completedCount++;
                } else {
                    long timeRemaining = march.getTimeRemaining();
                    if (timeRemaining > 0 && timeRemaining < shortestTimeRemaining) {
                        shortestTimeRemaining = timeRemaining;
                    }
                }
            }
            
            if (completedCount > 0) {
                Main.addToConsole("📊 " + instance.name + " has " + completedCount + " completed marches, checking for redeployment");
                publish("📊 " + completedCount + " marches completed - checking for new deployment");
                
                // Some marches completed - check if we should deploy more
                int availableSlots = gatherSettings.maxQueues - (instanceMarches.size() - completedCount);
                if (availableSlots > 0) {
                    Main.addToConsole("🚀 " + instance.name + " has " + availableSlots + " available slots, deploying new marches");
                    return performInitialMarchDeployment();
                }
            }
            
            // Display time until next march completion
            if (shortestTimeRemaining != Long.MAX_VALUE && shortestTimeRemaining > 0) {
                String timeStr = TimeUtils.formatTime(shortestTimeRemaining);
                publish("⏳ Next march completes in: " + timeStr + " (" + instanceMarches.size() + " active)");
            } else {
                publish("⏳ Waiting for marches to complete (" + instanceMarches.size() + " active)");
            }
            
            return false; // Continue waiting
            
        } catch (Exception e) {
            System.err.println("❌ Error waiting for march completion: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deploy all marches, wait for troops to REACH resources, then collect details
     */
    private boolean startMarchesAndCollectDetailsWithFixedTiming(List<Integer> idleQueues, int currentActive) {
        try {
            int slotsAvailable = gatherSettings.maxQueues - currentActive;
            int marchesToStart = Math.min(slotsAvailable, idleQueues.size());
            
            Main.addToConsole("🚀 " + instance.name + " deploying " + marchesToStart + " march(es) then waiting for troops to reach resources");
            publish("🚀 Deploying " + marchesToStart + " march(es) with timing");
            
            int successCount = 0;
            boolean isFirstMarch = true;
            List<MarchDeployInfo> successfulDeployments = new ArrayList<>();
            
            // PHASE 1: Deploy all marches first
            for (int i = 0; i < marchesToStart; i++) {
                String resourceType = gatherSettings.getNextResource();
                int queueNumber = idleQueues.get(i);
                
                Main.addToConsole("📋 " + instance.name + " deploying " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                publish("📋 Deploying " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                
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
                    Main.addToConsole("✅ " + instance.name + " successfully deployed " + resourceType + " on Queue " + queueNumber);
                    
                    String deployTime = gatheringController.getLastExtractedTime();
                    if (deployTime == null) deployTime = lastExtractedTime;
                    
                    // FIXED: Store with exact queue number (no conversion)
                    MarchDeployInfo deployInfo = new MarchDeployInfo(
                        queueNumber,        // Use exact queue number from game
                        resourceType, 
                        deployStartTime,
                        deployTime
                    );
                    successfulDeployments.add(deployInfo);
                    System.out.println("📊 Added march to deployment list: Queue " + queueNumber + 
                                     " deployed at " + deployStartTime.toLocalTime() + 
                                     " (travel time: " + deployTime + ")");
                    
                    if (i < marchesToStart - 1) {
                        Thread.sleep(1000);
                    }
                } else {
                    Main.addToConsole("❌ " + instance.name + " failed to deploy " + resourceType + " on Queue " + queueNumber);
                    isFirstMarch = true;
                }
            }
            
            if (successCount > 0) {
                Main.addToConsole("🎉 " + instance.name + " deployment phase complete: " + successCount + "/" + marchesToStart + " successful");
                
                // PHASE 2: Wait for troops to REACH resources
                waitForTroopsToReachResources(successfulDeployments);
                
                // PHASE 3: Collect details for all deployed marches
                if (!successfulDeployments.isEmpty()) {
                    Main.addToConsole("🎯 " + instance.name + " collecting details for all " + successfulDeployments.size() + " marches");
                    publish("🎯 Collecting details for " + successfulDeployments.size() + " marches...");
                    
                    boolean detailsSuccess = detailsCollector.collectMarchDetailsFromAllDeployedMarches(successfulDeployments);
                    
                    if (detailsSuccess) {
                        // Add all marches to march tracker AFTER details collection
                        for (MarchDeployInfo deployInfo : successfulDeployments) {
                            addToMarchTracker(deployInfo);
                        }
                        
                        Main.addToConsole("✅ " + instance.name + " details collected for all marches");
                        
                        // Show march tracker window
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                MarchTrackerGUI.showTracker();
                            } catch (Exception e) {
                                System.err.println("Error showing march tracker: " + e.getMessage());
                            }
                        });
                    } else {
                        Main.addToConsole("⚠️ " + instance.name + " some details collection failed");
                        
                        // Still add to tracker with estimated times
                        for (MarchDeployInfo deployInfo : successfulDeployments) {
                            addToMarchTracker(deployInfo);
                        }
                    }
                }
                
                return true;
            } else {
                Main.addToConsole("❌ " + instance.name + " no marches deployed successfully");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in march deployment and details collection: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " deployment error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Wait for troops to reach resources with correct timing calculation
     */
    private void waitForTroopsToReachResources(List<MarchDeployInfo> deployedMarches) throws InterruptedException {
        try {
            if (deployedMarches.isEmpty()) {
                System.out.println("⚠️ No deployed marches to wait for");
                return;
            }
            
            // Find the longest deploy time
            long maxDeploySeconds = 0;
            String longestDeployTime = "00:00:00";
            
            for (MarchDeployInfo march : deployedMarches) {
                long deploySeconds = TimeUtils.parseTimeToSeconds(march.estimatedDeployDuration);
                if (deploySeconds > maxDeploySeconds) {
                    maxDeploySeconds = deploySeconds;
                    longestDeployTime = march.estimatedDeployDuration;
                }
            }
            
            System.out.println("🕐 Waiting for full deploy time:");
            System.out.println("  - Longest deploy time: " + longestDeployTime);
            System.out.println("  - Waiting " + maxDeploySeconds + " seconds");
            
            Main.addToConsole("⏳ " + instance.name + " waiting " + longestDeployTime + " for troops to reach resources");
            publish("⏳ Waiting " + longestDeployTime + " for troops to reach resources");
            
            // Wait with progress updates
            long waitInterval = 15000; // 15 second intervals
            long totalWaitTime = maxDeploySeconds * 1000;
            long currentWaitTime = 0;
            
            while (currentWaitTime < totalWaitTime && !shouldStop && !isCancelled()) {
                long thisWait = Math.min(waitInterval, totalWaitTime - currentWaitTime);
                Thread.sleep(thisWait);
                currentWaitTime += thisWait;
                
                long remainingSeconds = (totalWaitTime - currentWaitTime) / 1000;
                if (remainingSeconds > 0) {
                    publish("⏳ Troops reaching resources in " + TimeUtils.formatTime(remainingSeconds));
                } else {
                    publish("🎯 All troops have reached resources!");
                }
            }
            
            if (!shouldStop && !isCancelled()) {
                Main.addToConsole("🎯 " + instance.name + " all troops have reached resources, starting details collection");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Main.addToConsole("🛑 " + instance.name + " wait interrupted");
            throw e;
        }
    }
    
    /**
     * FIXED: Add march to tracker using exact queue numbers (no conversion)
     */
    private void addToMarchTracker(MarchDeployInfo deployInfo) {
        try {
            // Check if we have real gathering time from details collection
            if (deployInfo.actualGatheringTime != null) {
                // Use REAL times from details page
                String actualGatheringTime = deployInfo.actualGatheringTime;
                String actualTotalTime = deployInfo.calculateTotalTime();
                
                // Calculate marching time (deploy time for one-way trip)
                long totalSeconds = TimeUtils.parseTimeToSeconds(actualTotalTime);
                long gatheringSeconds = TimeUtils.parseTimeToSeconds(actualGatheringTime);
                long marchingSeconds = (totalSeconds - gatheringSeconds) / 2; // Round trip, so divide by 2
                String marchingTime = TimeUtils.formatTime(marchingSeconds);
                
                // FIXED: Use exact queue number (no conversion)
                MarchTrackerGUI.getInstance().addMarch(
                    instance.index, 
                    deployInfo.queueNumber,  // Use exact queue number from deployment
                    deployInfo.resourceType, 
                    actualGatheringTime,     // REAL gathering time from details page
                    marchingTime,            // Calculated marching time
                    actualTotalTime          // REAL total time
                );
                
                System.out.println("✅ Added to march tracker with REAL times: Queue " + deployInfo.queueNumber + 
                                 " (EXACT - no conversion), " + deployInfo.resourceType + 
                                 ", Gathering: " + actualGatheringTime +
                                 ", Total: " + actualTotalTime);
                
            } else {
                // Fallback: Use estimated times if details collection failed
                System.out.println("⚠️ No actual gathering time available for Queue " + deployInfo.queueNumber + ", using estimated times");
                
                long totalSeconds = TimeUtils.parseTimeToSeconds(deployInfo.estimatedDeployDuration);
                long marchingSeconds = totalSeconds / 2;
                long gatheringSeconds = totalSeconds - marchingSeconds;
                
                String marchingTime = TimeUtils.formatTime(marchingSeconds);
                String gatheringTime = TimeUtils.formatTime(gatheringSeconds);
                
                // FIXED: Use exact queue number (no conversion)
                MarchTrackerGUI.getInstance().addMarch(
                    instance.index, 
                    deployInfo.queueNumber,               // Use exact queue number
                    deployInfo.resourceType, 
                    gatheringTime,                        // Estimated gathering time
                    marchingTime,                         // Estimated marching time
                    deployInfo.estimatedDeployDuration    // Deploy time (fallback)
                );
                
                System.out.println("📊 Added to march tracker with ESTIMATED times: Queue " + deployInfo.queueNumber + 
                                 " (EXACT - no conversion), " + deployInfo.resourceType + 
                                 ", Deploy: " + deployInfo.estimatedDeployDuration);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error adding march to tracker: " + e.getMessage());
        }
    }
    
    /**
     * Find idle queues from march detector results
     */
    private List<Integer> findIdleQueues(List<MarchDetector.MarchInfo> queues) {
        List<Integer> idleQueues = new ArrayList<>();
        
        System.out.println("🔍 Validating detected queues against expected status:");
        for (MarchDetector.MarchInfo queue : queues) {
            System.out.println("  Queue " + queue.queueNumber + ": " + queue.status);
            
            if (queue.status == MarchDetector.MarchStatus.IDLE) {
                idleQueues.add(queue.queueNumber);
                System.out.println("  ✅ Added Queue " + queue.queueNumber + " as available");
            } else {
                System.out.println("  ❌ Queue " + queue.queueNumber + " not available: " + queue.status);
            }
        }
        
        System.out.println("📊 Found " + idleQueues.size() + " idle queues: " + idleQueues);
        return idleQueues;
    }
    
    /**
     * Count active queues from march detector results
     */
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
            Main.addToConsole("✅ " + instance.name + " enhanced auto gather completed");
        } catch (Exception e) {
            Main.addToConsole("❌ " + instance.name + " enhanced auto gather failed");
        }
    }
    
    public void stopGathering() {
        shouldStop = true;
        cancel(true);
        Main.addToConsole("🛑 " + instance.name + " enhanced auto gather stop requested");
    }
}

/* 
 * KEY FIXES MADE:
 * 
 * 1. FIXED QUEUE NUMBER MAPPING:
 *    - Removed queue conversion (3 - queueNumber)
 *    - Queue numbers now match exactly what's shown in-game
 *    - Queue 1 in-game shows as Queue 1 in GUI
 * 
 * 2. ELIMINATED UNNECESSARY MARCH PANEL CHECKING:
 *    - After initial deployment, system waits for march completion
 *    - Uses march tracker data instead of repeatedly checking march panels
 *    - Only checks march panels when deploying new marches
 *    - Significantly reduces system load and UI interactions
 * 
 * 3. INTELLIGENT WAITING LOGIC:
 *    - Monitors active marches through MarchTrackerGUI
 *    - Calculates when to deploy new marches based on completion times
 *    - Waits for march completion instead of polling march status
 * 
 * 4. IMPROVED DEPLOYMENT FLOW:
 *    - Deploy initial batch → Wait for completion → Deploy new batch
 *    - No continuous march panel checking during wait periods
 *    - Better resource usage and less game UI interference
 * 
 * EXAMPLE CORRECTED FLOW:
 * 1. Check march panels once → Deploy marches on idle queues
 * 2. Wait for marches to complete (using tracker data)
 * 3. When marches complete → Deploy new marches
 * 4. Repeat cycle without unnecessary panel checking
 */