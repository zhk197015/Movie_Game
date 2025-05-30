package model.game;

import lombok.Data;

/**
 * Victory Condition Model
 */
@Data
public class WinCondition {
    // Condition type (movie genre, actor, director, etc.)
    private String conditionType;

    // Conditional value (specific type, actor name, director name, etc.)
    private String conditionValue;

    // Target times (how many times are needed to achieve the condition)）
    private int targetCount;

    // Current progress
    private int currentCount;

    /**
     * Constructor
     */
    public WinCondition(String conditionType, String conditionValue, int targetCount) {
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.targetCount = targetCount;
        this.currentCount = 0;
    }

    /**
     * Constructor (default target count is 1)
     */
    public WinCondition(String conditionType, String conditionValue) {
        this(conditionType, conditionValue, 1);
    }

    /**
     * Increase current progress
     */
    public void incrementProgress() {
        this.currentCount++;
    }

    /**
     * Check if victory conditions have been met
     */
    public boolean isAchieved() {
        return this.currentCount >= this.targetCount;
    }
    
    public String getConditionValue() {
        return this.conditionValue;
    }

}
