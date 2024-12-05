package com.qp.quantum_share.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CSVReaderUtil {

	private static final Logger logger = LoggerFactory.getLogger(CSVReaderUtil.class); // Logger instance

	public List<String> readHashtagsFromCSV() {
		List<String> hashtags = new ArrayList<>();

		try (InputStream inputStream = getClass().getResourceAsStream("/Hashtags.csv")) {
			if (inputStream == null) {
				logger.error("CSV file not found in resources.");
				return hashtags;
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(",");

					if (parts.length >= 2) {
						String hashtag = parts[1].trim();
						if (!hashtag.startsWith("#")) {
							hashtag = "#" + hashtag;
						}
						hashtags.add(hashtag);
						logger.debug("Hashtag read: {}", hashtag);
					} else {
						logger.warn("Skipping malformed line: {}", line);
					}
				}
			}
		} catch (IOException e) {
			logger.error("Error reading CSV file: {}", e.getMessage(), e);
		}

		return hashtags;
	}
}
