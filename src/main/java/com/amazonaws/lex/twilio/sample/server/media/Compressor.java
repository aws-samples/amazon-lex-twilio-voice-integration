package com.amazonaws.lex.twilio.sample.server.media;

/**
 * referenced from http://thorntonzone.com/manuals/Compression/Fax,%20IBM%20MMR/MMSC/mmsc/uk/co/mmscomputing/sound/CompressInputStream.java
 * <p>
 * Do not use this class in production
 */
abstract class Compressor {
    protected abstract int compress(short sample);
}
