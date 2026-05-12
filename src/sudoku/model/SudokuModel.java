package sudoku.model;

import java.util.Observer;

/**
 * Interface for the Sudoku Model.
 * All public methods used by Controller, View, and JUnit tests go through this interface.
 */
public interface SudokuModel {

    // ── Observer pattern ──────────────────────────────────────────────────────

    void addObserver(Observer o);

    // ── Board queries ─────────────────────────────────────────────────────────

    /**
     * Returns the digit (1-9) at (row, col), or 0 if empty.
     * @requires 0 <= row < 9 && 0 <= col < 9
     */
    int getCell(int row, int col);

    /**
     * Returns true iff the cell at (row, col) was pre-filled (given).
     * @requires 0 <= row < 9 && 0 <= col < 9
     */
    boolean isGiven(int row, int col);

    /**
     * Returns true iff the cell at (row, col) currently contains a duplicate
     * violation (only meaningful when validation feedback is enabled).
     * @requires 0 <= row < 9 && 0 <= col < 9
     */
    boolean isInvalid(int row, int col);

    // ── Board mutations ───────────────────────────────────────────────────────

    /**
     * Places digit in the editable cell (row, col).
     * Has no effect if the cell is given, or if digit is outside 1-9.
     * @requires 0 <= row < 9 && 0 <= col < 9 && 1 <= digit <= 9
     * @ensures getCell(row,col) == digit  (unless cell is given)
     */
    void setCell(int row, int col, int digit);

    /**
     * Clears an editable cell, setting it to 0.
     * Has no effect if the cell is given.
     * @requires 0 <= row < 9 && 0 <= col < 9
     * @ensures getCell(row,col) == 0  (unless cell is given)
     */
    void eraseCell(int row, int col);

    /**
     * Reverts the most recent user action (single-level undo).
     * Has no effect if there is nothing to undo.
     */
    void undo();

    /**
     * Reveals a correct value for one empty editable cell (hint).
     * Has no effect if hints are disabled or no empty cells remain.
     */
    void hint();

    /**
     * Resets all editable cells to 0, restoring the initial puzzle state.
     * @ensures forall (r,c): !isGiven(r,c) => getCell(r,c) == 0
     */
    void reset();

    /**
     * Loads the next puzzle from puzzles.txt according to the Puzzle Selection Flag.
     * @ensures !isComplete()
     */
    void newGame();

    // ── State queries ─────────────────────────────────────────────────────────

    /**
     * Returns true iff all cells are correctly filled (no duplicates anywhere).
     */
    boolean isComplete();

    /**
     * Validates the full board and returns true iff no duplicates exist.
     */
    boolean isValid();

    // ── Flags ─────────────────────────────────────────────────────────────────

    boolean isValidationFeedbackEnabled();
    void setValidationFeedbackEnabled(boolean enabled);

    boolean isHintEnabled();
    void setHintEnabled(boolean enabled);

    boolean isRandomPuzzleSelection();
    void setRandomPuzzleSelection(boolean random);

    // ── Test-support: set board into a desired state ──────────────────────────

    /**
     * Loads a puzzle directly from an 81-character string (digits and zeros).
     * Intended for unit tests only.
     * @requires s.length() == 81 && s matches [0-9]{81}
     */
    void loadPuzzleFromString(String s);
}
