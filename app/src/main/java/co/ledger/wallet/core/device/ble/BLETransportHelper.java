package co.ledger.wallet.core.device.ble;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Vector;

public class BLETransportHelper {

    public static final int COMMAND_APDU = 0x05;
    public static final int COMMAND_ACK = 0x06;
    private static final int OFFSET_COMMAND = 0;
    private static final int OFFSET_SEQH = 1;
    private static final int OFFSET_SEQL = 2;
    private static final int OFFSET_SEQ0_LENH = 3;
    private static final int OFFSET_SEQ0_LENL = 4;
    private static final int OFFSET_SEQ0_DATA = 5;
    private static final int OFFSET_SEQN_DATA = 3;

    public static byte[][] split(int command, byte[] dataToTransport, int chunksize) {
        if (chunksize < 8) {
            throw new RuntimeException("Invalid chunk size");
        }
        ArrayList<byte[]> arrays = new ArrayList<byte[]>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int remaining_length = dataToTransport.length;
        int offset = 0;
        int seq = 0;

        while (remaining_length > 0) {
            // new packet
            baos.write(command);
            // sequence
            baos.write(seq >> 8);
            baos.write(seq);

            int l = 0;
            if (seq == 0) {
                // first packet has the total transport length
                baos.write(remaining_length >> 8);
                baos.write(remaining_length);
                // data length is minus length field for the first packet
                l = Math.min(chunksize - OFFSET_SEQ0_DATA, remaining_length);
            } else {
                l = Math.min(chunksize - OFFSET_SEQN_DATA, remaining_length);
            }
            baos.write(dataToTransport, offset, l);
            remaining_length -= l;
            offset += l;
            arrays.add(baos.toByteArray());
            baos.reset();
            seq++;
        }

        return arrays.toArray(new byte[1][]);
    }

    public static byte[] join(int command, Vector<byte[]> chunks) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int seq = 0;
        int length = -1;

        for (int i=0; i<chunks.size(); i++) {
            byte[] chunk = chunks.get(i);
            if (chunk[OFFSET_COMMAND] != command) {
                throw new RuntimeException("Unexpected command");
            }
            // check sequence ordering
            if ((chunk[OFFSET_SEQH] & 0xFF) != ((seq >> 8) & 0xFF)
                    || (chunk[OFFSET_SEQL] & 0xFF) != ((seq) & 0xFF)) {
                throw new RuntimeException(
                        "Invalid chunk sequence counter, expected:"
                                + (seq & 0xFFFF)
                                + ", observed:"
                                + (((chunk[OFFSET_SEQH] & 0xFF) << 8) | (chunk[OFFSET_SEQL] & 0xFF)));
            }
            seq++;

            // process the first packet
            if (i == 0) {
                // read total length before data
                length = (((chunk[OFFSET_SEQ0_LENH] & 0xFF) << 8) | (chunk[OFFSET_SEQ0_LENL] & 0xFF));
                if (chunk.length - OFFSET_SEQ0_DATA > length) {
                    throw new RuntimeException("First chunk data length bigger than total data length");
                }
                baos.write(chunk, OFFSET_SEQ0_DATA, chunk.length
                        - OFFSET_SEQ0_DATA);
                length -= chunk.length - OFFSET_SEQ0_DATA;
            }
            // process other packets
            else {
                if (chunk.length - OFFSET_SEQN_DATA > length) {
                    throw new RuntimeException("Too much data in the last chunk");
                }

                baos.write(chunk, OFFSET_SEQN_DATA, chunk.length
                        - OFFSET_SEQN_DATA);
                length -= chunk.length - OFFSET_SEQN_DATA;
            }
        }

        // not all chunks received to be able to concat
        if (length != 0) {
            return null;
        }
        // return concat data
        return baos.toByteArray();
    }
}

