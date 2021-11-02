import java.util.*;
import java.io.*;
import java.lang.reflect.Array;

public class App {

    static ArrayList<String> movies = new ArrayList<>();
    static ArrayList<String> users = new ArrayList<>();

    static HashMap<String, Integer> u1u2bothratedcount = new HashMap<>();
    //ratings stores list of all ratings from ratings.csv
    static ArrayList<String> ratings = new ArrayList<>();
    //testratings stores list of all ratings from test_ratings
    static ArrayList<String> testratings = new ArrayList<>();
    //moviesover100ratings = list of movies rated over 100 times in training data
    static ArrayList<String> moviesOver100ratings = new ArrayList<>();
    //avgratings stores key = movieid, value = average rating for that movie across the training testratings file
    static HashMap<String, Double> avgratings = new HashMap<>();
    //totalratings stores key = movieid, value = sum of all ratings of that movie 
    static HashMap<String, Integer> totalratings = new HashMap<>();
    //movieratingsCount stores key = movieid, value = count of ratings for that movie
    static HashMap<String, Double> movieratingsCount = new HashMap<String, Double>();
    //useratingscount stores key = userid, value = ratingscount
    static HashMap<String, Double> userratingsCount = new HashMap<String, Double>();
    //user2moviesrated stores key = userid, value = list of movies rated
    static HashMap<String, ArrayList<String>> user2moviesrated = new HashMap<String, ArrayList<String>>();
    //movie2usersrated stores key = movieid, value = list of users who rated that movie
    static HashMap<String, ArrayList<String>> movie2usersrated = new HashMap<String, ArrayList<String>>();
    //useridmovieid2rating stores key = userid + movieid, value = rating that user gave that movie
    static HashMap<String, Integer> useridmovieid2rating = new HashMap<String, Integer>();

    static void buildGraph(ArrayList<String> list) {
        ArrayList<String> moviesSeen = new ArrayList<>();
        ArrayList<String> usersSeen = new ArrayList<>();
        //double totalratingcount = 0.0;

        for (String rating : list) {
            String[] thisRating = rating.split(",");
            String userid = thisRating[0];
            users.add(userid);
            String movieid = thisRating[1];
            int movierating = Integer.parseInt(thisRating[2]);
            useridmovieid2rating.put(userid+"+"+movieid, movierating);
            

            if (!moviesSeen.contains(movieid)){
                totalratings.put(movieid, movierating);
                movieratingsCount.put(movieid, 1.0);
                moviesSeen.add(movieid);
                movie2usersrated.put(movieid, new ArrayList<>());
                movie2usersrated.get(movieid).add(userid);
            }
            else {
                totalratings.put(movieid, totalratings.get(movieid) + movierating);
                movieratingsCount.put(movieid, movieratingsCount.get(movieid) + 1);
                double avg = movierating;
                avg += totalratings.get(movieid);
                avg = avg / movieratingsCount.get(movieid);
                avgratings.put(movieid, avg);
                movie2usersrated.get(movieid).add(userid);

            }

            if (!usersSeen.contains(userid)){
                userratingsCount.put(userid, 1.0);
                usersSeen.add(userid);
                user2moviesrated.put(userid, new ArrayList<String>());
                user2moviesrated.get(userid).add(movieid);
            }
            else {
                userratingsCount.put(userid, userratingsCount.get(userid) + 1);
                user2moviesrated.get(userid).add(movieid);

            }
            //totalratingcount += 1;

        }
        Collections.sort(users);
        double max = -1.0;
        double min = Double.MAX_VALUE;
        int maxkey = 0;
        int minKey = 0;
        double finaltotalratings = 0.0;
        for (String movieid : movieratingsCount.keySet()) {
            finaltotalratings+= totalratings.get(movieid);
            if (movieratingsCount.get(movieid) < 100) continue;
            else {
                moviesOver100ratings.add(movieid);
                if (avgratings.get(movieid) > max) {
                    maxkey = Integer.parseInt(movieid);
                    max = avgratings.get(movieid);
                }
                else if (avgratings.get(movieid) < min) {
                    minKey = Integer.parseInt(movieid);
                    min = avgratings.get(movieid);
                }
            }

        }
    }

