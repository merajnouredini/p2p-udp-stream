package ir.ac.iust.protocol;

/**
 * Packet Model Class
 *
 * Created by meraj on 12/20/15.
 */
public class PKT{
    public int type;
    public byte[] data;

    public PKT(int type, byte[] data) {
        this.type = type;
        this.data = data;
    }
}