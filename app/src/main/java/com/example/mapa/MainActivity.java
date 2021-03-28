package com.example.mapa;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.maps.android.PolyUtil;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mapa.Utils.GPS_controler;
import com.example.mapa.Utils.Utils;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    Context context;
    GoogleMap nMap;
    JsonObjectRequest jsonObjectRequest;
    RequestQueue request;
    Location location;
    GPS_controler gpsTracker;
    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private static final int MY_PERMISSION_REQUEST_API = 1002;
    String[] appPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkAndRequestPermissions()) {
            // El permiso ya ha sido concedido.

            // Obtain the SupportMapFragment and get notified when the
            // map is ready to be used.
            gpsTracker = new GPS_controler(getApplicationContext());
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            request = Volley.newRequestQueue(getApplicationContext());
        }

    }

    public boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    perm
            ) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    listPermissionsNeeded.toArray(
                            new String[listPermissionsNeeded.size()]
                    ), PERMISSIONS_REQUEST_CODE
            );
            return false;
        }
        return true;
    }


    @Override
    public void onMapClick(LatLng latLng) {

    }

    Boolean actualPosition = true; // Indica si es la posición actual del usuario.
    JSONObject jsonObject; // JSONObject que tiene la información para realizar el trazado de ruta.
    Double longitudOrigen, latitudOrigen; // Contienen la información de Longitud/Latitud de origen.

    LatLng origen, destino; // Contienen la Longitud/Latitud de los marcadores Origen/Destino.
    ArrayList markerPoints = new ArrayList(); // ArrayList que contiene los datos de los marcadores.
    private FusedLocationProviderClient fusedLocationProviderClient;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        nMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        nMap.setMyLocationEnabled(true); // Permite la localización del usuario.

        /* Habilita la localización de servicios mediante el proveedor de cliente actual. */
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        /* Obtiene nuestra última localización. */
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(
                this,
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) { // Localización correcta.
                        if (location != null) {
                            latitudOrigen = location.getLatitude(); // Altitud localización act.
                            longitudOrigen = location.getLongitude(); // Longitud loc. actual.
                            actualPosition = false;

                            /* Posición actual del usuario, añade los datos de Latitud/Longitud. */
                            LatLng miPosicion = new LatLng(latitudOrigen, longitudOrigen);

                            /* Cambia la posición geográfica de la camara a la del usuario. */
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(latitudOrigen, longitudOrigen))
                                    .zoom(14)
                                    .build();
                            nMap.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(cameraPosition)
                            );
                        } else {
                            /* Falló en obtener la ubicación del usuario. */
                            Toast.makeText(
                                    MainActivity.this,
                                    "No se pudo obtener su ubicación...",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );

        /* OnLongClickListener se encarga de agregar los marcadores en el sitio donde el
         * usuario quiere obtener la ruta entre dos puntos. */
        nMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            /**
             * Obtiene la Longitud/Latitud del punto donde esta agregando el marcador.
             * @param latLng Contiene los datos de Longitud/Latitud.
             */
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                    nMap.clear();
                }

                markerPoints.add(latLng); // Añade el marcador al Array de Points.

                MarkerOptions options = new MarkerOptions();
                options.position(latLng);

                if (markerPoints.size() == 1) { // Marcador del punto de origen.
                    nMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("Marcador Origen")
                            .snippet("Origen"));
                } else if (markerPoints.size() == 2) { // Marcador del punto de destino.
                    nMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("Marcador Destino")
                            .snippet("Destino"));
                }

                if (markerPoints.size() >= 2) { // Si ya existen dos puntos, traza la ruta.
                    origen = (LatLng) markerPoints.get(0); // Latitud/Longitud marcador origen.
                    destino = (LatLng) markerPoints.get(1); // Latitud/Longitud marcador destino.

                    /* URL que utiliza la Longitud/Latitud de los puntos de origen y destino
                     * y utiliza la API Key. */
                    String url =
                            "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                                    origen.latitude + "," + origen.longitude +
                                    "&destination=" + destino.latitude + "," + destino.longitude +
                                    "&key=AIzaSyC2-KpjjCwUXSpCLWh4mt4KRKFEPtGz4Rs";

                    /* Se realiza un Request con Volley para interpretar los datos JSON del URL. */
                    RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) { // Respuesta para trazar la ruta.
                            try {
                                jsonObject = new JSONObject(response);
                                TrazarRuta(jsonObject);

                                Log.i("JSONruta: ", response);
                            } catch (JSONException e) { // No hubo respuesta, se produjo un error.
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() { // Error en el Volley.
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });

                    queue.add(stringRequest); // Añade el request.
                }
            }
        });
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Utils.coordenadas.setOrigenLat(latLng.latitude);
        Utils.coordenadas.setDestinoLat(latLng.longitude);
        Toast.makeText(MainActivity.this,"Toque icono para que seleccione",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {

                Toast.makeText(this, "You didn't give permission to access device location", Toast.LENGTH_LONG).show();
                startInstalledAppDetailsActivity();
            }
        }
    }
    public void ObtenerRuta(String latInicial, String lngInicial, String latFinal, String lngFinal){

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + latInicial + "," + lngInicial + "&destination=" + latFinal + "," + lngFinal + "&key=AIzaSyCd5cH4hiWh7UuN4SO_wS6Ld13ZCy-mN_A&mode=drive";



        jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray jRoutes = null;
                JSONArray jLegs = null;
                JSONArray jSteps = null;


                try {

                    jRoutes = response.getJSONArray("routes");


                    for(int i=0;i<jRoutes.length();i++){

                        jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                        List<HashMap<String, String>> path = new ArrayList<HashMap<String, String>>();

                        for(int j=0;j<jLegs.length();j++){
                            jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");


                            for(int k=0;k<jSteps.length();k++){

                                String polyline = "";
                                polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                                List<LatLng> list = decodePoly(polyline);

                                for(int l=0;l<list.size();l++){

                                    HashMap<String, String> hm = new HashMap<String, String>();
                                    hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
                                    hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
                                    path.add(hm);

                                }
                            }

                            Utils.routers.add(path);

                            Intent intent = new Intent(MainActivity.this, TrasarLinea.class);
                            startActivity(intent);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }catch (Exception e){
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "No se puede conectar "+error.toString(), Toast.LENGTH_LONG).show();
                System.out.println();
                Log.d("ERROR: ", error.toString());
            }
        }
        );

        request.add(jsonObjectRequest);

    }


    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }


    private void TrazarRuta(JSONObject jso) {
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {
            jRoutes = jso.getJSONArray("routes");
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) (jRoutes.get(i))).getJSONArray("legs");

                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "" + ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        Log.i("END", "" + polyline);
                        List<LatLng> list = PolyUtil.decode(polyline);
                        nMap.addPolyline(new PolylineOptions().addAll(list).color(Color.GREEN).width(5));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }




    public  class MyAsyncTask extends AsyncTask<Integer, Integer, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();




        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected String doInBackground(Integer... integers) {

            try {
                while (location == null){
                    location = gpsTracker.getLocation();
                    publishProgress(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            location = gpsTracker.getLocation();
            publishProgress(2);

            return "Fin";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            if(values[0] == 0){
                Log.d("Asyntask", "null");
            }else{
                Log.d("Asyntask", "Coordenadas");
                Toast.makeText(MainActivity.this, "LISTO", Toast.LENGTH_SHORT).show();
                Utils.coordenadas.setOrigenLat(location.getLatitude());
                Utils.coordenadas.setOrigenLng(location.getLongitude());
                Log.d("Asyntask", String.valueOf(location.getLatitude()));
                Log.d("Asyntask", String.valueOf(location.getLongitude()));
                ObtenerRuta(String.valueOf(Utils.coordenadas.getOrigenLat()), String.valueOf(Utils.coordenadas.getOrigenLng()),
                        String.valueOf(Utils.coordenadas.getDestinoLat()), String.valueOf(Utils.coordenadas.getDestinoLng()));

            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("asyntask", "FIN");

        }
    }







    public void AlertShow(String title, final LatLng latLng){
        AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);

        builder.setMessage("Desea ir este punto?");
        builder.setTitle(title);
        builder.setCancelable(false);

        builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utils.coordenadas.setDestinoLat(latLng.latitude);
                Utils.coordenadas.setDestinoLng(latLng.longitude);

                new MyAsyncTask().execute(0);

            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        AlertShow(marker.getTitle(),marker.getPosition());
        return false;
    }
    private Boolean permissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == ((PackageManager.PERMISSION_GRANTED));
    }
    private void startInstalledAppDetailsActivity() {
        Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

}