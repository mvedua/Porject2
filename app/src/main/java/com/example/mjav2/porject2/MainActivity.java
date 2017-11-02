package com.example.mjav2.porject2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private RadioGroup resHallDirections;
    private RadioButton northRadio, southRadio;
    private TextView coordsTitle, usrHallTitle, coordinates, usrHall, wash, dry;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location loc;
    private double[] latLong;
    private Map<String, double[]> res_halls;
    private Map<String, String[]> locationId;
    public static final String MyPREF = "Saved";
    public static final String LatKey = "latkey";
    public static final String LongKey = "longkey";
    public static final String ClosestResHallKey = "reshallkey";

    SharedPreferences sharedPreferences;

    String url =  "http://msu.esuds.net/RoomStatus/showRoomStatus.i?locationId=1019834"; // East Wilson

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initEverything();

        sharedPreferences = getSharedPreferences(MyPREF, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        // Creates double[] from SharedPrefs and will set the coordinates to it
        // as long as the SharedPrefs does not return the default value of 0.
        latLong = new double[]{Double.parseDouble(sharedPreferences.getString(LatKey,"0")),Double.parseDouble(sharedPreferences.getString(LongKey,"0"))};
        if(latLong[0] != 0) {
            String c = String.format("%.4f", latLong[0]) + ", " + String.format("%.4f", latLong[1]);
            coordinates.setText(c);
            usrHall.setText(closestHall(latLong));
        }

        locationListener = new LocationListener() {
            @Override
            // Updates location of the user and sets the coordinates and closestHall TextView
            // Saves the closest hall, latitude, and longitude to SharedPrefs as Strings
            public void onLocationChanged(Location location) {
                Toast.makeText(getApplicationContext(), "Location Updated...", Toast.LENGTH_SHORT).show();
                loc = location;

                updateViews();

                editor.putString(ClosestResHallKey, closestHall(new double[]{loc.getLatitude(), loc.getLongitude()}));
                editor.putString(LatKey, String.valueOf(location.getLatitude()));
                editor.putString(LongKey, String.valueOf(location.getLongitude()));
                editor.apply();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            // Has user turn on Location Services if it is disabled
            public void onProviderDisabled(String provider) {
                Toast.makeText(getApplicationContext(), "Please enable Location Services.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };

        // Checks if Android Version > 6.0 to see if it needs runtime permissions
        // Starts location if it is less than Android 6.0, asks for permissions if greater than
        // If permission is granted, then it also automatically starts Locations Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                 requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.INTERNET}, 10);
            } else {
                startGPS();
            }
        }


    } // onCreate


    @Override
    // Callback to see if user granted permissions
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            // 10 - Locations Permission
            // Starts GPS if user granted access and builds an AlertDialog if the user
            // does not. The app
            case 10:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startGPS();
                } else {
                    AlertDialog.Builder builder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
                    } else {
                        builder = new AlertDialog.Builder(this);
                    }
                    builder.setTitle("Locations Services")
                            .setMessage("This application will not function unless you accept to share your location.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.INTERNET}, 10);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                return;
        }
    }

    private void startGPS() {
        Toast.makeText(getApplicationContext(), "Starting Location Updates.", Toast.LENGTH_LONG).show();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locationListener);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.northRadio:
                break;
            case R.id.southRadio:
                break;
        }
    }

    private Map<String, double[]> addResHalls(){
        Map<String, double[]> m = new HashMap<String, double[]>();

        // double[0] = Latitude, [1] = Longitude, [2] = NSEWAB
        // [2] - 0 = north/south, 1 = east/west, 2 = a/b, 3 = none
        m.put("Abbot Hall", new double[]{42.731230, -84.472907,3});
        m.put("Akers Hall", new double[]{42.724194, -84.464760,1});
        m.put("Armstrong Hall", new double[]{42.731107, -84.497205,2});
        m.put("Bailey Hall", new double[]{42.730157,-84.596438,2});
        m.put("Bryan Hall", new double[]{42.731950, -84.497173,2});
        m.put("Butterfield Hall", new double[]{42.732674,-84.494732,2});
        m.put("Campbell Hall", new double[]{42.734523, -84.484397,0});
        m.put("Case Hall", new double[]{42.724523, -84.4888709,0});
        m.put("Emmons Hall", new double[]{42.730387, -84.494458,3});
        m.put("Gilchrist Hall", new double[]{42.733793, -84.487397,3});
        m.put("Holden Hall", new double[]{42.721043, -84.488539,1});
        m.put("Holmes Hall", new double[]{42.726620, -84.464674,1});
        m.put("Hubbard Hall", new double[]{42.723387, -84.463757,0});
        m.put("Landon Hall", new double[]{42.733856, -84.484998,1});
        m.put("Mason Hall", new double[]{42.731427, -84.474108,3});
        m.put("McDonel Hall", new double[]{42.726549, -84.467889,1});
        m.put("Phillips Hall", new double[]{42.729972, -84.473455,3});
        m.put("Rather Hall", new double[]{42.732896,-84.496556,2});
        m.put("Shaw Hall", new double[]{42.726708, -84.475381,1});
        m.put("Snyder Hall", new double[]{42.729936, -84.472836,3});
        m.put("Williams Hall", new double[]{42.734199, -84.488339,0});
        m.put("Wilson Hall", new double[]{42.722631, -84.488967,1});
        m.put("Wonders Hall", new double[]{42.724302, -84.490008,0});
        m.put("Yakeley Hall", new double[]{42.733738, -84.486425,1});

        return m;
    }

    private Map<String,String[]> addLocIds() {
        Map<String,String[]> m = new HashMap<String, String[]>();

        m.put("Abbot Hall",new String[]{"none","1027009"});
        m.put("East Akers Hall",new String[]{"East","1016486"});
        m.put("West Akers Hall",new String[]{"West","1016365"});
        m.put("Armstrong A Hall",new String[]{"A","1028672"});
        m.put("Armstrong B Hall",new String[]{"B","1028673"});
        m.put("A Bailey Hall",new String[]{"A","1028675"});
        m.put("B Bailey Hall",new String[]{"B","1028676"});
        m.put("A Bryan Hall",new String[]{"A","1027010"});
        m.put("B Bryan Hall",new String[]{"B","1027029"});
        m.put("A Butterfield Hall",new String[]{"A","1027030"});
        m.put("B Butterfield Hall",new String[]{"B","1027012"});
        m.put("North Campbell Hall",new String[]{"North","1027014"});
        m.put("South Campbell Hall",new String[]{"South","1027015"});
        m.put("North Case Hall",new String[]{"North","1019825"});
        m.put("South Case Hall",new String[]{"South","1019826"});
        m.put("Emmons Hall",new String[]{"none","1027018"});
        m.put("Gilchrist Hall",new String[]{"none","1028670"});
        m.put("East Holden Hall",new String[]{"East","1019828"});
        m.put("West Holden Hall",new String[]{"West","1019829"});
        m.put("East Holmes Hall",new String[]{"East","1008992"});
        m.put("West Holmes Hall",new String[]{"West","1008993"});
        m.put("North Hubbard Hall",new String[]{"North","1016925"});
        m.put("South Hubbard Hall",new String[]{"South","1016906"});
        m.put("East Landon Hall",new String[]{"East","1027020"});
        m.put("West Landon Hall",new String[]{"West","1027021"});
        m.put("Mason Hall",new String[]{"none","1027023"});
        m.put("East McDonel Hall",new String[]{"East","1016907"});
        m.put("West McDonel Hall",new String[]{"West","1016908"});
        m.put("Phillips Hall",new String[]{"none","1027025"});
        m.put("A Rather Hall",new String[]{"A","1027027"});
        m.put("B Rather Hall",new String[]{"B","1027048"});
        m.put("East Shaw Hall",new String[]{"East","1028649"});
        m.put("West Shaw Hall",new String[]{"West","1028650"});
        m.put("Snyder Hall",new String[]{"none","1027050"});
        m.put("North Williams Hall",new String[]{"North","1027052"});
        m.put("South Williams Hall",new String[]{"South","1027053"});
        m.put("East Wilson Hall",new String[]{"East","1019834"});
        m.put("West Wilson Hall",new String[]{"West","1019835"});
        m.put("North Wonders Hall",new String[]{"North","1019832"});
        m.put("South Wonders Hall",new String[]{"South","1019831"});
        m.put("East Yakeley Hall",new String[]{"East","1027055"});
        m.put("West Yakeley Hall",new String[]{"West","1027056"});

        return m;
    }

    private String closestHall(double[] l){

        TreeMap<Double, String> m = new TreeMap<Double, String>();

        for(Map.Entry<String, double[]> entry : res_halls.entrySet()){
            m.put(distance(l, entry.getValue()), entry.getKey());
        }
        return m.firstEntry().getValue();
    }

    private Double distance(double[] h1, double[] h2){
        return new Double(Math.sqrt( Math.pow( (h1[0]-h2[0]),2 ) + Math.pow( (h1[1]-h2[1]),2 ) ));
    }

    private void initEverything(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        res_halls = addResHalls();
        locationId = addLocIds();

        coordinates = (TextView) findViewById(R.id.coords_dynamic);
        usrHall = (TextView) findViewById(R.id.hall_dynamic);
        coordsTitle = (TextView) findViewById(R.id.coords_static);
        usrHallTitle = (TextView) findViewById(R.id.hall_static);
        resHallDirections = (RadioGroup) findViewById(R.id.resDirections);
        northRadio = (RadioButton) findViewById(R.id.northRadio);
        southRadio = (RadioButton) findViewById(R.id.southRadio);
        wash = (TextView) findViewById(R.id.wash);
        dry = (TextView) findViewById(R.id.dry);

        Typeface gotham = Typeface.createFromAsset(getAssets(), "fonts/GothamUltra.otf");

        coordsTitle.setTypeface(gotham);
        coordinates.setTypeface(gotham);
        usrHallTitle.setTypeface(gotham);
        usrHall.setTypeface(gotham);
        northRadio.setTypeface(gotham);
        southRadio.setTypeface(gotham);
        wash.setTypeface(gotham);
        dry.setTypeface(gotham);

        resHallDirections.setVisibility(View.VISIBLE);
    }

    // Updates Views at launch and whenever location is changed
    private void updateViews(){
        if(loc != null){
            latLong = new double[] {loc.getLatitude(), loc.getLongitude()};
            String c = String.format("%.4f", latLong[0]) + ", " + String.format("%.4f", latLong[1]);
            coordinates.setText(c);
            usrHall.setText(closestHall(latLong));

            if ( (res_halls.get(closestHall(latLong)))[2] == 3 ) {
                resHallDirections.setVisibility(View.INVISIBLE);
            } else {
                resHallDirections.setVisibility(View.VISIBLE);
                switch ( (int)((res_halls.get(closestHall(latLong)))[2]) ){
                    case 0:
                        northRadio.setText("North");
                        southRadio.setText("South");
                        break;
                    case 1:
                        northRadio.setText("East");
                        southRadio.setText("West");
                        break;
                    case 2:
                        northRadio.setText("A");
                        southRadio.setText("B");
                        break;
                }
            }
        }

    }
}