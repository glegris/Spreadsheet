package spreadsheet;

import java.util.HashSet;
import java.util.Set;

import spreadsheet.api.CellLocation;
import spreadsheet.api.ExpressionUtils;
import spreadsheet.api.observer.Observer;
import spreadsheet.api.value.InvalidValue;
import spreadsheet.api.value.Value;

public class Cell implements Observer<Cell> {

    private final Spreadsheet sheet;
    private final CellLocation loc;

    private Value val;
    private String expr;

    // What do I depend on?
    private Set<Cell> thisReferences = new HashSet<Cell>();

    // What depends on me?
    private Set<Observer<Cell>> referencesMe = new HashSet<Observer<Cell>>();

    public Cell(Spreadsheet sheet, CellLocation loc) {
        this.sheet = sheet;
        this.loc = loc;
        this.setVal(null);
        this.expr = "";
    }

    public final Value getVal() {
        return val;
    }

    public final void setVal(Value val) {
        this.val = val;
    }

    public final String getExpr() {
        return expr;
    }

    public final void setExpr(String newExpr) {
        for (Cell c : thisReferences) {
            c.referencesMe.remove(this);
        }
        thisReferences.clear();

        System.out.println("Change " + expr + " to " + newExpr + " at "
                + getLoc());

        this.expr = newExpr;
        setVal(new InvalidValue(newExpr));
        addToInvalid();

        Set<CellLocation> locs =
                ExpressionUtils.getReferencedLocations(newExpr);

        for (CellLocation l : locs) {
            sheet.setExpression(l, sheet.getExpression(l));

            Cell c = sheet.getCellAt(l);
            thisReferences.add(c);
            c.referencesMe.add(this);

        }

        for (Observer<Cell> c : referencesMe) {
            c.update(this);
        }
    }

    public final boolean isInInvalidSet() {
        return sheet.getInvalid().contains(this);
    }

    public final void addToInvalid() {
        sheet.getInvalid().add(this);
    }

    @Override
    public final void update(Cell changed) {
        if (!isInInvalidSet()) {
            changed.addToInvalid();
            changed.setVal(new InvalidValue(changed.getExpr()));

            for (Observer<Cell> obs : referencesMe) {
                obs.update(changed);
            }
        }
    }

    public final Set<Cell> getReferences() {
        return thisReferences;
    }

    public final CellLocation getLoc() {
        return loc;
    }

    public final String toString() {
        return "(" + loc + " -> " + val.toString() + ")";
    }

}
