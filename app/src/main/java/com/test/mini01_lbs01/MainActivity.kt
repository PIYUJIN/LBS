package com.test.mini01_lbs01

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.test.mini01_lbs01.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    lateinit var activityMainBinding: ActivityMainBinding

    // 승인받을 권한 목록
    val permissionList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // 위치 측정 리스너
    var myLocationListener: LocationListener? = null

    // 구글 지도 객체
    lateinit var mainGoogleMap: GoogleMap

    // 현재 사용자 위치에 표시되는 마커
    var myMarker: Marker? = null

    // 사용자의 현재 위치
    lateinit var userLocation:Location

    val dialogData = arrayOf(
        "accounting", "airport", "amusement_park",
        "aquarium", "art_gallery", "atm", "bakery",
        "bank", "bar", "beauty_salon", "bicycle_store",
        "book_store", "bowling_alley", "bus_station",
        "cafe", "campground", "car_dealer", "car_rental",
        "car_repair", "car_wash", "casino", "cemetery",
        "church", "city_hall", "clothing_store", "convenience_store",
        "courthouse", "dentist", "department_store", "doctor",
        "drugstore", "electrician", "electronics_store", "embassy",
        "fire_station", "florist", "funeral_home", "furniture_store",
        "gas_station", "gym", "hair_care", "hardware_store", "hindu_temple",
        "home_goods_store", "hospital", "insurance_agency",
        "jewelry_store", "laundry", "lawyer", "library", "light_rail_station",
        "liquor_store", "local_government_office", "locksmith", "lodging",
        "meal_delivery", "meal_takeaway", "mosque", "movie_rental", "movie_theater",
        "moving_company", "museum", "night_club", "painter", "park", "parking",
        "pet_store", "pharmacy", "physiotherapist", "plumber", "police", "post_office",
        "primary_school", "real_estate_agency", "restaurant", "roofing_contractor",
        "rv_park", "school", "secondary_school", "shoe_store", "shopping_mall",
        "spa", "stadium", "storage", "store", "subway_station", "supermarket",
        "synagogue", "taxi_stand", "tourist_attraction", "train_station",
        "transit_station", "travel_agency", "university", "eterinary_care","zoo"
    )

    val latitudeList = mutableListOf<Double>()
    val longitutdeList = mutableListOf<Double>()
    val nameList = mutableListOf<String>()
    val vicinityList = mutableListOf<String>()
    val markerList = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        installSplashScreen()

        MapsInitializer.initialize(this,MapsInitializer.Renderer.LATEST,null)

        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        activityMainBinding.run{
            toolbarMain.run{
                title = "LBSProject"
                setTitleTextColor(Color.WHITE)
                inflateMenu(R.menu.main_menu)
                setOnMenuItemClickListener {

                    when(it?.itemId){
                        // 현재 위치 메뉴
                        R.id.menu_location ->{
                            // 현재 위치를 측정하고 지도를 갱신한다.
                            getMyLocation()
                        }
                        // 장소 종류 선택
                        R.id.menu_place -> {
                            val builder = AlertDialog.Builder(this@MainActivity)
                            builder.setTitle("장소 종류 선택")
                            builder.setNegativeButton("취소", null)
                            builder.setNeutralButton("초기화"){ dialogInterface: DialogInterface, i: Int ->
                                // 리스트 초기화
                                latitudeList.clear()
                                longitutdeList.clear()
                                nameList.clear()
                                vicinityList.clear()
                                for(marker in markerList){
                                    marker.remove()
                                }
                                markerList.clear()
                            }
                            builder.setItems(dialogData){ dialogInterface: DialogInterface, i: Int ->
                                thread {
                                    // 접속할 주소
                                    val location = "${userLocation.latitude},${userLocation.longitude}"
                                    val radius = 50000
                                    val language = "ko"
                                    val type = dialogData[i]
                                    val key = "${BuildConfig.MAPS_API_KEY}"
                                    val site = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${location}&radius=${radius}&language=${language}&type=${type}&key=${key}"

                                    runOnUiThread {
                                        // 리스트 초기화
                                        latitudeList.clear()
                                        longitutdeList.clear()
                                        nameList.clear()
                                        vicinityList.clear()
                                        for(marker in markerList){
                                            marker.remove()
                                        }
                                        markerList.clear()
                                    }

                                    // 다음 페이지의 토큰
                                    var nextToken:String? = null

                                    do {
                                        // nextToken != null인 경우 : 주소 뒤에 삽입
                                        val site2 = if(nextToken != null){
                                            "${site}&pagetoken=${nextToken}"
                                        } else {
                                            site
                                        }

                                        // 요청
                                        val url = URL(site2)
                                        val httpURLConnection = url.openConnection() as HttpURLConnection
                                        val inputStreamReader = InputStreamReader(httpURLConnection.inputStream)
                                        val bufferedReader = BufferedReader(inputStreamReader)

                                        var str:String? = null
                                        val stringBuffer = StringBuffer()

                                        do{
                                            str = bufferedReader.readLine()
                                            if(str != null){
                                                stringBuffer.append(str)
                                            }
                                        } while(str != null)

                                        val data = stringBuffer.toString()

                                        // JSON Object 생성
                                        val root = JSONObject(data)
                                        val status = root.getString("status")
                                        // status = ok인 경우에만 수행
                                        if(status == "OK"){

                                            val resultsArray = root.getJSONArray("results")

                                            // JSONArray가 관리하는 JSONObject의 수 만큼 반복
                                            for(idx in 0 until resultsArray.length()){
                                                // idx 번째 JSONObject 추출
                                                val resultObject = resultsArray.getJSONObject(idx)

                                                // 위도, 경도
                                                val geometryObject = resultObject.getJSONObject("geometry")
                                                val locationObject = geometryObject.getJSONObject("location")
                                                val lat = locationObject.getDouble("lat")
                                                val lng = locationObject.getDouble("lng")
                                                val name = resultObject.getString("name")
                                                // 주소
                                                val vicinity = resultObject.getString("vicinity")

                                                latitudeList.add(lat)
                                                longitutdeList.add(lng)
                                                nameList.add(name)
                                                vicinityList.add(vicinity)
                                            }
                                        }

                                        if(root.has("next_page_token")){
                                            nextToken = root.getString("next_page_token")
                                        } else {
                                            nextToken = null
                                        }

                                    }while(nextToken != null)

                                    runOnUiThread {
                                        // 지도에 마커 표시
                                        // 데이터 수 만큼 반복
                                        for(idx in 0 until latitudeList.size){

                                            val markerOptions = MarkerOptions()
                                            val loc = LatLng(latitudeList[idx], longitutdeList[idx])
                                            markerOptions.position(loc)
                                            markerOptions.title(nameList[idx])
                                            markerOptions.snippet(vicinityList[idx])

                                            val marker = mainGoogleMap.addMarker(markerOptions)
                                            markerList.add(marker!!)
                                        }
                                    }
                                }
                            }
                            builder.show()
                        }
                    }

                    false
                }
            }
        }

        requestPermissions(permissionList,0)

        // MapFragment 객체 추출
        val supportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // 구글 지도 사용 준비 완료시 반응하는 리스너 등록
        supportMapFragment.getMapAsync {

            // 구글맵 객체 변수에 담아 사용
            mainGoogleMap = it

            // 지도의 옵션 설정
            // 확대 축소 기능
            it.uiSettings.isZoomControlsEnabled = true

            // 현재 위치 표시 기능
            it.isMyLocationEnabled = true

            // 현재 위치 표시하는 버튼 표시 여부
            it.uiSettings.isMyLocationButtonEnabled = false

            // 맵타입
            // it.mapType = GoogleMap.MAP_TYPE_NONE
            // it.mapType = GoogleMap.MAP_TYPE_NORMAL
            // it.mapType = GoogleMap.MAP_TYPE_TERRAIN
            // it.mapType = GoogleMap.MAP_TYPE_SATELLITE
            // it.mapType = GoogleMap.MAP_TYPE_HYBRID


            // 위치 정보 관리하는 객체 가져오기
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            // 권한 허용 여부 확인
            // 허용 : PERMISSION_GRANTED
            // 허용 X : PERMISSION_DENIED
            val a1 = ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
            val a2 = ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)

            if(a1 == PackageManager.PERMISSION_GRANTED && a2 == PackageManager.PERMISSION_GRANTED){
                // 현재 저장되어 있는 위치 정보값 가져오기
                val location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val location2 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if(location1 != null){
                    setMyLocation(location1)
                } else if(location2 != null){
                    setMyLocation(location2)
                }

                // 현재 위치 측정하여 지도 갱신하는 함수
                getMyLocation()
            }
        }
    }

    // 현재 위치의 위도 경도값을 통해 구글 지도를 해당 위치로 이동
    fun setMyLocation(location: Location){

        // 측정된 사용자의 현재 위치를 담아준다.
        userLocation = location

        // 위치 측정 중단
        if(myLocationListener != null) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager.removeUpdates(myLocationListener!!)
            myLocationListener = null
        }

        // 위도 & 경도 관리 객체
        val latLng = LatLng(location.latitude, location.longitude)

        // 지도를 이용시키기 위한 객체
        //val cameraUpdate = CameraUpdateFactory.newLatLng(latLng)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)

        // 지도 이동
        // mainGoogleMap.moveCamera(cameraUpdate)
        // 애니메이션 적용
        mainGoogleMap.animateCamera(cameraUpdate)

        // 현재 위치에 마커 표시
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)

        // 마커 이미지 변경
        val markerBitmap = BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation)
        markerOptions.icon(markerBitmap)

        // 기존에 표시한 마커를 제거한다.
        if(myMarker != null){
            myMarker?.remove()
            myMarker = null
        }
        mainGoogleMap.addMarker(markerOptions)

        Toast.makeText(this@MainActivity, "위도 : ${location.latitude}, 경도 : ${location.longitude}", Toast.LENGTH_SHORT).show()

    }

    fun getMyLocation(){
        val a1 = ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        val a2 = ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        if(a1 == PackageManager.PERMISSION_GRANTED && a2 == PackageManager.PERMISSION_GRANTED){

            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            // 위치 측정 리스너
            myLocationListener = object : LocationListener {
                override fun onLocationChanged(p0: Location) {
                    setMyLocation(p0)
                }
            }

            // 위치 측정 요청
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    0, 0f, myLocationListener!!)
            }

//            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
//                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
//                    0, 0f, myLocationListener!!)
//            }
        }
    }
}

