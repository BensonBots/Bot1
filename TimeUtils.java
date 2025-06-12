package newgame;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TimeUtils {
    
    public static String parseTimeFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        try {
            String cleaned = text.replaceAll("[^0-9:]", "");
            
            Pattern hhmmssPattern = Pattern.compile("(\\d{1,2}):(\\d{2}):(\\d{2})");
            Matcher hhmmssMatch = hhmmssPattern.matcher(cleaned);
            if (hhmmssMatch.find()) {
                String time = hhmmssMatch.group(0);
                return validateAndFormatTime(time);
            }
            
            Pattern mmssPattern = Pattern.compile("(\\d{1,2}):(\\d{2})");
            Matcher mmssMatch = mmssPattern.matcher(cleaned);
            if (mmssMatch.find()) {
                String time = "00:" + mmssMatch.group(0);
                return validateAndFormatTime(time);
            }
            
            Pattern digitsPattern = Pattern.compile("(\\d{4,6})");
            Matcher digitsMatch = digitsPattern.matcher(cleaned);
            if (digitsMatch.find()) {
                String digits = digitsMatch.group(0);
                String formattedTime = formatDigitsAsTime(digits);
                if (formattedTime != null) {
                    return formattedTime;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String formatDigitsAsTime(String digits) {
        try {
            if (digits.length() == 6) {
                return digits.substring(0, 2) + ":" + digits.substring(2, 4) + ":" + digits.substring(4, 6);
            } else if (digits.length() == 4) {
                return "00:" + digits.substring(0, 2) + ":" + digits.substring(2, 4);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String validateAndFormatTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                
                if (hours >= 0 && hours < 24 && minutes >= 0 && minutes < 60 && seconds >= 0 && seconds < 60) {
                    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
                }
            }
            return timeStr;
        } catch (Exception e) {
            return timeStr;
        }
    }
    
    public static boolean isValidMarchTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length != 3) return false;
            
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            
            if (hours == 0 && minutes == 0 && seconds == 0) return false;
            if (hours > 12) return false;
            if (minutes >= 60 || seconds >= 60) return false;
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static long parseTimeToSeconds(String timeStr) {
        try {
            if (timeStr == null || timeStr.trim().isEmpty()) {
                return 0;
            }
            
            String[] parts = timeStr.split(":");
            if (parts.length == 3) {
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                long seconds = Long.parseLong(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            } else if (parts.length == 2) {
                long minutes = Long.parseLong(parts[0]);
                long seconds = Long.parseLong(parts[1]);
                return minutes * 60 + seconds;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    public static String calculateTotalTime(String gatheringTime, String marchingTime) {
        try {
            if (gatheringTime == null || marchingTime == null) {
                return "02:30:00";
            }
            
            long gatherSeconds = parseTimeToSeconds(gatheringTime);
            long marchSeconds = parseTimeToSeconds(marchingTime);
            
            long totalSeconds = gatherSeconds + (marchSeconds * 2);
            
            return formatTime(totalSeconds);
            
        } catch (Exception e) {
            return "02:30:00";
        }
    }
    
    public static long getTimeRemaining(java.time.LocalDateTime startTime, String totalTimeStr) {
        try {
            long totalSeconds = parseTimeToSeconds(totalTimeStr);
            long elapsedSeconds = java.time.Duration.between(startTime, java.time.LocalDateTime.now()).getSeconds();
            return Math.max(0, totalSeconds - elapsedSeconds);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static double getProgressPercentage(java.time.LocalDateTime startTime, String totalTimeStr) {
        try {
            long totalSeconds = parseTimeToSeconds(totalTimeStr);
            if (totalSeconds <= 0) return 100.0;
            
            long elapsedSeconds = java.time.Duration.between(startTime, java.time.LocalDateTime.now()).getSeconds();
            double progress = (double) elapsedSeconds / totalSeconds * 100.0;
            return Math.min(100.0, Math.max(0.0, progress));
        } catch (Exception e) {
            return 0.0;
        }
    }
}