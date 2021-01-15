package com.amazonaws.lex.twilio.sample.server.media;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * referenced from http://thorntonzone.com/manuals/Compression/Fax,%20IBM%20MMR/MMSC/mmsc/uk/co/mmscomputing/sound/CompressInputStream.java
 * <p>
 * Any credit for this work solely belongs to the original author of this code.
 * <p>
 * Do not use this class in production
 */
public class CompressInputStream extends FilterInputStream {

  /*
    Convert mono PCM byte stream into A-Law u-Law byte stream

    static AudioFormat alawformat= new AudioFormat(AudioFormat.Encoding.ALAW,8000,8,1,1,8000,false);
    static AudioFormat ulawformat= new AudioFormat(AudioFormat.Encoding.ULAW,8000,8,1,1,8000,false);

    PCM 8000.0 Hz, 16 bit, mono, SIGNED, little-endian
    static AudioFormat pcmformat = new AudioFormat(8000,16,1,true,false);

  */

    private static final Compressor alawcompressor = new ALawCompressor();
    private static final Compressor ulawcompressor = new ULawCompressor();

    private Compressor compressor;

    public CompressInputStream(InputStream in, boolean useALaw) {
        super(in);
        compressor = (useALaw) ? alawcompressor : ulawcompressor;
    }

    public int read() throws IOException {
        throw new IOException(getClass().getName() + ".read() :\n\tDo not support simple read().");
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int i, sample;
        byte[] inb;

        inb = new byte[len << 1];          // get 16bit PCM data
        len = in.read(inb);
        if (len == -1) {
            return -1;
        }

        i = 0;
        while (i < len) {
            sample = (inb[i++] & 0x00FF);
            sample |= (inb[i++] << 8);
            b[off++] = (byte) compressor.compress((short) sample);
        }
        return len >> 1;
    }
}
