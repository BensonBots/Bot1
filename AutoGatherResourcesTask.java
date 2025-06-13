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
 * FIXED AutoGatherResourcesTask with proper timing and march tracker integration
 * 
 * Key fixes:
 * 1. Deploy all marches first, then wait for last arrival time
 * 2. Calculate precise arrival times accounting for deployment timing  
 * 3. Collect details AFTER all troops arrive
 * 4. Add to march tracker with REAL times from details page (not deploy times)
 */
public class AutoGatherResourcesTask extends SwingWorker<Void, String> {
    private final MemuInstance instance;
    private volatile boolean shouldStop = false;
    private AutoGatherModule.AutoGatherSettings gatherSettings;
    
    // Enhanced tracking for march details collection
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
            
            while (!shouldStop && !isCancelled()) {
                try {
                    publish("📋 Checking march queues...");
                    
                    // Step 1: Setup march view
                    if (!navigator.setupMarchView()) {
                        publish("❌ Failed to setup march view, retrying in 15s...");
                        Thread.sleep(15000);
                        continue;
                    }
                    
                    // Step 2: Read queue statuses
                    List<MarchDetector.MarchInfo> queues = MarchDetector.readMarchQueues(instance.index);
                    if (queues.isEmpty()) {
                        publish("⚠️ No queues detected, retrying in 15s...");
                        Thread.sleep(15000);
                        continue;
                    }
                    
                    // Step 3: Find idle queues
                    List<Integer> idleQueues = findIdleQueues(queues);
                    int activeQueues = countActiveQueues(queues);
                    
                    String status = String.format("%d active, %d idle", activeQueues, idleQueues.size());
                    Main.addToConsole("📊 " + instance.name + " queues: " + status);
                    publish("📊 " + status);
                    
                    // Step 4: Start marches if we have idle queues
                    if (!idleQueues.isEmpty() && activeQueues < gatherSettings.maxQueues) {
                        deployedMarches.clear(); // Clear previous deployments
                        startMarchesAndCollectDetailsWithProperTiming(idleQueues, activeQueues);
                        
                        Main.addToConsole("⏳ " + instance.name + " waiting 2 minutes for next cycle...");
                        Thread.sleep(120000);
                    } else {
                        // No action needed, wait and check again
                        Thread.sleep(20000);
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
     * FIXED: Deploy all marches, wait for precise arrival times, then collect details
     * Example: Queue 1 (15:00 + 2:30) = 15:02:30, Queue 2 (15:01 + 1:00) = 15:02:00
     * Wait until 15:02:31 (last arrival + buffer) then collect all details
     */
    private void startMarchesAndCollectDetailsWithProperTiming(List<Integer> idleQueues, int currentActive) {
        try {
            int slotsAvailable = gatherSettings.maxQueues - currentActive;
            int marchesToStart = Math.min(slotsAvailable, idleQueues.size());
            
            Main.addToConsole("🚀 " + instance.name + " deploying " + marchesToStart + " march(es) then waiting for optimal timing");
            publish("🚀 Deploying " + marchesToStart + " march(es) with smart timing");
            
            int successCount = 0;
            boolean isFirstMarch = true;
            List<MarchDeployInfo> successfulDeployments = new ArrayList<>();
            
            // PHASE 1: Deploy all marches first (fast, no interruptions)
            for (int i = 0; i < marchesToStart; i++) {
                String resourceType = gatherSettings.getNextResource();
                int queueNumber = idleQueues.get(i);
                
                Main.addToConsole("📋 " + instance.name + " deploying " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                publish("📋 Deploying " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                
                // Record deployment start time BEFORE deploying
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
                    
                    // Track deployed march with EXACT deployment time
                    String deployTime = gatheringController.getLastExtractedTime();
                    if (deployTime == null) deployTime = lastExtractedTime;
                    
                    MarchDeployInfo deployInfo = new MarchDeployInfo(
                        queueNumber, 
                        resourceType, 
                        deployStartTime,  // EXACT deployment time
                        deployTime
                    );
                    successfulDeployments.add(deployInfo);
                    System.out.println("📊 Added march to deployment list: Queue " + queueNumber + 
                                     " deployed at " + deployStartTime.toLocalTime() + 
                                     " (+" + deployTime + ")");
                    
                    // Short delay between deployments
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
                
                // PHASE 2: Wait for optimal timing (when LAST march arrives)
                waitForOptimalDetailsCollectionTime(successfulDeployments);
                
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
            } else {
                Main.addToConsole("❌ " + instance.name + " no marches deployed successfully");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in march deployment and details collection: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " deployment error: " + e.getMessage());
        }
    }
    
    /**
     * FIXED: Calculate precise arrival times and wait efficiently
     * Example: Queue 1 (15:00 + 2:30) = 15:02:30, Queue 2 (15:01 + 1:00) = 15:02:00
     * Wait until 15:02:31 (last arrival + 1 second buffer)
     * @throws InterruptedException 
     */
    private void waitForOptimalDetailsCollectionTime(List<MarchDeployInfo> deployedMarches) throws InterruptedException {
        try {
            if (deployedMarches.isEmpty()) {
                System.out.println("⚠️ No deployed marches to wait for");
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime latestArrivalTime = now;
            
            System.out.println("🕐 Current time: " + now.toLocalTime());
            System.out.println("📊 Calculating precise arrival times:");
            
            // Calculate when each march will actually arrive
            for (MarchDeployInfo march : deployedMarches) {
                long deploySeconds = TimeUtils.parseTimeToSeconds(march.estimatedDeployDuration);
                
                // Get the deployment time (correct field name is deployTime)
                LocalDateTime deployTime = march.deployTime;
                LocalDateTime arrivalTime = deployTime.plusSeconds(deploySeconds);
                
                System.out.println("  Queue " + march.queueNumber + 
                                 ": deployed " + deployTime.toLocalTime() + 
                                 " → arrives " + arrivalTime.toLocalTime() + 
                                 " (+" + march.estimatedDeployDuration + ")");
                
                if (arrivalTime.isAfter(latestArrivalTime)) {
                    latestArrivalTime = arrivalTime;
                }
            }
            
            // Add 1 second buffer to ensure all troops have arrived
            latestArrivalTime = latestArrivalTime.plusSeconds(1);
            
            // Calculate how long we ACTUALLY need to wait from NOW
            long waitSeconds = java.time.Duration.between(now, latestArrivalTime).getSeconds();
            
            System.out.println("🕐 Wait calculation: now=" + now.toLocalTime() + 
                             ", latest=" + latestArrivalTime.toLocalTime() + 
                             ", waitSeconds=" + waitSeconds);
            
            if (waitSeconds <= 10) {
                Main.addToConsole("🎯 " + instance.name + " all troops have arrived or arriving very soon!");
                System.out.println("🎯 Last arrival: " + latestArrivalTime.toLocalTime() + " (in " + waitSeconds + " seconds)");
                
                // Wait the remaining time if needed
                if (waitSeconds > 0) {
                    System.out.println("⏳ Short wait: " + waitSeconds + " seconds + 5 second buffer");
                    Thread.sleep(waitSeconds * 1000 + 5000); // Add 5 second buffer
                }
                return;
            }
            
            String waitTimeStr = TimeUtils.formatTime(waitSeconds);
            Main.addToConsole("⏳ " + instance.name + " waiting " + waitTimeStr + " for last troops to arrive...");
            publish("⏳ Waiting " + waitTimeStr + " for last troops to arrive...");
            System.out.println("⏳ Last arrival: " + latestArrivalTime.toLocalTime() + " (in " + waitTimeStr + ")");
            
            // Wait with progress updates
            long waitInterval = 15000; // 15 second intervals
            long totalWaitTime = waitSeconds * 1000;
            long currentWaitTime = 0;
            
            System.out.println("⏳ Starting wait loop: totalWaitTime=" + totalWaitTime + "ms, waitInterval=" + waitInterval + "ms");
            
            while (currentWaitTime < totalWaitTime && !shouldStop && !isCancelled()) {
                long thisWait = Math.min(waitInterval, totalWaitTime - currentWaitTime);
                System.out.println("⏳ Waiting " + thisWait + "ms (currentWaitTime=" + currentWaitTime + "/" + totalWaitTime + ")");
                
                Thread.sleep(thisWait);
                currentWaitTime += thisWait;
                
                long remainingSeconds = (totalWaitTime - currentWaitTime) / 1000;
                if (remainingSeconds > 0) {
                    publish("⏳ Last troops arriving in " + TimeUtils.formatTime(remainingSeconds));
                    System.out.println("⏳ Remaining: " + TimeUtils.formatTime(remainingSeconds));
                } else {
                    publish("🎯 All troops have arrived at resources!");
                    System.out.println("🎯 All troops have arrived!");
                }
            }
            
            System.out.println("⏳ Wait loop completed: currentWaitTime=" + currentWaitTime + "/" + totalWaitTime);
            
            if (!shouldStop && !isCancelled()) {
                Main.addToConsole("🎯 " + instance.name + " all troops have arrived, ready for details collection");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Main.addToConsole("🛑 " + instance.name + " deploy wait interrupted");
        } catch (Exception e) {
            System.err.println("❌ Error calculating arrival times: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to simple longest time wait
            long maxDeployTimeSeconds = 0;
            String longestTimeStr = "00:00:00";
            
            for (MarchDeployInfo march : deployedMarches) {
                long deploySeconds = TimeUtils.parseTimeToSeconds(march.estimatedDeployDuration);
                if (deploySeconds > maxDeployTimeSeconds) {
                    maxDeployTimeSeconds = deploySeconds;
                    longestTimeStr = march.estimatedDeployDuration;
                }
            }
            
            if (maxDeployTimeSeconds > 0) {
                Main.addToConsole("⏳ " + instance.name + " fallback wait: " + longestTimeStr);
                Thread.sleep(maxDeployTimeSeconds * 1000);
            }
        }
    }
    
    /**
     * Add march to tracker using REAL times from details collection
     * MarchDetailsCollector populates actualGatheringTime, so we use that!
     */
    private void addToMarchTracker(MarchDeployInfo deployInfo) {
        try {
            // Check if we have real gathering time from details collection
            if (deployInfo.actualGatheringTime != null) {
                // Use REAL times from details page
                String actualGatheringTime = deployInfo.actualGatheringTime;
                String actualTotalTime = deployInfo.calculateTotalTime(); // This uses actualGatheringTime
                
                // Calculate marching time (deploy time for one-way trip)
                long totalSeconds = TimeUtils.parseTimeToSeconds(actualTotalTime);
                long gatheringSeconds = TimeUtils.parseTimeToSeconds(actualGatheringTime);
                long marchingSeconds = (totalSeconds - gatheringSeconds) / 2; // Round trip, so divide by 2
                String marchingTime = TimeUtils.formatTime(marchingSeconds);
                
                MarchTrackerGUI.getInstance().addMarch(
                    instance.index, 
                    deployInfo.queueNumber, 
                    deployInfo.resourceType, 
                    actualGatheringTime,  // REAL gathering time from details page
                    marchingTime,         // Calculated marching time
                    actualTotalTime       // REAL total time (4+ hours!)
                );
                
                System.out.println("✅ Added to march tracker with REAL times: Queue " + deployInfo.queueNumber + 
                                 ", " + deployInfo.resourceType + 
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
                
                MarchTrackerGUI.getInstance().addMarch(
                    instance.index, 
                    deployInfo.queueNumber, 
                    deployInfo.resourceType, 
                    gatheringTime,                        // Estimated gathering time
                    marchingTime,                         // Estimated marching time
                    deployInfo.estimatedDeployDuration    // Deploy time (fallback)
                );
                
                System.out.println("📊 Added to march tracker with ESTIMATED times: Queue " + deployInfo.queueNumber + 
                                 ", " + deployInfo.resourceType + 
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
 * KEY IMPROVEMENTS MADE:
 * 
 * 1. PRECISE TIMING CALCULATION:
 *    - Records EXACT deployment time for each march
 *    - Calculates precise arrival time: deployTime + deployDuration  
 *    - Waits until LAST march arrives (not longest deploy time)
 *    - Example: Queue 1 (15:00 + 2:30) = 15:02:30, Queue 2 (15:01 + 1:00) = 15:02:00
 *              Wait until 15:02:31 (saves time vs waiting full 2:30)
 * 
 * 2. THREE-PHASE WORKFLOW:
 *    - Phase 1: Deploy all marches quickly (no interruptions)
 *    - Phase 2: Wait for optimal timing (when all troops arrive)
 *    - Phase 3: Collect details for all marches together
 * 
 * 3. MARCH TRACKER INTEGRATION:
 *    - Adds marches AFTER details collection
 *    - TODO: Need MarchDeployInfo updated with real times from details page
 *    - Should show 04:23:29 total time, not 00:02:36 deploy time
 * 
 * 4. ROBUST ERROR HANDLING:
 *    - Fallback to simple timing if calculation fails
 *    - Still adds marches to tracker even if details collection fails
 *    - Graceful degradation throughout
 * 
 * NEXT STEPS:
 * - Need MarchDeployInfo.java to add fields for real times from details page
 * - Update MarchDetailsCollector to populate those fields  
 * - Use real times in addToMarchTracker() method
 */