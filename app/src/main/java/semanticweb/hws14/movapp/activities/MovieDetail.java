package semanticweb.hws14.movapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import semanticweb.hws14.activities.R;
import semanticweb.hws14.movapp.model.EventListener;
import semanticweb.hws14.movapp.model.Movie;
import semanticweb.hws14.movapp.model.MovieDet;
import semanticweb.hws14.movapp.request.HttpRequestQueueSingleton;
import semanticweb.hws14.movapp.request.HttpRequester;
import semanticweb.hws14.movapp.request.SparqlQueries;


public class MovieDetail extends Activity {

    private Activity that = this;
    private MovieDet movieDet;
    private Button btnSpoiler;
    private Button btnActorList;
    private Button btnImdbPage;
    private Button btnRandomRelatedMovies;
    private Button btnRelatedMovies;
    private int rowCount=0;
    private queryForMovieData q;
    private boolean staticRequestCanceled;
    private static Movie staticMovie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_movie_detail);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        initDetailView();

        Intent intent = getIntent();
        Movie movie = intent.getParcelableExtra("movie");
        MovieDet movieD = new MovieDet(movie);
        if(movie.equals(staticMovie)) {
            movie.geteListener().onFinished(movieDet);
        } else {
            staticMovie = movie;
            q = new queryForMovieData();
            q.execute(movieD);
        }

        movieD.setOnFinishedEventListener(new EventListener() {
            @Override
            public void onFinished(final MovieDet movie) {
                movieDet = movie;
                //for the async HttpRequester
                staticRequestCanceled = false;
                setData(movie);
            }
        });
    }

    //Stops async Task and async HttpRequester
    @Override
    protected void onStop () {
        super.onStop();
        if(null != q) {
            q.cancel(true);
        }
        if(staticRequestCanceled) {
            HttpRequestQueueSingleton.getInstance(this.getApplicationContext()).cancelPendingRequests("movieDetail");
        }
    }


    private void initDetailView(){
        btnSpoiler = (Button) findViewById(R.id.btnSpoiler);
        btnActorList =  (Button) findViewById(R.id.btnToActorList);
        btnImdbPage =  (Button) findViewById(R.id.btnLinkImdB);
        btnSpoiler = (Button) findViewById(R.id.btnSpoiler);
        btnRandomRelatedMovies = (Button) findViewById(R.id.btnRandomRelatedMovies);
        btnRelatedMovies = (Button) findViewById(R.id.btnRelatedMovies);

        btnSpoiler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View wikiAbs = findViewById(R.id.tvWikiAbstract);
                if(wikiAbs.getVisibility() == View.VISIBLE) {
                    wikiAbs.setVisibility(View.GONE);
                } else {
                    wikiAbs.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    //This method sets all the data, it receives the movie object from the handler function "setOnFinishedEventListener" which is called from the HTTP Requester after all data has arrived
    private void setData(final MovieDet movie) {
        Thread picThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ImageView img = (ImageView) findViewById(R.id.imageViewMovie);
                    URL url = new URL(movie.getPoster());
                    HttpGet httpRequest;

                    httpRequest = new HttpGet(url.toURI());

                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = httpclient.execute(httpRequest);

                    HttpEntity entity = response.getEntity();
                    BufferedHttpEntity b_entity = new BufferedHttpEntity(entity);
                    InputStream input = b_entity.getContent();

                    Bitmap bitmap = BitmapFactory.decodeStream(input);

                    img.setImageBitmap(bitmap);

                } catch (Exception ex) {
                    Log.d("MovieDetail Picture Error ", ex.toString());
                }
            }
        });
        picThread.start();

        if(!"".equals(movie.getTitle())) {
            TextView movieTitle = (TextView) findViewById(R.id.tvMovieTitle);
            movieTitle.setText(movie.getTitle());
            movieTitle.setVisibility(View.VISIBLE);
        }

        if(!"".equals(movie.getPlot())){
            TextView moviePlot = (TextView) findViewById(R.id.tvPlot);
            moviePlot.setText(movie.getPlot());
            moviePlot.setVisibility(View.VISIBLE);
        }

        TextView ageRestriction = (TextView) findViewById(R.id.tvAgeRestriction);
        TextView arHc = (TextView) findViewById(R.id.tvAgeRestrictionHC);
        String aR = String.valueOf(movie.getRated());

        //Decode the ageRestriction
        if(!aR.equals("")) {
            ageRestriction.setVisibility(View.VISIBLE);
            arHc.setVisibility(View.VISIBLE);
            if (aR.equals("X")) {
                ageRestriction.setText("18+");
            } else if (aR.equals("R")) {
                ageRestriction.setText("16+");
            } else if (aR.equals("M")) {
                ageRestriction.setText("12+");
            } else if (aR.equals("G")) {
                ageRestriction.setText("6+");
            } else {
                ageRestriction.setVisibility(View.GONE);
                arHc.setVisibility(View.GONE);
            }
        }

        TextView ratingCount = (TextView) findViewById(R.id.tvMovieRatingCount);
        TextView ratingCountHc = (TextView) findViewById(R.id.tvMovieRatingCountHC);
        String ratingCountText = movie.getVoteCount();
        ratingCount.setText(ratingCountText);
        manageEmptyTextfields(ratingCountHc, ratingCount, ratingCountText, false);

        if(!"".equals(movie.getImdbRating())) {
            TextView movieRating = (TextView) findViewById(R.id.tvMovieRating);
            if(movie.getImdbRating().equals("0 No Rating")) {
                movieRating.setText("No Rating");
            } else if(movie.getImdbRating().equals("0 No Data")) {
                movieRating.setText("");
            } else {
                movieRating.setText(movie.getImdbRating()+"/10");
            }
            movieRating.setVisibility(View.VISIBLE);
            findViewById(R.id.tvMovieRatingHC).setVisibility(View.VISIBLE);
        }


        TextView metaScore = (TextView) findViewById(R.id.tvMetaScore);
        TextView metaScoreHc = (TextView) findViewById(R.id.tvMetaScoreHC);
        String metaSoreText = String.valueOf(movie.getMetaScore());
        metaScore.setText(metaSoreText +"/100");
        manageEmptyTextfields(metaScoreHc, metaScore, metaSoreText, false);


        TextView tvGenreHc = (TextView) findViewById(R.id.tvGenreHC);
        TextView genre = (TextView) findViewById(R.id.tvGenre);
        String genreText = String.valueOf(movie.createTvOutOfList(movie.getGenres()));
        genre.setText(genreText);
        manageEmptyTextfields(tvGenreHc, genre, genreText, true);

        TextView releaseYear = (TextView) findViewById(R.id.tvReleaseYear);
        TextView releaseYearHc = (TextView) findViewById(R.id.tvReleaseYearHC);
        String releaseYearText = String.valueOf(movie.getReleaseYear());
        releaseYear.setText(releaseYearText);
        manageEmptyTextfields(releaseYearHc, releaseYear, releaseYearText, true);

        TextView runtime = (TextView) findViewById(R.id.tvRuntime);
        TextView runTimeHc = (TextView) findViewById(R.id.tvRuntimeHC);
        String runtimeText = "";
        try {
            runtimeText = runtimeComputation(Float.parseFloat(movie.getRuntime()));
        } catch (Exception e) {
            runtimeText = movie.getRuntime();
        }
        runtime.setText(runtimeText);
        manageEmptyTextfields(runTimeHc, runtime, runtimeText, true);


        TextView budget = (TextView) findViewById(R.id.tvBudget);
        TextView budgetHc = (TextView) findViewById(R.id.tvBudgetHC);
        String budgetText = movie.getBudget();
        //Decode the budget
        if(budgetText.contains("E")){
            BigDecimal myNumber = new BigDecimal(budgetText);
            long budgetLong = myNumber.longValue();
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            budgetText = formatter.format(budgetLong);
            budgetText = String.valueOf(budgetText);
            budgetText = budgetComputation(budgetText);
            budget.setText(budgetText);
            manageEmptyTextfields(budgetHc, budget, budgetText, true);
        }

        TextView awards = (TextView) findViewById(R.id.tvAwards);
        TextView awardsHc = (TextView) findViewById(R.id.tvAwardsHC);
        String awardsText = movie.getAwards();
        awards.setText(awardsText);
        manageEmptyTextfields(awardsHc, awards, awardsText, true);

        TextView tvDirHc = (TextView) findViewById(R.id.tvDirectorsHC);
        TextView directors = (TextView) findViewById(R.id.tvDirectors);
        String directorText = String.valueOf(movie.createTvOutOfList(movie.getDirectors()));
        directors.setText(directorText);
        manageEmptyTextfields(tvDirHc, directors, directorText, true);

        TextView tvWriterHc = (TextView) findViewById(R.id.tvWritersHC);
        TextView writers = (TextView) findViewById(R.id.tvWriters);
        String writerText = String.valueOf(movie.createTvOutOfList(movie.getWriters()));
        writers.setText(writerText);
        manageEmptyTextfields(tvWriterHc, writers, writerText, true);

        TextView tvActorsHc = (TextView) findViewById(R.id.tvActorsHC);
        TextView actors = (TextView) findViewById(R.id.tvActors);
        String actorText = String.valueOf(movie.createTvOutOfList(movie.getActors()));
        actors.setText(actorText);
        manageEmptyTextfields(tvActorsHc, actors, actorText, true);
        colorIt(actors);

        if(movie.getActors().size() > 0) {
            btnActorList.setVisibility(View.VISIBLE);
        }

        btnRandomRelatedMovies.setVisibility(View.VISIBLE);
        btnRelatedMovies.setVisibility(View.VISIBLE);

        TextView tvRolesHc = (TextView) findViewById(R.id.tvRolesHC);
        TextView roles = (TextView) findViewById(R.id.tvRoles);
        ArrayList<String> roleList = movie.getRoles();

        if(roleList.size() > 0){
            roles.setVisibility(View.VISIBLE);
            tvRolesHc.setVisibility(View.VISIBLE);
            roles.setText(String.valueOf(movie.createTvOutOfList(roleList)));
        }

        TextView wikiAbstract = (TextView) findViewById(R.id.tvWikiAbstract);
        wikiAbstract.setText(movie.getWikiAbstract());
        if(!"".equals(movie.getImdbId())) {
            btnImdbPage.setVisibility(View.VISIBLE);
        }
        if(!"".equals(movie.getWikiAbstract())) {
            btnSpoiler.setVisibility(View.VISIBLE);
        }
        try {
            picThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setProgressBarIndeterminateVisibility(false);
    }

    //If a field would be empty, hide it
    private void manageEmptyTextfields(TextView tvHc, TextView tv, String text, boolean doColorIt){
        if(!text.equals("") && !text.equals("N/A") &&  !text.equals("0")){
            tv.setVisibility(View.VISIBLE);
            tvHc.setVisibility(View.VISIBLE);
            if(doColorIt) {
                colorIt(tvHc);
            }
        }
    }

    private void colorIt(TextView tv){
        LinearLayout p = (LinearLayout) tv.getParent();
        if(rowCount%2==0){
            p.setBackgroundColor(Color.rgb(206,238,237));
        }
        else{
            p.setBackgroundColor(Color.rgb(236,248,248));
        }
        rowCount++;
    }

    private String runtimeComputation(float initRuntime){
        int runtime = (int) initRuntime;
        if(initRuntime>1000){
            return runtime/60 + " min";
        }
        return runtime+" min";
    }

    private String budgetComputation(String budgetString){

        int i=budgetString.indexOf(".");
        budgetString = budgetString.substring(0,i);

        return budgetString;
    }

