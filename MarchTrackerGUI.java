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
 * OPTIMIZED: March Tracker GUI with better performance and fixed status detection
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

    // REMOVED STATUS COLUMN
    private static final String[] COLUMNS = {
        "Instance", "Queue", "Resource", "Time Remaining", "Progress"
    };

    // OPTIMIZED: Performance tracking
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; // 1 second

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
        setSize(900, 600); // Slightly smaller since we removed a column
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

        // UPDATED: Column widths after removing Status column
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

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshMarchData());

        JButton clearCompletedBtn = new JButton("Clear Completed");
        clearCompletedBtn.addActionListener(e -> clearCompletedMarches());

        // REMOVED: Print Status button

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

    /**
     * OPTIMIZED: More efficient update timer with proper status updates
     */
    private void startOptimizedUpdateTimer() {
        updateTimer = new Timer(1000, e -> {
            long currentTime = System.currentTimeMillis();
            
            // OPTIMIZED: Skip update if called too frequently
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
        
        // FIXED: Store with display queue number (what user sees in game)
        int displayQueueNumber = 3 - queueNumber; // Convert internal queue to display queue
        String marchId = instanceIndex + "-" + displayQueueNumber;
        LocalDateTime startTime = LocalDateTime.now();
        
        // Create march with display queue number
        ActiveMarch march = new ActiveMarch(
            instanceIndex, 
            displayQueueNumber, // Use display queue number consistently
            resourceType, 
            gatheringTime, marchingTime, totalTime, startTime
        );
        
        // Store with display queue number
        activeMarches.put(marchId, march);
        
        SwingUtilities.invokeLater(() -> {
            updateTableDataOptimized();
            statusLabel.setText("Added march: " + resourceType + " on Instance " + instanceIndex + " Queue " + displayQueueNumber);
        });
        
        System.out.println("📊 [TRACKER] Added march: " + march);
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

    // REMOVED: printMarchStatus() method since Print Status button is removed

    /**
     * OPTIMIZED: More efficient display update with batch processing
     */
    private void updateDisplayOptimized() {
        SwingUtilities.invokeLater(() -> {
            // OPTIMIZED: Batch update statuses first
            int updatedCount = 0;
            for (ActiveMarch march : activeMarches.values()) {
                march.updateStatus(); // This will only update if status actually changed
                if (march.isCompleted()) {
                    updatedCount++;
                }
            }
            
            // OPTIMIZED: Only update table if there are active marches
            if (!activeMarches.isEmpty()) {
                updateTableDataOptimized();
                updateStatusLabelsOptimized();
            }
            
            // OPTIMIZED: Auto-move completed marches
            if (updatedCount > 0) {
                moveCompletedMarchesToCompletedList();
            }
        });
    }

    /**
     * OPTIMIZED: More efficient table data update
     */
    private void updateTableDataOptimized() {
        // OPTIMIZED: Only clear and rebuild if row count changed
        int currentRowCount = tableModel.getRowCount();
        int expectedRowCount = activeMarches.size();
        
        if (currentRowCount != expectedRowCount) {
            tableModel.setRowCount(0);
            
            for (ActiveMarch march : activeMarches.values()) {
                Object[] row = createTableRow(march);
                tableModel.addRow(row);
            }
        } else {
            // OPTIMIZED: Update existing rows in place
            int rowIndex = 0;
            for (ActiveMarch march : activeMarches.values()) {
                if (rowIndex < tableModel.getRowCount()) {
                    updateTableRow(rowIndex, march);
                }
                rowIndex++;
            }
        }
    }

    /**
     * UPDATED: Create table row data without Status column
     */
    private Object[] createTableRow(ActiveMarch march) {
        return new Object[]{
            "Instance " + march.getInstanceIndex(),
            "Queue " + march.getQueueNumber(),
            march.getResourceType(),
            formatTimeRemaining(march.getTimeRemaining()),
            Math.round(march.getProgressPercentage())
        };
    }

    /**
     * UPDATED: Update existing table row without Status column
     */
    private void updateTableRow(int rowIndex, ActiveMarch march) {
        tableModel.setValueAt(formatTimeRemaining(march.getTimeRemaining()), rowIndex, 3);
        tableModel.setValueAt(Math.round(march.getProgressPercentage()), rowIndex, 4);
    }

    /**
     * OPTIMIZED: More efficient status label updates
     */
    private void updateStatusLabelsOptimized() {
        int totalMarches = activeMarches.size();
        int gathering = 0;
        int marching = 0;
        int returning = 0;
        int completed = 0;

        for (ActiveMarch march : activeMarches.values()) {
            String status = march.getStatus();
            if (status.contains("Gathering")) {
                gathering++;
            } else if (status.contains("Marching")) {
                marching++;
            } else if (status.contains("Returning")) {
                returning++;
            } else if (status.contains("Completed")) {
                completed++;
            }
        }

        totalMarchesLabel.setText(String.format(
            "Total: %d | Marching: %d | Gathering: %d | Returning: %d | Completed: %d", 
            totalMarches, marching, gathering, returning, completed));
    }

    /**
     * OPTIMIZED: Move completed marches efficiently
     */
    private void moveCompletedMarchesToCompletedList() {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, ActiveMarch> entry : activeMarches.entrySet()) {
            ActiveMarch march = entry.getValue();
            if (march.isCompleted()) {
                completedMarches.add(march);
                toRemove.add(entry.getKey());
            }
        }
        
        for (String marchId : toRemove) {
            activeMarches.remove(marchId);
        }
        
        if (!toRemove.isEmpty()) {
            System.out.println("📊 [TRACKER] Moved " + toRemove.size() + " completed marches to completed list");
        }
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
        activeMarches.entrySet().removeIf(entry -> 
            entry.getValue().getStatus().contains("Completed") || 
            entry.getValue().getTimeRemaining() <= 0
        );
        updateTableDataOptimized();
        statusLabel.setText("Cleared completed marches");
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
            if (value instanceof Integer) {
                progress = (Integer) value;
            } else if (value instanceof Double) {
                progress = (int) Math.round((Double) value);
            } else if (value instanceof String) {
                try {
                    progress = Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    progress = 0;
                }
            }
            
            progress = Math.max(0, Math.min(100, progress));
            setValue(progress);
            setString(progress + "%");
            
            if (progress >= 90) {
                setForeground(new Color(76, 175, 80));
                setBackground(new Color(45, 45, 48));
            } else if (progress >= 50) {
                setForeground(new Color(255, 193, 7));
                setBackground(new Color(45, 45, 48));
            } else {
                setForeground(new Color(244, 67, 54));
                setBackground(new Color(45, 45, 48));
            }
            
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