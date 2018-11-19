package com.tcqq.placepicker.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.jakewharton.rxbinding2.widget.RxTextView
import com.tcqq.placepicker.R
import com.tcqq.placepicker.adapter.items.ProgressItem
import com.tcqq.placepicker.enums.DebounceTime
import com.tcqq.placepicker.items.AutocompleteItem
import com.tcqq.placepicker.model.PoiItemModel
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

    private lateinit var poiSearch: PoiSearch
    private lateinit var adapter: FlexibleAdapter<IFlexible<*>>
    private var items: ArrayList<IFlexible<*>> = arrayListOf()
    private var poiItem = arrayListOf<PoiItemModel>()
    private val progressItem by lazy {
        ProgressItem()
    }
    private var pageNumber = 1
    private var keyWords = ""

    companion object {
        private const val STATE_PAGE_NUMBER = "page_number"
        private const val STATE_KEY_WORDS = "key_words"
        private const val STATE_POI_ITEM = "poi_item"
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putInt(STATE_PAGE_NUMBER, pageNumber)
            putString(STATE_KEY_WORDS, keyWords)
            putParcelableArrayList(STATE_POI_ITEM, poiItem)
//            adapter.onSaveInstanceState(this)
        }
        super.onSaveInstanceState(outState)
    }

/*    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.run {
            adapter.onRestoreInstanceState(savedInstanceState)
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            with(savedInstanceState) {
                pageNumber = getInt(STATE_PAGE_NUMBER)
                keyWords = getString(STATE_KEY_WORDS) ?: ""
                poiItem = getParcelableArrayList<PoiItemModel>(STATE_POI_ITEM)!!
            }
        }
        setContentView(R.layout.activity_autocomplete)
        setActionBar(toolbar)
        initRecyclerView()
        initSearchView()
    }

    private fun loadMore(currentPage: Int) {
        Observable
                .just(currentPage)
                .switchMap {
                    getSearchObservable(edit_text.text.toString(), it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(object : Observer<PoiResult?> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onNext(result: PoiResult) {
                        result.pois.also {
                            Timber.d("poiItemSize:${it.size} poiItem: $it")
                            val newItem = arrayListOf<IFlexible<*>>()
                            if (it.isNotEmpty()) {
                                val itemCount = adapter.getItemCountOfTypes(R.layout.item_autocomplete)
                                for (index in it.indices) {
                                    poiItem.add(PoiItemModel(it[index]))
                                    newItem.add(AutocompleteItem((itemCount + index).toString(), it[index].title, it[index].snippet))
                                }
                            }
                            adapter.apply {
                                onLoadMoreComplete(newItem)
                                if (newItem.size < 10) {
                                    setEndlessProgressItem(null)
                                }
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e.localizedMessage)
                        --pageNumber
                        progressItem.status = ProgressItem.StatusEnum.ON_ERROR
                        adapter.apply {
                            setEndlessProgressItem(null)
                            addScrollableFooter(progressItem)
                        }
                    }
                })
    }

    private fun initRecyclerView() {
        adapter = FlexibleAdapter(
                with(poiItem) {
                    if (isNotEmpty()) {
                        Timber.d("Restore item, poiItem: $poiItem")
                        for (index in poiItem.indices) {
                            with(poiItem[index].poiItem) {
                                items.add(AutocompleteItem(index.toString(), title, snippet))
                            }
                        }
                        items
                    } else {
                        Timber.d("Init items: null")
                        null
                    }
                },
                this,
                true)
        adapter.setAutoScrollOnExpand(true)
                //.setAnimateToLimit(Integer.MAX_VALUE) //Use the default value
                .setNotifyMoveOfFilteredItems(true) //When true, filtering on big list is very slow, not in this case!
                .setNotifyChangeOfUnfilteredItems(true) //true by default
                .setAnimationOnReverseScrolling(true)
        recycler_view.layoutManager = SmoothScrollLinearLayoutManager(this)
        recycler_view.adapter = adapter
        recycler_view.setHasFixedSize(true)
        adapter.apply {
            fastScroller = fast_scroller
            setEndlessScrollListener(this@AutocompleteActivity, progressItem)
        }
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
                    it.isNotBlank() && keyWords != it.toString()
                }
                .switchMap {
                    Timber.d("keyWords changed: $it")
                    keyWords = it.toString()
                    poiItem.clear()
                    pageNumber = 1
                    getSearchObservable(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe { result ->
                    items.clear()
                    result.pois.also {
                        Timber.d("poiItem: $it")
                        for (index in it.indices) {
                            items.add(AutocompleteItem(index.toString(), it[index].title, it[index].snippet))
                            poiItem.add(PoiItemModel(it[index]))
                        }
                        adapter.apply {
                            updateDataSet(items)
                            setEndlessProgressItem(progressItem)
                        }
                    }

                }.isDisposed

        clear_button.setOnClickListener {
            edit_text.text?.clear()
        }
        edit_text.requestFocus()

        edit_text.setHintTextColor(Color.parseColor("#42FFFFFF"))
        edit_text.setTextColor(Color.parseColor("#FFFFFF"))
        clear_button.setColorFilter(Color.parseColor("#FFFFFF"))
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        Timber.d("onItemClick > position: $position")
        if (progressItem.status == ProgressItem.StatusEnum.ON_ERROR) {
            val itemCount = adapter.getItemCountOfTypes(R.layout.item_autocomplete)
            if (itemCount == position) {
                loadMore(++pageNumber)
                Timber.d("onItemClick#Retry > pageNumber: $pageNumber")
            }
        }
        return false
    }

    override fun noMoreLoad(newItemsSize: Int) {
        Timber.d("noMoreLoad > newItemsSize: $newItemsSize")
    }

    override fun onLoadMore(lastPosition: Int, currentPage: Int) {
        loadMore(++pageNumber)
        Timber.d("onLoadMore > pageNumber: $pageNumber")
    }

    private fun getSearchObservable(keyWord: CharSequence, pageNum: Int = 1): Observable<PoiResult> {
        return Observable.create(ObservableOnSubscribe<PoiResult> {
            /**
             * 第一个参数keyWord表示搜索字符串，
             * 第二个参数表示POI搜索类型，二者选填其一，选用POI搜索类型时建议填写类型代码，码表可以参考下方（而非文字）
             * 第三个参数cityCode表示POI搜索区域，可以是城市编码也可以是城市名称，也可以传空字符串，空字符串代表全国在全国范围内进行搜索
             */
            val query = PoiSearch.Query(keyWord.toString(), null)
            query.pageSize = 10//设置每页最多返回多少条
            query.pageNum = pageNum//设置查第一页
            poiSearch = PoiSearch(this, query)
            poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                override fun onPoiItemSearched(poiItem: PoiItem, resultCode: Int) {
                    Timber.v("onPoiItemSearched")
                }

                override fun onPoiSearched(poiResult: PoiResult, resultCode: Int) {
                    Timber.v("onPoiSearched")
                    if (resultCode == 1000) {
                        it.onNext(poiResult)
                    } else {
                        val errorMessage = "Location error. Please check the error code: https://lbs.amap.com/api/android-sdk/guide/map-tools/error-code"
                        Timber.e(errorMessage)
                        it.onError(Throwable(errorMessage))
                    }
                }
            })
            poiSearch.searchPOIAsyn()
        })
                .subscribeOn(Schedulers.io())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
    }
}