//Button Functions
    public void linkToImdb(View view){
        String imdbUrl = "http://www.imdb.com/title/"+ movieDet.getImdbId()+"/";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imdbUrl));
        startActivity(browserIntent);
    }

    public void toActorList(View view) {
        Intent intent = new Intent(that, ActorList.class);
        intent.putStringArrayListExtra("actorList", movieDet.getActors());
        startActivity(intent);
    }

    public void findRandomRelatedMovies(View view) {
        Intent intent = new Intent(that, MovieList.class);
        HashMap<String, Object> criteria = new HashMap<String, Object>();
        Movie movie = new Movie(movieDet.getTitle(), 0, "");
        movie.setDBPmovieResource(movieDet.getDBPmovieResource());
        criteria.put("isRandomRelated", true);
        criteria.put("relatedMovie", movie);
        intent.putExtra("criteria", criteria);
        startActivity(intent);
    }

    public void findRelatedMovies(View view) {
        Intent intent = new Intent(that, RelationList.class);
        Movie movie = new Movie(movieDet.getTitle(), 0, "");
        movie.setDBPmovieResource(movieDet.getDBPmovieResource());
        intent.putExtra("movie", movie);
        startActivity(intent);
    }

    //Async Task to query for the data
    private class queryForMovieData extends AsyncTask<MovieDet, String, MovieDet> {

        @Override
        protected MovieDet doInBackground(MovieDet... movieArray) {
            staticRequestCanceled = true;
            final SparqlQueries sparqler = new SparqlQueries();
            final MovieDet movie = movieArray[0];
        /* LMDB */
            Thread tLMDBDetail = new Thread(new Runnable() {
                public void run() {
                    String LMDBsparqlQueryString = sparqler.LMDBDetailQuery(movie);
                    Query query = QueryFactory.create(LMDBsparqlQueryString);
                    QueryExecution qexec = QueryExecutionFactory.sparqlService("http://linkedmdb.org/sparql", query);
                    ResultSet results;
                    try {
                        results = qexec.execSelect();
                        for (; results.hasNext(); ) {
                            QuerySolution soln = results.nextSolution();
                            try {
                            if (soln.getLiteral("run") != null && "".equals(movie.getRuntime())) {
                                movie.setRuntime(soln.getLiteral("run").getString());
                            }}catch (Exception e) {
                                Log.d("movieDetail LMDb run ", e.toString());
                            }
                            try {
                                if (soln.getLiteral("aN") != null) {
                                    if(soln.getLiteral("rN") != null) {
                                        movie.addActorRole(soln.getLiteral("aN").getString(), soln.getLiteral("rN").getString());
                                    } else {
                                        movie.addActorRole(soln.getLiteral("aN").getString(), "");
                                    }
                                }}catch (Exception e) {
                                Log.d("movieDetail LMDb aN ", e.toString());
                            }
                            try {
                            if (soln.getLiteral("dN") != null) {
                                movie.addDirector(soln.getLiteral("dN").getString());
                            }}catch (Exception e) {
                                Log.d("movieDetail LMDb dN ", e.toString());
                            }
                            try {
                            if (soln.getLiteral("wN") != null) {
                                movie.addWriter(soln.getLiteral("wN").getString());
                            }}catch (Exception e) {
                                Log.d("movieDetail LMDb wN ", e.toString());
                            }
                            try {
                            if (soln.getLiteral("gN") != null) {
                                movie.addGenre(soln.getLiteral("gN").getString());
                            }}catch (Exception e) {
                                Log.d("movieDetail LMDb gN ", e.toString());
                            }
                        }
                    } catch (Exception e) {
                        Log.e("LINKEDMDBDetail", "Failed " + e.toString());
                    }
                    qexec.close();
                }
            });

            tLMDBDetail.start();
        /* DPBEDIA */

            String dbPediaSparqlQueryString = sparqler.DBPEDIADetailQuery(movie);
            Query query = QueryFactory.create(dbPediaSparqlQueryString);
            QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
            ResultSet results;
            try {
                results = qexec.execSelect();
                for (; results.hasNext(); ) {
                    QuerySolution soln = results.nextSolution();
                    try {
                    if(soln.getLiteral("abs") != null && "".equals(movie.getWikiAbstract())) {
                        movie.setWikiAbstract(soln.getLiteral("abs").getString());
                    }}catch (Exception e) {
                        Log.d("movieDetail dbP abs ", e.toString());
                    }
                    try {
                    if(soln.getLiteral("bu") != null && "".equals(movie.getBudget())) {
                        movie.setBudget(soln.getLiteral("bu").getString());
                    }}catch (Exception e) {
                        Log.d("movieDetail dbP bu ", e.toString());
                    }
                    try {
                    if (soln.getLiteral("r") != null && "".equals(movie.getRuntime())) {
                        movie.setRuntime(soln.getLiteral("r").getString());
                    }}catch (Exception e) {
                        Log.d("movieDetail dbP r ", e.toString());
                    }
                    try {
                    if (soln.getLiteral("aN") != null) {
                        movie.addActorRole(soln.getLiteral("aN").getString(), "");
                    }}catch (Exception e) {
                        Log.d("movieDetail  dbP aN ", e.toString());
                    }
                    try {
                    if (soln.getLiteral("dN") != null) {
                        movie.addDirector(soln.getLiteral("dN").getString());
                    }}catch (Exception e) {
                        Log.d("movieDetail dbP dN ", e.toString());
                    }
                    try {
                    if (soln.getLiteral("wN") != null ) {
                        movie.addWriter(soln.getLiteral("wN").getString());
                    }}catch (Exception e) {
                        Log.d("movieDetail dbP wN ", e.toString());
                    }
                    try {
                    if (soln.getLiteral("gN") != null ) {
                        movie.addGenre(soln.getLiteral("gN").getString());
                    }}catch (Exception e) {
                        Log.d("movieDetail dbP gN ", e.toString());
                    }
                }
            }catch (Exception e){
                Log.e("DBPEDIADetail", "Failed DBPEDIA DOWN "+ e.toString());
                publishProgress("A problem with DBPedia occured");
            }
            qexec.close();

            try {
                tLMDBDetail.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            publishProgress("Sparql loaded. Now loading additional data");
            return movie;
        }

        @Override
        protected void onProgressUpdate (String... values) {
            Toast.makeText(that, values[0], Toast.LENGTH_SHORT).show();
        }

        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);

        }
        //Load additional data from the web service
        public void onPostExecute(MovieDet movie) {
            HttpRequester.loadWebServiceData(that, movie);
        }
    }
}

//TODO Testing
//TODO Add License


