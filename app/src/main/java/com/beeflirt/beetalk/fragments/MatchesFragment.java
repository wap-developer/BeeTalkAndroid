package com.beeflirt.beetalk.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.MatchesAdapter;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Match;
import com.beeflirt.beetalk.repository.MatchRepository;
import com.beeflirt.beetalk.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchesFragment extends Fragment implements MatchesAdapter.OnMatchClickListener {

    private RecyclerView recyclerView;
    private MatchesAdapter adapter;
    private List<Match> matches;
    private ProgressBar progressBar;
    private LinearLayout tvNoMatches;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ApiService apiService;
    private MatchRepository matchRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matches, container, false);

        // Initialize API
        apiService = ApiClient.getApiService(requireContext());
        matchRepository = new MatchRepository(requireContext());

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoMatches = view.findViewById(R.id.tv_no_matches);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        // Initialize matches list
        matches = new ArrayList<>();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadMatches);

        // Load matches
        loadMatches();

        // Initialize and load banner ad
        RelativeLayout bannerContainer = view.findViewById(R.id.banner_container);
        Ads.ShowBanner(getActivity(), bannerContainer, com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_BANNER_MATCHES);

        return view;
    }

    private void setupRecyclerView() {
        adapter = new MatchesAdapter(matches, this);
        // Use GridLayoutManager with 2 columns for modern grid layout
        androidx.recyclerview.widget.GridLayoutManager gridLayoutManager = new androidx.recyclerview.widget.GridLayoutManager(
                requireContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void loadMatches() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        tvNoMatches.setVisibility(View.GONE);

        matchRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> newMatches) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (newMatches != null && !newMatches.isEmpty()) {
                    matches.clear();
                    matches.addAll(newMatches);
                    adapter.notifyDataSetChanged();

                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoMatches.setVisibility(View.GONE);
                } else {
                    showNoMatches();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                showNoMatches();
            }
        });
    }

    private void showNoMatches() {
        recyclerView.setVisibility(View.GONE);
        tvNoMatches.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMatchClick(Match match) {
        if (!isAdded()) return;

        android.content.Intent intent = new android.content.Intent(requireContext(),
                com.beeflirt.beetalk.activities.ChatActivity.class);
        intent.putExtra("match_id", match.getMatchId());
        intent.putExtra("user_id", match.getUserId());
        intent.putExtra("user_name", match.getName());
        intent.putExtra("user_photo", match.getPrimaryPhoto());
        intent.putExtra("is_online", match.getIsOnline());
        intent.putExtra("last_seen", match.getLastSeen());
        startActivity(intent);
    }

    @Override
    public void onUnmatchClick(Match match) {
        if (!isAdded()) return;
        confirmUnmatch(match);
    }

    private void confirmUnmatch(Match match) {
        if (!isAdded()) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Unmatch")
                .setMessage("Are you sure you want to unmatch with " + match.getName()
                        + "? This will delete all your conversation history.")
                .setPositiveButton("Unmatch", (dialog, which) -> unmatch(match))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void unmatch(Match match) {
        if (!isAdded()) return;

        progressBar.setVisibility(View.VISIBLE);

        apiService.unmatch(match.getMatchId()).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Toast.makeText(requireContext(), "Unmatched successfully", Toast.LENGTH_SHORT).show();
                        matches.remove(match);
                        adapter.notifyDataSetChanged();

                        if (matches.isEmpty()) {
                            showNoMatches();
                        }
                    } else {
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to unmatch", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload matches when fragment resumes (e.g., after coming back from
        // ChatActivity)
        loadMatches();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
