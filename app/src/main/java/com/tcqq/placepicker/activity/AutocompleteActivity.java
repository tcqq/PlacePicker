package com.tcqq.placepicker.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;

import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.tcqq.placepicker.R;
import com.tcqq.placepicker.items.AutocompleteItem;
import com.tcqq.placepicker.items.ProgressItem;
import com.tcqq.placepicker.model.LimitTime;
import com.tcqq.placepicker.model.PoiItemModel;
import com.tcqq.placepicker.model.SelectedLocation;
import com.trello.rxlifecycle3.android.ActivityEvent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.items.IFlexible;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.tcqq.placepicker.activity.PlacePickerActivity.EXTRA_SELECTED_LOCATION;

/**
 * @author Alan Dreamer
 * @since 2018-12-11 Created
 */
public class AutocompleteActivity extends BaseActivity implements
        FlexibleAdapter.EndlessScrollListener,
        FlexibleAdapter.OnItemClickListener {

    private final String STATE_PAGE_NUMBER = "page_number";
    private final String STATE_KEY_WORDS = "key_words";
    private final String STATE_POI_ITEM = "poi_item";

    private PoiSearch poiSearch;
    private FlexibleAdapter<IFlexible<?>> adapter;
    private ArrayList<IFlexible<?>> items = new ArrayList<>();
    private List<PoiItemModel> poiItem = new ArrayList<>();
    private ProgressItem progressItem = new ProgressItem();
    private int pageNumber = 1;
    private String keyWords = "";

    private AppCompatEditText editText;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_PAGE_NUMBER, pageNumber);
        outState.putString(STATE_KEY_WORDS, keyWords);
        outState.putParcelableArrayList(STATE_POI_ITEM, (ArrayList<? extends Parcelable>) poiItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pageNumber = savedInstanceState.getInt(STATE_PAGE_NUMBER);
            keyWords = savedInstanceState.getString(STATE_KEY_WORDS);
            poiItem = savedInstanceState.getParcelableArrayList(STATE_POI_ITEM);
        }
        setContentView(R.layout.activity_autocomplete);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
        initRecyclerView();
        initSearchView();
    }

    private void initRecyclerView() {
        if (!poiItem.isEmpty()) {
            for (int i = 0; i < poiItem.size(); i++) {
                PoiItem item = poiItem.get(i).getPoiItem();
                items.add(new AutocompleteItem(
                        String.valueOf(i),
                        item.getTitle(),
                        item.getSnippet(),
                        item.getLatLonPoint().getLatitude(),
                        item.getLatLonPoint().getLongitude()));
            }
            adapter = new FlexibleAdapter<>(items, this, true);
        } else {
            adapter = new FlexibleAdapter<>(null, this, true);
        }
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new SmoothScrollLinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        FastScroller fastScroller = findViewById(R.id.fast_scroller);
        adapter.setFastScroller(fastScroller);
        adapter.setEndlessScrollListener(this, progressItem);
    }

    private void initSearchView() {
        editText = findViewById(R.id.edit_text);
        AppCompatImageButton clearButton = findViewById(R.id.clear_button);

        RxTextView
                .textChanges(editText)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(charSequence -> {
                    if (charSequence.length() == 0) clearButton.setVisibility(View.GONE);
                    else clearButton.setVisibility(View.VISIBLE);
                })
                .debounce(LimitTime.SEARCH_DEBOUNCE_MILLISECONDS, TimeUnit.MILLISECONDS)
                .filter(charSequence -> !TextUtils.isEmpty(charSequence) && !keyWords.equals(charSequence.toString()))
                .switchMap((Function<CharSequence, ObservableSource<PoiResult>>) charSequence -> {
                    Timber.d("keyWords changed: %s", charSequence);
                    keyWords = charSequence.toString();
                    poiItem.clear();
                    pageNumber = 1;
                    return getSearchObservable(charSequence, 1);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(result -> {
                    items.clear();
                    ArrayList<PoiItem> poiItems = result.getPois();
                    Timber.d("poiItem: %s", poiItems);
                    for (int i = 0; i < poiItems.size(); i++) {
                        PoiItem item = poiItems.get(i);
                        items.add(new AutocompleteItem(
                                String.valueOf(i),
                                item.getTitle(),
                                item.getSnippet(),
                                item.getLatLonPoint().getLatitude(),
                                item.getLatLonPoint().getLongitude()));
                        poiItem.add(new PoiItemModel(item));
                    }
                    adapter.updateDataSet(items);
                    adapter.setEndlessProgressItem(progressItem);
                }).isDisposed();

        clearButton.setOnClickListener(v -> editText.setText(""));
        editText.requestFocus();
        editText.setHintTextColor(Color.parseColor("#42FFFFFF"));
        editText.setTextColor(Color.parseColor("#FFFFFF"));
        clearButton.setColorFilter(Color.parseColor("#FFFFFF"));
    }

    @Override
    public boolean onItemClick(View view, int position) {
        int itemCount = adapter.getItemCountOfTypes(R.layout.item_autocomplete);
        if (progressItem.getStatus() == ProgressItem.StatusEnum.ON_ERROR
                && itemCount == position) {
            loadMore(++pageNumber);
            Timber.d("onItemClick#retry#position: " + position + "pageNumber: " + pageNumber);
        } else {
            Timber.d("onItemClick#position: %s", position);
            AutocompleteItem item = adapter.getItem(position, AutocompleteItem.class);
            if (item != null) {
                setSelectedLocation(item.getPlaceName(), item.getLatitude(), item.getLongitude());
            }
        }
        return false;
    }

    private void setSelectedLocation(String placeName,
                                     Double latitude,
                                     Double longitude) {
        Intent intent = new Intent(this, PlacePickerActivity.class);
        intent.putExtra(EXTRA_SELECTED_LOCATION, new SelectedLocation(placeName, latitude, longitude));
        setResult(RESULT_OK, intent);
        finish();
    }

    private void loadMore(int currentPage) {
        Observable
                .just(currentPage)
                .switchMap((Function<Integer, ObservableSource<PoiResult>>) integer -> getSearchObservable(Objects.requireNonNull(editText.getText()).toString(), integer))
                .subscribe(new Observer<PoiResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(PoiResult result) {
                        ArrayList<PoiItem> poiItems = result.getPois();
                        Timber.d("poiItemSize: " + poiItems.size() + "poiItem: " + poiItems);
                        List<IFlexible<?>> newItem = new ArrayList<>();
                        if (!poiItems.isEmpty()) {
                            int itemCount = adapter.getItemCountOfTypes(R.layout.item_autocomplete);
                            for (int i = 0; i < poiItems.size(); i++) {
                                poiItem.add(new PoiItemModel(poiItems.get(i)));
                                PoiItem item = poiItems.get(i);
                                newItem.add(new AutocompleteItem(
                                        String.valueOf((itemCount + i)),
                                        item.getTitle(),
                                        item.getSnippet(),
                                        item.getLatLonPoint().getLatitude(),
                                        item.getLatLonPoint().getLongitude()));
                            }
                        }
                        adapter.onLoadMoreComplete(newItem);
                        if (newItem.size() < 10) {
                            adapter.setEndlessProgressItem(null);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e.getLocalizedMessage());
                        --pageNumber;
                        progressItem.setStatus(ProgressItem.StatusEnum.ON_ERROR);
                        adapter.setEndlessProgressItem(null);
                        adapter.addScrollableFooter(progressItem);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void noMoreLoad(int newItemsSize) {
        Timber.d("noMoreLoad#newItemsSize:%s", newItemsSize);
    }

    @Override
    public void onLoadMore(int lastPosition, int currentPage) {
        loadMore(++pageNumber);
        Timber.d("onLoadMore#pageNumber: %s", pageNumber);
    }

    private Observable<PoiResult> getSearchObservable(CharSequence keyWord, int pageNum) {
        return Observable.create((ObservableOnSubscribe<PoiResult>) emitter -> {
            /*
             * 第一个参数keyWord表示搜索字符串，
             * 第二个参数表示POI搜索类型，二者选填其一，选用POI搜索类型时建议填写类型代码，码表可以参考下方（而非文字）
             * 第三个参数cityCode表示POI搜索区域，可以是城市编码也可以是城市名称，也可以传空字符串，空字符串代表全国在全国范围内进行搜索
             */
            PoiSearch.Query query = new PoiSearch.Query(keyWord.toString(), null);
            query.setPageSize(10); //设置每页最多返回多少条
            query.setPageNum(pageNum); //设置查第一页
            poiSearch = new PoiSearch(this, query);
            poiSearch.searchPOIAsyn();
            poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiItemSearched(PoiItem poiItem, int resultCode) {
                    Timber.v("onPoiItemSearched");
                }

                @Override
                public void onPoiSearched(PoiResult poiResult, int resultCode) {
                    Timber.v("onPoiSearched");
                    if (resultCode == 1000) {
                        emitter.onNext(poiResult);
                    } else {
                        final String errorMessage = "SelectedLocation error. Please check the error code: https://lbs.amap.com/api/android-sdk/guide/map-tools/error-code";
                        Timber.e(errorMessage);
                        emitter.onError(new Throwable(errorMessage));
                    }
                }
            });
        })
                .subscribeOn(Schedulers.io())
                .compose(bindUntilEvent(ActivityEvent.DESTROY));
    }
}
