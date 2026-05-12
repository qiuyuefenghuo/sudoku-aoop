package sudoku.view;

/**
 * Interface that the Controller uses to talk back to the View
 * (e.g. enable/disable buttons).  Keeps Controller free of Swing imports.
 */
public interface SudokuView {
    /** Enable or disable the Hint button. */
    void setHintButtonEnabled(boolean enabled);
}
