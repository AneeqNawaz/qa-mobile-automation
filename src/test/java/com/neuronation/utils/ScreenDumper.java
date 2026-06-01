package com.neuronation.utils;

import com.neuronation.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility to dump all visible elements on the current screen.
 * Helps discover real locators when building/updating screen page objects.
 *
 * Usage in a test:
 *   ScreenDumper.dumpCurrentScreen("LaunchScreen");
 *
 * Output saved to: screen-dumps/{screenName}.txt
 * Also attached to Allure report.
 */
public class ScreenDumper {
    private static final Logger log = LoggerFactory.getLogger(ScreenDumper.class);
    private static final String DUMP_DIR = "screen-dumps";

    @Step("Dump all elements on screen: {screenName}")
    public static String dumpCurrentScreen(String screenName) {
        AppiumDriver driver = DriverManager.getDriver();
        String pageSource = driver.getPageSource();

        // Parse the XML and extract meaningful elements
        StringBuilder report = new StringBuilder();
        report.append("=== SCREEN DUMP: ").append(screenName).append(" ===\n\n");

        // Parse XML elements with resource-id, text, or content-desc
        String[] lines = pageSource.split("<");
        int elementCount = 0;

        for (String line : lines) {
            String resourceId = extractAttr(line, "resource-id");
            String text = extractAttr(line, "text");
            String contentDesc = extractAttr(line, "content-desc");
            String className = extractAttr(line, "class");
            String clickable = extractAttr(line, "clickable");
            String bounds = extractAttr(line, "bounds");

            boolean hasContent = (resourceId != null && !resourceId.isEmpty())
                    || (text != null && !text.isEmpty())
                    || (contentDesc != null && !contentDesc.isEmpty());

            if (hasContent) {
                elementCount++;
                report.append(String.format("--- Element #%d ---\n", elementCount));
                if (resourceId != null && !resourceId.isEmpty())
                    report.append(String.format("  resource-id : %s\n", resourceId));
                if (text != null && !text.isEmpty())
                    report.append(String.format("  text        : %s\n", text));
                if (contentDesc != null && !contentDesc.isEmpty())
                    report.append(String.format("  content-desc: %s\n", contentDesc));
                if (className != null)
                    report.append(String.format("  class       : %s\n", className));
                if ("true".equals(clickable))
                    report.append("  clickable   : YES\n");
                if (bounds != null && !bounds.isEmpty())
                    report.append(String.format("  bounds      : %s\n", bounds));
                report.append("\n");
            }
        }

        report.append(String.format("Total elements with content: %d\n", elementCount));

        String result = report.toString();

        // Save to file
        try {
            Path dir = Paths.get(DUMP_DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(screenName + ".txt");
            Files.writeString(file, result);
            log.info("Screen dump saved to: {}", file);
        } catch (IOException e) {
            log.warn("Could not save screen dump file: {}", e.getMessage());
        }

        // Attach to Allure
        try {
            Allure.addAttachment("Screen Dump - " + screenName, "text/plain", result);
        } catch (Exception ignored) {}

        // Log summary
        log.info("Screen dump for '{}': {} elements found", screenName, elementCount);

        return result;
    }

    private static String extractAttr(String line, String attrName) {
        String search = attrName + "=\"";
        int start = line.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = line.indexOf("\"", start);
        if (end == -1) return null;
        return line.substring(start, end);
    }
}
