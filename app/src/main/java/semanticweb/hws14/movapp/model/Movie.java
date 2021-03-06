package semanticweb.hws14.movapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Frederik on 29.10.2014.
 */

//Movie Class for movies in list and for passing over by intents
public class Movie implements Parcelable {
    private String title;
    private int releaseYear;
    private String genre;
    private String imdbId;
    private String LMDBmovieResource;
    private String DBPmovieResource;
    private String imdbRating;

    private EventListener eListener = null;

    /* Constructors */

    public Movie(String title, int releaseYear, String genre) {
        this.title = title;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.imdbId = "";
        this.LMDBmovieResource = "";
        this.DBPmovieResource = "";
        this.imdbRating = "";
    }

    public Movie(String title, int releaseYear, String genre, String dbPmovieResource, String lmdBmovieResource, String imdbId, String imdbRating) {
        this.title = title;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.imdbId = imdbId;
        this.LMDBmovieResource = lmdBmovieResource;
        this.DBPmovieResource = dbPmovieResource;
        this.imdbRating = imdbRating;

    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(title);
        out.writeInt(releaseYear);
        out.writeString(genre);
        out.writeString(imdbId);
        out.writeString(LMDBmovieResource);
        out.writeString(DBPmovieResource);
        out.writeString(imdbRating);
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    private Movie(Parcel in) {
        title = in.readString();
        releaseYear = in.readInt();
        genre = in.readString();
        imdbId = in.readString();
        LMDBmovieResource = in.readString();
        DBPmovieResource = in.readString();
        imdbRating = in.readString();
    }

    /* Getter and Setter */

    public EventListener geteListener() {
        return eListener;
    }

    public void setOnFinishedEventListener(EventListener listener) {
        eListener = listener;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(String imdbRating) {
        this.imdbRating = imdbRating;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre += " "+genre;
    }

    public String getLMDBmovieResource() {
        return LMDBmovieResource;
    }

    public void setLMDBmovieResource(String LMDBmovieResource) {
        this.LMDBmovieResource = LMDBmovieResource;
    }

    public String getDBPmovieResource() {
        return DBPmovieResource;
    }

    public void setDBPmovieResource(String DBPmovieResource) {
        this.DBPmovieResource = DBPmovieResource;
    }

    //Theses methods help to save data of doublicates before removing them


    public void setMovieResource (String movieResource) {
        if(!"".equals(movieResource)) {
            Pattern p = Pattern.compile("linkedmdb");
            Matcher m = p.matcher(movieResource);
            if(m.find()) {
                if("".equals(LMDBmovieResource)) {
                    LMDBmovieResource = movieResource;
                }
            } else {
                if("".equals(DBPmovieResource)) {
                    DBPmovieResource = movieResource;
                } else {
                    String cleanMovieResource = movieResource.replace('_', ' ');
                    if(cleanMovieResource.equals("<http://dbpedia.org/resource/"+title+">") && !movieResource.equals(DBPmovieResource)) {
                        DBPmovieResource = movieResource;
                    }
                }
            }
        }
    }

    public String getMovieResource () {
        if(!"".equals(DBPmovieResource)) {
            return DBPmovieResource;
        } else if (!"".equals(LMDBmovieResource)) {
            return  LMDBmovieResource;
        } else return "";
    }

    @Override
    public String toString(){
        String rating = "" + imdbRating;
        String out = this.title.toString() + "\nImdb-Rating: " + rating.toString();
        return out;
    }
}
