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
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
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
                //.namespace(namespace)  // client操作的默认根路径
                .defaultData(null)
                .build();
        client.start();

        String path = "/my/path";
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

//        client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL)
//                .forPath("/my/path", "xxx".getBytes());
//        byte[] bs =  client.getData().forPath("/my/path");
//        System.out.println(new String(bs));

      /*  // 监听子节点的增加删除，子节点数据的变更
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
        pathChildrenCache.getListenable().addListener((client1, event) -> {
            ChildData data = event.getData();
            if (data == null) {
                System.out.println("Receive event: "
                        + "type=[" + event.getType() + "]");
            } else {
                System.out.println("Receive event: "
                        + "type=[" + event.getType() + "]"
                        + ", path=[" + data.getPath() + "]"
                        + ", data=[" + new String(data.getData()) + "]"
                        + ", stat=[" + data.getStat() + "]");
            }
        });
//        watcher.start(PathChildrenCache.StartMode.NORMAL);// 历史数据触发事件，CurrentData为空
        pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);// 历史数据不触发事件，而是初始化到CurrentData
//        watcher.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);// 历史数据触发事件，CurrentData为空，最后会收到一个加载完毕事件
        System.out.println("Register zk watcher successfully!");

        List<ChildData> datas = pathChildrenCache.getCurrentData();
        System.out.println("datas.size:" + datas.size());
        for (ChildData data : datas) {
            System.out.println("current data:" + data);
        }*/


        // 监听当前节点的值变化
        /*NodeCache nodeCache = new NodeCache(client, path);
        nodeCache.getListenable().addListener(() -> {
            System.out.println(nodeCache.getCurrentData());
        });
//        nodeCache.start(false); // 不初始化，走事件
        nodeCache.start(true); // 走初始化，不走事件
        ChildData nodeCacheData = nodeCache.getCurrentData();
        System.out.println("current data:" + nodeCacheData);*/

        // 监听当前节点的值变化，以及子节点，以及子节点的子节点
        TreeCache treeCache = new TreeCache(client, path);
        treeCache.getListenable().addListener((client1, event) -> {
            ChildData data = event.getData();
            if (data == null) {
                System.out.println("Receive event: "
                        + "type=[" + event.getType() + "]");
            } else {
                System.out.println("Receive event: "
                        + "type=[" + event.getType() + "]"
                        + ", path=[" + data.getPath() + "]"
                        + ", data=[" + data.getData() + "]"
                        + ", stat=[" + data.getStat() + "]");
            }
        });
        treeCache.start();// 只有一种模式 PathChildrenCache.StartMode.POST_INITIALIZED_EVENT
        // 历史数据触发事件，CurrentData为空，最后会收到一个加载完毕事件
        ChildData treeCacheData = treeCache.getCurrentData(path);
        System.out.println("current data:" + treeCacheData);

        Thread.sleep(Integer.MAX_VALUE);

        client.close();
    }
}
