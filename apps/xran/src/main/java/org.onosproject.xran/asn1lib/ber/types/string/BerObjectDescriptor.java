/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.onosproject.xran.asn1lib.ber.types.string;

import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.ber.BerTag;

import java.io.IOException;
import java.io.InputStream;

public class BerObjectDescriptor extends BerGraphicString {

    public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.PRIMITIVE, BerTag.OBJECT_DESCRIPTOR_TAG);
    private static final long serialVersionUID = 1L;

    public BerObjectDescriptor() {
    }

    public BerObjectDescriptor(byte[] value) {
        super(value);
    }

    @Override
    public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

        int codeLength;

        codeLength = super.encode(os, false);
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
