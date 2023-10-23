import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LogAnalyzer which takes raw collected logs, creates sanitized versions of them,
 * which are easier to read, and print statistics calculated from the logs to the console.
 */
class Scratch {

    //global stringBuilder to collect everything which should be printed
    private static final StringBuilder sb = new StringBuilder();

    //storage variables for log analysis
    private static final List<Long> msgProcessingDurations = new ArrayList<>();
    private static final Map<String, List<Long>> msgWaitTimesInQueue = new HashMap<>();
    private static final Map<String, Integer> msgCountMap = new HashMap<>();
    private static final Map<String, Long> podsStartedTimeMap = new HashMap<>();
    private static final Map<String, Long> podsStoppedTimeMap = new HashMap<>();
    private static Long lastLog = null;
    private static Long lastLogNonHistoryLog = null;
    private static Float businessFlowSeconds = null;
    private static Float historyFlowSeconds = null;

    //constants
    private static final int startStopSeconds = 15;

    //config
    private static final String DAY = "2024-02-19T";
    private static final String TIME = "21-30";
    private static final String NUMBER_OF_MESSAGES = "750";
    private static final int TEST_NR = 1;

    private static final String TEST_CASE = TEST_NR == 1
                                            ? "no-scaling"
                                            : TEST_NR == 2
                                              ? "cpu-hpa"
                                              : TEST_NR == 3
                                                ? "MAHA-no-follow"
                                                : TEST_NR == 4 ? "MAHA-full" : "invalid";

    private static final boolean MAHA_ACTIVE = TEST_CASE.contains("MAHA");
    private static final String DATE = DAY + TIME;

    public static void main(String[] args) throws IOException {

        final String BASE_DIR = System.getProperty("user.dir") + "/cup/scripts/logs/test-";

        final String cupPath = BASE_DIR + DATE + ".log";
        final String mahaPath = BASE_DIR + DATE + "-maha.log";
        final String cupSanitizedPath = BASE_DIR + DATE + "_sanitized.log";
        final String mahaSanitizedPath = BASE_DIR + DATE + "-maha_sanitized.log";
        final String statsPath = BASE_DIR + DATE + "-stats_" + TEST_CASE + "_" + NUMBER_OF_MESSAGES + ".txt";

        Map<String, String> replaceMap = createReplaceMap();

        createSanitizedFile(cupPath, replaceMap, cupSanitizedPath);
        createSanitizedFile(mahaPath, replaceMap, mahaSanitizedPath);

        readLogFile(cupSanitizedPath);
        createAnalysisOutput();

        String output = sb.toString();
        System.out.println(output);
        toFile(output, statsPath);
    }

