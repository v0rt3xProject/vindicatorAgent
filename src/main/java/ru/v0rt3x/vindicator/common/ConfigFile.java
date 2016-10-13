package ru.v0rt3x.vindicator.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigFile {

    private final Map<String, String> configData;
    private static final Logger logger = LoggerFactory.getLogger(ConfigFile.class);

    private final File configFile;

    private static final Pattern sectionPattern = Pattern.compile(
            "^\\s*\\[(?<section>[^\\]]+)\\]\\s*$"
    );
    private static final Pattern configPattern = Pattern.compile(
            "^\\s*(?<key>[^#\\s]+)\\s*=\\s*(?<value>([^#\'\"]+)|('[^']+')|(\"[^\"]+\"))\\s*(?<comment>#\\s*(.*?))?$"
    );
    private static final Pattern commentPattern = Pattern.compile(
            "^\\s*(#\\s*(?<comment>.*?))$"
    );

    public ConfigFile(File config) throws IOException {
        configData = new HashMap<>();
        configFile = config;

        readConfig();
    }

    public void readConfig() throws IOException {
        BufferedReader configReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));

        String configLine;
        String sectionName = "DEFAULT";

        int lineNumber = 1;
        while ((configLine = configReader.readLine()) != null) {
            Matcher sectionMatcher = sectionPattern.matcher(configLine);
            Matcher configMatcher = configPattern.matcher(configLine);
            Matcher commentMatcher = commentPattern.matcher(configLine);

            if (sectionMatcher.matches()) {
                sectionName = sectionMatcher.group("section");
            } else if (configMatcher.matches()) {
                String keyName = configMatcher.group("key");
                String keyValue = configMatcher.group("value");

                configData.put(sectionName.toLowerCase() + "." + keyName.toLowerCase(), keyValue);
            } else if (!commentMatcher.matches()&&(configLine.length() > 0)) {
                logger.warn("Invalid configuration found on {}:{}", configFile.getName(), lineNumber);
            }

            lineNumber++;
        }
    }

    public String getString(String key, String defaultValue) {
        return (configData.containsKey(key.toLowerCase())) ? configData.get(key.toLowerCase()) : defaultValue;
    }

    public Integer getInt(String key, Integer defaultValue) {
        return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
    }

    public Long getLong(String key, Long defaultValue) {
        return Long.parseLong(getString(key, String.valueOf(defaultValue)));
    }

    public Float getFloat(String key, Float defaultValue) {
        return Float.parseFloat(getString(key, String.valueOf(defaultValue)));
    }

    public Double getDouble(String key, Double defaultValue) {
        return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
    }

    public File getFile(String key, String defaultValue) {
        return new File(getString(key, defaultValue));
    }

    public <T> List<T> getList(String key, Class<T> valueType) {
        List<T> result = new ArrayList<>();
        String configValue = getString(key, null);

        if (configValue != null) {
            for (String valuePart: configValue.split("\\s*,\\s*")) {
                result.add(valueType.cast(valuePart));
            }
        }

        return result;
    }

    public List<String> getKeys(String keyGroup) {
        return configData.keySet().stream()
                .filter(key -> key.startsWith(keyGroup))
                .collect(Collectors.toList());
    }

    public File getConfigDir() {
        return configFile.getAbsoluteFile().getParentFile();
    }

    public File getConfigFile() {
        return configFile;
    }
}