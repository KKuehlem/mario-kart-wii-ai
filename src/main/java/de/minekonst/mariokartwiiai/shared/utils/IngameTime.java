package de.minekonst.mariokartwiiai.shared.utils;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class IngameTime implements Serializable {

    public static final int FRAMES_PER_SECOND = 60;
    private static final Pattern REGEX = Pattern.compile("\\s*(?<M>\\d+:)?(?<S>\\d+)(?<MS>[.,]\\d+)?");

    private final int frames;

    public IngameTime(int frames) {
        this.frames = frames;
    }

    @Override
    public String toString() {
        double sec = 1.0 * frames / FRAMES_PER_SECOND;
        int minutes = (int) (sec / 60);
        sec -= minutes * 60;

        return minutes > 0 || true ? String.format(Locale.US, "%d:%06.3f", minutes, sec) : String.format(Locale.US, "%06.3f", sec);
    }

    public static IngameTime fromString(String s) {
        Matcher m = REGEX.matcher(s);
        if (m.matches() && m.group("S") != null) {
            int minutes = m.group("M") != null ? Integer.valueOf(m.group("M").substring(0, m.group("M").length() - 1)) : 0;
            int sec = Integer.valueOf(m.group("S"));
            int ms = m.group("MS") != null ? Integer.valueOf(m.group("MS").substring(1)) : 0;

            return new IngameTime((int) ((minutes * 60 + sec + ms / 1000.0) * FRAMES_PER_SECOND + 0.5));
        }
        else {
            throw new IllegalArgumentException("Invalid format");
        }
    }

}
