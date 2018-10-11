package com.tcqq.placepicker.activity

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
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
import com.github.florent37.expectanim.ExpectAnim
import com.github.florent37.expectanim.core.Expectations.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding2.view.RxView
import com.tcqq.placepicker.items.NearbyPlacesHeaderItem
import com.tcqq.placepicker.items.NearbyPlacesItem
import com.tcqq.placepicker.R
import com.tcqq.placepicker.enums.DebounceTime
import com.tcqq.placepicker.utils.*
import com.tcqq.placepicker.viewmodel.PlacePickerViewModel
import com.trello.rxlifecycle2.android.ActivityEvent
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
 * @since 17/09/2018 Created
 */
class PlacePickerActivity : BaseActivity(),
        FlexibleAdapter.OnItemClickListener {

    private var adapter: FlexibleAdapter<IFlexible<*>>? = null
    private var items = ArrayList<IFlexible<*>>()

    private var showSearchBarAnim: ExpectAnim? = null
    private var hideSearchBarAnim: ExpectAnim? = null
    private var isShowSearchBarAnim: Boolean = false
    private var isHideSearchBarAnim: Boolean = false

    private var showAppBarLayoutAnim: ExpectAnim? = null
    private var hideAppBarLayoutAnim: ExpectAnim? = null
    private var isShowAppBarLayoutAnim: Boolean = false
    private var isHideAppBarLayoutAnim: Boolean = false

    private var behavior: BottomSheetBehavior<FrameLayout>? = null
    private var fullScreenForXiaoMi: Boolean = false

    private lateinit var aMap: AMap
    private var locationClient: AMapLocationClient? = null
    private var locationOption: AMapLocationClientOption? = null
    private var location: AMapLocation? = null
    private var locationGpsMarker: Marker? = null
    private var locationChanged = false
    private var geocodeSearch: GeocodeSearch? = null

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
        stopLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Anim & Widget
        showSearchBarAnim = null
        hideSearchBarAnim = null
        showAppBarLayoutAnim = null
        hideAppBarLayoutAnim = null
        if (behavior != null) {
            behavior!!.setBottomSheetCallback(null)
            behavior = null
        }
        if (adapter != null) {
            adapter!!.mItemClickListener = null
            adapter = null
        }
        //Map
        stopLocation()
        map_view.onDestroy()
        if (locationClient != null) {
            locationClient!!.onDestroy()
            locationClient = null
            locationOption = null
        }
        if (geocodeSearch != null) {
            geocodeSearch = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> Toast.makeText(this, "autocomplete", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (behavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            behavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
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

        if (ScreenUtils.isPortrait(this)) {
            val model = ViewModelProviders.of(this).get(PlacePickerViewModel::class.java)
            if (RomUtils.isMiuiRom) {
                Timber.d("Is Xiaomi")
                Toast.makeText(this, "Is Xiaomi", Toast.LENGTH_SHORT).show()
                val fullScreen = Settings.Global.getInt(contentResolver, "force_fsg_nav_bar", 0) != 0
                Timber.d("Full screen: $fullScreen")
                fullScreenForXiaoMi = fullScreen
                model.peekHeight.value = fullScreen.let {
                    if (it) {
                        AutoUtils.getDisplayHeightValue(464) + BarUtils.getNavBarHeight()
                    } else {
                        AutoUtils.getDisplayHeightValue(464)
                    }
                }
            } else {
                Timber.d("Not Xiaomi")
                Toast.makeText(this, "Not Xiaomi", Toast.LENGTH_SHORT).show()
                model.peekHeight.value = AutoUtils.getDisplayHeightValue(464)
            }

            model.peekHeight.observe(this, androidx.lifecycle.Observer<Int> {
                behavior!!.peekHeight = it
            })

            if (BarUtils.hasTransparentStatusBar()) {
                val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        AutoUtils.getDisplayHeightValue(157))
                lp.setMargins(AutoUtils.getDisplayWidthValue(14),
                        BarUtils.getStatusBarHeight() + AutoUtils.getDisplayHeightValue(12),
                        AutoUtils.getDisplayWidthValue(14),
                        0)
                search_bar!!.layoutParams = lp
            }
            RxView
                    .clicks(search_bar!!)
                    .throttleFirst(DebounceTime.CLICK_SECONDS.time, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribe {
                        Toast.makeText(this, "autocomplete", Toast.LENGTH_SHORT).show()
                    }.isDisposed
        } else {
            behavior!!.peekHeight = AutoUtils.getDisplayHeightValue(452)
        }

        val publishSubject: PublishSubject<Int> = PublishSubject.create()

        behavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
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
            status_bar.layoutParams.height = BarUtils.getStatusBarHeight()
        }

        val appBarLayoutLayoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT)
        appBarLayoutLayoutParams.setMargins(0,
                -(status_bar.layoutParams.height + getToolBarHeight()),
                0,
                0)
        app_bar_layout.layoutParams = appBarLayoutLayoutParams

        RxView
                .clicks(floating_action_button)
                .throttleFirst(DebounceTime.CLICK_SECONDS.time, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    locationChanged = false
                    if (location == null) {
                        startLocation()
                    } else {
                        locationSuccessTask()
                    }
                }.isDisposed
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            floating_action_button.setColorFilter(Color.parseColor("#757575"))
        }
    }

    private fun initRecyclerView() {
        adapter = FlexibleAdapter(null, this, true)
        recycler_view.layoutManager = SmoothScrollLinearLayoutManager(this)
        recycler_view.adapter = adapter
        recycler_view.setHasFixedSize(true)
        adapter!!.addScrollableHeader(NearbyPlacesHeaderItem("SHI"))
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        return false
    }

    private fun getSlideOffsetObservable(slideOffset: Int): Observable<Int> {
        return Observable.create(ObservableOnSubscribe<Int> {
            Timber.d("slideOffset: $slideOffset")
            it.onNext(slideOffset)
            it.onComplete()
        }).subscribeOn(Schedulers.io())
    }

    /* ====
     * AMap
     * ==== */

    private fun initMapView(savedInstanceState: Bundle?) {
        map_view.onCreate(savedInstanceState)
        aMap = map_view.map
        val uiSettings: UiSettings = aMap.uiSettings
        uiSettings.isZoomControlsEnabled = false//是否显示地图中放大缩小按钮
        uiSettings.isMyLocationButtonEnabled = false//是否显示默认的定位按钮
        uiSettings.isScaleControlsEnabled = false//是否显示缩放级别
        aMap.isMyLocationEnabled = false//是否可触发定位并显示定位层
        aMap.setMapLanguage(AMap.CHINESE)
        aMap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChangeFinish(cameraPosition: CameraPosition?) {
                Timber.v("onCameraChangeFinish")
//                setLoadingStatus(true)
                if (location != null && cameraPosition != null) {
                    queryNearbyPlaces(cameraPosition.target.latitude, cameraPosition.target.longitude)
                } else {
                    Timber.e("location or cameraPosition is empty")
                }
            }

            override fun onCameraChange(cameraPosition: CameraPosition?) {
                Timber.v("onCameraChange")
            }
        })
    }

    private fun initLocation() {
        //初始化client
        locationClient = AMapLocationClient(applicationContext)
        locationOption = getDefaultOption()
        //设置定位参数
        locationClient!!.setLocationOption(locationOption)
        //设置定位监听
        val publishSubject: PublishSubject<AMapLocation> = PublishSubject.create()
        locationClient!!.setLocationListener {
            publishSubject.onNext(it)
        }
        publishSubject
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    if (it.errorCode == 0) {//可在其中解析AMapLocation获取相应内容。
                        location = it
                        if (!locationChanged) {
                            locationSuccessTask()
                        }
                    } else {
                        Timber.e("Location error, Error code: ${it.errorCode}, Error info: ${it.errorInfo}")
                    }
                }.isDisposed
        startLocation()
    }

    private fun startLocation() {
        locationClient?.startLocation()
    }

    private fun stopLocation() {
        locationClient?.stopLocation()
    }

    /**
     * 定位成功后将执行的任务
     */
    private fun locationSuccessTask() {
        moveMapCamera(location!!.latitude, location!!.longitude)
        refreshLocationMark(location!!.latitude, location!!.longitude)
    }

    private fun moveMapCamera(latitude: Double, longitude: Double) {
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16F))
    }

    private fun refreshLocationMark(latitude: Double, longitude: Double) {
        if (locationGpsMarker == null) {
            locationGpsMarker = aMap.addMarker(MarkerOptions()
                    .position(LatLng(latitude, longitude))
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                            .decodeResource(resources, R.drawable.ic_bluedot)))
                    .draggable(true))
        }
        locationGpsMarker!!.position = LatLng(latitude, longitude)
    }

    private fun getDefaultOption(): AMapLocationClientOption {
        val option = AMapLocationClientOption()
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        option.isGpsFirst = false//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        option.httpTimeOut = 30000//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        option.interval = 2000//可选，设置定位间隔。默认为2秒
        option.isNeedAddress = true//可选，设置是否返回逆地理地址信息。默认是true
        option.isOnceLocation = false//可选，设置是否单次定位。默认是false
        option.isOnceLocationLatest = false//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP)//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        option.isSensorEnable = false//可选，设置是否使用传感器。默认是false
        option.isWifiScan = true //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        option.isMockEnable = true//如果您希望位置被模拟，请通过setMockEnable(true);方法开启允许位置模拟
        return option
    }

    private fun initGeocodeSearch() {
        geocodeSearch = GeocodeSearch(this)
        geocodeSearch!!.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
            override fun onRegeocodeSearched(result: RegeocodeResult?, resultCode: Int) {
                locationChanged = true
//                setLoadingStatus(false)
                if (resultCode == 1000) {
                    if (result != null) {
                        val poiItem = result.regeocodeAddress.pois
                        Timber.d("onRegeocodeSearched result: $poiItem")
                        items.clear()
                        for (index in poiItem.indices) {
                            items.add(NearbyPlacesItem(index.toString(), poiItem[index].title))
                        }
                        adapter!!.updateDataSet(items)
                    }
                } else {
                    Timber.e("Search error. Please check the error code: https://lbs.amap.com/api/android-sdk/guide/map-tools/error-code")
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
        geocodeSearch!!.getFromLocationAsyn(query)
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
                        if (hideAppBarLayoutAnim!!.isPlaying && visible) {
                            Timber.d("Showing appBarLayout - onSubscribe")
                            isShowAppBarLayoutAnim = true
                            d.dispose()
                        } else if (showAppBarLayoutAnim!!.isPlaying && !visible) {
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
                        if (hideSearchBarAnim!!.isPlaying && visible) {
                            Timber.d("Showing searchBar - onSubscribe")
                            isShowSearchBarAnim = true
                            d.dispose()
                        } else if (showSearchBarAnim!!.isPlaying && !visible) {
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
                                (BarUtils.getStatusBarHeight() + AutoUtils.getDisplayHeightValue(12)).toFloat()).toFloat()),
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
                                -(BarUtils.getStatusBarHeight() + AutoUtils.getDisplayHeightValue(12 + 157)).toFloat()).toFloat()),
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