    private static void readLogFile(final String cupSanitizedPath) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        //read log file lines
        try (BufferedReader reader = new BufferedReader(new FileReader(cupSanitizedPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //collect msg processing times within a pod
                Matcher matcher = Pattern.compile("processing time: (\\d+)")
                                         .matcher(line);
                if (matcher.find()) {
                    long duration = Long.parseLong(matcher.group(1));
                    msgProcessingDurations.add(duration);
                }
                //collect times pods stopped
                String[] parts = line.split("\\s+");
                String podName = parts[0];
                if (line.contains("ApplicationStopped") && line.contains("LifeCycleLogger")) {
                    String timestamp = parts[2] + " " + parts[3];
                    podsStoppedTimeMap.put(podName,
                                           dateFormat.parse(timestamp)
                                                     .getTime());
                }

                if (parts.length >= 5) {
                    //collect count of received messages
                    //collect times pods started (or rather received first msg)
                    if (parts[4].equals("Received")) {
                        msgCountMap.put(podName, msgCountMap.getOrDefault(podName, 0) + 1);
                        if (!podsStartedTimeMap.containsKey(podName)) {
                            String timestamp = parts[2] + " " + parts[3];
                            podsStartedTimeMap.put(podName,
                                                   dateFormat.parse(timestamp)
                                                             .getTime());
                        }
                    }
                    //collect time of last log and last non cup-history log
                    try {
                        String timestamp = parts[2] + " " + parts[3];
                        lastLog = dateFormat.parse(timestamp)
                                            .getTime();
                        if (!parts[1].equals("cup-history") && parts[1].startsWith("cup-")) {
                            lastLogNonHistoryLog = lastLog;
                        }
                    } catch (ParseException ignored) {
                        //just ignore if it is a non parsable log line
                    }

                }
                //collect how long a msg waited in a queue
                if (parts.length >= 8 && parts[6].equals("waited")) {
                    String queue = parts[11];
                    Long time = Long.parseLong(parts[7]);
                    List<Long> waitsPerQueue = msgWaitTimesInQueue.getOrDefault(queue, new ArrayList<>());
                    waitsPerQueue.add(time);
                    msgWaitTimesInQueue.put(queue, waitsPerQueue);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createAnalysisOutput() throws IOException {
        //mark from which log file the analysis is
        sb.append("Saved in " + DATE + "\n\n");

        sb.append(getHistorySummary());

        // print percentiles of msg processing times within pods
        if (!msgProcessingDurations.isEmpty()) {
            Collections.sort(msgProcessingDurations);
            sb.append("Statistics on Durations:\n");
            calculateAndPrintPercentiles(msgProcessingDurations);
        } else {
            sb.append("No durations found in the log file.\n");
        }
        // print percentiles how long messages waited in queue
        if (!msgWaitTimesInQueue.isEmpty()) {
            msgWaitTimesInQueue.forEach((queue, waits) -> {
                Collections.sort(waits);
                sb.append("\nStatistics on wait times for " + queue + "\n");
                sb.append("Number of messages: " + waits.size() + "\n");
                calculateAndPrintPercentiles(waits);
            });
        } else {
            sb.append("No wait times found in the log file.\n");
        }
        // print consumption and alive time per pod
        sb.append("\nTime from first to last message per pod:\n");
        for (Map.Entry<String, Long> entry : podsStartedTimeMap.entrySet()) {
            String pod = entry.getKey();
            if (!msgCountMap.containsKey(pod)) {
                continue;
            }
            long firstTime = entry.getValue();
            long lastTime = podsStoppedTimeMap.getOrDefault(pod, lastLog);
            long timeDifference = lastTime - firstTime;
            float utilization = msgCountMap.get(pod) / (timeDifference / 1000F / 60F);

            sb.append("%s: %d in %s ; Consumption per min: %f%n".formatted(pod,
                                                                           msgCountMap.get(pod),
                                                                           prettyTime(timeDifference),
                                                                           utilization));

        }

        printStatisticsSummary();
    }

    private static void calculateAndPrintPercentiles(List<Long> longs) {
        sb.append("1st percentile: " + calculatePercentile(longs, 1) + " ms\n");
        sb.append("5th percentile: " + calculatePercentile(longs, 5) + " ms\n");
        for (int i = 10; i <= 90; i += 10) {
            sb.append(i + "th percentile: " + calculatePercentile(longs, i) + " ms\n");
        }
        sb.append("95th percentile: " + calculatePercentile(longs, 95) + " ms\n");
        sb.append("99th percentile: " + calculatePercentile(longs, 99) + " ms\n");
    }

    private static double calculatePercentile(List<Long> values, int percentile) {
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        return values.get(index);
    }

    private static void printStatisticsSummary() {

        int totalMessages = 0;
        int nonHistoryMessages = 0;
        long totalTime = 0;
        long nonHistoryTime = 0;

        // Calculate total messages and total time
        for (String pod : msgCountMap.keySet()) {
            int messageCount = msgCountMap.getOrDefault(pod, 0);
            long firstMessageTime = podsStartedTimeMap.getOrDefault(pod, 0L);
            long lastMessageTime = podsStoppedTimeMap.getOrDefault(pod, lastLog);

            totalMessages += messageCount;
            totalTime += (lastMessageTime - firstMessageTime);
            if (!pod.startsWith("cup-history")) {
                nonHistoryMessages += messageCount;
                nonHistoryTime += (podsStoppedTimeMap.getOrDefault(pod, lastLogNonHistoryLog) - firstMessageTime);
            }
        }

        int numPods = msgCountMap.size();
        int numPodsNonHistory = (int) msgCountMap.keySet()
                                                 .stream()
                                                 .filter(p -> !p.startsWith("cup-history"))
                                                 .count();

        // Calculate statistics without cup-history
        double avgMessagesPerPodNonHistory = nonHistoryMessages / (double) numPodsNonHistory;
        double avgTimePerPodNonHistory = nonHistoryTime / (double) numPodsNonHistory;
        float avgUtilizationNonHistory = nonHistoryMessages / (nonHistoryTime / 1000F / 60F);
        float overHeadSecondsNonHistory = (numPodsNonHistory - 5) * startStopSeconds;
        if (MAHA_ACTIVE) {
            overHeadSecondsNonHistory += businessFlowSeconds;
        }
        float billingSecondsNonHistory = (nonHistoryTime / 1000F) + overHeadSecondsNonHistory;

        // Calculate statistics total
        double avgMessagesPerPod = totalMessages / (double) numPods;
        double avgTimePerPod = totalTime / (double) numPods;
        float avgUtilization = totalMessages / (totalTime / 1000F / 60F);
        float overHeadSeconds = (numPods - 6) * startStopSeconds;
        if (MAHA_ACTIVE) {
            overHeadSeconds += historyFlowSeconds;
        }
        float billingSeconds = (totalTime / 1000F) + overHeadSeconds;

        // Print statistics without cup-history
        sb.append("\nStats without cup-history:\n");
        sb.append("Seconds taken: " + businessFlowSeconds + "\n");
        sb.append("Number of messages: " + nonHistoryMessages + "\n");
        sb.append("Time across all pods: " + nonHistoryTime / 1000F + "\n");
        sb.append("Additional overhead time seconds: " + overHeadSecondsNonHistory + "\n");
        sb.append("Total billing seconds: " + billingSecondsNonHistory + "\n");
        sb.append("Number of pods: " + numPodsNonHistory + "\n");
        sb.append("Average number of messages per pod: " + avgMessagesPerPodNonHistory + "\n");
        sb.append("Average time per pod: " + avgTimePerPodNonHistory / 1000 + "\n");
        sb.append("Average consumption per pod per min: " + avgUtilizationNonHistory + "\n");

        // Print statistics total
        sb.append("\nTotal stats:" + "\n");
        sb.append("Seconds taken: " + historyFlowSeconds + "\n");
        sb.append("Total number of messages: " + totalMessages + "\n");
        sb.append("Total time across all pods: " + totalTime / 1000F + "\n");
        sb.append("Additional overhead time seconds: " + overHeadSeconds + "\n");
        sb.append("Total billing seconds: " + billingSeconds + "\n");
        sb.append("Number of pods: " + numPods + "\n");
        sb.append("Average number of messages per pod: " + avgMessagesPerPod + "\n");
        sb.append("Average time per pod: " + avgTimePerPod / 1000 + "\n");
        sb.append("Average consumption per pod per min: " + avgUtilization + "\n");

        // Summary convenience print for copying to overall analysis

        sb.append("\nSummary convenience print:" + "\n" + "\n");

        final StringBuilder convSb = new StringBuilder();

        convSb.append(businessFlowSeconds + "\n")
              .append(nonHistoryTime / 1000F + "\n")
              .append(overHeadSecondsNonHistory + "\n")
              .append(billingSecondsNonHistory + "\n")
              .append(numPodsNonHistory + "\n")
              .append(avgUtilizationNonHistory + "\n");

        convSb.append("\n");

        convSb.append(historyFlowSeconds + "\n")
              .append(totalTime / 1000F + "\n")
              .append(overHeadSeconds + "\n")
              .append(billingSeconds + "\n")
              .append(numPods + "\n")
              .append(avgUtilization + "\n");

        toClipboard(convSb.toString());

        sb.append(convSb);

    }

    private static String prettyTime(final double time) {
        long secondsDifference = (long) (time / 1000);
        long minutesDifference = secondsDifference / 60;
        long secondsRemainder = secondsDifference % 60;
        return minutesDifference + " min, " + secondsRemainder + " sec";
    }

    private static Map<String, String> createReplaceMap() {
        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("INFO 1 --- ", "");
        replaceMap.put("c.c.p.boundary.producer.SqsProducer      :", "");
        replaceMap.put("c.c.p.boundary.consumer.SqsConsumer      :", "");
        replaceMap.put("c.c.p.s.history.DefaultHistoryService    :", "");
        replaceMap.put("[enerContainer-", "");
        replaceMap.put("[nerContainer-", "");
        replaceMap.put("[nio-8080-exec-", "");
        replaceMap.put("[pool-3-thread-1]", "");
        replaceMap.put("[pool-3-thread-2]", "");
        replaceMap.put("[pool-3-thread-3]", "");
        replaceMap.put("[nio-8080-exec-1]", "");
        replaceMap.put("[   scheduling-1]", "");
        replaceMap.put("[           main]", "");
        replaceMap.put("com.schoeniu.maha.api.PrometheusApi      :", "PrometheusApi  :");
        replaceMap.put("c.schoeniu.maha.service.ScalingSchedule  :", "ScalingSchedule:");
        replaceMap.put("c.s.maha.observability.MetricManager     :", "MetricManager  :");
        replaceMap.put("com.schoeniu.maha.api.K8sApi             :", "K8sApi         :");
        replaceMap.put(" maha 20", " 20");
        return replaceMap;
    }

    private static void createSanitizedFile(String filePath, Map<String, String> replaceMap, String newFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
                BufferedWriter writer = new BufferedWriter(new FileWriter(newFilePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
                writer.write(line);
                writer.newLine();
            }
            sb.append("File saved as " + newFilePath.substring(newFilePath.lastIndexOf("\\") + 1) + "\n");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static String getHistorySummary() throws IOException {
        URL url = new URL("http://localhost:30085/status/summary");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "text/plain");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        String businessFlowLabel = "Business flow duration in seconds: ";
        String historyFlowLabel = "Total flow duration in seconds: ";

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine)
                    .append("\n");
            if (inputLine.contains(businessFlowLabel)) {
                businessFlowSeconds = Float.valueOf(inputLine.replace(businessFlowLabel, ""));
            }
            if (inputLine.contains(historyFlowLabel)) {
                historyFlowSeconds = Float.valueOf(inputLine.replace(historyFlowLabel, ""));
            }
        }
        in.close();
        con.disconnect();

        return response.toString();
    }

    private static void toClipboard(final String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit()
                                     .getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private static void toFile(final String text, final String path) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(path);
        byte[] strToBytes = text.getBytes();
        outputStream.write(strToBytes);
        outputStream.close();
    }

}