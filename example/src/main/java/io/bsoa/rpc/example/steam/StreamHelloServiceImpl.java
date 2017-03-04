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
package io.bsoa.rpc.example.steam;

import io.bsoa.rpc.invoke.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/10 23:50. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class StreamHelloServiceImpl implements StreamHelloService {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(StreamHelloServiceImpl.class);

    @Override
    public String download(String name, StreamObserver<String> msgs) {
        LOGGER.info("Download {}", name);
        try {
            msgs.onValue("aaaaaaaaaaaaaaaa");
            msgs.onValue("bbbbbbbbbbbbbbbbbbbbbb");
            msgs.onValue("cccc");
        } catch (Exception e) {
            msgs.onError(e);
        } finally {
            msgs.onCompleted();
        }
        return "Thanks for download " + name;
    }

    @Override
    public StreamObserver<String> upload(String name) {
        LOGGER.info("Start Uploading {}", name);
        return new StreamObserver<String>() {
            @Override
            public void onValue(String value) {
                LOGGER.info("Service receive data: {}", name, value);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Upload {} end", name);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("Upload  " + name + " exception", t);
            }
        };
    }

    @Override
    public StreamObserver<String> sayHello(String name, StreamObserver<String> msgs) {
        LOGGER.info("Download {}", name);
        return new StreamObserver<String>() {
            @Override
            public void onValue(String value) {
                LOGGER.info("{}-{}", name, value);
                msgs.onValue(value + " over!");
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Upload {} over", name);
                msgs.onValue("Upload " + name + " over");
                msgs.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("Upload  " + name + " exception", t);
            }
        };
    }
}
