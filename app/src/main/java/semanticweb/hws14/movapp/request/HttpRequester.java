package semanticweb.hws14.movapp.request;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

import semanticweb.hws14.movapp.activities.MovieList;
import semanticweb.hws14.movapp.model.Movie;
import semanticweb.hws14.movapp.model.MovieComparator;
import semanticweb.hws14.movapp.model.MovieDet;

/**
 * Created by Frederik on 29.10.2014.
 */

//This class handles the http requests to the webservice
public class HttpRequester {
    //this method is for getting the rating, time, genre in list view
    public static void addOmdbData(final Activity listActivity, final ArrayList<Movie> movieList, final ArrayAdapter<Movie> mlAdapter, final boolean isTime, final boolean isGenre, final boolean isActor, final boolean isDirector, final boolean isCity, final boolean isState, final boolean isPartName) {

            for (final Movie movie : movieList) {


                String url = prepareURL(movie, false);

                JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject r) {
                        boolean response = false;
                        boolean lastMovie = false;
                        try {
                            response = r.getBoolean("Response");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (response) {
                            //TIME is only needed here, when time is active and one of those (isActor, isDirector...)
                            //Because if none of those is active, time attribute is not mandatory in sparql and we would only get movies that fit the constraints.
                            //If one of those others is active, time is optional and needs to be filtered again, this time with data from the web service
                            //--> get more correct movies
                            try {
                                if (isTime && (isActor || isDirector || isGenre || isPartName)) {
                                    if (0 == movie.getReleaseYear()) {
                                        int releaseYear = r.getInt("Year");
                                        movie.setReleaseYear(releaseYear);
                                    }
                                    if (SparqlQueries.filterReleaseDate(movie)) {
                                        if (movieList.size() <= movieList.indexOf(movie) + 1) {
                                            lastMovie = true;
                                        }
                                        movieList.remove(movie);
                                    }
                                }
                            } catch (JSONException e) {
                                movie.setImdbRating("0 No Data");
                            }

                            if (!(movieList.indexOf(movie) == -1)) {
                                //Genre: Same as for time
                                try {
                                    if (isGenre && (isActor || isDirector || isPartName ||isCity || isState || isTime)) {
                                        String genreName = r.getString("Genre");
                                        movie.setGenre(genreName);
                                        if (SparqlQueries.filterGenre(movie)) {
                                            if (movieList.size() <= movieList.indexOf(movie) + 1) {
                                                lastMovie = true;
                                            }
                                            movieList.remove(movie);
                                        }
                                    }
                                } catch (JSONException e) {
                                    movie.setImdbRating("0 No Data");
                                }

                                if (!(movieList.indexOf(movie) == -1)) {
                                    //IMDB ID
                                    try {
                                        if ("".equals(movie.getImdbId())) {
                                            String imdbID = r.getString("imdbID");
                                            movie.setImdbId(imdbID);
                                        }
                                    } catch (JSONException e) {
                                        movie.setImdbId("");
                                    }

                                    //IMDB RATING
                                    try {
                                        double imdbRating = r.getDouble("imdbRating");
                                        movie.setImdbRating(String.valueOf(imdbRating));
                                    } catch (JSONException e) {
                                        movie.setImdbRating("0 No Rating");
                                    }
                                }
                            }
                        } else {
                            if (movieList.size() <= movieList.indexOf(movie) + 1) {
                                lastMovie = true;
                            }
                            movieList.remove(movie);
                        }

                        if (lastMovie || movieList.size() <= movieList.indexOf(movie) + 1) {

                            //adds movies to the list, sorts , and so on
                            if (movieList.size() > 0) {
                                mlAdapter.clear();
                                Collections.sort(movieList, new MovieComparator());
                                mlAdapter.addAll(movieList);
                                listActivity.setProgressBarIndeterminateVisibility(false);
                                MovieList.staticMovieList = movieList;
                                MovieList.staticRequestCanceled = false;
                                MovieList.imdbButton.setVisible(false);
                                MovieList.staticRatingLoaded = true;
                            } else {
                                AlertDialog ad = new AlertDialog.Builder(listActivity).create();
                                ad.setMessage("No movies found!");
                                ad.setCancelable(false); // This blocks the 'BACK' button
                                ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        listActivity.finish();
                                    }
                                });
                                ad.show();
                                listActivity.setProgressBarIndeterminateVisibility(false);
                            }

                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        boolean lastMovie = false;
                        if (movieList.size() <= movieList.indexOf(movie) + 1) {
                            lastMovie = true;
                        }
                        movieList.remove(movie);
                        if (lastMovie || movieList.size() <= movieList.indexOf(movie) + 1) {
                            if (movieList.size() > 0) {
                                Log.e("JSONException", "RESPONSE FAILED");
                                mlAdapter.clear();
                                Collections.sort(movieList, new MovieComparator());
                                mlAdapter.addAll(movieList);
                                listActivity.setProgressBarIndeterminateVisibility(false);
                                MovieList.staticMovieList = movieList;
                                MovieList.staticRequestCanceled = false;
                            } else {
                                AlertDialog ad = new AlertDialog.Builder(listActivity).create();
                                ad.setMessage("No movies found!");
                                ad.setCancelable(false); // This blocks the 'BACK' button
                                ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        listActivity.finish();
                                    }
                                });
                                ad.show();
                                listActivity.setProgressBarIndeterminateVisibility(false);
                            }
                        }
                    }
                });
                jsObjRequest.setTag("movieList");
                HttpRequestQueueSingleton.getInstance(listActivity.getApplicationContext()).addToRequestQueue(jsObjRequest);
            }
        }

    //This method gets data for the detail movie
    public static void loadWebServiceData (final Activity detailActivity, final MovieDet movie) {
        String url = prepareURL(movie, true);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject r) {
                boolean response = false;
                try {
                    response = r.getBoolean("Response");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(response) {
                    try {
                        double imdbRating = r.getDouble("imdbRating");
                        movie.setImdbRating(String.valueOf(imdbRating));
                    } catch (JSONException e) {
                        movie.setImdbRating("0 No Rating");
                    }

                    try {
                        String rated = r.getString("Rated");
                        movie.setRated(rated);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        String genre = r.getString("Genre");
                        String[] genreArray = (genre.split(", "));
                        ArrayList<String> newGenres = new ArrayList<String>();
                        for(int i=0; i< genreArray.length; i++) {
                            newGenres.add(new String(genreArray[i]).trim());

                        }
                        for(int i =0; i < newGenres.size(); i++) {
                            if(!movie.getGenres().contains(newGenres.get(i))) {
                                movie.addGenre(genreArray[i]);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        String plot = r.getString("Plot");
                        movie.setPlot(plot);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        String awards = r.getString("Awards");
                        movie.setAwards(awards);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        String posterUrl = r.getString("Poster");
                        movie.setPoster(posterUrl);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        String metascore = r.getString("Metascore");
                        if(!metascore.equals("N/A")) {
                            movie.setMetaScore(Integer.parseInt(metascore));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        String voteCount = r.getString("imdbVotes");
                        movie.setVoteCount(voteCount);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        int releaseYear = r.getInt("Year");
                        movie.setReleaseYear(releaseYear);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if("".equals(movie.getRuntime())) {
                        try {
                            String runtime = r.getString("Runtime");
                            movie.setRuntime(runtime);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    movie.geteListener().onFinished(movie);
                } else {
                    Log.e("FUCK", "THE SYSTEM");
                    movie.geteListener().onFinished(movie);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("JSONException", "RESPONSE FAILED");
                movie.geteListener().onFinished(movie);
            }
        });
        jsObjRequest.setTag("movieDetail");
        HttpRequestQueueSingleton.getInstance(detailActivity.getApplicationContext()).addToRequestQueue(jsObjRequest);
    }

    //This method chooses the right URL and prepares those
    private static String prepareURL(Movie movie, boolean detail) {
        String url = "";
        String urlTitle = null;
        try {
            urlTitle = URLEncoder.encode(movie.getTitle(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(!"".equals(movie.getImdbId())) {
            url = "http://www.omdbapi.com/?i=" + movie.getImdbId();
        } else if(movie.getReleaseYear() != 0) {
            url = "http://www.omdbapi.com/?t=" + urlTitle + "%20&y=" + movie.getReleaseYear();
        } else {
            url = "http://www.omdbapi.com/?t=" + urlTitle;
        }
        if(detail) {
            url+="&plot=full";
        } else {
            url+="&plot=short";
        }
        return url;
    }
}
