package sudoku.controller;

import sudoku.model.SudokuModel;
import sudoku.view.SudokuView;

/**
 * Controller for the GUI version of Sudoku.
 *
 * Responsibilities:
 *   - Validate requests before forwarding them to the Model
 *   - Query the Model to decide button enable/disable state
 *   - Never contains GUI code; it may call View methods to enable/disable widgets
 *
 * Does NOT implement Observer; the View observes the Model directly.
 */
public class SudokuController {

    private final SudokuModel model;
    private final SudokuView  view;

    public SudokuController(SudokuModel model, SudokuView view) {
        this.model = model;
        this.view  = view;
    }

    // ── Cell editing ──────────────────────────────────────────────────────────

    /**
     * Called when the user enters a digit in cell (row, col).
     * Forwards only if the cell is editable and the digit is 1-9.
     */
    public void onDigitEntered(int row, int col, int digit) {
        if (model.isGiven(row, col)) return;
        if (digit < 1 || digit > 9)  return;
        model.setCell(row, col, digit);
        updateButtonStates();
    }

    /**
     * Called when the user requests erase on the currently selected cell.
     */
    public void onErase(int row, int col) {
        if (model.isGiven(row, col)) return;
        model.eraseCell(row, col);
        updateButtonStates();
    }

    // ── Toolbar actions ───────────────────────────────────────────────────────

    /** Undo last move – only affects editable cells. */
    public void onUndo() {
        model.undo();
        updateButtonStates();
    }

    /** Reveal a hint for an empty cell. */
    public void onHint() {
        if (!model.isHintEnabled()) return;
        model.hint();
        updateButtonStates();
    }

    /** Reset the puzzle to its initial state. */
    public void onReset() {
        model.reset();
        updateButtonStates();
    }

    /** Load a new puzzle from puzzles.txt. */
    public void onNewGame() {
        model.newGame();
        updateButtonStates();
    }

    // ── Flag toggles (runtime, GUI only) ─────────────────────────────────────

    public void onToggleValidation(boolean enabled) {
        model.setValidationFeedbackEnabled(enabled);
    }

    public void onToggleHint(boolean enabled) {
        model.setHintEnabled(enabled);
        view.setHintButtonEnabled(enabled);
    }

    public void onToggleRandomSelection(boolean random) {
        model.setRandomPuzzleSelection(random);
    }

    // ── Button state management ───────────────────────────────────────────────

    /**
     * Queries the model and tells the view which buttons should be active.
     * Hint button is enabled only when hints are on and there are empty cells.
     */
    public void updateButtonStates() {
        boolean hasEmpty = hasEmptyEditableCell();
        view.setHintButtonEnabled(model.isHintEnabled() && hasEmpty);
    }

    private boolean hasEmptyEditableCell() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (!model.isGiven(r, c) && model.getCell(r, c) == 0) return true;
            }
        }
        return false;
    }
}
