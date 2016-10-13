package ru.v0rt3x.vindicator.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PSUtils {

    public static List<String> exec(String... command) throws IOException, InterruptedException {
        List<String> output = new ArrayList<>();

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);

        Process process = processBuilder.start();

        BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = is.readLine()) != null) {
            output.add(line);
        }

        process.waitFor();

        is.close();

        return output;
    }
}
