/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.mock;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.util.ISO8601;

/**
 * Mock version of a {@link Value}.
 */
public class MockValue implements Value {

    private int type;
    private String stringifiedValue;

    @SuppressWarnings("unused")
    public MockValue() {
        // used by JAXB
    }

    public MockValue(String stringifiedValue) {
        this(PropertyType.STRING, stringifiedValue);
    }

    public MockValue(int type, String stringifiedValue) {
        this.type = type;
        this.stringifiedValue = stringifiedValue;
    }

    public MockValue(Value value) throws RepositoryException {
        this.type = value.getType();

        switch (type) {
        case PropertyType.STRING:
        {
            this.stringifiedValue = value.getString();
            break;
        }
        case PropertyType.DATE:
        {
            this.stringifiedValue = ISO8601.format(value.getDate());
            break;
        }
        case PropertyType.BOOLEAN:
        {
            this.stringifiedValue = Boolean.toString(value.getBoolean());
            break;
        }
        case PropertyType.LONG:
        {
            this.stringifiedValue = Long.toString(value.getLong());
            break;
        }
        case PropertyType.DOUBLE:
        {
            this.stringifiedValue = Double.toString(value.getDouble());
            break;
        }
        case PropertyType.DECIMAL:
        {
            this.stringifiedValue = value.getDecimal().toString();
            break;
        }
        default:
        {
            throw new UnsupportedOperationException("Unsupported type, " + type + ". Only primitive number/string values are currently supported.");
        }
        }
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getString() throws ValueFormatException {
        return stringifiedValue;
    }

    @Override
    public Calendar getDate() throws ValueFormatException {
        try {
            Calendar date = ISO8601.parse(stringifiedValue);

            if (date == null) {
                throw new ValueFormatException("Invalid date format (ISO8601). " + stringifiedValue);
            }

            return date;
        } catch (Exception e) {
            throw new ValueFormatException(e);
        }
    }

    @Override
    public boolean getBoolean() throws ValueFormatException {
        try {
            return Boolean.parseBoolean(stringifiedValue);
        } catch (Exception e) {
            throw new ValueFormatException(e);
        }
    }

    @Override
    public long getLong() throws ValueFormatException {
        try {
            return Long.parseLong(stringifiedValue);
        } catch (Exception e) {
            throw new ValueFormatException(e);
        }
    }

    @Override
    public double getDouble() throws ValueFormatException {
        try {
            return Double.parseDouble(stringifiedValue);
        } catch (Exception e) {
            throw new ValueFormatException(e);
        }
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException {
        try {
            return new BigDecimal(stringifiedValue);
        } catch (Exception e) {
            throw new ValueFormatException(e);
        }
    }

    @Override
    public InputStream getStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Binary getBinary() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof MockValue) {
            MockValue other = (MockValue)o;
            return stringifiedValue.equals(other.stringifiedValue);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return stringifiedValue.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + ";{type: " + type + ", stringifiedValue: '" + stringifiedValue + "'}";
    }
}
