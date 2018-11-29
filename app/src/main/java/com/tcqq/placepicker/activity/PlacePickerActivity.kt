package com.tcqq.placepicker.activity

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.UiSettings
import com.amap.api.maps.model.*
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.tcqq.placepicker.R
import com.tcqq.placepicker.items.NearbyPlacesHeaderItem
import com.tcqq.placepicker.items.NearbyPlacesItem
import com.tcqq.placepicker.enums.MapDimension
import com.tcqq.placepicker.model.LimitTime
import com.tcqq.placepicker.model.SelectedLocation
import com.tcqq.placepicker.utils.*
import com.tcqq.placepicker.viewmodel.PlacePickerViewModel
import com.github.florent37.expectanim.ExpectAnim
import com.github.florent37.expectanim.core.Expectations.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle3.android.ActivityEvent
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.IFlexible
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_place_picker.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * @author Alan Dreamer
 * @since 2018/09/17 Created
 */
class PlacePickerActivity : BaseActivity(),
        FlexibleAdapter.OnItemClickListener,
        AMap.OnCameraChangeListener,
        DeviceCompass.OnOrientationChangedEventListener {

    private lateinit var adapter: FlexibleAdapter<IFlexible<*>>
    private var items: ArrayList<IFlexible<*>> = arrayListOf()

    private lateinit var showSearchBarAnim: ExpectAnim
    private lateinit var hideSearchBarAnim: ExpectAnim
    private var isShowSearchBarAnim: Boolean = false
    private var isHideSearchBarAnim: Boolean = false

    private lateinit var showAppBarLayoutAnim: ExpectAnim
    private lateinit var hideAppBarLayoutAnim: ExpectAnim
    private var isShowAppBarLayoutAnim: Boolean = false
    private var isHideAppBarLayoutAnim: Boolean = false

    private lateinit var showScaleViewAnim: ExpectAnim
    private lateinit var hideScaleViewAnim: ExpectAnim
    private var isShowScaleViewAnim: Boolean = false
    private var isHideScaleViewAnim: Boolean = false

    private lateinit var behavior: BottomSheetBehavior<FrameLayout>
    private var fullScreenForXiaoMi: Boolean = false

    private lateinit var aMap: AMap
    private lateinit var uiSettings: UiSettings

    private val myLocationStyle by lazy {
        MyLocationStyle()
    }

    private var markers: List<Marker>? = null
    private lateinit var locationClient: AMapLocationClient
    private lateinit var locationOption: AMapLocationClientOption
    private lateinit var circle: Circle
    private lateinit var geocodeSearch: GeocodeSearch

    private var location: AMapLocation? = null
    private var locationLocked = true
    private var zoom = 16F
    private var mapDimension: MapDimension = MapDimension.TWO_DIMENSIONAL
    private var cameraPosition: CameraPosition? = null

    private var currentPlaceName = ""
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    private lateinit var cameraChangePublishSubject: PublishSubject<CameraPosition>
    private lateinit var cameraChangeFinishPublishSubject: PublishSubject<CameraPosition>
    private lateinit var cameraChangeFinishFixPublishSubject: PublishSubject<CameraPosition>

    private lateinit var deviceCompass: DeviceCompass
    private lateinit var azimuthPublishSubject: PublishSubject<Float>

    companion object {
/*        private const val STATE_PLACE_NAME = "place_name"
        private const val STATE_LATITUDE = "latitude"
        private const val STATE_LONGITUDE = "longitude"*/

        const val EXTRA_SELECTED_LOCATION = "selected_location"

        private const val REQUEST_AUTOCOMPLETE = 10
    }

    override fun onSaveInstanceState(outState: Bundle?) {
/*        outState?.run {
            putDouble(STATE_LATITUDE, latitude)
            putDouble(STATE_LONGITUDE, longitude)
            putString(STATE_PLACE_NAME, currentPlaceName)
        }*/
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
/*        if (savedInstanceState != null) {
            with(savedInstanceState) {
                latitude = getDouble(STATE_LATITUDE)
                longitude = getDouble(STATE_LONGITUDE)
                currentPlaceName = getString(STATE_PLACE_NAME)!!
            }
        }*/
        AutoUtils.setSize(this, !BarUtils.hasTransparentStatusBar(), 1080, 1920)
        setContentView(R.layout.activity_place_picker)
        BarUtils.transparentStatusBar(this)
        AutoUtils.auto(this)
        setActionBar(toolbar)
        setActionBarTitle(R.string.choose_a_nearby_place)
        setMenuFromResource(R.menu.menu_place_picker)
        initView()
        initMapView(savedInstanceState)
        initGeocodeSearch()
        initLocation()
        initRecyclerView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_AUTOCOMPLETE) {
            data?.getParcelableExtra<SelectedLocation>(EXTRA_SELECTED_LOCATION)?.apply {
                setSelectedLocation(placeName, latitude, longitude)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onResume() {
        super.onResume()
        if (RomUtils.isMiuiRom) {
            val fullScreen = Settings.Global.getInt(contentResolver, "force_fsg_nav_bar", 0) != 0
            if (fullScreenForXiaoMi != fullScreen) {
                fullScreenForXiaoMi = fullScreen
                ViewModelProviders.of(this).get(PlacePickerViewModel::class.java).peekHeight.value = fullScreen.let {
                    if (it) AutoUtils.getDisplayHeightValue(464) + BarUtils.getNavBarHeight()
                    else AutoUtils.getDisplayHeightValue(464)
                }
            }
        }
        map_view.onResume()
        startLocation()
        deviceCompass.startListening()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
        stopLocation()
        deviceCompass.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocation()
        map_view.onDestroy()
        locationClient.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> openAutocomplete()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }
        super.onBackPressed()
    }

    private fun initView() {
        behavior = BottomSheetBehavior.from(bottom_sheet)
        showSearchBarAnim = ExpectAnim()
        hideSearchBarAnim = ExpectAnim()
        showAppBarLayoutAnim = ExpectAnim()
        hideAppBarLayoutAnim = ExpectAnim()
        showScaleViewAnim = ExpectAnim()
        hideScaleViewAnim = ExpectAnim()

        if (ScreenUtils.isPortrait(this)) {
            val model = ViewModelProviders.of(this).get(PlacePickerViewModel::class.java)
            if (RomUtils.isMiuiRom) {
                val fullScreen = Settings.Global.getInt(contentResolver, "force_fsg_nav_bar", 0) != 0
                Timber.d("Xiaomi > Full screen: $fullScreen")
                fullScreenForXiaoMi = fullScreen
                model.peekHeight.value = fullScreen.let {
                    if (it) {
                        AutoUtils.getDisplayHeightValue(464) + BarUtils.getNavBarHeight()
                    } else {
                        AutoUtils.getDisplayHeightValue(464)
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val decorView = window.decorView
                    decorView.post {
                        val displayCutout = decorView.rootWindowInsets.displayCutout
                        val rect = displayCutout?.boundingRects
                        if (rect?.isEmpty() != false) {
                            Timber.d("Android P > Not cutout")
                            model.peekHeight.value = AutoUtils.getDisplayHeightValue(464)
                        } else {
                            Timber.d("Android P > Has cutout")
                            model.peekHeight.value = AutoUtils.getDisplayHeightValue(464) + BarUtils.getNavBarHeight()
                        }
                    }
                } else {
                    Timber.d("Not cutout")
                    model.peekHeight.value = AutoUtils.getDisplayHeightValue(464)
                }
            }

            model.peekHeight.observe(this, androidx.lifecycle.Observer<Int> {
                behavior.peekHeight = it
            })

            if (BarUtils.hasTransparentStatusBar()) {
                val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        AutoUtils.getDisplayHeightValue(157))
                lp.setMargins(AutoUtils.getDisplayWidthValue(14),
                        BarUtils.getStatusBarHeight(this) + AutoUtils.getDisplayHeightValue(12),
                        AutoUtils.getDisplayWidthValue(14),
                        0)
                search_bar!!.layoutParams = lp
            }
            RxView
                    .clicks(search_bar!!)
                    .throttleFirst(LimitTime.CLICK_THROTTLE_SECONDS, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribe {
                        openAutocomplete()
                    }.isDisposed
        } else {
            behavior.peekHeight = AutoUtils.getDisplayHeightValue(452)
        }

        val publishSubject: PublishSubject<Int> = PublishSubject.create()

        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, newState: Float) {
            }

            override fun onStateChanged(bottomSheet: View, slideOffset: Int) {
                publishSubject.onNext(slideOffset)
            }
        })

        publishSubject
                .switchMap {
                    return@switchMap getSlideOffsetObservable(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    when (it) {
                        BottomSheetBehavior.STATE_DRAGGING -> setSearchBarVisibility(false)
                        BottomSheetBehavior.STATE_SETTLING -> {
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> setAppBarLayoutVisibility(true)
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            setSearchBarVisibility(true)
                            setAppBarLayoutVisibility(false)
                        }
                        BottomSheetBehavior.STATE_HIDDEN -> {
                        }
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        }
                    }
                }.isDisposed

        if (BarUtils.hasTransparentStatusBar()) {
            status_bar.layoutParams.height = BarUtils.getStatusBarHeight(this)
        }

        val appBarLayoutLayoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT)
        appBarLayoutLayoutParams.setMargins(0,
                -(status_bar.layoutParams.height + getToolBarHeight()),
                0,
                0)
        app_bar_layout.layoutParams = appBarLayoutLayoutParams

        floating_action_button.setOnClickListener {
            when (mapDimension) {
                MapDimension.TWO_DIMENSIONAL -> {
                    if (locationLocked) {
                        zoom = 18F
                        mapDimension = MapDimension.THREE_DIMENSIONAL
                        floating_action_button.setImageResource(R.drawable.ic_explore_black_24dp)
                        aMap.myLocationStyle = myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE_NO_CENTER)
                    }
                }
                MapDimension.THREE_DIMENSIONAL -> {
                    if (locationLocked) {
                        zoom = 16F
                        mapDimension = MapDimension.TWO_DIMENSIONAL
                        floating_action_button.setImageResource(R.drawable.ic_my_location_black_24dp)
                        aMap.myLocationStyle = myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
                    }
                }
            }

            setLocationLock(true)

            if (location == null) {
                startLocation()
            } else {
                resetLocationMarker(location!!)
                moveMapCamera(location!!)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            floating_action_button.setColorFilter(ThemeUtils.getThemeValue(R.attr.colorSecondary, this))
        }

        select_marker_location.setOnClickListener {
            setSelectedLocation(currentPlaceName, currentLatitude, currentLongitude)
        }
    }

    private fun setSelectedLocation(placeName: String,
                                    latitude: Double,
                                    longitude: Double) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_SELECTED_LOCATION, SelectedLocation(placeName, latitude, longitude))
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        Timber.d("onItemClick > position: $position")
        if (position != 0) {
            adapter.getItem(position, NearbyPlacesItem::class.java)!!.apply {
                setSelectedLocation(placeName, latitude, longitude)
            }
        }
        return false
    }

    private fun initRecyclerView() {
        adapter = FlexibleAdapter(null, this, true)
        recycler_view.layoutManager = SmoothScrollLinearLayoutManager(this)
        recycler_view.adapter = adapter
        recycler_view.setHasFixedSize(true)
        adapter.addScrollableHeader(NearbyPlacesHeaderItem("SHI"))
    }

    private fun getSlideOffsetObservable(slideOffset: Int): Observable<Int> {
        return Observable.create(ObservableOnSubscribe<Int> {
            Timber.d("slideOffset: $slideOffset")
            it.onNext(slideOffset)
        }).subscribeOn(Schedulers.io())
    }

    /* ====
     * AMap
     * ==== */

    override fun onCameraChangeFinish(cameraPosition: CameraPosition) {
        Timber.d("onCameraChangeFinish > cameraPosition: $cameraPosition")
        val location = Location("")//provider name is unnecessarily
        cameraPosition.target.apply {
            location.latitude = latitude
            location.longitude = latitude
            currentLatitude = latitude
            currentLongitude = longitude
        }
        val coordinate = ConvertUtils.latLng2DMS(location)
        Timber.d("latLng2DMS: $coordinate")
        currentPlaceName = coordinate
    }

    //FIXME: Using 3D maps will enter an infinite loop
    override fun onCameraChange(cameraPosition: CameraPosition) {
        Timber.v("onCameraChange > cameraPosition: $cameraPosition")
        cameraChangePublishSubject.onNext(cameraPosition)
    }

    override fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float) {
        azimuthPublishSubject.onNext(azimuth)
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        map_view.onCreate(savedInstanceState)
        aMap = map_view.map
        aMap.isMyLocationEnabled = false//是否可触发定位并显示定位层
        aMap.setMapLanguage(AMap.CHINESE)
        aMap.myLocationStyle = myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
        aMap.setOnCameraChangeListener(this)
        aMap.setAMapGestureListener(object : AMapGestureListener {
            //单指按下
            override fun onDown(x: Float, y: Float) {
                Timber.v("AMapGestureListener > onDown")
            }

            //单指双击
            override fun onDoubleTap(x: Float, y: Float) {
                Timber.v("AMapGestureListener > onDoubleTap")
            }

            //单指惯性滑动
            override fun onFling(x: Float, y: Float) {
                Timber.v("AMapGestureListener > onFling")
            }

            //单指单击
            override fun onSingleTap(x: Float, y: Float) {
                Timber.v("AMapGestureListener > onSingleTap")
            }

            //单指滑动
            override fun onScroll(x: Float, y: Float) {
                setLocationLock(false)
                Timber.v("AMapGestureListener > onScroll")
            }

            //地图稳定下来会回到此接口
            override fun onMapStable() {
                Timber.v("AMapGestureListener > onMapStable")
            }

            //单指抬起
            override fun onUp(x: Float, y: Float) {
                Timber.v("AMapGestureListener > onUp")
            }

            //长按
            override fun onLongPress(x: Float, y: Float) {
                Timber.v("AMapGestureListener > onLongPress")
            }
        })

        uiSettings = aMap.uiSettings
        uiSettings.isZoomControlsEnabled = false//是否显示地图中放大缩小按钮
        uiSettings.isMyLocationButtonEnabled = false//是否显示默认的定位按钮
        uiSettings.isScaleControlsEnabled = true//是否显示缩放级别
        uiSettings.isScaleControlsEnabled = false//是否可见比例尺控件
        uiSettings.setZoomInByScreenCenter(true)
        if (ScreenUtils.isPortrait(this)) {
            uiSettings.setLogoBottomMargin(AutoUtils.getDisplayHeightValue(45))
            uiSettings.setLogoLeftMargin(AutoUtils.getDisplayHeightValue(42))
        } else {
            uiSettings.setLogoBottomMargin(AutoUtils.getDisplayHeightValue(75))
            uiSettings.setLogoLeftMargin(AutoUtils.getDisplayHeightValue(25))
        }

        cameraChangePublishSubject = PublishSubject.create()
        cameraChangePublishSubject
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .doOnNext {
                    Timber.d("cameraChangePublishSubject#doOnNext > $it")
                    when (mapDimension) {
                        MapDimension.TWO_DIMENSIONAL -> cameraChangeFinishPublishSubject.onNext(it)
                        MapDimension.THREE_DIMENSIONAL -> cameraChangeFinishFixPublishSubject.onNext(it)
                    }
                }
                .filter {
                    zoom != it.zoom
                }
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.d("cameraChangePublishSubject#onNext > show scaleView")
                    zoom = it.zoom
                    scale_view.update(it.zoom, it.target.latitude)
                    setScaleViewVisibility(true)
                }.isDisposed

        cameraChangeFinishPublishSubject = PublishSubject.create()
        cameraChangeFinishPublishSubject
                .debounce(300, TimeUnit.MILLISECONDS)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    showProgress(true)
                }
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .doOnNext {
                    Timber.d("cameraChangePublishSubject#cameraChangeFinishPublishSubject")
                    queryNearbyPlaces(it.target.latitude, it.target.longitude)
                }
                .debounce(2700, TimeUnit.MILLISECONDS)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.d("cameraChangePublishSubject#cameraChangeFinishFixPublishSubject > hide scaleView")
                    setScaleViewVisibility(false)
                }.isDisposed

        //Fix maps switched from 2D to 3D cause problem. https://lbs.amap.com/dev/ticket/list/opened
        cameraChangeFinishFixPublishSubject = PublishSubject.create()
        cameraChangeFinishFixPublishSubject
                .throttleFirst(1, TimeUnit.SECONDS)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .filter {
                    cameraPosition != it
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    showProgress(true)
                }
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .doOnNext {
                    Timber.d("cameraChangePublishSubject#cameraChangeFinishFixPublishSubject > $it")
                    cameraPosition = it
                    queryNearbyPlaces(it.target.latitude, it.target.longitude)
                }
                .debounce(3, TimeUnit.SECONDS)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.d("cameraChangePublishSubject#cameraChangeFinishFixPublishSubject > hide scaleView")
                    setScaleViewVisibility(false)
                }.isDisposed

        deviceCompass = DeviceCompass(applicationContext).apply {
            setOnOrientationChangedEventListener(this@PlacePickerActivity)
        }

        azimuthPublishSubject = PublishSubject.create()
        azimuthPublishSubject
                .filter {
                    if (markers != null) return@filter markers!!.size == 2
                    else return@filter false
                }
                .map {
                    360 - it
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    Timber.v("onOrientationChanged > rotateAngle: $it")
                    markers!![1].rotateAngle = it
                }.isDisposed

        scale_view.setExpandRtlEnabled(!SystemUtils.isRtl(this))
    }

    private fun showProgress(show: Boolean) {
        val viewHolder = recycler_view.findViewHolderForAdapterPosition(0) as NearbyPlacesHeaderItem.ViewHolder?
        viewHolder?.progressbar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun initLocation() {
        //初始化client
        locationClient = AMapLocationClient(applicationContext)
        locationOption = getDefaultOption()
        //设置定位参数
        locationClient.setLocationOption(locationOption)
        //设置定位监听
        val publishSubject: PublishSubject<AMapLocation> = PublishSubject.create()
        locationClient.setLocationListener {
            publishSubject.onNext(it)
        }
        publishSubject
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    if (it.errorCode == 0) {//可在其中解析AMapLocation获取相应内容。
                        Timber.v("onLocationChanged")
                        location = it
                        if (locationLocked) {
                            moveMapCamera(it)
                        }
                        resetLocationMarker(it)
                    } else {
                        Timber.e("SelectedLocation error, Error code: ${it.errorCode}, Error info: ${it.errorInfo}")
                    }
                }.isDisposed
        startLocation()
    }

    private fun startLocation() {
        locationClient.startLocation()
    }

    private fun stopLocation() {
        locationClient.stopLocation()
    }

    private fun moveMapCamera(location: AMapLocation) {
        val currentLocation = LatLng(location.latitude, location.longitude)
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoom))
    }

    private fun resetLocationMarker(location: AMapLocation) {
        val currentLocation = LatLng(location.latitude, location.longitude)
        val accuracy = location.accuracy
        if (markers == null) {
            markers = addMarker(currentLocation)
            circle = aMap.addCircle(CircleOptions().center(currentLocation)
                    .fillColor(Color.argb(50, 65, 135, 245))
                    .radius(accuracy.toDouble())
                    .strokeColor(Color.argb(255, 66, 133, 244))
                    .strokeWidth(0.5f))
        } else {
            for (index in markers!!.indices) {
                markers!![index].position = currentLocation
            }
            circle.apply {
                center = currentLocation
                radius = accuracy.toDouble()
            }
        }
    }

    private fun addMarker(point: LatLng): ArrayList<Marker> {
        val blueDotBitmap = BitmapFactory.decodeResource(resources,
                R.drawable.new_blue_dot)
        val blueDotIcon = BitmapDescriptorFactory.fromBitmap(blueDotBitmap)
        val blueDotMarkerOption = MarkerOptions()
                .position(point)
                .icon(blueDotIcon)
                .anchor(0.5F, 0.5F)
                .setFlat(true)

        val blueConeBitmap = BitmapFactory.decodeResource(resources,
                R.drawable.blue_cone_150)
        val blueConeIcon = BitmapDescriptorFactory.fromBitmap(blueConeBitmap)
        val blueConeMarkerOption = MarkerOptions()
                .position(point)
                .icon(blueConeIcon)
                .anchor(0.5F, 0.5F)
                .setFlat(true)

        val markerOptions = arrayListOf<MarkerOptions>()
        markerOptions.add(blueDotMarkerOption)
        markerOptions.add(blueConeMarkerOption)
        return aMap.addMarkers(markerOptions, false)
    }

    private fun getDefaultOption(): AMapLocationClientOption {
        return AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
            isGpsFirst = false//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
            httpTimeOut = 30000//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
            interval = 2000//可选，设置定位间隔。默认为2秒
            isNeedAddress = true//可选，设置是否返回逆地理地址信息。默认是true
            isOnceLocation = false//可选，设置是否单次定位。默认是false
            isOnceLocationLatest = false//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
            isSensorEnable = false//可选，设置是否使用传感器。默认是false
            isWifiScan = true //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
            isMockEnable = true//如果您希望位置被模拟，请通过setMockEnable(true);方法开启允许位置模拟
        }
    }

    private fun initGeocodeSearch() {
        geocodeSearch = GeocodeSearch(this)
        geocodeSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
            override fun onRegeocodeSearched(result: RegeocodeResult?, resultCode: Int) {
                showProgress(false)
                if (resultCode == 1000) {
                    if (result != null) {
                        val poiItem = result.regeocodeAddress.pois
                        Timber.d("onRegeocodeSearched > poiItem: $poiItem")
                        items.clear()
                        for (index in poiItem.indices) {
                            with(poiItem[index]) {
                                items.add(NearbyPlacesItem(
                                        index.toString(),
                                        title,
                                        snippet,
                                        latLonPoint.latitude,
                                        latLonPoint.longitude))
                            }
                        }
                        adapter.updateDataSet(items)
                    }
                } else {
                    Timber.e("Error code: $resultCode, Error info: Please check the error code: https://lbs.amap.com/api/android-sdk/guide/map-tools/error-code")
                }
            }

            override fun onGeocodeSearched(result: GeocodeResult?, resultCode: Int) {

            }
        })
    }

    /**
     * 通过经纬度获取当前地址详细信息，逆地址编码
     */
    private fun queryNearbyPlaces(latitude: Double, longitude: Double) {
        /*
         * point - 要进行逆地理编码的地理坐标点。
         * radius - 查找范围。默认值为1000，取值范围1-3000，单位米。
         * latLonType - 输入参数坐标类型。包含GPS坐标和高德坐标。 可以参考RegeocodeQuery.setLatLonType(String)
         */
        val query = RegeocodeQuery(LatLonPoint(latitude, longitude), 1000f, GeocodeSearch.AMAP)
        geocodeSearch.getFromLocationAsyn(query)
    }

    private fun setLocationLock(locked: Boolean) {
        if (locationLocked != locked) {
            locationLocked = locked
            if (locked) {
                floating_action_button.setColorFilter(ThemeUtils.getThemeValue(R.attr.colorSecondary, this))
            } else {
                floating_action_button.setColorFilter(Color.parseColor("#757575"))
            }
        }
    }

    private fun openAutocomplete() {
        val intent = Intent(this, AutocompleteActivity::class.java)
        startActivityForResult(intent, REQUEST_AUTOCOMPLETE)
    }

    /* =========
     * ScaleView
     * ========= */

    private fun setScaleViewVisibility(visible: Boolean) {
        Observable
                .just(visible)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(object : Observer<Boolean> {
                    override fun onSubscribe(d: Disposable) {
                        if (hideScaleViewAnim.isPlaying && visible) {
                            Timber.d("Showing scaleView")
                            isHideScaleViewAnim = true
                            d.dispose()
                        }
                    }

                    override fun onNext(aBoolean: Boolean) {
                        if (aBoolean) {
                            Timber.d("Showing scaleView")
                            showScaleViewAnim()
                        } else {
                            Timber.d("Hiding scaleView")
                            hideScaleViewAnim()
                        }
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e.localizedMessage)
                    }

                    override fun onComplete() {

                    }
                })
    }

    private fun showScaleViewAnim() {
        ExpectAnim()
                .expect(scale_view)
                .toBe(
                        visible()
                )
                .toAnimation()
                .setDuration(0)
                .addStartListener {
                    isShowScaleViewAnim = false
                }
                .addEndListener {
                    if (isShowScaleViewAnim) {
                        hideScaleViewAnim()
                    }
                }
                .start()
    }

    private fun hideScaleViewAnim() {
        hideScaleViewAnim = ExpectAnim()
                .expect(scale_view)
                .toBe(
                        invisible()
                )
                .toAnimation()
                .setDuration(250)
                .addStartListener {
                    isHideScaleViewAnim = false
                }
                .addEndListener {
                    if (isHideScaleViewAnim) showScaleViewAnim()
                }
                .start()
    }

    /* ============
     * AppBarLayout
     * ============ */

    private fun setAppBarLayoutVisibility(visible: Boolean) {
        Observable
                .just(visible)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(object : Observer<Boolean> {
                    override fun onSubscribe(d: Disposable) {
                        if (hideAppBarLayoutAnim.isPlaying && visible) {
                            Timber.d("Showing appBarLayout - onSubscribe")
                            isShowAppBarLayoutAnim = true
                            d.dispose()
                        } else if (showAppBarLayoutAnim.isPlaying && !visible) {
                            Timber.d("Hiding appBarLayout - onSubscribe")
                            isHideAppBarLayoutAnim = true
                            d.dispose()
                        }
                    }

                    override fun onNext(aBoolean: Boolean) {
                        if (aBoolean) {
                            Timber.d("Showing appBarLayout - onNext")
                            showAppBarLayoutAnim()
                        } else {
                            Timber.d("Hiding appBarLayout - onNext")
                            hideAppBarLayoutAnim()
                        }
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e.localizedMessage)
                    }

                    override fun onComplete() {

                    }
                })
    }

    private fun showAppBarLayoutAnim() {
        showAppBarLayoutAnim = ExpectAnim()
                .expect(app_bar_layout)
                .toBe(
                        topOfParent().withMarginDp(0F),
                        visible()
                )
                .toAnimation()
                .setDuration(250)
                .addStartListener {
                    isHideAppBarLayoutAnim = false
                }
                .addEndListener {
                    if (isHideAppBarLayoutAnim) hideAppBarLayoutAnim()
                }
                .start()
    }

    private fun hideAppBarLayoutAnim() {
        hideAppBarLayoutAnim = ExpectAnim()
                .expect(app_bar_layout)
                .toBe(
                        topOfParent().withMarginDp(ConvertUtils.px2dp(
                                this@PlacePickerActivity,
                                -(status_bar.layoutParams.height + getToolBarHeight()).toFloat()).toFloat()),
                        invisible()
                )
                .toAnimation()
                .setDuration(200)
                .addStartListener {
                    isShowAppBarLayoutAnim = false
                }
                .addEndListener {
                    if (isShowAppBarLayoutAnim) showAppBarLayoutAnim()
                }
                .start()
    }

    /* =========
     * SearchBar
     * ========= */

    private fun setSearchBarVisibility(visible: Boolean) {
        Observable
                .just(visible)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .doOnSubscribe { t ->
                    if (!ScreenUtils.isPortrait(this)) {
                        t.dispose()
                    }
                }
                .subscribe(object : Observer<Boolean> {
                    override fun onSubscribe(d: Disposable) {
                        if (hideSearchBarAnim.isPlaying && visible) {
                            Timber.d("Showing searchBar - onSubscribe")
                            isShowSearchBarAnim = true
                            d.dispose()
                        } else if (showSearchBarAnim.isPlaying && !visible) {
                            Timber.d("Hiding searchBar - onSubscribe")
                            isHideSearchBarAnim = true
                            d.dispose()
                        }
                    }

                    override fun onNext(aBoolean: Boolean) {
                        if (aBoolean) {
                            Timber.d("Showing searchBar - onNext")
                            showSearchBarAnim()
                        } else {
                            Timber.d("Hiding searchBar - onNext")
                            hideSearchBarAnim()
                        }
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e.localizedMessage)
                    }

                    override fun onComplete() {

                    }
                })
    }

    private fun showSearchBarAnim() {
        showSearchBarAnim = ExpectAnim()
                .expect(search_bar)
                .toBe(
                        topOfParent().withMarginDp(ConvertUtils.px2dp(
                                this@PlacePickerActivity,
                                (BarUtils.getStatusBarHeight(this) + AutoUtils.getDisplayHeightValue(12)).toFloat()).toFloat()),
                        visible()
                )
                .toAnimation()
                .setDuration(450)
                .addStartListener {
                    isHideSearchBarAnim = false
                }
                .addEndListener {
                    if (isHideSearchBarAnim) hideSearchBarAnim()
                }
                .start()
    }

    private fun hideSearchBarAnim() {
        hideSearchBarAnim = ExpectAnim()
                .expect(search_bar)
                .toBe(
                        topOfParent().withMarginDp(ConvertUtils.px2dp(
                                this@PlacePickerActivity,
                                -(BarUtils.getStatusBarHeight(this) + AutoUtils.getDisplayHeightValue(12 + 157)).toFloat()).toFloat()),
                        invisible()
                )
                .toAnimation()
                .setDuration(500)
                .addStartListener {
                    isShowSearchBarAnim = false
                }
                .addEndListener {
                    if (isShowSearchBarAnim) showSearchBarAnim()
                }
                .start()
    }

    private fun getToolBarHeight(): Int {
        val attrs = intArrayOf(R.attr.actionBarSize)
        val ta = obtainStyledAttributes(attrs)
        val toolBarHeight = ta.getDimensionPixelSize(0, -1)
        ta.recycle()
        return toolBarHeight
    }
}