JTextArea helpText = new JTextArea(
            "Troop Training Help:\n\n" +
            "• Balanced Army: Good for general gameplay and defense\n" +
            "• Focused Builds: Better for specific strategies (PvP, gathering)\n" +
            "• Resource Allocation: Higher % = faster growth, lower % = more resources for other activities\n\n" +
            "Training Tips:\n" +
            "• Background training during gathering maximizes efficiency\n" +
            "• Adjust composition based on your alliance's needs\n" +
            "• Higher tier troops are stronger but cost more resources"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(helpText, gbc);

        return panel;
    }

    private JPanel createDailyTasksPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Enable Daily Tasks
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JCheckBox enableDailyTasks = new JCheckBox("Enable Automatic Daily Tasks & VIP Activities");
        enableDailyTasks.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(enableDailyTasks, gbc);
        settingsComponents.put("dailyTasksEnabled", enableDailyTasks);

        // Task types
        gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel taskTypesPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        taskTypesPanel.setBorder(BorderFactory.createTitledBorder("Task Types"));
        
        JCheckBox dailyQuests = new JCheckBox("Daily Quests & Missions");
        JCheckBox vipPoints = new JCheckBox("VIP Point Collection");
        JCheckBox allianceHelp = new JCheckBox("Alliance Help Requests");
        JCheckBox freeChests = new JCheckBox("Free Chests & Rewards");
        JCheckBox loginRewards = new JCheckBox("Login Bonuses");
        JCheckBox eventTasks = new JCheckBox("Limited Time Events");
        
        dailyQuests.setSelected(true);
        vipPoints.setSelected(true);
        loginRewards.setSelected(true);
        freeChests.setSelected(true);
        
        taskTypesPanel.add(dailyQuests);
        taskTypesPanel.add(vipPoints);
        taskTypesPanel.add(allianceHelp);
        taskTypesPanel.add(freeChests);
        taskTypesPanel.add(loginRewards);
        taskTypesPanel.add(eventTasks);
        
        panel.add(taskTypesPanel, gbc);
        settingsComponents.put("dailyQuests", dailyQuests);
        settingsComponents.put("vipPoints", vipPoints);
        settingsComponents.put("allianceHelp", allianceHelp);
        settingsComponents.put("freeChests", freeChests);
        settingsComponents.put("loginRewards", loginRewards);
        settingsComponents.put("eventTasks", eventTasks);

        // Execution timing
        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Run daily tasks:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JComboBox<String> taskTiming = new JComboBox<>(new String[]{
            "At startup (before other modules)",
            "Between gathering cycles", 
            "At end of automation cycle",
            "Every 4 hours",
            "Custom schedule"
        });
        taskTiming.setSelectedIndex(1); // Between cycles
        panel.add(taskTiming, gbc);
        settingsComponents.put("taskTiming", taskTiming);

        // Safety settings
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel safetyPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        safetyPanel.setBorder(BorderFactory.createTitledBorder("Safety Settings"));
        
        JCheckBox avoidSpending = new JCheckBox("Avoid spending premium currency (gems)");
        JCheckBox skipRisky = new JCheckBox("Skip tasks that might affect gameplay (research, building)");
        JCheckBox confirmImportant = new JCheckBox("Require confirmation for important rewards");
        
        avoidSpending.setSelected(true);
        skipRisky.setSelected(true);
        
        safetyPanel.add(avoidSpending);
        safetyPanel.add(skipRisky);
        safetyPanel.add(confirmImportant);
        
        panel.add(safetyPanel, gbc);
        settingsComponents.put("avoidSpending", avoidSpending);
        settingsComponents.put("skipRisky", skipRisky);
        settingsComponents.put("confirmImportant", confirmImportant);

        // Help text
        gbc.gridy++; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "Daily Tasks Help:\n\n" +
            "• Daily Quests: Complete objectives for rewards\n" +
            "• VIP Points: Collect daily VIP benefits and bonuses\n" +
            "• Alliance Help: Automatically help alliance members\n" +
            "• Free Chests: Claim free rewards and bonuses\n\n" +
            "Safety Features:\n" +
            "• Premium currency protection prevents accidental spending\n" +
            "• Skip risky tasks to avoid interfering with your strategy\n" +
            "• Confirmation for important items gives you control"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(helpText, gbc);

        return panel;
    }

    private JPanel createSafetyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Anti-detection features
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel detectionTitle = new JLabel("🛡️ Anti-Detection Features");
        detectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(detectionTitle, gbc);

        gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel detectionPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        
        JCheckBox randomDelays = new JCheckBox("Use random delays between actions (1-5 seconds)");
        JCheckBox humanBehavior = new JCheckBox("Simulate human behavior patterns");
        JCheckBox breakPeriods = new JCheckBox("Take random breaks (5-15 minutes every 2-4 hours)");
        JCheckBox activityMasking = new JCheckBox("Vary activity patterns daily");
        
        randomDelays.setSelected(true);
        humanBehavior.setSelected(true);
        breakPeriods.setSelected(true);
        
        detectionPanel.add(randomDelays);
        detectionPanel.add(humanBehavior);
        detectionPanel.add(breakPeriods);
        detectionPanel.add(activityMasking);
        
        panel.add(detectionPanel, gbc);
        settingsComponents.put("randomDelays", randomDelays);
        settingsComponents.put("humanBehavior", humanBehavior);
        settingsComponents.put("breakPeriods", breakPeriods);
        settingsComponents.put("activityMasking", activityMasking);

        // Emergency stops
        gbc.gridy++; gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(10), gbc);
        
        gbc.gridy++; gbc.gridwidth = 2;
        JLabel emergencyTitle = new JLabel("🚨 Emergency Stop Conditions");
        emergencyTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(emergencyTitle, gbc);

        gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel emergencyPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        
        JCheckBox stopOnAttack = new JCheckBox("Stop all automation if under attack");
        JCheckBox stopOnLowResources = new JCheckBox("Stop when resources below 10% storage");
        JCheckBox stopOnErrors = new JCheckBox("Stop after 3 consecutive errors");
        JCheckBox stopOnUnknownScreens = new JCheckBox("Stop on unrecognized game screens");
        
        stopOnAttack.setSelected(true);
        stopOnErrors.setSelected(true);
        stopOnUnknownScreens.setSelected(true);
        
        emergencyPanel.add(stopOnAttack);
        emergencyPanel.add(stopOnLowResources);
        emergencyPanel.add(stopOnErrors);
        emergencyPanel.add(stopOnUnknownScreens);
        
        panel.add(emergencyPanel, gbc);
        settingsComponents.put("stopOnAttack", stopOnAttack);
        settingsComponents.put("stopOnLowResources", stopOnLowResources);
        settingsComponents.put("stopOnErrors", stopOnErrors);
        settingsComponents.put("stopOnUnknownScreens", stopOnUnknownScreens);

        // Performance settings
        gbc.gridy++; gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(10), gbc);
        
        gbc.gridy++; gbc.gridwidth = 2;
        JLabel performanceTitle = new JLabel("⚡ Performance & Resource Management");
        performanceTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(performanceTitle, gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("CPU usage limit:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JComboBox<String> cpuLimit = new JComboBox<>(new String[]{
            "Low (20%) - Minimal impact", "Medium (50%) - Balanced", "High (80%) - Fast execution", "No limit"
        });
        cpuLimit.setSelectedIndex(1);
        panel.add(cpuLimit, gbc);
        settingsComponents.put("cpuLimit", cpuLimit);

        gbc.gridy++; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Screenshot frequency:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JComboBox<String> screenshotFreq = new JComboBox<>(new String[]{
            "Minimal (only when needed)", "Normal (balanced)", "Frequent (more responsive)", "Continuous (debugging)"
        });
        screenshotFreq.setSelectedIndex(1);
        panel.add(screenshotFreq, gbc);
        settingsComponents.put("screenshotFreq", screenshotFreq);

        // Help text
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "Safety & Timing Help:\n\n" +
            "Anti-Detection:\n" +
            "• Random delays make automation look more human\n" +
            "• Break periods prevent 24/7 activity patterns\n" +
            "• Activity masking varies timing to avoid detection\n\n" +
            "Emergency Stops:\n" +
            "• Attack detection protects your army and resources\n" +
            "• Error limits prevent infinite failure loops\n" +
            "• Unknown screen detection prevents dangerous actions\n\n" +
            "Performance:\n" +
            "• Lower CPU usage = less system impact but slower response\n" +
            "• Screenshot frequency affects responsiveness vs resource usage"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(helpText, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton testButton = new JButton("Test Settings");
        testButton.setToolTipText("Test the configuration without saving");
        testButton.addActionListener(e -> testConfiguration());

        JButton saveButton = new JButton("Save Configuration");
        saveButton.setBackground(new Color(34, 139, 34));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveButton.addActionListener(e -> saveConfiguration());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        panel.add(testButton);
        panel.add(cancelButton);
        panel.add(saveButton);

        return panel;
    }

    private void loadCurrentSettings() {
        // Load existing settings for this instance
        Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new HashMap<>());
        
        // Load gathering settings
        ModuleState<?> gatherModule = modules.get("Auto Gather Resources");
        boolean gatheringEnabled = gatherModule != null && gatherModule.enabled;
        ((JCheckBox) settingsComponents.get("gatheringEnabled")).setSelected(gatheringEnabled);
        
        if (gatherModule != null && gatherModule.settings != null) {
            // Parse existing settings and populate UI
            // This would parse the settings string and set the appropriate UI components
        }
        
        // Load gift settings
        ModuleState<?> giftModule = modules.get("Auto Gift Claim");
        boolean giftsEnabled = giftModule != null && giftModule.enabled;
        ((JCheckBox) settingsComponents.get("giftsEnabled")).setSelected(giftsEnabled);
        
        // Load building settings
        ModuleState<?> buildingModule = modules.get("Auto Building & Upgrades");
        boolean buildingEnabled = buildingModule != null && buildingModule.enabled;
        if (settingsComponents.containsKey("buildingEnabled")) {
            ((JCheckBox) settingsComponents.get("buildingEnabled")).setSelected(buildingEnabled);
        }
        
        // Load troop settings
        ModuleState<?> troopModule = modules.get("Auto Troop Training");
        boolean troopsEnabled = troopModule != null && troopModule.enabled;
        if (settingsComponents.containsKey("troopsEnabled")) {
            ((JCheckBox) settingsComponents.get("troopsEnabled")).setSelected(troopsEnabled);
        }
        
        // Load daily tasks settings
        ModuleState<?> dailyModule = modules.get("Auto Daily Tasks");
        boolean dailyEnabled = dailyModule != null && dailyModule.enabled;
        if (settingsComponents.containsKey("dailyTasksEnabled")) {
            ((JCheckBox) settingsComponents.get("dailyTasksEnabled")).setSelected(dailyEnabled);
        }
        
        // Set defaults for other components
        ((JSpinner) settingsComponents.get("maxQueues")).setValue(6);
        ((JComboBox<?>) settingsComponents.get("levelPreference")).setSelectedIndex(0);
        ((JComboBox<?>) settingsComponents.get("gatheringStrategy")).setSelectedIndex(0);
        ((JComboBox<?>) settingsComponents.get("giftFrequency")).setSelectedIndex(1);
    }

    private void testConfiguration() {
        try {
            if (!validateSettings()) {
                return;
            }
            
            StringBuilder testResults = new StringBuilder();
            testResults.append("Configuration Test Results:\n");
            testResults.append("=".repeat(50)).append("\n\n");
            
            // Module Priority Analysis
            testResults.append("⚡ MODULE EXECUTION ORDER:\n");
            @SuppressWarnings("unchecked")
            JList<String> moduleOrder = (JList<String>) settingsComponents.get("moduleOrder");
            for (int i = 0; i < moduleOrder.getModel().getSize(); i++) {
                String module = moduleOrder.getModel().getElementAt(i);
                testResults.append("  ").append(i + 1).append(". ").append(module).append("\n");
            }
            
            int moduleWait = (Integer) ((JSpinner) settingsComponents.get("moduleWait")).getValue();
            testResults.append("  • Wait between modules: ").append(moduleWait).append(" seconds\n");
            
            boolean sequential = ((JRadioButton) settingsComponents.get("sequential")).isSelected();
            testResults.append("  • Execution mode: ").append(sequential ? "Sequential" : "Parallel/Adaptive").append("\n\n");
            
            // Module Analysis
            testResults.append("🔧 ENABLED MODULES:\n");
            
            boolean buildingEnabled = false;
            if (settingsComponents.containsKey("buildingEnabled")) {
                buildingEnabled = ((JCheckBox) settingsComponents.get("buildingEnabled")).isSelected();
            }
            if (buildingEnabled) {
                String buildingPriority = (String) ((JComboBox<?>) settingsComponents.get("buildingPriority")).getSelectedItem();
                String resourceThreshold = (String) ((JComboBox<?>) settingsComponents.get("resourceThreshold")).getSelectedItem();
                testResults.append("🏗️ Building & Upgrades: ENABLED\n");
                testResults.append("  • Priority: ").append(buildingPriority).append("\n");
                testResults.append("  • Resource threshold: ").append(resourceThreshold).append("\n");
            }
            
            boolean troopsEnabled = false;
            if (settingsComponents.containsKey("troopsEnabled")) {
                troopsEnabled = ((JCheckBox) settingsComponents.get("troopsEnabled")).isSelected();
            }
            if (troopsEnabled) {
                String composition = (String) ((JComboBox<?>) settingsComponents.get("troopComposition")).getSelectedItem();
                int resourcePercent = (Integer) ((JSpinner) settingsComponents.get("resourcePercent")).getValue();
                testResults.append("⚔️ Troop Training: ENABLED\n");
                testResults.append("  • Composition: ").append(composition).append("\n");
                testResults.append("  • Resource allocation: ").append(resourcePercent).append("% of resources\n");
            }
            
            boolean gatheringEnabled = ((JCheckBox) settingsComponents.get("gatheringEnabled")).isSelected();
            if (gatheringEnabled) {
                int maxQueues = (Integer) ((JSpinner) settingsComponents.get("maxQueues")).getValue();
                String strategy = (String) ((JComboBox<?>) settingsComponents.get("gatheringStrategy")).getSelectedItem();
                testResults.append("🌾 Resource Gathering: ENABLED\n");
                testResults.append("  • Max queues: ").append(maxQueues).append("\n");
                testResults.append("  • Strategy: ").append(strategy).append("\n");
            }
            
            boolean giftsEnabled = ((JCheckBox) settingsComponents.get("giftsEnabled")).isSelected();
            if (giftsEnabled) {
                String frequency = (String) ((JComboBox<?>) settingsComponents.get("giftFrequency")).getSelectedItem();
                testResults.append("🎁 Gift Claiming: ENABLED\n");
                testResults.append("  • Frequency: ").append(frequency).append("\n");
            }
            
            boolean dailyTasksEnabled = false;
            if (settingsComponents.containsKey("dailyTasksEnabled")) {
                dailyTasksEnabled = ((JCheckBox) settingsComponents.get("dailyTasksEnabled")).isSelected();
            }
            if (dailyTasksEnabled) {
            String dailySettings = buildDailyTasksSettingsString();
            modules.put("Auto Daily Tasks", new ModuleState<>(true, dailySettings));
        } else {
            modules.put("Auto Daily Tasks", new ModuleState<>(false, null));
        }
        
        // Save Module Priority & Safety settings
        String prioritySettings = buildPrioritySettingsString();
        modules.put("Module Priority", new ModuleState<>(true, prioritySettings));
        
        String safetySettings = buildSafetySettingsString();
        modules.put("Safety Settings", new ModuleState<>(true, safetySettings));
    }

    private String buildBuildingSettingsString() {
        StringBuilder settings = new StringBuilder();
        
        String priority = (String) ((JComboBox<?>) settingsComponents.get("buildingPriority")).getSelectedItem();
        settings.append("Priority:").append(priority).append(";");
        
        String threshold = (String) ((JComboBox<?>) settingsComponents.get("resourceThreshold")).getSelectedItem();
        settings.append("ResourceThreshold:").append(threshold).append(";");
        
        int maxBuildings = (Integer) ((JSpinner) settingsComponents.get("maxBuildings")).getValue();
        settings.append("MaxQueue:").append(maxBuildings).append(";");
        
        boolean upgradeExisting = ((JCheckBox) settingsComponents.get("upgradeExisting")).isSelected();
        settings.append("UpgradeExisting:").append(upgradeExisting).append(";");
        
        boolean balanceBuildings = ((JCheckBox) settingsComponents.get("balanceBuildings")).isSelected();
        settings.append("BalanceBuildings:").append(balanceBuildings).append(";");
        
        return settings.toString();
    }

    private String buildTroopSettingsString() {
        StringBuilder settings = new StringBuilder();
        
        String composition = (String) ((JComboBox<?>) settingsComponents.get("troopComposition")).getSelectedItem();
        settings.append("Composition:").append(composition).append(";");
        
        String priority = (String) ((JComboBox<?>) settingsComponents.get("trainingPriority")).getSelectedItem();
        settings.append("TrainingPriority:").append(priority).append(";");
        
        int maxTraining = (Integer) ((JSpinner) settingsComponents.get("maxTraining")).getValue();
        settings.append("MaxQueue:").append(maxTraining).append(";");
        
        int resourcePercent = (Integer) ((JSpinner) settingsComponents.get("resourcePercent")).getValue();
        settings.append("ResourcePercent:").append(resourcePercent).append(";");
        
        boolean trainWhenIdle = ((JCheckBox) settingsComponents.get("trainWhenIdle")).isSelected();
        settings.append("TrainWhenIdle:").append(trainWhenIdle).append(";");
        
        boolean trainDuringGathering = ((JCheckBox) settingsComponents.get("trainDuringGathering")).isSelected();
        settings.append("TrainDuringGathering:").append(trainDuringGathering).append(";");
        
        return settings.toString();
    }

    private String buildGatheringSettingsString() {
        StringBuilder settings = new StringBuilder();
        
        // Resource loop - for now, use default order
        settings.append("Loop:Food,Wood,Stone,Iron;");
        
        // Max queues
        int maxQueues = (Integer) ((JSpinner) settingsComponents.get("maxQueues")).getValue();
        settings.append("MaxQueues:").append(maxQueues).append(";");
        
        // Level preference
        String levelPref = (String) ((JComboBox<?>) settingsComponents.get("levelPreference")).getSelectedItem();
        if (levelPref.contains("Highest")) {
            settings.append("LevelStrategy:HighToLow;");
        } else if (levelPref.contains("Medium")) {
            settings.append("LevelStrategy:MediumRange;");
        } else if (levelPref.contains("Low")) {
            settings.append("LevelStrategy:LowLevels;");
        } else if (levelPref.contains("Specific")) {
            int specificLevel = (Integer) ((JSpinner) settingsComponents.get("specificLevel")).getValue();
            settings.append("LevelStrategy:Specific;SpecificLevel:").append(specificLevel).append(";");
        }
        
        // Gathering strategy
        String strategy = (String) ((JComboBox<?>) settingsComponents.get("gatheringStrategy")).getSelectedItem();
        if (strategy.contains("Balanced")) {
            settings.append("Strategy:Balanced;");
        } else if (strategy.contains("Focus")) {
            settings.append("Strategy:Focus;");
        } else if (strategy.contains("Adaptive")) {
            settings.append("Strategy:Adaptive;");
        }
        
        // Hibernation - always enabled by default
        settings.append("Hibernation:true;");
        settings.append("MinHibernationMinutes:5;");
        
        return settings.toString();
    }

    private String buildGiftSettingsString() {
        StringBuilder settings = new StringBuilder();
        
        // Frequency
        String frequency = (String) ((JComboBox<?>) settingsComponents.get("giftFrequency")).getSelectedItem();
        settings.append("Frequency:").append(frequency).append(";");
        
        // Gift types
        if (((JCheckBox) settingsComponents.get("dailyGifts")).isSelected()) {
            settings.append("DailyGifts:true;");
        }
        if (((JCheckBox) settingsComponents.get("mailGifts")).isSelected()) {
            settings.append("MailGifts:true;");
        }
        if (((JCheckBox) settingsComponents.get("eventGifts")).isSelected()) {
            settings.append("EventGifts:true;");
        }
        if (((JCheckBox) settingsComponents.get("allianceGifts")).isSelected()) {
            settings.append("AllianceGifts:true;");
        }
        if (((JCheckBox) settingsComponents.get("vipGifts")).isSelected()) {
            settings.append("VipGifts:true;");
        }
        if (((JCheckBox) settingsComponents.get("questGifts")).isSelected()) {
            settings.append("QuestGifts:true;");
        }
        
        // Safety settings
        if (((JCheckBox) settingsComponents.get("skipUnknown")).isSelected()) {
            settings.append("SkipUnknown:true;");
        }
        if (((JCheckBox) settingsComponents.get("limitAttempts")).isSelected()) {
            settings.append("LimitAttempts:true;");
        }
        
        return settings.toString();
    }

    private String buildDailyTasksSettingsString() {
        StringBuilder settings = new StringBuilder();
        
        String timing = (String) ((JComboBox<?>) settingsComponents.get("taskTiming")).getSelectedItem();
        settings.append("Timing:").append(timing).append(";");
        
        // Task types
        if (((JCheckBox) settingsComponents.get("dailyQuests")).isSelected()) {
            settings.append("DailyQuests:true;");
        }
        if (((JCheckBox) settingsComponents.get("vipPoints")).isSelected()) {
            settings.append("VipPoints:true;");
        }
        if (((JCheckBox) settingsComponents.get("allianceHelp")).isSelected()) {
            settings.append("AllianceHelp:true;");
        }
        if (((JCheckBox) settingsComponents.get("freeChests")).isSelected()) {
            settings.append("FreeChests:true;");
        }
        if (((JCheckBox) settingsComponents.get("loginRewards")).isSelected()) {
            settings.append("LoginRewards:true;");
        }
        
        // Safety
        if (((JCheckBox) settingsComponents.get("avoidSpending")).isSelected()) {
            settings.append("AvoidSpending:true;");
        }
        if (((JCheckBox) settingsComponents.get("skipRisky")).isSelected()) {
            settings.append("SkipRisky:true;");
        }
        
        return settings.toString();
    }

    private String buildPrioritySettingsString() {
        StringBuilder settings = new StringBuilder();
        
        // Module execution order
        @SuppressWarnings("unchecked")
        JList<String> moduleOrder = (JList<String>) settingsComponents.get("moduleOrder");
        StringBuilder orderList = new StringBuilder();
        for (int i = 0; i < moduleOrder.getModel().getSize(); i++) {
            if (i > 0) orderList.append(",");
            orderList.append(moduleOrder.getModel().getElementAt(i));
        }
        settings.append("ModuleOrder:").append(orderList.toString()).append(";");
        
        int moduleWait = (Integer) ((JSpinner) settingsComponents.get("moduleWait")).getValue();
        settings.append("ModuleWait:").append(moduleWait).append(";");
        
        boolean sequential = ((JRadioButton) settingsComponents.get("sequential")).isSelected();
        settings.append("ExecutionMode:").append(sequential ? "Sequential" : "Parallel").append(";");
        
        boolean retryFailed = ((JCheckBox) settingsComponents.get("retryFailed")).isSelected();
        settings.append("RetryFailed:").append(retryFailed).append(";");
        
        return settings.toString();
    }

    private String buildSafetySettingsString() {
        StringBuilder settings = new StringBuilder();
        
        // Anti-detection
        if (((JCheckBox) settingsComponents.get("randomDelays")).isSelected()) {
            settings.append("RandomDelays:true;");
        }
        if (((JCheckBox) settingsComponents.get("humanBehavior")).isSelected()) {
            settings.append("HumanBehavior:true;");
        }
        if (((JCheckBox) settingsComponents.get("breakPeriods")).isSelected()) {
            settings.append("BreakPeriods:true;");
        }
        
        // Emergency stops
        if (((JCheckBox) settingsComponents.get("stopOnAttack")).isSelected()) {
            settings.append("StopOnAttack:true;");
        }
        if (((JCheckBox) settingsComponents.get("stopOnErrors")).isSelected()) {
            settings.append("StopOnErrors:true;");
        }
        
        // Performance
        String cpuLimit = (String) ((JComboBox<?>) settingsComponents.get("cpuLimit")).getSelectedItem();
        settings.append("CpuLimit:").append(cpuLimit).append(";");
        
        String screenshotFreq = (String) ((JComboBox<?>) settingsComponents.get("screenshotFreq")).getSelectedItem();
        settings.append("ScreenshotFreq:").append(screenshotFreq).append(";");
        
        return settings.toString();
    }
}sEnabled) {
                String timing = (String) ((JComboBox<?>) settingsComponents.get("taskTiming")).getSelectedItem();
                testResults.append("📋 Daily Tasks: ENABLED\n");
                testResults.append("  • Timing: ").append(timing).append("\n");
            }
            
            testResults.append("\n🛡️ SAFETY FEATURES:\n");
            boolean randomDelays = ((JCheckBox) settingsComponents.get("randomDelays")).isSelected();
            boolean breakPeriods = ((JCheckBox) settingsComponents.get("breakPeriods")).isSelected();
            boolean stopOnAttack = ((JCheckBox) settingsComponents.get("stopOnAttack")).isSelected();
            
            testResults.append("  • Random delays: ").append(randomDelays ? "ENABLED" : "DISABLED").append("\n");
            testResults.append("  • Break periods: ").append(breakPeriods ? "ENABLED" : "DISABLED").append("\n");
            testResults.append("  • Stop on attack: ").append(stopOnAttack ? "ENABLED" : "DISABLED").append("\n");
            
            testResults.append("\n📋 EXECUTION PLAN:\n");
            testResults.append("When you start this instance, it will:\n");
            testResults.append("1. 🎮 Start the game automatically\n");
            
            int step = 2;
            if (buildingEnabled) {
                testResults.append(step++).append(". 🏗️ Check for building upgrades\n");
            }
            if (troopsEnabled) {
                testResults.append(step++).append(". ⚔️ Begin troop training\n");
            }
            if (giftsEnabled) {
                testResults.append(step++).append(". 🎁 Claim available gifts\n");
            }
            if (gatheringEnabled) {
                testResults.append(step++).append(". 🌾 Start resource gathering\n");
                testResults.append(step++).append(". 😴 Hibernate during long marches\n");
            }
            if (dailyTasksEnabled) {
                testResults.append(step++).append(". 📋 Complete daily tasks\n");
            }
            testResults.append(step).append(". 🔄 Repeat cycle\n");
            
            JTextArea resultArea = new JTextArea(testResults.toString());
            resultArea.setEditable(false);
            resultArea.setFont(new Font("Consolas", Font.PLAIN, 11));
            
            JScrollPane scrollPane = new JScrollPane(resultArea);
            scrollPane.setPreferredSize(new Dimension(600, 500));
            
            JOptionPane.showMessageDialog(this, scrollPane, 
                "Configuration Test - " + instance.name, 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error testing configuration: " + e.getMessage(),
                "Test Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateSettings() {
        // Validate max queues
        if (settingsComponents.containsKey("maxQueues")) {
            int maxQueues = (Integer) ((JSpinner) settingsComponents.get("maxQueues")).getValue();
            if (maxQueues < 1 || maxQueues > 6) {
                JOptionPane.showMessageDialog(this, 
                    "Maximum queues must be between 1 and 6.",
                    "Invalid Settings", 
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        
        // Validate troop resource allocation
        if (settingsComponents.containsKey("resourcePercent")) {
            int resourcePercent = (Integer) ((JSpinner) settingsComponents.get("resourcePercent")).getValue();
            if (resourcePercent < 5 || resourcePercent > 95) {
                JOptionPane.showMessageDialog(this, 
                    "Resource allocation must be between 5% and 95%.",
                    "Invalid Settings", 
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        
        // Check if at least one module is enabled
        boolean anyEnabled = false;
        String[] moduleKeys = {"buildingEnabled", "troopsEnabled", "gatheringEnabled", "giftsEnabled", "dailyTasksEnabled"};
        for (String key : moduleKeys) {
            if (settingsComponents.containsKey(key)) {
                JCheckBox checkbox = (JCheckBox) settingsComponents.get(key);
                if (checkbox.isSelected()) {
                    anyEnabled = true;
                    break;
                }
            }
        }
        
        if (!anyEnabled) {
            int result = JOptionPane.showConfirmDialog(this,
                "No automation modules are enabled. This will disable all automation\n" +
                "except Auto Start Game. Continue?",
                "No Modules Enabled",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            return result == JOptionPane.YES_OPTION;
        }
        
        return true;
    }

    private void saveConfiguration() {
        try {
            if (!validateSettings()) {
                return;
            }
            
            // Get or create modules map for this instance
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new HashMap<>());
            
            // Always ensure Auto Start Game is enabled (hidden from user)
            modules.put("Auto Start Game", new ModuleState<>(true, null));
            
            // Save all module settings
            saveModuleSettings(modules);
            
            // Save to main modules map
            Main.instanceModules.put(instance.index, modules);
            main.saveSettings();
            
            // Show success message
            StringBuilder summary = new StringBuilder();
            summary.append("Configuration saved successfully!\n\n");
            
            // Count enabled modules
            int enabledCount = 0;
            if (settingsComponents.containsKey("buildingEnabled") && ((JCheckBox) settingsComponents.get("buildingEnabled")).isSelected()) enabledCount++;
            if (settingsComponents.containsKey("troopsEnabled") && ((JCheckBox) settingsComponents.get("troopsEnabled")).isSelected()) enabledCount++;
            if (((JCheckBox) settingsComponents.get("gatheringEnabled")).isSelected()) enabledCount++;
            if (((JCheckBox) settingsComponents.get("giftsEnabled")).isSelected()) enabledCount++;
            if (settingsComponents.containsKey("dailyTasksEnabled") && ((JCheckBox) settingsComponents.get("dailyTasksEnabled")).isSelected()) enabledCount++;
            
            summary.append("🎮 Auto Start Game: Always enabled\n");
            summary.append("🔧 Automation modules: ").append(enabledCount).append(" enabled\n");
            summary.append("⚡ Module priority: Configured\n");
            summary.append("🛡️ Safety features: Configured\n");
            
            if (enabledCount > 0) {
                summary.append("\nTo activate these settings, start the instance using the\n");
                summary.append("'Start Selected' button in the main window.\n\n");
                summary.append("The modules will run in the priority order you specified.");
            }
            
            JOptionPane.showMessageDialog(this, summary.toString(), 
                "Settings Saved - " + instance.name, 
                JOptionPane.INFORMATION_MESSAGE);
            
            dispose();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error saving configuration: " + e.getMessage(),
                "Save Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveModuleSettings(Map<String, ModuleState<?>> modules) {
        // Save Building & Upgrades
        boolean buildingEnabled = false;
        if (settingsComponents.containsKey("buildingEnabled")) {
            buildingEnabled = ((JCheckBox) settingsComponents.get("buildingEnabled")).isSelected();
        }
        if (buildingEnabled) {
            String buildingSettings = buildBuildingSettingsString();
            modules.put("Auto Building & Upgrades", new ModuleState<>(true, buildingSettings));
        } else {
            modules.put("Auto Building & Upgrades", new ModuleState<>(false, null));
        }
        
        // Save Troop Training
        boolean troopsEnabled = false;
        if (settingsComponents.containsKey("troopsEnabled")) {
            troopsEnabled = ((JCheckBox) settingsComponents.get("troopsEnabled")).isSelected();
        }
        if (troopsEnabled) {
            String troopSettings = buildTroopSettingsString();
            modules.put("Auto Troop Training", new ModuleState<>(true, troopSettings));
        } else {
            modules.put("Auto Troop Training", new ModuleState<>(false, null));
        }
        
        // Save Resource Gathering (existing)
        boolean gatheringEnabled = ((JCheckBox) settingsComponents.get("gatheringEnabled")).isSelected();
        if (gatheringEnabled) {
            String gatherSettings = buildGatheringSettingsString();
            modules.put("Auto Gather Resources", new ModuleState<>(true, gatherSettings));
        } else {
            modules.put("Auto Gather Resources", new ModuleState<>(false, null));
        }
        
        // Save Gift Claiming (existing)
        boolean giftsEnabled = ((JCheckBox) settingsComponents.get("giftsEnabled")).isSelected();
        if (giftsEnabled) {
            String giftSettings = buildGiftSettingsString();
            modules.put("Auto Gift Claim", new ModuleState<>(true, giftSettings));
        } else {
            modules.put("Auto Gift Claim", new ModuleState<>(false, null));
        }
        
        // Save Daily Tasks
        boolean dailyTasksEnabled = false;
        if (settingsComponents.containsKey("dailyTasksEnabled")) {
            dailyTasksEnabled = ((JCheckBox) settingsComponents.get("dailyTasksEnabled")).isSelected();
        }
        if (dailyTaskpackage newgame;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Enhanced module configuration dialog with detailed settings
 */
public class DetailedModuleDialog extends JDialog {
    private final Main main;
    private final MemuInstance instance;
    private final Map<String, JComponent> settingsComponents = new HashMap<>();

    public DetailedModuleDialog(Main main, MemuInstance instance) {
        super(main, "Configure Automation - " + instance.name, true);
        this.main = main;
        this.instance = instance;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(700, 600);
        setLocationRelativeTo(getParent());

        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Module Priority Tab - NEW!
        tabbedPane.addTab("⚡ Module Priority", createModulePriorityPanel());
        
        // Auto Gather Resources Tab
        tabbedPane.addTab("🌾 Resource Gathering", createGatheringPanel());
        
        // Auto Gift Claim Tab
        tabbedPane.addTab("🎁 Gift Claiming", createGiftClaimPanel());
        
        // Auto Build & Upgrade Tab - NEW!
        tabbedPane.addTab("🏗️ Building & Upgrades", createBuildingPanel());
        
        // Auto Train Troops Tab - NEW!
        tabbedPane.addTab("⚔️ Troop Training", createTroopTrainingPanel());
        
        // Auto VIP & Daily Tasks Tab - NEW!
        tabbedPane.addTab("📋 Daily Tasks", createDailyTasksPanel());
        
        // Safety & Timing Tab - Enhanced
        tabbedPane.addTab("🛡️ Safety & Timing", createSafetyPanel());

        // Bottom buttons
        JPanel buttonPanel = createButtonPanel();

        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        loadCurrentSettings();
    }

    private JPanel createModulePriorityPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Module execution order
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Module Execution Order & Priority");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(titleLabel, gbc);

        gbc.gridy++; gbc.gridwidth = 2;
        JLabel descLabel = new JLabel("Drag modules to reorder execution priority (top = first):");
        panel.add(descLabel, gbc);

        // Module list with drag & drop
        gbc.gridy++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        
        String[] modules = {
            "🎮 Auto Start Game (Always first - hidden)",
            "🏗️ Building & Upgrades", 
            "⚔️ Troop Training",
            "🎁 Gift Claiming",
            "🌾 Resource Gathering", 
            "📋 Daily Tasks",
            "🛡️ Safety Checks"
        };
        
        JList<String> moduleOrderList = new JList<>(modules);
        moduleOrderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moduleOrderList.setDragEnabled(true);
        
        JScrollPane listScroll = new JScrollPane(moduleOrderList);
        listScroll.setPreferredSize(new Dimension(400, 150));
        panel.add(listScroll, gbc);
        settingsComponents.put("moduleOrder", moduleOrderList);

        // Priority settings panel
        gbc.gridy++; gbc.weighty = 0;
        JPanel prioritySettingsPanel = new JPanel(new GridBagLayout());
        prioritySettingsPanel.setBorder(BorderFactory.createTitledBorder("Priority Settings"));
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.insets = new Insets(5, 5, 5, 5);
        pgbc.anchor = GridBagConstraints.WEST;

        // Wait between modules
        pgbc.gridx = 0; pgbc.gridy = 0;
        prioritySettingsPanel.add(new JLabel("Wait between modules:"), pgbc);
        pgbc.gridx = 1;
        JSpinner moduleWait = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        JLabel secondsLabel = new JLabel(" seconds");
        JPanel waitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        waitPanel.add(moduleWait);
        waitPanel.add(secondsLabel);
        prioritySettingsPanel.add(waitPanel, pgbc);
        settingsComponents.put("moduleWait", moduleWait);

        // Retry failed modules
        pgbc.gridy++; pgbc.gridx = 0; pgbc.gridwidth = 2;
        JCheckBox retryFailed = new JCheckBox("Retry failed modules after completing cycle");
        retryFailed.setSelected(true);
        prioritySettingsPanel.add(retryFailed, pgbc);
        settingsComponents.put("retryFailed", retryFailed);

        // Stop on critical failure
        pgbc.gridy++; pgbc.gridwidth = 2;
        JCheckBox stopOnCritical = new JCheckBox("Stop all modules if critical module fails (Auto Start Game)");
        stopOnCritical.setSelected(true);
        prioritySettingsPanel.add(stopOnCritical, pgbc);
        settingsComponents.put("stopOnCritical", stopOnCritical);

        panel.add(prioritySettingsPanel, gbc);

        // Execution strategy
        gbc.gridy++; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel strategyPanel = new JPanel(new GridBagLayout());
        strategyPanel.setBorder(BorderFactory.createTitledBorder("Execution Strategy"));
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5, 5, 5, 5);
        sgbc.anchor = GridBagConstraints.WEST;

        sgbc.gridx = 0; sgbc.gridy = 0; sgbc.gridwidth = 2;
        JRadioButton sequential = new JRadioButton("Sequential - Run modules one after another", true);
        JRadioButton parallel = new JRadioButton("Parallel - Run compatible modules simultaneously");
        JRadioButton adaptive = new JRadioButton("Adaptive - Switch based on game state");

        ButtonGroup strategyGroup = new ButtonGroup();
        strategyGroup.add(sequential);
        strategyGroup.add(parallel);
        strategyGroup.add(adaptive);

        strategyPanel.add(sequential, sgbc);
        sgbc.gridy++;
        strategyPanel.add(parallel, sgbc);
        sgbc.gridy++;
        strategyPanel.add(adaptive, sgbc);

        settingsComponents.put("sequential", sequential);
        settingsComponents.put("parallel", parallel);
        settingsComponents.put("adaptive", adaptive);

        panel.add(strategyPanel, gbc);

        // Help text
        gbc.gridy++; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.4;
        JTextArea helpText = new JTextArea(
            "Module Priority Help:\n\n" +
            "• Sequential: Safest option - modules run in order (recommended)\n" +
            "• Parallel: Faster but may cause conflicts between modules\n" +
            "• Adaptive: Intelligent switching based on what the game needs most\n\n" +
            "Typical Priority Order:\n" +
            "1. Building & Upgrades (when resources available)\n" +
            "2. Troop Training (continuous background activity) \n" +
            "3. Gift Claiming (quick tasks)\n" +
            "4. Resource Gathering (long-running, can hibernate)\n" +
            "5. Daily Tasks (end of cycle cleanup)"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(helpText, gbc);

        return panel;
    }

    private JPanel createGatheringPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Enable Auto Gathering
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JCheckBox enableGathering = new JCheckBox("Enable Automatic Resource Gathering");
        enableGathering.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(enableGathering, gbc);
        settingsComponents.put("gatheringEnabled", enableGathering);

        // Resource Priority
        gbc.gridy++; gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Resource Priority:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        JPanel priorityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        String[] resources = {"Food", "Wood", "Stone", "Iron"};
        JList<String> priorityList = new JList<>(resources);
        priorityList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        priorityList.setVisibleRowCount(4);
        JScrollPane priorityScroll = new JScrollPane(priorityList);
        priorityScroll.setPreferredSize(new Dimension(100, 80));
        priorityPanel.add(priorityScroll);
        
        JPanel priorityButtons = new JPanel(new GridLayout(3, 1, 2, 2));
        JButton moveUp = new JButton("↑");
        JButton moveDown = new JButton("↓");
        JButton reset = new JButton("Reset");
        moveUp.setFont(new Font("Arial", Font.BOLD, 12));
        moveDown.setFont(new Font("Arial", Font.BOLD, 12));
        reset.setFont(new Font("Arial", Font.PLAIN, 10));
        
        priorityButtons.add(moveUp);
        priorityButtons.add(moveDown);
        priorityButtons.add(reset);
        priorityPanel.add(priorityButtons);
        
        panel.add(priorityPanel, gbc);
        settingsComponents.put("resourcePriority", priorityList);

        // Max March Queues
        gbc.gridy++; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Maximum March Queues:"), gbc);
        gbc.gridx = 1;
        JSpinner maxQueues = new JSpinner(new SpinnerNumberModel(6, 1, 6, 1));
        maxQueues.setPreferredSize(new Dimension(60, 25));
        panel.add(maxQueues, gbc);
        settingsComponents.put("maxQueues", maxQueues);

        // Resource Level Preference
        gbc.gridy++; gbc.gridx = 0;
        panel.add(new JLabel("Preferred Resource Level:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> levelPref = new JComboBox<>(new String[]{
            "Highest Available (Level 8→1)", 
            "Medium Levels (Level 5→3)", 
            "Low Levels (Level 3→1)",
            "Specific Level Only"
        });
        panel.add(levelPref, gbc);
        settingsComponents.put("levelPreference", levelPref);

        // Specific Level (when "Specific Level Only" is selected)
        gbc.gridy++; gbc.gridx = 0;
        JLabel specificLevelLabel = new JLabel("Specific Level:");
        panel.add(specificLevelLabel, gbc);
        gbc.gridx = 1;
        JSpinner specificLevel = new JSpinner(new SpinnerNumberModel(7, 1, 8, 1));
        specificLevel.setPreferredSize(new Dimension(60, 25));
        specificLevel.setEnabled(false);
        panel.add(specificLevel, gbc);
        settingsComponents.put("specificLevel", specificLevel);
        settingsComponents.put("specificLevelLabel", specificLevelLabel);

        // Enable/disable specific level based on selection
        levelPref.addActionListener(e -> {
            boolean isSpecific = "Specific Level Only".equals(levelPref.getSelectedItem());
            specificLevel.setEnabled(isSpecific);
            specificLevelLabel.setEnabled(isSpecific);
        });

        // Gathering Strategy
        gbc.gridy++; gbc.gridx = 0;
        panel.add(new JLabel("Gathering Strategy:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> strategy = new JComboBox<>(new String[]{
            "Balanced Rotation (Cycle through all resources)",
            "Resource Focus (Prioritize one resource type)",
            "Smart Adaptive (Based on march completion times)"
        });
        panel.add(strategy, gbc);
        settingsComponents.put("gatheringStrategy", strategy);

        // Help text
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "Resource Gathering Help:\n\n" +
            "• Resource Priority: Drag to reorder which resources to gather first\n" +
            "• Max Queues: How many march slots to use (1-6)\n" +
            "• Level Preference: Which resource levels to target\n" +
            "• Strategy: How to choose between different resources\n\n" +
            "The bot will automatically find available resources and deploy marches\n" +
            "based on your preferences. It will hibernate during long marches to\n" +
            "save system resources."
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(helpText, gbc);

        return panel;
    }

    private JPanel createGiftClaimPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Enable Gift Claiming
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JCheckBox enableGifts = new JCheckBox("Enable Automatic Gift Claiming");
        enableGifts.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(enableGifts, gbc);
        settingsComponents.put("giftsEnabled", enableGifts);

        // Claim Frequency
        gbc.gridy++; gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Check for Gifts Every:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JComboBox<String> frequency = new JComboBox<>(new String[]{
            "30 minutes", "1 hour", "2 hours", "4 hours", "When marches complete"
        });
        panel.add(frequency, gbc);
        settingsComponents.put("giftFrequency", frequency);

        // Gift Types to Claim
        gbc.gridy++; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Gift Types to Claim:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        JPanel giftTypesPanel = new JPanel(new GridLayout(3, 2, 5, 3));
        JCheckBox dailyGifts = new JCheckBox("Daily Login Gifts");
        JCheckBox mailGifts = new JCheckBox("Mail System Gifts");
        JCheckBox eventGifts = new JCheckBox("Event Rewards");
        JCheckBox allianceGifts = new JCheckBox("Alliance Gifts");
        JCheckBox vipGifts = new JCheckBox("VIP Rewards");
        JCheckBox questGifts = new JCheckBox("Quest Completions");
        
        dailyGifts.setSelected(true);
        mailGifts.setSelected(true);
        
        giftTypesPanel.add(dailyGifts);
        giftTypesPanel.add(mailGifts);
        giftTypesPanel.add(eventGifts);
        giftTypesPanel.add(allianceGifts);
        giftTypesPanel.add(vipGifts);
        giftTypesPanel.add(questGifts);
        
        panel.add(giftTypesPanel, gbc);
        settingsComponents.put("dailyGifts", dailyGifts);
        settingsComponents.put("mailGifts", mailGifts);
        settingsComponents.put("eventGifts", eventGifts);
        settingsComponents.put("allianceGifts", allianceGifts);
        settingsComponents.put("vipGifts", vipGifts);
        settingsComponents.put("questGifts", questGifts);

        // Safety Settings
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(10), gbc);
        
        gbc.gridy++; gbc.gridwidth = 2;
        JLabel safetyLabel = new JLabel("Safety Settings:");
        safetyLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(safetyLabel, gbc);

        gbc.gridy++; gbc.gridwidth = 2;
        JCheckBox skipUnknown = new JCheckBox("Skip unknown popup dialogs for safety");
        skipUnknown.setSelected(true);
        panel.add(skipUnknown, gbc);
        settingsComponents.put("skipUnknown", skipUnknown);

        gbc.gridy++; gbc.gridwidth = 2;
        JCheckBox limitAttempts = new JCheckBox("Limit claim attempts (stop after 3 failures)");
        limitAttempts.setSelected(true);
        panel.add(limitAttempts, gbc);
        settingsComponents.put("limitAttempts", limitAttempts);

        // Help text
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "Gift Claiming Help:\n\n" +
            "• The bot will automatically check for and claim gifts\n" +
            "• Choose which types of gifts to claim based on your preferences\n" +
            "• Safety settings help prevent accidental actions\n" +
            "• Gift claiming runs between resource gathering cycles\n\n" +
            "Note: This feature is currently in development. Basic daily\n" +
            "login gifts and mail rewards are supported."
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(helpText, gbc);

        return panel;
    }

    private JPanel createBuildingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Enable Building automation
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JCheckBox enableBuilding = new JCheckBox("Enable Automatic Building & Upgrades");
        enableBuilding.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(enableBuilding, gbc);
        settingsComponents.put("buildingEnabled", enableBuilding);

        // Building priority
        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0;
        panel.add(new JLabel("Building Priority:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        String[] buildingPriorities = {
            "Resource Buildings First (Farm, Sawmill, Quarry, Mine)",
            "Military Buildings First (Barracks, Academy, Armory)",
            "Defensive Buildings First (Wall, Watchtowers, Traps)",
            "Balanced Approach (Mix of all types)",
            "Custom Order (Define your own sequence)"
        };
        JComboBox<String> buildingPriority = new JComboBox<>(buildingPriorities);
        panel.add(buildingPriority, gbc);
        settingsComponents.put("buildingPriority", buildingPriority);

        // Resource threshold
        gbc.gridy++; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Start building when resources reach:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JComboBox<String> resourceThreshold = new JComboBox<>(new String[]{
            "50% of storage capacity", "75% of storage capacity", "90% of storage capacity", "100% (storage full)"
        });
        resourceThreshold.setSelectedIndex(1); // 75%
        panel.add(resourceThreshold, gbc);
        settingsComponents.put("resourceThreshold", resourceThreshold);

        // Building limits
        gbc.gridy++; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Max building queue:"), gbc);
        gbc.gridx = 1;
        JSpinner maxBuildings = new JSpinner(new SpinnerNumberModel(2, 1, 5, 1));
        panel.add(maxBuildings, gbc);
        settingsComponents.put("maxBuildings", maxBuildings);

        // Building strategy
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel strategyPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        strategyPanel.setBorder(BorderFactory.createTitledBorder("Building Strategy"));
        
        JCheckBox upgradeExisting = new JCheckBox("Upgrade existing buildings before building new ones");
        JCheckBox balanceBuildings = new JCheckBox("Keep building levels balanced (same level across type)");
        JCheckBox pauseForUpgrades = new JCheckBox("Pause gathering when major upgrades available");
        
        upgradeExisting.setSelected(true);
        balanceBuildings.setSelected(true);
        
        strategyPanel.add(upgradeExisting);
        strategyPanel.add(balanceBuildings);
        strategyPanel.add(pauseForUpgrades);
        
        panel.add(strategyPanel, gbc);
        settingsComponents.put("upgradeExisting", upgradeExisting);
        settingsComponents.put("balanceBuildings", balanceBuildings);
        settingsComponents.put("pauseForUpgrades", pauseForUpgrades);

        // Help text
        gbc.gridy++; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "Building & Upgrades Help:\n\n" +
            "• Resource Buildings: Increase production and storage\n" +
            "• Military Buildings: Enable troop training and research\n" +
            "• Defensive Buildings: Protect your city from attacks\n\n" +
            "Strategy Tips:\n" +
            "• Start with resource buildings for steady growth\n" +
            "• Keep buildings balanced for optimal efficiency\n" +
            "• Higher thresholds prevent wasting resources on small upgrades"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(helpText, gbc);

        return panel;
    }

    private JPanel createTroopTrainingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Enable Troop Training
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JCheckBox enableTroops = new JCheckBox("Enable Automatic Troop Training");
        enableTroops.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(enableTroops, gbc);
        settingsComponents.put("troopsEnabled", enableTroops);

        // Troop composition
        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0;
        panel.add(new JLabel("Troop Composition:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        JComboBox<String> troopComposition = new JComboBox<>(new String[]{
            "Balanced Army (25% each type)",
            "Infantry Focus (60% Infantry, 20% Ranged, 20% Cavalry)", 
            "Ranged Focus (60% Ranged, 20% Infantry, 20% Cavalry)",
            "Cavalry Focus (60% Cavalry, 20% Infantry, 20% Ranged)",
            "Resource Gathering (Fast troops for quick marches)",
            "Defensive (Strong troops for city defense)"
        });
        panel.add(troopComposition, gbc);
        settingsComponents.put("troopComposition", troopComposition);

        // Training priority
        gbc.gridy++; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Training Priority:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JComboBox<String> trainingPriority = new JComboBox<>(new String[]{
            "Highest Tier Available", "Fastest Training Time", "Best Resource Efficiency", "Power Growth Focus"
        });
        panel.add(trainingPriority, gbc);
        settingsComponents.put("trainingPriority", trainingPriority);

        // Training limits
        gbc.gridy++; gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Max training queue:"), gbc);
        gbc.gridx = 1;
        JSpinner maxTraining = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
        panel.add(maxTraining, gbc);
        settingsComponents.put("maxTraining", maxTraining);

        // Resource allocation
        gbc.gridy++; gbc.gridx = 0;
        panel.add(new JLabel("Use % of resources for training:"), gbc);
        gbc.gridx = 1;
        JSpinner resourcePercent = new JSpinner(new SpinnerNumberModel(30, 10, 90, 5));
        panel.add(resourcePercent, gbc);
        settingsComponents.put("resourcePercent", resourcePercent);

        // Training conditions
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel conditionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        conditionsPanel.setBorder(BorderFactory.createTitledBorder("Training Conditions"));
        
        JCheckBox trainWhenIdle = new JCheckBox("Train continuously when not gathering");
        JCheckBox trainDuringGathering = new JCheckBox("Train during resource gathering (background)");
        JCheckBox stopWhenFull = new JCheckBox("Stop when army capacity reached");
        JCheckBox trainWhenAttacked = new JCheckBox("Prioritize training when recently attacked");
        
        trainWhenIdle.setSelected(true);
        trainDuringGathering.setSelected(true);
        stopWhenFull.setSelected(true);
        
        conditionsPanel.add(trainWhenIdle);
        conditionsPanel.add(trainDuringGathering);
        conditionsPanel.add(stopWhenFull);
        conditionsPanel.add(trainWhenAttacked);
        
        panel.add(conditionsPanel, gbc);
        settingsComponents.put("trainWhenIdle", trainWhenIdle);
        settingsComponents.put("trainDuringGathering", trainDuringGathering);
        settingsComponents.put("stopWhenFull", stopWhenFull);
        settingsComponents.put("trainWhenAttacked", trainWhenAttacked);

        // Help text
        gbc.gridy++; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "