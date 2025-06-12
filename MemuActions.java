package newgame;

import javax.swing.*;

public class MemuActions {
    // Standard resolution settings
    private static final int TARGET_WIDTH = 400;
    private static final int TARGET_HEIGHT = 652;
    private static final int TARGET_DPI = 133;
    
    public static void startInstance(JFrame parent, int index, Runnable onSuccess) {
        executeCommand(parent, "start", index, onSuccess);
    }

    public static void stopInstance(JFrame parent, int index, Runnable onSuccess) {
        executeCommand(parent, "stop", index, onSuccess);
    }

    public static void optimizeInstanceInBackground(int index, Runnable onComplete) {
        new Thread(() -> {
            try {
                System.out.println("Auto-optimizing instance " + index + " in background...");
                
                // Check if instance is already stopped to avoid unnecessary restart
                boolean wasRunning = BotUtils.isInstanceRunning(index);
                
                // Stop instance first to change resolution (only if running)
                if (wasRunning) {
                    System.out.println("Stopping running instance " + index + " for optimization...");
                    executeMemuCommand("stop", "-i", String.valueOf(index));
                    Thread.sleep(3000); // Wait for stop
                } else {
                    System.out.println("Instance " + index + " already stopped, proceeding with optimization...");
                }
                
                // Set resolution to 400x652 with 133 DPI
                executeMemuCommand("setconfigex", "-i", String.valueOf(index), 
                    "resolution", TARGET_WIDTH + "," + TARGET_HEIGHT + "," + TARGET_DPI);
                Thread.sleep(1000);
                
                // Set other optimization settings
                executeMemuCommand("setconfigex", "-i", String.valueOf(index), "cpus", "2");
                Thread.sleep(500);
                executeMemuCommand("setconfigex", "-i", String.valueOf(index), "memory", "3000");
                Thread.sleep(500);
                executeMemuCommand("setconfigex", "-i", String.valueOf(index), "fps", "30");
                Thread.sleep(1000);
                
                System.out.println("Instance " + index + " optimized to " + TARGET_WIDTH + "x" + TARGET_HEIGHT + " @ " + TARGET_DPI + " DPI");
                System.out.println("Note: Instance will be started separately after optimization");
                
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
                
            } catch (Exception e) {
                System.err.println("Background optimization failed for instance " + index + ": " + e.getMessage());
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            }
        }).start();
    }

    private static void executeCommand(JFrame parent, String command, int index, Runnable onSuccess) {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                executeMemuCommand(command, "-i", String.valueOf(index));
                return null;
            }
            
            protected void done() {
                try {
                    get();
                    if (onSuccess != null) {
                        SwingUtilities.invokeLater(onSuccess);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(parent, 
                        "Command failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private static void executeMemuCommand(String... args) throws Exception {
        String[] fullCommand = new String[args.length + 1];
        fullCommand[0] = Main.MEMUC_PATH;
        System.arraycopy(args, 0, fullCommand, 1, args.length);
        
        Process p = Runtime.getRuntime().exec(fullCommand);
        int exitCode = p.waitFor();
        
        if (exitCode != 0) {
            throw new Exception("MEmu command failed with exit code: " + exitCode);
        }
    }
}