/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.onosproject.xran.asn1lib.ber.types;

import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.ber.BerTag;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

public class BerEnum extends BerInteger {

    public final static BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.PRIMITIVE, BerTag.ENUMERATED_TAG);
    private static final long serialVersionUID = 1L;

    public BerEnum() {
    }

    public BerEnum(byte[] code) {
        this.code = code;
    }

    public BerEnum(BigInteger val) {
        this.value = val;
    }

    public BerEnum(long val) {
        this.value = BigInteger.valueOf(val);
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
