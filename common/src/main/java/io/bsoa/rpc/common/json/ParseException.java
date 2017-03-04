/*
 * Copyright 2016 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.bsoa.rpc.common.json;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2016/7/31 18:16. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class ParseException extends RuntimeException {
    private static final long serialVersionUID = 2560442967697088006L;
    private int position = 0;
    private String json = "";

    /**
     * Constructs a new json exception with the specified detail message.
     *
     * @param json     the json text which cause JSONParseException
     * @param position the position of illegal escape char at json text;
     * @param message  the detail message. The detail message is saved for
     *                 later retrieval by the {@link #getMessage()} method.
     */
    public ParseException(String json, int position, String message) {
        super(message);
        this.json = json;
        this.position = position;
    }

    /**
     * Get message about error when parsing illegal json
     *
     * @return error message
     */
    @Override
    public String getMessage() {
        final int maxTipLength = 10;
        int end = position + 1;
        int start = end - maxTipLength;
        if (start < 0) start = 0;
        if (end > json.length()) end = json.length();
        return String.format("%s  (%d):%s", json.substring(start, end), position, super.getMessage());
    }

    public String getJson() {
        return this.json;
    }

    public int getPosition() {
        return this.position;
    }

}
