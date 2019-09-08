/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.onosproject.xran.asn1lib.ber.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.ber.BerLength;
import org.onosproject.xran.asn1lib.ber.BerTag;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class BerNull implements Serializable {

    public final static BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.PRIMITIVE, BerTag.NULL_TAG);
    private static final long serialVersionUID = 1L;
    
    @JsonIgnore public byte[] code = null;

    public BerNull() {
    }

    public BerNull(byte[] code) {
    }

    public int encode(BerByteArrayOutputStream os) throws IOException {
        return encode(os, true);
    }

    public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

        int codeLength = BerLength.encodeLength(os, 0);

        if (withTag) {
            codeLength += tag.encode(os);
        }

        return codeLength;
    }

    public int decode(InputStream is) throws IOException {
        return decode(is, true);
    }

    public int decode(InputStream is, boolean withTag) throws IOException {

        int codeLength = 0;

        if (withTag) {
            codeLength += tag.decodeAndCheck(is);
        }

        BerLength length = new BerLength();
        codeLength += length.decode(is);

        if (length.val != 0) {
            throw new IOException("Decoded length of BerNull is not correct");
        }

        return codeLength;
    }

    
    @Override
    public String toString() {
        return "ASN1_NULL";
    }

}
