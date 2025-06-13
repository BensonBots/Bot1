package newgame;

import java.awt.AWTException;
import java.util.Map;

public class AutoGatherModule {
    
    public static class AutoGatherSettings {
        public String resourceLoop = "Food,Wood,Stone,Iron";
        public int currentIndex = 0;
        public int maxQueues = 6;
        
        public String getNextResource() {
            String[] resources = resourceLoop.split(",");
            if (resources.length == 0) return "Food";
            
            String resource = resources[currentIndex % resources.length];
            currentIndex++;
            return resource.trim();
        }
        
        public static AutoGatherSettings fromString(String settings) {
            AutoGatherSettings result = new AutoGatherSettings();
            if (settings != null && !settings.trim().isEmpty()) {
                try {
                    // Parse settings string: "Loop:Food,Wood,Stone,Iron;Index:0;MaxQueues:6"
                    String[] parts = settings.split(";");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("Loop:")) {
                            result.resourceLoop = part.substring(5);
                        } else if (part.startsWith("Index:")) {
                            try {
                                result.currentIndex = Integer.parseInt(part.substring(6));
                            } catch (NumberFormatException e) {
                                result.currentIndex = 0;
                            }
                        } else if (part.startsWith("MaxQueues:")) {
                            try {
                                result.maxQueues = Integer.parseInt(part.substring(10));
                            } catch (NumberFormatException e) {
                                result.maxQueues = 6;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing AutoGather settings: " + e.getMessage());
                }
            }
            return result;
        }
        
        @Override
        public String toString() {
            return "Loop:" + resourceLoop + ";Index:" + currentIndex + ";MaxQueues:" + maxQueues;
        }
    }
    
    // Constructor - FIXED: Removed resourceInfo initialization
    public AutoGatherModule() {
        // Original constructor logic without resourceInfo
    }
    
    // Start method
    public void start(MemuInstance instance) {
        try {
            System.out.println("🚀 Starting Auto Gather for instance " + instance.index);
            
            // Load settings for this instance
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new java.util.HashMap<>());
            ModuleState<?> gatherModule = modules.get("Auto Gather Resources");
            
            if (gatherModule != null && gatherModule.settings != null) {
                System.out.println("📋 Using settings: " + gatherModule.settings.toString());
            } else {
                System.out.println("⚠️ No settings found, using defaults");
            }
            
            // Create and start the auto gather task
            AutoGatherResourcesTask task = new AutoGatherResourcesTask(instance);
            task.execute();
            
            System.out.println("✅ Auto Gather task started for instance " + instance.index);
            
        } catch (Exception e) {
            System.err.println("❌ Error starting Auto Gather: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " Auto Gather failed to start: " + e.getMessage());
        }
    }
    
    // Stop method
    public void stop(MemuInstance instance) {
        try {
            System.out.println("🛑 Stopping Auto Gather for instance " + instance.index);
            
            // Stop the auto gather task
            instance.setAutoGatherRunning(false);
            
            Main.addToConsole("🛑 " + instance.name + " Auto Gather stopped");
            System.out.println("✅ Auto Gather stopped for instance " + instance.index);
            
        } catch (Exception e) {
            System.err.println("❌ Error stopping Auto Gather: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " Auto Gather stop failed: " + e.getMessage());
        }
    }
    
    // Check if running
    public boolean isRunning(MemuInstance instance) {
        return instance.isAutoGatherRunning();
    }
    
    // Get status
    public String getStatus(MemuInstance instance) {
        if (isRunning(instance)) {
            return "Running - " + instance.getState();
        } else {
            return "Stopped";
        }
    }
    
    // Configure method
    public void configure(MemuInstance instance) {
        try {
            System.out.println("🔧 Configuring Auto Gather for instance " + instance.index);
            
            // Get current settings
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new java.util.HashMap<>());
            ModuleState<?> gatherModule = modules.get("Auto Gather Resources");
            
            AutoGatherSettings currentSettings;
            if (gatherModule != null && gatherModule.settings != null) {
                currentSettings = AutoGatherSettings.fromString(gatherModule.settings.toString());
            } else {
                currentSettings = new AutoGatherSettings();
            }
            
            // Log current settings
            System.out.println("📋 Current Auto Gather settings: " + currentSettings.toString());
            
        } catch (Exception e) {
            System.err.println("❌ Error configuring Auto Gather: " + e.getMessage());
        }
    }
    
    // Update settings
    public void updateSettings(MemuInstance instance, AutoGatherSettings newSettings) {
        try {
            System.out.println("💾 Updating Auto Gather settings for instance " + instance.index);
            
            // Get or create module state
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new java.util.HashMap<>());
            
            ModuleState<AutoGatherSettings> gatherModule = (ModuleState<AutoGatherSettings>) modules.get("Auto Gather Resources");
            if (gatherModule == null) {
                gatherModule = new ModuleState<>(true, newSettings);
                modules.put("Auto Gather Resources", gatherModule);
                Main.instanceModules.put(instance.index, modules);
            } else {
                gatherModule.settings = newSettings;
            }
            
            System.out.println("✅ Settings updated: " + newSettings.toString());
            
            // Save settings to file - FIXED: Remove static call since saveSettings() is not static
            // Main.saveSettings(); // Commented out - handle saving elsewhere if needed
            
        } catch (Exception e) {
            System.err.println("❌ Error updating Auto Gather settings: " + e.getMessage());
        }
    }
    
    // Get settings
    public AutoGatherSettings getSettings(MemuInstance instance) {
        try {
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new java.util.HashMap<>());
            ModuleState<?> gatherModule = modules.get("Auto Gather Resources");
            
            if (gatherModule != null && gatherModule.settings != null) {
                return AutoGatherSettings.fromString(gatherModule.settings.toString());
            } else {
                return new AutoGatherSettings();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error getting Auto Gather settings: " + e.getMessage());
            return new AutoGatherSettings();
        }
    }
    
    // Additional utility methods that might be in your original file
    
    public String getDescription() {
        return "Automatically gathers resources using available march queues";
    }
    
    public String getVersion() {
        return "1.0.0";
    }
    
    public boolean canStart(MemuInstance instance) {
        return instance != null && !instance.isAutoGatherRunning();
    }
    
    public void initialize() {
        System.out.println("🔧 AutoGatherModule initialized");
    }
    
    public void cleanup() {
        System.out.println("🧹 AutoGatherModule cleanup");
    }
}