import java.util.ArrayList;
import java.util.Arrays;

class Package {
  int[] obsSeq;
  int maxT;
  double[][] alpha, beta;
  double[][] gamma;
  double[][][] digamma;
  double[] cs;

  Package(int[] obsSeq, int n) {
    this.maxT = obsSeq.length;
    this.obsSeq = obsSeq;
    this.alpha = new double[maxT][n];
    this.beta = new double[maxT][n];
    this.digamma = new double[maxT-1][n][n];
    this.gamma = new double[maxT][n];
    this.cs = new double[maxT];
  }
}

public class BaumWelch {
  public HMMModel model;
  private int n, m;
  private Package[] packs;

  public BaumWelch(HMMModel initialModel, ArrayList<int[]> obsSeqs) {
    this.model = initialModel;
    this.n = initialModel.n;
    this.m = initialModel.m;
    packs = new Package[obsSeqs.size()];
    for (int i = 0; i < obsSeqs.size(); i++)
      packs[i] = new Package(obsSeqs.get(i), n);
  }

  private void alphaPass(Package pack) {
    int[] obsSeq = pack.obsSeq;
    double[][] alpha = pack.alpha;
    double[] cs = pack.cs;
    int maxT = pack.maxT;

    // compute alpha[0]
    double[] firstObsProbs = MatrixUtils.getCol(model.b, obsSeq[0]);
    alpha[0] = MatrixUtils.vectorMult(model.pi, firstObsProbs);

    // empty prev scaling values
    for (int i = 0; i < cs.length; i++)
      cs[i] = 0;

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
          alpha[t][i] += alpha[t-1][j]*model.a[j][i];
        alpha[t][i] *= model.b[i][obsSeq[t]];
        cs[t] += alpha[t][i];
      }

      // scale alpha[t]
      cs[t] = 1/cs[t];
      for (int i = 0; i < n; i++)
        alpha[t][i] *= cs[t];
    }
  }

  private void betaPass(Package pack) {
    int[] obsSeq = pack.obsSeq;
    double[][] beta = pack.beta;
    double[] cs = pack.cs;
    int maxT = pack.maxT;


    // initialize beta[T-1]
    for (int i = 0; i < n; i++)
      beta[maxT-1][i] = cs[maxT-1];

    // compute beta[0..T-2]
    for (int t = maxT-2; t >= 0; t--) {
      for (int i = 0; i < n; i++) {
        beta[t][i] = 0;
        for (int j = 0; j < n; j++)
          beta[t][i] += beta[t+1][j]*model.a[i][j]*model.b[j][obsSeq[t+1]];
        // scale beta[t][i]
        beta[t][i] *= cs[t];
      }
    }
  }

  private void gammaDigamma(Package pack) {
    int[] obsSeq = pack.obsSeq;
    double[][] alpha = pack.alpha;
    double[][] beta = pack.beta;
    double[][][] digamma = pack.digamma;
    double[][] gamma = pack.gamma;
    int maxT = pack.maxT;

    for (int t = 0; t < maxT-1; t++) {
      double denom = 0;
      for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
          denom += alpha[t][i]*model.a[i][j]*model.b[j][obsSeq[t+1]]*beta[t+1][j];
      for (int i = 0; i < n; i++) {
        gamma[t][i] = 0;
        for (int j = 0; j < n; j++) {
          digamma[t][i][j] = (alpha[t][i]*model.a[i][j]*model.b[j][obsSeq[t+1]]*beta[t+1][j])/denom;
          gamma[t][i] += digamma[t][i][j];
        }
      }
    }
    // special case for gamma[T-1]
    double denom = 0;
    for (int i = 0; i < n; i++)
      denom += alpha[maxT-1][i];
    for (int i = 0; i < n; i++) {
      gamma[maxT - 1][i] = alpha[maxT - 1][i] / denom;
    }
  }

  private void reestimatePi() {
    Arrays.fill(model.pi, 0);

    for (Package p : packs) {
      double[] fstRow = p.gamma[0];
      for (int i = 0; i < model.pi.length; i++)
        for (double v : fstRow)
          model.pi[i] += v;
    }
    for (int i = 0; i < model.pi.length; i++)
      model.pi[i] /= packs.length;
  }

  private void reestimateA() {
    for (int i = 0; i < n; i++)
      for (int j = 0; j < n; j++) {
        double numer = 0;
        double denom = 0;
        for (Package p : packs) {
          for (int t = 0; t < p.maxT - 1; t++) {
            numer += p.digamma[t][i][j];
            denom += p.gamma[t][i];
          }
        }
        model.a[i][j] = numer / denom;
      }

    for (int i = 0; i < n; i++) {
      removeZeroes(model.a[i]);
    }
  }

  private void reestimateB() {
    for (int i = 0; i < n; i++)
      for (int j = 0; j < m; j++) {
        double numer = 0;
        double denom = 0;
        for (Package p : packs) {
          for (int t = 0; t < p.maxT; t++) {
            denom += p.gamma[t][i];
            if (p.obsSeq[t] == j) {
              numer += p.gamma[t][i];
            }
          }
        }
        model.b[i][j] = numer / denom;
      }

    for (int i = 0; i < n; i++) {
      removeZeroes(model.b[i]);
    }
  }

  private void removeZeroes(double[] row) {
    double sum = 0;
    for (int i = 0; i < row.length; i++) {
      row[i] += 0.00000000000000000000001;
      sum += row[i];
    }
    for (int i = 0; i < row.length; i++) {
      row[i] /= sum;
    }
  }

  private void reestimateModel() {
    for (Package p : packs)
      gammaDigamma(p);

    reestimatePi();
    reestimateA();
    reestimateB();
  }

  private double computeLogProb() {
    double logProb = 0;
    for (Package p : packs)
      for (double c : p.cs)
        logProb += Math.log(c);
    return -logProb;
  }

  public void run() {
    int maxIters = 30;
    int iters = 0;
    double oldLogProb = -1000000;
    double logProb = -999999;
    while (iters < maxIters && oldLogProb < logProb) {
      oldLogProb = logProb;
      for (Package p : packs) {
        alphaPass(p);
        betaPass(p);
      }
      reestimateModel();
      logProb = computeLogProb();
      iters++;
    }
  }
}