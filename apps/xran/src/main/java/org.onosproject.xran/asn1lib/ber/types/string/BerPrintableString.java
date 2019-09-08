/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.onosproject.xran.asn1lib.ber.types.string;

import com.fasterxml.jackson.annotation.JsonValue;
import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.ber.BerTag;
import org.onosproject.xran.asn1lib.ber.types.BerOctetString;

import java.io.IOException;
import java.io.InputStream;

public class BerPrintableString extends BerOctetString {

    public final static BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.PRIMITIVE, BerTag.PRINTABLE_STRING_TAG);
    private static final long serialVersionUID = 1L;

    public BerPrintableString() {
    }

    public BerPrintableString(byte[] value) {
        this.value = value;
    }

    
    @Override
    public String toString() {
        return new String(value);
    }

    @Override
    public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

        int codeLength = super.encode(os, false);

        if (withTag) {
            codeLength += tag.encode(os);
        }

        return codeLength;
    }

    @Override
    public int decode(InputStream is, boolean withTag) throws IOException {

        int codeLength = 0;

        if (withTag) {
            codeLength += tag.decodeAndCheck(is);
        }

        codeLength += super.decode(is, false);

        return codeLength;
    }

}
