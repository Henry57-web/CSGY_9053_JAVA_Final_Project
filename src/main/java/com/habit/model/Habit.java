package com.habit.model;

/**
 * Model class representing a Habit entity.
 */
public class Habit {
    private int id;
    private String name;
    private String frequency;

    public Habit(int id, String name, String frequency) {
        this.id = id;
        this.name = name;
        this.frequency = frequency;
    }

    public int getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; } // Added Setter

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; } // Added Setter

    @Override
    public String toString() {
        return name + " (" + frequency + ")";
    }
}