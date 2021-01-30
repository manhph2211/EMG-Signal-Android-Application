package emgsignal.v3.SignalProcessing;


public class KaiserWindow {
    private final int L;
    private final double beta;

    public KaiserWindow(int L, double beta) {
        this.beta = beta;
        this.L = L;
    }

    public double value(int n) {
        if (n < 0 || n >= L) {
            return 0;
        }
        return BesselI.value(0, beta * Math.sqrt(1 - Math.pow(((n - (double) (L - 1) / 2) / ((double) (L - 1) / 2)), 2))) / BesselI.value(0, beta);
    }
}
