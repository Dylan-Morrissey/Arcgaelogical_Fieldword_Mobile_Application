package org.wit.archaeologicalfieldwork.views.map

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.content_hillfort_maps.*
import org.wit.archaeologicalfieldwork.helpers.readImageFromPath
import org.wit.archaeologicalfieldwork.main.MainApp

class HillfortMapPresenter (val view: HillfortMapView){
    var app: MainApp

    init {
        app = view.application as MainApp
    }

    fun doPopulateMap(map: GoogleMap) {
        map.uiSettings.setZoomControlsEnabled(true)
        map.setOnMarkerClickListener(view)
        app.users.findCurrentUser().hillforts.forEach {
            val loc = LatLng(it.lat, it.lng)
            val options = MarkerOptions().title(it.name).position(loc)
            map.addMarker(options).tag = it.id
        }
    }

    fun doMarkerSelected(marker: Marker) {
        val tag = marker.tag as Long
        val hillfort = app.users.findHillfortById(app.users.findCurrentUser(), tag)
        if(hillfort != null)view.showHillfort(hillfort)

    }
}