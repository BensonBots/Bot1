package newgame;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIXED: March Tracker GUI with corrected progress bar and completed march handling
 */
public class MarchTrackerGUI extends JFrame {
    private static MarchTrackerGUI instance;
    private DefaultTableModel tableModel;
    private JTable marchTable;
    private Timer updateTimer;
    private Map<String, ActiveMarch> activeMarches;
    private List<ActiveMarch> completedMarches;
    private JLabel statusLabel;
    private JLabel totalMarchesLabel;
    private JCheckBox showCompletedCheckbox;

    private static final String[] COLUMNS = {
        "Instance", "Queue", "Resource", "Time Remaining", "Progress"
    };

    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000;

    private MarchTrackerGUI() {
        activeMarches = new ConcurrentHashMap<>();
        completedMarches = new ArrayList<>();
        initializeUI();
        startOptimizedUpdateTimer();
    }

    public static synchronized MarchTrackerGUI getInstance() {
        if (instance == null) {
            instance = new MarchTrackerGUI();
        }
        return instance;
    }

    private void initializeUI() {
        setTitle("March Tracker - Active Gathering Operations");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        marchTable = new JTable(tableModel);
        configureTable();

        JPanel statusPanel = createStatusPanel();
        JPanel controlPanel = createControlPanel();

        setLayout(new BorderLayout());
        add(statusPanel, BorderLayout.NORTH);
        add(new JScrollPane(marchTable), BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        applyDarkTheme();
    }

    private void configureTable() {
        marchTable.setRowHeight(40);
        marchTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        marchTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        marchTable.getColumnModel().getColumn(0).setPreferredWidth(120); // Instance
        marchTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Queue
        marchTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Resource
        marchTable.getColumnModel().getColumn(3).setPreferredWidth(180); // Time Remaining
        marchTable.getColumnModel().getColumn(4).setPreferredWidth(250); // Progress

        marchTable.getColumnModel().getColumn(3).setCellRenderer(new TimeRemainingRenderer());
        marchTable.getColumnModel().getColumn(4).setCellRenderer(new ProgressBarRenderer());
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        totalMarchesLabel = new JLabel("Total Active Marches: 0");
        totalMarchesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        statusLabel = new JLabel("March Tracker Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        panel.add(totalMarchesLabel, BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // FIXED: Add checkbox to show/hide completed marches
        showCompletedCheckbox = new JCheckBox("Show Completed", false);
        showCompletedCheckbox.addActionListener(e -> {
            updateTableDataOptimized();
            System.out.println("🔄 Show completed marches: " + showCompletedCheckbox.isSelected());
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshMarchData());

        JButton clearCompletedBtn = new JButton("Clear Completed");
        clearCompletedBtn.addActionListener(e -> clearCompletedMarches());

        panel.add(showCompletedCheckbox);
        panel.add(refreshBtn);
        panel.add(clearCompletedBtn);

        return panel;
    }

    private void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
        }
    }

    private void startOptimizedUpdateTimer() {
        updateTimer = new Timer(1000, e -> {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
                return;
            }
            
            updateDisplayOptimized();
            lastUpdateTime = currentTime;
        });
        updateTimer.start();
        
        System.out.println("🚀 [TRACKER] Optimized update timer started (1-second intervals)");
    }

    public void addMarch(int instanceIndex, int queueNumber, String resourceType, 
                        String gatheringTime, String marchingTime, String totalTime) {
        
        String marchId = instanceIndex + "-" + queueNumber;
        LocalDateTime startTime = LocalDateTime.now();
        
        ActiveMarch march = new ActiveMarch(
            instanceIndex, 
            queueNumber,
            resourceType, 
            gatheringTime, marchingTime, totalTime, startTime
        );
        
        activeMarches.put(marchId, march);
        
        SwingUtilities.invokeLater(() -> {
            updateTableDataOptimized();
            statusLabel.setText("Added march: " + resourceType + " on Instance " + instanceIndex + " Queue " + queueNumber);
        });
        
        System.out.println("📊 [TRACKER] FIXED: Added march with EXACT queue number " + queueNumber);
        System.out.println("📊 [TRACKER] March: " + march);
        System.out.println("📊 [TRACKER] March timing - Marching: " + marchingTime + ", Gathering: " + gatheringTime + ", Total: " + totalTime);
    }

    public static void registerNewMarch(int instanceIndex, int queueNumber, String resourceType, String marchTime) {
        getInstance().addMarch(instanceIndex, queueNumber, resourceType, "", "", marchTime);
    }

