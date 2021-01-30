package emgsignal.v3.SignalProcessing;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FFT {
    public static Complex[] transform(double[] x) {
        return transform(MyComplex.toComplexArray(x));
    }

    public static Complex[] transform(Complex[] x) {
        int n = x.length;
        if ((n & (n - 1)) == 0)  // Is power of 2
            return transformRadix2(x);
        else  // More complicated algorithm for arbitrary sizes
            return transformBluestein(x);
    }

    public static double[] absFFT(Complex[] fft) {
        double[] absFFT = new double[fft.length];
        for (int i = 0; i < fft.length; ++i) {
            absFFT[i] = fft[i].abs() / absFFT.length;
        }
        return absFFT;
    }

    public static double[] dbFFT(double[] absFFT) {
        double[] dbFFT = new double[absFFT.length];
        for (int i = 0; i < absFFT.length; ++i) {
            dbFFT[i] = 20 * Math.log10(absFFT[i]);
        }
        return dbFFT;
    }

    public static Complex[] inverseTransform(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];
        for (int i = 0; i < n; ++i) {
            y[i] = x[i].conjugate();
        }
        y = transform(y);
        for (int i = 0; i < n; ++i) {
            y[i] = y[i].conjugate();
            y[i] = y[i].divide(n);
        }
        return y;
    }

    private static Complex[] transformRadix2(Complex[] x) {
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        return transformer.transform(x, TransformType.FORWARD);
    }

    private static Complex[] transformBluestein(Complex[] x) {
        int n = x.length;
        if (n >= 0x20000000)
            throw new IllegalArgumentException("Array too large");
        int m = Integer.highestOneBit(n) * 4;

        // Trigonometric tables
        double[] cosTable = new double[n];
        double[] sinTable = new double[n];
        for (int i = 0; i < n; i++) {
            int j = (int) ((long) i * i % (n * 2));  // This is more accurate than j = i * i
            cosTable[i] = Math.cos(Math.PI * j / n);
            sinTable[i] = Math.sin(Math.PI * j / n);
        }

        // Temporary vectors and preprocessing
        double[] areal = new double[m];
        double[] aimag = new double[m];
        for (int i = 0; i < n; i++) {
            areal[i] = x[i].getReal() * cosTable[i] + x[i].getImaginary() * sinTable[i];
            aimag[i] = -x[i].getReal() * sinTable[i] + x[i].getImaginary() * cosTable[i];
        }
        double[] breal = new double[m];
        double[] bimag = new double[m];
        breal[0] = cosTable[0];
        bimag[0] = sinTable[0];
        for (int i = 1; i < n; i++) {
            breal[i] = breal[m - i] = cosTable[i];
            bimag[i] = bimag[m - i] = sinTable[i];
        }

        Complex[] a = MyComplex.toComplexArray(areal, aimag);
        Complex[] b = MyComplex.toComplexArray(breal, bimag);
        Complex[] c = convolve(a, b);
        Complex[] y = new Complex[n];
        // Postprocessing
        for (int i = 0; i < n; i++) {
            y[i] = new Complex(c[i].getReal() * cosTable[i] + c[i].getImaginary() * sinTable[i],
                    -c[i].getReal() * sinTable[i] + c[i].getImaginary() * cosTable[i]);
        }
        return y;
    }


    public static Complex[] convolve(Complex[] x, Complex[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException("Mismatched lengths");
        int n = x.length;
        x = x.clone();
        y = y.clone();
        Complex[] a = transform(x);
        Complex[] b = transform(y);

        Complex[] c = new Complex[n];
        for (int i = 0; i < n; ++i) {
            c[i] = a[i].multiply(b[i]);
        }
        return inverseTransform(c);
    }
}