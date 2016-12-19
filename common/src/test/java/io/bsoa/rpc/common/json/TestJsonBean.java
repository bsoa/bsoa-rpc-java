/**
 * TestJsonBean.java Created on 16-11-4 上午11:59
 * <p>
 * Copyright (c) 2016 by www.jd.com.
 */
package io.bsoa.rpc.common.json;

import java.util.ArrayList;

/**
 * Title: <br>
 *
 * Description: <br>
 *
 * Company: <a href=www.jd.com>京东</a><br>
 *
 * @author <a href=mailto:zhanggeng@jd.com>章耿</a>
 */
public class TestJsonBean {

    @JSONField(alias = "Name", isRequired = true)
    private String name;
    @JSONField(alias = "Sex")
    private boolean sex;
    private int age;
    @JSONField(skipIfNull = true)
    private ArrayList<TestJsonBean> friends;
    @JSONField(alias = "Remark")
    private Object[] remark;

    @JSONField(skipIfNull = true)
    private Status status;

    private Long step;

    private static String staticString;
    private transient String transString;
    @JSONIgnore
    private String ignoreString;

    public String getName() {
        return name;
    }

    public boolean isSex() {
        return sex;
    }

    public int getAge() {
        return age;
    }

    public ArrayList<TestJsonBean> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<TestJsonBean> friends) {
        this.friends = friends;
    }

    public Object[] getRemark() {
        return remark;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setRemark(Object[] remark){
        this.remark = remark;
    }

    public Long getStep() {
        return step;
    }

    public void setStep(Long step) {
        this.step = step;
    }

    public String getTransString() {
        return transString;
    }

    public void setTransString(String transString) {
        this.transString = transString;
    }

    public String getIgnoreString() {
        return ignoreString;
    }

    public void setIgnoreString(String ignoreString) {
        this.ignoreString = ignoreString;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        START(0, "启动"),
        STOP(1, "停止");

        int code;
        String name;
        Status(int code, String name) {
            this.code = code;
            this.name = name();
        }
    }
}

