package org.carol.tfm.domain.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DataExportService {
    private final static String PATH_TO_ACTION_FILE = "/home/carol/tfm_results/actions.csv";
    private final static String PATH_TO_VALUE_FILE = "/home/carol/tfm_results/values.csv";
    private final static String PATH_TO_RESERVOIR_FILE_C1 = "/home/carol/tfm_results/resevoir_c1.csv";
    private final static String PATH_TO_RESERVOIR_FILE_C2 = "/home/carol/tfm_results/resevoir_c2.csv";
    private final static String PATH_TO_RESERVOIR_FILE_C3 = "/home/carol/tfm_results/resevoir_c3.csv";
    private final static String PATH_TO_AFORO_FILE_C3 = "/home/carol/tfm_results/aforo_c3.csv";
    private final static String PATH_TO_AFORO_FILE_C4 = "/home/carol/tfm_results/aforo_c4.csv";

    public static void initLogFiles() {
        File file = new File(PATH_TO_VALUE_FILE);
        if (file.exists()) {
            file.delete();
        }

        file = new File(PATH_TO_ACTION_FILE);
        if (file.exists()) {
            file.delete();
        }

        file = new File(PATH_TO_RESERVOIR_FILE_C1);
        if (file.exists()) {
            file.delete();
        }

        file = new File(PATH_TO_RESERVOIR_FILE_C2);
        if (file.exists()) {
            file.delete();
        }

        file = new File(PATH_TO_RESERVOIR_FILE_C3);
        if (file.exists()) {
            file.delete();
        }

        file = new File(PATH_TO_AFORO_FILE_C3);
        if (file.exists()) {
            file.delete();
        }

        file = new File(PATH_TO_AFORO_FILE_C4);
        if (file.exists()) {
            file.delete();
        }

    }
    public static void appendLineToActions(String line) {
        try {
            appendLineToFile(PATH_TO_ACTION_FILE, line);
        } catch (IOException e) {
            System.err.println("Ocurrió un error al añadir la línea: " + e.getMessage());
        }
    }

    public static void appendLineToValues(String line) {
        try {
            appendLineToFile(PATH_TO_VALUE_FILE, line);
        } catch (IOException e) {
            System.err.println("Ocurrió un error al añadir la línea: " + e.getMessage());
        }
    }

    public static void appendLineToReservoirC1(String line) {
        try {
            appendLineToFile(PATH_TO_RESERVOIR_FILE_C1, line);
        } catch (IOException e) {
            System.err.println("Ocurrió un error al añadir la línea: " + e.getMessage());
        }
    }

    public static void appendLineToReservoirC2(String line) {
        try {
            appendLineToFile(PATH_TO_RESERVOIR_FILE_C2, line);
        } catch (IOException e) {
            System.err.println("Ocurrió un error al añadir la línea: " + e.getMessage());
        }
    }


    public static void appendLineToReservoirC3(String line) {
        try {
            appendLineToFile(PATH_TO_RESERVOIR_FILE_C3, line);
        } catch (IOException e) {
            System.err.println("Ocurrió un error al añadir la línea: " + e.getMessage());
        }
    }

    public static void appendLineToAforoC3(String line) {
        try {
            appendLineToFile(PATH_TO_AFORO_FILE_C3, line);
        } catch (IOException e) {
            System.err.println("Ocurrió un error al añadir la línea: " + e.getMessage());
        }
    }

    public static void appendLineToAforoC4(String line) {
        try {
            appendLineToFile(PATH_TO_AFORO_FILE_C4, line);
        } catch (IOException e) {
            System.err.println("Ocurrió un error al añadir la línea: " + e.getMessage());
        }
    }


    private static void appendLineToFile(String filePath, String newLine) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            file.createNewFile();
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(newLine);
            writer.newLine();
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }
}
