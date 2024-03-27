package de.minekonst.mariokartwiiai.main;

import java.io.File;

public class Constants {

    // Emulator
    public static final String DOLPHIN_PATH = Main.getProjectDir() + File.separator + "Dolphin" + File.separator + "Dolphin.exe";
    public static final String AI_DIR = Main.getProjectDir() + File.separator + "Dolphin" + File.separator + "AI";
    public static final String INPUT_FILE = AI_DIR + File.separator + "AI_%d_Input.txt";
    public static final String ISO_NAME = "MKWii.iso";

    // Server / Client
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 7322;
    public static final int CLIENT_TIMEOUT = 10_000;
    public static final int TASK_TIMEOUT = 15 * 60_000;
    
    public static final boolean DEFAULT_LOAD_WITH_COUNTDOWN = true;
    public static final double DEFAULT_EMULATOR_SPEED = 5;
    
    public static final int FRAMERATE = 60;

    private Constants() {
    }
}
