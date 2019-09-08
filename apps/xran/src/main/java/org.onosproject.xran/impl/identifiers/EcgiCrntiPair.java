/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.xran.impl.identifiers;

import com.google.common.base.Objects;
import javafx.util.Pair;
import org.onosproject.xran.asn1lib.api.CRNTI;
import org.onosproject.xran.asn1lib.api.ECGI;

/**
 * Class to maintain pair of ECGI and CRNTI.
 */
public class EcgiCrntiPair extends Pair<ECGI, CRNTI> {

    /**
     * Creates a new pair.
     *
     * @param key   The key for this pair
     * @param value The value to use for this pair
     */
    public EcgiCrntiPair(ECGI key, CRNTI value) {
        super(key, value);
    }

    /**
     * Return a new EcgiCrntiPair.
     *
     * @param key ECGI
     * @param value CRNTI
     * @return EcgiCrntiPair
     */
    public static EcgiCrntiPair valueOf(ECGI key, CRNTI value) {
        return new EcgiCrntiPair(key, value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getKey(), getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EcgiCrntiPair) {
            return ((EcgiCrntiPair) o).getKey().equals(getKey()) &&
                    ((EcgiCrntiPair) o).getValue().equals(getValue());
        }
        return super.equals(o);
    }
}
