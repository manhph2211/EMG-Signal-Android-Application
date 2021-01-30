package emgsignal.v3.SignalProcessing;

import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.List;

public class Signal {
    public static double[] kaiser(int N, double b) {
        KaiserWindow kaiser = new KaiserWindow(N, b);
        double[] w = new double[N];
        for (int i = 0; i < N; ++i) {
            w[i] = kaiser.value(i);
        }
        return w;
    }

    public static double[] periodogram(double[] x, double[] w, double Fs) {
        double[] Sxx = computeperiodogram(x, w);
        return computepsd(Sxx, Fs);
    }

    private static double[] computeperiodogram(double[] x, double[] w) {
        double[] xw = new double[x.length];
        for (int i = 0; i < x.length; ++i) {
            xw[i] = x[i] * w[i];
        }
        Complex[] Xx = FFT.transform(xw);
        double U = 0;
        for (double v : w) {
            U += v * v;
        }
        double[] Pxx = new double[x.length];
        for (int i = 0; i < x.length; ++i) {
            Pxx[i] = Xx[i].multiply(Xx[i].conjugate()).divide(U).getReal();
        }
        return Pxx;
    }

    private static double[] computepsd(double[] Sxx1, double Fs) {
        int select;
        int nfft = Sxx1.length;
        double[] Sxx;
        if (Sxx1.length % 2 != 0) {
            select = (nfft + 1) / 2;
            Sxx = new double[select];
            if (select >= 0) System.arraycopy(Sxx1, 0, Sxx, 0, select);
            for (int i = 0; i < select; ++i) {
                Sxx[i] = 2 * Sxx[i];
            }
        } else {
            select = nfft / 2 + 1;
            Sxx = new double[select];
            System.arraycopy(Sxx1, 0, Sxx, 0, select);
            for (int i = 1; i < select - 1; ++i) {
                Sxx[i] = 2 * Sxx[i];
            }
        }

        for (int i = 0; i < select; ++i) {
            Sxx[i] /= Fs;
        }
        return Sxx;
    }

    private static double enbw(double[] w, double Fs) {
        double bw_t = 0;
        for (double v : w) {
            bw_t += v * v;
        }
        bw_t /= w.length;
        bw_t /= Math.pow(Progressing.mean(w), 2);
        return bw_t * Fs / w.length;
    }

    public static double timeSNR(double[] x, double Fs) {
        double aver = Progressing.mean(x);
        double[] xTmp = new double[x.length];
        for (int i = 0; i < x.length; ++i) {
            xTmp[i] = x[i] - aver;
        }
        int n = x.length;
        double[] w = kaiser(n, 38);
        double rbw = enbw(w, Fs);
        double[] Pxx = periodogram(xTmp, w, Fs);

        double[] f = Progressing.getFrequency(x.length, Fs);
        double[] F = new double[Pxx.length];
        System.arraycopy(f, 0, F, 0, F.length);
        return computeSNR(Pxx, F, rbw);
    }

    private static double computeSNR(double[] Pxx, double[] F, double rbw) {
        double[] origPxx = new double[Pxx.length];
        System.arraycopy(Pxx, 0, origPxx, 0, Pxx.length);
        Pxx[0] *= 2;
        int[] idx = new int[4];
        double[] fund;
        fund = getPowerFreqToneFromPSD(Pxx, F, rbw, 0, idx);
        for (int i = idx[1] - 1; i <= idx[2] - 1; ++i) {
            Pxx[i] = 0;
        }

        fund = getPowerFreqToneFromPSD(Pxx, F, rbw, idx);
        double Pfund = fund[0];
        double Ffund = fund[1];
        for (int i = idx[1] - 1; i <= idx[2] - 1; ++i) {
            Pxx[i] = 0;
        }

        for (int i = 2; i <= 6; ++i) {
            double toneFreq = i * Ffund;
            fund = getPowerFreqToneFromPSD(Pxx, F, rbw, toneFreq, idx);
            if (!Double.isNaN(fund[0])) {
                for (int j = idx[1] - 1; j <= idx[2] - 1; ++j) {
                    Pxx[j] = 0;
                }
            }
        }

        List<Double> tmp = new ArrayList<>();
        for (double pxx : Pxx) {
            if (pxx > 0) {
                tmp.add(pxx);
            }
        }
        double[] d = new double[tmp.size()];
        for (int i = 0; i < d.length; ++i) {
            d[i] = tmp.get(i);
        }

        double estimatedNoiseDensity = Progressing.median(d);

        for (int i = 0; i < Pxx.length; i++) {
            if (Pxx[i] == 0) {
                Pxx[i] = estimatedNoiseDensity;
            }
        }

        for (int i = 0; i < Pxx.length; i++) {
            Pxx[i] = Progressing.min(Pxx[i], origPxx[i]);
        }

        double totalNoise = bandpower(Pxx, F);
        return 10 * Math.log10(Pfund / totalNoise);
    }

