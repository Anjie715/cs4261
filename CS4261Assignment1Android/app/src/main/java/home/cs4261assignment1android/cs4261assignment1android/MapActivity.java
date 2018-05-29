package home.cs4261assignment1android.cs4261assignment1android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import home.cs4261assignment1android.R;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Context context;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location location;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        username = getIntent().getExtras().getString("username");
        Log.d("User is :", username);
        getLastKnownLocation();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }

    @SuppressLint("MissingPermission")
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            getLastKnownLocation();
        }
    }


    public void getLastKnownLocation() {
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                Log.d("Location log", location.getLatitude() + ", " + location.getLongitude());
                                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                        .findFragmentById(R.id.map);
                                MapActivity.this.location = location;
                                mapFragment.getMapAsync(MapActivity.this);
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng current = new LatLng(this.location.getLatitude(), this.location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(current).title("Me"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 6));
        new GetAllMarkers().execute();
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class GetAllMarkers extends AsyncTask<Void, Void, Boolean> {

        String reponseString;

        @Override
        protected Boolean doInBackground(Void... params) {
            // Simulate network access.
            URL url;
            String response = "";
            try {
                url = new URL("https://test-register-api-heroku.herokuapp.com/auth/location");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);


                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                Map<String, String> postDataParams = new HashMap<>();
                postDataParams.put("username", username);
                postDataParams.put("latitude", "" + location.getLatitude());
                postDataParams.put("longitude", "" + location.getLongitude());

                writer.write(Helper.getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } else {
                    response = "";

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                JSONObject json = new JSONObject(response);
                int success = json.getInt("success");
                if (success == 1) {
                    reponseString = response;
                    return true;
                } else {
                    return false;
                }
            } catch (JSONException e) {
                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                try {
                    JSONObject json = new JSONObject(reponseString);
                    JSONArray markerArray = json.getJSONArray("json");
                    for (int i = 0; i < markerArray.length(); i++) {
                        String title = markerArray.getJSONObject(i).getString("username");
                        if (!title.trim().equals(username)) {
                            Double lat = markerArray.getJSONObject(i).getDouble("latitude");
                            Double lon = markerArray.getJSONObject(i).getDouble("longitude");
                            LatLng temp = new LatLng(lat, lon);
                            mMap.addMarker(new MarkerOptions().position(temp).title(title));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {

            }
        }

    }
}