    public static void addMarch(int id, String type, String target, String status, String extra) {
        getInstance().addMarch(id, 1, type, "", "", "");
    }

    public void updateMarchStatus(int instanceIndex, int queueNumber, String newStatus) {
        String marchId = instanceIndex + "-" + queueNumber;
        ActiveMarch march = activeMarches.get(marchId);
        
        if (march != null) {
            march.setStatus(newStatus);
            SwingUtilities.invokeLater(() -> {
                updateTableDataOptimized();
                statusLabel.setText("Updated march status: " + newStatus);
            });
        }
    }

    public void completeMarch(int instanceIndex, int queueNumber) {
        String marchId = instanceIndex + "-" + queueNumber;
        ActiveMarch march = activeMarches.remove(marchId);
        
        if (march != null) {
            march.setStatus("✅ Completed");
            completedMarches.add(march);
            SwingUtilities.invokeLater(() -> {
                updateTableDataOptimized();
                statusLabel.setText("March completed: " + march.getResourceType());
            });
        }
    }

    public void removeMarch(int instanceIndex, int queueNumber) {
        String marchId = instanceIndex + "-" + queueNumber;
        ActiveMarch removed = activeMarches.remove(marchId);
        
        if (removed != null) {
            SwingUtilities.invokeLater(() -> {
                updateTableDataOptimized();
                statusLabel.setText("March removed: " + removed.getResourceType());
            });
        }
    }

    public boolean isQueueMarching(int instanceIndex, int queueNumber) {
        String marchId = instanceIndex + "-" + queueNumber;
        return activeMarches.containsKey(marchId);
    }

    public ActiveMarch getMarchInfo(int instanceIndex, int queueNumber) {
        String marchId = instanceIndex + "-" + queueNumber;
        return activeMarches.get(marchId);
    }

    public List<ActiveMarch> getActiveMarches() {
        return new ArrayList<>(activeMarches.values());
    }

    public List<ActiveMarch> getCompletedMarches() {
        return new ArrayList<>(completedMarches);
    }

    public void clearAllMarches() {
        activeMarches.clear();
        completedMarches.clear();
        SwingUtilities.invokeLater(() -> {
            updateTableDataOptimized();
            statusLabel.setText("All marches cleared");
        });
        System.out.println("🧹 March tracker cleared");
    }

    /**
     * FIXED: Update display without automatically moving completed marches
     */
    private void updateDisplayOptimized() {
        SwingUtilities.invokeLater(() -> {
            // Update statuses for all marches
            for (ActiveMarch march : activeMarches.values()) {
                march.updateStatus();
            }
            
            // Always update table and status labels
            updateTableDataOptimized();
            updateStatusLabelsOptimized();
        });
    }

    /**
     * FIXED: More efficient table update that only updates when needed
     */
    private void updateTableDataOptimized() {
        // Determine which marches to show
        List<ActiveMarch> marchesToShow = new ArrayList<>();
        
        // Always show active marches
        for (ActiveMarch march : activeMarches.values()) {
            if (!march.isCompleted()) {
                marchesToShow.add(march);
            } else if (showCompletedCheckbox.isSelected()) {
                // Show completed marches if checkbox is checked
                marchesToShow.add(march);
            }
        }
        
        // Add completed marches from the completed list if checkbox is checked
        if (showCompletedCheckbox.isSelected()) {
            marchesToShow.addAll(completedMarches);
        }
        
        // FIXED: Only update if row count changed or every 10 seconds
        int currentRowCount = tableModel.getRowCount();
        int expectedRowCount = marchesToShow.size();
        
        boolean shouldUpdate = (currentRowCount != expectedRowCount) || 
                              (System.currentTimeMillis() % 10000 < 1000); // Every 10 seconds
        
        if (shouldUpdate) {
            // Clear and rebuild table
            tableModel.setRowCount(0);
            
            for (ActiveMarch march : marchesToShow) {
                Object[] row = createTableRow(march);
                tableModel.addRow(row);
            }
            
            // FIXED: Only log significant updates
            if (currentRowCount != expectedRowCount) {
                System.out.println("🔄 [TRACKER] Table updated: " + marchesToShow.size() + " marches shown");
            }
        }
    }

