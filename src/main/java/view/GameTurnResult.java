package view;

import com.googlecode.lanterna.gui2.Label;

public class GameTurnResult {
    public final String selectedTitle;
    public final Label errorLabel;

    public GameTurnResult(String selectedTitle, Label errorLabel) {
        this.selectedTitle = selectedTitle;
        this.errorLabel = errorLabel;
    }
}
