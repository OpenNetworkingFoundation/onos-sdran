/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.onosproject.xran.asn1lib.ber.types;

import com.fasterxml.jackson.annotation.JsonValue;
import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.ber.BerLength;
import org.onosproject.xran.asn1lib.ber.BerTag;
import org.onosproject.xran.asn1lib.ber.internal.Util;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class BerOctetString implements Serializable {

    public final static BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.PRIMITIVE, BerTag.OCTET_STRING_TAG);
    private static final long serialVersionUID = 1L;
    public byte[] value;

    public BerOctetString() {
    }

    public BerOctetString(byte[] value) {
        this.value = value;
    }

    public int encode(BerByteArrayOutputStream os) throws IOException {
        return encode(os, true);
    }

    public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

        os.write(value);
        int codeLength = value.length;

        codeLength += BerLength.encodeLength(os, codeLength);

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

        value = new byte[length.val];

        if (length.val != 0) {
            Util.readFully(is, value);
            codeLength += length.val;
        }

        return codeLength;

    }

    @JsonValue
    @Override
    public String toString() {
        return DatatypeConverter.printHexBinary(value);
    }

}
