package com.neuronation.api.model;

public class StatsEntry {
    private int statsType;
    private int statsId;
    private int statsComponent;
    private int valueInt;
    private String valueText;
    private String lastEdit;

    public int getStatsType() { return statsType; }
    public int getStatsId() { return statsId; }
    public int getStatsComponent() { return statsComponent; }
    public int getValueInt() { return valueInt; }
    public String getValueText() { return valueText; }
    public String getLastEdit() { return lastEdit; }
}
