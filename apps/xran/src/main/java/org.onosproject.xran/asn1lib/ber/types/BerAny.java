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

public class BerAny implements Serializable {

    private static final long serialVersionUID = 1L;

    public byte[] value;

    public BerAny() {
    }

    public BerAny(byte[] value) {
        this.value = value;
    }

    public int encode(BerByteArrayOutputStream os) throws IOException {
        os.write(value);
        return value.length;
    }

    public int decode(InputStream is) throws IOException {

        return decode(is, null);
    }

    public int decode(InputStream is, BerTag tag) throws IOException {

        int decodedLength = 0;

        int tagLength;

        if (tag == null) {
            tag = new BerTag();
            tagLength = tag.decode(is);
            decodedLength += tagLength;
        } else {
            tagLength = tag.encode(new BerByteArrayOutputStream(10));
        }

        BerLength lengthField = new BerLength();
        int lengthLength = lengthField.decode(is);
        decodedLength += lengthLength + lengthField.val;

        value = new byte[tagLength + lengthLength + lengthField.val];

        Util.readFully(is, value, tagLength + lengthLength, lengthField.val);
        BerByteArrayOutputStream os = new BerByteArrayOutputStream(value, tagLength + lengthLength - 1);
        BerLength.encodeLength(os, lengthField.val);
        tag.encode(os);

        return decodedLength;
    }

    @JsonValue
    @Override
    public String toString() {
        return DatatypeConverter.printHexBinary(value);
    }

}
