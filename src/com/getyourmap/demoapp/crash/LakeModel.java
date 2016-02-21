package com.getyourmap.demoapp.crash;

import java.io.Serializable;

/**
 * Created by V.Tanakov on 02.07.2015.
 */
public class LakeModel implements Serializable {

    private int id;
    private String name;
    private String path;
    private double lat;
    private double lng;
    private boolean isFree;

    public LakeModel() {
    }

    public LakeModel(int id, String name, String path, double lat, double lng, boolean isFree) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.lat = lat;
        this.lng = lng;
        this.isFree = isFree;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setIsFree(boolean isFree) {
        this.isFree = isFree;
    }
}
