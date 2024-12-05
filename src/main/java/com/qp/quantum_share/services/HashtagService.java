package com.qp.quantum_share.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.helper.CSVReaderUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HashtagService {

    private final CSVReaderUtil csvReaderUtil;
    private static final Logger logger = LoggerFactory.getLogger(HashtagService.class);

    public HashtagService(CSVReaderUtil csvReaderUtil) {
        this.csvReaderUtil = csvReaderUtil;
    }

    public List<String> getHashtagSuggestions(String query) {
        // Ensure query is not empty
        if (query == null || query.trim().isEmpty()) {
            logger.debug("Empty or null query provided.");
            return List.of(); // Return an empty list if query is empty
        }

       
        List<String> hashtags = csvReaderUtil.readHashtagsFromCSV();

        
        if (hashtags == null) {
            logger.error("Failed to load hashtags from CSV.");
            return List.of();  
        }

       
        logger.debug("Loaded hashtags: {}", hashtags);

        
        String queryLower = query.trim().toLowerCase();  
        List<String> filteredHashtags = hashtags.stream()
                .filter(tag -> tag.toLowerCase().contains(queryLower))  
                .collect(Collectors.toList());

       
        logger.debug("Filtered hashtags: {}", filteredHashtags);

        return filteredHashtags;
    }
}
