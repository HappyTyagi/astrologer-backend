package com.astro.backend.RequestDTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KundliSvgRequest {
    @JsonProperty("year")
    private int year;

    @JsonProperty("month")
    private int month;

    @JsonProperty("date")
    private int date;

    @JsonProperty("hours")
    private int hours;

    @JsonProperty("minutes")
    private int minutes;

    @JsonProperty("seconds")
    private int seconds;

    @JsonProperty("latitude")
    private double latitude;

    @JsonProperty("longitude")
    private double longitude;

    @JsonProperty("timezone")
    private double timezone;

    @JsonProperty("ayanamsha")
    private String ayanamsha;

    @JsonProperty("observation_point")
    private String observationPoint;

    public KundliSvgRequest() {
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getTimezone() {
        return timezone;
    }

    public void setTimezone(double timezone) {
        this.timezone = timezone;
    }

    public String getAyanamsha() {
        return ayanamsha;
    }

    public void setAyanamsha(String ayanamsha) {
        this.ayanamsha = ayanamsha;
    }

    public String getObservationPoint() {
        return observationPoint;
    }

    public void setObservationPoint(String observationPoint) {
        this.observationPoint = observationPoint;
    }
}
