package com.panaceasoft.pswallpaper.ui.search.selection.categoryselection;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.panaceasoft.pswallpaper.Config;
import com.panaceasoft.pswallpaper.R;
import com.panaceasoft.pswallpaper.binding.FragmentDataBindingComponent;
import com.panaceasoft.pswallpaper.databinding.FragmentCategorySelectionListBinding;
import com.panaceasoft.pswallpaper.ui.common.DataBoundListAdapter;
import com.panaceasoft.pswallpaper.ui.common.PSFragment;
import com.panaceasoft.pswallpaper.ui.search.adapter.CategorySelectionAdapter;
import com.panaceasoft.pswallpaper.utils.AutoClearedValue;
import com.panaceasoft.pswallpaper.utils.Constants;
import com.panaceasoft.pswallpaper.utils.Utils;
import com.panaceasoft.pswallpaper.viewmodel.category.CategoryViewModel;
import com.panaceasoft.pswallpaper.viewobject.Category;
import com.panaceasoft.pswallpaper.viewobject.common.Status;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CategorySelectionListFragment extends PSFragment implements DataBoundListAdapter.DiffUtilDispatchedInterface {

    //region Variables

    private final androidx.databinding.DataBindingComponent dataBindingComponent = new FragmentDataBindingComponent(this);

    private CategoryViewModel categoryViewModel;

    private String cat_id;

    @VisibleForTesting
    private AutoClearedValue<FragmentCategorySelectionListBinding> binding;
    private AutoClearedValue<CategorySelectionAdapter> adapter;


    //endregion

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        FragmentCategorySelectionListBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_category_selection_list, container, false, dataBindingComponent);

        binding = new AutoClearedValue<>(this, dataBinding);

        binding.get().setLoadingMore(connectivity.isConnected());
        setHasOptionsMenu(true);

        if (getActivity() != null) {
            Intent intent = getActivity().getIntent();
            this.cat_id = intent.getStringExtra(Constants.INTENT__CAT_ID);
        }

        return binding.get().getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.clear_button, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.clear) {
            this.cat_id = "";

            initAdapters();

            initData();

            navigationController.navigateBackToSearchFragment(this.getActivity(), this.cat_id, "");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initUIAndActions() {
        binding.get().scrollFloatingButton.setOnClickListener(v -> binding.get().categoryList.smoothScrollToPosition(0));

        binding.get().categoryList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager)
                        recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastPosition = layoutManager
                            .findLastVisibleItemPosition();
                    if (lastPosition == adapter.get().getItemCount() - 1) {

                        if (!binding.get().getLoadingMore() && !categoryViewModel.forceEndLoading) {

                            if (connectivity.isConnected()) {

                                categoryViewModel.loadingDirection = Utils.LoadingDirection.bottom;

                                int limit = Config.CATEGORY_COUNT;
                                categoryViewModel.offset = categoryViewModel.offset + limit;

                                categoryViewModel.setNextPageLoadingStateObj(String.valueOf(Config.CATEGORY_COUNT), String.valueOf(categoryViewModel.offset));
                            }
                        }
                    }
                }
            }
        });

        binding.get().swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.view__primary_line));
        binding.get().swipeRefresh.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.global__primary));
        binding.get().swipeRefresh.setOnRefreshListener(() -> {

            categoryViewModel.loadingDirection = Utils.LoadingDirection.top;

            // reset reservationViewModel.offset
            categoryViewModel.offset = 0;

            // reset reservationViewModel.forceEndLoading
            categoryViewModel.forceEndLoading = false;

            // update live data
            categoryViewModel.setCategoryList(String.valueOf(Config.CATEGORY_COUNT), String.valueOf(categoryViewModel.offset));

        });
    }

    @Override
    protected void initViewModels() {
        categoryViewModel = new ViewModelProvider(this, viewModelFactory).get(CategoryViewModel.class);
    }

    @Override
    protected void initAdapters() {
        CategorySelectionAdapter nvadapter = new CategorySelectionAdapter(dataBindingComponent,
                (category, id) -> {

                    navigationController.navigateBackToSearchFragment(this.getActivity(), category.cat_id, category.cat_name);

                    if (getActivity() != null) {
                        CategorySelectionListFragment.this.getActivity().finish();
                    }
                }, this.cat_id);
        this.adapter = new AutoClearedValue<>(this, nvadapter);
        binding.get().categoryList.setAdapter(this.adapter.get());


        showHideWhenScroll();
    }

    private void showHideWhenScroll() {
        binding.get().categoryList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) binding.get().scrollFloatingButton.show();
                else binding.get().scrollFloatingButton.hide();
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    @Override
    protected void initData() {

        categoryViewModel.setCategoryList(String.valueOf(Config.CATEGORY_COUNT), String.valueOf(categoryViewModel.offset));
        categoryViewModel.getAllCategoryList().observe(this, resource -> {

            if (resource != null) {

                Utils.psLog("Got Data" + resource.message + resource.toString());

                switch (resource.status) {
                    case LOADING:
                        // Loading State
                        // Data are from Local DB

                        if (resource.data != null) {

                            fadeIn(binding.get().getRoot());

                            replaceDataByCategories(resource.data);
                        }
                        break;
                    case SUCCESS:
                        // Success State
                        // Data are from Server

                        if (resource.data != null) {

                            replaceDataByCategories(resource.data);
                        }

                        categoryViewModel.setLoadingState(false);
                        break;
                    case ERROR:
                        // Error State
                        categoryViewModel.setLoadingState(false);
                        break;
                    default:
                        // Default

                        break;
                }

            } else {

                // Init Object or Empty Data
                Utils.psLog("Empty Data");

            }


            // we don't need any null checks here for the adapter since LiveData guarantees that
            // it won't call us if fragment is stopped or not started.
            if (resource != null) {
                Utils.psLog("Got Data Of all wallpapers.");


            } else {

                Utils.psLog("No Data of all wallpapers.");
            }
        });


        categoryViewModel.getNextPageLoadingStateData().observe(this, state -> {
            if (state != null) {
                if (state.status == Status.ERROR) {
                    Utils.psLog("Next Page State : " + state.data);

                    categoryViewModel.setLoadingState(false);//hide
                    categoryViewModel.forceEndLoading = true;//stop
                }
            }
        });

        categoryViewModel.getLoadingState().observe(this, loadingState -> {

            binding.get().setLoadingMore(categoryViewModel.isLoading);

            if (loadingState != null && !loadingState) {
                binding.get().swipeRefresh.setRefreshing(false);
            }

        });

    }
    //endregion

    private void replaceDataByCategories(List<Category> categories) {
        adapter.get().replace(categories);
        binding.get().executePendingBindings();


    }

    @Override
    public void onDispatched() {

    }
}
