import java.util.ArrayList;
import java.util.HashMap;

class BirdModel {
    ArrayList<int[]> savedSequences;
    HMMModel model;
    int birdType;

    BirdModel(int birdType) {
        this.savedSequences = new ArrayList<>();
        this.birdType = birdType;
        initModel();
    }

    private void initModel() {
        model = new HMMModel(InitializeModel.generateRandomMatrix(), InitializeModel.generateRandomMatrix(),
            InitializeModel.generateRandomRow(10));
    }

    void addSequence(int[] sequence) {
        this.savedSequences.add(sequence);
    }

    void emptySavedSequences() {
        this.savedSequences.clear();
    }
}

class Player {
    HashMap<Integer, BirdModel> birdModelGuesses = new HashMap<>();
    // bird, round, obs seq
    int t = 0;
    int timePeriod = 100;

    public Player() {
    }

    /**
     * Shoot!
     *
     * This is the function where you start your work.
     *
     * You will receive a variable pState, which contains information about all
     * birds, both dead and alive. Each bird contains all past moves.
     *
     * The state also contains the scores for all players and the number of
     * time steps elapsed since the last time this function was called.
     *
     * @param pState the GameState object with observations etc
     * @param pDue time before which we must have returned
     * @return the prediction of a bird we want to shoot at, or cDontShoot to pass
     */
    public Action shoot(GameState pState, Deadline pDue) {

//        if (t < 30) {
//            t++;
//            return cDontShoot;
//        }
//        if (true) {
//            HMMModel[] models = new HMMModel[pState.getNumBirds()];
//            for (int b = 0; b < pState.getNumBirds(); b++) {
//                Bird bird = pState.getBird(b);
//                int[] obsSeq = new int[bird.getSeqLength()];
//                for (int i = 0; i < bird.getSeqLength(); i++) {
//                    birdSeqs[b][i*pState.getRound()] = bird.getObservation(i);
//                }
//                HMMModel model = new HMMModel(InitializeModel.generateRandomMatrix(), InitializeModel.generateRandomMatrix(),
//                    InitializeModel.generateRandomRow(10));
//                BaumWelch bw = new BaumWelch(model, obsSeq, 10, 10);
//                bw.run();
//                models[b] = bw.model;
//            }
//
//            for (int m = 0 ; m<models.length; m++) {
//                for (int o = 0; o<models.length; o++) {
//                    double dist = MatrixUtils.distance(models[m].b, models[o].b);
//                    System.err.println("Distance between bird " + m + " and " + o + " : " + dist);
//                }
//            }
//        }
        /*
         * Here you should write your clever algorithms to get the best action.
         * This skeleton never shoots.
         */
        t++;
        return cDontShoot;
        // This line chooses not to shoot.
        // return cDontShoot;

        // This line would predict that bird 0 will move right and shoot at it.
        // return Action(0, MOVE_RIGHT);
    }

    /**
     * Guess the species!
     * This function will be called at the end of each round, to give you
     * a chance to identify the species of the birds for extra points.
     *
     * Fill the vector with guesses for the all birds.
     * Use SPECIES_UNKNOWN to avoid guessing.
     *
     * @param pState the GameState object with observations etc
     * @param pDue time before which we must have returned
     * @return a vector with guesses for all the birds
     */
    public int[] guess(GameState pState, Deadline pDue) {
        /*
         * Here you should write your clever algorithms to guess the species of
         * each bird. This skeleton makes no guesses, better safe than sorry!
         */
        int[] lGuess = new int[pState.getNumBirds()];
        for (int i = 0; i < pState.getNumBirds(); i++) {
            int specie = 0;
            double maxVal = -100000000;
            Bird b = pState.getBird(i);
            int[] obsSeq = new int[b.getSeqLength()];
            for (int j = 0; j < obsSeq.length; j++) {
                obsSeq[j] = b.getObservation(j);
            }
            for (BirdModel birdModel : birdModelGuesses.values()) {
//                System.err.println("birdtype: " + birdModel.birdType);
                double prob = birdModel.model.logProbForObsSeq(obsSeq);
                if (prob > maxVal) {
                    maxVal = prob;
                    specie = birdModel.birdType;
                }
            }
//            System.err.println("MAXVAL: " + maxVal);
            lGuess[i] = specie;
        }

        return lGuess;
    }

    /**
     * If you hit the bird you were trying to shoot, you will be notified
     * through this function.
     *
     * @param pState the GameState object with observations etc
     * @param pBird the bird you hit
     * @param pDue time before which we must have returned
     */
    public void hit(GameState pState, int pBird, Deadline pDue) {
        System.err.println("HIT BIRD!!!");
    }

    /**
     * If you made any guesses, you will find out the true species of those
     * birds through this function.
     *
     * @param pState the GameState object with observations etc
     * @param pSpecies the vector with species
     * @param pDue time before which we must have returned
     */
    public void reveal(GameState pState, int[] pSpecies, Deadline pDue) {
        for (int i = 0; i < pSpecies.length; i++) {
            int birdType = pSpecies[i];
            if (birdType == Constants.SPECIES_UNKNOWN)
                continue;

            BirdModel bm = birdModelGuesses.get(birdType);
            Bird bird = pState.getBird(i);
            int[] obsSeq = new int[bird.getSeqLength()];
            for (int s = 0;  s < bird.getSeqLength(); s++) {
                obsSeq[s] = bird.getObservation(s);
            }
            if (bm == null) {
                birdModelGuesses.put(birdType, new BirdModel(birdType));
            }
            birdModelGuesses.get(birdType).addSequence(obsSeq);
//            System.err.println("Bird " + i + " is of specie " + pSpecies[i]);
        }


        //Räkna Baum welch
        for(BirdModel bm : birdModelGuesses.values()) {
            int seqCount = bm.savedSequences.size();
            if (seqCount > 0) {
                int[][] obsSeqs = bm.savedSequences.toArray(new int[seqCount][timePeriod]);

                BaumWelch bw = new BaumWelch(bm.model, obsSeqs);
                bw.run();
//                bm.emptySavedSequences();
            }
        }
        t=0;
    }

    public static final Action cDontShoot = new Action(-1, -1);
}
