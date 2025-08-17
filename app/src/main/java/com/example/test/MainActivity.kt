package com.example.test

import android.content.SharedPreferences
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : ComponentActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var polylineManager: PolylineAnnotationManager
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var pointAnnotationManager: PointAnnotationManager


    private var startPosition: Point? = null
    private val accessToken =
        "sk.eyJ1IjoibWVkaWhhMjAyNSIsImEiOiJjbTk5bnd4c2UwNnlzMmlzN3I2djZlcWZsIn0.FFvgxi-gu7WVatH0ZAUA3g"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val savedPoint = sharedPreferences.getString("start_position", null)
        startPosition = savedPoint?.let { Point.fromJson(it) }

        setContent {
            MyMapScreen()
        }
    }

    @Composable
    fun MyMapScreen() {

        AndroidView(factory = {
            mapView = MapView(it)
            mapboxMap = mapView.mapboxMap
            polylineManager = mapView.annotations.createPolylineAnnotationManager()
            locationComponent = mapView.location
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                // Active la localisation
                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }

            }
            mapView
        }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var isToastShowing = false

            Button(onClick = {
                mapView.location.apply {
                    enabled = true}
                    val carDrawable = AppCompatResources.getDrawable(this@MainActivity, R.drawable.car)
                    val carBitmap = (carDrawable as BitmapDrawable).bitmap
                    val personDrawable = AppCompatResources.getDrawable(this@MainActivity, R.drawable.walk)
                    val personBitmap = (personDrawable as BitmapDrawable).bitmap
                    val imageHolder = ImageHolder.from(personBitmap)
                    mapView.location. locationPuck = LocationPuck2D(topImage = imageHolder)
                    val annotationManager = mapView.annotations.createPointAnnotationManager()
                    annotationManager.deleteAll()

                    val startMarker = startPosition?.let {
                        PointAnnotationOptions()
                            .withPoint(it)
                            .withIconImage(carBitmap)
                    }
                    if (startMarker != null) {
                        annotationManager.create(startMarker)
                    }

                mapView.location.addOnIndicatorPositionChangedListener { location ->
                        val latitude = location.latitude()
                        val longitude = location.longitude()
                        startPosition = Point.fromLngLat(longitude, latitude)
                        sharedPreferences.edit()
                            .putString("start_position", startPosition?.toJson())
                            .apply()

                    if (!isToastShowing) {
                        isToastShowing = true

                        // Affiche le Toast
                        Toast.makeText(this@MainActivity, "📍 Position saved", Toast.LENGTH_SHORT).show()

                        // Reset après 2 secondes (durée de l'affichage du Toast)
                       /* Handler(Looper.getMainLooper()).postDelayed({
                            isToastShowing = false
                        }, 1000) */// 2000 ms = durée du Toast
                    }
                    }

            }) {
                Text("📍 Save my position")
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                val destinationJson = sharedPreferences.getString("start_position", null)
                val destination = destinationJson?.let { Point.fromJson(it) }

                if (destination == null) {
                    Toast.makeText(this@MainActivity, "❗ Aucune position sauvegardée", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Mettre à jour en temps réel la position actuelle et tracer l’itinéraire
               mapView.location.addOnIndicatorPositionChangedListener { location ->
                    val currentPosition = Point.fromLngLat(location.longitude(), location.latitude())
                    drawRoute(currentPosition, destination)
                }




            }) {
                Text("📍 Go back")
            }



        }}
    private fun drawRoute(currentLocation: Point, startLocation: Point) {
        val routeOptions = RouteOptions.builder()
            .coordinatesList(listOf(currentLocation, startLocation))
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .build()
        val client = MapboxDirections.builder()
            .routeOptions(routeOptions)
            .accessToken(getString(R.string.mapbox_access_token))
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val route = response.body()?.routes()?.firstOrNull()
                val geometry = route?.geometry()
                val lineString = geometry?.let {
                    LineString.fromPolyline(it, 6) }
                val routePoints = lineString?.coordinates()

                if (!routePoints.isNullOrEmpty()) {
                    val annotationManager = mapView.annotations.createPolylineAnnotationManager()
                    annotationManager.deleteAll()

                    val polyline = PolylineAnnotationOptions()
                        .withPoints(routePoints)
                        .withLineColor("#007AFF") // bleu iOS-style
                        .withLineWidth(5.0)

                    annotationManager.create(polyline)

                    // Centrer la caméra entre les deux points
                    val midPoint = Point.fromLngLat(
                        (currentLocation.longitude() + startLocation.longitude()) / 2,
                        (currentLocation.latitude() + startLocation.latitude()) / 2
                    )

                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(midPoint)
                            .zoom(14.0)
                            .build()
                    )
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Aucune route trouvée",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "Erreur lors de la récupération de l'itinéraire : ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


}


























