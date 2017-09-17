import java.util.Random;

public class InitializeModel {
  static double[] generateRandomRow(int n) {
    double[] returnRow = new double[n];

    double sum = 0;
    Random rand = new Random();

    for (int i = 0; i < n; i++) {
      returnRow[i] = rand.nextInt(100) + 950;
      sum += returnRow[i];
    }

    for (int i = 0; i < n; i++) {
      returnRow[i] /= sum;
    }

    return returnRow;
  }

  static double[][] generateRandomMatrix() {
    int rowCount = 10;
    int colCount = 10;
    double[][] matrix = new double[rowCount][colCount];
    for (int i = 0; i < rowCount; i++) {
      matrix[i] = generateRandomRow(colCount);
    }
    return matrix;
  }
//
//  public static void main(String[] args) {
//    double[] row = generateRandomRow(10);
//    double sum = 0;
//    MatrixUtils.prettyPrint(generateRandomMatrix());
////    for (int i = 0; i < 10; i++) {
////      System.out.println(row[i]);
////      sum += row[i];
////    }
////    System.out.println("sum: " + sum);
//  }
}
