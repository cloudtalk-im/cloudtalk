package com.zhangwuji.im.ui.entity;

import com.zhangwuji.im.DB.entity.User;

import java.io.Serializable;

public class NearByUser implements Serializable {

    private static final long serialVersionUID = 1192604362014099435L;

    protected int id;
    protected int peerId;
    /** Not-null value.
     * userEntity --> nickName
     * groupEntity --> groupName
     * */
    protected String mainName;
    /** Not-null value.*/
    protected String avatar;
    protected int created;
    protected int updated;
    private String dists;
    private String sign_info;
    private String nickname;
    private int sex;
    /** Not-null value. */
    private String pinyinName;
    /** Not-null value. */
    /** Not-null value. */
    private String phone;
    /** Not-null value. */
    private String email;
    private int departmentId;
    private int status;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPeerId() {
        return peerId;
    }

    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }

    public String getMainName() {
        return mainName;
    }

    public void setMainName(String mainName) {
        this.mainName = mainName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public String getDists() {
        return dists;
    }

    public void setDists(String dists) {
        this.dists = dists;
    }

    public String getSign_info() {
        return sign_info;
    }

    public void setSign_info(String sign_info) {
        this.sign_info = sign_info;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getPinyinName() {
        return pinyinName;
    }

    public void setPinyinName(String pinyinName) {
        this.pinyinName = pinyinName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
