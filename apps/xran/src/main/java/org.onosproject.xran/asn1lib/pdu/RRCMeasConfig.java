/**
 * This class file was automatically generated by jASN1 v1.8.2 (http://www.openmuc.org)
 */

package org.onosproject.xran.asn1lib.pdu;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.onosproject.xran.asn1lib.api.*;
import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.ber.BerLength;
import org.onosproject.xran.asn1lib.ber.BerTag;
import org.onosproject.xran.asn1lib.ber.types.string.BerUTF8String;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RRCMeasConfig implements Serializable {

    public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);
    private static final long serialVersionUID = 1L;
    @JsonIgnore
    public byte[] code = null;
    private ECGI ecgi = null;
    private Crnti crnti = null;
    private MeasObjects measObjects = null;
    private ReportConfigs reportConfigs = null;
    private MeasIds measIds = null;

    public RRCMeasConfig() {
    }

    public RRCMeasConfig(byte[] code) {
        this.code = code;
    }

    public static XrancPdu constructPacket(
            ECGI ecgi, CRNTI crnti, MeasObjects measObjects, ReportConfigs reportConfigs,
            MeasIds measIds, int rxSignalInterval
    ) {
        RRCMeasConfig rrcMeasConfig = new RRCMeasConfig();

        Crnti crntilist = new Crnti();
        crntilist.getCRNTI().add(crnti);

        rrcMeasConfig.setEcgi(ecgi);
        rrcMeasConfig.setCrnti(crntilist);
        rrcMeasConfig.setMeasIds(measIds);
        rrcMeasConfig.setMeasObjects(measObjects);
        rrcMeasConfig.setReportConfigs(reportConfigs);

        BerUTF8String ver = null;
        try {
            ver = new BerUTF8String("5");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    XrancApiID apiID = new XrancApiID(15);
        XrancPduBody body = new XrancPduBody();
        body.setRRCMeasConfig(rrcMeasConfig);

        XrancPduHdr hdr = new XrancPduHdr();
        hdr.setVer(ver);
        hdr.setApiId(apiID);

        XrancPdu pdu = new XrancPdu();
        pdu.setBody(body);
        pdu.setHdr(hdr);

        return pdu;
    }

    public ECGI getEcgi() {
        return ecgi;
    }

    public void setEcgi(ECGI ecgi) {
        this.ecgi = ecgi;
    }

    public Crnti getCrnti() {
        return crnti;
    }

    public void setCrnti(Crnti crnti) {
        this.crnti = crnti;
    }

    public MeasObjects getMeasObjects() {
        return measObjects;
    }

    public void setMeasObjects(MeasObjects measObjects) {
        this.measObjects = measObjects;
    }

    public ReportConfigs getReportConfigs() {
        return reportConfigs;
    }

    public void setReportConfigs(ReportConfigs reportConfigs) {
        this.reportConfigs = reportConfigs;
    }

    public MeasIds getMeasIds() {
        return measIds;
    }

    public void setMeasIds(MeasIds measIds) {
        this.measIds = measIds;
    }

    public int encode(BerByteArrayOutputStream os) throws IOException {
        return encode(os, true);
    }

    public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

        if (code != null) {
            for (int i = code.length - 1; i >= 0; i--) {
                os.write(code[i]);
            }
            if (withTag) {
                return tag.encode(os) + code.length;
            }
            return code.length;
        }

        int codeLength = 0;
        codeLength += measIds.encode(os, false);
        // write tag: CONTEXT_CLASS, CONSTRUCTED, 4
        os.write(0xA4);
        codeLength += 1;

        codeLength += reportConfigs.encode(os, false);
        // write tag: CONTEXT_CLASS, CONSTRUCTED, 3
        os.write(0xA3);
        codeLength += 1;

        codeLength += measObjects.encode(os, false);
        // write tag: CONTEXT_CLASS, CONSTRUCTED, 2
        os.write(0xA2);
        codeLength += 1;

        if (crnti != null) {
            codeLength += crnti.encode(os, false);
            // write tag: CONTEXT_CLASS, CONSTRUCTED, 1
            os.write(0xA1);
            codeLength += 1;
        }

        codeLength += ecgi.encode(os, false);
        // write tag: CONTEXT_CLASS, CONSTRUCTED, 0
        os.write(0xA0);
        codeLength += 1;

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
        int subCodeLength = 0;
        BerTag berTag = new BerTag();

        if (withTag) {
            codeLength += tag.decodeAndCheck(is);
        }

        BerLength length = new BerLength();
        codeLength += length.decode(is);

        int totalLength = length.val;
        codeLength += totalLength;

        subCodeLength += berTag.decode(is);
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 0)) {
            ecgi = new ECGI();
            subCodeLength += ecgi.decode(is, false);
            subCodeLength += berTag.decode(is);
        } else {
            throw new IOException("Tag does not match the mandatory sequence element tag.");
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
            crnti = new Crnti();
            subCodeLength += crnti.decode(is, false);
            subCodeLength += berTag.decode(is);
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 2)) {
            measObjects = new MeasObjects();
            subCodeLength += measObjects.decode(is, false);
            subCodeLength += berTag.decode(is);
        } else {
            throw new IOException("Tag does not match the mandatory sequence element tag.");
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 3)) {
            reportConfigs = new ReportConfigs();
            subCodeLength += reportConfigs.decode(is, false);
            subCodeLength += berTag.decode(is);
        } else {
            throw new IOException("Tag does not match the mandatory sequence element tag.");
        }

        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 4)) {
            measIds = new MeasIds();
            subCodeLength += measIds.decode(is, false);
            if (subCodeLength == totalLength) {
                return codeLength;
            }
        }
        throw new IOException("Unexpected end of sequence, length tag: " + totalLength + ", actual sequence length: " + subCodeLength);


    }

    public void encodeAndSave(int encodingSizeGuess) throws IOException {
        BerByteArrayOutputStream os = new BerByteArrayOutputStream(encodingSizeGuess);
        encode(os, false);
        code = os.getArray();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendAsString(sb, 0);
        return sb.toString();
    }

    public void appendAsString(StringBuilder sb, int indentLevel) {

        sb.append("{");
        sb.append("\n");
        for (int i = 0; i < indentLevel + 1; i++) {
            sb.append("\t");
        }
        if (ecgi != null) {
            sb.append("ecgi: ");
            ecgi.appendAsString(sb, indentLevel + 1);
        } else {
            sb.append("ecgi: <empty-required-field>");
        }

        if (crnti != null) {
            sb.append(",\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            sb.append("crnti: ");
            crnti.appendAsString(sb, indentLevel + 1);
        }

        sb.append(",\n");
        for (int i = 0; i < indentLevel + 1; i++) {
            sb.append("\t");
        }
        if (measObjects != null) {
            sb.append("measObjects: ");
            measObjects.appendAsString(sb, indentLevel + 1);
        } else {
            sb.append("measObjects: <empty-required-field>");
        }

        sb.append(",\n");
        for (int i = 0; i < indentLevel + 1; i++) {
            sb.append("\t");
        }
        if (reportConfigs != null) {
            sb.append("reportConfigs: ");
            reportConfigs.appendAsString(sb, indentLevel + 1);
        } else {
            sb.append("reportConfigs: <empty-required-field>");
        }

        sb.append(",\n");
        for (int i = 0; i < indentLevel + 1; i++) {
            sb.append("\t");
        }
        if (measIds != null) {
            sb.append("measIds: ");
            measIds.appendAsString(sb, indentLevel + 1);
        } else {
            sb.append("measIds: <empty-required-field>");
        }

        sb.append("\n");
        for (int i = 0; i < indentLevel; i++) {
            sb.append("\t");
        }
        sb.append("}");
    }

    public static class Crnti implements Serializable {

        public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);
        private static final long serialVersionUID = 1L;
        @JsonIgnore
        public byte[] code = null;
        private List<CRNTI> seqOf = null;

        public Crnti() {
            seqOf = new ArrayList<CRNTI>();
        }

        public Crnti(byte[] code) {
            this.code = code;
        }

        @JsonValue
        public List<CRNTI> getCRNTI() {
            if (seqOf == null) {
                seqOf = new ArrayList<CRNTI>();
            }
            return seqOf;
        }

        public int encode(BerByteArrayOutputStream os) throws IOException {
            return encode(os, true);
        }

        public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

            if (code != null) {
                for (int i = code.length - 1; i >= 0; i--) {
                    os.write(code[i]);
                }
                if (withTag) {
                    return tag.encode(os) + code.length;
                }
                return code.length;
            }

            int codeLength = 0;
            for (int i = (seqOf.size() - 1); i >= 0; i--) {
                codeLength += seqOf.get(i).encode(os, true);
            }

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
            int subCodeLength = 0;
            if (withTag) {
                codeLength += tag.decodeAndCheck(is);
            }

            BerLength length = new BerLength();
            codeLength += length.decode(is);
            int totalLength = length.val;

            while (subCodeLength < totalLength) {
                CRNTI element = new CRNTI();
                subCodeLength += element.decode(is, true);
                seqOf.add(element);
            }
            if (subCodeLength != totalLength) {
                throw new IOException("Decoded SequenceOf or SetOf has wrong length. Expected " + totalLength + " but has " + subCodeLength);

            }
            codeLength += subCodeLength;

            return codeLength;
        }

        public void encodeAndSave(int encodingSizeGuess) throws IOException {
            BerByteArrayOutputStream os = new BerByteArrayOutputStream(encodingSizeGuess);
            encode(os, false);
            code = os.getArray();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendAsString(sb, 0);
            return sb.toString();
        }

        public void appendAsString(StringBuilder sb, int indentLevel) {

            sb.append("{\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            if (seqOf == null) {
                sb.append("null");
            } else {
                Iterator<CRNTI> it = seqOf.iterator();
                if (it.hasNext()) {
                    sb.append(it.next());
                    while (it.hasNext()) {
                        sb.append(",\n");
                        for (int i = 0; i < indentLevel + 1; i++) {
                            sb.append("\t");
                        }
                        sb.append(it.next());
                    }
                }
            }

            sb.append("\n");
            for (int i = 0; i < indentLevel; i++) {
                sb.append("\t");
            }
            sb.append("}");
        }

    }

    public static class MeasObjects implements Serializable {

        public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);
        private static final long serialVersionUID = 1L;
        @JsonIgnore
        public byte[] code = null;
        private List<MeasObject> seqOf = null;

        public MeasObjects() {
            seqOf = new ArrayList<MeasObject>();
        }

        public MeasObjects(byte[] code) {
            this.code = code;
        }

        @JsonValue
        public List<MeasObject> getMeasObject() {
            if (seqOf == null) {
                seqOf = new ArrayList<MeasObject>();
            }
            return seqOf;
        }

        public int encode(BerByteArrayOutputStream os) throws IOException {
            return encode(os, true);
        }

        public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

            if (code != null) {
                for (int i = code.length - 1; i >= 0; i--) {
                    os.write(code[i]);
                }
                if (withTag) {
                    return tag.encode(os) + code.length;
                }
                return code.length;
            }

            int codeLength = 0;
            for (int i = (seqOf.size() - 1); i >= 0; i--) {
                codeLength += seqOf.get(i).encode(os, true);
            }

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
            int subCodeLength = 0;
            if (withTag) {
                codeLength += tag.decodeAndCheck(is);
            }

            BerLength length = new BerLength();
            codeLength += length.decode(is);
            int totalLength = length.val;

            while (subCodeLength < totalLength) {
                MeasObject element = new MeasObject();
                subCodeLength += element.decode(is, true);
                seqOf.add(element);
            }
            if (subCodeLength != totalLength) {
                throw new IOException("Decoded SequenceOf or SetOf has wrong length. Expected " + totalLength + " but has " + subCodeLength);

            }
            codeLength += subCodeLength;

            return codeLength;
        }

        public void encodeAndSave(int encodingSizeGuess) throws IOException {
            BerByteArrayOutputStream os = new BerByteArrayOutputStream(encodingSizeGuess);
            encode(os, false);
            code = os.getArray();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendAsString(sb, 0);
            return sb.toString();
        }

        public void appendAsString(StringBuilder sb, int indentLevel) {

            sb.append("{\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            if (seqOf == null) {
                sb.append("null");
            } else {
                Iterator<MeasObject> it = seqOf.iterator();
                if (it.hasNext()) {
                    it.next().appendAsString(sb, indentLevel + 1);
                    while (it.hasNext()) {
                        sb.append(",\n");
                        for (int i = 0; i < indentLevel + 1; i++) {
                            sb.append("\t");
                        }
                        it.next().appendAsString(sb, indentLevel + 1);
                    }
                }
            }

            sb.append("\n");
            for (int i = 0; i < indentLevel; i++) {
                sb.append("\t");
            }
            sb.append("}");
        }

    }

    public static class ReportConfigs implements Serializable {

        public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);
        private static final long serialVersionUID = 1L;
        @JsonIgnore
        public byte[] code = null;
        private List<ReportConfig> seqOf = null;

        public ReportConfigs() {
            seqOf = new ArrayList<ReportConfig>();
        }

        public ReportConfigs(byte[] code) {
            this.code = code;
        }

        @JsonValue
        public List<ReportConfig> getReportConfig() {
            if (seqOf == null) {
                seqOf = new ArrayList<ReportConfig>();
            }
            return seqOf;
        }

        public int encode(BerByteArrayOutputStream os) throws IOException {
            return encode(os, true);
        }

        public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

            if (code != null) {
                for (int i = code.length - 1; i >= 0; i--) {
                    os.write(code[i]);
                }
                if (withTag) {
                    return tag.encode(os) + code.length;
                }
                return code.length;
            }

            int codeLength = 0;
            for (int i = (seqOf.size() - 1); i >= 0; i--) {
                codeLength += seqOf.get(i).encode(os, true);
            }

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
            int subCodeLength = 0;
            if (withTag) {
                codeLength += tag.decodeAndCheck(is);
            }

            BerLength length = new BerLength();
            codeLength += length.decode(is);
            int totalLength = length.val;

            while (subCodeLength < totalLength) {
                ReportConfig element = new ReportConfig();
                subCodeLength += element.decode(is, true);
                seqOf.add(element);
            }
            if (subCodeLength != totalLength) {
                throw new IOException("Decoded SequenceOf or SetOf has wrong length. Expected " + totalLength + " but has " + subCodeLength);

            }
            codeLength += subCodeLength;

            return codeLength;
        }

        public void encodeAndSave(int encodingSizeGuess) throws IOException {
            BerByteArrayOutputStream os = new BerByteArrayOutputStream(encodingSizeGuess);
            encode(os, false);
            code = os.getArray();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendAsString(sb, 0);
            return sb.toString();
        }

        public void appendAsString(StringBuilder sb, int indentLevel) {

            sb.append("{\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            if (seqOf == null) {
                sb.append("null");
            } else {
                Iterator<ReportConfig> it = seqOf.iterator();
                if (it.hasNext()) {
                    it.next().appendAsString(sb, indentLevel + 1);
                    while (it.hasNext()) {
                        sb.append(",\n");
                        for (int i = 0; i < indentLevel + 1; i++) {
                            sb.append("\t");
                        }
                        it.next().appendAsString(sb, indentLevel + 1);
                    }
                }
            }

            sb.append("\n");
            for (int i = 0; i < indentLevel; i++) {
                sb.append("\t");
            }
            sb.append("}");
        }

    }

    public static class MeasIds implements Serializable {

        public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);
        private static final long serialVersionUID = 1L;
        @JsonIgnore
        public byte[] code = null;
        private List<MeasID> seqOf = null;

        public MeasIds() {
            seqOf = new ArrayList<MeasID>();
        }

        public MeasIds(byte[] code) {
            this.code = code;
        }

        @JsonValue
        public List<MeasID> getMeasID() {
            if (seqOf == null) {
                seqOf = new ArrayList<MeasID>();
            }
            return seqOf;
        }

        public int encode(BerByteArrayOutputStream os) throws IOException {
            return encode(os, true);
        }

        public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {

            if (code != null) {
                for (int i = code.length - 1; i >= 0; i--) {
                    os.write(code[i]);
                }
                if (withTag) {
                    return tag.encode(os) + code.length;
                }
                return code.length;
            }

            int codeLength = 0;
            for (int i = (seqOf.size() - 1); i >= 0; i--) {
                codeLength += seqOf.get(i).encode(os, true);
            }

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
            int subCodeLength = 0;
            if (withTag) {
                codeLength += tag.decodeAndCheck(is);
            }

            BerLength length = new BerLength();
            codeLength += length.decode(is);
            int totalLength = length.val;

            while (subCodeLength < totalLength) {
                MeasID element = new MeasID();
                subCodeLength += element.decode(is, true);
                seqOf.add(element);
            }
            if (subCodeLength != totalLength) {
                throw new IOException("Decoded SequenceOf or SetOf has wrong length. Expected " + totalLength + " but has " + subCodeLength);

            }
            codeLength += subCodeLength;

            return codeLength;
        }

        public void encodeAndSave(int encodingSizeGuess) throws IOException {
            BerByteArrayOutputStream os = new BerByteArrayOutputStream(encodingSizeGuess);
            encode(os, false);
            code = os.getArray();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendAsString(sb, 0);
            return sb.toString();
        }

        public void appendAsString(StringBuilder sb, int indentLevel) {

            sb.append("{\n");
            for (int i = 0; i < indentLevel + 1; i++) {
                sb.append("\t");
            }
            if (seqOf == null) {
                sb.append("null");
            } else {
                Iterator<MeasID> it = seqOf.iterator();
                if (it.hasNext()) {
                    it.next().appendAsString(sb, indentLevel + 1);
                    while (it.hasNext()) {
                        sb.append(",\n");
                        for (int i = 0; i < indentLevel + 1; i++) {
                            sb.append("\t");
                        }
                        it.next().appendAsString(sb, indentLevel + 1);
                    }
                }
            }

            sb.append("\n");
            for (int i = 0; i < indentLevel; i++) {
                sb.append("\t");
            }
            sb.append("}");
        }

    }

}

