/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.onosproject.xran.asn1lib.ber;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class BerByteArrayOutputStream extends OutputStream {

    private final boolean automaticResize;
    public byte[] buffer;
    public int index;

    /**
     * Creates a <code>BerByteArrayOutputStream</code> with a byte array of size <code>bufferSize</code>. The buffer
     * will not be resized automatically. Use {@link #BerByteArrayOutputStream(int, boolean)} instead if you want the
     * buffer to be dynamically resized.
     *
     * @param bufferSize the size of the underlying buffer
     */
    public BerByteArrayOutputStream(int bufferSize) {
        this(new byte[bufferSize], bufferSize - 1, false);
    }

    public BerByteArrayOutputStream(int bufferSize, boolean automaticResize) {
        this(new byte[bufferSize], bufferSize - 1, automaticResize);
    }

    public BerByteArrayOutputStream(byte[] buffer) {
        this(buffer, buffer.length - 1, false);
    }

    public BerByteArrayOutputStream(byte[] buffer, int startingIndex) {
        this(buffer, startingIndex, false);
    }

    public BerByteArrayOutputStream(byte[] buffer, int startingIndex, boolean automaticResize) {
        if (buffer.length <= 0) {
            throw new IllegalArgumentException("buffer size may not be <= 0");
        }
        this.buffer = buffer;
        index = startingIndex;
        this.automaticResize = automaticResize;
    }

    @Override
    public void write(int arg0) throws IOException {
        write((byte) arg0);
    }

    public void write(byte arg0) throws IOException {
        try {
            buffer[index] = arg0;
        } catch (ArrayIndexOutOfBoundsException e) {
            if (automaticResize) {
                resize();
                buffer[index] = arg0;
            } else {
                throw new ArrayIndexOutOfBoundsException("buffer.length = " + buffer.length);
            }
        }
        index--;
    }

    private void resize() {
        byte[] newBuffer = new byte[buffer.length * 2];
        System.arraycopy(buffer, index + 1, newBuffer, buffer.length + index + 1, buffer.length - index - 1);
        index += buffer.length;
        buffer = newBuffer;

    }

    @Override
    public void write(byte[] byteArray) throws IOException {
        for (int i = byteArray.length - 1; i >= 0; i--) {
            try {
                buffer[index] = byteArray[i];
            } catch (ArrayIndexOutOfBoundsException e) {
                if (automaticResize) {
                    resize();
                    buffer[index] = byteArray[i];
                } else {
                    throw new ArrayIndexOutOfBoundsException("buffer.length = " + buffer.length);
                }
            }
            index--;
        }
    }

    /**
     * Returns a new array containing the subarray of the stream array that contains the coded content.
     *
     * @return a new array containing the subarray of the stream array
     */
    public byte[] getArray() {
        if (index == -1) {
            return buffer;
        }
        int subBufferLength = buffer.length - index - 1;
        byte[] subBuffer = new byte[subBufferLength];
        System.arraycopy(buffer, index + 1, subBuffer, 0, subBufferLength);
        return subBuffer;

    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(buffer, index + 1, buffer.length - (index + 1));
    }

    public void reset() {
        index = buffer.length - 1;
    }
}
