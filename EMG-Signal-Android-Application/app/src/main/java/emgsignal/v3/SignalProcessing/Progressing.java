package emgsignal.v3.SignalProcessing;

import android.support.annotation.NonNull;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Progressing {
    private static final Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static double max(double[] arr) {
        double max = arr[0];
        for (double it : arr) {
            if (it > max) max = it;
        }
        return max;
    }

    public static double max(double[] arr, int from, int to) {
        double max = arr[from];
        for (int i = from; i <= to; ++i) {
            if (arr[i] > max) max = arr[i];
        }
        return max;
    }

    public static double min(double[] arr) {
        double min = arr[0];
        for (double it : arr) {
            if (it < min) min = it;
        }
        return min;
    }

    public static double min(double[] arr, int from, int to) {
        double min = arr[from];
        for (int i = from; i <= to; ++i) {
            if (arr[i] < min) min = arr[i];
        }
        return min;
    }

    public static double min(double a, double b) {
        if (Double.isNaN(a)) {
            return b;
        } else if (Double.isNaN(b)) {
            return a;
        } else if (Double.isNaN(a) && Double.isNaN(b)) {
            return Double.NaN;
        }
        return Math.min(a, b);
    }

    public static int indexOfMax(double[] arr) {
        int pos = 0;
        double max = arr[0];
        for (int i = 0; i < arr.length; ++i) {
            if (arr[i] > max) {
                pos = i;
                max = arr[i];
            }
        }
        return pos;
    }

    public static int indexOfMax(double[] arr, int from, int to) {
        int pos = from;
        double max = arr[from];
        for (int i = from; i <= to; ++i) {
            if (arr[i] > max) {
                pos = i;
                max = arr[i];
            }
        }
        return pos;
    }

    public static int indexOfMin(double[] arr) {
        int pos = 0;
        double min = arr[0];
        for (int i = 0; i < arr.length; ++i) {
            if (arr[i] < min) {
                pos = i;
                min = arr[i];
            }
        }
        return pos;
    }

    public static int indexOfMin(double[] arr, int from, int to) {
        int pos = from;
        double min = arr[from];
        for (int i = from; i <= to; ++i) {
            if (arr[i] < min) {
                pos = i;
                min = arr[i];
            }
        }
        return pos;
    }

    public static double sum(double[] arr) {
        double result = 0;
        for (double it : arr) {
            result += it;
        }
        return result;
    }

    public static double mean(double[] arr) {
        return sum(arr) / arr.length;
    }

    private static double variance(double[] arr) {
        double result = 0;
        double aver = mean(arr);
        for (double it : arr) {
            result += (it - aver) * (it - aver);
        }
        return result / arr.length;
    }

    public static double power(double a) {
        return Math.pow(a, 2) / 2;
    }

    public static double totalPower(double[] arr) {
        double result = 0;
        for (int i = 0; i < arr.length / 2; ++i) {
            result += power(arr[i]);
        }
        return result;
    }

    public static double median(double[] arr) {
        if (arr.length == 0) {
            return Double.NaN;
        }
        double[] tmpArr = new double[arr.length];
        System.arraycopy(arr, 0, tmpArr, 0, arr.length);
        Arrays.sort(tmpArr);
        return tmpArr[arr.length / 2];
    }

    public static double[] getFrequency(int length, double Fs) {
        double[] frequency = new double[length];
        for (int i = 0; i < length; ++i) {
            frequency[i] = i * Fs / length;
        }
        return frequency;
    }

    public static double meanFrequency(double[] arr, double Fs) {
        double[] frequency = getFrequency(arr.length, Fs);
        double fm = 0;
        for (int i = 0; i < arr.length / 2; ++i) {
            fm = fm + power(arr[i]) * frequency[i];
        }
        return fm / totalPower(arr);
    }

    public static double medianFrequency(double[] arr, double Fs) {
        double[] frequency = getFrequency(arr.length, Fs);
        double checked = totalPower(arr) / 2;
        double total = 0;
        int pos = 0;
        for (int i = 0; i < arr.length / 2; ++i) {
            total += power(arr[i]);
            if (total >= checked) {
                pos = i;
                break;
            }
        }
        return frequency[pos];
    }

    public static double maxFrequency(double[] arr, double Fs) {
        double max = max(arr, 0, arr.length/2);
        int pos = 0;
        for (double v : arr) {
            if (v == max) {
                break;
            }
            ++pos;
        }
        return pos * Fs / arr.length;
    }

    public static double minFrequency(double[] arr, double Fs) {
        double min = min(arr, 0, arr.length /2);
        int pos = 0;
        for (double v : arr) {
            if (v == min) {
                break;
            }
            ++pos;
        }
        return pos * Fs / arr.length;
    }

    public static double snr(double[] x, double Fs) {
        return Signal.timeSNR(x, Fs);
    }

    public static void drawGraph(@NonNull GraphView graph, LineGraphSeries<DataPoint> graphSeries, double minY, double maxY, double maxX) {
        graph.removeAllSeries();
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);
        graph.getGridLabelRenderer().setNumVerticalLabels(5);
        graph.getViewport().setMaxX(maxX);
        graph.getViewport().setMaxY(maxY);
        graph.getViewport().setMinY(minY);
        graph.addSeries(graphSeries);
    }

    @NonNull
    public static double[] readFile(File file) {
        String line;
        ArrayList<Double> lines = new ArrayList<>();
        try {
            BufferedReader bf = new BufferedReader(new FileReader(file));
            while ((line = bf.readLine()) != null) {
                if (isNumeric(line)) {
                    lines.add(Double.parseDouble(line));
                }
            }
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        double[] out = new double[lines.size()];
        for (int i = 0; i < out.length; ++i) {
            out[i] = lines.get(i);
        }
        return out;
    }

    private static boolean isNumeric(String s) {
        if (s == null) {
            return false;
        }
        return pattern.matcher(s).matches();
    }
}
