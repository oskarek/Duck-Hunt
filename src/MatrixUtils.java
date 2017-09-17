public class MatrixUtils {
  public static double[] getCol(double[][] m, int n) {
    double[] col = new double[m.length];
    for (int i = 0; i < m.length; i++) {
      col[i] = m[i][n];
    }
    return col;
  }

  public static double[] vectorMult(double[] v1, double[] v2) {
    assert v1.length == v2.length;

    double[] resVec = new double[v1.length];
    for (int i = 0; i < v1.length; i++) {
      resVec[i] = v1[i]*v2[i];
    }
    return resVec;
  }

  public static double distance(double[][] mat1, double[][] mat2) {
    double dist = 0;
    for(int i = 0; i<mat1.length;i++) {
      for(int j = 0;j<mat1[i].length;j++) {
        dist += Math.abs(mat1[i][j] - mat2[i][j]);
      }
    }
    return dist/(mat1.length*mat1[0].length);
  }

  public static void prettyPrint(double[][] mat) {
    for (double[] aMat : mat) {
      System.err.println();
      for (int j = 0; j < mat[0].length; j++) {
        System.err.printf("%.6f ", aMat[j]);
      }
    }
    System.err.println();
  }
}
