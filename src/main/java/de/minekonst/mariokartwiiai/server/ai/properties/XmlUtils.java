package de.minekonst.mariokartwiiai.server.ai.properties;

import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

public class XmlUtils {

    public static String encodeObject(Object o) {
        try {
            // Write Object -> Zip -> Base64 Encode
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            OutputStream stream = arr;
            stream = new GZIPOutputStream(stream);
            ObjectOutput out = new FSTObjectOutput(stream);
            out.writeObject(o);
            out.close();

            return Base64.getEncoder().encodeToString(arr.toByteArray());
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public static <N> String encodeNetwork(LearningMethod<N> method, N network) {
        try {
            // Write Object -> Zip -> Base64 Encode
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            OutputStream stream = new GZIPOutputStream(arr);
            method.writeNetwork(network, stream);
            stream.close();

            return Base64.getEncoder().encodeToString(arr.toByteArray());
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T decodeObject(String s, Class<T> cls) {
        try {
            // Base64 Decode -> Unzip -> Read Object
            InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(s));
            stream = new GZIPInputStream(stream);
            ObjectInput in = new FSTObjectInput(stream);
            Object o = in.readObject();
            in.close();

            return (T) o;
        }
        catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <N> N decodeNetwork(LearningMethod<N> method, String s) {
        try {
            // Base64 Decode -> Unzip -> Read Object
            InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(s));
            stream = new GZIPInputStream(stream);
            N n = method.loadNetwork(stream);
            stream.close();
            return n;
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private XmlUtils() {
    }

}
