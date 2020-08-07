package com.github.com.almasud.Augmented_School.model.entity;

import java.io.Serializable;

/**
 * An entity language model class for {@link Subject}.
 *
 * @author Abdullah Almasud
 */
public class Language implements Serializable {
    public static final int BENGALI = 0;
    public static final int ENGLISH = 1;
    private int id;
    private String name;

    public Language(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return An Id of {@link Language}.
     */
    public int getId() {
        return id;
    }

    /**
     * @return A {@link String} name of {@link Language}.
     */
    public String getName() {
        return name;
    }
}
