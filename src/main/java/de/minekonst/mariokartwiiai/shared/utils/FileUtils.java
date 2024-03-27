package de.minekonst.mariokartwiiai.shared.utils;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Write an Object to a file (will use fst)
     *
     * @param path   The path of the file
     * @param object The object to write. The object and all fields (recurive)
     *               need to implement the {@link Serializable} interface
     *
     * @throws UnsupportedOperationException if the file cannot be written. This
     *                                       most likly happens if a class does
     *                                       not implement {@link Serializable}.
     *                                       Check the exception cause if this
     *                                       happens.
     */
    public static void writeObject(String path, Serializable object) {
        writeObject(path, object, true);
    }

    /**
     * Write an Object to a file
     *
     * @param path   The path of the file
     * @param object The object to write. The object and all fields (recurive)
     *               need to implement the {@link Serializable} interface
     * @param fst    if true the fst libray will be used (need to be in the
     *               classpath)
     *
     * @throws UnsupportedOperationException if the file cannot be written. This
     *                                       most likly happens if a class does
     *                                       not implement {@link Serializable}.
     *                                       Check the exception cause if this
     *                                       happens.
     */
    public static void writeObject(String path, Serializable object, boolean fst) {
        writeObject(path, object, fst, false);
    }

    /**
     * Write an Object to a file
     *
     * @param path   The path of the file
     * @param object The object to write. The object and all fields (recurive)
     *               need to implement the {@link Serializable} interface
     * @param fst    if true the fst libray will be used (need to be in the
     *               classpath)
     * @param zip    If true the data will be compressed using
     *               {@link GZIPOutputStream}. Make sure to read the data back
     *               with {@link FileUtils#readObject(java.lang.String, java.lang.Class, boolean, boolean)
     *               } and zip set to true
     *
     * @throws UnsupportedOperationException if the file cannot be written. This
     *                                       most likly happens if a class does
     *                                       not implement {@link Serializable}.
     *                                       Check the exception cause if this
     *                                       happens.
     */
    public static void writeObject(String path, Serializable object, boolean fst, boolean zip) {
        try {
            // Write empty string to clean (or even create) file
            writeFile(path, "");

            OutputStream outStream = new FileOutputStream(path);
            if (zip) {
                outStream = new GZIPOutputStream(outStream);
            }

            ObjectOutput out = fst ? new FSTObjectOutput(outStream) : new ObjectOutputStream(outStream);
            out.writeObject(object);
            out.close();
        }
        catch (IOException ex) {
            throw new UnsupportedOperationException("Can not write to file (make sure all fields of the object implement serializable)", ex);
        }
    }

    /**
     * Try to open a file for editing.
     *
     * @param parent If noit null and an exception occured, becuase
     *               {@link Desktop} did not know how to open this file, an
     *               error dialog will be displayed.
     * @param file   The file to open
     *
     * @return true, if the file has been opened for editing
     *
     * @see Desktop#edit(java.io.File)
     */
    public static boolean editFile(JFrame parent, File file) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().edit(file);
                return true;
            }
            catch (IOException ex) {
                if (parent != null) {
                    JOptionPane.showMessageDialog(parent, "Kann Datei nicht öffnen",
                            "Es wurde keine Anwendung gefunden, um diese Datei zu öffnen", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        return false;
    }

    /**
     * Try to open a file.
     *
     * @param parent If noit null and an exception occured, becuase
     *               {@link Desktop} did not know how to open this file, an
     *               error dialog will be displayed.
     * @param file   The file to open
     *
     * @return true, if the file has been opened
     *
     * @see Desktop#open(java.io.File)
     */
    public static boolean openFile(JFrame parent, File file) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
                return true;
            }
            catch (IOException ex) {
                if (parent != null) {
                    JOptionPane.showMessageDialog(parent, "Kann Datei nicht öffnen",
                            "Es wurde keine Anwendung gefunden, um diese Datei zu öffnen", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        return false;
    }

    /**
     * Read an Object from a file using {@link FSTObjectInput}. Make sure the
     * library is in the claspath.
     *
     * @param <T>  The type of the object to read
     * @param path The path were to find this object
     * @param cls  The class of the object
     *
     * @return The object or null, if the file cannot be found.
     *
     * @throws UnsupportedOperationException if the class type is unknown or the
     *                                       object in the file cannot be
     *                                       converted to this class.
     * @throws IllegalStateException         If the file cannot be read. This
     *                                       most likely happens, if the class
     *                                       has been modified since the object
     *                                       of the class has been saved
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T readObject(String path, Class<T> cls) {
        return readObject(path, cls, true, false);
    }

    /**
     * Read an Object from a file.
     *
     * @param <T>  The type of the object to read
     * @param path The path were to find this object
     * @param cls  The class of the object
     * @param fst  If true, {@link FSTObjectInput} will be used. The library
     *             must be in the class path.
     *
     * @return The object or null, if the file cannot be found.
     *
     * @throws UnsupportedOperationException if the class type is unknown or the
     *                                       object in the file cannot be
     *                                       converted to this class.
     * @throws IllegalStateException         If the file cannot be read. This
     *                                       most likely happens, if the class
     *                                       has been modified since the object
     *                                       of the class has been saved
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T readObject(String path, Class<T> cls, boolean fst) {
        return readObject(path, cls, fst, false);
    }

    /**
     * Read an Object from a file.
     *
     * @param <T>  The type of the object to read
     * @param path The path were to find this object
     * @param cls  The class of the object
     * @param fst  If true, {@link FSTObjectInput} will be used. The library
     *             must be in the class path.
     * @param zip  If true the data will be uncompressed. This will throw an
     *             exception, if the data in the file is not compressed
     *
     * @return The object or null, if the file cannot be found.
     *
     * @throws UnsupportedOperationException if the class type is unknown or the
     *                                       object in the file cannot be
     *                                       converted to this class.
     * @throws IllegalStateException         If the file cannot be read. This
     *                                       most likely happens, if the class
     *                                       has been modified since the object
     *                                       of the class has been saved
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T readObject(String path, Class<T> cls, boolean fst, boolean zip) {
        try {
            InputStream inStream = new FileInputStream(path);
            if (zip) {
                inStream = new GZIPInputStream(inStream);
            }

            ObjectInput in = fst ? new FSTObjectInput(inStream) : new ObjectInputStream(inStream);

            Object o = in.readObject();
            in.close();

            return (T) o;
        }
        catch (FileNotFoundException ex) {
            return null;
        }
        catch (IOException ex) {
            throw new IllegalStateException("Cannot read file", ex);
        }
        catch (ClassNotFoundException | ClassCastException ex) {
            throw new UnsupportedOperationException("Class is unknow or not of paramter type", ex);
        }
    }

    public static boolean writeFile(String path, String value) {
        try {
            File file = new File(path);
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(value);
            out.close();

            return true;
        }
        catch (IOException ex) {
            return false;
        }
    }

    public static boolean writeFile(String path, Object value) {
        return writeFile(path, value.toString());
    }

    /**
     * Read file
     *
     * @param path The path of the file
     *
     * @return Collection with file split in lines (empty, if error occurred)
     */
    public static List<String> readFile(String path) {
        ArrayList<String> lines = new ArrayList<>(20);

        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);

            String currentLine;

            while ((currentLine = br.readLine()) != null) {
                lines.add(currentLine);
            }

            fr.close();
            br.close();
        }
        catch (IOException ex) {

        }

        return Collections.unmodifiableList(lines);
    }

    public static void zipDirectory(String directory, String zipFile) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Directory must be directory");
        }

        try {
            File target = new File(zipFile);
            target.createNewFile();
            FileOutputStream dest = new FileOutputStream(target);
            ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(dest));

            writeDirToZip(dir, zip, "");
            zip.finish();
            zip.close();
            dest.close();
        }
        catch (IOException ex) {
            throw new IllegalStateException("Cannot write file", ex);
        }
    }

    private static void writeDirToZip(File dir, ZipOutputStream zip, String parent) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                writeDirToZip(f, zip, parent + f.getName() + "/");
                continue;
            }

            try {
                zip.putNextEntry(new ZipEntry(parent + f.getName()));
                FileInputStream fis = new FileInputStream(f);

                byte[] buffer = new byte[1024];
                int l;
                while ((l = fis.read(buffer)) >= 0) {
                    zip.write(buffer, 0, l);
                    zip.flush();
                }

                zip.flush();
                zip.closeEntry();
                fis.close();
            }
            catch (IOException ex) {
                throw new IllegalStateException("Failed to write file " + f.getName() + " to zipfile", ex);
            }
        }
    }

    public static void unzipDirectory(String path, String dir) {
        File zipFile = new File(path);
        if (!zipFile.exists()) {
            throw new IllegalArgumentException("Zipfile does not exist");
        }

        File target = new File(dir);
        if (!target.exists()) {
            if (!target.mkdir()) {
                throw new IllegalStateException("Cannot create ouput directory");
            }
        }

        try {
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zip = new ZipInputStream(fis);

            byte[] buffer = new byte[1024];
            ZipEntry next = null;
            while ((next = zip.getNextEntry()) != null) {
                File file = new File(dir + File.separator + next.getName());
                (new File(file.getParent())).mkdirs();

                FileOutputStream fos = new FileOutputStream(file);
                int len;
                while ((len = zip.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                zip.closeEntry();
            }

            zip.closeEntry();
            zip.close();
            fis.close();
        }
        catch (IOException ex) {
            throw new IllegalStateException("Cannot read file", ex);
        }
    }

    public static boolean deleteDirectory(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }

        return deleteDirectory(file);
    }

    public static boolean deleteDirectory(File file) {
        boolean ok = true;

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (!deleteDirectory(child)) {
                    ok = false;
                }
            }
        }

        return file.delete() & ok;
    }

    /**
     * Get lines of text from ".java" files in a directory recursive
     *
     * @param directory Directory to start at
     *
     * @return Array has 3 values: Count of files, Amount of lines and Total
     *         size of parsed files
     */
    public static int[] linesOfDirectory(File directory) {
        int[] r = new int[3];
        r[0] = 0;
        r[1] = 0;
        r[2] = 0;

        for (final File fileEntry : directory.listFiles()) {
            if (fileEntry.isDirectory()) {
                int[] s = linesOfDirectory(fileEntry);
                r[0] += s[0];
                r[1] += s[1];
                r[2] += s[2];
            }
            else {
                if (fileEntry.getName().endsWith(".java")
                        && !fileEntry.getName().contains("Gui.")) {
                    r[0]++;

                    r[1] += readFile(fileEntry.getAbsolutePath()).size();
                    r[2] += (int) fileEntry.length();
                }
            }
        }

        return r;
    }

    /**
     * Copy a file
     *
     * @param original The path of the original file
     * @param copy     The path of the copy
     *
     * @return true, if the file has been copied
     */
    public static boolean copyFile(String original, String copy) {
        try (
                 InputStream in = new BufferedInputStream(
                        new FileInputStream(original));  OutputStream out = new BufferedOutputStream(
                        new FileOutputStream(copy))) {

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }

            out.close();
            in.close();
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

}
