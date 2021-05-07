import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 */
public class Solution implements MonotonicClock {
    private final RegularInt c1 = new RegularInt(0);
    private final RegularInt c2 = new RegularInt(0);
    private final RegularInt c3 = new RegularInt(0);

    private final RegularInt c4 = new RegularInt(0);
    private final RegularInt c5 = new RegularInt(0);
    private final RegularInt c6 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {

        c4.setValue(time.getD1());
        c5.setValue(time.getD2());
        c6.setValue(time.getD3());

        c3.setValue(time.getD3());
        c2.setValue(time.getD2());
        c1.setValue(time.getD1());
    }

    @NotNull
    @Override
    public Time read() {
        RegularInt d1 = new RegularInt(c1.getValue());
        RegularInt d2 = new RegularInt(c2.getValue());
        RegularInt d3 = new RegularInt(c3.getValue());

        RegularInt d6 = new RegularInt(c6.getValue());
        RegularInt d5 = new RegularInt(c5.getValue());
        RegularInt d4 = new RegularInt(c4.getValue());

        Time t1 = new Time(d1.getValue(), d2.getValue(), d3.getValue());
        Time t2 = new Time(d4.getValue(), d5.getValue(), d6.getValue());

        if (t1.compareTo(t2) == 0) {
            return t1;
        } else {
            if (d1.getValue() == d4.getValue()) {
                if (d2.getValue() == d5.getValue()) {
                    return new Time(d4.getValue(), d5.getValue(), d6.getValue());
                }
                return new Time(d4.getValue(), d5.getValue(), 0);
            }
            return new Time(d4.getValue(), 0, 0);
        }
    }
}
