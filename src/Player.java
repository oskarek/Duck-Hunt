import java.util.ArrayList;
import java.util.Arrays;
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
                InitializeModel.generateRandomRow(5));
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
    HashMap<Bird, Boolean> hasTriedToShoot = new HashMap<>();
    // bird, round, obs seq
    int t = 0;
    int timePeriod = 100;
    int timeBreak = 5;
    int[] simsteps;

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

    private void initShootHash(GameState pState) {
        for (int i = 0; i < pState.getNumBirds(); i++) {
            hasTriedToShoot.put(pState.getBird(i), false);
        }
    }
    public Action shoot(GameState pState, Deadline pDue) {
        if (t == 0) {
            simsteps = new int[pState.getNumBirds()];
            initShootHash(pState);
        }
        if (t < 30 || pState.getRound() < 2) {
            t++;
            return cDontShoot;
        }

        for (int i = 0; i < pState.getNumBirds(); i++) {
            if (pDue.remainingMs() > timeBreak) {
                Bird bird = pState.getBird(i);
                if (bird.isAlive() && !hasTriedToShoot.get(bird)) {
                    int[] obsSeq = getBirdSeqUntilDeath(bird);
                    if (!isCloseToAnotherSpecie(pState, obsSeq))
                        break;
                    BirdModel blackStorkModel = birdModelGuesses.get(Constants.SPECIES_BLACK_STORK);
                    BirdModel maxBirdModel = null;
                    double prob = -100000000;
                    if (blackStorkModel == null) {
                        //Här har vi inte en modell för BS - Kanske inte skjuta?
                        //return cDontShoot;
                    } else {

                        double logProb = blackStorkModel.model.logProbForObsSeq(obsSeq);
                        double threshold = -200;
                        if (logProb > threshold) {
                            return cDontShoot;
                        }

                    }
                    if (t > 0 && t > bird.getSeqLength()) {
                        if (bird.getObservation(t - 1) == bird.getObservation(t))
                            simsteps[i]++;
                        else
                            simsteps[i] = 0;
                        if (simsteps[i] > 15)
                            return new Action(i, bird.getObservation(t));
                    }

                    if (pDue.remainingMs() < timeBreak)
                        break;

                    //Träna modell
                    HMMModel initModel = generateInitModel();
                    ArrayList<int[]> obsSeqArrayList = new ArrayList<>();
                    obsSeqArrayList.add(obsSeq);
                    //obsSeqArrayList.addAll(maxBirdModel.savedSequences);
                    BaumWelch bw = new BaumWelch(initModel, obsSeqArrayList);
                    bw.run();

                    double[][] alpha = bw.model.getAlpha(obsSeq).m;
                    double[] obsProbs = bw.model.obsProbsNextStep(alpha);
                    double maxVal = -1000000000;
                    int maxO = -1;
                    for (int o = 0; o < obsProbs.length; o++) {
                        if (pDue.remainingMs() < timeBreak)
                            break;
                        if (obsProbs[o] > maxVal) {
                            maxVal = obsProbs[o];
                            maxO = o;
                        }
                    }
                    if (maxVal > 0.8 && maxO != Constants.MOVE_DEAD) {
                        System.err.println("trying to shoot bird " + i + " in round " + pState.getRound());
                        hasTriedToShoot.put(bird, true);
                        return new Action(i, maxO);
                        //System.err.println("maxVal is " + maxVal + " with sequence : " + Arrays.toString(obsProbs));
                    }
                }

            } else {
                break;
            }
        }
        t++;
        return cDontShoot;
    }

    public boolean isCloseToAnotherSpecie(GameState pState, int[] obsSeq) {
        for (BirdModel birdModel : birdModelGuesses.values()) {
            if (birdModel.birdType != Constants.SPECIES_BLACK_STORK) {
                if (birdModel.model.logProbForObsSeq(obsSeq) > -200) {
                    return true;
                }
            }
        }
        return false;
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
            int[] obsSeq = getBirdSeqUntilDeath(b);
            if (obsSeq.length == 0) {
                lGuess[i] = Constants.SPECIES_UNKNOWN;
                continue;
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
        System.err.println("Hit bird " + pBird);
        pState.getBird(pBird).kill();
        if(pBird == Constants.SPECIES_BLACK_STORK) {
            System.err.println("Hit black stork!");
        }

    }

    private int[] getBirdSeqUntilDeath(Bird bird) {
        ArrayList<Integer> obsList = new ArrayList<>();
        for (int i = 0; i < bird.getSeqLength(); i++) {
            int obs = bird.getObservation(i);
            if (obs == Constants.MOVE_DEAD) {
                break;
            }
            obsList.add(obs);
        }
        int[] obsSeq = new int[obsList.size()];
        for (int i = 0; i < obsList.size(); i++) {
            obsSeq[i] = obsList.get(i);
        }
        return obsSeq;
    }

    private HMMModel generateInitModel() {
        return new HMMModel(InitializeModel.generateRandomMatrix(), InitializeModel.generateRandomMatrix(),
                InitializeModel.generateRandomRow(5));
    }

    private ArrayList<Integer> arrayToArraylist(int[] l) {
        ArrayList<Integer> arrayList = new ArrayList<>(l.length);
        for (int i = 0; i < l.length; i++) {
            arrayList.add(l[i]);
        }
        return arrayList;
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
            if (pDue.remainingMs() < timeBreak)
                break;
            int birdType = pSpecies[i];
            if (birdType == Constants.SPECIES_UNKNOWN)
                continue;

            BirdModel bm = birdModelGuesses.get(birdType);
            Bird bird = pState.getBird(i);
            int[] obsSeq = getBirdSeqUntilDeath(bird);
            if (bm == null) {
                birdModelGuesses.put(birdType, new BirdModel(birdType));
            }
            birdModelGuesses.get(birdType).addSequence(obsSeq);
//            System.err.println("Bird " + i + " is of specie " + pSpecies[i]);
        }


        //Räkna Baum welch
        for(BirdModel bm : birdModelGuesses.values()) {
            if (pDue.remainingMs() < timeBreak)
                break;
            int seqCount = bm.savedSequences.size();
            if (seqCount > 0) {
//                int[][] obsSeqs = bm.savedSequences.toArray(new int[seqCount][timePeriod]);

                BaumWelch bw = new BaumWelch(bm.model, bm.savedSequences);
                bw.run();
//                bm.emptySavedSequences();
            }
        }
        t=0;
    }

    public static final Action cDontShoot = new Action(-1, -1);
}