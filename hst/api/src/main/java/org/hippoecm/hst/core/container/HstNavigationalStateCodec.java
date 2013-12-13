/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.UnsupportedEncodingException;

/**
 * Codec component interface to generate navigation state information string.
 * 
 * @version $Id: HstNavigationalStateCodec.java 37754 2013-01-09 16:14:45Z mdenburger $
 */
public interface HstNavigationalStateCodec {

    /**
     * Encodes the parameter with the specified character encoding
     * 
     * @param value
     * @param characterEncoding
     * @return
     * @throws UnsupportedEncodingException
     */
    String encodeParameters(String value, String characterEncoding) throws UnsupportedEncodingException;

    /**
     * Decodes the parameter with the specified character encoding
     * 
     * @param value
     * @param characterEncoding
     * @return
     * @throws UnsupportedEncodingException
     */
    String decodeParameters(String value, String characterEncoding) throws UnsupportedEncodingException;
    
}