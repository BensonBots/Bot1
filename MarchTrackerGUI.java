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
 * FIXED: March Tracker GUI with smooth second-by-second updates and preserved completed marches
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

    private MarchTrackerGUI() {
        activeMarches = new ConcurrentHashMap<>();
        completedMarches = new ArrayList<>();
        initializeUI();
        startSmoothUpdateTimer();
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

        showCompletedCheckbox = new JCheckBox("Show Completed", false);
        showCompletedCheckbox.addActionListener(e -> {
            updateTableData();
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

    // FIXED: Smooth timer that updates every second and handles automatic completion
    private void startSmoothUpdateTimer() {
        updateTimer = new Timer(1000, e -> {
            SwingUtilities.invokeLater(() -> {
                // Update march statuses and handle automatic completion
                List<String> completedMarchIds = new ArrayList<>();
                
                for (Map.Entry<String, ActiveMarch> entry : activeMarches.entrySet()) {
                    ActiveMarch march = entry.getValue();
                    march.updateStatus();
                    
                    // Check if march just completed
                    if (march.isCompleted() && !march.getStatus().contains("Completed")) {
                        march.setStatus("✅ Completed");
                        completedMarchIds.add(entry.getKey());
                        System.out.println("📊 [TRACKER] March auto-completed: " + march.getSummary());
                    }
                }
                
                // Move completed marches to completed list
                for (String marchId : completedMarchIds) {
                    ActiveMarch completedMarch = activeMarches.remove(marchId);
                    if (completedMarch != null) {
                        completedMarches.add(completedMarch);
                    }
                }
                
                // Update display
                updateTableData();
                updateStatusLabels();
                
                // Log completed marches
                if (!completedMarchIds.isEmpty()) {
                    statusLabel.setText("Auto-completed " + completedMarchIds.size() + " march(es)");
                }
            });
        });
        updateTimer.start();
        
        System.out.println("🚀 [TRACKER] Smooth update timer started (1-second intervals with auto-completion)");
    }

    public void addMarch(int instanceIndex, int queueNumber, String resourceType, 
                        String gatheringTime, String marchingTime, String totalTime) {
        
        // FIXED: Use timestamp in march ID to prevent overwriting completed marches
        String marchId = instanceIndex + "-" + queueNumber + "-" + System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        
        // Check if there's an existing active march for this instance+queue
        String oldMarchId = findExistingActiveMarch(instanceIndex, queueNumber);
        if (oldMarchId != null) {
            System.out.println("📊 [TRACKER] Found existing active march for Instance " + instanceIndex + " Queue " + queueNumber + ", completing it first");
            // Move existing march to completed before adding new one
            ActiveMarch existingMarch = activeMarches.remove(oldMarchId);
            if (existingMarch != null) {
                existingMarch.setStatus("✅ Completed (New march started)");
                completedMarches.add(existingMarch);
                System.out.println("📊 [TRACKER] Moved existing march to completed: " + existingMarch.getSummary());
            }
        }
        
        ActiveMarch march = new ActiveMarch(
            instanceIndex, 
            queueNumber,
            resourceType, 
            gatheringTime, marchingTime, totalTime, startTime
        );
        
        activeMarches.put(marchId, march);
        
        SwingUtilities.invokeLater(() -> {
            updateTableData();
            statusLabel.setText("Added march: " + resourceType + " on Instance " + instanceIndex + " Queue " + queueNumber);
        });
        
        System.out.println("📊 [TRACKER] Added new march with unique ID: " + marchId);
        System.out.println("📊 [TRACKER] March details: " + march);
    }
    
    // Helper method to find existing active march for same instance+queue
    private String findExistingActiveMarch(int instanceIndex, int queueNumber) {
        for (Map.Entry<String, ActiveMarch> entry : activeMarches.entrySet()) {
            ActiveMarch march = entry.getValue();
            if (march.getInstanceIndex() == instanceIndex && march.getQueueNumber() == queueNumber) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void registerNewMarch(int instanceIndex, int queueNumber, String resourceType, String marchTime) {
        getInstance().addMarch(instanceIndex, queueNumber, resourceType, "", "", marchTime);
    }

    public static void addMarch(int id, String type, String target, String status, String extra) {
        getInstance().addMarch(id, 1, type, "", "", "");
    }

    public void updateMarchStatus(int instanceIndex, int queueNumber, String newStatus) {
        // FIXED: Find march by instance+queue since we now use unique IDs
        String marchId = findExistingActiveMarch(instanceIndex, queueNumber);
        if (marchId != null) {
            ActiveMarch march = activeMarches.get(marchId);
            if (march != null) {
                march.setStatus(newStatus);
                SwingUtilities.invokeLater(() -> {
                    updateTableData();
                    statusLabel.setText("Updated march status: " + newStatus);
                });
            }
        }
    }

    public void completeMarch(int instanceIndex, int queueNumber) {
        // FIXED: Find march by instance+queue since we now use unique IDs
        String marchId = findExistingActiveMarch(instanceIndex, queueNumber);
        if (marchId != null) {
            ActiveMarch march = activeMarches.remove(marchId);
            if (march != null) {
                march.setStatus("✅ Completed");
                completedMarches.add(march);
                SwingUtilities.invokeLater(() -> {
                    updateTableData();
                    statusLabel.setText("March completed: " + march.getResourceType());
                });
                System.out.println("📊 [TRACKER] March completed and moved to completed list: " + march.getSummary());
            }
        }
    }

    public void removeMarch(int instanceIndex, int queueNumber) {
        // FIXED: Find march by instance+queue since we now use unique IDs
        String marchId = findExistingActiveMarch(instanceIndex, queueNumber);
        if (marchId != null) {
            ActiveMarch removed = activeMarches.remove(marchId);
            if (removed != null) {
                SwingUtilities.invokeLater(() -> {
                    updateTableData();
                    statusLabel.setText("March removed: " + removed.getResourceType());
                });
                System.out.println("📊 [TRACKER] March removed: " + removed.getSummary());
            }
        }
    }

    public boolean isQueueMarching(int instanceIndex, int queueNumber) {
        // FIXED: Check by instance+queue since we now use unique IDs
        return findExistingActiveMarch(instanceIndex, queueNumber) != null;
    }

    public ActiveMarch getMarchInfo(int instanceIndex, int queueNumber) {
        // FIXED: Find march by instance+queue since we now use unique IDs
        String marchId = findExistingActiveMarch(instanceIndex, queueNumber);
        if (marchId != null) {
            return activeMarches.get(marchId);
        }
        return null;
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
            updateTableData();
            statusLabel.setText("All marches cleared");
        });
        System.out.println("🧹 March tracker cleared");
    }

    // FIXED: Clean table update that always refreshes
    private void updateTableData() {
        // Determine which marches to show
        List<ActiveMarch> marchesToShow = new ArrayList<>();
        
        // Always show active marches
        for (ActiveMarch march : activeMarches.values()) {
            if (!march.isCompleted()) {
                marchesToShow.add(march);
            } else if (showCompletedCheckbox.isSelected()) {
                marchesToShow.add(march);
            }
        }
        
        // Add completed marches from the completed list if checkbox is checked
        if (showCompletedCheckbox.isSelected()) {
            marchesToShow.addAll(completedMarches);
        }
        
        // Always rebuild table for smooth updates
        tableModel.setRowCount(0);
        
        for (ActiveMarch march : marchesToShow) {
            Object[] row = createTableRow(march);
            tableModel.addRow(row);
        }
        
        // Force table repaint for smooth updates
        marchTable.repaint();
    }

    private Object[] createTableRow(ActiveMarch march) {
        double progressPercent = march.getProgressPercentage();
        
        return new Object[]{
            "Instance " + march.getInstanceIndex(),
            "Queue " + march.getQueueNumber(),
            march.getResourceType(),
            march.getTimeRemaining(), // Raw seconds for renderer
            progressPercent
        };
    }

    private void updateStatusLabels() {
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
        updateTableData();
        statusLabel.setText("Refreshed march data");
    }

    private void clearCompletedMarches() {
        // Remove completed marches from active marches (move them to completed list)
        Iterator<Map.Entry<String, ActiveMarch>> iterator = activeMarches.entrySet().iterator();
        int movedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, ActiveMarch> entry = iterator.next();
            ActiveMarch march = entry.getValue();
            if (march.isCompleted()) {
                march.setStatus("✅ Completed");
                completedMarches.add(march);
                iterator.remove();
                movedCount++;
            }
        }
        
        // Clear the completed marches list
        int clearedCount = completedMarches.size();
        completedMarches.clear();
        
        updateTableData();
        statusLabel.setText("Moved " + movedCount + " active completed marches, cleared " + clearedCount + " old completed marches");
        System.out.println("🧹 [TRACKER] Moved " + movedCount + " completed marches from active, cleared " + clearedCount + " from completed list");
    }

    public static void showTracker() {
        SwingUtilities.invokeLater(() -> {
            MarchTrackerGUI tracker = getInstance();
            tracker.setVisible(true);
            tracker.toFront();
        });
    }

    // FIXED: Time remaining renderer that works with raw seconds
    private class TimeRemainingRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                     boolean hasFocus, int row, int column) {
            
            String timeStr;
            if (value instanceof Long) {
                long seconds = (Long) value;
                timeStr = formatTimeRemaining(seconds);
            } else {
                timeStr = value.toString();
            }
            
            Component c = super.getTableCellRendererComponent(table, timeStr, isSelected, hasFocus, row, column);
            
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
            
            progress = Math.max(0, Math.min(100, progress));
            setValue(progress);
            setString(progress + "%");
            
            // Color coding
            if (progress >= 100) {
                setForeground(new Color(76, 175, 80)); // Green
            } else if (progress >= 75) {
                setForeground(new Color(139, 195, 74)); // Light green
            } else if (progress >= 50) {
                setForeground(new Color(255, 193, 7)); // Yellow
            } else if (progress >= 25) {
                setForeground(new Color(255, 152, 0)); // Orange
            } else {
                setForeground(new Color(244, 67, 54)); // Red
            }
            
            setForeground(Color.WHITE); // Override for visibility
            setBackground(new Color(45, 45, 48));
            
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