import javax.sql.rowset.serial.SerialRef;
import java.util.Arrays;

public class HMMModel {
  public double[] pi;
  public double[][] a, b;
  public int n, m;
  public HMMModel(double[][] a, double[][] b, double[] pi) {
    this.pi = pi;
    this.a = a;
    this.b = b;
    n = a.length;
    m = b[0].length;
  }

  public class Alpha {
    public double[][] m;
    public double[] cs;

    public Alpha(double[][] m, double[] cs){
      this.m = m;
      this.cs = cs;
    }
  }

  public double[] alphaPass(int[] obsSeq) {
    return getAlpha(obsSeq).cs;
  }

  public Alpha getAlpha(int[] obsSeq) {
    int maxT = obsSeq.length;
    double[] cs = new double[maxT];
    double[][] alpha = new double[maxT][n];

    // compute alpha[0]
    double[] firstObsProbs = MatrixUtils.getCol(b, obsSeq[0]);
    alpha[0] = MatrixUtils.vectorMult(pi, firstObsProbs);

    // scale the alpha[0]
    for (double a : alpha[0])
      cs[0] += a;
    cs[0] = 1/cs[0];
    for (int i = 0; i < n; i++)
      alpha[0][i] *= cs[0];

    // compute alpha[0..T-1]
    for (int t = 1; t < maxT; t++) {
      for (int i = 0; i < n; i++) {
        alpha[t][i] = 0;
        for (int j = 0; j < n; j++)
          alpha[t][i] += alpha[t-1][j]*a[j][i];
        alpha[t][i] *= b[i][obsSeq[t]];
        cs[t] += alpha[t][i];
      }

      // scale alpha[t]
      cs[t] = 1/cs[t];
      for (int i = 0; i < n; i++)
        alpha[t][i] *= cs[t];
    }

    return new Alpha(alpha, cs);
  }

  public double logProbForObsSeq(int[] obsSeq) {
    double[] cs = alphaPass(obsSeq);
    double logProb = 0;
    for (double c : cs)
      logProb += Math.log(c);
    return -logProb;
  }

  public double[] obsProbsNextStep(double[][] alpha) {
    double[] obsList = new double[Constants.COUNT_MOVE];
    for (int o = 0; o < Constants.COUNT_MOVE; o++) {
      double probForObs = 0;
      for(int i = 0; i < alpha[alpha.length-1].length; i++) {
        probForObs += obsProbForAlphaIGivenObs(i, o, alpha);
      }
      obsList[o] = probForObs;
    }
    return obsList;
  }

  private double obsProbForAlphaIGivenObs(int i, int obs, double[][] alpha) {
    double[] lastAlphaRow = alpha[alpha.length-1];
    double obsProb = 0;
    //iterate through last alpha
    for(int j = 0; j < lastAlphaRow.length; j++){
      obsProb += lastAlphaRow[j] * a[j][i] * b[i][obs];
    }
    return obsProb;
  }
}