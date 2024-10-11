package dev.kavindupere.elasticspring.domain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvReaderService {

    private final RestTemplate restTemplate;

    @Scheduled(fixedRate = 600_000)
    public void readCsv() {
        String csvToReadDirectoryPath = "./csvToRead";
        var listOfCsvFiles = listCsvFiles(csvToReadDirectoryPath);

        listOfCsvFiles.forEach(file -> {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                String[] headers = null;
                while ((line = br.readLine()) != null) {
                    if (headers == null) {
                        headers = line.split(",");  // First line as headers
                    } else {
                        // Create a map of values from the CSV line
                        Map<String, String> csvData = new HashMap<>();
                        String[] values = line.split(",");
                        for (int i = 0; i < values.length; i++) {
                            csvData.put(headers[i], values[i]);
                        }
                        // Send data to Logstash
                        postToLogstash(csvData);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
    }

    private Set<File> listCsvFiles(String dirPath) {
        try (var csvList = Stream.of(new File(dirPath).listFiles())) {
            return csvList.filter(File::isFile)
            .map(file -> {
                log.debug("filePath : {}", file.getPath());
                return file;
            })
            .collect(Collectors.toSet());
        } catch(NullPointerException exception) {
            log.error("No CSV files available in the path!!", exception.getMessage());
        }
        return Collections.emptySet();
    }

    // Helper method to post data to Logstash
    private void postToLogstash(Map<String, String> csvData) {
        String logstashUrl = "http://localhost:5000";
        try {
            restTemplate.postForObject(logstashUrl, csvData, String.class);
            System.out.println("Data sent to Logstash: " + csvData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
