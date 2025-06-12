package newgame;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MarchTrackerGUI extends JFrame {
    private static MarchTrackerGUI instance;
    private DefaultTableModel tableModel;
    private JTable marchTable;
    private Timer updateTimer;
    private Map<String, ActiveMarch> activeMarches;
    private List<ActiveMarch> completedMarches;
    private JLabel statusLabel;
    private JLabel totalMarchesLabel;

    private static final String[] COLUMNS = {
        "Instance", "Queue", "Resource", "Status", "Time Remaining", "Progress"
    };

    private MarchTrackerGUI() {
        activeMarches = new ConcurrentHashMap<>();
        completedMarches = new ArrayList<>();
        initializeUI();
        startUpdateTimer();
    }

    public static synchronized MarchTrackerGUI getInstance() {
        if (instance == null) {
            instance = new MarchTrackerGUI();
        }
        return instance;
    }

    private void initializeUI() {
        setTitle("March Tracker - Active Gathering Operations");
        setSize(1000, 600);
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

        marchTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        marchTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        marchTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        marchTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        marchTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        marchTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        marchTable.getColumnModel().getColumn(4).setCellRenderer(new TimeRemainingRenderer());
        marchTable.getColumnModel().getColumn(5).setCellRenderer(new ProgressBarRenderer());
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

        JButton printStatusBtn = new JButton("Print Status");
        printStatusBtn.addActionListener(e -> printMarchStatus());

        panel.add(refreshBtn);
        panel.add(clearCompletedBtn);
        panel.add(printStatusBtn);

        return panel;
    }

    private void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
        }
    }

    private void startUpdateTimer() {
        updateTimer = new Timer(1000, e -> updateDisplay());
        updateTimer.start();
    }

    public void addMarch(int instanceIndex, int queueNumber, String resourceType, 
                        String gatheringTime, String marchingTime, String totalTime) {
        
        String marchId = instanceIndex + "-" + queueNumber;
        LocalDateTime startTime = LocalDateTime.now();
        
        ActiveMarch march = new ActiveMarch(
            instanceIndex, queueNumber, resourceType, 
            gatheringTime, marchingTime, totalTime, startTime
        );
        
        activeMarches.put(marchId, march);
        
        SwingUtilities.invokeLater(() -> {
            updateTableData();
            statusLabel.setText("Added march: " + resourceType + " on Instance " + instanceIndex + " Queue " + queueNumber);
        });
        
        System.out.println("📊 March Tracker: Added " + march);
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
                updateTableData();
                statusLabel.setText("Updated march status: " + newStatus);
            });
        }
    }

    public void completeMarch(int instanceIndex, int queueNumber) {
        String marchId = instanceIndex + "-" + queueNumber;
        ActiveMarch march = activeMarches.remove(marchId);
        
        if (march != null) {
            march.setStatus("Completed");
            completedMarches.add(march);
            SwingUtilities.invokeLater(() -> {
                updateTableData();
                statusLabel.setText("March completed: " + march.getResourceType());
            });
        }
    }

    public void removeMarch(int instanceIndex, int queueNumber) {
        String marchId = instanceIndex + "-" + queueNumber;
        ActiveMarch removed = activeMarches.remove(marchId);
        
        if (removed != null) {
            SwingUtilities.invokeLater(() -> {
                updateTableData();
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
            updateTableData();
            statusLabel.setText("All marches cleared");
        });
        System.out.println("🧹 March tracker cleared");
    }

    public void printMarchStatus() {
        System.out.println("📊 === MARCH TRACKER STATUS ===");
        System.out.println("Active marches: " + activeMarches.size());
        
        for (ActiveMarch march : activeMarches.values()) {
            System.out.println("  🚀 " + march);
        }
        
        System.out.println("Completed marches: " + completedMarches.size());
        System.out.println("================================");
    }

    private void updateDisplay() {
        SwingUtilities.invokeLater(() -> {
            updateTableData();
            updateStatusLabels();
        });
    }

    private void updateTableData() {
        tableModel.setRowCount(0);
        
        for (ActiveMarch march : activeMarches.values()) {
            Object[] row = {
                "Instance " + march.getInstanceIndex(),
                "Queue " + march.getQueueNumber(),
                march.getResourceType(),
                march.getStatus(),
                formatTimeRemaining(march.getTimeRemaining()),
                Math.round(march.getProgressPercentage())
            };
            tableModel.addRow(row);
        }
    }

    private void updateStatusLabels() {
        int totalMarches = activeMarches.size();
        int gathering = 0;
        int marching = 0;
        int completed = 0;

        for (ActiveMarch march : activeMarches.values()) {
            switch (march.getStatus()) {
                case "Gathering": gathering++; break;
                case "Returning": marching++; break;
                case "Completed": completed++; break;
            }
        }

        totalMarchesLabel.setText(String.format("Total: %d | Gathering: %d | Returning: %d | Completed: %d", 
            totalMarches, gathering, marching, completed));
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
        statusLabel.setText("Refreshed march data");
    }

    private void clearCompletedMarches() {
        activeMarches.entrySet().removeIf(entry -> 
            entry.getValue().getStatus().equals("Completed") || 
            entry.getValue().getTimeRemaining() <= 0
        );
        updateTableData();
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