    private static void getToneFromPSD(double[] Pxx, double[] F, double rbw, double toneFreq, int[] res) {
        double[] absF = new double[F.length];
        for (int i = 0; i < F.length; ++i) {
            absF[i] = Math.abs(F[i] - toneFreq);
        }
        res[0] = Progressing.indexOfMin(absF) + 1;
        int iLeftBin = Math.max(1, res[0] - 1);
        int iRightBin = Math.min(res[0] + 1, Pxx.length);
        int idxMax = Progressing.indexOfMax(Pxx, iLeftBin - 1, iRightBin - 1) - iLeftBin + 2;
        res[0] = iLeftBin + idxMax - 1;

        int idxToneScalar = res[0];
        res[1] = idxToneScalar - 1;
        res[2] = idxToneScalar + 1;

        while (true) {
            if (res[1] <= 0) break;
            if (Pxx[res[1] - 1] > Pxx[res[1]]) break;
            res[1] -= 1;
        }

        while (true) {
            if (res[2] > Pxx.length) break;
            if (Pxx[res[2] - 2] < Pxx[res[2] - 1]) break;
            res[2] += 1;
        }

        res[1] += 1;
        res[2] -= 1;
        res[3] = idxToneScalar;
    }

    private static void getToneFromPSD(double[] Pxx, double[] F, double rbw, int[] res) {
        res[0] = Progressing.indexOfMax(Pxx) + 1;

        int idxToneScalar = res[0];
        res[1] = idxToneScalar - 1;
        res[2] = idxToneScalar + 1;

        while (true) {
            if (res[1] <= 0) break;
            if (Pxx[res[1] - 1] > Pxx[res[1]]) break;
            res[1] -= 1;
        }

        while (true) {
            if (res[2] > Pxx.length) break;
            if (Pxx[res[2] - 2] < Pxx[res[2] - 1]) break;
            res[2] += 1;
        }

        res[1] += 1;
        res[2] -= 1;
        res[3] = idxToneScalar;
    }

    private static double[] getPowerFreqToneFromPSD(double[] Pxx, double[] F, double rbw, double toneFreq, int[] idx) {
        if (F[0] <= toneFreq && toneFreq <= F[F.length - 1]) {
            getToneFromPSD(Pxx, F, rbw, toneFreq, idx);
            int idxLeftScalar = idx[1];
            int idxRightScalar = idx[2];
            double power = 0;
            double[] Ffund = new double[idxRightScalar - idxLeftScalar + 1];
            double[] Sfund = new double[idxRightScalar - idxLeftScalar + 1];
            if (idxRightScalar - (idxLeftScalar - 1) >= 0)
                System.arraycopy(F, idxLeftScalar - 1, Ffund, 0, idxRightScalar - (idxLeftScalar - 1));

            if (idxRightScalar - (idxLeftScalar - 1) >= 0)
                System.arraycopy(Pxx, idxLeftScalar - 1, Sfund, 0, idxRightScalar - (idxLeftScalar - 1));
            double freq = 0;
            for (int i = 0; i < Ffund.length; ++i) {
                freq += Ffund[i] * Sfund[i];
            }
            freq /= Progressing.sum(Sfund);
            if (idxLeftScalar < idxRightScalar) {
                power = bandpower(Sfund, Ffund);
            } else if (1 < idxRightScalar && idxRightScalar < Pxx.length) {
                power = Pxx[idxRightScalar - 1] * (F[idxRightScalar] - F[idxRightScalar - 2]) / 2;
            } else {
                double[] diff = new double[F.length - 1];
                for (int i = 1; i < F.length; ++i) {
                    diff[i - 1] = F[i] - F[i - 1];
                }
                power = Pxx[idxRightScalar - 1] * Progressing.mean(diff);
            }
            if (power < rbw * Pxx[idx[3] - 1]) {
                power = rbw * Pxx[idx[3] - 1];
                freq = F[idx[3] - 1];
            }
            return new double[]{power, freq};
        }
        idx[0] = 0;
        idx[1] = 0;
        idx[2] = 0;
        idx[3] = 0;
        return new double[]{Double.NaN, Double.NaN};
    }

