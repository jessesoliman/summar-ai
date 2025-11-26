package com.example.summar_ai.config;

import com.example.summar_ai.models.Tool;
import com.example.summar_ai.repositories.ToolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ToolRepository toolRepository) {
        return args -> {
            // Define all tools that should exist
            String[][] tools = {
                {"Google Calendar", "google"},
                {"Zoom", "zoom"},
                {"Jira", "jira"}
                // Add new tools here as you create integrations
                // Example: {"Slack", "slack"}
            };

            int addedCount = 0;
            int skippedCount = 0;

            for (String[] toolData : tools) {
                String toolName = toolData[0];
                String provider = toolData[1];

                // Check if tool already exists
                if (toolRepository.findByToolName(toolName).isEmpty()) {
                    Tool tool = new Tool();
                    tool.setToolName(toolName);
                    tool.setProvider(provider);
                    toolRepository.save(tool);
                    addedCount++;
                    System.out.println("Added tool: " + toolName);
                } else {
                    skippedCount++;
                }
            }

            System.out.println("Tool initialization complete - Added: " + addedCount + ", Already existed: " + skippedCount);
        };
    }
}
