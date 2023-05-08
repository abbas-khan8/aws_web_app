package api.models;

import com.google.gson.Gson;

public class User {
    private int id;
    private String username;
    private String email;

    public User(int id, String name, String email) {
        this.id = id;
        this.username = name;
        this.email = email;
    }

    public User(String json) {
        Gson gson = new Gson();
        User tmp = gson.fromJson(json, User.class);

        this.id = tmp.id;
        this.username = tmp.username;
        this.email = tmp.email;
    }

    public User(int id, String json) {
        Gson gson = new Gson();
        User tmp = gson.fromJson(json, User.class);

        this.id = id;
        this.username = tmp.username;
        this.email = tmp.email;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return username;
    }

    public void setUserName(String name) {
        this.username = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
