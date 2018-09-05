package com.dji.mediaManagerDemo.Model;


import android.support.annotation.NonNull;

public class User {

    private String Name;
    private String Password;

    public User() {
    }

    public User(@NonNull String name,@NonNull String password) {
        Name = name;
        Password = password;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }
}