    public static double getCRD(String user1, String user2, int threshold) {
        double angulardistance = 0.0;
        ArrayList<String> moviesbothrated = new ArrayList<>();
        for (String movieid : user2moviesrated.get(user1)) {
            if (movie2usersrated.get(movieid).contains(user2)) {
                moviesbothrated.add(movieid);
            }
        }
        u1u2bothratedcount.put(user1 + "+" + user2, moviesbothrated.size());

        if (moviesbothrated.size() < 3) {
            angulardistance = 1;
            return angulardistance;
        }
        int[] ratingsa = new int[moviesbothrated.size()];
        int[] ratingsb = new int[moviesbothrated.size()];
        int index = 0;
        //store user1 and user2 ratings for the movies they have in common in seperate arrays ratingsa and ratingsb
        for (String movieid : moviesbothrated) {
            //System.out.println(user1 + movieid);
            ratingsa[index] = useridmovieid2rating.get(user1+"+"+movieid);
            ratingsb[index] = useridmovieid2rating.get(user2+"+"+movieid);
            index++;
        }
        // THIS IS THE PART WHERE WE CALCULATE THE ANGULAR SUM
        //TOP OF EQUATION IS DOT PRODUCT OF ratingsA * ratingsB

        double topofequation = 0;
        double bottomofequation = 0;

        for (int i = 0; i < ratingsa.length; i++) {
            topofequation+= ratingsa[i] * ratingsb[i];
        }
        //BOTTOM OF EQUATION IS PRODUCT OF THE SQUARE ROOT OF THE SUM OF SQUARES FOR EACH ELEMENT IN A AND B
        double sum1 = 0;
        double sum2 = 0;

        for (int i = 0; i < ratingsa.length; i++) {
            sum1+= ratingsa[i] * ratingsa[i];
            sum2+= ratingsb[i] * ratingsb[i];
        }
        sum1 = Math.sqrt(sum1);
        sum2 = Math.sqrt(sum2);

        bottomofequation = sum1 * sum2;
        angulardistance = 1 - topofequation/bottomofequation;

        return angulardistance;


    }
    public static Map<String, Double> reccomender(String user1, int r, int k) {
        HashMap<String, Double> smoothedPrediction = new HashMap<>();

        HashMap<String, ArrayList<String>> haveSeen = new HashMap<>();
        HashMap<String, Double> NJ = new HashMap<>();

        ArrayList<String> M = new ArrayList<>();
        int minthreshold = 3;
        ArrayList<String> N = kNeighborSearch(user1, users, minthreshold, k);

        for (String user2 : N) {
            for (String movieid : user2moviesrated.get(user2)) {
                if (!user2moviesrated.get(user1).contains(movieid)) M.add(movieid);
            }
        }

        double p = 3.5;
        for (String user2 : N) {

            for (String moviej : user2moviesrated.get(user2)) {
                if(!M.contains(moviej)) continue;
                NJ.putIfAbsent(moviej, 0.0);
                haveSeen.putIfAbsent(moviej, new ArrayList<String>());
                NJ.put(moviej, NJ.get(moviej) + useridmovieid2rating.get(user2+"+"+moviej));
                haveSeen.get(moviej).add(user2);
            }
        }
        for (String moviej : M) {
            //System.out.println("movie = " + moviej);
            double yi = NJ.get(moviej) / haveSeen.get(moviej).size();
            //System.out.println("yi = " + yi);
            double topofequation = haveSeen.get(moviej).size() * yi;
            topofequation+= p;
            //System.out.println("topofequation = " + topofequation);
            double bottomofequation = 1 + haveSeen.get(moviej).size();
            //System.out.println("bottomofequation = " + bottomofequation);
            double prediction = topofequation / bottomofequation;
            //System.out.println("prediciton = " + prediction);
            smoothedPrediction.put(moviej, prediction);
        }
        
        List<Map.Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>(smoothedPrediction.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b){
                return b.getValue().compareTo(a.getValue());
            }
        });
        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;

    }
  

    public static ArrayList<String> kNeighborSearch(String user1, ArrayList<String> s, int threshold, int k ) {
        List<String> list = new ArrayList<String>();
        HashMap<String, Double> unsortedMap = new HashMap<>();

        for (String user2 : s ) {
            if (user1.equals(user2)) continue;
            unsortedMap.put(user1+"+"+user2, getCRD(user1, user2, 3));
            if (u1u2bothratedcount.get(user1 + "+" + user2) >= threshold) {
                list.add(user2);
            }
        }

        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
 
        unsortedMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        ArrayList<String> ret = new ArrayList<>();
        int count = 0;
        for (String key : sortedMap.keySet()) {
            String str = key.substring(key.indexOf('+' )+1);
            //System.out.println("str = " + str);
            //ret.add(key.substring(2));
            ret.add(str);
            count++;
            if (count == k) break;

        }
        return ret;

    }

    public static void main(String[] args) throws FileNotFoundException {
        Map<String, Map<String, Double>> allReccomendations = new LinkedHashMap<>();
        int r = 5; int k = 30; 
        String ratingsfile = "ratings.csv"; String moviesfile = "movies.txt"; 
        String testratingsfile = "test_ratings.csv";
        double RMSE = 0.0;
        double baselineRMSE = 0.0;

        try (Scanner s = new Scanner(new FileReader(ratingsfile))) {
            while (s.hasNext()) ratings.add(s.nextLine());   
        }


        try (Scanner s = new Scanner(new FileReader(testratingsfile))) {
            while (s.hasNext()) testratings.add(s.nextLine());   
        }
        //when using test_ratings
        testratings.remove(0);
        try (Scanner s = new Scanner(new FileReader(moviesfile))) {
            while (s.hasNext()) movies.add(s.nextLine());   
        }
        buildGraph(ratings);
        int numtestratings = 1;

        
 /* RMSE CALCULATION
        for (int i = 0; i < numtestratings; i++) {
            String[] thisRating = ratings.get(i).split(",");
            String userid = thisRating[0]; String movieid = thisRating[1];
            Double movierating = Double.parseDouble(thisRating[2]);
            double predictedRating = 0;
            //System.out.println("userid =" + userid);
            double baselineTop = ((movierating - 3.5) * (movierating - 3.5));

            baselineRMSE += baselineTop / numtestratings;
            

            Map<String, Double> map = reccomender(userid, r, k);
            if (!map.containsKey(movieid)) {
                predictedRating = 3.5;
            }
            else {
                predictedRating = map.get(movieid);
            }
            double topofequation = movierating - predictedRating;
            topofequation = topofequation*topofequation;
            RMSE+= topofequation/numtestratings;

            allReccomendations.put(userid, map);

        }
        
        RMSE = Math.sqrt(RMSE);
        baselineRMSE = Math.sqrt(baselineRMSE);
        
        System.out.println("RMSE = " + RMSE);
        System.out.println("Baseline RMSE = " + baselineRMSE);
        //Map<String, Double> reccomendations = allReccomendations.get("196");
*/

        //SORTING AND PRINTING RECCOMENDATIONS 
        Map<String, Double> reccomendations = reccomender("1", r, k);

        int count = 0;
        for (String moviej : reccomendations.keySet()) {
            if (count == r) break;
            System.out.println("movie: " + movies.get(Integer.parseInt(moviej)-1) + 
            "\n prediction: " + reccomendations.get(moviej));
            count++;
            
        }
        //FINISH SORTING AND PRINTING RECCOMENDATIONS*/

    }

}