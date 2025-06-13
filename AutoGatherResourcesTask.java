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
 * REFACTORED: Main AutoGather task with enhanced march details collection
 * Split into smaller, more manageable components
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
                        publish("❌ Failed to setup march view, retrying in 30s...");
                        Thread.sleep(30000);
                        continue;
                    }
                    
                    // Step 2: Read queue statuses
                    List<MarchDetector.MarchInfo> queues = MarchDetector.readMarchQueues(instance.index);
                    if (queues.isEmpty()) {
                        publish("⚠️ No queues detected, retrying in 30s...");
                        Thread.sleep(30000);
                        continue;
                    }
                    
                    // Step 3: Find idle queues
                    List<Integer> idleQueues = findIdleQueues(queues);
                    int activeQueues = countActiveQueues(queues);
                    
                    String status = String.format("%d active, %d idle", activeQueues, idleQueues.size());
                    Main.addToConsole("📊 " + instance.name + " queues: " + status);
                    publish("📊 " + status);
                    
                    // Step 4: ENHANCED: Check if we need to collect march details from previously deployed marches
                    if (!deployedMarches.isEmpty()) {
                        publish("🕒 Checking deployed marches for detail collection...");
                        boolean allDetailsCollected = checkAndCollectMarchDetails();
                        
                        if (allDetailsCollected) {
                            Main.addToConsole("✅ " + instance.name + " all march details collected successfully");
                            deployedMarches.clear(); // Clear for next cycle
                        }
                    }
                    
                    // Step 5: Start marches if we have idle queues
                    if (!idleQueues.isEmpty() && activeQueues < gatherSettings.maxQueues) {
                        deployedMarches.clear(); // Clear previous deployments
                        startMarchesSequentially(idleQueues, activeQueues);
                        
                        // ENHANCED: Wait for marches to arrive, then collect details
                        if (!deployedMarches.isEmpty()) {
                            waitForMarchesToArrive();
                        }
                        
                        Main.addToConsole("⏳ " + instance.name + " waiting 3 minutes for next cycle...");
                        Thread.sleep(180000);
                    } else {
                        // No action needed, wait and check again
                        Thread.sleep(30000);
                    }
                    
                } catch (InterruptedException e) {
                    Main.addToConsole("🛑 " + instance.name + " auto gather interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("Error in gather loop: " + e.getMessage());
                    publish("❌ Error: " + e.getMessage());
                    Thread.sleep(20000);
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
     * FIXED: Wait for marches to arrive with reduced buffer time (5-10 seconds max)
     */
    private void waitForMarchesToArrive() {
        try {
            if (deployedMarches.isEmpty()) {
                System.out.println("⚠️ No deployed marches to wait for");
                return;
            }
            
            // Calculate the longest estimated deploy time
            long maxWaitTimeSeconds = 0;
            for (MarchDeployInfo march : deployedMarches) {
                long deploySeconds = TimeUtils.parseTimeToSeconds(march.estimatedDeployDuration);
                maxWaitTimeSeconds = Math.max(maxWaitTimeSeconds, deploySeconds);
            }
            
            // FIXED: Add only 10 seconds buffer instead of 30
            maxWaitTimeSeconds += 10;
            
            Main.addToConsole("⏳ " + instance.name + " waiting " + TimeUtils.formatTime(maxWaitTimeSeconds) + " for marches to arrive...");
            publish("⏳ Waiting for marches to arrive (" + TimeUtils.formatTime(maxWaitTimeSeconds) + ")");
            
            // Wait with progress updates
            long waitInterval = 30000; // 30 seconds
            long totalWaitTime = maxWaitTimeSeconds * 1000; // Convert to milliseconds
            long currentWaitTime = 0;
            
            while (currentWaitTime < totalWaitTime && !shouldStop && !isCancelled()) {
                Thread.sleep(Math.min(waitInterval, totalWaitTime - currentWaitTime));
                currentWaitTime += waitInterval;
                
                long remainingSeconds = (totalWaitTime - currentWaitTime) / 1000;
                if (remainingSeconds > 0) {
                    publish("⏳ Marches arriving in " + TimeUtils.formatTime(remainingSeconds));
                }
            }
            
            if (!shouldStop && !isCancelled()) {
                Main.addToConsole("🎯 " + instance.name + " marches should have arrived, collecting details...");
                publish("🎯 Marches arrived, collecting details...");
                
                // Now collect march details
                detailsCollector.collectMarchDetailsFromAllDeployedMarches(deployedMarches);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Main.addToConsole("🛑 " + instance.name + " march waiting interrupted");
        } catch (Exception e) {
            System.err.println("❌ Error waiting for marches: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " error waiting for marches: " + e.getMessage());
        }
    }
    
    /**
     * Check if deployed marches are ready for detail collection
     */
    private boolean checkAndCollectMarchDetails() {
        try {
            if (deployedMarches.isEmpty()) {
                return true; // No marches to check
            }
            
            LocalDateTime now = LocalDateTime.now();
            boolean allReady = true;
            
            for (MarchDeployInfo march : deployedMarches) {
                if (!march.detailsCollected) {
                    if (march.hasArrived()) {
                        System.out.println("🎯 Queue " + march.queueNumber + " should have arrived, ready for details collection");
                    } else {
                        allReady = false;
                        long remainingSeconds = march.getSecondsUntilArrival();
                        System.out.println("⏳ Queue " + march.queueNumber + " arriving in " + TimeUtils.formatTime(remainingSeconds));
                    }
                }
            }
            
            if (allReady) {
                return detailsCollector.collectMarchDetailsFromAllDeployedMarches(deployedMarches);
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error checking march readiness: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Start marches sequentially and track deployment info
     */
    private void startMarchesSequentially(List<Integer> idleQueues, int currentActive) {
        try {
            int slotsAvailable = gatherSettings.maxQueues - currentActive;
            int marchesToStart = Math.min(slotsAvailable, idleQueues.size());
            
            Main.addToConsole("🚀 " + instance.name + " starting " + marchesToStart + " march(es) SEQUENTIALLY");
            publish("🚀 Starting " + marchesToStart + " march(es) sequentially");
            
            int successCount = 0;
            boolean isFirstMarch = true;
            
            for (int i = 0; i < marchesToStart; i++) {
                String resourceType = gatherSettings.getNextResource();
                int queueNumber = idleQueues.get(i);
                
                Main.addToConsole("📋 " + instance.name + " starting " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                publish("📋 Starting " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                
                boolean marchSuccess;
                
                if (isFirstMarch) {
                    marchSuccess = gatheringController.startFirstMarch(resourceType, queueNumber);
                    isFirstMarch = false;
                } else {
                    marchSuccess = gatheringController.startSubsequentMarch(resourceType, queueNumber);
                }
                
                if (marchSuccess) {
                    successCount++;
                    Main.addToConsole("✅ " + instance.name + " successfully started " + resourceType + " on Queue " + queueNumber);
                    
                    // ENHANCED: Track deployed march for later details collection
                    // Get the extracted time from the gathering controller
                    String deployTime = gatheringController.getLastExtractedTime();
                    if (deployTime == null) deployTime = lastExtractedTime;
                    
                    MarchDeployInfo deployInfo = new MarchDeployInfo(
                        queueNumber, 
                        resourceType, 
                        LocalDateTime.now(),
                        deployTime
                    );
                    deployedMarches.add(deployInfo);
                    System.out.println("📊 Added march to tracking: " + deployInfo);
                    
                    // REMOVED: Don't add to march tracker immediately
                    // We'll add it after collecting details with total time
                    
                    if (i < marchesToStart - 1) {
                        Thread.sleep(2000);
                    }
                } else {
                    Main.addToConsole("❌ " + instance.name + " failed to start " + resourceType + " on Queue " + queueNumber);
                    isFirstMarch = true;
                }
            }
            
            if (successCount > 0) {
                Main.addToConsole("🎉 " + instance.name + " sequential deployment complete: " + successCount + "/" + marchesToStart + " successful");
                Main.addToConsole("📊 " + instance.name + " tracking " + deployedMarches.size() + " marches for details collection");
                
                // Show march tracker window
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        MarchTrackerGUI.showTracker();
                    } catch (Exception e) {
                        System.err.println("Error showing march tracker: " + e.getMessage());
                    }
                });
            } else {
                Main.addToConsole("❌ " + instance.name + " no marches started successfully");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in sequential march starting: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " sequential march error: " + e.getMessage());
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
    
    // === HELPER METHODS FOR IMPROVED MARCH DETECTION ===
    
    /**
     * IMPROVED: Extract march text panel with better positioning for status detection
     */
    private static boolean extractMarchTextPanelImproved(String sourcePath, String outputPath) {
        try {
            BufferedImage sourceImage = ImageIO.read(new File(sourcePath));
            if (sourceImage == null) {
                System.err.println("❌ Could not load march screenshot: " + sourcePath);
                return false;
            }
            
            // IMPROVED: Adjust extraction to better capture status text based on your image
            // The march queue panel shows clear status text that we need to capture better
            int panelX = 90;        // Slightly more left to capture full words
            int panelY = 210;       // Start from where queue text begins
            int panelWidth = 140;   // Wider to capture full status words like "Idle"
            int panelHeight = 270;  // Cover all 6 queues
            
            System.out.println("📐 [DEBUG] Extracting IMPROVED panel region: x=" + panelX + 
                              ", y=" + panelY + ", w=" + panelWidth + ", h=" + panelHeight);
            System.out.println("🎯 [DEBUG] Improved focus on full status words");
            
            // Bounds checking
            panelX = Math.max(0, panelX);
            panelY = Math.max(0, panelY);
            panelWidth = Math.min(panelWidth, sourceImage.getWidth() - panelX);
            panelHeight = Math.min(panelHeight, sourceImage.getHeight() - panelY);
            
            if (panelWidth <= 0 || panelHeight <= 0) {
                System.err.println("❌ Invalid panel dimensions after bounds check");
                return false;
            }
            
            // Extract the improved text region
            BufferedImage textPanel = sourceImage.getSubimage(panelX, panelY, panelWidth, panelHeight);
            
            // Use shared OCRUtils for image enhancement
            textPanel = OCRUtils.enhanceImageForOCR(textPanel);
            
            // Save the extracted panel
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            if (ImageIO.write(textPanel, "PNG", outputFile)) {
                System.out.println("✅ [DEBUG] Improved text panel extracted: " + outputPath + " (size: " + outputFile.length() + " bytes)");
                return true;
            } else {
                System.err.println("❌ Failed to save text panel");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting march text panel: " + e.getMessage());
            return false;
        }
    }
}