package com.tcqq.placepicker

import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import com.github.florent37.expectanim.ExpectAnim
import com.github.florent37.expectanim.core.Expectations.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding2.view.RxView
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
class PlacePickerActivity : BaseActivity() {

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
    }

    override fun onDestroy() {
        super.onDestroy()
        showSearchBarAnim = null
        hideSearchBarAnim = null
        showAppBarLayoutAnim = null
        hideAppBarLayoutAnim = null
        if (behavior != null) {
            behavior!!.setBottomSheetCallback(null)
            behavior = null
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

        val lp = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0,
                -(status_bar.layoutParams.height + getToolBarHeight()),
                0,
                0)
        app_bar_layout.layoutParams = lp
    }

    private fun initRecyclerView() {
        val adapter: FlexibleAdapter<IFlexible<*>> = FlexibleAdapter(getNearbyPlacesItems(), this, true)
        recycler_view.layoutManager = SmoothScrollLinearLayoutManager(this)
        recycler_view.adapter = adapter
        recycler_view.setHasFixedSize(true)
        adapter.addScrollableHeader(NearbyPlacesHeaderItem("SHI"))
    }

    private fun getSlideOffsetObservable(slideOffset: Int): Observable<Int> {
        return Observable.create(ObservableOnSubscribe<Int> {
            Timber.d("slideOffset: $slideOffset")
            it.onNext(slideOffset)
            it.onComplete()
        }).subscribeOn(Schedulers.io())
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

    /* ============
     * Example data
     * ============ */
    private fun getNearbyPlacesItems(): List<IFlexible<*>> {
        val items = ArrayList<IFlexible<*>>()
        items.add(NearbyPlacesItem("1", "A"))
        items.add(NearbyPlacesItem("2", "B"))
        items.add(NearbyPlacesItem("3", "C"))
        items.add(NearbyPlacesItem("4", "D"))
        items.add(NearbyPlacesItem("5", "E"))
        items.add(NearbyPlacesItem("6", "F"))
        items.add(NearbyPlacesItem("7", "G"))
        items.add(NearbyPlacesItem("8", "H"))
        items.add(NearbyPlacesItem("9", "I"))
        items.add(NearbyPlacesItem("10", "J"))
        items.add(NearbyPlacesItem("11", "K"))
        items.add(NearbyPlacesItem("12", "L"))
        items.add(NearbyPlacesItem("13", "M"))
        items.add(NearbyPlacesItem("14", "N"))
        items.add(NearbyPlacesItem("15", "O"))
        return items
    }

    private fun getToolBarHeight(): Int {
        val attrs = intArrayOf(R.attr.actionBarSize)
        val ta = obtainStyledAttributes(attrs)
        val toolBarHeight = ta.getDimensionPixelSize(0, -1)
        ta.recycle()
        return toolBarHeight
    }
}