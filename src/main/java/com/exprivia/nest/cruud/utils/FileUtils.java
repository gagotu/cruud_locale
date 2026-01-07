package com.exprivia.nest.cruud.utils;

import com.exprivia.nest.cruud.dto.ResultUrbanDataset;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * File-related helpers used by the conversion pipeline.
 */
@Slf4j
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Method to get names of files from folder path.
     *
     * @param folderPath that include files
     * @return files name
     */
    public static List<String> getFilesNameFromPath(String folderPath) {
        try (Stream<Path> files = Files.list(Paths.get(folderPath))) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            log.error("Error: {} -> path doesn't exist or not contains csv files!", e.getMessage());
        }

        return List.of(); // returned void list if files doesn't exist
    }

    /**
     * Method to clean files completed from folder path.
     *
     * @param folderPath where clean files
     */
    public static void cleanFilesCompleted(String folderPath) {
        File folder = new File(folderPath);

        // Verify path is an existing directory.
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("The folder '{}' doesn't exists", folderPath);
            return;
        }

        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                // Delete only files.
                if (file.isFile() && !file.delete()) {
                    log.error("Error during delete: {}", file.getName());
                }
            }
        }
    }

    /**
     * Method to create a file into a specific path.
     *
     * @param createFilePath file path output
     * @param urbanDataset urban data set that
     * @param objectMapper object to write a json
     *
     * @throws IOException error
     */
    public static void createFile(String createFilePath, ResultUrbanDataset urbanDataset, ObjectMapper objectMapper) throws IOException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Path path = Paths.get(createFilePath);

        // Create parent folders when missing.
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        objectMapper.writeValue(new File(createFilePath), urbanDataset);
    }

    /**
     * Method to move a file from a path to another.
     *
     * @param inputFilePath input file path
     * @param outputFilePath output file path
     *
     * @throws IOException error
     */
    public static void moveFile(String inputFilePath, String outputFilePath) throws IOException {
        Path sourceFile = Paths.get(inputFilePath);
        Path outputFolder = Paths.get(outputFilePath);

        // Create destination folders when missing.
        if (outputFolder.getParent() != null) {
            Files.createDirectories(outputFolder.getParent());
        }

        Files.move(sourceFile, outputFolder, StandardCopyOption.REPLACE_EXISTING);
    }
}
