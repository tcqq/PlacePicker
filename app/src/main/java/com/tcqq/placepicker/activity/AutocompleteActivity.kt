package com.tcqq.placepicker.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.strategy.SocketInternetObservingStrategy
import com.jakewharton.rxbinding2.widget.RxTextView
import com.tcqq.placepicker.R
import com.tcqq.placepicker.enums.DebounceTime
import com.tcqq.placepicker.items.AutocompleteItem
import com.tcqq.placepicker.items.ProgressItem
import com.trello.rxlifecycle3.android.ActivityEvent
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.IFlexible
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_autocomplete.*
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * @author Alan Dreamer
 * @since 2018/09/17 Created
 */
class AutocompleteActivity : BaseActivity(),
        FlexibleAdapter.EndlessScrollListener,
        FlexibleAdapter.OnItemClickListener {

    private var poiSearch: PoiSearch? = null
    private var adapter: FlexibleAdapter<IFlexible<*>>? = null
    private var items: ArrayList<IFlexible<*>>? = null
    private var progressItem: ProgressItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autocomplete)
        setActionBar(toolbar)
        initRecyclerView()
        initSearchView()
    }

    override fun onDestroy() {
        super.onDestroy()
        poiSearch = null
        if (items != null) {
            items!!.clear()
            items = null
        }
        if (adapter != null) {
            adapter!!.mItemClickListener = null
            adapter!!.setEndlessScrollListener(null, progressItem!!)
            adapter = null
        }
        progressItem = null
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        Timber.d("onItemClick > position: $position")
        if (progressItem!!.status != ProgressItem.StatusEnum.MORE_TO_LOAD) {
            val itemCount = adapter!!.getItemCountOfTypes(R.layout.item_autocomplete)
            if (itemCount == position) {
                Timber.d("onItemClick#Retry > currentPage: ${adapter!!.endlessCurrentPage + 1}")
                loadMore(adapter!!.endlessCurrentPage)
            }
        }
        return false
    }

    override fun noMoreLoad(newItemsSize: Int) {
        Timber.d("noMoreLoad > newItemsSize: $newItemsSize")
    }

    override fun onLoadMore(lastPosition: Int, currentPage: Int) {
        Timber.d("onLoadMore > lastPosition: $lastPosition - currentPage: ${currentPage + 1}")
        loadMore(currentPage)
    }

    private fun loadMore(currentPage: Int) {
        Observable
                .just(currentPage + 1)
                .switchMap {
                    getSearchObservable(edit_text.text.toString(), it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    val poiItem = it.pois
                    Timber.d("onLoadMore > poiItem: $poiItem")

                    val newItems = arrayListOf<IFlexible<*>>()
                    if (poiItem.isNotEmpty()) {
                        val itemCount = adapter!!.getItemCountOfTypes(R.layout.item_autocomplete)
                        for (index in poiItem.indices) {
                            newItems.add(AutocompleteItem((itemCount + index).toString(), poiItem[index].title, poiItem[index].snippet))
                        }
                    }
                    adapter!!.onLoadMoreComplete(newItems)
                }.isDisposed
    }

    private fun initRecyclerView() {
        adapter = FlexibleAdapter(null, this, true)
        recycler_view.layoutManager = SmoothScrollLinearLayoutManager(this)
        recycler_view.adapter = adapter
        recycler_view.setHasFixedSize(true)

        items = arrayListOf()
        progressItem = ProgressItem()

        // Add FastScroll to the RecyclerView, after the Adapter has been attached the RecyclerView!!!
        adapter!!.fastScroller = fast_scroller
        adapter!!
                .setEndlessScrollListener(this, progressItem!!)
                .endlessPageSize = 10//Endless is automatically disabled if newItems < 10
    }

    private fun initSearchView() {
        RxTextView
                .textChanges(edit_text)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (it.isEmpty()) clear_button.visibility = View.GONE
                    else clear_button.visibility = View.VISIBLE
                }
                .debounce(DebounceTime.SEARCH_MILLISECONDS.time, TimeUnit.MILLISECONDS)
                .filter {
                    it.isNotBlank()
                }
                .switchMap {
                    getSearchObservable(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    val poiItem = it.pois
                    Timber.d("poiItem: $poiItem")
                    items!!.clear()
                    for (index in poiItem.indices) {
                        items!!.add(AutocompleteItem(index.toString(), poiItem[index].title, poiItem[index].snippet))
                    }
                    adapter!!.updateDataSet(items)

                    adapter!!.setEndlessProgressItem(progressItem)
                }.isDisposed

        clear_button.setOnClickListener {
            edit_text.text?.clear()
        }
        edit_text.requestFocus()

        edit_text.setHintTextColor(Color.parseColor("#42FFFFFF"))
        edit_text.setTextColor(Color.parseColor("#FFFFFF"))
        clear_button.setColorFilter(Color.parseColor("#FFFFFF"))
    }

    private fun getSearchObservable(keyWord: CharSequence, pageNum: Int = 1): Observable<PoiResult> {
        return Observable.create(ObservableOnSubscribe<PoiResult> { observableEmitter ->
            val settings = InternetObservingSettings.builder()
                    .host("www.baidu.com")
                    .strategy(SocketInternetObservingStrategy())
                    .build()
            ReactiveNetwork
                    .checkInternetConnectivity(settings)
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribe(Consumer {
                        if (it) {
                            progressItem!!.status = ProgressItem.StatusEnum.MORE_TO_LOAD

                            /*
                             * 第一个参数keyWord表示搜索字符串，
                             * 第二个参数表示POI搜索类型，二者选填其一，选用POI搜索类型时建议填写类型代码，码表可以参考下方（而非文字）
                             * 第三个参数cityCode表示POI搜索区域，可以是城市编码也可以是城市名称，也可以传空字符串，空字符串代表全国在全国范围内进行搜索
                             */
                            val query = PoiSearch.Query(keyWord.toString(), null)//第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
                            query.pageSize = 10//设置每页最多返回多少条
                            query.pageNum = pageNum//设置查第一页
                            poiSearch = PoiSearch(this, query)
                            poiSearch!!.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                                override fun onPoiItemSearched(poiItem: PoiItem, resultCode: Int) {
                                    Timber.v("onPoiItemSearched")
                                }

                                override fun onPoiSearched(poiResult: PoiResult, resultCode: Int) {
                                    Timber.v("onPoiSearched")
                                    if (resultCode == 1000) {
                                        observableEmitter.onNext(poiResult)
                                    } else {
                                        Timber.e("Location error. Please check the error code: https://lbs.amap.com/api/android-sdk/guide/map-tools/error-code")
                                        progressItem!!.status = ProgressItem.StatusEnum.ON_ERROR
                                        adapter!!.onLoadMoreComplete(null)
                                        adapter!!.setEndlessProgressItem(progressItem)
                                        adapter!!.addScrollableFooter(progressItem!!)
                                    }
                                }
                            })
                            poiSearch!!.searchPOIAsyn()
                        } else {
                            Timber.d("Network Unavailable")
                            progressItem!!.status = ProgressItem.StatusEnum.NETWORK_UNAVAILABLE
                            adapter!!.onLoadMoreComplete(null)
                            adapter!!.setEndlessProgressItem(progressItem)
                            adapter!!.addScrollableFooter(progressItem!!)
                        }
                    }).isDisposed
        })
                .subscribeOn(Schedulers.io())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
    }
}