    private static double[] getPowerFreqToneFromPSD(double[] Pxx, double[] F, double rbw, int[] idx) {
        getToneFromPSD(Pxx, F, rbw, idx);
        int idxLeftScalar = idx[1];
        int idxRightScalar = idx[2];
        double power = 0;
        double[] Ffund = new double[idxRightScalar - idxLeftScalar + 1];
        double[] Sfund = new double[idxRightScalar - idxLeftScalar + 1];
        if (idxRightScalar - (idxLeftScalar - 1) >= 0)
            System.arraycopy(F, idxLeftScalar - 1, Ffund, 0, idxRightScalar - (idxLeftScalar - 1));
        if (idxRightScalar - (idxLeftScalar - 1) >= 0)
            System.arraycopy(Pxx, idxLeftScalar - 1, Sfund, 0, idxRightScalar - (idxLeftScalar - 1));

        double freq = 0;
        for (int i = 0; i < Ffund.length; ++i) {
            freq += Ffund[i] * Sfund[i];
        }
        freq /= Progressing.sum(Sfund);
        if (idxLeftScalar < idxRightScalar) {
            power = bandpower(Sfund, Ffund);
        } else if (1 < idxRightScalar && idxRightScalar < Pxx.length) {
            power = Pxx[idxRightScalar - 1] * (F[idxRightScalar] - F[idxRightScalar - 2]) / 2;
        } else {
            double[] diff = new double[F.length - 1];
            for (int i = 1; i < F.length; ++i) {
                diff[i - 1] = F[i] - F[i - 1];
            }
            power = Pxx[idxRightScalar - 1] * Progressing.mean(diff);
        }
        if (power < rbw * Pxx[idx[3] - 1]) {
            power = rbw * Pxx[idx[3] - 1];
            freq = F[idx[3] - 1];
        }
        return new double[]{power, freq};
    }

    private static double bandpower(double[] Pxx, double[] F) {
        double[] freqrange = new double[]{F[0], F[F.length - 1]};

        int idx1 = 0, idx2 = 0;
        for (int i = 0; i < F.length; ++i) {
            if (F[i] <= freqrange[0]) idx1 = i;
        }
        for (int i = 0; i < F.length; ++i) {
            if (F[i] >= freqrange[1]) {
                idx2 = i;
                break;
            }
        }
        double[] width = new double[F.length];
        System.arraycopy(F, 0, width, 0, F.length);
        double[] W_diff = new double[F.length - 1];
        for (int i = 1; i < F.length; ++i) {
            W_diff[i - 1] = F[i] - F[i - 1];
        }
        double missingWidth = (F[F.length - 1] - F[0]) / (F.length - 1);
        boolean centerDC = F[0] != 0;
        if (centerDC) {
            width[0] = missingWidth;
            System.arraycopy(W_diff, 0, width, 1, width.length - 1);
        } else {
            System.arraycopy(W_diff, 0, width, 0, width.length - 1);
            width[width.length - 1] = missingWidth;
        }
        double pwr = 0;
        for (int i = idx1; i <= idx2; ++i) {
            pwr += width[i] * Pxx[i];
        }
        return pwr;
    }
}
