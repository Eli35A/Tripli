package com.example.tripli.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.tripli.R
import com.example.tripli.databinding.FragmentMapBinding
import com.example.tripli.base.ServiceLocator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private val markerToPost = HashMap<String, MapViewModel.PostWithLocation>()
    private var pinDescriptor: BitmapDescriptor? = null

    private val viewModel: MapViewModel by viewModels {
        MapViewModel.Factory(
            ServiceLocator.provideHomePostRepository(requireContext()),
            requireActivity().application
        )
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) enableMyLocation() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFrag = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction().replace(R.id.mapContainer, it).commit()
            }
        mapFrag.getMapAsync(this)
        observeViewModel()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        requestLocationPermission()
        viewModel.postLocations.value?.let { placeMarkers(it) }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.mapLoadingIndicator.isVisible = it }
        viewModel.postLocations.observe(viewLifecycleOwner) { placeMarkers(it) }
        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (msg == null) return@observe
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            viewModel.onErrorShown()
        }
    }

    private fun placeMarkers(posts: List<MapViewModel.PostWithLocation>) {
        val map = googleMap ?: return
        map.clear()
        markerToPost.clear()
        if (pinDescriptor == null) pinDescriptor = createPinDescriptor()
        posts.forEach { item ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(item.latLng)
                    .title(item.post.title)
                    .icon(pinDescriptor)
            )
            if (marker != null) markerToPost[marker.id] = item
        }
        map.setOnMarkerClickListener { marker ->
            markerToPost[marker.id]?.let { item ->
                PostMapBottomSheetFragment.newInstance(item.post)
                    .show(childFragmentManager, PostMapBottomSheetFragment.TAG)
            }
            true
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        val map = googleMap ?: return
        map.isMyLocationEnabled = true
        LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), 5f
                        )
                    )
                }
            }
    }

    private fun createPinDescriptor(): BitmapDescriptor {
        val size = 36
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        canvas.drawCircle(cx, cx, cx, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E8857A") })
        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
