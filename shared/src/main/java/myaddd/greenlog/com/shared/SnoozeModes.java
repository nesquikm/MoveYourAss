package myaddd.greenlog.com.shared;

public class SnoozeModes {
    public static State.SuspendState getSuspendState(int id) {
        switch (id) {
            case 0:
                return State.SuspendState.Timeout;
            case 1:
                return State.SuspendState.Timeout;
            case 2:
                return State.SuspendState.Timeout;
            default:
            case 3:
                return State.SuspendState.NextOverstay;
            case 4:
                return State.SuspendState.UntilTomorrow;
            case 5:
                return State.SuspendState.Forever;
        }
    }

    public static long getSuspendTimeoutSeconds(int id) {
        switch (id) {
            case 0:
                return 5 * 60;
            case 1:
                return 15 * 60;
            case 2:
                return 30 * 60;
            default:
                return 0;
        }
    }
}