    /**
     * FIXED: Create table row with proper progress calculation
     */
    private Object[] createTableRow(ActiveMarch march) {
        // FIXED: Use the march's built-in progress calculation
        double progressPercent = march.getProgressPercentage();
        
        return new Object[]{
            "Instance " + march.getInstanceIndex(),
            "Queue " + march.getQueueNumber(),
            march.getResourceType(),
            formatTimeRemaining(march.getTimeRemaining()),
            progressPercent // FIXED: Use actual progress percentage
        };
    }

    private void updateStatusLabelsOptimized() {
        int totalActive = 0;
        int gathering = 0;
        int marching = 0;
        int returning = 0;
        int completed = 0;

        for (ActiveMarch march : activeMarches.values()) {
            if (!march.isCompleted()) {
                totalActive++;
                String status = march.getStatus();
                if (status.contains("Gathering")) {
                    gathering++;
                } else if (status.contains("Marching")) {
                    marching++;
                } else if (status.contains("Returning")) {
                    returning++;
                }
            } else {
                completed++;
            }
        }
        
        // Add completed marches from completed list
        completed += completedMarches.size();

        totalMarchesLabel.setText(String.format(
            "Active: %d | Marching: %d | Gathering: %d | Returning: %d | Completed: %d", 
            totalActive, marching, gathering, returning, completed));
    }

    private String formatTimeRemaining(long secondsRemaining) {
        if (secondsRemaining <= 0) {
            return "00:00:00";
        }
        
        long hours = secondsRemaining / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void refreshMarchData() {
        updateDisplayOptimized();
        statusLabel.setText("Refreshed march data");
    }

    private void clearCompletedMarches() {
        // Remove completed marches from active marches
        activeMarches.entrySet().removeIf(entry -> entry.getValue().isCompleted());
        
        // Clear the completed marches list
        completedMarches.clear();
        
        updateTableDataOptimized();
        statusLabel.setText("Cleared completed marches");
        System.out.println("🧹 [TRACKER] Cleared all completed marches");
    }

    public static void showTracker() {
        SwingUtilities.invokeLater(() -> {
            MarchTrackerGUI tracker = getInstance();
            tracker.setVisible(true);
            tracker.toFront();
        });
    }

    private class TimeRemainingRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                     boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            String timeStr = value.toString();
            if (timeStr.equals("00:00:00")) {
                c.setForeground(Color.GREEN);
                setText("✅ COMPLETED");
            } else if (timeStr.startsWith("00:0")) {
                c.setForeground(Color.ORANGE);
            } else {
                c.setForeground(isSelected ? Color.WHITE : Color.LIGHT_GRAY);
            }
            
            return c;
        }
    }

    /**
     * FIXED: Progress bar renderer that properly handles the progress value
     */
    private class ProgressBarRenderer extends JProgressBar implements TableCellRenderer {
        
        public ProgressBarRenderer() {
            setStringPainted(true);
            setMinimum(0);
            setMaximum(100);
            setBorderPainted(false);
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                     boolean hasFocus, int row, int column) {
            
            int progress = 0;
            
            // FIXED: Proper handling of different value types
            if (value instanceof Double) {
                progress = (int) Math.round((Double) value);
            } else if (value instanceof Integer) {
                progress = (Integer) value;
            } else if (value instanceof String) {
                try {
                    progress = Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    progress = 0;
                }
            }
            
            // FIXED: Ensure progress is within valid range
            progress = Math.max(0, Math.min(100, progress));
            setValue(progress);
            setString(progress + "%");
            
            // FIXED: Better color coding
            if (progress >= 100) {
                setForeground(new Color(76, 175, 80)); // Green for completed
                setBackground(new Color(45, 45, 48));
            } else if (progress >= 75) {
                setForeground(new Color(139, 195, 74)); // Light green for almost done
                setBackground(new Color(45, 45, 48));
            } else if (progress >= 50) {
                setForeground(new Color(255, 193, 7)); // Yellow for halfway
                setBackground(new Color(45, 45, 48));
            } else if (progress >= 25) {
                setForeground(new Color(255, 152, 0)); // Orange for started
                setBackground(new Color(45, 45, 48));
            } else {
                setForeground(new Color(244, 67, 54)); // Red for just started
                setBackground(new Color(45, 45, 48));
            }
            
            // Override foreground to white for better visibility
            setForeground(Color.WHITE);
            
            if (isSelected) {
                setBorder(BorderFactory.createLineBorder(new Color(100, 150, 255), 1));
            } else {
                setBorder(BorderFactory.createLineBorder(new Color(60, 60, 63), 1));
            }
            
            return this;
        }
    }

    @Override
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        super.dispose();
    }
}