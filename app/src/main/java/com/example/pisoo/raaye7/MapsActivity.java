package com.example.pisoo.raaye7;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText mfrom, mTo;
    private Button mTakemyCar, mRequestPickup;
    private TextView mTimeText;
    private LinearLayout mLLTime;
    private ArrayList markerPoints;
    // flags to know the searching address is source or destination
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE_From = 1;
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE_To = 2;
    private View mapView;
    private Date date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getSupportActionBar().setTitle(R.string.app_name);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapView = mapFragment.getView();

        linkTheViews();
        manageClicklisteners();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE_From) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                mfrom.setText(place.getName());
                mfrom.setTextColor(Color.BLACK);
                LatLng latLng = place.getLatLng();
                clearTheScreen();
                drawRoutes(latLng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.e("TAG", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE_To) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                mTo.setText(place.getName());
                mTo.setTextColor(Color.BLACK);
                LatLng latLng = place.getLatLng();
                // mMap.addMarker(new MarkerOptions().position(sydney).title(place.getName().toString()));
                drawRoutes(latLng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                //Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }

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
        LatLng CairoLatLang = new LatLng(30.046854, 31.235119);
        mMap.addMarker(new MarkerOptions().position(CairoLatLang).title("Cairo"));
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CairoLatLang, 12));

        // show The Traffic Data
        mMap.setTrafficEnabled(true);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                drawRoutes(latLng);
                getAddress(latLng);
            }
        });
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    // check if The GPS is Opened or not
                    if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                        LatLng latLng = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                        clearTheScreen();
                        CameraUpdate location = CameraUpdateFactory.newLatLngZoom(latLng, 12);
                        mMap.animateCamera(location);
                        drawRoutes(latLng);
                    } else {
                        // release Alart to open The GPS
                        buildAlertMessageNoGps();
                    }
                    return true;
                }
            });

            if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
                // Get the button view
                View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
                // and next place it, on bottom right (as Google Maps app)
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                        locationButton.getLayoutParams();
                // position on right bottom
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                layoutParams.setMargins(0, 0, 30, 30);
            }
        } else {
            Toast.makeText(this, "Your GPS is Closed .", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoutes(LatLng latLng) {

        if (markerPoints.size() > 1) {
            clearTheScreen();
        }
        // Adding new item to the ArrayList
        markerPoints.add(latLng);
        // Creating MarkerOptions
        MarkerOptions options = new MarkerOptions();
        // Setting the position of the marker
        options.position(latLng);
        if (markerPoints.size() == 1) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mfrom.setText(getAddress(latLng));
            mfrom.setTextColor(Color.BLACK);
        } else if (markerPoints.size() == 2) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mTo.setText(getAddress(latLng));
            mTo.setTextColor(Color.BLACK);
        }
        // Add new marker to the Google Map Android API V2
        mMap.addMarker(options);
        // Checks, whether start and end locations are captured
        if (markerPoints.size() >= 2) {
            LatLng origin = (LatLng) markerPoints.get(0);
            LatLng dest = (LatLng) markerPoints.get(1);
            // Getting URL to the Google Directions AP
            String url = getDirectionsUrl(origin, dest);
            DownloadTask downloadTask = new DownloadTask();
            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
        }
    }

    private void clearTheScreen() {
        markerPoints.clear();
        mMap.clear();
        mLLTime.animate().alpha(0.0f).setDuration(2000);
        mfrom.setText("");
        mTo.setText("");
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions;
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);
                for (int j = 0; j < path.size(); j++) {

                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    points.add(new LatLng(lat, lng));
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.geodesic(true);
                lineOptions.color(getRouteColor(i));
                // Drawing polyline in the Google Map for the i-th route
                mMap.addPolyline(lineOptions);
            }
        }
    }

    private int getRouteColor(int i) {
        return ((i == 0) ? Color.BLUE : (i == 1) ? Color.GRAY : (i == 2) ? Color.GREEN : Color.MAGENTA);
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Sensor enabled
        String sensor = "sensor=false";
        //set units with metric
        String units = "units=metric";
        // alternatives enabled to get Multi Route
        String alternatives = "alternatives=true";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + units + "&" + alternatives;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String getAddress(LatLng latLng) {
        if (latLng == null)
            return "";
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String finalAddress = address + " ";
            if (city != null)
                finalAddress += city + " ";
            if (state != null)
                finalAddress += state;
            return finalAddress;
        } catch (IOException io) {
        }
        return "";
    }

    private void linkTheViews() {
        date = new Date();
        markerPoints = new ArrayList();
        mLLTime = (LinearLayout) findViewById(R.id.ll_time);
        mLLTime.animate().alpha(0.0f);
        mTimeText = (TextView) findViewById(R.id.tv_TimeText);
        mfrom = (EditText) findViewById(R.id.dt_from);
        mTo = (EditText) findViewById(R.id.dt_To);
        mTakemyCar = (Button) findViewById(R.id.bt_TakeMyCar);
        mRequestPickup = (Button) findViewById(R.id.bt_RequestPickup);
    }

    private void manageClicklisteners() {

        mTakemyCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTripTime();
            }
        });
        mRequestPickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTripTime();
            }
        });
        // the user will select a destination of the trip
        mTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                            .setCountry("EG")
                            .build();
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .setFilter(typeFilter)
                                    .build(MapsActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE_To);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                    Log.v("Error ", "GooglePlayServicesRepairableException");
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                    Log.v("Error ", "GooglePlayServicesNotAvailableException");
                }
            }
        });
        // the user will select a source of the trip
        mfrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                            .setCountry("EG")
                            .build();
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .setFilter(typeFilter)
                                    .build(MapsActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE_From);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                    Log.e("Error ", "GooglePlayServicesRepairableException");
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                    Log.e("Error ", "GooglePlayServicesNotAvailableException");
                }
            }
        });
    }

    private void getTripTime() {
        Calendar cal = Calendar.getInstance();
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH);
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        final int minute = cal.get(Calendar.MINUTE);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                MapsActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                date.setYear(year - 1900);
                date.setMonth(month);
                date.setDate(dayOfMonth);

                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(MapsActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                date.setHours(hourOfDay);
                                date.setMinutes(minute);

                                SimpleDateFormat parseFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
                                mTimeText.setText(parseFormat.format(date));
                                // make the Trip time Fade in within 2 seconds
                                mLLTime.animate().alpha(1.0f).setDuration(2000);
                            }
                        }, hour, minute, false);
                mTimePicker.setTitle(R.string.SelectTime);
                mTimePicker.show();
            }
        }, year, month, day);
        datePickerDialog.setTitle(R.string.SelectDate);
        datePickerDialog.show();
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.AlartMessageNoGPS)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
