/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bsoa.rpc.registry.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/2/18 18:01. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class CuratorFrameworkTest {

    public static void main(String[] args) throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .sessionTimeoutMs(30000)
                .connectionTimeoutMs(30000)
                .canBeReadOnly(false)
                .retryPolicy(retryPolicy)
                //.namespace(namespace)
                .defaultData(null)
                .build();
        client.start();

//        create()增
//        delete(): 删
//        checkExists(): 判断是否存在
//        setData():  改
//        getData(): 查
//        所有这些方法都以forpath()结尾，辅以watch(监听)，withMode（指定模式），和inBackground（后台运行）等方法来使用。

        //此外，Curator还支持事务，一组crud操作同生同灭。代码如下
//        CuratorTransaction transaction = client.inTransaction();
//        Collection<CuratorTransactionResult> results = transaction.create()
//                .forPath("/a/path", "some data".getBytes()).and().setData()
//                .forPath("/another/path", "other data".getBytes()).and().delete().forPath("/yet/another/path")
//                .and().commit();

//        for (CuratorTransactionResult result : results) {
//            System.out.println(result.getForPath() + " - " + result.getType());
//        }

        client.create().creatingParentContainersIfNeeded().forPath("/my/path", "xxx".getBytes());
        byte[] bs =  client.getData().forPath("/my/path");
        System.out.println(new String(bs));

        client.close();
    }
}
