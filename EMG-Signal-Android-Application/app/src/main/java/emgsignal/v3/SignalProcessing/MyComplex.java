package emgsignal.v3.SignalProcessing;

import org.apache.commons.math3.complex.Complex;

public class MyComplex {

    public static double[] toRealArray(Complex[] x) {
        double[] real = new double[x.length];
        for (int i = 0; i < x.length; ++i) {
            real[i] = x[i].getReal();
        }
        return real;
    }

    public static double[] toImagArray(Complex[] x) {
        double[] imag = new double[x.length];
        for (int i = 0; i < x.length; ++i) {
            imag[i] = x[i].getImaginary();
        }
        return imag;
    }

    public static Complex[] toComplexArray(double[] real) {
        Complex[] comp = new Complex[real.length];
        for (int i = 0; i < real.length; ++i) {
            comp[i] = new Complex(real[i], 0);
        }
        return comp;
    }

    public static Complex[] toComplexArray(double[] real, double[] imag) {
        Complex[] comp = new Complex[real.length];
        for (int i = 0; i < real.length; ++i) {
            comp[i] = new Complex(real[i], imag[i]);
        }
        return comp;
    }
}
