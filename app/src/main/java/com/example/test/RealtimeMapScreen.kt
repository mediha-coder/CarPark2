
/*package com.example.test

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
@SuppressLint("Lifecycle")
@Composable
fun RealtimeMapScreen(context: Context, destination: Point) {
    val accessToken = context.getString(R.string.mapbox_access_token)
    val mapInitOptions = MapInitOptions(context).apply {
        this.resourceOptions = ResourceOptions.Builder()
            .accessToken(accessToken)
            .build()
    }

    val mapView = remember { MapView(context, mapInitOptions) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(factory = {
        mapView.apply {
            getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
                val locationComponent = location
                locationComponent.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }

                val annotationApi = annotations
                val polylineManager = annotationApi.createPolylineAnnotationManager()

                locationComponent.addOnIndicatorPositionChangedListener { currentPoint ->
                    // Déplace la caméra
                    getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(currentPoint)
                            .zoom(15.0)
                            .build()
                    )

                    // Calculer un itinéraire entre currentPoint et destination
                    val points = listOf(currentPoint, destination)
                   // polylineManager.deleteAll() // Nettoyer l'ancien itinéraire
                    val polyline = PolylineAnnotationOptions()
                        .withPoints(points)
                        .withLineColor("#ff0000")
                        .withLineWidth(5.0)

                    polylineManager.create(polyline)
                }

                locationComponent.onStart()
            }
        }
    })
}*/








