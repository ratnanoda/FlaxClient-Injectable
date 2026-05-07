package me.eldodebug.soar.management.remote.update;

import me.eldodebug.soar.Glide;

public class Update {

    String updateLink = "https://glideclient.github.io/";
    String updateVersionString = "something is broken lmao";
    int updateBuildID = 0;
    boolean discontinued = false;
    boolean soar8Released = false;

    public void setUpdateLink(String in){
        this.updateLink = in;
    }
    public String getUpdateLink(){
        return updateLink;
    }

    public void setVersionString(String in){
        this.updateVersionString = in;
    }
    public String getVersionString(){
        return updateVersionString;
    }

    public void setBuildID(int in){this.updateBuildID = in;}
    public int getBuildID(){
        return updateBuildID;
    }

    public void setDiscontinued(boolean in){
        this.discontinued = in;
    }
    public boolean getDiscontinued(){
        return discontinued;
    }

    public void setSoar8Released(boolean in){
        this.soar8Released = in;
    }
    public boolean getSoar8Released() {return soar8Released;}


    public void check(){
        Glide g = Glide.getInstance();
        g.setUpdateNeeded(false);
        g.setSoar8Released(false);
    }

    public void checkForUpdates(){
        Glide g = Glide.getInstance();
        g.setUpdateNeeded(false);
        g.setSoar8Released(false);
    }

}
