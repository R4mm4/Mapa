package com.example.mapa.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.example.mapa.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class Marcadores {
    GoogleMap nMap;
    Context context;

    public Marcadores(GoogleMap nMap, Context context) {
        this.nMap = nMap;
        this.context = context;
    }
    public void addMarkersDefault(){
        uno(-12.198191,-77.013433,"uno");
    }
    public void uno(Double latitud, Double longitud, String titulo){
        LatLng punto = new LatLng(latitud,longitud);
        int height = 140;
        int width = 165;
        BitmapDrawable uno =(BitmapDrawable) context.getResources().getDrawable(R.drawable.doss);
        Bitmap unos = uno.getBitmap();
        Bitmap uns = Bitmap.createScaledBitmap(unos,width,height,false);
        nMap.addMarker(new MarkerOptions().position(punto).title(titulo).snippet("uno").icon(BitmapDescriptorFactory.fromBitmap(uns)));
    }
}
